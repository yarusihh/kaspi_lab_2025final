#!/usr/bin/env python3
# =========================================================
# PARALLEL UPLOAD TEST
# 12 parallel requests:
#   - 6 share SAME payload (generated each run)
#   - 6 use UNIQUE payloads
# =========================================================

import sys
import uuid
import os
import requests
from concurrent.futures import ThreadPoolExecutor, as_completed

URL = "http://127.0.0.1:8080/api/v1/upload/"
FILE_SIZE_MB = 1
TIMEOUT = 120

TOTAL_REQUESTS = 12
SHARED_COUNT = 6
UNIQUE_COUNT = TOTAL_REQUESTS - SHARED_COUNT


def generate_payload(size_mb: int) -> bytes:
    return os.urandom(size_mb * 9000 * 9024)


def send_file(filename: str, data: bytes):
    files = {
        "file": (filename, data, "application/octet-stream")
    }

    try:
        r = requests.post(URL, files=files, timeout=TIMEOUT)

        try:
            body = r.json()
        except Exception:
            body = r.text

        return filename, r.status_code, body

    except requests.exceptions.RequestException as e:
        return filename, "CONN_ERROR", repr(e)


def main():
    print("========================================")
    print("PARALLEL UPLOAD (12 REQUESTS)")
    print("6 SAME PAYLOAD + 6 UNIQUE")
    print("========================================")

    # new shared payload every run
    shared_payload = generate_payload(FILE_SIZE_MB)

    tasks = []

    # --- 6 identical payloads ---
    for _ in range(SHARED_COUNT):
        tasks.append(
            (f"same_{uuid.uuid4().hex}.bin", shared_payload)
        )

    # --- 6 unique payloads ---
    for _ in range(UNIQUE_COUNT):
        tasks.append(
            (f"uniq_{uuid.uuid4().hex}.bin", generate_payload(FILE_SIZE_MB))
        )

    results = []

    with ThreadPoolExecutor(max_workers=TOTAL_REQUESTS) as executor:
        futures = [executor.submit(send_file, name, data) for name, data in tasks]

        for f in as_completed(futures):
            filename, status, body = f.result()

            print("\n----------------------------------------")
            print("FILE:", filename)
            print("STATUS:", status)
            print("RESPONSE:")
            print(body)

            results.append(status)

    ok = sum(1 for s in results if s == 202)

    print("\n========================================")
    print(f"ACCEPTED: {ok}/{TOTAL_REQUESTS}")

    sys.exit(0 if ok == TOTAL_REQUESTS else 1)


if __name__ == "__main__":
    main()
