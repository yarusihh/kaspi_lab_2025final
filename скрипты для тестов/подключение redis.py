import time
import redis

HOST = "redis.icod.kz"
PORT = 6379
PASSWORD = "admin12345@"    

r = redis.Redis(
    host=HOST,
    port=PORT,
    password=PASSWORD,
    db=0,
    decode_responses=True,
    socket_timeout=3,
    socket_connect_timeout=3,
)

print("\nREDIS QUICK TEST")
print("=" * 40)

# ----------------------------------------------------------
# PING
# ----------------------------------------------------------
print("[PING]")
print("OK =", r.ping())

# ----------------------------------------------------------
# SET / GET
# ----------------------------------------------------------
print("\n[SET/GET]")
r.set("test:key", "value", ex=10)
print("GET =", r.get("test:key"))

# ----------------------------------------------------------
# TTL
# ----------------------------------------------------------
print("\n[TTL]")
print("TTL =", r.ttl("test:key"))

# ----------------------------------------------------------
# ATOMIC
# ----------------------------------------------------------
print("\n[INCR]")
r.delete("counter")
for _ in range(5):
    print("counter =", r.incr("counter"))

# ----------------------------------------------------------
# PIPELINE
# ----------------------------------------------------------
print("\n[PIPELINE]")

pipe = r.pipeline()
for i in range(10):
    pipe.set(f"p:{i}", i)
pipe.execute()

print("p:5 =", r.get("p:5"))

# ----------------------------------------------------------
# PUB/SUB
# ----------------------------------------------------------
print("\n[PUBSUB]")

pubsub = r.pubsub()
pubsub.subscribe("test:chan")

def wait_msg():
    for msg in pubsub.listen():
        if msg["type"] == "message":
            return msg["data"]

r.publish("test:chan", "hello")
msg = wait_msg()
print("message =", msg)
pubsub.close()

# ----------------------------------------------------------
# SPEED TEST
# ----------------------------------------------------------
print("\n[SPEED]")

start = time.time()
for i in range(1000):
    r.set(f"speed:{i}", i)
duration = time.time() - start

print("1000 SET in", round(duration, 3), "sec →", int(1000/duration), "ops/sec")

print("\n✔ REDIS WORKING")
