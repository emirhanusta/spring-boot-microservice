#!/usr/bin/env bash
# End-to-end smoke test, everything goes through the gateway (default http://localhost:8080).
# Requires: curl, jq. Exits non-zero on the first failed check. Safe to re-run (unique usernames per run).
set -euo pipefail

GW="${GATEWAY_URL:-http://localhost:8080}"
RUN="$(date +%s)$RANDOM"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
ALL_BODIES="$WORK/all-response-bodies.txt"
: > "$ALL_BODIES"

STATUS=""
BODY=""

fail() { echo "SMOKE FAILED: $*" >&2; exit 1; }
step() { echo "== $*"; }

# call METHOD PATH [TOKEN] [JSON_BODY] -> sets STATUS and BODY, records the body for the leak scan
call() {
  local method="$1" path="$2" token="${3:-}" data="${4:-}"
  local args=(-sS -X "$method" -o "$WORK/body" -w '%{http_code}' "$GW$path")
  [[ -n "$token" ]] && args+=(-H "Authorization: Bearer $token")
  [[ -n "$data" ]] && args+=(-H 'Content-Type: application/json' -d "$data")
  STATUS="$(curl "${args[@]}")" || fail "$method $path: curl could not reach $GW"
  BODY="$(cat "$WORK/body")"
  printf '%s\n' "$BODY" >> "$ALL_BODIES"
}

expect() { # expect STATUS DESCRIPTION
  [[ "$STATUS" == "$1" ]] || fail "$2: expected HTTP $1, got $STATUS. Body: $BODY"
  echo "   ok: $2 (HTTP $STATUS)"
}

json() { jq -er "$1" <<<"$BODY" || fail "unexpected response, jq '$1' failed on: $BODY"; }

register() { # register USERNAME [EXTRA_JSON_FIELDS]
  local extra="${2:-}"
  call POST /api/v1/auth/register "" "{\"username\":\"$1\",\"email\":\"$1@example.com\",\"password\":\"s3cret-pass-$RUN\"$extra}"
}

login() { # login USERNAME PASSWORD -> prints access token
  call POST /api/v1/auth/login "" "{\"username\":\"$1\",\"password\":\"$2\"}"
  expect 200 "login $1" >&2
  json .accessToken
}

ALICE="alice_$RUN"
BOB="bob_$RUN"
PASS="s3cret-pass-$RUN"

step "gateway reachable"
call GET /actuator/health
expect 200 "gateway health"

step "1. register alice, login, GET /users/me"
register "$ALICE"
expect 201 "register $ALICE"
ALICE_TOKEN="$(login "$ALICE" "$PASS")"
call GET /api/v1/users/me "$ALICE_TOKEN"
expect 200 "GET /users/me"
[[ "$(json .username)" == "$ALICE" ]] || fail "/users/me returned the wrong user: $BODY"
[[ "$(json .role)" == "USER" ]] || fail "alice should have role USER: $BODY"

step "2. registering with role ADMIN yields a USER"
register "mallory_$RUN" ',"role":"ADMIN"'
expect 201 "register with role ADMIN"
[[ "$(json .role)" == "USER" ]] || fail "self-registered role must be USER, got: $BODY"
echo "   ok: returned role is USER"

step "3. admin creates an author and a book"
ADMIN_TOKEN="$(login admin admin)"
call POST /api/v1/authors "$ADMIN_TOKEN" '{"name":"Smoke Author","biography":"created by smoke.sh"}'
expect 201 "admin creates author"
AUTHOR_ID="$(json .id)"
call POST /api/v1/books "$ADMIN_TOKEN" \
  "{\"authorId\":\"$AUTHOR_ID\",\"title\":\"Smoke Book $RUN\",\"description\":\"smoke\",\"totalCopies\":3,\"genres\":[\"FANTASY\",\"NOVEL\"]}"
expect 201 "admin creates book"
BOOK_ID="$(json .id)"

step "4. alice cannot create books; anonymous listing works and genres is an array"
call POST /api/v1/books "$ALICE_TOKEN" \
  "{\"authorId\":\"$AUTHOR_ID\",\"title\":\"Nope\",\"genres\":[\"NOVEL\"]}"
expect 403 "alice POST /books is forbidden"
call GET "/api/v1/books?size=100"
expect 200 "anonymous GET /books"
[[ "$(jq -r --arg id "$BOOK_ID" '[.content[] | select(.id == $id)] | length' <<<"$BODY")" == "1" ]] \
  || fail "created book $BOOK_ID not in listing: $BODY"
[[ "$(jq -r --arg id "$BOOK_ID" '.content[] | select(.id == $id) | .genres | type' <<<"$BODY")" == "array" ]] \
  || fail "genres is not a JSON array: $BODY"
echo "   ok: book listed, genres is an array"

step "5. reviews: ownership is enforced"
call POST /api/v1/reviews "$ALICE_TOKEN" "{\"bookId\":\"$BOOK_ID\",\"rating\":5,\"body\":\"Loved it\"}"
expect 201 "alice posts a review"
REVIEW_ID="$(json .id)"
register "$BOB"
expect 201 "register $BOB"
BOB_TOKEN="$(login "$BOB" "$PASS")"
call DELETE "/api/v1/reviews/$REVIEW_ID" "$BOB_TOKEN"
expect 403 "bob cannot delete alice's review"
call DELETE "/api/v1/reviews/$REVIEW_ID" "$ALICE_TOKEN"
expect 204 "alice deletes her review"

step "6. invalid token is rejected at the gateway"
call GET /api/v1/books "invalid.token.value"
expect 401 "garbage bearer token"

step "7. no response leaked a password or hash"
if grep -Eiq 'password|\$2[aby]\$' "$ALL_BODIES"; then
  grep -Ein 'password|\$2[aby]\$' "$ALL_BODIES" >&2 || true
  fail "a response body contained 'password' or a bcrypt hash"
fi
echo "   ok: $(wc -l < "$ALL_BODIES") response bodies scanned, none contain 'password' or '\$2a\$'"

echo
echo "SMOKE PASSED"
