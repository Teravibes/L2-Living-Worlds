@echo off
REM ===========================================================================
REM  One-step setup + launch for the FPC brain - local Ollama model or DeepSeek
REM  cloud API, your choice.
REM
REM  What it does:
REM    1. Asks which AI provider to use: Ollama (local/offline) or DeepSeek
REM       (cloud, needs an API key). Asked every run - press Enter to keep
REM       whatever you picked last time, or answer again to switch.
REM    2. Ollama chosen: installs Ollama if missing, starts its server, pulls
REM       the chosen chat model (default gemma3:12b).
REM       DeepSeek chosen: asks for (or reuses the saved) API key. Ollama is
REM       never installed/started/pulled on this path.
REM    3. Creates a Python virtualenv and installs requirements.
REM    4. Writes the local .env file with the resolved settings.
REM    5. Starts fpc_brain.py.
REM
REM  Usage:
REM    setup_brain.bat                                - normal run, asks for provider
REM    setup_brain.bat --reset                        - wipes the saved .env first, so
REM                                                      everything is asked fresh again
REM                                                      (including the DeepSeek key)
REM    set OLLAMA_MODEL=llama3.1 & setup_brain.bat     - override the Ollama model
REM ===========================================================================

setlocal enabledelayedexpansion
cd /d "%~dp0"

echo.
echo ==^> FPC brain setup
echo ==^> Working folder:
cd
echo.

REM --- Basic project file checks --------------------------------------------

if not exist fpc_brain.py (
    echo ERROR: fpc_brain.py not found in this folder.
    echo Put setup_brain.bat in the same folder as fpc_brain.py.
    pause
    exit /b 1
)

if not exist requirements.txt (
    echo ERROR: requirements.txt not found in this folder.
    echo.
    echo Create a file named requirements.txt with these lines:
    echo flask
    echo openai
    echo python-dotenv
    echo.
    pause
    exit /b 1
)

REM --- 0. --reset wipes the saved config so everything is asked fresh -------

if /i "%~1"=="--reset" (
    if exist .env (
        echo ==^> --reset: removing existing .env - you will be asked to reconfigure.
        del /f /q .env
    )
)

REM --- Load any existing configuration as defaults for the prompts below ----

set "EXIST_PROVIDER="
set "EXIST_MODEL="
set "EXIST_KEY="
if exist .env (
    for /f "usebackq tokens=1,* delims==" %%A in (".env") do (
        if /i "%%A"=="PROVIDER" set "EXIST_PROVIDER=%%B"
        if /i "%%A"=="OLLAMA_MODEL" set "EXIST_MODEL=%%B"
        if /i "%%A"=="DEEPSEEK_API_KEY" set "EXIST_KEY=%%B"
    )
)

REM --- 1. Ask which provider to use, every run --------------------------------

echo Choose an AI provider for the FPC brain:
echo   [O] Ollama   - local model, free, fully offline (needs a decent GPU/CPU)
echo   [D] DeepSeek - cloud API, needs an API key, works on any PC
echo.

set "PROVIDER_CHOICE="
if defined EXIST_PROVIDER (
    set /p "PROVIDER_CHOICE=Use Ollama or DeepSeek? [O/D] (Enter = keep '!EXIST_PROVIDER!'): "
) else (
    set /p "PROVIDER_CHOICE=Use Ollama or DeepSeek? [O/D]: "
)

if "!PROVIDER_CHOICE!"=="" (
    if defined EXIST_PROVIDER (
        set "PROVIDER=!EXIST_PROVIDER!"
    ) else (
        echo ERROR: You must choose O or D on first setup.
        pause
        exit /b 1
    )
) else if /i "!PROVIDER_CHOICE:~0,1!"=="O" (
    set "PROVIDER=ollama"
) else if /i "!PROVIDER_CHOICE:~0,1!"=="D" (
    set "PROVIDER=deepseek"
) else (
    echo ERROR: Please answer O or D.
    pause
    exit /b 1
)

