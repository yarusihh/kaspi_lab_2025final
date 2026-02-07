#!/usr/bin/env python3
# dump_tree_java_locked.py
# HARD-LOCK: never goes above starting directory. Never escapes BASE.
# Does NOT follow symlinks/junctions. Dumps .java content only.

import os

BASE = os.path.realpath(os.getcwd())

SKIP_DIRS = {
    ".git",
    "node_modules",
    "target",
    "__pycache__",
    ".idea",
    ".vscode",
    "build",
    "dist",
    ".gradle",
}

MAX_JAVA_SIZE = 1 * 1024 * 1024  # 1MB


def inside_base(path: str) -> bool:
    try:
        return os.path.commonpath([BASE, os.path.realpath(path)]) == BASE
    except Exception:
        return False


def is_binary(path: str) -> bool:
    try:
        with open(path, "rb") as f:
            return b"\x00" in f.read(1024)
    except Exception:
        return True


def dump_java(path: str, indent: int):
    try:
        if os.path.getsize(path) > MAX_JAVA_SIZE:
            print(" " * indent + "[SKIP: too large]")
            return
    except Exception:
        return

    if is_binary(path):
        print(" " * indent + "[SKIP: binary]")
        return

    print(" " * indent + "----- JAVA BEGIN -----")
    try:
        with open(path, "r", encoding="utf-8", errors="replace") as f:
            for line in f:
                print(" " * indent + line.rstrip())
    except Exception as e:
        print(" " * indent + f"[READ ERROR: {e}]")
    print(" " * indent + "------ JAVA END ------\n")


def walk(path: str, indent: int = 0):
    if not inside_base(path):
        return

    try:
        with os.scandir(path) as it:
            entries = sorted(it, key=lambda e: (not e.is_dir(follow_symlinks=False), e.name.lower()))
    except Exception:
        return

    for e in entries:
        full = os.path.join(path, e.name)

        if not inside_base(full):
            continue

        # ---- DIR ----
        if e.is_dir(follow_symlinks=False):
            if e.name in SKIP_DIRS:
                continue
            print(" " * indent + f"[DIR] {e.name}/")
            walk(full, indent + 2)
            continue

        # ---- FILE ----
        if e.is_file(follow_symlinks=False):
            print(" " * indent + f"[FILE] {e.name}")

            if e.name.lower().endswith(".java"):
                dump_java(full, indent + 2)


if __name__ == "__main__":
    print("LOCKED ROOT:", BASE, "\n")
    walk(BASE)
