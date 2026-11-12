#!/usr/bin/env python3
"""Basic style checks for ERRSA-MC-Core's editable Java sources.

Usage: python tools/check_java_style.py
This complements, rather than replaces, compilation and automated formatting.
"""
from pathlib import Path
import sys

PROJECT = Path(__file__).resolve().parents[1]
SOURCES = sorted((PROJECT / 'src' / 'main' / 'java').rglob('*.java'))
SOURCES += sorted((PROJECT / 'tools').rglob('*.java'))
errors = []
for path in SOURCES:
    content = path.read_text(encoding='utf-8')
    lines = content.splitlines()
    for index, line in enumerate(lines, start=1):
        if len(line) > 120:
            errors.append(f'{path.relative_to(PROJECT)}:{index}: line longer than 120 characters')
        if '\t' in line:
            errors.append(f'{path.relative_to(PROJECT)}:{index}: tab character')
        if line.rstrip() != line:
            errors.append(f'{path.relative_to(PROJECT)}:{index}: trailing whitespace')
    if not content.endswith('\n'):
        errors.append(f'{path.relative_to(PROJECT)}: missing final newline')
    if '/src/main/java/' in path.as_posix() and not any(line.startswith('package ') for line in lines[:12]):
        errors.append(f'{path.relative_to(PROJECT)}: missing package statement')
print(f'Checked {len(SOURCES)} Java files.')
for error in errors:
    print('FAIL', error)
if errors:
    print(f'{len(errors)} style violations found.')
    sys.exit(1)
print('PASS: line length, spaces, trailing whitespace, package declarations and EOF newlines.')
