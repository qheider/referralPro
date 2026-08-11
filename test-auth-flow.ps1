# Test ReferralPro Authentication Flow
Write-Host "=== Testing ReferralPro Authentication ===" -ForegroundColor Cyan

# Step 1: Login
Write-Host "`n1. Logging in as admin@company.com..." -ForegroundColor Yellow
$loginBody = '{"username":"admin@company.com","password":"password123"}'

try {
    $loginResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json"
    Write-Host "✓ Login successful! Role: $($loginResponse.data.role)" -ForegroundColor Green
    $token = $loginResponse.data.token
    $headers = @{Authorization = "Bearer $token"}
} catch {
    Write-Host "✗ Login failed: $($_.Exception.Message)" -ForegroundColor Red
    exit
}

# Step 2: Test ambassador list
Write-Host "`n2. Testing /api/admin/ambassadors..." -ForegroundColor Yellow
try {
    $ambassadors = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/ambassadors" -Headers $headers
    Write-Host "✓ Found $($ambassadors.data.totalElements) ambassadors" -ForegroundColor Green
    
    if ($ambassadors.data.content.Count -gt 0) {
        $id = $ambassadors.data.content[0].id
        
        # Step 3: Test ambassador detail
        Write-Host "`n3. Testing /api/admin/ambassadors/$id..." -ForegroundColor Yellow
        try {
            $detail = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/ambassadors/$id" -Headers $headers
            Write-Host "✓ Ambassador: $($detail.data.firstName) $($detail.data.lastName)" -ForegroundColor Green
            Write-Host "  Email: $($detail.data.email)" -ForegroundColor Gray
            Write-Host "  Status: $($detail.data.status)" -ForegroundColor Gray
        } catch {
            Write-Host "✗ Detail failed: Status $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
        }
    }
} catch {
    Write-Host "✗ List failed: Status $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
}

Write-Host "`n=== Complete ===" -ForegroundColor Cyan
