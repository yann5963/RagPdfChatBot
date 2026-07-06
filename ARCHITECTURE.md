# Architecture Microservices pour l'Application RAG

La transition d'une architecture monolithique vers une architecture orientée microservices nécessite une refonte de la structuration de l'application. Cette proposition répond à vos demandes pour diviser le monolithe en trois microservices : `document-ingestion-service`, `rag-retriever-service` et `ui-gateway-service`.

---

## 1. Structure du Projet Multi-Modules (Maven)

L'utilisation de modules Maven permet une gestion cohérente et centralisée des dépendances et de la compilation. Le projet comprendra un module parent (le POM agissant comme un BOM et gérant la construction), un module partagé (`common`) et les trois services.

### Structure des Répertoires
```text
rag-microservices/
├── pom.xml (Parent POM)
├── common-service/
│   ├── pom.xml
│   └── src/main/java/com/antigravity/common/... (DTOs, Exceptions, Interfaces)
├── document-ingestion-service/
│   ├── pom.xml
│   └── src/main/java/com/antigravity/ingestion/...
├── rag-retriever-service/
│   ├── pom.xml
│   └── src/main/java/com/antigravity/retriever/...
└── ui-gateway-service/
    ├── pom.xml
    └── src/main/java/com/antigravity/ui/... (Controllers Thymeleaf, Gateway/Feign)
```

