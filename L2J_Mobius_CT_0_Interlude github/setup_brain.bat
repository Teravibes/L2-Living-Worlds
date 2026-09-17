@echo off
REM ===========================================================================
REM  One-step setup + launch for the FPC brain - a local Ollama model or one of
REM  several cloud APIs (DeepSeek, OpenAI, Groq, OpenRouter, Mistral), your choice.
REM
REM  What it does:
REM    1. Asks which AI provider to use from a numbered menu: Ollama (local/offline)
REM       or a cloud API. Asked every run - press Enter to keep whatever you picked
REM       last time, or answer again to switch.
REM    2. Lets you pick the chat model: a short suggestion list per provider plus a
REM       "type your own ID" option (Ollama recommends gemma3:12b).
REM       Ollama chosen: installs Ollama if missing, starts its server, pulls the
REM       chosen model.
REM       A cloud provider chosen: asks for (or reuses the saved) API key. Ollama is
REM       never installed/started/pulled on this path.
REM    3. Creates a Python virtualenv and installs requirements.
REM    4. Writes the local .env file with the resolved settings (every provider key
REM       you have entered is kept, so switching back and forth does not lose them).
REM    5. Starts fpc_brain.py.
REM
REM  Usage:
REM    setup_brain.bat                                - if the brain is already set up
REM                                                      (.env + .venv exist) it starts
REM                                                      straight away with no provider
REM                                                      question; otherwise it runs the
REM                                                      first-time setup.
REM    setup_brain.bat --reconfigure                  - always runs the provider/model
REM                                                      questions again, even when already
REM                                                      set up, but KEEPS the saved .env as
REM                                                      the defaults (press Enter to keep a
REM                                                      value; saved API keys are reused).
REM                                                      This is what the launcher's
REM                                                      "Set up / configure brain" button
REM                                                      uses to let you switch provider or
REM                                                      model without re-entering keys.
REM    setup_brain.bat --reset                        - wipes the saved .env first, so
REM                                                      everything is asked fresh again
REM                                                      (including every API key). Use this
REM                                                      to start over from scratch.
REM    setup_brain.bat --auto                         - non-interactive launch used by the
REM                                                      one-click launcher: if the brain is
REM                                                      already configured (.env + .venv), it
REM                                                      just starts fpc_brain.py with no
REM                                                      prompts and no pauses. If it is NOT
REM                                                      configured yet, it prints a short
REM                                                      note and exits WITHOUT blocking the
REM                                                      server (exit code 2), so a boot with
REM                                                      StartBrain=true never hangs on a
REM                                                      prompt. Configure it once by running
REM                                                      setup_brain.bat with no arguments.
REM    set OLLAMA_MODEL=llama3.1 & setup_brain.bat     - override the Ollama model
REM                                                      (skips the model menu on the
REM                                                      Ollama path)
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
    if /i not "%~1"=="--auto" pause
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
    if /i not "%~1"=="--auto" pause
    exit /b 1
)

REM --- 0a. --auto: non-interactive launch for the one-click launcher ---------
REM The launcher calls this when StartBrain=true. It must never prompt or pause,
REM so a normal server boot cannot hang here. If the brain is already configured
REM (an .env and a .venv exist), start it straight away. If it is not configured
REM yet, print a short note and exit with code 2 so the launcher just skips it;
REM the tester configures it once by double-clicking setup_brain.bat normally.

if /i "%~1"=="--auto" (
    if not exist .env (
        echo ==^> Brain not configured yet - skipping auto-start.
        echo     Run setup_brain.bat once ^(no arguments^) to choose a provider and set it up.
        exit /b 2
    )
    if not exist .venv (
        echo ==^> Brain Python environment missing - skipping auto-start.
        echo     Run setup_brain.bat once ^(no arguments^) to build it.
        exit /b 2
    )
    call .venv\Scripts\activate.bat
    if errorlevel 1 (
        echo ==^> Could not activate the brain virtualenv - skipping auto-start.
        exit /b 2
    )
    echo ==^> Starting the FPC brain on http://127.0.0.1:5000 ^(auto^) ...
    python fpc_brain.py
    exit /b !errorlevel!
)

