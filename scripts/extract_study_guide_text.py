#!/usr/bin/env python3
"""Extract per-page text from One Nation, One People for in-app reading and TTS."""
import json
import subprocess
from pathlib import Path

root = Path(__file__).resolve().parents[1]
pdf = root / "references" / "USCIS-2025-Civics-Test-Study-Guide.pdf"
dest = root / "app" / "src" / "main" / "assets" / "study-guide-text.json"
info = subprocess.check_output(["pdfinfo", str(pdf)], text=True)
pages_n = int(next(line.split(":", 1)[1] for line in info.splitlines() if line.startswith("Pages:")))
pages = []
for i in range(1, pages_n + 1):
    text = subprocess.check_output(
        ["pdftotext", "-f", str(i), "-l", str(i), "-enc", "UTF-8", str(pdf), "-"],
        text=True,
    )
    pages.append("\n".join(line.rstrip() for line in text.splitlines()).strip())
dest.write_text(json.dumps(pages, ensure_ascii=False), encoding="utf-8")
print(f"Wrote {pages_n} pages to {dest}")
