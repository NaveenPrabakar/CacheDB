@echo off
REM Run script for E-Commerce Demo (Windows)

echo Building CacheDB...
cd ..
call mvn clean compile -q

echo Compiling example...
javac -cp "target\classes;%USERPROFILE%\.m2\repository\com\mysql\mysql-connector-j\8.0.33\mysql-connector-j-8.0.33.jar" example\ECommerceDemo.java -d target\classes

echo Running E-Commerce Demo...
echo.
java -cp "target\classes;%USERPROFILE%\.m2\repository\com\mysql\mysql-connector-j\8.0.33\mysql-connector-j-8.0.33.jar" example.ECommerceDemo

pause

