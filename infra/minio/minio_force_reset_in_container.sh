#!/usr/bin/env bash
set -Eeuo pipefail


CONTAINER="kaspi_minio"

MINIO_URL="http://127.0.0.1:9000"

ROOT_USER="admin"
ROOT_PASS="admin12345@"

APP_USER="app"
APP_PASS="app12345@"

CONSOLE_USER="console"
CONSOLE_PASS="console12345@"

BUCKET="kaspi-lab-public"
POLICY_WRITE="app-write-only"

PUBLIC_ENDPOINT="https://minio-s3.icod.kz"


log()  { echo -e "\033[0;32m[INFO]\033[0m $*"; }
warn() { echo -e "\033[0;33m[WARN]\033[0m $*" >&2; }
err()  { echo -e "\033[0;31m[ERROR]\033[0m $*" >&2; exit 1; }

exec_in() {
    docker exec -i "$CONTAINER" sh -c "$*"
}


docker inspect "$CONTAINER" >/dev/null 2>&1 || err "Container '$CONTAINER' not found"

if [[ "$(docker inspect -f '{{.State.Running}}' "$CONTAINER")" != "true" ]]; then
    log "Starting container..."
    docker start "$CONTAINER" >/dev/null
    sleep 2
fi

log "Configure mc alias"
exec_in "mc alias set local $MINIO_URL $ROOT_USER $ROOT_PASS --api s3v4"


log "Cleaning buckets"
docker exec -i "$CONTAINER" mc ls local --json 2>/dev/null | \
sed -n 's/.*"key":"\([^"]*\)".*/\1/p' | sed 's|/$||' | while read -r b; do
    [[ -z "$b" ]] && continue
    log "Remove bucket: $b"
    exec_in "mc rm -r --force local/$b || true"
    exec_in "mc rb --force local/$b || true"
done || true


log "Cleaning users"
docker exec -i "$CONTAINER" mc admin user list local --json 2>/dev/null | \
sed -n 's/.*"accessKey":"\([^"]*\)".*/\1/p' | while read -r u; do
    [[ -z "$u" || "$u" == "$ROOT_USER" ]] && continue
    log "Remove user: $u"
    exec_in "mc admin user remove local $u || true"
done || true


log "Cleaning policies"
docker exec -i "$CONTAINER" mc admin policy list local --json 2>/dev/null | \
sed -n 's/.*"policy":"\([^"]*\)".*/\1/p' | while read -r p; do
    case "$p" in
        ""|consoleAdmin|diagnostics|readonly|readwrite|writeonly) continue ;;
        *)
            log "Remove policy: $p"
            exec_in "mc admin policy remove local $p || true"
            ;;
    esac
done || true


log "Create bucket"
exec_in "mc mb local/$BUCKET || true"
exec_in "mc version enable local/$BUCKET || true"


log "Create APP user (upload only)"
exec_in "mc admin user add local $APP_USER $APP_PASS || true"

log "Create CONSOLE admin user"
exec_in "mc admin user add local $CONSOLE_USER $CONSOLE_PASS || true"
exec_in "mc admin policy attach local consoleAdmin --user $CONSOLE_USER || true"


log "Create write-only policy"

POLICY_JSON=$(cat <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": [
        "s3:PutObject",
        "s3:AbortMultipartUpload",
        "s3:ListBucketMultipartUploads",
        "s3:GetBucketLocation"
      ],
      "Effect": "Allow",
      "Resource": [
        "arn:aws:s3:::$BUCKET",
        "arn:aws:s3:::$BUCKET/*"
      ]
    }
  ]
}
EOF
)

echo "$POLICY_JSON" | docker exec -i "$CONTAINER" mc admin policy create local "$POLICY_WRITE" /dev/stdin || true
exec_in "mc admin policy attach local $POLICY_WRITE --user $APP_USER"


log "Enable PUBLIC READ"
exec_in "mc anonymous set download local/$BUCKET"


log "Self-test upload"
exec_in "echo healthcheck > /tmp/minio_health.txt"
exec_in "mc cp /tmp/minio_health.txt local/$BUCKET/health.txt" || warn "Upload failed"
exec_in "rm -f /tmp/minio_health.txt"


echo ""
echo "=================================================="
echo -e "\033[0;32mMINIO RESET COMPLETE\033[0m"
echo "=================================================="

echo ""
echo "==== EXTERNAL S3 ACCESS ===="
echo "Endpoint   : $PUBLIC_ENDPOINT"
echo "Bucket     : $BUCKET"
echo "AccessKey  : $APP_USER"
echo "SecretKey  : $APP_PASS"
echo "Region     : us-east-1"
echo "SSL        : enabled"
echo "PathStyle  : required"
echo ""

echo "Public URL format:"
echo "$PUBLIC_ENDPOINT/$BUCKET/<filename>"
echo ""

echo "==== CONSOLE ADMIN ===="
echo "Console URL : $PUBLIC_ENDPOINT"
echo "User        : $CONSOLE_USER"
echo "Password    : $CONSOLE_PASS"
echo ""

echo "==== CURRENT LIMITS / FACTS ===="
echo "- Bucket quota       : none"
echo "- Max objects        : unlimited"
echo "- Versioning         : enabled"
echo "- Public read        : enabled"
echo "- Upload user rights : PUT only"
echo "- Delete protection  : none"
echo "- Lifecycle rules    : none"
echo "- Retention          : none"
echo "- Auto cleanup       : none"
echo ""

echo "==== DOCKER LIMITS ===="
docker inspect "$CONTAINER" | grep -E '"NanoCpus"|"Memory"' || true
echo ""

echo "==== VERIFY POLICY ===="
exec_in "mc admin policy entities local --user $APP_USER || true"

echo ""
echo "==== VERIFY PUBLIC ===="
exec_in "mc anonymous get local/$BUCKET || true"

echo ""
echo "==== VERIFY OBJECT ===="
exec_in "mc ls local/$BUCKET/health.txt || true"

echo ""
echo "=================================================="
echo "SYSTEM READY"
echo "=================================================="
