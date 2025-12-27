@echo off
REM Run script for Analytics Counter Demo (Windows)

echo ========================================
echo   Analytics Counter Demo
echo ========================================
echo.

echo Building CacheDB...
cd ..
call mvn clean compile -q
if errorlevel 1 (
    echo ERROR: Failed to compile CacheDB
    pause
    exit /b 1
)

echo Compiling Analytics Counter Demo...
javac -cp "target\classes;%USERPROFILE%\.m2\repository\com\mysql\mysql-connector-j\8.0.33\mysql-connector-j-8.0.33.jar" example\AnalyticsCounterDemo.java -d target\classes
if errorlevel 1 (
    echo ERROR: Failed to compile example
    pause
    exit /b 1
)

echo.
echo Running Analytics Counter Demo...
echo.
java -cp "target\classes;%USERPROFILE%\.m2\repository\com\mysql\mysql-connector-j\8.0.33\mysql-connector-j-8.0.33.jar" example.AnalyticsCounterDemo

pause

