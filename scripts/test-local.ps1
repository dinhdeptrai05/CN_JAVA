# Recreates ONLY the disposable unischedule_test schema on the project's server, port 3307.
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $projectRoot
$settings=Get-Content -Raw '.local/mysql-test/connection.json' | ConvertFrom-Json
if(!$settings.url.StartsWith('jdbc:mysql://127.0.0.1:3307/unischedule?')){throw 'Unexpected database target'}
$rootConfig=(Resolve-Path '.local/mysql-test/root.cnf').Path
$exists=& '.local/mysql-test/mysql-8.4.6-winx64/bin/mysql.exe' "--defaults-extra-file=$rootConfig" --batch --skip-column-names --execute="SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='unischedule_test';"
if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}
if($exists -eq '1') {
    & '.local/mysql-test/mysql-8.4.6-winx64/bin/mysql.exe' "--defaults-extra-file=$rootConfig" --execute='DROP DATABASE unischedule_test;'
    if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}
}
Start-Sleep -Milliseconds 750
$emptySchema=Join-Path $projectRoot '.local/mysql-test/data/unischedule_test'
# OneDrive can retain an empty schema directory after DROP DATABASE. Never remove files here.
if((Test-Path -LiteralPath $emptySchema) -and @(Get-ChildItem -Force -LiteralPath $emptySchema).Count -eq 0){Remove-Item -LiteralPath $emptySchema -Force}
& '.local/mysql-test/mysql-8.4.6-winx64/bin/mysql.exe' "--defaults-extra-file=$rootConfig" --execute='CREATE DATABASE unischedule_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;'
if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}
$env:UNISCHEDULE_DB_URL=$settings.url.Replace('/unischedule?','/unischedule_test?')
$env:UNISCHEDULE_DB_USER=$settings.username
$env:UNISCHEDULE_DB_PASSWORD=$settings.password
mvn -q clean compile exec:java '-Dexec.mainClass=vn.edu.donga.unischedule.config.DatabaseSetup' '-Dexec.args=--seed'
if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}
mvn test
exit $LASTEXITCODE
