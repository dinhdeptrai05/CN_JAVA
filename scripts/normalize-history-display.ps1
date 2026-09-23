$ErrorActionPreference='Stop'
Push-Location -LiteralPath (Split-Path $PSScriptRoot -Parent)
$oldUrl=$env:UNISCHEDULE_DB_URL
$oldUser=$env:UNISCHEDULE_DB_USER
$oldPassword=$env:UNISCHEDULE_DB_PASSWORD
try {
    $settings=Get-Content -Raw -LiteralPath '.local/mysql-test/connection.json' | ConvertFrom-Json
    $env:UNISCHEDULE_DB_URL=$settings.url
    $env:UNISCHEDULE_DB_USER=$settings.username
    $env:UNISCHEDULE_DB_PASSWORD=$settings.password
    & mvn -q compile exec:java '-Dexec.mainClass=vn.edu.donga.unischedule.simulation.HistoricalDisplayNormalizer'
    if($LASTEXITCODE -ne 0){throw 'History display normalization failed.'}
} finally {
    $env:UNISCHEDULE_DB_URL=$oldUrl
    $env:UNISCHEDULE_DB_USER=$oldUser
    $env:UNISCHEDULE_DB_PASSWORD=$oldPassword
    Pop-Location
}
