# Registers PresAssistant add-in with Office via registry shared folder catalog
# No admin rights required

$addinFolder = "$env:LOCALAPPDATA\PresAssistantAddin"
$catalogId   = "{a1b2c3d4-e5f6-7890-abcd-ef1234567890}"
$manifestUrl = "https://localhost:8082/addin/manifest.xml"

# Create local folder for manifest
New-Item -ItemType Directory -Path $addinFolder -Force | Out-Null

# Bypass SSL validation for self-signed certificate
[Net.ServicePointManager]::ServerCertificateValidationCallback = { $true }

# Download production manifest from Spring Boot
Write-Host "Downloading manifest from https://localhost:8082..."
try {
    (New-Object Net.WebClient).DownloadFile($manifestUrl, "$addinFolder\manifest.xml")
    Write-Host "Manifest saved to $addinFolder" -ForegroundColor Green
} catch {
    Write-Error "Cannot connect to https://localhost:8082. Make sure the server is running. Error: $_"
    exit 1
}

# Catalog URL must end with backslash for Office to recognise it as a folder
$catalogUrl = $addinFolder.TrimEnd('\') + '\'

# Register under all installed Office versions (15.0, 16.0)
$officeVersions = @("15.0", "16.0")
$registered = $false

foreach ($ver in $officeVersions) {
    $wefBase = "HKCU:\SOFTWARE\Microsoft\Office\$ver\WEF"
    if (Test-Path $wefBase) {
        $regPath = "$wefBase\TrustedCatalogs\$catalogId"
        New-Item -Path $regPath -Force | Out-Null
        Set-ItemProperty -Path $regPath -Name "Id"    -Value $catalogId
        Set-ItemProperty -Path $regPath -Name "Url"   -Value $catalogUrl
        Set-ItemProperty -Path $regPath -Name "Flags" -Type DWord -Value 1
        Write-Host "Registered for Office $ver" -ForegroundColor Green
        $registered = $true
    }
}

if (-not $registered) {
    Write-Warning "No supported Office installation found (checked 15.0, 16.0)"
    exit 1
}

# Add localhost to Trusted Sites zone (must run as current user, not elevated)
# This allows WebView2/IE inside Office to load HTTPS content from localhost
$zoneKey = "HKCU:\Software\Microsoft\Windows\CurrentVersion\Internet Settings\ZoneMap\Domains\localhost"
New-Item -Path $zoneKey -Force | Out-Null
Set-ItemProperty -Path $zoneKey -Name "https" -Type DWord -Value 2
Write-Host "localhost added to Trusted Sites (current user)." -ForegroundColor Green

Write-Host ""
Write-Host "Done. Next steps:" -ForegroundColor Yellow
Write-Host "  1. Fully close PowerPoint (check Task Manager - no POWERPNT.EXE)"
Write-Host "  2. Open PowerPoint"
Write-Host "  3. Insert -> My Add-ins -> SHARED FOLDER tab"
Write-Host "  4. Select 'Ассистент лектора' -> Add"
