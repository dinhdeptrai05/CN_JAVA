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
    $mode=if($Preview){'--forward-preview'}elseif($DryRun){'--forward-dry-run'}elseif($Verify){'--forward-verify'}else{'--forward-apply'}
    & mvn -q compile exec:java '-Dexec.mainClass=vn.edu.donga.unischedule.simulation.FourDayTimetableSeed' "-Dexec.args=$mode"
    if($LASTEXITCODE -ne 0){throw 'Forward five-year timetable seed failed.'}
    $activityMode=if($Preview){'preview'}elseif($DryRun){'dry-run'}elseif($Verify){'verify'}else{'apply'}
    & mvn -q exec:java '-Dexec.mainClass=vn.edu.donga.unischedule.simulation.ForwardActivitySeed' "-Dexec.args=$activityMode"
    if($LASTEXITCODE -ne 0){throw 'Forward five-year activity seed failed.'}
    & mvn -q exec:java '-Dexec.mainClass=vn.edu.donga.unischedule.simulation.ForwardDateNormalizer' "-Dexec.args=$activityMode"
    if($LASTEXITCODE -ne 0){throw 'Forward timetable date normalization failed.'}
} finally {
    $env:UNISCHEDULE_DB_URL=$oldUrl
    $env:UNISCHEDULE_DB_USER=$oldUser
    $env:UNISCHEDULE_DB_PASSWORD=$oldPassword
    Pop-Location
}
