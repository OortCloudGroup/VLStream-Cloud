# -*- coding: utf-8 -*-
import json
import os

transcript_path = r"C:\Users\oort\.gemini\antigravity\brain\518eb702-4d23-4229-b31f-bf038da1c681\.system_generated\logs\transcript_full.jsonl"
chunk_path = r"d:\work\ide\WorkSpace\vls-open-source\vls-server\Antigravity\dict_chunk_0.txt"

original_content = None

with open(transcript_path, "r", encoding="utf-8") as f:
    for line in f:
        step = json.loads(line)
        if step.get("step_index") == 3 and step.get("type") == "VIEW_FILE":
            original_content = step.get("content")
            break

if original_content is None:
    raise ValueError("Could not find step_index 3 of type VIEW_FILE in transcript")

# Strip the prefix header added by view_file:
# "Created At: ...\nCompleted At: ...\nFile Path: ...\nTotal Lines: ...\nTotal Bytes: ...\nShowing lines ...\nThe following code has been modified to include a line number before every line, in the format: <line_number>: <original_line>. Please note that any changes targeting the original code should remove the line number, colon, and leading space.\n"
# And strip the suffix:
# "\nThe above content shows the entire, complete file contents of the requested file." (if present)

header_marker = "Please note that any changes targeting the original code should remove the line number, colon, and leading space.\n"
header_idx = original_content.find(header_marker)
if header_idx != -1:
    original_content = original_content[header_idx + len(header_marker):]

suffix_marker = "\nThe above content shows the entire, complete file contents of the requested file."
suffix_idx = original_content.find(suffix_marker)
if suffix_idx != -1:
    original_content = original_content[:suffix_idx]

# Now, we must remove the line numbers added by view_file!
# Split by '\n' to parse line by line
lines = original_content.split('\n')
cleaned_lines = []

import re
for line in lines:
    # Check if this line starts a new file line
    m = re.match(r'^(\d+): (.*)', line)
    if m:
        # Strip trailing carriage return from the line content
        cleaned_lines.append(m.group(2).rstrip('\r'))
    else:
        # Continuation of the last line (e.g. newline inside copyright block, or empty line)
        if cleaned_lines:
            cleaned_lines[-1] += '\n' + line.rstrip('\r')
        else:
            # If at the very start of content
            pass

# Join with '\n'. On Windows, writing in 'w' mode translates '\n' to '\r\n'
restored_text = "\n".join(cleaned_lines)

with open(chunk_path, "w", encoding="utf-8") as f:
    f.write(restored_text)


print("dict_chunk_0.txt has been restored successfully!")
print(f"Total lines in restored: {len(cleaned_lines)}")

