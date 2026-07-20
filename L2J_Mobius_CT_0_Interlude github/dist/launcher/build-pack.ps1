<#
  build-pack.ps1  -  assemble the "install nothing" one-click pack (run on Windows)
  -------------------------------------------------------------------------------
  Produces a single zip a player can unzip on a bare Windows PC and run by
  double-clicking Start-Server.bat - no JDK, no XAMPP, no manual DB install.

  It bundles:
    * the built server (GameServer.jar + LoginServer.jar + full dist)  <- from 'ant'
    * a FULL JDK 25 at  pack\jre\        (full JDK, not a JRE - the server compiles
                                          datapack scripts at runtime and needs javac)
    * a portable MariaDB at  pack\mariadb\
    * the launcher (already part of dist\)

  RUN THIS ON your dev/live Windows PC (it needs JDK 25 + Ant to build the jars).
  The resulting zip is what you copy to the clean test VM.

  Usage examples (from dist\launcher\):
    powershell -ExecutionPolicy Bypass -File build-pack.ps1
    powershell -ExecutionPolicy Bypass -File build-pack.ps1 -SkipBuild
    powershell -ExecutionPolicy Bypass -File build-pack.ps1 -MariaDbZip C:\dl\mariadb-11.4.5-winx64.zip
#>

param(
    [string]$JdkHome    = $env:JAVA_HOME,                     # full JDK 25 to bundle
    [string]$MariaDbZip = '',                                 # local MariaDB winx64 zip (skip download)
    [string]$MariaDbVersion = '11.4.5',                      # used to build the download URL
    [string]$MariaDbUrl = '',                                 # override the download URL entirely
    [string]$OutDir     = '',                                 # where to write the final zip (default: repo root)
    [switch]$SkipBuild                                        # reuse an existing build\...zip instead of running ant
)

$ErrorActionPreference = 'Stop'
function Info($t) { Write-Host "[build] $t" -ForegroundColor Cyan }
function Ok($t)   { Write-Host "[ ok  ] $t" -ForegroundColor Green }
function Die($t)  { Write-Host "[fail ] $t" -ForegroundColor Red; exit 1 }

# Robust recursive copy of a whole directory tree. Copy-Item -Recurse with a
# wildcard source has a long-standing bug on large trees ("Container cannot be
# copied onto existing leaf item"), so use robocopy, which is built for this.
# robocopy exit codes < 8 are success (0=nothing to do, 1=copied, etc.).
function Copy-Tree($src, $dst) {
    New-Item -ItemType Directory -Path $dst -Force | Out-Null
    robocopy $src $dst /E /NFL /NDL /NJH /NJS /NP /R:1 /W:1 | Out-Null
    $code = $LASTEXITCODE
    $global:LASTEXITCODE = 0   # reset so a robocopy "success" code isn't seen as failure later
    if ($code -ge 8) { Die "copy failed (robocopy code $code): $src -> $dst" }
}

# ---- locate folders --------------------------------------------------------
$LauncherDir = Split-Path -Parent $MyInvocation.MyCommand.Path       # ...\dist\launcher
$DistDir     = Split-Path -Parent $LauncherDir                        # ...\dist
$ProjectRoot = Split-Path -Parent $DistDir                            # ...\L2J_Mobius_CT_0_Interlude github
$BuildXml    = Join-Path $ProjectRoot 'build.xml'
if (-not (Test-Path $BuildXml)) { Die "build.xml not found at $BuildXml - run this from dist\launcher\ inside the project." }
if ($OutDir -eq '') { $OutDir = $ProjectRoot }

$Staging  = Join-Path $env:TEMP ("l2pack_" + [DateTime]::Now.ToString('yyyyMMdd_HHmmss'))
$Pack     = Join-Path $Staging 'pack'
New-Item -ItemType Directory -Path $Pack -Force | Out-Null
Info "staging at $Staging"

# ---- 1. validate the JDK (must be a FULL JDK with javac) -------------------
if (-not $JdkHome -or -not (Test-Path $JdkHome)) { Die "JDK not found. Pass -JdkHome or set JAVA_HOME to a JDK 25 install." }
if (-not (Test-Path (Join-Path $JdkHome 'bin\javac.exe'))) {
    Die "'$JdkHome' has no bin\javac.exe - that's a JRE, not a JDK. The server compiles datapack scripts at runtime and needs a full JDK 25."
}
Ok "JDK to bundle: $JdkHome"

