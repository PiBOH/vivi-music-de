#!/usr/bin/env python3
"""Strips a reporter's own words out of a release body.

A release body is the changelog section for one version, published on GitHub the
moment the tag is created: a changelog entry that quotes the owner's message ends
up readable by everyone, and six already-published releases (6.0.6.8, 6.0.8.6,
6.0.8.7, 6.0.8.8, 6.0.8.9 x2) had to be rewritten by hand for exactly that
reason. This runs on the extracted section in `auto-release.yml`, so the next
entry that carries a quotation cannot publish it.

What counts as a quotation: a double-quoted span that reads Italian — it carries
an Italian diacritic (a, e, i, o, u with the accent) or at least two of the
Italian function words that never appear in an English sentence by accident. An
English quotation (`the report "it goes wrong from the start"`) is left alone, and
so is a quoted UI label that happens to be Italian (`"Passa automaticamente alla
canzone successiva in caso di errore"`).

Trade-off, deliberate: a *legitimate* Italian sentence with an accent inside
double quotes is dropped too. A release body is English (AGENTS.md), so the only
Italian quotations it can carry are the owner's, and losing one description is
cheaper than publishing his words.

Usage: `python3 scripts/clean_release_notes.py < notes.md > cleaned.md`
"""

import re
import sys

ITALIAN_DIACRITIC = re.compile(r"[àèéìòùÀÈÉÌÒÙ]")

# Function words that mark a sentence as Italian. Deliberately excludes "di",
# "con", "per", "la", "il", "in": they are single letters away from English
# words, and the accented-vowel rule already catches most prose (a citation
# without any accent still carries two or more of these).
ITALIAN_WORDS = re.compile(
    r"\b("
    r"che|non|sono|anche|perché|perche|quando|deve|devo|mio|mia|gli|così|cosi|"
    r"abbiamo|adesso|allora|questo|questa|quello|quella|sulla|nella|dalla|alla|"
    r"dello|più|piu|ho|ecco|però|pero|quindi|niente|tutto|tutti|solo|poi|"
    r"viene|vanno|fatto|fare|essere|stato|fosse|perchè"
    r")\b",
    re.IGNORECASE,
)

QUOTED = re.compile(r'"([^"\n]{3,900})"')


def is_reporter_quote(inner: str) -> bool:
    return bool(ITALIAN_DIACRITIC.search(inner)) or len(ITALIAN_WORDS.findall(inner)) >= 2


def clean(text: str) -> str:
    def drop(match: re.Match) -> str:
        return "" if is_reporter_quote(match.group(1)) else match.group(0)

    cleaned = QUOTED.sub(drop, text)
    # A dropped quote leaves the sentence it was embedded in with two spaces or a
    # dangling " - - ": tidy exactly that, never the text around it.
    cleaned = re.sub(r"[ \t]{2,}", " ", cleaned)
    cleaned = re.sub(r"\s+-\s+-{1,2}\s+", " - ", cleaned)
    cleaned = re.sub(r"\s+([.,;:])", r"\1", cleaned)
    return cleaned


def main() -> int:
    sys.stdout.write(clean(sys.stdin.read()))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
