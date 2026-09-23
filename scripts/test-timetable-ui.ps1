$ErrorActionPreference='Stop'
Push-Location -LiteralPath (Split-Path $PSScriptRoot -Parent)
$oldUrl=$env:UNISCHEDULE_DB_URL
$oldUser=$env:UNISCHEDULE_DB_USER
$oldPassword=$env:UNISCHEDULE_DB_PASSWORD
$oldTest=$env:UNISCHEDULE_HISTORY_UI_TEST
try {
    $path=Join-Path $PWD '.local/mysql-test/connection.json'
    if(!(Test-Path -LiteralPath $path)){throw 'Project MySQL configuration not found.'}
    $settings=Get-Content -Raw -LiteralPath $path | ConvertFrom-Json
    $env:UNISCHEDULE_DB_URL=$settings.url
    $env:UNISCHEDULE_DB_USER=$settings.username
    $env:UNISCHEDULE_DB_PASSWORD=$settings.password
    $env:UNISCHEDULE_HISTORY_UI_TEST='1'
    & mvn -q '-Dtest=TimetableHistoryIntegrationTest,TimetablePeriodTest' test
    if($LASTEXITCODE -ne 0){throw 'Historical timetable UI verification failed.'}
} finally {
    $env:UNISCHEDULE_DB_URL=$oldUrl
    $env:UNISCHEDULE_DB_USER=$oldUser
    $env:UNISCHEDULE_DB_PASSWORD=$oldPassword
    $env:UNISCHEDULE_HISTORY_UI_TEST=$oldTest
    Pop-Location
}
