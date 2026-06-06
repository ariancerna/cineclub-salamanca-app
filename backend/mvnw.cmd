@echo off
setlocal

set DIRNAME=%~dp0
set WRAPPER_JAR=%DIRNAME%.mvn\wrapper\maven-wrapper.jar
set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain
set JAVA_EXE=%ProgramFiles%\Microsoft\jdk-21.0.11.10-hotspot\bin\java.exe

if not exist "%JAVA_EXE%" (
    if not "%JAVA_HOME%"=="" set JAVA_EXE=%JAVA_HOME%\bin\java.exe
)

if not exist "%JAVA_EXE%" (
    echo ERROR: No se encontro java.exe. Asegurate de tener Java 21 instalado.
    exit /b 1
)

"%JAVA_EXE%" ^
  "-Dmaven.multiModuleProjectDirectory=%DIRNAME%" ^
  -classpath "%WRAPPER_JAR%" ^
  %WRAPPER_LAUNCHER% ^
  %*

exit /b %ERRORLEVEL%