REM --- 0b. --reset wipes the saved config so everything is asked fresh -------

if /i "%~1"=="--reset" (
    if exist .env (
        echo ==^> --reset: removing existing .env - you will be asked to reconfigure.
        del /f /q .env
    )
)

REM --- 0c. Already set up? Skip the questions and go straight to launch -------
REM If an .env (chosen provider) and a .venv (built Python env) already exist,
REM the brain was configured on a previous run, so a plain double-click starts it
REM immediately - no provider question. --reconfigure forces the questions again
REM (keeping .env values as defaults); --reset wipes .env above so it never skips
REM here either. Both therefore fall through to the interactive setup below.

if /i "%~1"=="--reconfigure" goto interactive_setup
if exist .env if exist ".venv\Scripts\activate.bat" goto smart_launch

:interactive_setup

REM --- Load any existing configuration as defaults for the prompts below ----

set "EXIST_PROVIDER="
set "EXIST_MODEL="
set "LEGACY_OLLAMA_MODEL="
set "KEY_DEEPSEEK="
set "KEY_OPENAI="
set "KEY_GROQ="
set "KEY_OPENROUTER="
set "KEY_MISTRAL="
if exist .env (
    for /f "usebackq tokens=1,* delims==" %%A in (".env") do (
        if /i "%%A"=="PROVIDER" set "EXIST_PROVIDER=%%B"
        if /i "%%A"=="MODEL" set "EXIST_MODEL=%%B"
        if /i "%%A"=="OLLAMA_MODEL" set "LEGACY_OLLAMA_MODEL=%%B"
        if /i "%%A"=="DEEPSEEK_API_KEY" set "KEY_DEEPSEEK=%%B"
        if /i "%%A"=="OPENAI_API_KEY" set "KEY_OPENAI=%%B"
        if /i "%%A"=="GROQ_API_KEY" set "KEY_GROQ=%%B"
        if /i "%%A"=="OPENROUTER_API_KEY" set "KEY_OPENROUTER=%%B"
        if /i "%%A"=="MISTRAL_API_KEY" set "KEY_MISTRAL=%%B"
    )
)
REM Older .env files only wrote OLLAMA_MODEL; use it as the saved-model default.
if not defined EXIST_MODEL if defined LEGACY_OLLAMA_MODEL set "EXIST_MODEL=!LEGACY_OLLAMA_MODEL!"

REM --- 1. Ask which provider to use, every run --------------------------------

echo Choose an AI provider for the FPC brain:
echo   [1] Ollama     - local model, free, fully offline (needs a decent GPU/CPU)
echo   [2] DeepSeek   - cloud API, needs an API key, works on any PC
echo   [3] OpenAI     - cloud API, needs an API key
echo   [4] Groq       - cloud API, needs an API key
echo   [5] OpenRouter - cloud API, needs an API key
echo   [6] Mistral    - cloud API, needs an API key
echo.

set "PROVIDER_CHOICE="
if defined EXIST_PROVIDER (
    set /p "PROVIDER_CHOICE=Pick 1-6 (Enter = keep '!EXIST_PROVIDER!'): "
) else (
    set /p "PROVIDER_CHOICE=Pick 1-6: "
)

