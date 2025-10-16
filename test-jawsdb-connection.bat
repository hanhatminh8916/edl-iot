@echo off
echo ============================================
echo Testing JawsDB MySQL Connection
echo ============================================
echo.
echo Database: gve28c5a0gz2mc4b
echo Username: zr4jwtp3sfgeb5sd
echo.
echo Trying different JawsDB hostnames...
echo.

REM Test 1: US East (RDS)
echo [1] Testing: uyu7j8yh4b0cwi4v.cbetxkdyhwsb.us-east-1.rds.amazonaws.com
ping -n 1 uyu7j8yh4b0cwi4v.cbetxkdyhwsb.us-east-1.rds.amazonaws.com > nul 2>&1
if %errorlevel% equ 0 (
    echo    [OK] Host reachable!
) else (
    echo    [FAIL] Cannot reach host
)
echo.

REM Test 2: EU West (RDS)
echo [2] Testing: uyu7j8yh4b0cwi4v.chzain6bshtu.eu-west-1.rds.amazonaws.com
ping -n 1 uyu7j8yh4b0cwi4v.chzain6bshtu.eu-west-1.rds.amazonaws.com > nul 2>&1
if %errorlevel% equ 0 (
    echo    [OK] Host reachable!
) else (
    echo    [FAIL] Cannot reach host
)
echo.

REM Test 3: JawsDB.com
echo [3] Testing: uyu7j8yh4b0cwi4v.jawsdb.com
ping -n 1 uyu7j8yh4b0cwi4v.jawsdb.com > nul 2>&1
if %errorlevel% equ 0 (
    echo    [OK] Host reachable!
) else (
    echo    [FAIL] Cannot reach host
)
echo.

echo ============================================
echo Check JawsDB Dashboard for exact hostname!
echo https://www.jawsdb.com/ or Heroku Dashboard
echo ============================================
pause
