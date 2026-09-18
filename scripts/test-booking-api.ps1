# This script exercises the API without opening the browser.
# Run it from the repository root after PostgreSQL and Spring Boot are running.

$ErrorActionPreference = "Stop"
$baseUrl = "http://localhost:8080"

Write-Host "Checking API health..." -ForegroundColor Cyan
Invoke-RestMethod "$baseUrl/api/v1/health" | Format-List

Write-Host "Checking availability..." -ForegroundColor Cyan
$availability = Invoke-RestMethod "$baseUrl/api/v1/availability/slots?serviceId=gel-overlay-art&date=2026-10-03"
$availability | Format-List

$body = @{
    clientName = "Amina Patel"
    phone = "+27821234567"
    email = "amina@example.com"
    serviceId = "gel-overlay-art"
    preferredDate = "2026-10-03"
    preferredTime = "14:00"
    notes = "Automated API smoke test"
    source = "website"
} | ConvertTo-Json

Write-Host "Creating booking..." -ForegroundColor Cyan
$booking = Invoke-RestMethod `
    -Uri "$baseUrl/api/v1/bookings" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body
$booking | Format-List

Write-Host "The first booking succeeded. A second identical request should return HTTP 409." -ForegroundColor Yellow
try {
    Invoke-RestMethod `
        -Uri "$baseUrl/api/v1/bookings" `
        -Method Post `
        -ContentType "application/json" `
        -Body $body
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 409) {
        Write-Host "Conflict check passed: HTTP 409 returned." -ForegroundColor Green
    } else {
        throw
    }
}
