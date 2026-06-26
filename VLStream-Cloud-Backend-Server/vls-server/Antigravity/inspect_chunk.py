# -*- coding: utf-8 -*-
import os

chunk_path = r"d:\work\ide\WorkSpace\vls-open-source\vls-server\Antigravity\dict_chunk_0.txt"

with open(chunk_path, "r", encoding="utf-8") as f:
    lines = f.readlines()

print(f"Total lines read: {len(lines)}")
for idx in [0, 1, 32, 33, 400]:
    if idx < len(lines):
        print(f"Line {idx+1}: {repr(lines[idx])}")
