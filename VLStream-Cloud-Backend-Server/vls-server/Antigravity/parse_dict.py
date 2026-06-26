# -*- coding: utf-8 -*-
import base64

dict_path = r"d:\work\ide\WorkSpace\vls-open-source\vls-server\Antigravity\chinese_dictionary.txt"

with open(dict_path, "r", encoding="utf-8") as f:
    lines = f.readlines()

print(f"Total lines: {len(lines)}")
invalid_count = 0
for idx, line in enumerate(lines, 1):
    parts = line.strip().split(" ||| ")
    if len(parts) >= 2:
        key = parts[0].strip()
        # Remove all whitespace from key to see if it becomes valid
        cleaned_key = "".join(key.split())
        try:
            base64.b64decode(cleaned_key)
        except Exception as e:
            invalid_count += 1
            print(f"Line {idx} has invalid base64 key: {repr(key)}")
            print(f"  Error: {e}")
            if invalid_count >= 10:
                print("Too many invalid keys, stopping check.")
                break

print(f"Check complete. Found {invalid_count} invalid base64 keys.")
