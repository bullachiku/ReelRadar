@echo off
setlocal enabledelayedexpansion

title ReelRadar 1-Click APK Builder
color 0B

echo =====================================================================
echo                 ReelRadar - Automated APK Builder
echo =====================================================================
echo.

rem 1. Check if java is already in PATH
where java >nul 2>nul
if %ERRORLEVEL% equ 0 (
    echo [OK] Java detected in PATH!
    goto BUILD
)

rem 2. Check Android Studio bundled Java (JBR)
if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
    echo [OK] Found Android Studio Java!
    set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
    set "PATH=!JAVA_HOME!\bin;!PATH!"
    goto BUILD
)

rem 3. Check Program Files Java / JDK installations
for /d %%i in ("C:\Program Files\Java\jdk*") do (
    if exist "%%i\bin\java.exe" (
        echo [OK] Found Java at %%i
        set "JAVA_HOME=%%i"
        set "PATH=!JAVA_HOME!\bin;!PATH!"
        goto BUILD
    )
)
for /d %%i in ("C:\Program Files\Eclipse Adoptium\jdk*") do (
    if exist "%%i\bin\java.exe" (
        echo [OK] Found Eclipse Adoptium Java at %%i
        set "JAVA_HOME=%%i"
        set "PATH=!JAVA_HOME!\bin;!PATH!"
        goto BUILD
    )
)
for /d %%i in ("C:\Program Files\Microsoft\jdk*") do (
    if exist "%%i\bin\java.exe" (
        echo [OK] Found Microsoft JDK at %%i
        set "JAVA_HOME=%%i"
        set "PATH=!JAVA_HOME!\bin;!PATH!"
        goto BUILD
    )
)

echo [!] Java is not found on your system.
echo.
echo TO FIX THIS EASILY (Choose Option 1 or 2):
echo.
echo Option 1 (Recommended - No Coding):
echo   Upload this ReelRadar folder to a free GitHub account.
echo   Our included GitHub Action will automatically build and give you
echo   a direct download link for the APK on your phone!
echo.
echo Option 2 (Install Java on your PC):
echo   Download and install free Java 17 from:
echo   https://adoptium.net/temurin/releases/?version=17
echo   Then double-click this build-apk.bat script again!
echo.
pause
exit /b 1

:BUILD
echo.
echo [1/2] Compiling and packaging ReelRadar Android App...
echo       (This takes about 1-2 minutes on first run)
echo.

call gradlew.bat assembleDebug

if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Build failed! Check the error log above.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [2/2] Build Finished Successfully!
echo.

if exist "app\build\outputs\apk\debug\app-debug.apk" (
    copy /y "app\build\outputs\apk\debug\app-debug.apk" "ReelRadar-v1.0.apk" >nul
    echo =====================================================================
    echo   SUCCESS! Your ready-to-use APK has been created:
    echo   FILE: ReelRadar-v1.0.apk (in this folder)
    echo =====================================================================
    echo.
    echo HOW TO INSTALL ON YOUR PHONE:
    echo 1. Send ReelRadar-v1.0.apk to your phone (via USB, Google Drive, or WhatsApp).
    echo 2. Tap the file on your phone to Install.
    echo 3. Open ReelRadar and follow the quick onboarding steps!
    echo.
    explorer.exe /select,"ReelRadar-v1.0.apk"
) else (
    echo APK built in app\build\outputs\apk\debug\
)

pause