echo ==^> Provider: !PROVIDER!
echo.

if /i "!PROVIDER!"=="deepseek" goto setup_deepseek
goto setup_ollama

:setup_deepseek
REM --- 2a. DeepSeek: reuse or ask for the API key, skip Ollama entirely ------

set "DEEPSEEK_API_KEY="
if defined EXIST_KEY (
    set /p "KEY_CHOICE=Use saved DeepSeek API key ending '...!EXIST_KEY:~-4!'? [Y/n]: "
    if /i not "!KEY_CHOICE:~0,1!"=="N" set "DEEPSEEK_API_KEY=!EXIST_KEY!"
)

if not defined DEEPSEEK_API_KEY (
    set /p "DEEPSEEK_API_KEY=Enter your DeepSeek API key: "
)

if "!DEEPSEEK_API_KEY!"=="" (
    echo ERROR: A DeepSeek API key is required when using DeepSeek.
    pause
    exit /b 1
)

echo ==^> DeepSeek configured.
goto after_provider_setup

:setup_ollama
REM --- 2b. Ollama: install if missing, start the server, pull the model ------

if "%OLLAMA_MODEL%"=="" (
    if defined EXIST_MODEL (
        set "OLLAMA_MODEL=!EXIST_MODEL!"
    ) else (
        set "OLLAMA_MODEL=gemma3:12b"
    )
)

echo ==^> Ollama model: !OLLAMA_MODEL!
echo.

REM Try common Ollama install paths first, in case PATH is not refreshed.
if exist "%LOCALAPPDATA%\Programs\Ollama\ollama.exe" (
    set "PATH=%LOCALAPPDATA%\Programs\Ollama;%PATH%"
)

if exist "%ProgramFiles%\Ollama\ollama.exe" (
    set "PATH=%ProgramFiles%\Ollama;%PATH%"
)

where ollama >nul 2>&1
if errorlevel 1 (
    echo ==^> Ollama not found.

    if exist "%~dp0OllamaSetup.exe" (
        echo ==^> Installing bundled OllamaSetup.exe...
        start /wait "" "%~dp0OllamaSetup.exe"
    ) else (
        where winget >nul 2>&1
        if errorlevel 1 (
            echo ==^> winget not available. Downloading Ollama installer directly...

            if not exist "%TEMP%\fpc_brain_setup" mkdir "%TEMP%\fpc_brain_setup"

            powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $url='https://ollama.com/download/OllamaSetup.exe'; $out=Join-Path $env:TEMP 'fpc_brain_setup\OllamaSetup.exe'; Invoke-WebRequest -Uri $url -OutFile $out"

            if errorlevel 1 (
                echo ERROR: Could not download Ollama installer.
                echo Check internet connection or download OllamaSetup.exe manually.
                pause
                exit /b 1
            )

            echo ==^> Running downloaded Ollama installer...
            start /wait "" "%TEMP%\fpc_brain_setup\OllamaSetup.exe"
        ) else (
            echo ==^> Installing Ollama via winget...
            winget install --id Ollama.Ollama -e --accept-package-agreements --accept-source-agreements

            if errorlevel 1 (
                echo WARNING: winget install failed. Trying direct download instead...

                if not exist "%TEMP%\fpc_brain_setup" mkdir "%TEMP%\fpc_brain_setup"

                powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $url='https://ollama.com/download/OllamaSetup.exe'; $out=Join-Path $env:TEMP 'fpc_brain_setup\OllamaSetup.exe'; Invoke-WebRequest -Uri $url -OutFile $out"

                if errorlevel 1 (
                    echo ERROR: Could not download Ollama installer.
                    pause
                    exit /b 1
                )

                echo ==^> Running downloaded Ollama installer...
                start /wait "" "%TEMP%\fpc_brain_setup\OllamaSetup.exe"
            )
        )
    )

    REM Refresh PATH after installer.
    if exist "%LOCALAPPDATA%\Programs\Ollama\ollama.exe" (
        set "PATH=%LOCALAPPDATA%\Programs\Ollama;%PATH%"
    )

    if exist "%ProgramFiles%\Ollama\ollama.exe" (
        set "PATH=%ProgramFiles%\Ollama;%PATH%"
    )

    where ollama >nul 2>&1
    if errorlevel 1 (
        echo ERROR: Ollama installer finished, but ollama.exe is still not available.
        echo Close this Command Prompt window, open a new one, and run this BAT again.
        pause
        exit /b 1
    )
) else (
    echo ==^> Ollama already installed.
)

