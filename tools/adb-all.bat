@echo off
rem adb-all.bat - EVERYTHING in ONE adb session, for Windows (cmd). Same steps as tools\adb-all.sh. NOT tested on Windows (written carefully; tell me if a line fails).
rem   adb-all.bat                       permissions, helper, probe, pull the tablet's Settings app, screenshots of the stock Settings pages
rem   adb-all.bat Launcher-v0.5.0.apk   same, but install (update) the launcher APK first
rem   adb-all.bat --browser-off         also disable the old Browser app (reversible: adb shell pm enable com.android.browser)
rem   adb-all.bat --status              only show what is granted / running, change nothing
rem   adb-all.bat --no-pull             skip the big file copies and the screenshots
rem Plug the tablet in, USB debugging ON, tap Allow. Everything lands in the folder probe-out : send me that folder (zip it).
setlocal EnableDelayedExpansion
set PKG=com.tj.launcher
set A11Y=%PKG%/%PKG%.TjAccessibilityService
set OUT=probe-out
if not exist %OUT%\shots mkdir %OUT%\shots
if not exist %OUT%\apk mkdir %OUT%\apk
set STATUS=%OUT%\status.txt
set REPORT=%OUT%\launcher-probe.txt
type nul > %STATUS%
type nul > %REPORT%
set APK=
set BROWSER=0
set STATUSONLY=0
set PULL=1
:args
if "%~1"=="" goto argsdone
if /i "%~1"=="--browser-off" (set BROWSER=1) else if /i "%~1"=="--status" (set STATUSONLY=1) else if /i "%~1"=="--no-pull" (set PULL=0) else (set APK=%~1)
shift
goto args
:argsdone

where adb >nul 2>nul
if errorlevel 1 goto noadb
adb get-state >nul 2>nul
if errorlevel 1 goto nodevice
call :say == adb-all %DATE% %TIME%
if "%APK%"=="" goto afterinstall
call :say == 0. install %APK%
adb install -r "%APK%" >> %STATUS% 2>&1
:afterinstall
adb shell pm path %PKG% | find "package:" >nul
if errorlevel 1 goto nolauncher

if "%STATUSONLY%"=="1" goto status
call :say == 1. permissions
for %%P in (WRITE_SECURE_SETTINGS DUMP CHANGE_CONFIGURATION READ_EXTERNAL_STORAGE PACKAGE_USAGE_STATS) do (
  adb shell pm grant %PKG% android.permission.%%P >> %STATUS% 2>&1
)
for %%O in (GET_USAGE_STATS WRITE_SETTINGS RUN_IN_BACKGROUND RUN_ANY_IN_BACKGROUND) do (
  adb shell cmd appops set %PKG% %%O allow >> %STATUS% 2>&1
)
adb shell dumpsys deviceidle whitelist +%PKG% >> %STATUS% 2>&1

call :say == 1b. speed: compile the launcher ahead of time (about a minute)
adb shell cmd package compile -m speed -f %PKG% >> %STATUS% 2>&1

call :say == 2. accessibility helper
set CUR=
for /f "delims=" %%i in ('adb shell settings get secure enabled_accessibility_services') do set CUR=%%i
echo !CUR! | find "%PKG%" >nul
if not errorlevel 1 goto a11ydone
if "!CUR!"=="" (set NEW=%A11Y%) else if /i "!CUR!"=="null" (set NEW=%A11Y%) else (set NEW=!CUR!:%A11Y%)
adb shell settings put secure enabled_accessibility_services !NEW!
adb shell settings put secure accessibility_enabled 1
:a11ydone

call :say == 3. the invisible clean-up helper (lasts until the next restart: run this script again then)
set APKPATH=
for /f "tokens=2 delims=:" %%i in ('adb shell pm path %PKG%') do if not defined APKPATH set APKPATH=%%i
adb shell "CLASSPATH=!APKPATH! setsid nohup app_process /system/bin com.tj.launcher.ShellServer >/dev/null 2>&1 &" >nul 2>&1

