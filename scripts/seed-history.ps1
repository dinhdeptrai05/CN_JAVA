param(
    [switch]$Preview,
    [switch]$Verify,
    [string]$AsOf = (Get-Date -Format 'yyyy-MM-dd'),
    [long]$Seed = 20260923,
    [string]$Output = '',
    [switch]$UseEnvironment
)
$ErrorActionPreference = 'Stop'
Push-Location -LiteralPath (Split-Path $PSScriptRoot -Parent)
$savedUrl = $env:UNISCHEDULE_DB_URL
$savedUser = $env:UNISCHEDULE_DB_USER
$savedPassword = $env:UNISCHEDULE_DB_PASSWORD
try {
    if (!$Preview -and !$UseEnvironment) {
        $path = Join-Path $PWD '.local/mysql-test/connection.json'
        if (Test-Path -LiteralPath $path) {
            $settings = Get-Content -Raw -LiteralPath $path | ConvertFrom-Json
            $env:UNISCHEDULE_DB_URL = $settings.url
            $env:UNISCHEDULE_DB_USER = $settings.username
            $env:UNISCHEDULE_DB_PASSWORD = $settings.password
        }
    }
    $arguments = '--as-of ' + $AsOf + ' --seed ' + $Seed
    if ($Verify) { $arguments += ' --verify' }
    elseif (!$Preview) { $arguments += ' --apply' }
    if ($Output) { $arguments += ' --out "' + $Output + '"' }
    & mvn -q compile exec:java '-Dexec.mainClass=vn.edu.donga.unischedule.simulation.HistoricalSeedMain' "-Dexec.args=$arguments"
    if ($LASTEXITCODE -ne 0) { throw 'Historical seeding or verification failed; inspect output for commit status.' }
} finally {
    $env:UNISCHEDULE_DB_URL = $savedUrl
    $env:UNISCHEDULE_DB_USER = $savedUser
    $env:UNISCHEDULE_DB_PASSWORD = $savedPassword
    Pop-Location
}
