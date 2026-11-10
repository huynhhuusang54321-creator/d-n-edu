@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"
if exist "target\VINABACUS-0.4.0-SNAPSHOT.jar" goto RUN
where mvn >nul 2>&1
if not errorlevel 1 (
    call mvn -q -DskipTests package
) else (
    if exist "C:\Program Files\NetBeans-25\netbeans\java\maven\bin\mvn.cmd" (
        call "C:\Program Files\NetBeans-25\netbeans\java\maven\bin\mvn.cmd" -q -DskipTests package
    ) else (
        echo Khong tim thay Maven. Vui long build du an bang NetBeans.
        pause
        exit /b 1
    )
)
if errorlevel 1 (
    echo Loi khi bien dich du an.
    pause
    exit /b 1
)
:RUN
java -jar "target\VINABACUS-0.4.0-SNAPSHOT.jar"
if errorlevel 1 pause
