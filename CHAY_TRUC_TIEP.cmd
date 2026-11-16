@echo off
chcp 65001 >nul
cd /d "%~dp0"
java -jar "dist\VINABACUS_2_CO_SO.jar"
if errorlevel 1 (
    echo.
    echo Khong chay duoc. Hay cai Java 17+ va thu mo du an bang NetBeans 25.
    pause
)
