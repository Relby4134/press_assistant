# Registers PresAssistant add-in with Office 2019/365 via registry
# Mimics what office-addin-debugging does automatically during npm start
# No admin rights required

$addinFolder = "$env:LOCALAPPDATA\PresAssistantAddin"
$catalogId   = "{a1b2c3d4-e5f6-7890-abcd-ef1234567890}"
$manifestUrl = "https://localhost:8082/addin/manifest.xml"

# Create local folder for manifest
New-Item -ItemType Directory -Path $addinFolder -Force | Out-Null

# Download production manifest from Spring Boot
Write-Host "Downloading manifest from https://localhost:8082..."
try {
    Invoke-WebRequest -Uri $manifestUrl -OutFile "$addinFolder\manifest.xml"
    Write-Host "Manifest saved to $addinFolder" -ForegroundColor Green
} catch {
    Write-Error "Cannot connect to https://localhost:8082. Make sure the server is running."
    exit 1
}

# Write registry entry (HKCU — no admin needed)
$regPath = "HKCU:\SOFTWARE\Microsoft\Office\16.0\WEF\TrustedCatalogs\$catalogId"
New-Item -Path $regPath -Force | Out-Null
Set-ItemProperty -Path $regPath -Name "Id"    -Value $catalogId
Set-ItemProperty -Path $regPath -Name "Url"   -Value $addinFolder
Set-ItemProperty -Path $regPath -Name "Flags" -Type DWord -Value 1

Write-Host ""
Write-Host "Registry entry created." -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Restart PowerPoint"
Write-Host "  2. Insert -> My Add-ins -> tab 'SHARED FOLDER'"
Write-Host "  3. Select 'Ассистент лектора' -> Add"
