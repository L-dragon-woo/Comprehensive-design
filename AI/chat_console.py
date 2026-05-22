from __future__ import annotations

import json
import sys
import urllib.request


API_URL = "http://localhost:8000/api/chat"


def ask(message: str, session_id: str | None) -> tuple[str | None, str, str]:
    payload = json.dumps(
        {"message": message, "sessionId": session_id},
        ensure_ascii=False,
    ).encode("utf-8")
    request = urllib.request.Request(
        API_URL,
        data=payload,
        headers={"Content-Type": "application/json; charset=utf-8"},
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        data = json.loads(response.read().decode("utf-8"))
    return data.get("sessionId"), data.get("content", ""), data.get("mode", "unknown")


def main() -> None:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    if hasattr(sys.stdin, "reconfigure"):
        sys.stdin.reconfigure(encoding="utf-8")

    session_id: str | None = None
    print("SkinAI 상담 콘솔입니다. 종료하려면 exit 입력")
    print("참고: mode=fallback이면 LLM 키 없이 로컬 분석 결과 기반 규칙 상담입니다.")
    while True:
        try:
            message = input("You: ").strip()
        except (EOFError, KeyboardInterrupt):
            print()
            break
        if message.lower() in {"exit", "quit"}:
            break
        if not message:
            continue

        try:
            session_id, answer, mode = ask(message, session_id)
        except Exception as exc:  # noqa: BLE001
            print(f"AI> 요청 실패: {exc}")
            continue
        print(f"AI ({mode})> {answer}")


if __name__ == "__main__":
    main()
