<#
  L2 Offline Server - One-Click Launcher
  --------------------------------------
  Boots the whole stack from a single double-click:
     DB engine  ->  first-run schema install  ->  Login server  ->  Game server  ->  (optional) brain

  Works two ways, chosen by launcher.ini:
    * Bundled pack   - DataDir set => a portable MariaDB shipped inside the pack, initialized on
                       first run. Combined with a bundled JDK at dist\jre\, the player installs nothing.
    * External DB    - DataDir blank => uses an existing MySQL/XAMPP (auto-starts it if needed).

  Driven by Start-Server.bat in the parent (dist) folder. Nothing here touches game logic -
  it only orchestrates the pieces that already exist.
#>

$ErrorActionPreference = 'Stop'

# ---- paths -----------------------------------------------------------------
$LauncherDir = Split-Path -Parent $MyInvocation.MyCommand.Path      # dist\launcher
$DistDir     = Split-Path -Parent $LauncherDir                       # dist
$IniPath     = Join-Path $LauncherDir 'launcher.ini'
$MarkerPath  = Join-Path $LauncherDir '.db_installed'

function Write-Head($t) { Write-Host ""; Write-Host "==== $t ====" -ForegroundColor Cyan }
function Write-Ok($t)   { Write-Host "  [ OK ] $t" -ForegroundColor Green }
function Write-Info($t) { Write-Host "  [info] $t" -ForegroundColor Gray }
function Fail($t)       { Write-Host ""; Write-Host "  [FAIL] $t" -ForegroundColor Red; Write-Host ""; exit 1 }

# ---- tiny INI parser -------------------------------------------------------
function Read-Ini($path) {
    $ini = @{}; $section = ''
    foreach ($line in Get-Content $path) {
        $l = $line.Trim()
        if ($l -eq '' -or $l.StartsWith('#') -or $l.StartsWith(';')) { continue }
        if ($l -match '^\[(.+)\]$') { $section = $Matches[1]; $ini[$section] = @{}; continue }
        $idx = $l.IndexOf('=')
        if ($idx -lt 0) { continue }
        $key = $l.Substring(0, $idx).Trim()
        $val = $l.Substring($idx + 1).Trim()
        if ($section -eq '') { continue }
        $ini[$section][$key] = $val
    }
    return $ini
}

function Get-Ini($ini, $sec, $key, $default = '') {
    if ($ini.ContainsKey($sec) -and $ini[$sec].ContainsKey($key) -and $ini[$sec][$key] -ne '') { return $ini[$sec][$key] }
    return $default
}
function Is-True($v) { return @('true','1','yes','on') -contains ("$v").ToLower() }

# ---- helpers ---------------------------------------------------------------
function Test-Port($p) {
    try {
        $c = New-Object System.Net.Sockets.TcpClient
        $iar = $c.BeginConnect('127.0.0.1', [int]$p, $null, $null)
        $ok = $iar.AsyncWaitHandle.WaitOne(600)
        if ($ok -and $c.Connected) { $c.EndConnect($iar); $c.Close(); return $true }
        $c.Close(); return $false
    } catch { return $false }
}

function Wait-Port($p, $timeoutSec) {
    for ($i = 0; $i -lt $timeoutSec; $i++) {
        if (Test-Port $p) { return $true }
        Start-Sleep -Seconds 1
    }
    return $false
}

# ---- find java (returns $null if not found - never exits, so pre-flight can
#      aggregate every missing item into one screen) ---------------------------
function Find-Java($javaHome) {
    if ($javaHome -ne '') {
        $cand = Join-Path $javaHome 'bin\java.exe'
        if (Test-Path $cand) { return $cand }
        return $null
    }
    $bundled = Join-Path $DistDir 'jre\bin\java.exe'
    if (Test-Path $bundled) { return $bundled }
    if ($env:JAVA_HOME) {
        $cand = Join-Path $env:JAVA_HOME 'bin\java.exe'
        if (Test-Path $cand) { return $cand }
    }
    $onPath = (Get-Command java.exe -ErrorAction SilentlyContinue)
    if ($onPath) { return $onPath.Source }
    return $null
}

# ============================================================================
Clear-Host
Write-Host "########################################################" -ForegroundColor Magenta
Write-Host "#   L2 Offline 'Living World' - One-Click Launcher     #" -ForegroundColor Magenta
Write-Host "########################################################" -ForegroundColor Magenta