set "PROVIDER="
if "!PROVIDER_CHOICE!"=="" (
    if defined EXIST_PROVIDER (
        set "PROVIDER=!EXIST_PROVIDER!"
    ) else (
        echo ERROR: You must pick a provider on first setup.
        pause
        exit /b 1
    )
)
REM Accept the menu numbers, plus the old O/D letters for muscle memory.
if "!PROVIDER_CHOICE!"=="1" set "PROVIDER=ollama"
if "!PROVIDER_CHOICE!"=="2" set "PROVIDER=deepseek"
if "!PROVIDER_CHOICE!"=="3" set "PROVIDER=openai"
if "!PROVIDER_CHOICE!"=="4" set "PROVIDER=groq"
if "!PROVIDER_CHOICE!"=="5" set "PROVIDER=openrouter"
if "!PROVIDER_CHOICE!"=="6" set "PROVIDER=mistral"
if /i "!PROVIDER_CHOICE!"=="O" set "PROVIDER=ollama"
if /i "!PROVIDER_CHOICE!"=="D" set "PROVIDER=deepseek"

if not defined PROVIDER (
    echo ERROR: Please pick a number 1-6.
    pause
    exit /b 1
)

echo ==^> Provider: !PROVIDER!
echo.

REM If the chosen provider differs from the one saved in .env, do not offer the
REM old provider's model as the Enter-default in the model menu below.
if defined EXIST_PROVIDER if /i not "!PROVIDER!"=="!EXIST_PROVIDER!" set "EXIST_MODEL="

if /i "!PROVIDER!"=="ollama" goto setup_ollama
goto setup_cloud

:setup_cloud
REM --- 2a. Cloud provider: reuse or ask for the API key, then pick a model ---
REM Every cloud provider here is OpenAI-compatible, so the only per-provider
REM difference is the API key (stored as <PROVIDER>_API_KEY) and the model.

set "CLOUD_KEY="
set "EXIST_CLOUD_KEY="
if /i "!PROVIDER!"=="deepseek"   set "EXIST_CLOUD_KEY=!KEY_DEEPSEEK!"
if /i "!PROVIDER!"=="openai"     set "EXIST_CLOUD_KEY=!KEY_OPENAI!"
if /i "!PROVIDER!"=="groq"       set "EXIST_CLOUD_KEY=!KEY_GROQ!"
if /i "!PROVIDER!"=="openrouter" set "EXIST_CLOUD_KEY=!KEY_OPENROUTER!"
if /i "!PROVIDER!"=="mistral"    set "EXIST_CLOUD_KEY=!KEY_MISTRAL!"

if defined EXIST_CLOUD_KEY if not "!EXIST_CLOUD_KEY!"=="" (
    set /p "KEY_CHOICE=Use saved !PROVIDER! API key ending '...!EXIST_CLOUD_KEY:~-4!'? [Y/n]: "
    if /i not "!KEY_CHOICE:~0,1!"=="N" set "CLOUD_KEY=!EXIST_CLOUD_KEY!"
)

if not defined CLOUD_KEY set /p "CLOUD_KEY=Enter your !PROVIDER! API key: "

if "!CLOUD_KEY!"=="" (
    echo ERROR: An API key is required for !PROVIDER!.
    pause
    exit /b 1
)

REM Store the key back in its own slot so .env keeps every provider's key.
if /i "!PROVIDER!"=="deepseek"   set "KEY_DEEPSEEK=!CLOUD_KEY!"
if /i "!PROVIDER!"=="openai"     set "KEY_OPENAI=!CLOUD_KEY!"
if /i "!PROVIDER!"=="groq"       set "KEY_GROQ=!CLOUD_KEY!"
if /i "!PROVIDER!"=="openrouter" set "KEY_OPENROUTER=!CLOUD_KEY!"
if /i "!PROVIDER!"=="mistral"    set "KEY_MISTRAL=!CLOUD_KEY!"

echo ==^> !PROVIDER! key configured.

call :set_model_suggestions
call :pick_model
echo ==^> Model: !MODEL!
goto after_provider_setup

:setup_ollama
REM --- 2b. Ollama: pick a model, install if missing, start server, pull -----

