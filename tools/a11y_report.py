#!/usr/bin/env python3
"""Turns the accessibility scan results of the instrumented tests into a report.

The tests (testutils/A11yScanner.kt) write findings.jsonl and a screenshot per scanned screen to
/sdcard/test-a11y on the device. Pull them, then run this:

    adb shell rm -rf /sdcard/test-a11y          # before the run - findings are appended
    ./gradlew :app:connectedDebugAndroidTest ...
    adb pull /sdcard/test-a11y build/a11y
    python3 tools/a11y_report.py build/a11y

Writes build/a11y/report.html and prints a summary. Standard library only.
"""
import html
import json
import re
import struct
import sys
from collections import Counter, defaultdict
from pathlib import Path

SEVERITY_ORDER = {"ERROR": 0, "WARNING": 1, "INFO": 2}


def load(findings_file: Path):
    findings, scan_errors, seen = [], [], set()
    for line in findings_file.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        f = json.loads(line)
        if "scanError" in f:
            scan_errors.append(f)
            continue
        # the same screen may be scanned more than once - count each problem once
        key = (f["screen"], f["check"], f.get("resourceId"), f.get("text"), f.get("contentDescription"), f.get("bounds"))
        if key in seen:
            continue
        seen.add(key)
        findings.append(f)
    return findings, scan_errors


def is_guideline(f):
    return f["wcag"].startswith("Guideline")


def element_label(f):
    parts = [f.get("class", "").split(".")[-1]]
    if f.get("resourceId"):
        parts.append(f["resourceId"].split("/")[-1])
    label = f.get("text") or f.get("contentDescription")
    if label:
        parts.append(f'"{label[:60]}"')
    return " ".join(p for p in parts if p)


def summary(findings, scan_errors):
    wcag = [f for f in findings if not is_guideline(f)]
    lines = [
        f"{len(findings)} findings on {len({f['screen'] for f in findings})} screens "
        f"({len(wcag)} WCAG 2.1, {len(findings) - len(wcag)} guideline-only)",
        "",
        "By WCAG criterion (errors / warnings / info):",
    ]
    by_criterion = defaultdict(Counter)
    for f in findings:
        by_criterion[f["wcag"]][f["severity"]] += 1
    for criterion in sorted(by_criterion):
        c = by_criterion[criterion]
        lines.append(f"  {c['ERROR']:4} / {c['WARNING']:4} / {c['INFO']:4}  {criterion}")
    lines += ["", "By screen (errors + warnings):"]
    by_screen = Counter(f["screen"] for f in findings if f["severity"] != "INFO")
    for screen, n in by_screen.most_common():
        lines.append(f"  {n:4}  {screen}")
    if scan_errors:
        lines += ["", f"{len(scan_errors)} scan(s) could not run:"]
        lines += [f"  {e['screen']}: {e['scanError']}" for e in scan_errors]
    return "\n".join(lines)


def png_size(path: Path):
    """(width, height) from the PNG header, or None."""
    try:
        with open(path, "rb") as f:
            head = f.read(24)
        if head[:8] == b"\x89PNG\r\n\x1a\n":
            return struct.unpack(">II", head[16:24])
    except OSError:
        pass
    return None


THUMB_WIDTH = 150


def highlight(f, shot: Path, size):
    """The screenshot, small, with the finding's element boxed - so an element without an id or
    label can still be told apart."""
    if not size or not f.get("bounds"):
        return ""
    try:
        left, top, right, bottom = (int(v) for v in f["bounds"].split(","))
    except ValueError:
        return ""
    scale = THUMB_WIDTH / size[0]
    box = (f"left:{left * scale:.0f}px;top:{top * scale:.0f}px;"
           f"width:{max(2, (right - left) * scale):.0f}px;height:{max(2, (bottom - top) * scale):.0f}px")
    return (f"<div class='shot' style='width:{THUMB_WIDTH}px;height:{size[1] * scale:.0f}px'>"
            f"<img src='screens/{html.escape(shot.name)}' alt=''><div class='box' style='{box}'></div></div>")


def report_html(findings, scan_errors, screens_dir: Path):
    esc = html.escape
    out = [
        "<!doctype html><meta charset='utf-8'><title>Accessibility report</title>",
        "<style>body{font:14px system-ui,sans-serif;margin:24px;max-width:1200px}"
        "table{border-collapse:collapse;width:100%;margin:8px 0 24px}"
        "td,th{border:1px solid #ccc;padding:4px 6px;text-align:left;vertical-align:top}"
        ".ERROR{color:#b00020;font-weight:600}.WARNING{color:#8a5a00}.INFO{color:#555}"
        "section>img{max-width:220px;border:1px solid #ccc;float:right;margin-left:16px}"
        ".shot{position:relative;border:1px solid #ccc}.shot img{width:100%;display:block}"
        ".box{position:absolute;border:2px solid #e00;box-shadow:0 0 0 2px #fff}"
        "section{clear:both;border-top:2px solid #333;padding-top:8px}</style>",
        "<h1>Accessibility report (WCAG 2.1 AA)</h1>",
        f"<pre>{esc(summary(findings, scan_errors))}</pre>",
    ]
    by_screen = defaultdict(list)
    for f in findings:
        by_screen[f["screen"]].append(f)
    for screen in sorted(by_screen):
        items = sorted(by_screen[screen], key=lambda f: (is_guideline(f), SEVERITY_ORDER.get(f["severity"], 9), f["wcag"]))
        # same file name A11yScanner.saveScreenshot uses: each run of other characters -> "_"
        shot = screens_dir / (re.sub(r"[^A-Za-z0-9_.-]+", "_", screen) + ".png")
        out.append(f"<section><h2>{esc(screen)}</h2>")
        if shot.exists():
            out.append(f"<img src='screens/{esc(shot.name)}' alt='Screenshot of {esc(screen)}'>")
        size = png_size(shot) if shot.exists() else None
        out.append("<table><tr><th>Severity</th><th>WCAG</th><th>Element</th><th>Where</th><th>Problem</th></tr>")
        for f in items:
            out.append(
                f"<tr><td class='{f['severity']}'>{f['severity']}</td><td>{esc(f['wcag'])}</td>"
                f"<td>{esc(element_label(f))}<br><small>{esc(f.get('bounds', ''))}</small></td>"
                f"<td>{highlight(f, shot, size)}</td>"
                f"<td>{esc(f['message'])}</td></tr>"
            )
        out.append("</table></section>")
    return "\n".join(out)


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    folder = Path(sys.argv[1])
    findings, scan_errors = load(folder / "findings.jsonl")
    (folder / "report.html").write_text(report_html(findings, scan_errors, folder / "screens"), encoding="utf-8")
    print(summary(findings, scan_errors))
    print(f"\nReport: {folder / 'report.html'}")


if __name__ == "__main__":
    main()
