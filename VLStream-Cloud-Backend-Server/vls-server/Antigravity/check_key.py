# -*- coding: utf-8 -*-
import base64

b64 = "IOajgOa1i+WIsCA="
decoded = base64.b64decode(b64).decode('utf-8')
print("Decoded:")
print(f"repr: {repr(decoded)}")
print(f"bytes: {list(decoded.encode('utf-8'))}")

# Compare with some candidates
candidate1 = " 检测到 "
print(f"Candidate 1 repr: {repr(candidate1)}")
print(f"Candidate 1 bytes: {list(candidate1.encode('utf-8'))}")
print(f"Equal? {decoded == candidate1}")

candidate2 = " 检测到  "
print(f"Candidate 2 repr: {repr(candidate2)}")
print(f"Candidate 2 bytes: {list(candidate2.encode('utf-8'))}")
print(f"Equal? {decoded == candidate2}")