if not "%OLLAMA_MODEL%"=="" (
    REM Explicit override:  set OLLAMA_MODEL=... & setup_brain.bat  - honor it, no menu.
    set "MODEL=%OLLAMA_MODEL%"
    echo ==^> Using OLLAMA_MODEL override: !MODEL!
) else (
    call :set_model_suggestions
    call :pick_model
)
set "OLLAMA_MODEL=!MODEL!"

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

REM Every provider key entered this run (and any carried over from .env) lives in
REM its own KEY_* slot and is written back below, so switching providers later
REM never loses a previously entered key. MODEL holds the resolved model.

REM --- 3. Python env + deps ----------------------------------------------------

REM Make an already-installed Python visible even if PATH wasn't refreshed yet.
call :ensure_python_on_path

REM Detect a REAL Python. Windows 10/11 ship zero-byte "App execution alias"
REM stubs named python.exe / python3.exe in %LOCALAPPDATA%\Microsoft\WindowsApps
REM that only open the Microsoft Store. Plain `where python` finds those stubs -
REM so it looks installed - but running them just prints "Python was not found"
REM and fails. Gate on actually running python (and reject the Store alias) so a
REM machine with only the stub still triggers the auto-install below.
call :have_real_python
if errorlevel 1 (
    echo ==^> Python not found. Installing it automatically...

    where winget >nul 2>&1
    if errorlevel 1 (
        echo ==^> winget not available. Downloading the official Python installer...
        if not exist "%TEMP%\fpc_brain_setup" mkdir "%TEMP%\fpc_brain_setup"
        powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $url='https://www.python.org/ftp/python/3.12.8/python-3.12.8-amd64.exe'; $out=Join-Path $env:TEMP 'fpc_brain_setup\python-installer.exe'; Invoke-WebRequest -Uri $url -OutFile $out"
        if errorlevel 1 (
            echo ERROR: Could not download the Python installer.
            echo Install Python manually from https://python.org - tick "Add python.exe to PATH".
            pause
            exit /b 1
        )
        echo ==^> Running the Python installer silently ^(this can take a minute^)...
        start /wait "" "%TEMP%\fpc_brain_setup\python-installer.exe" /quiet InstallAllUsers=0 PrependPath=1 Include_pip=1 Include_launcher=1
    ) else (
        echo ==^> Installing Python via winget...
        winget install --id Python.Python.3.12 -e --accept-package-agreements --accept-source-agreements
        if errorlevel 1 (
            echo WARNING: winget install failed. Downloading the official installer instead...
            if not exist "%TEMP%\fpc_brain_setup" mkdir "%TEMP%\fpc_brain_setup"
            powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $url='https://www.python.org/ftp/python/3.12.8/python-3.12.8-amd64.exe'; $out=Join-Path $env:TEMP 'fpc_brain_setup\python-installer.exe'; Invoke-WebRequest -Uri $url -OutFile $out"
            if errorlevel 1 (
                echo ERROR: Could not download the Python installer.
                pause
                exit /b 1
            )
            echo ==^> Running the Python installer silently ^(this can take a minute^)...
            start /wait "" "%TEMP%\fpc_brain_setup\python-installer.exe" /quiet InstallAllUsers=0 PrependPath=1 Include_pip=1 Include_launcher=1
        )
    )

    REM Make the freshly installed Python visible to THIS window (PrependPath only
    REM affects newly opened shells).
    call :ensure_python_on_path

    call :have_real_python
    if errorlevel 1 (
        echo ERROR: Python was installed, but it is not visible in this window yet.
        echo Close this Command Prompt, open a new one, and run this BAT again.
        echo.
        echo If it keeps failing, the Microsoft Store "App execution alias" for
        echo Python may be shadowing the real install. Turn it off under Settings
        echo ^> Apps ^> Advanced app settings ^> App execution aliases ^(switch off
        echo both python.exe and python3.exe^), then run this BAT again.
        pause
        exit /b 1
    )
) else (
    echo ==^> Python already installed.
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
    echo MODEL=!MODEL!
    if defined KEY_DEEPSEEK   if not "!KEY_DEEPSEEK!"==""   echo DEEPSEEK_API_KEY=!KEY_DEEPSEEK!
    if defined KEY_OPENAI     if not "!KEY_OPENAI!"==""     echo OPENAI_API_KEY=!KEY_OPENAI!
    if defined KEY_GROQ       if not "!KEY_GROQ!"==""       echo GROQ_API_KEY=!KEY_GROQ!
    if defined KEY_OPENROUTER if not "!KEY_OPENROUTER!"=="" echo OPENROUTER_API_KEY=!KEY_OPENROUTER!
    if defined KEY_MISTRAL    if not "!KEY_MISTRAL!"==""    echo MISTRAL_API_KEY=!KEY_MISTRAL!
) > .env

