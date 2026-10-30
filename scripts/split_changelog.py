#!/usr/bin/env python3
"""CHANGELOG.dev.md -> CHANGELOG.md.

CHANGELOG.dev.md is the record: every entry with the whole story, no word
limit. CHANGELOG.md is what a reader and the release pipeline get: one line per
change, the entry's own head and nothing else.

Rules, per line of the dev file:

  * a heading is copied as it is (a `### ... Commits` subsection is dropped
    with its list, because a commit list is history, not a change);
  * a blank line is copied as it is;
  * a note at column 0 (`> ...` or a `**bold**` paragraph) is copied as it is;
  * a bullet carrying a tag (`- [DE] ...`) or a bold head is reduced to its
    head: the bold span that starts it, or its first sentence when it has no
    bold span;
  * any other bullet (`- v: DE ...`, a sub-item) is dropped;
  * a continuation line (indented) belongs to the entry above it and is
    dropped.

Written for the project's CRLF working copy.
"""
import re

SRC = "CHANGELOG.dev.md"
DST = "CHANGELOG.md"

TAGGED = re.compile(r"^- \[([A-Z\]\[]+)\](?: \[([A-Z]+)\])?\s*(.*)$")
ANY_BULLET = re.compile(r"^- \s*(.*)$")
COMMITS = re.compile(r"^###\s.*Commits\s*$", re.IGNORECASE)


def _tidy(inner):
    """One sentence: a long head stops at its first clause, and ends with a stop."""
    inner = inner.strip()
    if len(inner.split()) > 22:
        best = None
        for pat in (r"[.!?](\s|$)", r":\s", r"\s[—–]\s", r"\s-\s", r",\s", r";\s"):
            for m in re.finditer(pat, inner):
                head = inner[: m.end()]
                if len(head.split()) >= 6 and (best is None or len(head) < len(best)):
                    best = head
                break
        if best:
            inner = best.strip().rstrip(",;:-").strip()
    if not inner.endswith((".", "!", "?", ":")):
        inner += "."
    return inner


def head_of(rest):
    """The change itself: the bold head, or the first sentence without one."""
    r = rest.strip()
    if r.startswith("**"):
        end = r.find("**", 2)
        if end > 0:
            return "**" + _tidy(r[2:end]) + "**"
    m = re.match(r"(.{10,}?[.!?])(\s|$)", r)
    if m:
        return _tidy(m.group(1))
    return _tidy(r)


def main():
    src = open(SRC, "rb").read().decode("utf-8").replace("\r\n", "\n").split("\n")
    out = []
    stats = {"heads": 0, "sentences": 0, "dropped_bullets": 0, "continuations": 0, "notes": 0}
    skip_section = False
    for line in src:
        if not line.strip():
            out.append("")
            continue
        if line.startswith("#"):
            skip_section = bool(COMMITS.match(line))
            if not skip_section:
                out.append(line)
            continue
        if skip_section:
            continue
        if line.startswith(("  ", "\t")):
            stats["continuations"] += 1
            continue
        m = TAGGED.match(line)
        if m:
            tag, tag2, rest = m.group(1), m.group(2), m.group(3)
            prefix = f"[{tag}]" + (f" [{tag2}]" if tag2 else "")
            head = head_of(rest)
            stats["heads" if head.startswith("**") else "sentences"] += 1
            out.append(f"- {prefix} {head}".rstrip())
            continue
        m = ANY_BULLET.match(line)
        if m:
            rest = m.group(1).strip()
            if rest.startswith("**"):
                head = head_of(rest)
                stats["heads"] += 1
                out.append(f"- {head}".rstrip())
            else:
                stats["dropped_bullets"] += 1
            continue
        # a note at column 0
        if line.startswith(">") or line.startswith("**"):
            stats["notes"] += 1
            out.append(line)
            continue
        stats["continuations"] += 1

    clean = []
    for line in out:
        if line == "" and clean and clean[-1] == "":
            continue
        clean.append(line)
    while clean and clean[-1] == "":
        clean.pop()
    open(DST, "wb").write(("\r\n".join(clean) + "\r\n").encode("utf-8"))
    print(stats)
    print(f"CHANGELOG.md: {len(clean)} righe, {sum(1 for l in clean if l.startswith('- '))} punti")


if __name__ == "__main__":
    main()