REM --- Make sure the Ollama server is up --------------------------------------

curl -fsS http://127.0.0.1:11434/api/tags >nul 2>&1
if errorlevel 1 (
    echo ==^> Starting Ollama server in the background...
    start "" /b ollama serve

    for /l %%i in (1,1,30) do (
        timeout /t 1 /nobreak >nul
        curl -fsS http://127.0.0.1:11434/api/tags >nul 2>&1
        if not errorlevel 1 goto ollama_up
    )
)

:ollama_up
curl -fsS http://127.0.0.1:11434/api/tags >nul 2>&1
if errorlevel 1 (
    echo ERROR: Ollama server did not come up.
    echo Try closing this window, opening a new Command Prompt, and running this BAT again.
    pause
    exit /b 1
)

echo ==^> Ollama server is up.

REM --- Pull the model ----------------------------------------------------------

echo ==^> Pulling model "!OLLAMA_MODEL!". First run may download several GB.
ollama pull !OLLAMA_MODEL!

if errorlevel 1 (
    echo ERROR: Failed to pull the model "!OLLAMA_MODEL!".
    pause
    exit /b 1
)

echo ==^> Ollama model is ready.

:after_provider_setup

REM Preserve whichever DeepSeek key/model were on record when the OTHER
REM provider was chosen this run, so switching back and forth later does not
REM lose them.
if not defined DEEPSEEK_API_KEY set "DEEPSEEK_API_KEY=!EXIST_KEY!"
if not defined OLLAMA_MODEL set "OLLAMA_MODEL=!EXIST_MODEL!"

REM --- 3. Python env + deps ----------------------------------------------------

where python >nul 2>&1
if errorlevel 1 (
    echo ERROR: Python not found on PATH.
    echo Install Python from python.org and tick Add python.exe to PATH.
    echo Then close this window and run this BAT again.
    pause
    exit /b 1
)

python --version
if errorlevel 1 (
    echo ERROR: Python command exists but failed to run.
    pause
    exit /b 1
)

if not exist .venv (
    echo ==^> Creating Python virtualenv .venv...
    python -m venv .venv

    if errorlevel 1 (
        echo ERROR: Could not create Python virtualenv.
        echo Check that Python is installed correctly.
        pause
        exit /b 1
    )
) else (
    echo ==^> Python virtualenv already exists.
)

call .venv\Scripts\activate.bat

if errorlevel 1 (
    echo ERROR: Failed to activate Python virtualenv.
    pause
    exit /b 1
)

echo ==^> Installing Python requirements...
python -m pip install --upgrade pip

if errorlevel 1 (
    echo ERROR: Failed to upgrade pip.
    pause
    exit /b 1
)

python -m pip install -r requirements.txt

if errorlevel 1 (
    echo ERROR: Failed to install Python requirements.
    pause
    exit /b 1
)

REM --- 4. Write the resolved .env (always overwritten with this run's choice) -

echo ==^> Writing .env...
(
    echo PROVIDER=!PROVIDER!
    echo OLLAMA_MODEL=!OLLAMA_MODEL!
    echo DEEPSEEK_API_KEY=!DEEPSEEK_API_KEY!
) > .env

REM --- 5. Launch ---------------------------------------------------------------

echo.
echo ==^> Starting the FPC brain on http://127.0.0.1:5000 ...
echo Press CTRL+C to stop it.
echo.

python fpc_brain.py

echo.
echo FPC brain exited with code %errorlevel%.
pause

endlocal
