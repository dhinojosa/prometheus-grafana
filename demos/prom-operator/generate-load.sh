#!/usr/bin/env bash
# Generates traffic against the quarkus-app so orders_placed_total,
# inventory_stock_level, and order_fulfillment_duration_seconds have data to
# show in Prometheus/Grafana. Requires jq, and the quarkus-app port-forward
# from README step 8 to be running:
#   kubectl port-forward -n coffee svc/quarkus-app 8081:8081
set -euo pipefail

BASE_URL="http://localhost:8081"

if ! command -v jq > /dev/null; then
  echo "jq is required (brew install jq)" >&2
  exit 1
fi

echo "Restocking inventory..."
for item in ESPRESSO LATTE CAPPUCCINO AMERICANO; do
  curl -s -X POST "$BASE_URL/inventory/restock" \
    -H "Content-Type: application/json" \
    -d "{\"item\": \"$item\", \"quantity\": 100}" > /dev/null
done
curl -s "$BASE_URL/inventory" | jq .

echo "Placing orders..."
ITEMS=(ESPRESSO LATTE CAPPUCCINO AMERICANO)
ORDER_IDS=()
for i in $(seq 1 20); do
  ITEM=${ITEMS[$((RANDOM % 4))]}
  QTY=$((RANDOM % 3 + 1))
  ID=$(curl -s -X POST "$BASE_URL/orders" \
    -H "Content-Type: application/json" \
    -d "{\"item\": \"$ITEM\", \"quantity\": $QTY}" | jq -r '.id')
  ORDER_IDS+=("$ID")
  sleep 0.2
done

echo "Completing orders..."
for id in "${ORDER_IDS[@]:0:15}"; do
  curl -s -X PATCH "$BASE_URL/orders/$id/complete" > /dev/null
  sleep 0.2
done

echo "Triggering a deliberate 409 (order exceeds remaining stock)..."
curl -s -o /dev/null -w "  -> HTTP %{http_code}\n" -X POST "$BASE_URL/orders" \
  -H "Content-Type: application/json" \
  -d '{"item": "ESPRESSO", "quantity": 999}'

echo "Custom metrics:"
curl -s "$BASE_URL/q/metrics" | grep -E "orders_placed_total|inventory_stock_level|order_fulfillment_duration_seconds"
