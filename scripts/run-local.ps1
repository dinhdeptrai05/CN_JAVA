param([switch]$Smoke)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $projectRoot
$configPath=Join-Path $projectRoot '.local/mysql-test/connection.json'
if(!(Test-Path -LiteralPath $configPath)){throw 'Local MySQL configuration was not found. See README_DATABASE.md.'}
$settings=Get-Content -Raw -LiteralPath $configPath | ConvertFrom-Json
$mysqlBase=Join-Path $projectRoot '.local/mysql-test/mysql-8.4.6-winx64'
$rootConfig=Join-Path $projectRoot '.local/mysql-test/root.cnf'
& "$mysqlBase/bin/mysqladmin.exe" "--defaults-extra-file=$rootConfig" ping --silent 2>$null | Out-Null
if($LASTEXITCODE -ne 0) {
    $mysqlData=Join-Path $projectRoot '.local/mysql-test/data'
    $arguments=@('--no-defaults',('"--basedir='+$mysqlBase+'"'),('"--datadir='+$mysqlData+'"'),'--bind-address=127.0.0.1','--port=3307','--mysqlx=OFF','--console')
    Start-Process -FilePath "$mysqlBase/bin/mysqld.exe" -ArgumentList $arguments -WindowStyle Hidden -RedirectStandardOutput "$projectRoot/.local/mysql-test/server.out.log" -RedirectStandardError "$projectRoot/.local/mysql-test/server.err.log" | Out-Null
    for($attempt=0;$attempt -lt 30;$attempt++) {
        Start-Sleep -Milliseconds 500
        & "$mysqlBase/bin/mysqladmin.exe" "--defaults-extra-file=$rootConfig" ping --silent 2>$null | Out-Null
        if($LASTEXITCODE -eq 0){break}
    }
    if($LASTEXITCODE -ne 0){throw 'Could not start project MySQL. See .local/mysql-test/server.err.log'}
}
$env:UNISCHEDULE_DB_URL=$settings.url
$env:UNISCHEDULE_DB_USER=$settings.username
$env:UNISCHEDULE_DB_PASSWORD=$settings.password
if($Smoke){mvn -q compile exec:java '-Dexec.args=--smoke'}else{mvn -q compile exec:java}
exit $LASTEXITCODE
