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
    Write-Host "You can now open PowerPoint and use the PresAssistant add-in."
} else {
    Write-Error "Failed to install certificate. Make sure you run this script as Administrator."
}
