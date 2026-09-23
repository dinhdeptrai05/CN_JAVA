param([switch]$Preview, [switch]$DryRun, [switch]$Verify)
$ErrorActionPreference='Stop'
if((@($Preview,$DryRun,$Verify) | Where-Object {$_}).Count -gt 1){throw 'Choose one of -Preview, -DryRun or -Verify.'}
Push-Location -LiteralPath (Split-Path $PSScriptRoot -Parent)
$oldUrl=$env:UNISCHEDULE_DB_URL
$oldUser=$env:UNISCHEDULE_DB_USER
$oldPassword=$env:UNISCHEDULE_DB_PASSWORD
try {
    $path=Join-Path $PWD '.local/mysql-test/connection.json'
    if(!(Test-Path -LiteralPath $path)){throw 'Project MySQL configuration not found.'}
    $settings=Get-Content -Raw -LiteralPath $path | ConvertFrom-Json
    $env:UNISCHEDULE_DB_URL=$settings.url
    $env:UNISCHEDULE_DB_USER=$settings.username
    $env:UNISCHEDULE_DB_PASSWORD=$settings.password
    $mode=if($Preview){''}elseif($DryRun){'--dry-run'}elseif($Verify){'--verify'}else{'--apply'}
    & mvn -q compile exec:java '-Dexec.mainClass=vn.edu.donga.unischedule.simulation.FourDayTimetableSeed' "-Dexec.args=$mode"
    if($LASTEXITCODE -ne 0){throw 'Four-day room timetable seed failed.'}
} finally {
    $env:UNISCHEDULE_DB_URL=$oldUrl
    $env:UNISCHEDULE_DB_USER=$oldUser
    $env:UNISCHEDULE_DB_PASSWORD=$oldPassword
    Pop-Location
}