### `pom.xml` Parent Proposé

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.2</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>

    <groupId>com.antigravity</groupId>
    <artifactId>rag-microservices</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>rag-microservices</name>
    <description>RAG Application with Spring AI (Microservices Architecture)</description>

    <modules>
        <module>common-service</module>
        <module>document-ingestion-service</module>
        <module>rag-retriever-service</module>
        <module>ui-gateway-service</module>
    </modules>

    <properties>
        <java.version>19</java.version>
        <spring-ai.version>1.0.0-M5</spring-ai.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Spring AI BOM -->
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>${spring-ai.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <!-- Module interne commun -->
            <dependency>
                <groupId>com.antigravity</groupId>
                <artifactId>common-service</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <repositories>
        <repository>
            <id>spring-milestones</id>
            <name>Spring Milestones</name>
            <url>https://repo.spring.io/milestone</url>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
    </repositories>
</project>
```

---

## 2. Répartition des Configurations Spring AI

Il est primordial d'alléger chaque microservice pour qu'il n'embarque que les dépendances et Beans dont il a besoin.

### A. `document-ingestion-service` (Ingestion et Vectorisation)
**Responsabilité :** Extraire le texte (Tika), chunker, vectoriser, insérer dans la BDD.
*   **Starters :**
    *   `spring-ai-tika-document-reader`
    *   `spring-ai-ollama-spring-boot-starter` (pour l'Embedding uniquement)
    *   `spring-ai-pgvector-store-spring-boot-starter`
*   **Beans requis :**
    *   `EmbeddingModel` (Fourni automatiquement par Ollama Starter)
    *   `VectorStore` (Fourni automatiquement par PGVector Starter)
    *   Beans personnalisés : `DocumentReader`, `TextSplitter` (ex: `RecursiveCharacterTextSplitter`).
*   **A Noter :** Le composant Chat d'Ollama (`ChatModel` ou `ChatClient`) **n'est pas nécessaire** ici.

### B. `rag-retriever-service` (Recherche et Inférence LLM)
**Responsabilité :** Interroger la base vectorielle et générer la réponse via LLM.
*   **Starters :**
    *   `spring-ai-ollama-spring-boot-starter` (pour le ChatModel / ChatClient ET l'EmbeddingModel)
    *   `spring-ai-pgvector-store-spring-boot-starter`
*   **Beans requis :**
    *   `VectorStore` (Utilisé pour la recherche `similaritySearch`).
    *   `EmbeddingModel` (Requis par le VectorStore pour transformer la question de l'utilisateur en vecteur avant la recherche).
    *   `ChatClient` ou `ChatModel` (Pour la génération de réponse).

### C. `ui-gateway-service` (Frontend et Routage)
**Responsabilité :** Gérer l'UI (Thymeleaf) et transmettre les requêtes.
*   **Starters :**
    *   `spring-boot-starter-web` (ou WebFlux selon le besoin de streaming)
    *   `spring-boot-starter-thymeleaf`
*   **Beans requis :** Aucun bean Spring AI. Ce service agit comme un client HTTP (RestTemplate, WebClient, ou FeignClient) appelant les API des autres microservices.

---

## 3. Fichiers de Configuration (`application.properties`)

Les services backend se connectent aux mêmes ressources (PostgreSQL, Ollama) mais sont isolés.

### A. `document-ingestion-service/src/main/resources/application.properties`
```properties
spring.application.name=document-ingestion-service
server.port=8081

# File Upload Limit (Besoin spécifique à l'ingestion)
spring.servlet.multipart.max-file-size=15MB
spring.servlet.multipart.max-request-size=15MB

# Database Configuration (PGVector)
spring.datasource.url=jdbc:postgresql://localhost:5432/vector_db
spring.datasource.username=yann
spring.datasource.password=postgresql
spring.datasource.driver-class-name=org.postgresql.Driver

# Vector Store Configuration (Il écrit et doit initialiser le schéma si besoin)
spring.ai.vectorstore.pgvector.index-type=HNSW
spring.ai.vectorstore.pgvector.distance-type=COSINE_DISTANCE
spring.ai.vectorstore.pgvector.dimensions=768
spring.ai.vectorstore.pgvector.initialize-schema=true

# Ollama Configuration (Embedding uniquement)
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.embedding.options.model=nomic-embed-text-v2-moe:latest
# Désactiver le chat model dans l'ingestion si le starter le charge par défaut
spring.ai.ollama.chat.enabled=false
```

### B. `rag-retriever-service/src/main/resources/application.properties`
```properties
spring.application.name=rag-retriever-service
server.port=8082

# Database Configuration (PGVector)
spring.datasource.url=jdbc:postgresql://localhost:5432/vector_db
spring.datasource.username=yann
spring.datasource.password=postgresql
spring.datasource.driver-class-name=org.postgresql.Driver

# Vector Store Configuration (Il lit uniquement, ne pas initialiser le schéma ici)
spring.ai.vectorstore.pgvector.index-type=HNSW
spring.ai.vectorstore.pgvector.distance-type=COSINE_DISTANCE
spring.ai.vectorstore.pgvector.dimensions=768
spring.ai.vectorstore.pgvector.initialize-schema=false

# Ollama Configuration (Embedding & Chat)
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.embedding.options.model=nomic-embed-text-v2-moe:latest
spring.ai.ollama.chat.options.model=glm-4.6:cloud
OLLAMA_KEEP_ALIVE=15s
```

### C. `ui-gateway-service/src/main/resources/application.properties`
```properties
spring.application.name=ui-gateway-service
server.port=8080

# Thymeleaf
spring.thymeleaf.cache=false

# File Upload Limit (Doit être configuré ici aussi pour laisser passer le PDF)
spring.servlet.multipart.max-file-size=15MB
spring.servlet.multipart.max-request-size=15MB

# URLs des services Backend
app.services.ingestion.url=http://localhost:8081
app.services.retriever.url=http://localhost:8082

# Models UI
app.available-models=glm-4.6:cloud,qwen3:4b,llama3.2:3b,gemma4:e2b
```

---

## 4. Communication Inter-Services et Contrats d'API REST

Pour assurer la communication entre l'`ui-gateway-service` et les backends, voici les contrats d'API recommandés. L'utilisation de DTOs partagés via le `common-service` facilitera la sérialisation/désérialisation.

### A. API `document-ingestion-service`
L'`ui-gateway-service` enverra le fichier uploadé via un `multipart/form-data`.

**Endpoint:** `POST /api/v1/ingestion/upload`
*   **Consumes:** `multipart/form-data`
*   **Parameters:**
    *   `file` (MultipartFile)
    *   `chunkingStrategy` (String) - ex: 'Simple', 'Recursive'
*   **Response (JSON):**
    ```json
    {
      "status": "SUCCESS",
      "message": "Fichier document.pdf traité avec succès",
      "chunksIngested": 145
    }
    ```

**Endpoint:** `DELETE /api/v1/ingestion/reset`
*   **Description :** Purger la base de données vectorielle.

### B. API `rag-retriever-service`
Pour l'inférence, une approche Streaming (Server-Sent Events - SSE ou Flux Reactor) est fortement recommandée pour une bonne UX.

**Endpoint:** `POST /api/v1/retriever/ask` (Non-streaming - Optionnel)
*   **Request Body (JSON):**
    ```json
    {
      "question": "Quelle est la définition de...",
      "model": "glm-4.6:cloud"
    }
    ```
*   **Response (JSON):**
    ```json
    {
      "answer": "La définition est...",
      "sources": ["document.pdf", "page 2"]
    }
    ```

**Endpoint:** `POST /api/v1/retriever/stream` (Streaming via Flux / SSE)
*   **Request Body (JSON):** Idem que ci-dessus.
*   **Produces:** `text/event-stream` (Server-Sent Events) ou `application/stream+json`
*   **Comportement :** L'`ui-gateway-service` agira comme un proxy transparent. Il consommera ce flux (ex: avec Spring WebFlux `WebClient`) et le renverra directement au navigateur web de l'utilisateur.

### Implémentation du Streaming (Exemple WebFlux dans l'UI Gateway)
Si vous utilisez Spring MVC standard, il est parfois complexe de faire du streaming transparent de l'API au navigateur. Une solution élégante est que la Gateway appelle l'API et retourne un `Flux<String>` (nécessite `spring-boot-starter-webflux`).
```java
// Dans UI Gateway Service
@PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> streamChat(@RequestBody ChatRequest request) {
    return webClient.post()
            .uri(retrieverUrl + "/api/v1/retriever/stream")
            .bodyValue(request)
            .retrieve()
            .bodyToFlux(String.class);
}
```
Dans le Retriever Service, l'API retourne directement le flux issu du `ChatClient` Spring AI.
