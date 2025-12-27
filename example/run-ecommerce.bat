@echo off
REM Run script for E-Commerce Demo (Windows)

echo ========================================
echo   E-Commerce Demo
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

echo Compiling E-Commerce Demo...
javac -cp "target\classes;%USERPROFILE%\.m2\repository\com\mysql\mysql-connector-j\8.0.33\mysql-connector-j-8.0.33.jar" example\ECommerceDemo.java -d target\classes
if errorlevel 1 (
    echo ERROR: Failed to compile example
    pause
    exit /b 1
)

echo.
echo Running E-Commerce Demo...
echo.
java -cp "target\classes;%USERPROFILE%\.m2\repository\com\mysql\mysql-connector-j\8.0.33\mysql-connector-j-8.0.33.jar" example.ECommerceDemo

pause

