# Run as Administrator
# Installs the PresAssistant SSL certificate into Windows Trusted Root
# so PowerPoint accepts the HTTPS connection to localhost:8082

$certFile = Join-Path $PSScriptRoot "presassistant-ca.crt"

if (-not (Test-Path $certFile)) {
    Write-Error "Certificate file not found: $certFile"
    exit 1
}

# Remove any old PresAssistant certs first
$old = Get-ChildItem Cert:\LocalMachine\Root | Where-Object { $_.Subject -like '*PresAssistant*' }
foreach ($c in $old) {
    Write-Host "Removing old cert: $($c.Thumbprint)"
    Remove-Item $c.PSPath -Force
}

Write-Host "Installing PresAssistant SSL certificate to Trusted Root..."
certutil -addstore -f "Root" $certFile

if ($LASTEXITCODE -eq 0) {
    Write-Host "Certificate installed successfully." -ForegroundColor Green
} else {
    Write-Error "Failed to install certificate. Make sure you run this script as Administrator."
    exit 1
}

# Add localhost to Trusted Sites zone so WebView2/Office trusts HTTPS on port 8082
$zoneKey = "HKCU:\Software\Microsoft\Windows\CurrentVersion\Internet Settings\ZoneMap\Domains\localhost"
New-Item -Path $zoneKey -Force | Out-Null
Set-ItemProperty -Path $zoneKey -Name "https" -Type DWord -Value 2
Write-Host "localhost added to Trusted Sites." -ForegroundColor Green
Write-Host "Restart PowerPoint to apply changes."
