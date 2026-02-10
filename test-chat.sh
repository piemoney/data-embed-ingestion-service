#!/bin/bash

# Test script for Chat API
# Usage: ./test-chat.sh

BASE_URL="http://localhost:8080"

echo "=== Testing Chat API (SSE) ==="
echo ""
echo "Query: What is Section 80C?"
echo ""

curl -N -X POST "${BASE_URL}/api/chat" \
  -H "Content-Type: application/json" \
  -d '{"query": "What is Section 80C?"}'

echo ""
echo ""
echo "=== Testing Chat API (Plain Text) ==="
echo ""
echo "Query: How do I request time off?"
echo ""

curl -N -X POST "${BASE_URL}/api/chat/text" \
  -H "Content-Type: application/json" \
  -d '{"query": "How do I request time off?"}'

echo ""
echo ""
