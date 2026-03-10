@echo off
REM Запуск backend без Gradle (требует установленной Java 17+)
echo Starting Codemy Backend...
echo.

REM Проверка JAVA_HOME
if "%JAVA_HOME%"=="" (
    echo ERROR: JAVA_HOME not set. Please install JDK 17+ and set JAVA_HOME.
    echo Download from: https://www.oracle.com/java/technologies/downloads/#java17
    pause
    exit /b 1
)

REM Запуск
"%JAVA_HOME%\bin\java" -jar build\libs\codemy_backend-all.jar

pause