if "%BROWSER%"=="1" (
  call :say == 4. Browser app off
  adb shell pm disable-user --user 0 com.android.browser >> %STATUS% 2>&1
) else (
  call :say == 4. Browser app skipped - add --browser-off to disable it
)

:status
call :say == 5. status of every permission command - what the tablet reports
adb shell "dumpsys package %PKG% | grep -E 'granted=(true|false)' | grep -E 'WRITE_SECURE_SETTINGS|DUMP|CHANGE_CONFIGURATION|READ_EXTERNAL_STORAGE|PACKAGE_USAGE_STATS|WRITE_SETTINGS'" >> %STATUS% 2>&1
adb shell "cmd appops get %PKG% | grep -E 'GET_USAGE_STATS|WRITE_SETTINGS|RUN_IN_BACKGROUND|RUN_ANY_IN_BACKGROUND'" >> %STATUS% 2>&1
adb shell settings get secure enabled_accessibility_services >> %STATUS% 2>&1
adb shell "ps -A -o ARGS | grep '[S]hellServer'" >> %STATUS% 2>&1
adb shell pm get-max-users >> %STATUS% 2>&1
adb shell pm list users >> %STATUS% 2>&1
type %STATUS%

call :say == 6. read-only probe into %REPORT%
for /f "usebackq delims=" %%L in ("%~dp0..\app\src\main\resources\probe-commands.txt") do (
  set "LINE=%%L"
  if "!LINE:~0,3!"=="## " (
    echo.>> %REPORT%
    echo ===== !LINE:~3!>> %REPORT%
  ) else (
    echo $ adb shell !LINE!>> %REPORT%
    adb shell "!LINE!" >> %REPORT% 2>&1
  )
)

if "%STATUSONLY%"=="1" goto done
if "%PULL%"=="0" goto done
call :say == 7. copy the tablet's own system apps
for %%K in (com.android.settings com.android.systemui com.android.launcher3 android) do (
  for /f "tokens=2 delims=:" %%i in ('adb shell pm path %%K') do (
    adb pull %%i %OUT%\apk\%%K.apk >nul 2>&1
  )
)
call :say == 8. screenshots of the stock Settings pages - touch nothing on the tablet for about a minute
for %%P in (SETTINGS WIFI_SETTINGS BLUETOOTH_SETTINGS DISPLAY_SETTINGS SOUND_SETTINGS APPLICATION_SETTINGS INTERNAL_STORAGE_SETTINGS BATTERY_SAVER_SETTINGS DATE_SETTINGS LOCALE_SETTINGS ACCESSIBILITY_SETTINGS SECURITY_SETTINGS PRIVACY_SETTINGS DEVICE_INFO_SETTINGS INPUT_METHOD_SETTINGS) do (
  adb shell am start -a android.settings.%%P >nul 2>&1
  timeout /t 3 /nobreak >nul
  adb exec-out screencap -p > %OUT%\shots\%%P.png
  adb shell uiautomator dump /sdcard/tjui.xml >nul 2>&1
  adb pull /sdcard/tjui.xml %OUT%\shots\%%P.xml >nul 2>&1
)
adb shell input keyevent KEYCODE_HOME >nul 2>&1

:done
call :say DONE. Send me the folder %OUT% - zip it - or paste %STATUS% and %REPORT%.
exit /b 0

:say
echo %*
echo %*>> %STATUS%
exit /b 0

:noadb
echo adb is not installed or not on PATH. Install Android platform-tools.
exit /b 1
:nodevice
echo No tablet found: USB debugging ON, cable in, tap ALLOW on the tablet, then run again.
exit /b 1
:nolauncher
echo The launcher %PKG% is not installed. Install it first: adb-all.bat Launcher-vX.apk
exit /b 1
