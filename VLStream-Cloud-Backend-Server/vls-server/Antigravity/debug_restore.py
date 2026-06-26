# -*- coding: utf-8 -*-
import json
import re

transcript_path = r"C:\Users\oort\.gemini\antigravity\brain\518eb702-4d23-4229-b31f-bf038da1c681\.system_generated\logs\transcript_full.jsonl"

original_content = None

with open(transcript_path, "r", encoding="utf-8") as f:
    for line in f:
        step = json.loads(line)
        if step.get("step_index") == 3 and step.get("type") == "VIEW_FILE":
            original_content = step.get("content")
            break

header_marker = "Please note that any changes targeting the original code should remove the line number, colon, and leading space.\n"
header_idx = original_content.find(header_marker)
if header_idx != -1:
    original_content = original_content[header_idx + len(header_marker):]

suffix_marker = "\nThe above content shows the entire, complete file contents of the requested file."
suffix_idx = original_content.find(suffix_marker)
if suffix_idx != -1:
    original_content = original_content[:suffix_idx]

lines = original_content.split('\n')
print(f"Total split lines: {len(lines)}")
for i in range(10):
    print(f"split line {i+1}: {repr(lines[i])}")

cleaned_lines = []
for line in lines:
    m = re.match(r'^(\d+): (.*)', line)
    if m:
        cleaned_lines.append(m.group(2))
    else:
        if cleaned_lines:
            cleaned_lines[-1] += '\n' + line

print(f"Total cleaned_lines: {len(cleaned_lines)}")
for i in range(5):
    print(f"cleaned line {i+1}: {repr(cleaned_lines[i])}")