REM --- 5. Launch ---------------------------------------------------------------

:launch_brain
echo.
echo ==^> Starting the FPC brain on http://127.0.0.1:5000 ...
echo Press CTRL+C to stop it.
echo.

python fpc_brain.py

echo.
echo FPC brain exited with code !errorlevel!.
pause

endlocal
goto :eof

REM --- Smart launch: already-configured fast path (from the check near the top).
REM Activate the existing venv and jump straight to launching, skipping the
REM provider question and all install steps.
:smart_launch
echo.
echo ==^> Brain already set up - starting it now.
echo     ^(To change provider or model, run: setup_brain.bat --reconfigure^)
call .venv\Scripts\activate.bat
if errorlevel 1 (
    echo ERROR: Could not activate the existing Python virtualenv.
    echo Run: setup_brain.bat --reset   to rebuild it.
    pause
    exit /b 1
)
goto launch_brain

REM ===========================================================================
REM  Subroutines
REM ===========================================================================

:set_model_suggestions
REM Fill MODREC (the recommended/Enter default) and SUGG1..SUGG5 (blank = absent)
REM for the current !PROVIDER!. These are suggestions only - the menu always lets
REM the user type any model ID, so the lists can be short and do not have to be
REM exhaustive or perfectly current.
set "SUGG1="
set "SUGG2="
set "SUGG3="
set "SUGG4="
set "SUGG5="
if /i "!PROVIDER!"=="ollama" (
    set "MODREC=gemma3:12b"
    set "SUGG1=gemma3:12b"
    set "SUGG2=llama3.1"
    set "SUGG3=qwen2.5:14b"
    set "SUGG4=phi3"
) else if /i "!PROVIDER!"=="deepseek" (
    set "MODREC=deepseek-chat"
    set "SUGG1=deepseek-chat"
    set "SUGG2=deepseek-reasoner"
) else if /i "!PROVIDER!"=="openai" (
    set "MODREC=gpt-4o-mini"
    set "SUGG1=gpt-4o-mini"
    set "SUGG2=gpt-4o"
    set "SUGG3=gpt-4.1-mini"
) else if /i "!PROVIDER!"=="groq" (
    set "MODREC=llama-3.3-70b-versatile"
    set "SUGG1=llama-3.3-70b-versatile"
    set "SUGG2=llama-3.1-8b-instant"
) else if /i "!PROVIDER!"=="openrouter" (
    set "MODREC=deepseek/deepseek-chat"
    set "SUGG1=deepseek/deepseek-chat"
    set "SUGG2=meta-llama/llama-3.3-70b-instruct"
) else if /i "!PROVIDER!"=="mistral" (
    set "MODREC=mistral-small-latest"
    set "SUGG1=mistral-small-latest"
    set "SUGG2=mistral-large-latest"
)
goto :eof

