# -*- coding: utf-8 -*-
import os

def check_chunks():
    base_dir = r"d:\work\ide\WorkSpace\vls-open-source\vls-server\Antigravity"
    for i in range(10):
        filename = f"dict_chunk_{i}.txt"
        filepath = os.path.join(base_dir, filename)
        if not os.path.exists(filepath):
            print(f"{filename}: DOES NOT EXIST")
            continue
            
        with open(filepath, 'r', encoding='utf-8') as f:
            lines = f.readlines()
            
        total = len(lines)
        translated = 0
        untranslated = 0
        for line in lines:
            parts = line.strip().split(" ||| ")
            if len(parts) >= 3 and parts[2].strip():
                translated += 1
            else:
                untranslated += 1
                
        print(f"{filename}: Total={total}, Translated={translated}, Untranslated={untranslated}")

if __name__ == "__main__":
    check_chunks()
