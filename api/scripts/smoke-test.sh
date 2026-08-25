#!/usr/bin/env bash
# Exercises every API endpoint against a running instance and asserts the
# expected HTTP status code, including the main error-handling paths
# (409 conflict, 404 not found, 401 unauthorized, 400 malformed JSON).
#
# Usage:
#   BASE_URL=http://localhost:4000 ADMIN_USERNAME=admin ADMIN_PASSWORD=... ./smoke-test.sh
set -uo pipefail

BASE="${BASE_URL:-http://localhost:4000}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-ChangeMe123!}"

PASS=0
FAIL=0
RESP_FILE=$(mktemp)
TOKEN=""

pass() { echo "  PASS - $1"; PASS=$((PASS + 1)); }
fail() { echo "  FAIL - $1"; FAIL=$((FAIL + 1)); }
body() { cat "$RESP_FILE"; }

# call METHOD PATH [DATA] [USE_AUTH(1/0), default 1]
call() {
  local method="$1" path="$2" data="${3:-}" use_auth="${4:-1}"
  local args=(-s -o "$RESP_FILE" -w "%{http_code}" -X "$method" "$BASE$path")
  if [ -n "$data" ]; then
    args+=(-H "Content-Type: application/json" -d "$data")
  fi
  if [ "$use_auth" = "1" ] && [ -n "$TOKEN" ]; then
    args+=(-H "Authorization: Bearer $TOKEN")
  fi
  curl "${args[@]}"
}

expect() {
  local desc="$1" expected="$2" actual="$3"
  if [ "$actual" = "$expected" ]; then
    pass "$desc ($actual)"
  else
    fail "$desc (expected $expected, got $actual): $(body)"
  fi
}

RAND=$RANDOM
REG_NO="CI-$RAND"

echo "== Health =="
STATUS=$(call GET /health "" 0)
expect "GET /health" 200 "$STATUS"

echo "== Auth =="
STATUS=$(call POST /api/auth/login "{\"username\":\"$ADMIN_USERNAME\",\"password\":\"$ADMIN_PASSWORD\"}" 0)
expect "POST /api/auth/login" 200 "$STATUS"
TOKEN=$(jq -r '.token // empty' "$RESP_FILE")
if [ -n "$TOKEN" ]; then pass "extract token from login response"; else fail "extract token from login response: $(body)"; fi

STATUS=$(call POST /api/auth/signup "{\"name\":\"CI User\",\"username\":\"ci_user_$RAND\",\"email\":\"ci_$RAND@school.com\",\"password\":\"secret123\"}" 0)
expect "POST /api/auth/signup" 201 "$STATUS"

STATUS=$(call GET /api/auth/me)
expect "GET /api/auth/me" 200 "$STATUS"

STATUS=$(call POST /api/auth/reset-password "{\"username\":\"ci_user_$RAND\",\"newPassword\":\"NewSecurePass123\"}" 0)
expect "POST /api/auth/reset-password" 202 "$STATUS"

echo "== Error handling =="
STATUS=$(call GET /api/students "" 0)
expect "GET /api/students without auth" 401 "$STATUS"

STATUS=$(call POST /api/auth/login "{\"username\":\"$ADMIN_USERNAME\",\"password\":\"wrong-password\"}" 0)
expect "POST /api/auth/login with wrong password" 401 "$STATUS"

STATUS=$(call POST /api/students '{not-json' 1)
expect "POST /api/students with malformed JSON" 400 "$STATUS"

STATUS=$(call GET /api/does-not-exist "" 0)
expect "GET unknown route" 404 "$STATUS"

echo "== License =="
STATUS=$(call GET /api/license/status)
expect "GET /api/license/status" 200 "$STATUS"

STATUS=$(call POST /api/license/renew '{"extendByDays":365,"note":"CI smoke test"}')
expect "POST /api/license/renew" 200 "$STATUS"

STATUS=$(call GET /api/license/history)
expect "GET /api/license/history" 200 "$STATUS"

echo "== Students =="
STATUS=$(call POST /api/students "{\"regNo\":\"$REG_NO\",\"name\":\"CI Student\",\"class\":\"3\",\"fatherName\":\"CI Father\",\"phone\":\"9999999999\",\"admissionDate\":\"2026-04-01\"}")
expect "POST /api/students" 201 "$STATUS"

STATUS=$(call POST /api/students "{\"regNo\":\"$REG_NO\",\"name\":\"Dup\",\"class\":\"3\",\"fatherName\":\"X\",\"phone\":\"1\",\"admissionDate\":\"2026-04-01\"}")
expect "POST /api/students duplicate regNo" 409 "$STATUS"

STATUS=$(call GET "/api/students?search=CI")
expect "GET /api/students?search=" 200 "$STATUS"

STATUS=$(call GET "/api/students/$REG_NO")
expect "GET /api/students/:regNo" 200 "$STATUS"