# ---- 2. build the server jars (ant) ---------------------------------------
# build.xml sets build="../build", so the zip lands one level ABOVE the project
# folder (at the git-repo root). Check that first, with a fallback for other layouts.
$zipName = 'L2J_Mobius_CT_0_Interlude.zip'
$zipCandidates = @(
    (Join-Path (Split-Path -Parent $ProjectRoot) (Join-Path 'build' $zipName)),  # ../build (per build.xml)
    (Join-Path $ProjectRoot (Join-Path 'build' $zipName))                          # ./build (fallback)
)
function Find-ServerZip { foreach ($c in $zipCandidates) { if (Test-Path $c) { return $c } } return $null }

if ($SkipBuild) {
    $serverZip = Find-ServerZip
    if (-not $serverZip) { Die "-SkipBuild set but no build zip found (looked in: $($zipCandidates -join '; ')). Run without -SkipBuild once." }
    Ok "reusing existing build: $serverZip"
} else {
    $ant = Get-Command ant.bat -ErrorAction SilentlyContinue
    if (-not $ant) { $ant = Get-Command ant -ErrorAction SilentlyContinue }
    if (-not $ant) { Die "Ant not found on PATH. Install Ant (or pass -SkipBuild to reuse an existing build\...zip)." }
    Info "running ant (this compiles the jars) ..."
    Push-Location $ProjectRoot
    try { & $ant.Source } finally { Pop-Location }
    if ($LASTEXITCODE -ne 0) { Die "ant build failed (exit $LASTEXITCODE)." }
    $serverZip = Find-ServerZip
    if (-not $serverZip) { Die "ant finished but no build zip found (looked in: $($zipCandidates -join '; '))." }
    Ok "server built: $serverZip"
}

# ---- 3. lay out the server (the ant zip already = a full dist\) ------------
Info "extracting server into pack ..."
Expand-Archive -Path $serverZip -DestinationPath $Pack -Force
if (-not (Test-Path (Join-Path $Pack 'libs\GameServer.jar'))) { Die "extracted pack is missing libs\GameServer.jar - build output unexpected." }
Ok "server staged"

# ---- 4. bundle the full JDK ------------------------------------------------
Info "copying JDK into pack\jre (this is the big one) ..."
Copy-Tree $JdkHome (Join-Path $Pack 'jre')
if (-not (Test-Path (Join-Path $Pack 'jre\bin\java.exe'))) { Die "JDK copy failed - pack\jre\bin\java.exe missing." }
Ok "JDK bundled"

# ---- 5. bundle portable MariaDB -------------------------------------------
$mdDir = Join-Path $Pack 'mariadb'
if ($MariaDbZip -eq '') {
    if ($MariaDbUrl -eq '') {
        $MariaDbUrl = "https://archive.mariadb.org/mariadb-$MariaDbVersion/winx64-packages/mariadb-$MariaDbVersion-winx64.zip"
    }
    $MariaDbZip = Join-Path $Staging 'mariadb.zip'
    Info "downloading MariaDB $MariaDbVersion ..."
    try { Invoke-WebRequest -Uri $MariaDbUrl -OutFile $MariaDbZip -UseBasicParsing }
    catch { Die "MariaDB download failed from $MariaDbUrl. Download the winx64 zip manually and re-run with -MariaDbZip <path>." }
}
if (-not (Test-Path $MariaDbZip)) { Die "MariaDB zip not found at $MariaDbZip." }
Info "extracting MariaDB ..."
$mdTmp = Join-Path $Staging 'md'
Expand-Archive -Path $MariaDbZip -DestinationPath $mdTmp -Force
# the zip contains a single top folder like mariadb-11.4.5-winx64\ - flatten it into pack\mariadb
$inner = Get-ChildItem -Path $mdTmp -Directory | Select-Object -First 1
if (-not $inner) { Die "unexpected MariaDB zip layout (no inner folder)." }
Copy-Tree $inner.FullName $mdDir
if (-not (Test-Path (Join-Path $mdDir 'bin\mysqld.exe'))) { Die "MariaDB bundle missing bin\mysqld.exe." }
Ok "MariaDB bundled ($($inner.Name))"

# ---- 6. configure the pack's launcher.ini + Database.ini -------------------
Info "wiring the pack to use the bundled JDK + MariaDB ..."
$iniPath = Join-Path $Pack 'launcher\launcher.ini'
$ini = Get-Content -Raw $iniPath
$ini = $ini -replace '(?m)^MysqlBin=.*$',  'MysqlBin=mariadb\bin'
$ini = $ini -replace '(?m)^DataDir=.*$',   'DataDir=mariadb\data'
Set-Content -Path $iniPath -Value $ini -NoNewline
# point MySqlBinLocation (backup tooling) at the bundled bin; login+game already use root/blank pw.
foreach ($cfg in @('game\config\Database.ini','login\config\Database.ini')) {
    $p = Join-Path $Pack $cfg
    if (Test-Path $p) {
        (Get-Content -Raw $p) -replace '(?m)^MySqlBinLocation\s*=.*$', 'MySqlBinLocation = ../../mariadb/bin/' |
            Set-Content -Path $p -NoNewline
    }
}
# a first-run marker must NOT be present in a shipped pack (empty DB must install)
Remove-Item -Path (Join-Path $Pack 'launcher\.db_installed') -ErrorAction SilentlyContinue
# strip the dev-only builder from the shipped pack so players only ever see Start-Server.bat
foreach ($devFile in @('launcher\build-pack.ps1','launcher\build-pack.bat')) {
    Remove-Item -Path (Join-Path $Pack $devFile) -ErrorAction SilentlyContinue
}
Ok "pack configured"

