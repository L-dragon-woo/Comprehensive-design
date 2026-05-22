from __future__ import annotations

import json
import re
import sys
import urllib.request


API_URL = "http://localhost:8000/api/chat"


def _t(text: str) -> str:
    return re.sub(r"\\u([0-9a-fA-F]{4})", lambda match: chr(int(match.group(1), 16)), text)


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
    print(_t("SkinAI \\uc0c1\\ub2f4 \\ucf58\\uc194\\uc785\\ub2c8\\ub2e4. \\uc885\\ub8cc\\ud558\\ub824\\uba74 exit \\uc785\\ub825"))
    print(_t("\\ucc38\\uace0: mode=llm\\uc774\\uba74 LLM \\uc751\\ub2f5, mode=llm_error/fallback\\uc774\\uba74 \\ub85c\\uceec \\ubd84\\uc11d \\uae30\\ubc18 \\uc751\\ub2f5\\uc785\\ub2c8\\ub2e4."))
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
            print(_t("AI> \\uc694\\uccad \\uc2e4\\ud328: ") + str(exc))
            continue
        print(f"AI ({mode})> {answer}")


if __name__ == "__main__":
    main()
