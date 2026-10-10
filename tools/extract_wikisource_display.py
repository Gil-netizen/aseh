#!/usr/bin/env python3
"""Convert a pinned MediaWiki rendered-HTML snapshot to stable display text."""

from __future__ import annotations

import argparse
import html
import re
from html.parser import HTMLParser
from pathlib import Path


BLOCK_TAGS = {
    "blockquote",
    "br",
    "div",
    "h1",
    "h2",
    "h3",
    "h4",
    "li",
    "p",
    "tr",
}


class DisplayTextParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self.parts: list[str] = []
        self._skip_depth = 0

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        classes = (dict(attrs).get("class") or "").split()
        if tag in {"script", "style", "sup"} or "mw-editsection" in classes or "noprint" in classes:
            self._skip_depth += 1
            return
        if not self._skip_depth and tag in BLOCK_TAGS:
            self.parts.append("\n")

    def handle_endtag(self, tag: str) -> None:
        if self._skip_depth:
            if tag in {"script", "style", "sup"}:
                self._skip_depth = max(0, self._skip_depth - 1)
            return
        if tag in BLOCK_TAGS:
            self.parts.append("\n")

    def handle_data(self, data: str) -> None:
        if not self._skip_depth:
            self.parts.append(data)


def extract_display_text(source: str) -> str:
    parser = DisplayTextParser()
    parser.feed(source)
    raw = html.unescape("".join(parser.parts)).replace("\N{NO-BREAK SPACE}", " ")
    lines: list[str] = []
    for raw_line in raw.splitlines():
        line = re.sub(r"[ \t]+", " ", raw_line).strip()
        if line and line not in {"עריכה", "מקור"}:
            lines.append(line)
    return "\n\n".join(lines) + "\n"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("input", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    source = args.input.read_text(encoding="utf-8")
    args.output.write_text(extract_display_text(source), encoding="utf-8", newline="\n")


if __name__ == "__main__":
    main()
