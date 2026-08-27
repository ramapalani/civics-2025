#!/usr/bin/env python3
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
path = root / "core/src/main/resources/questions.json"
questions = json.loads(path.read_text())
ids = [q["id"] for q in questions]
assert ids == list(range(1, 129)), ids
assert sum(1 for q in questions if q["starred6520"]) == 20
assert all(q["acceptedAnswers"] and q["extraInfo"] for q in questions)
assert {q["id"] for q in questions if q["answerKind"] == "all_n"} == {10, 48, 65, 67, 69, 81, 126}
print(f"OK: {len(questions)} questions, 20 starred, extraInfo present")
