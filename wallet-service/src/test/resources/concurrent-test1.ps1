$token = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ0WjhpX2YzY0tZeTF0SzZuYjkwbEVKU1VrQ0VmcEtyNkdQMFpDQlcwVmI0In0.eyJleHAiOjE3NzkyNjU5MTEsImlhdCI6MTc3OTI2NTYxMSwianRpIjoib25ydHJvOmI3OTkwZjJiLTNkYTAtMmNlMS03NGRiLWYyZDllYmMyYzgxYiIsImlzcyI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MC9yZWFsbXMvdHhuZmxvdyIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiI2MjVhZWVhZC01ZDg1LTQyY2EtODc2Yi0xM2I1NTg1MDIwMGYiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJ0eG5mbG93LWF1dGgtc2VydmljZSIsInNpZCI6Imxxb203bktNN1RkUTFaSV9FZ1UwZV9rSiIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOlsiIl0sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJvZmZsaW5lX2FjY2VzcyIsImRlZmF1bHQtcm9sZXMtdHhuZmxvdyIsInVtYV9hdXRob3JpemF0aW9uIiwiVVNFUiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoib3BlbmlkIGVtYWlsIHByb2ZpbGUiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiYXBwX3VzZXJfaWQiOiI5NmVkZTQzNS1mMjM0LTQ4OTEtYTE2Yy1iMTBjYWEzOWMyZDIiLCJuYW1lIjoiUmFtYW4ga3VtYXIiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJyZXhAZ21haWwuY29tIiwiZ2l2ZW5fbmFtZSI6IlJhbWFuIiwiZmFtaWx5X25hbWUiOiJrdW1hciIsImVtYWlsIjoicmV4QGdtYWlsLmNvbSJ9.nlTwCTm271MJvEwR5iEJZ_GO-7lTJ5mQ27G6Bzn8f4jRYrg4_4UkYpcy-FGf8Bhp_byPYMZfBzZnGOgBnaZfPtgG0KzM0K6NV6MtiPnz8N2NBHzb4S-q8DzSHu1mO00I7sk1ScOqPUBFnGmMHTOhEAL22yRCORZTOhwfPWtD25A-CMmriuu_X-Je2wQOns7f53yRG2xAU3H_y2X5jqnL5R__62sLYQUUOxU7eOo-LzBJzt7QA51YltdkA9jkH33b3ZoX8dOntsuSpHyMMSw7IAg1uXEGsbVKfeEHJXF9l1fRIqT0-JanglQ_-pnfaf7SCCbOjTS9r8rkTU7HIzq6Mg"
$receiverUserId = "dcfae284-fa32-4611-8a57-a2b0ec3cc374"

1..10 | ForEach-Object -Parallel {
    $body = @{
        receiverUserId = $using:receiverUserId
        amount = 100
        idempotencyKey = "concurrent-txn-$($_)"
        walletPin = "1234"
        description = "concurrent test"
    } | ConvertTo-Json

    Invoke-RestMethod `
        -Method Post `
        -Uri "http://localhost:8081/api/v1/wallets/transfers" `
        -Headers @{ Authorization = "Bearer $using:token" } `
        -ContentType "application/json" `
        -Body $body
} -ThrottleLimit 10