param(
    [int]$StartYear = 2027,
    [long]$Seed = 42,
    [string]$Baseline = 'simulation/baseline-2026-09-22.properties',
    [string]$Output = '',
    [string]$CaptureBaseline = ''
)
$ErrorActionPreference = 'Stop'
Push-Location -LiteralPath (Split-Path $PSScriptRoot -Parent)
try {
    if ($CaptureBaseline) {
        $settingsPath = Join-Path $PWD '.local/mysql-test/connection.json'
        if (Test-Path -LiteralPath $settingsPath) {
            $settings = Get-Content -Raw -LiteralPath $settingsPath | ConvertFrom-Json
            $savedUrl = $env:UNISCHEDULE_DB_URL
            $savedUser = $env:UNISCHEDULE_DB_USER
            $savedPassword = $env:UNISCHEDULE_DB_PASSWORD
            $env:UNISCHEDULE_DB_URL = $settings.url
            $env:UNISCHEDULE_DB_USER = $settings.username
            $env:UNISCHEDULE_DB_PASSWORD = $settings.password
            $restoreEnvironment = $true
        }
        $arguments = '--capture-baseline "' + $CaptureBaseline + '"'
    } else {
        if (!$Output) { $Output = 'simulation/results/' + $StartYear + '-seed' + $Seed + '-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') }
        $arguments = '--start-year ' + $StartYear + ' --seed ' + $Seed + ' --baseline "' + $Baseline + '" --out "' + $Output + '"'
    }
    & mvn -q compile exec:java '-Dexec.mainClass=vn.edu.donga.unischedule.simulation.SimulationMain' "-Dexec.args=$arguments"
    if ($LASTEXITCODE -ne 0) { throw 'Simulation failed. See the error above.' }
} finally {
    if ($restoreEnvironment) {
        $env:UNISCHEDULE_DB_URL = $savedUrl
        $env:UNISCHEDULE_DB_USER = $savedUser
        $env:UNISCHEDULE_DB_PASSWORD = $savedPassword
    }
    Pop-Location
}
