$base = "http://localhost:8081/api"
$s = Get-Random -Maximum 99999
$script:pass = 0; $script:fail = 0

function Call($method, $path, $body = $null, $token = $null) {
    $h = @{}
    if ($token) { $h["Authorization"] = "Bearer $token" }
    try {
        if ($body) {
            $json = ConvertTo-Json -InputObject $body -Depth 6
            return Invoke-RestMethod -Method $method -Uri "$base$path" -Headers $h -ContentType "application/json" -Body $json
        }
        return Invoke-RestMethod -Method $method -Uri "$base$path" -Headers $h
    } catch {
        $code = 0
        $msg = "$($_.ErrorDetails.Message)"
        if ($_.Exception.Response) {
            $code = [int]$_.Exception.Response.StatusCode
            if (-not $msg) {
                try { $msg = (New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())).ReadToEnd() } catch {}
            }
        }
        if (-not $msg) { $msg = $_.Exception.Message }
        Write-Host "      -> HTTP $code on $method $path : $msg" -ForegroundColor DarkYellow
        return [pscustomobject]@{ failed = $true; code = $code; msg = $msg }
    }
}

# GET se aayi list ko flat karke deta hai (PowerShell 5.1 ki array wali dikkat ka fix)
function ListOf($path, $tok) {
    $r = Call GET $path $null $tok
    $out = New-Object System.Collections.ArrayList
    foreach ($x in @($r)) {
        foreach ($y in @($x)) { if ($null -ne $y) { [void]$out.Add($y) } }
    }
    return $out.ToArray()
}

function Check($name, $ok) {
    if ($ok) { Write-Host "PASS  $name" -ForegroundColor Green; $script:pass++ }
    else { Write-Host "FAIL  $name" -ForegroundColor Red; $script:fail++ }
}

function Dump($label, $o) {
    Write-Host ("      {0}: {1}" -f $label, ($o | ConvertTo-Json -Depth 6 -Compress)) -ForegroundColor DarkGray
}

function Reg($name, $role, $extra = @{}) {
    $b = @{ name = $name; email = ("$name$s@test.com").ToLower(); password = "123456"; role = $role } + $extra
    return Call POST "/auth/register" $b
}

# Provider ki sabse nayi request
function FirstReq($tok) {
    return @(ListOf "/bookings/requests" $tok) | Sort-Object itemId -Descending | Select-Object -First 1
}

# Customer ki ek booking id se
function MyBooking($id) {
    return @(ListOf "/bookings/my" $cust.token) | Where-Object { $_.id -eq $id } | Select-Object -First 1
}

# ---------- Dates ----------
$d1 = (Get-Date).AddDays(30).ToString("yyyy-MM-dd")
$d2 = (Get-Date).AddDays(31).ToString("yyyy-MM-dd")
$d3 = (Get-Date).AddDays(32).ToString("yyyy-MM-dd")
$d4 = (Get-Date).AddDays(33).ToString("yyyy-MM-dd")
$today = (Get-Date).ToString("yyyy-MM-dd")

Write-Host "`n=== 1. REGISTER + LOGIN ===" -ForegroundColor Cyan
$cust = Reg "Raj" "CUSTOMER"
$cat  = Reg "Sharma" "CATERER" @{ basePrice = 450 }
$bar  = Reg "RoyalBar" "BAR_OWNER" @{ basePrice = 15000 }
$thk  = Reg "Ramesh" "THEKEDAR" @{ waiterCount = 20; captainCount = 5 }
Check "4 users registered" ($cust.token -and $cat.token -and $bar.token -and $thk.token)
if (-not $cust.token) { Write-Host "Register fail hua. App chal rahi hai? Port 8081 sahi hai?" -ForegroundColor Yellow; exit }

$l = Call POST "/auth/login" @{ email = $cust.user.email; password = "123456" }
Check "Login works" ($l.token)
$bad = Call POST "/auth/login" @{ email = $cust.user.email; password = "wrong" }
Check "Wrong password rejected (401)" ($bad.failed -and $bad.code -eq 401)

Write-Host "`n=== 2. BOOKING (caterer + bar + thekedar) ===" -ForegroundColor Cyan
$b1 = Call POST "/bookings" @{ eventDate = $d1; shift = "NIGHT"; plates = 300; items = @( @{ vendorId = $cat.user.id }, @{ vendorId = $bar.user.id }, @{ vendorId = $thk.user.id; waiters = 10; captains = 2 } ) } $cust.token
Check "Booking created as PENDING" ($b1.status -eq "PENDING")
Check "Total = 163600" ($b1.totalAmount -eq 163600)

$dup = Call POST "/bookings" @{ eventDate = $d1; shift = "NIGHT"; items = @( @{ vendorId = $bar.user.id } ) } $cust.token
Check "Double booking of bar blocked" ($dup.failed -and $dup.code -eq 400)

$over = Call POST "/bookings" @{ eventDate = $d3; shift = "NIGHT"; items = @( @{ vendorId = $thk.user.id; waiters = 50; captains = 1 } ) } $cust.token
Check "Thekedar over-quantity blocked" ($over.failed -and $over.code -eq 400)

