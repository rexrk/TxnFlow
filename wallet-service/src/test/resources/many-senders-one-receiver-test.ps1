$receiverUserId = "7e431738-45a5-4928-8402-c517c591694f"

$users = @(
    @{ email = "user1@gmail.com"; password = "password" },
    @{ email = "user2@gmail.com"; password = "password" },
    @{ email = "user3@gmail.com"; password = "password" },
    @{ email = "user4@gmail.com"; password = "password" },
    @{ email = "user5@gmail.com"; password = "password" }
)

$amount = 100
$pin = "1010"

$tokens = foreach ($user in $users) {
    $loginBody = @{
        email = $user.email
        password = $user.password
    } | ConvertTo-Json

    $loginResponse = Invoke-RestMethod `
        -Method Post `
        -Uri "http://localhost:8081/api/v1/auth/login" `
        -ContentType "application/json" `
        -Body $loginBody

    $loginResponse.access_token
}

$jobs = foreach ($token in $tokens) {
    Start-Job -ScriptBlock {
        param($token, $receiverUserId, $amount, $pin)

        $body = @{
            receiverUserId = $receiverUserId
            amount = $amount
            idempotencyKey = "many-senders-" + [guid]::NewGuid()
            walletPin = $pin
            description = "many senders one receiver test"
        } | ConvertTo-Json

        Invoke-RestMethod `
            -Method Post `
            -Uri "http://localhost:8081/api/v1/wallets/transfers" `
            -Headers @{ Authorization = "Bearer $token" } `
            -ContentType "application/json" `
            -Body $body

    } -ArgumentList $token, $receiverUserId, $amount, $pin
}

$jobs | Wait-Job
$jobs | Receive-Job
$jobs | Remove-Job