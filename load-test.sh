#!/bin/bash

API_URL="http://localhost:8080/api/v1/wallet"
API_URL_CREATE="http://localhost:8080/api/v1/wallets"
WALLET_ID=$(uuidgen)

curl -X POST "$API_URL_CREATE/$WALLET_ID/create" -H "Content-Type: application/json"

echo "Starting load test for wallet: $WALLET_ID"

seq 1 1000 | xargs -P 100 -I {} curl -X POST "$API_URL" \
  -H "Content-Type: application/json" \
  -d "{\"walletId\":\"$WALLET_ID\",\"operationType\":\"DEPOSIT\",\"amount\":1}" \
  -s -o /dev/null -w "%{http_code}\n" | sort | uniq -c