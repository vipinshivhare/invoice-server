#!/bin/bash

# Quick Server Test Script
# Tests if your Render deployment is working

BASE_URL="https://invoice-server-x4tn.onrender.com"

echo "=========================================="
echo "Testing Invoice Server on Render"
echo "=========================================="
echo ""

echo "1. Testing server connectivity..."
echo "   URL: $BASE_URL/health"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -I "$BASE_URL/health")
echo "   HTTP Status: $STATUS"

if [ "$STATUS" = "000" ]; then
    echo "   ❌ Server is not responding (Connection refused/Timeout)"
    echo "   → Check Render dashboard - is the service running?"
elif [ "$STATUS" = "200" ]; then
    echo "   ✅ Server is RUNNING!"
else
    echo "   ⚠️  Server responded with status: $STATUS"
fi

echo ""
echo "2. Testing webhook endpoint..."
WEBHOOK_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST $BASE_URL/api/webhooks/clerk)
echo "   Endpoint: /api/webhooks/clerk"
echo "   HTTP Status: $WEBHOOK_STATUS"

if [ "$WEBHOOK_STATUS" = "401" ] || [ "$WEBHOOK_STATUS" = "400" ]; then
    echo "   ✅ Endpoint exists! (401/400 is expected - needs proper headers)"
elif [ "$WEBHOOK_STATUS" = "404" ]; then
    echo "   ❌ Endpoint not found"
else
    echo "   ⚠️  Status: $WEBHOOK_STATUS"
fi

echo ""
echo "3. Testing protected endpoint (should return 401)..."
PROTECTED_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET $BASE_URL/api/invoices)
echo "   Endpoint: /api/invoices"
echo "   HTTP Status: $PROTECTED_STATUS"

if [ "$PROTECTED_STATUS" = "401" ] || [ "$PROTECTED_STATUS" = "403" ]; then
    echo "   ✅ Endpoint exists! (401/403 is expected - needs JWT token)"
elif [ "$PROTECTED_STATUS" = "404" ]; then
    echo "   ❌ Endpoint not found"
else
    echo "   ⚠️  Status: $PROTECTED_STATUS"
fi

echo ""
echo "=========================================="
if [ "$STATUS" != "000" ]; then
    echo "✅ SERVER IS WORKING!"
    echo ""
    echo "Next steps:"
    echo "1. Test with your frontend application"
    echo "2. Make sure to include JWT token in Authorization header"
    echo "3. Update Clerk webhook URL to: $BASE_URL/api/webhooks/clerk"
else
    echo "❌ SERVER IS NOT RESPONDING"
    echo ""
    echo "Check:"
    echo "1. Render dashboard - is service running?"
    echo "2. Service might be sleeping (free tier)"
    echo "3. Wait a few seconds and try again"
fi
echo "=========================================="

