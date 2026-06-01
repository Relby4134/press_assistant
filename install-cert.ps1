# Run as Administrator
# Installs the PresAssistant SSL certificate into Windows Trusted Root
# so PowerPoint accepts the HTTPS connection to localhost:8082

$logFile = "$env:TEMP\presassistant-cert-install.log"
Start-Transcript -Path $logFile -Force | Out-Null

Write-Host "PSScriptRoot: $PSScriptRoot"
Write-Host "Working dir: $(Get-Location)"

$certFile = Join-Path $PSScriptRoot "presassistant-ca.crt"
Write-Host "Looking for cert: $certFile"

if (-not (Test-Path $certFile)) {
    Write-Error "Certificate file not found: $certFile"
    Stop-Transcript | Out-Null
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
    Write-Host "Certificate installed successfully."
    Stop-Transcript | Out-Null
} else {
    Write-Error "Failed to install certificate. Make sure you run this script as Administrator."
    Stop-Transcript | Out-Null
    exit 1
}
