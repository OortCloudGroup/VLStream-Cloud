# -*- coding: utf-8 -*-
import base64

try:
    key1 = "KiBDb3B5cmlnaHQgKGMpIDIwMTgtMjA5OSwgQ2hpbGwgWmh1YW5nIOW6hOmqniAoYmxhZGVqYXZhQHFxLmNvbSkuDQogKiA8cD4NCiAqIExpY2Vuc2VkIHVuZGVyIHRoZSBHTlUgTEVTU0VSIEdFTkVSQUwgUFVCTElDIExJQ0VOU0UgMy4wOw0KICogeW91IG1heSBub3QgdXNlIHRoaXMgZmlsZSBleGNlcHQgaW4gY29tcGxpYW5jZSB3aXRoIHRoZSBMaWNlbnNlLg0KICogWW91IG1heSBvYnRhaW4gYSBjb3B5IG9mIHRoZSBMaWNlbnNlIGF0DQogKiA8cD4NCiAqIGh0dHA6Ly93d3cuZ251Lm9yZy9saWNlbnNlcy9sZ3BsLmh0bWwNCiAqIDxwPg0KICogVW5sZXNzIHJlcXVpcmVkIGJ5IGFwcGxpY2FibGUgbGF3IG9yIGFncmVlZCB0byBpbiB3cml0aW5nLCBzb2Z0d2FyZQ0KICogZGlzdHJpYnV0ZWQgdW5kZXIgdGhlIExpY2Vuc2UgaXMgZGlzdHJpYnV0ZWQgb24gYW4gIkFTIElTIiBCQVNJUywNCiAqIFdJVEhPVVQgV0FSUkFOVElFUyBPUiBDT05ESVRJT05TIE9GIEFOWSBLSU5ELCBlaXRoZXIgZXhwcmVzcyBvciBpbXBsaWVkLg0KICogU2VlIHRoZSBMaWNlbnNlIGZvciB0aGUgc3BlY2lmaWMgbGFuZ3VhZ2UgZ292ZXJuaW5nIHBlcm1pc3Npb25zIGFuZA0KICogbGltaXRhdGlvbnMgdW5kZXIgdGhlIExpY2Vuc2UuDQogKg=="
    print("Decoding key1 directly...")
    print(base64.b64decode(key1).decode('utf-8'))
except Exception as e:
    print(f"Direct decode failed: {e}")
    
try:
    print("Decoding key1 with whitespace removed...")
    cleaned_key = "".join(key1.split())
    print(base64.b64decode(cleaned_key).decode('utf-8'))
except Exception as e:
    print(f"Cleaned decode failed: {e}")