# ---- 6b. bundle the OPTIONAL FPC brain (AI chat) into pack\brain -----------
# The brain is a small local Flask service the game server talks to for
# in-character bot chat. It is OFF by default and needs Python (+ either a local
# Ollama model or a DeepSeek API key), so it is not part of the "installs
# nothing" core - it is bundled here so a tester can turn it on if they want.
# setup_brain.bat loads fpc_brain.py + knowledge\ from its own folder, so they
# must travel together; the launcher looks for dist\brain\setup_brain.bat.
Info "bundling the optional FPC brain into pack\brain ..."
$brainDst = Join-Path $Pack 'brain'
New-Item -ItemType Directory -Path $brainDst -Force | Out-Null
foreach ($f in @('fpc_brain.py', 'requirements.txt', 'setup_brain.bat', 'setup_brain.sh')) {
    $srcF = Join-Path $ProjectRoot $f
    if (Test-Path $srcF) { Copy-Item $srcF -Destination $brainDst -Force }
}
$knowSrc = Join-Path $ProjectRoot 'knowledge'
if (Test-Path $knowSrc) { Copy-Tree $knowSrc (Join-Path $brainDst 'knowledge') }

# A short player-facing readme for the bundled brain.
$brainReadme = @'
# Optional AI chat brain

The bots in this server can hold in-character chat (whisper / say / trade / shout)
through a small local service called the "brain". It is **optional and off by
default** - the server and all the bots work fully without it; this only adds
the talking.

## What you need

`setup_brain.bat` sets everything up for you - it installs Python automatically
if it isn't already present. You just choose how the bots "think":

- **Ollama** - a free local model that runs on your own PC (needs a decent
  GPU/CPU; the setup installs Ollama and downloads a few GB the first time), or
- **DeepSeek** - a cloud API (works on any PC, needs an API key you paste in).

## Turn it on

1. Double-click **setup_brain.bat** in this folder. It installs Python if needed,
   asks whether to use Ollama or DeepSeek, sets everything up, and starts the
   brain on http://127.0.0.1:5000.
2. To have the launcher start the brain automatically with the server instead,
   set `StartBrain=true` in `launcher\launcher.ini`.

That's it - once the brain is running, the bots start chatting.

> If Python was just installed for the first time, Windows may need a fresh
> Command Prompt to see it - if the script says so, just double-click
> setup_brain.bat again.
'@
Set-Content -Path (Join-Path $brainDst 'README.md') -Value $brainReadme -Encoding UTF8
Ok "brain bundled (optional; needs Python + a model or API key to run)"

# ---- 6c. bundle the FPC editor (visual bot-data editor) into pack\tools -----
$editorSrc = Join-Path $ProjectRoot 'tools\fpc-editor'
if (Test-Path $editorSrc) {
    Info "bundling the FPC editor into pack\tools\fpc-editor ..."
    Copy-Tree $editorSrc (Join-Path $Pack 'tools\fpc-editor')
    Ok "FPC editor bundled (open tools\fpc-editor\index.html in a browser)"
}

# ---- 7. zip it -------------------------------------------------------------
$outZip = Join-Path $OutDir 'L2J-Offline-OneClick.zip'
if (Test-Path $outZip) { Remove-Item $outZip -Force }
Info "compressing final pack (large - please wait) ..."
# .NET ZipFile handles the ~0.5 GB / tens-of-thousands-of-files JDK tree far better
# than Compress-Archive. Contents land at the zip root (no extra parent folder).
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory($Pack, $outZip, [System.IO.Compression.CompressionLevel]::Optimal, $false)
$sizeMb = [math]::Round((Get-Item $outZip).Length / 1MB, 1)
Ok "DONE -> $outZip  ($sizeMb MB)"
Write-Host ""
Write-Host "Copy that zip to the test VM, unzip anywhere, and double-click Start-Server.bat." -ForegroundColor Green
Write-Host "Cleaning staging..." -ForegroundColor Gray
Remove-Item -Path $Staging -Recurse -Force -ErrorAction SilentlyContinue
