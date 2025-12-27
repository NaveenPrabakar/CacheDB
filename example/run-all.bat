@echo off
REM Run all examples (Windows)

echo ========================================
echo   Running All CacheDB Examples
echo ========================================
echo.

echo [1/4] Running E-Commerce Demo...
call run-ecommerce.bat
echo.

echo [2/4] Running Session Management Demo...
call run-session.bat
echo.

echo [3/4] Running Analytics Counter Demo...
call run-analytics.bat
echo.

echo [4/4] Running Social Feed Demo...
call run-social.bat
echo.

echo ========================================
echo   All Examples Complete!
echo ========================================
pause

