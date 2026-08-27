"""Parse official M-1778 Q&A text into {id: [answers]}."""
from __future__ import annotations

import re
from pathlib import Path

QUESTION_START = re.compile(r"^(\d{1,3})\.\s+(.+)$")
BULLET = re.compile(r"^[•·]\s*(.+)$")
SKIP = re.compile(
    r"^(M-1778|AMERICAN |SYMBOLS |A: |B: |C: |\d+ of 19|uscis\.gov|"
    r"For a complete list|Listed below|These questions|test is an oral|"
    r"questions\. You must|of the civics test|On the civics|"
    r"test\. You must|naturalization interview|Although USCIS|"
    r"applicants are encouraged|65/20|If you are 65|resident of the|"
    r"have been marked|the civics test in|the 20 civics|60%\) correctly|$)",
    re.I,
)


def parse_official(path: Path) -> dict[int, list[str]]:
    text = path.read_text(encoding="utf-8")
    found: dict[int, list[str]] = {}
    current: int | None = None
    last_was_answer = False
    for original in text.replace("\x0c", "\n").splitlines():
        line = original.strip().lstrip("* ").rstrip()
        line = re.sub(r"\s+\*$", "", line).strip()
        if not line or line == "*" or SKIP.search(line):
            continue
        q = QUESTION_START.match(line)
        if q:
            current = int(q.group(1))
            found[current] = []
            last_was_answer = False
            continue
        if current is None:
            continue
        b = BULLET.match(line)
        if b:
            found[current].append(b.group(1).strip())
            last_was_answer = True
            continue
        if last_was_answer and found[current]:
            found[current][-1] = re.sub(r"\s+", " ", found[current][-1] + " " + line)
    return {i: answers for i, answers in found.items() if answers}


def usable(answer: str) -> bool:
    text = answer.strip()
    if not text:
        return False
    if text.lower().startswith("answers will vary"):
        return False
    if text.lower().startswith("visit uscis.gov"):
        return False
    return True
