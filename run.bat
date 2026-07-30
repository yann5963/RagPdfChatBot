@echo off
set "JAVA_HOME=E:\dev\outils\jdk-19.0.2"
set "M2_HOME=E:\dev\outils\apache-maven-3.9.12"
set "PATH=%JAVA_HOME%\bin;%M2_HOME%\bin;%PATH%"

echo Using JDK: %JAVA_HOME%
java -version

echo Building the microservices project...
call mvn clean package -DskipTests

echo Starting Document Ingestion Service (Port 8081)...
start "Document Ingestion Service" cmd /c "java -jar document-ingestion-service\target\document-ingestion-service-0.0.1-SNAPSHOT.jar"

echo Starting RAG Retriever Service (Port 8082)...
start "RAG Retriever Service" cmd /c "java -jar rag-retriever-service\target\rag-retriever-service-0.0.1-SNAPSHOT.jar"

echo Starting UI Gateway Service (Port 8080)...
start "UI Gateway Service" cmd /c "java -jar ui-gateway-service\target\ui-gateway-service-0.0.1-SNAPSHOT.jar"

echo All services are starting in separate windows.
echo Access the application at http://localhost:8080
pause
