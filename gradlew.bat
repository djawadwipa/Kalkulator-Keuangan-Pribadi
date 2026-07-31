@ECHO OFF
SETLOCAL
SET APP_HOME=%~dp0
SET WRAPPER_JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

PowerShell -NoProfile -ExecutionPolicy Bypass -File "%APP_HOME%scripts\bootstrap-gradle-wrapper.ps1"
IF ERRORLEVEL 1 EXIT /B 1

IF DEFINED JAVA_HOME (
  SET JAVA_EXE=%JAVA_HOME%\bin\java.exe
) ELSE (
  SET JAVA_EXE=java.exe
)

"%JAVA_EXE%" -Dorg.gradle.appname=gradlew -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
EXIT /B %ERRORLEVEL%