if (-not (Test-Path $IniPath)) { Fail "launcher.ini not found at $IniPath" }
$ini = Read-Ini $IniPath

$javaHome  = Get-Ini $ini 'java'     'JavaHome' ''
$dbHost    = Get-Ini $ini 'database' 'Host'     'localhost'
$dbPort    = Get-Ini $ini 'database' 'Port'     '3306'
$dbUser    = Get-Ini $ini 'database' 'User'     'root'
$dbPass    = Get-Ini $ini 'database' 'Password' ''
$dbName    = Get-Ini $ini 'database' 'Database' 'l2jmobiusinterlude'
$mysqlBin  = Get-Ini $ini 'database' 'MysqlBin' 'C:\xampp\mysql\bin'
$dataDir   = Get-Ini $ini 'database' 'DataDir'  ''
$autoMysql = Is-True (Get-Ini $ini 'database' 'AutoStartMysql' 'true')
$startLogin= Is-True (Get-Ini $ini 'servers'  'StartLogin' 'true')
$startGame = Is-True (Get-Ini $ini 'servers'  'StartGame'  'true')
$startBrain= Is-True (Get-Ini $ini 'servers'  'StartBrain' 'false')

# Resolve relative MysqlBin / DataDir against dist\ so the bundled pack is portable.
function Resolve-Rel($p) {
    if ($p -eq '') { return '' }
    if ([System.IO.Path]::IsPathRooted($p)) { return $p }
    return (Join-Path $DistDir $p)
}
$mysqlBin = Resolve-Rel $mysqlBin
if ($dataDir -ne '') { $dataDir = Resolve-Rel $dataDir }
$bundledDb = ($dataDir -ne '')   # bundled MariaDB mode when a data dir is configured

$mysqlExe = Join-Path $mysqlBin 'mysql.exe'
$mysqldExe= Join-Path $mysqlBin 'mysqld.exe'

# ---- 0. Pre-flight ---------------------------------------------------------
# One screen up front listing anything that is missing, instead of failing
# three separate steps in. Required things stop the run; the rest is info.
Write-Head "0/4  Pre-flight check"
$problems = @()
$javaProbe = Find-Java $javaHome
if ($javaProbe) { Write-Ok "Java found: $javaProbe" }
else { $problems += "Java (JDK 25) not found - you are running the raw launcher, not the bundled pack. Use the pack (it ships Java at dist\jre\), or set JavaHome / install JDK 25 for a manual run." }

$dbUp = Test-Port $dbPort
if ($dbUp) {
    Write-Ok "a database is already running on port $dbPort"
} elseif (-not (Test-Path $mysqldExe)) {
    $problems += "Database engine not found: expected mysqld.exe at $mysqldExe (fix MysqlBin in launcher.ini, or bundle MariaDB)."
} else {
    Write-Ok "database engine present: $mysqldExe"
}

if ($startLogin -and -not (Test-Path (Join-Path $DistDir 'login\..\libs\LoginServer.jar'))) {
    $problems += "LoginServer.jar missing from libs\ - build with 'ant' and include it in the pack."
}
if ($startGame -and -not (Test-Path (Join-Path $DistDir 'game\..\libs\GameServer.jar'))) {
    $problems += "GameServer.jar missing from libs\ - build with 'ant' and include it in the pack."
}

if ($problems.Count -gt 0) {
    Write-Host ""
    Write-Host "  Cannot start yet - the following are missing:" -ForegroundColor Yellow
    foreach ($p in $problems) { Write-Host "    - $p" -ForegroundColor Yellow }
    Fail "Resolve the items above and run again. (A fully bundled pack ships Java + MariaDB + jars so none of this is needed.)"
}

# ---- 1. Java ---------------------------------------------------------------
Write-Head "1/4  Java runtime"
$java = Find-Java $javaHome
if (-not $java) { Fail "Could not find Java. Use the bundled pack, set JavaHome in launcher.ini, or install JDK 25." }
Write-Ok "java: $java"

# ---- 2. Database engine ----------------------------------------------------
Write-Head "2/4  Database (MySQL/MariaDB)"

