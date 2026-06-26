# -*- coding: utf-8 -*-
import sys
sys.path.append(r"d:\work\ide\WorkSpace\vls-open-source\vls-server\Antigravity")
from translate_chunk import translation_map
import base64

b64 = "IOajgOa1i+WIsCA="
decoded = base64.b64decode(b64).decode('utf-8')

print(f"Decoded: {repr(decoded)} (bytes: {list(decoded.encode('utf-8'))})")
for key in translation_map:
    if "检测到" in key:
        print(f"Key in map: {repr(key)} (bytes: {list(key.encode('utf-8'))})")
        print(f"Equal? {key == decoded}")
