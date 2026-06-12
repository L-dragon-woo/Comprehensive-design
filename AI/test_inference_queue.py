import ast
import asyncio
import threading
import time
import unittest
import uuid
from collections import deque
from pathlib import Path


class FakeMetric:
    def labels(self, *args):
        return self

    def inc(self):
        pass

    def observe(self, value):
        pass

    def set(self, value):
        pass


class HTTPException(Exception):
    def __init__(self, status_code, detail):
        super().__init__(detail)
        self.status_code = status_code
        self.detail = detail


async def run_in_threadpool(operation):
    return await asyncio.to_thread(operation)


def load_inference_queue_class():
    api_path = Path(__file__).with_name("api.py")
    module = ast.parse(api_path.read_text(encoding="utf-8-sig"), filename=str(api_path))
    class_node = next(
        node for node in module.body if isinstance(node, ast.ClassDef) and node.name == "InferenceQueue"
    )
    metric = FakeMetric()
    namespace = {
        "HTTPException": HTTPException,
        "INFERENCE_COUNT": metric,
        "INFERENCE_QUEUE_ACTIVE": metric,
        "INFERENCE_QUEUE_WAIT_DURATION": metric,
        "INFERENCE_QUEUE_WAITING": metric,
        "asyncio": asyncio,
        "deque": deque,
        "run_in_threadpool": run_in_threadpool,
        "time": time,
        "uuid": uuid,
    }
    exec(compile(ast.Module(body=[class_node], type_ignores=[]), str(api_path), "exec"), namespace)
    return namespace["InferenceQueue"]


InferenceQueue = load_inference_queue_class()


class InferenceQueueTest(unittest.IsolatedAsyncioTestCase):
    async def wait_for_waiting_count(self, queue, expected):
        deadline = time.monotonic() + 2
        while time.monotonic() < deadline:
            async with queue._condition:
                if len(queue._waiting) == expected:
                    return
            await asyncio.sleep(0.001)
        self.fail(f"queue did not reach {expected} waiting requests")

    async def test_runs_waiting_operations_in_fifo_order(self):
        queue = InferenceQueue(max_waiting=3, wait_timeout_seconds=2)
        release_active = threading.Event()
        active_started = threading.Event()
        order = []

        def active_operation():
            active_started.set()
            release_active.wait(timeout=2)

        active = asyncio.create_task(queue.run(active_operation))
        self.assertTrue(await asyncio.to_thread(active_started.wait, 2))
        first = asyncio.create_task(queue.run(lambda: order.append("first")))
        await self.wait_for_waiting_count(queue, 1)
        second = asyncio.create_task(queue.run(lambda: order.append("second")))
        await self.wait_for_waiting_count(queue, 2)

        release_active.set()
        await asyncio.gather(active, first, second)
        self.assertEqual(["first", "second"], order)

    async def test_never_runs_more_than_one_operation_at_a_time(self):
        queue = InferenceQueue(max_waiting=4, wait_timeout_seconds=2)
        active_count = 0
        maximum_active_count = 0
        state_lock = threading.Lock()

        def operation():
            nonlocal active_count, maximum_active_count
            with state_lock:
                active_count += 1
                maximum_active_count = max(maximum_active_count, active_count)
            time.sleep(0.01)
            with state_lock:
                active_count -= 1

        await asyncio.gather(*(queue.run(operation) for _ in range(4)))
        self.assertEqual(1, maximum_active_count)

    async def test_rejects_new_request_when_waiting_queue_is_full(self):
        queue = InferenceQueue(max_waiting=1, wait_timeout_seconds=2)
        release_active = threading.Event()
        active_started = threading.Event()

        def active_operation():
            active_started.set()
            release_active.wait(timeout=2)

        active = asyncio.create_task(queue.run(active_operation))
        self.assertTrue(await asyncio.to_thread(active_started.wait, 2))
        waiting = asyncio.create_task(queue.run(lambda: None))
        await self.wait_for_waiting_count(queue, 1)

        with self.assertRaises(HTTPException) as raised:
            await queue.run(lambda: None)

        release_active.set()
        await asyncio.gather(active, waiting)
        self.assertEqual(429, raised.exception.status_code)

    async def test_removes_request_from_queue_when_wait_times_out(self):
        queue = InferenceQueue(max_waiting=1, wait_timeout_seconds=0.05)
        release_active = threading.Event()
        active_started = threading.Event()

        def active_operation():
            active_started.set()
            release_active.wait(timeout=2)

        active = asyncio.create_task(queue.run(active_operation))
        self.assertTrue(await asyncio.to_thread(active_started.wait, 2))

        with self.assertRaises(HTTPException) as raised:
            await queue.run(lambda: None)

        async with queue._condition:
            self.assertEqual(0, len(queue._waiting))
        release_active.set()
        await active
        self.assertEqual(503, raised.exception.status_code)

    async def test_releases_active_slot_when_operation_raises(self):
        queue = InferenceQueue(max_waiting=1, wait_timeout_seconds=2)

        with self.assertRaisesRegex(RuntimeError, "operation failed"):
            await queue.run(lambda: (_ for _ in ()).throw(RuntimeError("operation failed")))

        self.assertEqual("next operation ran", await queue.run(lambda: "next operation ran"))

    async def test_cancellation_removes_waiting_request(self):
        queue = InferenceQueue(max_waiting=2, wait_timeout_seconds=2)
        release_active = threading.Event()
        active_started = threading.Event()

        active = asyncio.create_task(
            queue.run(lambda: (active_started.set(), release_active.wait(timeout=2)))
        )
        self.assertTrue(await asyncio.to_thread(active_started.wait, 2))
        waiting = asyncio.create_task(queue.run(lambda: None))
        await self.wait_for_waiting_count(queue, 1)

        waiting.cancel()
        with self.assertRaises(asyncio.CancelledError):
            await waiting
        async with queue._condition:
            self.assertEqual(0, len(queue._waiting))

        release_active.set()
        await active

    async def test_active_cancellation_waits_for_inference_before_releasing_slot(self):
        queue = InferenceQueue(max_waiting=1, wait_timeout_seconds=2)
        release_active = threading.Event()
        active_started = threading.Event()
        next_started = threading.Event()

        active = asyncio.create_task(
            queue.run(lambda: (active_started.set(), release_active.wait(timeout=2)))
        )
        self.assertTrue(await asyncio.to_thread(active_started.wait, 2))
        active.cancel()
        waiting = asyncio.create_task(queue.run(next_started.set))
        await self.wait_for_waiting_count(queue, 1)
        self.assertFalse(next_started.is_set())

        release_active.set()
        with self.assertRaises(asyncio.CancelledError):
            await active
        await waiting
        self.assertTrue(next_started.is_set())


if __name__ == "__main__":
    unittest.main()