if (Test-Port $dbPort) {
    Write-Ok "database already running on port $dbPort"
} elseif ($autoMysql) {
    if (-not (Test-Path $mysqldExe)) { Fail "DB not running and mysqld.exe not found at $mysqldExe. Fix MysqlBin in launcher.ini." }

    if ($bundledDb) {
        # Bundled/portable MariaDB: initialize the data dir on first run, then start with --datadir.
        $needInit = -not (Test-Path (Join-Path $dataDir 'mysql')) -and -not (Test-Path (Join-Path $dataDir 'ibdata1'))
        if ($needInit) {
            Write-Info "first run - initializing bundled MariaDB data dir at $dataDir ..."
            # Start from a clean data dir - a partial dir left by a failed attempt breaks init.
            if (Test-Path $dataDir) { Remove-Item $dataDir -Recurse -Force -ErrorAction SilentlyContinue }
            New-Item -ItemType Directory -Path $dataDir -Force | Out-Null
            $installExe = $null
            foreach ($n in @('mariadb-install-db.exe','mysql_install_db.exe')) {
                $c = Join-Path $mysqlBin $n
                if (Test-Path $c) { $installExe = $c; break }
            }
            if (-not $installExe) { Fail "Bundled MariaDB is missing mariadb-install-db.exe / mysql_install_db.exe in $mysqlBin." }
            # NOTE: the Windows mariadb-install-db.exe only really accepts --datadir - it
            # auto-detects its base folder and REJECTS --basedir and the Linux-only
            # --auth-root-authentication-method. A plain init already creates root@localhost
            # with an EMPTY password, which is what Database.ini expects. Relax
            # ErrorActionPreference for the call because the tool logs progress to stderr,
            # which would otherwise be treated as fatal.
            $prevEAP = $ErrorActionPreference
            $ErrorActionPreference = 'Continue'
            & $installExe "--datadir=$dataDir" 2>&1 | ForEach-Object { Write-Host "    $_" }
            $code = $LASTEXITCODE
            $ErrorActionPreference = $prevEAP
            if ($code -ne 0) { Fail "MariaDB data dir initialization failed (code $code). See messages above." }
            Write-Ok "MariaDB data dir initialized"
        }
        Write-Info "starting bundled MariaDB (port $dbPort) ..."
        # Pass a single, explicitly-quoted argument string: Start-Process -ArgumentList
        # with an ARRAY does not quote elements, so a data dir path containing spaces
        # (e.g. "New folder") gets split and mysqld fails to start. Quoting fixes that.
        $mysqldArgLine = "--datadir=`"$dataDir`" --port=$dbPort --skip-name-resolve --console"
        Start-Process -FilePath $mysqldExe -WorkingDirectory $mysqlBin `
            -ArgumentList $mysqldArgLine -WindowStyle Minimized | Out-Null
    } else {
        # External engine (XAMPP etc.): start it with its own configured data dir.
        Write-Info "starting mysqld from $mysqlBin ..."
        Start-Process -FilePath $mysqldExe -WorkingDirectory $mysqlBin -WindowStyle Minimized | Out-Null
    }

    if (Wait-Port $dbPort 60) { Write-Ok "database is up on port $dbPort" }
    else { Fail "the database did not open port $dbPort within 60s. Check its (minimized) console window for errors, and avoid folder paths with spaces." }
} else {
    Fail "Nothing is listening on port $dbPort and AutoStartMysql=false. Start your DB and retry."
}