STATUS=$(call PUT "/api/students/$REG_NO" "{\"regNo\":\"$REG_NO\",\"name\":\"CI Student\",\"class\":\"4\",\"fatherName\":\"CI Father\",\"phone\":\"9999999999\",\"admissionDate\":\"2026-04-01\"}")
expect "PUT /api/students/:regNo" 200 "$STATUS"

echo "== Fees =="
STATUS=$(call POST /api/fees "{\"regNo\":\"$REG_NO\",\"feeType\":\"Tuition\",\"month\":\"April\",\"totalAmount\":5000,\"amountPaid\":3000,\"paymentDate\":\"2026-04-05\"}")
expect "POST /api/fees" 201 "$STATUS"
FEE_ID=$(jq -r '.fee.id // empty' "$RESP_FILE")

STATUS=$(call POST /api/fees "{\"regNo\":\"does-not-exist\",\"feeType\":\"Tuition\",\"totalAmount\":100,\"amountPaid\":0,\"paymentDate\":\"2026-04-05\"}")
expect "POST /api/fees for unknown student" 404 "$STATUS"

STATUS=$(call GET "/api/fees?search=$REG_NO")
expect "GET /api/fees?search=" 200 "$STATUS"

STATUS=$(call GET /api/fees/summary)
expect "GET /api/fees/summary" 200 "$STATUS"

STATUS=$(call PUT "/api/fees/$FEE_ID" "{\"regNo\":\"$REG_NO\",\"feeType\":\"Tuition\",\"month\":\"April\",\"totalAmount\":5000,\"amountPaid\":5000,\"paymentDate\":\"2026-04-05\"}")
expect "PUT /api/fees/:id" 200 "$STATUS"

echo "== Stock =="
STATUS=$(call POST /api/stock "{\"itemType\":\"Book\",\"category\":\"English Book\",\"class\":\"3\",\"itemName\":\"Class 3 - English Book (CI)\",\"totalQuantity\":50,\"quantitySold\":10,\"date\":\"2026-04-01\"}")
expect "POST /api/stock" 201 "$STATUS"
STOCK_ID=$(jq -r '.stock.id // empty' "$RESP_FILE")

STATUS=$(call POST /api/stock "{\"itemType\":\"Book\",\"category\":\"English Book\",\"class\":\"3\",\"itemName\":\"Class 3 - English Book (CI)\",\"totalQuantity\":20,\"quantitySold\":5,\"date\":\"2026-04-10\"}")
expect "POST /api/stock (restock merge)" 201 "$STATUS"
MERGED=$(jq -r '.merged' "$RESP_FILE")
if [ "$MERGED" = "true" ]; then pass "restock merged into existing record"; else fail "restock should have merged, got merged=$MERGED"; fi

STATUS=$(call GET /api/stock)
expect "GET /api/stock" 200 "$STATUS"

STATUS=$(call GET "/api/stock/class-availability?filter=3")
expect "GET /api/stock/class-availability" 200 "$STATUS"

STATUS=$(call GET /api/stock/uniform-sizes)
expect "GET /api/stock/uniform-sizes" 200 "$STATUS"

STATUS=$(call POST /api/stock/uniform-sizes '{"piece":"Pant","size":"15"}')
expect "POST /api/stock/uniform-sizes" 201 "$STATUS"

STATUS=$(call PUT "/api/stock/$STOCK_ID" "{\"itemType\":\"Book\",\"category\":\"English Book\",\"class\":\"3\",\"itemName\":\"Class 3 - English Book (CI)\",\"totalQuantity\":70,\"quantitySold\":15,\"date\":\"2026-04-10\"}")
expect "PUT /api/stock/:id" 200 "$STATUS"

echo "== Dashboard =="
STATUS=$(call GET /api/dashboard/summary)
expect "GET /api/dashboard/summary" 200 "$STATUS"

STATUS=$(call GET /api/dashboard/student-fee-status)
expect "GET /api/dashboard/student-fee-status" 200 "$STATUS"

echo "== Backup =="
STATUS=$(call GET /api/backup/export)
expect "GET /api/backup/export" 200 "$STATUS"

STATUS=$(call POST /api/backup/import '{"data":{"students":[],"fees":[],"stock":[]}}')
expect "POST /api/backup/import" 200 "$STATUS"

echo "== Cleanup =="
STATUS=$(call DELETE "/api/fees/$FEE_ID")
expect "DELETE /api/fees/:id" 204 "$STATUS"

STATUS=$(call DELETE "/api/stock/$STOCK_ID")
expect "DELETE /api/stock/:id" 204 "$STATUS"

STATUS=$(call DELETE "/api/students/$REG_NO")
expect "DELETE /api/students/:regNo" 204 "$STATUS"

STATUS=$(call POST /api/auth/logout)
expect "POST /api/auth/logout" 204 "$STATUS"

rm -f "$RESP_FILE"

echo ""
echo "===================="
echo "PASS: $PASS  FAIL: $FAIL"
echo "===================="

[ "$FAIL" -eq 0 ]
