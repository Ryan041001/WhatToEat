#!/usr/bin/env python3

from __future__ import annotations

import math
from pathlib import Path
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[1]
BADGE_DIR = ROOT / ".github" / "badges"
BACKEND_REPORT = ROOT / "backend" / "target" / "site" / "jacoco" / "jacoco.xml"
AI_REPORT = ROOT / "ai-service" / "coverage.xml"


def backend_coverage() -> float:
    root = ET.parse(BACKEND_REPORT).getroot()
    for counter in root.findall("counter"):
        if counter.attrib.get("type") == "LINE":
            covered = int(counter.attrib["covered"])
            missed = int(counter.attrib["missed"])
            return covered / (covered + missed) * 100 if covered + missed else 0.0
    raise ValueError("JaCoCo LINE counter not found")


def ai_coverage() -> float:
    root = ET.parse(AI_REPORT).getroot()
    line_rate = root.attrib.get("line-rate")
    if line_rate is None:
        raise ValueError("coverage.xml line-rate not found")
    return float(line_rate) * 100


def badge_color(coverage: float) -> str:
    if coverage >= 90:
        return "#2ea44f"
    if coverage >= 80:
        return "#97ca00"
    if coverage >= 70:
        return "#a4a61d"
    if coverage >= 60:
        return "#dfb317"
    if coverage >= 50:
        return "#fe7d37"
    return "#e05d44"


def text_width(text: str) -> int:
    return max(40, int(math.ceil(len(text) * 7.2 + 10)))


def render_badge(label: str, value: str, color: str) -> str:
    label_width = text_width(label)
    value_width = text_width(value)
    total_width = label_width + value_width
    label_center = label_width / 2
    value_center = label_width + value_width / 2
    return f"""<svg xmlns="http://www.w3.org/2000/svg" width="{total_width}" height="20" role="img" aria-label="{label}: {value}">
<linearGradient id="smooth" x2="0" y2="100%">
  <stop offset="0" stop-color="#fff" stop-opacity=".7"/>
  <stop offset=".1" stop-color="#aaa" stop-opacity=".1"/>
  <stop offset=".9" stop-color="#000" stop-opacity=".3"/>
  <stop offset="1" stop-color="#000" stop-opacity=".5"/>
</linearGradient>
<clipPath id="round">
  <rect width="{total_width}" height="20" rx="3" fill="#fff"/>
</clipPath>
<g clip-path="url(#round)">
  <rect width="{label_width}" height="20" fill="#555"/>
  <rect x="{label_width}" width="{value_width}" height="20" fill="{color}"/>
  <rect width="{total_width}" height="20" fill="url(#smooth)"/>
</g>
<g fill="#fff" text-anchor="middle" font-family="Verdana,Geneva,DejaVu Sans,sans-serif" font-size="11">
  <text x="{label_center}" y="15" fill="#010101" fill-opacity=".3">{label}</text>
  <text x="{label_center}" y="14">{label}</text>
  <text x="{value_center}" y="15" fill="#010101" fill-opacity=".3">{value}</text>
  <text x="{value_center}" y="14">{value}</text>
</g>
</svg>
"""


def write_badge(name: str, label: str, coverage: float) -> None:
    BADGE_DIR.mkdir(parents=True, exist_ok=True)
    value = f"{coverage:.1f}%"
    badge = render_badge(label, value, badge_color(coverage))
    (BADGE_DIR / name).write_text(badge, encoding="utf-8")


def main() -> None:
    write_badge("backend-coverage.svg", "backend coverage", backend_coverage())
    write_badge("ai-coverage.svg", "ai coverage", ai_coverage())


if __name__ == "__main__":
    main()
