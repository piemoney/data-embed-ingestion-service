#!/bin/bash
# Test script for Search API endpoints

BASE_URL="http://localhost:5000"

echo "=== Testing Search API ==="
echo ""

# Test 1: JSON endpoint (non-streaming)
echo "1. Testing JSON endpoint (POST /api/search/json)"
echo "----------------------------------------"
curl -X POST "${BASE_URL}/api/search/json" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What is Section 80C?",
    "limit": 5,
    "scoreThreshold": 0.0
  }' \
  | jq '.' 2>/dev/null || cat

echo ""
echo ""

# Test 2: Streaming endpoint (SSE)
echo "2. Testing Streaming endpoint (POST /api/search)"
echo "----------------------------------------"
echo "Note: Use -N flag to disable buffering for streaming"
curl -N -X POST "${BASE_URL}/api/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What is Section 80C?",
    "limit": 5,
    "scoreThreshold": 0.0
  }'

echo ""
echo ""

# Test 3: Minimal request (only query)
echo "3. Testing with minimal request (query only)"
echo "----------------------------------------"
curl -X POST "${BASE_URL}/api/search/json" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "tax deduction"
  }' \
  | jq '.' 2>/dev/null || cat

echo ""
echo ""

# Test 4: Custom limit and threshold
echo "4. Testing with custom limit and threshold"
echo "----------------------------------------"
curl -X POST "${BASE_URL}/api/search/json" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "employee benefits",
    "limit": 10,
    "scoreThreshold": 0.5
  }' \
  | jq '.' 2>/dev/null || cat
