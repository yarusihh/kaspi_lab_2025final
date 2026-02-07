import sys
import uuid
import json
import time
import threading
from typing import Optional, List

from confluent_kafka import Producer, Consumer, KafkaException


# ==========================================================
# CONFIG
# ==========================================================

BOOTSTRAP_SERVERS = "188.245.102.196:9092"
TOPIC = "Topic-1"
GROUP_ID = "kafka-smoke-test"

SINGLE_TEST_TIMEOUT = 15
BATCH_SIZE = 200
PARALLEL_THREADS = 4
PARALLEL_MESSAGES = 500


# ==========================================================
# PRODUCER
# ==========================================================

def create_producer() -> Producer:
    return Producer({
        "bootstrap.servers": BOOTSTRAP_SERVERS,
        "acks": "all",
        "enable.idempotence": True,
        "compression.type": "snappy",
        "linger.ms": 5,
        "batch.size": 65536,
        "retries": 1000000,
    })


# ==========================================================
# CONSUMER
# ==========================================================

def create_consumer() -> Consumer:
    return Consumer({
        "bootstrap.servers": BOOTSTRAP_SERVERS,
        "group.id": GROUP_ID,
        "enable.auto.commit": False,
        "auto.offset.reset": "earliest",
        "session.timeout.ms": 45000,
        "max.poll.interval.ms": 300000,
    })


# ==========================================================
# HELPERS
# ==========================================================

def make_event(event_id: str) -> bytes:
    payload = {
        "eventId": event_id,
        "clientId": "kafka-test",
        "category": "test",
        "createdAt": int(time.time()),
        "payload": {"text": "Kafka load test"},
    }
    return json.dumps(payload).encode()


# ==========================================================
# TEST 1 — SINGLE MESSAGE
# ==========================================================

def test_single_message():
    print("\n[TEST] Single produce/consume")

    producer = create_producer()
    consumer = create_consumer()

    event_id = str(uuid.uuid4())

    delivered = {"ok": False}

    def cb(err, msg):
        if err:
            raise RuntimeError(err)
        delivered["ok"] = True

    producer.produce(TOPIC, key=event_id.encode(), value=make_event(event_id), on_delivery=cb)
    producer.flush()

    if not delivered["ok"]:
        raise RuntimeError("Produce failed")

    consumer.subscribe([TOPIC])

    deadline = time.time() + SINGLE_TEST_TIMEOUT

    while time.time() < deadline:
        msg = consumer.poll(1)
        if msg is None:
            continue
        if msg.error():
            raise KafkaException(msg.error())

        data = json.loads(msg.value().decode())

        if data["eventId"] == event_id:
            consumer.commit(message=msg)
            consumer.close()
            print("✔ OK")
            return

    raise RuntimeError("Consume timeout")


# ==========================================================
# TEST 2 — BATCH PRODUCE
# ==========================================================

def test_batch_produce():
    print("\n[TEST] Batch produce:", BATCH_SIZE)

    producer = create_producer()

    start = time.time()

    for _ in range(BATCH_SIZE):
        eid = str(uuid.uuid4())
        producer.produce(TOPIC, value=make_event(eid))

    producer.flush()

    duration = time.time() - start
    print(f"✔ Sent {BATCH_SIZE} msgs in {duration:.2f}s ({BATCH_SIZE/duration:.0f} msg/sec)")


# ==========================================================
# TEST 3 — PARALLEL PRODUCE
# ==========================================================

def worker(count: int):
    p = create_producer()
    for _ in range(count):
        eid = str(uuid.uuid4())
        p.produce(TOPIC, value=make_event(eid))
    p.flush()


def test_parallel_produce():
    print(f"\n[TEST] Parallel produce: {PARALLEL_THREADS} threads × {PARALLEL_MESSAGES}")

    start = time.time()

    threads = []
    for _ in range(PARALLEL_THREADS):
        t = threading.Thread(target=worker, args=(PARALLEL_MESSAGES,))
        t.start()
        threads.append(t)

    for t in threads:
        t.join()

    duration = time.time() - start
    total = PARALLEL_THREADS * PARALLEL_MESSAGES

    print(f"✔ Sent {total} msgs in {duration:.2f}s ({total/duration:.0f} msg/sec)")


# ==========================================================
# TEST 4 — CONSUMER LAG
# ==========================================================

def test_consumer_lag():
    print("\n[TEST] Consumer lag scan")

    consumer = create_consumer()
    consumer.subscribe([TOPIC])

    count = 0
    start = time.time()

    while time.time() - start < 5:
        msg = consumer.poll(0.2)
        if msg and not msg.error():
            count += 1

    consumer.close()

    print("✔ Polled messages:", count)


# ==========================================================
# TEST 5 — JSON INTEGRITY
# ==========================================================

def test_json_integrity():
    print("\n[TEST] JSON integrity")

    producer = create_producer()
    consumer = create_consumer()

    event_id = str(uuid.uuid4())

    producer.produce(TOPIC, value=make_event(event_id))
    producer.flush()

    consumer.subscribe([TOPIC])
    deadline = time.time() + 10

    while time.time() < deadline:
        msg = consumer.poll(1)
        if msg and not msg.error():
            data = json.loads(msg.value().decode())
            if data["eventId"] == event_id:
                assert isinstance(data["payload"]["text"], str)
                consumer.close()
                print("✔ JSON OK")
                return

    raise RuntimeError("JSON broken")


# ==========================================================
# MAIN
# ==========================================================

def main():
    print("\nKafka FULL TEST")
    print("Broker:", BOOTSTRAP_SERVERS)
    print("=" * 50)

    try:
        test_single_message()
        test_batch_produce()
        test_parallel_produce()
        test_consumer_lag()
        test_json_integrity()

        print("\n✔ ALL TESTS PASSED")
        sys.exit(0)

    except Exception as e:
        print("\n✖ TEST FAILED:", e)
        sys.exit(1)


if __name__ == "__main__":
    main()
