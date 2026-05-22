$senderEmail = "rex@gmail.com"
$senderPassword = "password"

$loginBody = @{
    email = $senderEmail
    password = $senderPassword
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8081/api/v1/auth/login" `
    -ContentType "application/json" `
    -Body $loginBody

$token = $loginResponse.access_token

$receiverIds = @(
    "dbcac674-4bbd-4050-9d24-f509d5a86130",
    "b23439a9-e320-40f0-8510-a5e178bd7653",
    "78832670-80b0-4dca-88f5-18792ede8b0b",
    "bf95ddb4-58d2-4d48-85b8-e4d8768b98a6",
    "c0859217-2420-4d50-9b70-11ef569575f5"
)

$amount = 100
$pin = "1234"

$jobs = foreach ($receiver in $receiverIds) {
    Start-Job -ScriptBlock {
        param($token, $receiver, $amount, $pin)

        $body = @{
            receiverUserId = $receiver
            amount = $amount
            idempotencyKey = "multi-receiver-" + [guid]::NewGuid()
            walletPin = $pin
            description = "one sender many receivers test"
        } | ConvertTo-Json

        Invoke-RestMethod `
            -Method Post `
            -Uri "http://localhost:8081/api/v1/wallets/transfers" `
            -Headers @{ Authorization = "Bearer $token" } `
            -ContentType "application/json" `
            -Body $body

    } -ArgumentList $token, $receiver, $amount, $pin
}

$jobs | Wait-Job
$jobs | Receive-Job
$jobs | Remove-Job