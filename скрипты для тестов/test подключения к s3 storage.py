#!/usr/bin/env python3
# =========================================================
# MinIO PARALLEL UPLOADER + STORAGE API DEMO
# =========================================================

import os
import time
import uuid
import logging
from concurrent.futures import ThreadPoolExecutor, as_completed

import boto3
from botocore.config import Config
from botocore.exceptions import ClientError

# =============================
# DEBUG
# =============================

logging.basicConfig(level=logging.INFO)
logging.getLogger("botocore").setLevel(logging.WARNING)

# =============================
# CONFIG
# =============================

ENDPOINT = "https://minio-s3.icod.kz"
BUCKET = "kaspi-lab-public"

ACCESS_KEY = "app"
SECRET_KEY = "app12345@"

REGION = "us-east-1"

PARALLEL_UPLOADS = 5
FILE_SIZE_MB = 5
TOTAL_FILES = 5

# =============================
# S3 CLIENT
# =============================

s3 = boto3.client(
    "s3",
    endpoint_url=ENDPOINT,
    aws_access_key_id=ACCESS_KEY,
    aws_secret_access_key=SECRET_KEY,
    region_name=REGION,
    config=Config(
        signature_version="s3v4",
        s3={"addressing_style": "path"},
        max_pool_connections=64,
        retries={"max_attempts": 5},
    ),
)

# =============================
# RANDOM DATA
# =============================

def generate_random_bytes(size_mb: int) -> bytes:
    return os.urandom(size_mb * 1024 * 1024)


def random_filename() -> str:
    return f"{uuid.uuid4().hex}.bin"


# =============================
# UPLOAD
# =============================

def upload_one(index: int):
    try:
        data = generate_random_bytes(FILE_SIZE_MB)
        key = random_filename()

        start = time.time()

        response = s3.put_object(
            Bucket=BUCKET,
            Key=key,
            Body=data,
            ContentType="application/octet-stream",
        )

        duration = time.time() - start
        speed = FILE_SIZE_MB / duration if duration > 0 else 0

        meta = response.get("ResponseMetadata", {})

        print(
            "[UPLOAD]",
            "Status=", meta.get("HTTPStatusCode"),
            "Key=", key,
            "ETag=", response.get("ETag"),
        )

        return {
            "ok": True,
            "file": key,
            "time": duration,
            "speed": speed,
        }

    except ClientError as e:
        print("[S3 ERROR RAW]", e.response)
        return {"ok": False, "error": str(e)}

    except Exception as e:
        print("[ERROR RAW]", repr(e))
        return {"ok": False, "error": str(e)}


# =============================
# LIST OBJECTS (STORAGE VIEW)
# =============================

def list_objects():
    print("\n========================================")
    print("STORAGE OBJECT LIST")
    print("========================================")

    paginator = s3.get_paginator("list_objects_v2")
    page_iterator = paginator.paginate(Bucket=BUCKET)

    count = 0
    total_size = 0

    for page in page_iterator:
        contents = page.get("Contents", [])
        for obj in contents:
            key = obj["Key"]
            size = obj["Size"]
            last_modified = obj["LastModified"]

            count += 1
            total_size += size

            print(
                f"[OBJ] {key} | "
                f"{size / (1024*1024):.2f} MB | "
                f"{last_modified}"
            )

    print("----------------------------------------")
    print(f"Total objects : {count}")
    print(f"Total size    : {total_size / (1024*1024):.2f} MB")
    print("========================================")


# =============================
# OBJECT META (HEAD DEMO)
# =============================

def show_object_meta(sample_key: str):
    print("\n[HEAD OBJECT]", sample_key)

    try:
        meta = s3.head_object(Bucket=BUCKET, Key=sample_key)

        print("Size       =", meta.get("ContentLength"))
        print("ETag       =", meta.get("ETag"))
        print("Type       =", meta.get("ContentType"))
        print("Modified   =", meta.get("LastModified"))
        print("Metadata   =", meta.get("Metadata"))

    except ClientError as e:
        print("[HEAD ERROR]", e.response)


# =============================
# MAIN
# =============================

def main():
    print("========================================")
    print("MinIO PARALLEL UPLOAD + API DEMO")
    print("========================================")

    start_total = time.time()

    uploaded = 0
    failed = 0
    sample_key = None

    with ThreadPoolExecutor(max_workers=PARALLEL_UPLOADS) as executor:
        futures = [executor.submit(upload_one, i) for i in range(TOTAL_FILES)]

        for f in as_completed(futures):
            result = f.result()

            if result["ok"]:
                uploaded += 1
                sample_key = result["file"]
                print(
                    f"[OK] {result['file']} | "
                    f"{result['time']:.2f}s | "
                    f"{result['speed']:.2f} MB/s"
                )
            else:
                failed += 1
                print("[FAIL]", result["error"])

    total_time = time.time() - start_total

    print("\n========================================")
    print("UPLOAD RESULT")
    print("========================================")
    print(f"Uploaded : {uploaded}")
    print(f"Failed   : {failed}")
    print(f"Time     : {total_time:.2f}s")

    # =============================
    # STORAGE API DEMO
    # =============================

    list_objects()

    if sample_key:
        show_object_meta(sample_key)

    print("\nDone.")


if __name__ == "__main__":
    main()