:pick_model
REM Show the suggestion menu and set MODEL. The Enter default is the model saved
REM in .env for this provider (EXIST_MODEL) if any, otherwise the recommended one
REM (MODREC). "C" lets the user type any model ID. Requires set_model_suggestions
REM to have run first.
set "ENTERDEF=!MODREC!"
if defined EXIST_MODEL if not "!EXIST_MODEL!"=="" set "ENTERDEF=!EXIST_MODEL!"
echo.
echo Choose a model for !PROVIDER!  ^(recommended: !MODREC!^):
if defined SUGG1 echo   [1] !SUGG1!
if defined SUGG2 echo   [2] !SUGG2!
if defined SUGG3 echo   [3] !SUGG3!
if defined SUGG4 echo   [4] !SUGG4!
if defined SUGG5 echo   [5] !SUGG5!
echo   [C] Type a custom model ID
echo.
set "MODEL_CHOICE="
set /p "MODEL_CHOICE=Pick a number, C for a custom ID, or Enter to keep '!ENTERDEF!': "

if "!MODEL_CHOICE!"=="" (
    set "MODEL=!ENTERDEF!"
    goto :eof
)
if /i "!MODEL_CHOICE!"=="C" (
    set "CUSTOM_MODEL="
    set /p "CUSTOM_MODEL=Enter the exact model ID: "
    if "!CUSTOM_MODEL!"=="" (
        set "MODEL=!ENTERDEF!"
    ) else (
        set "MODEL=!CUSTOM_MODEL!"
    )
    goto :eof
)
set "MODEL="
if "!MODEL_CHOICE!"=="1" set "MODEL=!SUGG1!"
if "!MODEL_CHOICE!"=="2" set "MODEL=!SUGG2!"
if "!MODEL_CHOICE!"=="3" set "MODEL=!SUGG3!"
if "!MODEL_CHOICE!"=="4" set "MODEL=!SUGG4!"
if "!MODEL_CHOICE!"=="5" set "MODEL=!SUGG5!"
if not defined MODEL (
    echo Not a valid choice - keeping '!ENTERDEF!'.
    set "MODEL=!ENTERDEF!"
)
goto :eof

:ensure_python_on_path
REM Add the usual per-user / all-users Python install locations to PATH for THIS
REM window. The installer's PrependPath only affects newly opened shells, so a
REM Python we just installed (or one installed but not yet on PATH) would be
REM invisible without this. Any working python is fine - a venv is created from it.
if exist "%LOCALAPPDATA%\Programs\Python" (
    for /f "delims=" %%P in ('dir /b /ad /o-n "%LOCALAPPDATA%\Programs\Python\Python3*" 2^>nul') do (
        if exist "%LOCALAPPDATA%\Programs\Python\%%P\python.exe" (
            set "PATH=%LOCALAPPDATA%\Programs\Python\%%P;%LOCALAPPDATA%\Programs\Python\%%P\Scripts;!PATH!"
        )
    )
)
for %%D in ("%ProgramFiles%\Python313" "%ProgramFiles%\Python312" "%ProgramFiles%\Python311") do (
    if exist "%%~D\python.exe" set "PATH=%%~D;%%~D\Scripts;!PATH!"
)
goto :eof

:have_real_python
REM Succeeds (errorlevel 0) only when a usable Python is on PATH. Returns 1 when
REM none is found OR the only match is the Microsoft Store "App execution alias"
REM stub in WindowsApps (which reports as present via `where` but cannot run).
set "REALPY="
for /f "delims=" %%I in ('where python 2^>nul') do (
    echo %%I | find /i "\WindowsApps\" >nul
    if errorlevel 1 (
        if not defined REALPY set "REALPY=%%I"
    )
)
if not defined REALPY exit /b 1
REM A non-alias python.exe exists on PATH; confirm it actually runs.
"%REALPY%" --version >nul 2>&1
if errorlevel 1 exit /b 1
REM Pin its folder to the front of PATH so every later plain `python` call in
REM this window resolves to it and never to the WindowsApps Store alias.
for %%I in ("%REALPY%") do set "PATH=%%~dpI;%%~dpIScripts;!PATH!"
exit /b 0
