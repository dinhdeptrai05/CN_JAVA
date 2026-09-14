$projectRoot=Split-Path $PSScriptRoot -Parent
$mysqlAdmin=Join-Path $projectRoot '.local/mysql-test/mysql-8.4.6-winx64/bin/mysqladmin.exe'
$rootConfig=Join-Path $projectRoot '.local/mysql-test/root.cnf'
& $mysqlAdmin "--defaults-extra-file=$rootConfig" shutdown
exit $LASTEXITCODE
