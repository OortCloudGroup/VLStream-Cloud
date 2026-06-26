# -*- coding: utf-8 -*-
import json

transcript_path = r"C:\Users\oort\.gemini\antigravity\brain\518eb702-4d23-4229-b31f-bf038da1c681\.system_generated\logs\transcript_full.jsonl"

with open(transcript_path, "r", encoding="utf-8") as f:
    for line in f:
        step = json.loads(line)
        if step.get("step_index") == 3 and step.get("type") == "VIEW_FILE":
            content = step.get("content")
            # find where line 33 is
            idx = content.find("33: KiBDb")
            if idx != -1:
                print(repr(content[idx:idx+1500]))
            else:
                print("Line 33 not found in content")
            break
