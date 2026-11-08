@echo off
setlocal enabledelayedexpansion

echo ========================================================
echo   Building DueDate Application (Java 24+ Compatible)
echo   Apache NetBeans 25 / Maven Standard Project Layout
echo ========================================================

REM Check if Maven is available on PATH
where mvn >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo Maven detected! Running Maven build...
    call mvn clean package
    if %ERRORLEVEL% equ 0 (
        echo ========================================================
        echo   MAVEN BUILD SUCCESSFUL!
        echo   Executable created: DueDate.jar
        echo   Run using: run.bat OR java -jar DueDate.jar
        echo ========================================================
        exit /b 0
    ) else (
        echo Maven build failed, falling back to direct javac...
    )
)

REM Direct Javac Build Fallback
if not exist "bin" mkdir "bin"

echo Compiling main application source files...
javac -encoding UTF-8 -d bin src\main\java\com\duedate\model\*.java src\main\java\com\duedate\io\*.java src\main\java\com\duedate\config\*.java src\main\java\com\duedate\service\*.java src\main\java\com\duedate\ui\*.java src\main\java\com\duedate\ui\components\*.java src\main\java\com\duedate\*.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Compilation failed.
    exit /b %ERRORLEVEL%
)

echo Compiling test files...
javac -encoding UTF-8 -cp bin -d bin src\test\java\com\duedate\test\*.java
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Test compilation failed.
    exit /b %ERRORLEVEL%
)

echo Running automated test suite...
java -cp bin com.duedate.test.DueDateTest
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Automated tests failed!
    exit /b %ERRORLEVEL%
)

echo Packaging executable fat-JAR: DueDate.jar...
jar cvfe DueDate.jar com.duedate.Main -C bin . >nul 2>&1
if errorlevel 1 (
    echo [NOTE] DueDate.jar is currently open by Windows.
    echo Packaging as DueDate-updated.jar instead...
    jar cvfe DueDate-updated.jar com.duedate.Main -C bin .
    if errorlevel 1 (
        echo [ERROR] JAR packaging failed.
        exit /b 1
    )
    echo ========================================================
    echo   BUILD SUCCESSFUL!
    echo   (Note: App is running; updated build in DueDate-updated.jar)
    echo ========================================================
    exit /b 0
)

echo ========================================================
echo   BUILD SUCCESSFUL!
echo   Executable created: DueDate.jar
echo   Run using: run.bat OR java -jar DueDate.jar
echo ========================================================