Write-Host "`n=== 3. PROVIDERS ACCEPT ===" -ForegroundColor Cyan
$rc = FirstReq $cat.token; $rb = FirstReq $bar.token; $rt = FirstReq $thk.token
Check "Caterer request amount 135000" ($rc.amount -eq 135000)
Check "Thekedar request amount 13600" ($rt.amount -eq 13600)
$a1 = Call PUT "/bookings/items/$($rc.itemId)/respond?accept=true" $null $cat.token
$a2 = Call PUT "/bookings/items/$($rb.itemId)/respond?accept=true" $null $bar.token
$a3 = Call PUT "/bookings/items/$($rt.itemId)/respond?accept=true" $null $thk.token
Check "All 3 accepted" ($a1.status -eq "ACCEPTED" -and $a2.status -eq "ACCEPTED" -and $a3.status -eq "ACCEPTED")
Check "Booking CONFIRMED" ((MyBooking $b1.id).status -eq "CONFIRMED")

Write-Host "`n=== 4. PAYMENT ===" -ForegroundColor Cyan
$adv = Call POST "/bookings/$($b1.id)/pay-advance" $null $cust.token
Check "Advance = 49080 (30 percent)" ($adv.advancePaid -eq 49080)
$early = Call POST "/bookings/$($b1.id)/pay-balance" $null $cust.token
Check "Balance before function day blocked" ($early.failed)

Write-Host "`n=== 5. CASE 1: CATERER SUB-BOOKING ===" -ForegroundColor Cyan
$b2 = Call POST "/bookings" @{ eventDate = $d2; shift = "DAY"; plates = 200; items = @( @{ vendorId = $cat.user.id } ) } $cust.token
Check "Customer booked only caterer" ($b2.status -eq "PENDING")
$rc2 = FirstReq $cat.token
$sub = Call POST "/bookings/items/$($rc2.itemId)/sub-book" @{ vendorId = $bar.user.id } $cat.token
Check "Caterer sub-booked the bar" ($sub.status -eq "PENDING")
$early2 = Call PUT "/bookings/items/$($rc2.itemId)/respond?accept=true" $null $cat.token
Check "Caterer cannot accept before bar replies" ($early2.failed)
$rb2 = FirstReq $bar.token
Check "Bar sees it as sub-booking" ($rb2.subBooking -eq $true)
$barAcc = Call PUT "/bookings/items/$($rb2.itemId)/respond?accept=true" $null $bar.token
Check "Bar accepted the sub-booking" ($barAcc.status -eq "ACCEPTED")
$ac = Call PUT "/bookings/items/$($rc2.itemId)/respond?accept=true" $null $cat.token
Check "Caterer accepted after bar" ($ac.status -eq "ACCEPTED")
$m2 = MyBooking $b2.id
Check "Confirmed, total 90000 (bar cost not added)" ($m2.status -eq "CONFIRMED" -and $m2.totalAmount -eq 90000)

Write-Host "`n=== 6. REJECTION ===" -ForegroundColor Cyan
$b3 = Call POST "/bookings" @{ eventDate = $d4; shift = "DAY"; items = @( @{ vendorId = $bar.user.id } ) } $cust.token
$rb3 = FirstReq $bar.token
$rej = Call PUT "/bookings/items/$($rb3.itemId)/respond?accept=false" $null $bar.token
Check "Rejected by bar -> booking REJECTED" ((MyBooking $b3.id).status -eq "REJECTED")

Write-Host "`n=== 7. BALANCE ON FUNCTION DAY ===" -ForegroundColor Cyan
$b4 = Call POST "/bookings" @{ eventDate = $today; shift = "DAY"; items = @( @{ vendorId = $bar.user.id } ) } $cust.token
$rb4 = FirstReq $bar.token
Call PUT "/bookings/items/$($rb4.itemId)/respond?accept=true" $null $bar.token | Out-Null
Call POST "/bookings/$($b4.id)/pay-advance" $null $cust.token | Out-Null
$bal = Call POST "/bookings/$($b4.id)/pay-balance" $null $cust.token
Check "Balance paid, booking COMPLETED" ($bal.status -eq "COMPLETED")

Write-Host "`n=== 8. MENU + VENDOR LIST ===" -ForegroundColor Cyan
$mi = Call POST "/menu" @{ name = "Whisky"; price = 500 } $bar.token
Check "Bar owner added menu item" ($mi.name -eq "Whisky")
$vl = @(ListOf "/vendors?role=BAR_OWNER&date=$d1&shift=NIGHT" $cust.token) | Where-Object { $_.id -eq $bar.user.id } | Select-Object -First 1
Dump "vendor list entry" $vl
Check "Vendor list: bar busy on $d1 and menu visible" ($vl.available -eq $false -and @($vl.menu).Count -eq 1)

Write-Host "`n=== 9. ROLE SECURITY ===" -ForegroundColor Cyan
$x1 = Call POST "/bookings" @{ eventDate = $d4; shift = "DAY"; items = @( @{ vendorId = $bar.user.id } ) } $cat.token
Check "Caterer cannot create booking (403)" ($x1.failed -and $x1.code -eq 403)
$x2 = Call POST "/menu" @{ name = "x"; price = 1 } $cust.token
Check "Customer cannot add menu (403)" ($x2.failed -and $x2.code -eq 403)
$x3 = Call POST "/bookings/items/1/sub-book" @{ vendorId = 1 } $bar.token
Check "Bar cannot sub-book (403)" ($x3.failed -and $x3.code -eq 403)
$x4 = Call GET "/vendors?role=BAR_OWNER&date=$d1&shift=NIGHT"
Check "No token -> blocked (403)" ($x4.failed -and $x4.code -eq 403)

Write-Host "`nPASSED: $script:pass   FAILED: $script:fail" -ForegroundColor Yellow