# ---- 3. First-run schema install ------------------------------------------
Write-Head "3/4  Database schema"
if (Test-Path $MarkerPath) {
    Write-Ok "already installed (delete $($MarkerPath | Split-Path -Leaf) in launcher\ to reinstall)"
} else {
    if (-not (Test-Path $mysqlExe)) { Fail "First-run install needs mysql.exe at $mysqlExe. Fix MysqlBin in launcher.ini." }
    $skipInstall = $false

    # The mysql client prints a harmless stderr warning on a passwordless login
    # ("--ssl-verify-server-cert is disabled ..."). Under ErrorActionPreference=Stop
    # that stderr write aborts the script, so relax it for the DB-client calls below.
    # Real failures are still caught via $LASTEXITCODE after each call.
    $ErrorActionPreference = 'Continue'

    $mysqlArgs = @('-h', $dbHost, '-P', $dbPort, '-u', $dbUser)
    if ($dbPass -ne '') { $mysqlArgs += "--password=$dbPass" }

    # SAFETY: never run the schema import over a database that already has tables -
    # a number of the SQL files DROP TABLE before recreating (e.g. accounts), which
    # would wipe an existing server. If the DB already has tables, skip and mark done.
    $existingCount = (& $mysqlExe @mysqlArgs '-N' '-B' '-e' "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$dbName';" 2>$null)
    if ($LASTEXITCODE -ne 0) { Fail "Could not connect to the database. Check DB credentials in launcher.ini." }
    $existingCount = ("$existingCount").Trim()
    if ($existingCount -match '^\d+$' -and [int]$existingCount -gt 0) {
        Write-Ok "existing database '$dbName' detected ($existingCount tables) - skipping install to protect your data"
        Set-Content -Path $MarkerPath -Value "pre-existing db, install skipped $(Get-Date -Format s)"
        # fall through to server launch
        $skipInstall = $true
    }

    if (-not $skipInstall) {
    Write-Info "first run detected, empty database - installing '$dbName' schema ..."

    # create database
    & $mysqlExe @mysqlArgs '-e' "CREATE DATABASE IF NOT EXISTS ``$dbName`` CHARACTER SET utf8 COLLATE utf8_unicode_ci;" 2>$null
    if ($LASTEXITCODE -ne 0) { Fail "Could not connect / create database. Check DB credentials in launcher.ini." }

    $dbArgs = $mysqlArgs + $dbName
    foreach ($group in @('login','game')) {
        $sqlDir = Join-Path $DistDir "db_installer\sql\$group"
        if (-not (Test-Path $sqlDir)) { Write-Info "no $group SQL folder, skipping"; continue }
        $files = Get-ChildItem -Path $sqlDir -Filter *.sql | Sort-Object Name
        Write-Info "$group : $($files.Count) tables"
        foreach ($f in $files) {
            Get-Content -Raw $f.FullName | & $mysqlExe @dbArgs 2>$null
            if ($LASTEXITCODE -ne 0) { Fail "Error importing $($f.Name)" }
        }
    }
    New-Item -ItemType File -Path $MarkerPath -Force | Out-Null
    Set-Content -Path $MarkerPath -Value "installed $(Get-Date -Format s)"
    Write-Ok "schema installed"
    } # end if (-not $skipInstall)
}

# ---- 4. Launch servers -----------------------------------------------------
Write-Head "4/4  Servers"

function Start-JavaServer($name, $workDir, $jarRelative) {
    $cfgPath = Join-Path $workDir 'java.cfg'
    $jarPath = Join-Path $workDir $jarRelative
    if (-not (Test-Path $jarPath)) { Fail "$name jar not found at $jarPath (did you build with 'ant' and copy the jar?)" }
    $params = ''
    if (Test-Path $cfgPath) { $params = (Get-Content -Raw $cfgPath).Trim() }
    $argLine = "$params -jar `"$jarRelative`""
    Write-Info "launching $name ..."
    Start-Process -FilePath $java -ArgumentList $argLine -WorkingDirectory $workDir | Out-Null
    Write-Ok "$name started"
}

if ($startLogin) {
    Start-JavaServer 'Login Server' (Join-Path $DistDir 'login') '..\libs\LoginServer.jar'
    Start-Sleep -Seconds 3   # let the login server bind before the game server registers
}
if ($startGame) {
    Start-JavaServer 'Game Server' (Join-Path $DistDir 'game') '..\libs\GameServer.jar'
}

if ($startBrain) {
    $brainBat = Join-Path (Split-Path -Parent $DistDir) 'setup_brain.bat'
    if (Test-Path $brainBat) {
        Write-Info "launching FPC brain ..."
        Start-Process -FilePath 'cmd.exe' -ArgumentList "/c `"$brainBat`"" | Out-Null
        Write-Ok "brain started"
    } else {
        Write-Info "StartBrain=true but setup_brain.bat not found next to dist\ - skipping"
    }
}

Write-Host ""
Write-Host "All requested components launched. Each server runs in its own window." -ForegroundColor Green
Write-Host "Close those windows (or run Stop-Server.bat) to shut down." -ForegroundColor Green
Write-Host ""
