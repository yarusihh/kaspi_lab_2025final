#!/usr/bin/env python3
import sys
import json
import urllib.parse
from typing import Any, Dict, List

import requests


BASE_URL = "http://127.0.0.1:8080"
TIMEOUT = 5


class ApiError(Exception):
    pass


def _get(path: str) -> Dict[str, Any]:
    url = f"{BASE_URL}{path}"
    try:
        r = requests.get(url, timeout=TIMEOUT)
    except requests.RequestException as e:
        raise ApiError(f"Request failed: {e}") from e

    if r.status_code != 200:
        raise ApiError(f"HTTP {r.status_code}: {r.text[:500]}")

    try:
        return r.json()
    except ValueError as e:
        raise ApiError("Invalid JSON response") from e


def _validate_db_files(data: Dict[str, Any]) -> List[Dict[str, Any]]:
    if not data.get("success", False):
        raise ApiError(f"API error: {data.get('msg')}")

    files = data.get("files")
    if not isinstance(files, list):
        raise ApiError("Invalid 'files' format")

    required = {"id", "uuid", "uploadDate", "name", "size", "hash", "url"}
    for f in files:
        if not required.issubset(f.keys()):
            raise ApiError(f"Invalid file object: {f}")

    return files


def _validate_s3_files(data: Dict[str, Any]) -> List[Dict[str, Any]]:
    if not data.get("success", False):
        raise ApiError(f"API error: {data.get('msg')}")

    files = data.get("files")
    if not isinstance(files, list):
        raise ApiError("Invalid 'files' format")

    required = {"name", "url"}
    for f in files:
        if not required.issubset(f.keys()):
            raise ApiError(f"Invalid file object: {f}")

    return files


def test_db_files() -> None:
    print("== Testing /api/v1/db/files ==")
    data = _get("/api/v1/db/files")
    files = _validate_db_files(data)

    print(f"OK. Files count: {len(files)}")
    if files:
        print("Sample:")
        print(json.dumps(files[0], indent=2, ensure_ascii=False))


def test_s3_files() -> None:
    print("== Testing /api/v1/storage/files ==")
    data = _get("/api/v1/storage/files")
    files = _validate_s3_files(data)

    print(f"OK. Files count: {len(files)}")

    if files:
        f = files[0]
        print("Sample:")
        print(json.dumps(f, indent=2, ensure_ascii=False))

        # Проверка URL encoding
        encoded_name = urllib.parse.quote(f["name"])
        if encoded_name not in f["url"]:
            print("WARN: URL may be not encoded correctly")
        else:
            print("URL encoding looks correct")


def main() -> int:
    try:
        test_db_files()
        print()
        test_s3_files()
        return 0
    except ApiError as e:
        print(f"ERROR: {e}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
