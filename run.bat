@echo off
set "JAVA_HOME=E:\dev\outils\jdk-19.0.2"
set "M2_HOME=E:\dev\outils\apache-maven-3.9.12"
set "PATH=%JAVA_HOME%\bin;%M2_HOME%\bin;%PATH%"

echo Using JDK: %JAVA_HOME%
java -version

echo Starting RAG Application...
call mvn spring-boot:run
