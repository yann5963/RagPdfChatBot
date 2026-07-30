# Assistant RAG - Architecture Microservices

Ce projet est un assistant de recherche augmentée par génération (RAG) utilisant Spring Boot, Spring AI, PostgreSQL (pgvector) et Ollama. L'interface graphique est basée sur le Système de Design de l'État (DSFR).

## 🏗️ Architecture Multi-Modules

Le projet a été refactorisé depuis un monolithe vers une architecture orientée microservices composée des modules suivants :

1. **`common-service`** : Contient les objets de transferts de données (DTOs) partagés entre les services, comme `ChatRequest` et `ChatResponse`.
2. **`document-ingestion-service`** (Port 8081) : Responsable de la réception des PDF, du découpage du texte (chunking via Tika) et de la vectorisation via Ollama. Il écrit directement dans la base de données PostgreSQL (pgvector).
3. **`rag-retriever-service`** (Port 8082) : Responsable de l'interrogation de la base vectorielle, de la construction des prompts RAG et de l'inférence LLM via Ollama Chat.
4. **`ui-gateway-service`** (Port 8080) : Le point d'entrée Frontend (Thymeleaf). Il agit comme un proxy API et route les requêtes des utilisateurs vers les deux backends (`ingestion` ou `retriever`).

---

## 📋 Prérequis

### 1. Ollama (Docker)
Ollama doit être installé et disponible. Une solution recommandée est de l'exécuter dans un container Docker.
Une fois Ollama lancé, téléchargez les modèles nécessaires :

```bash
docker exec -it ollama ollama pull nomic-embed-text-v2-moe:latest
docker exec -it ollama ollama pull glm-4.6:cloud
docker exec -it ollama ollama pull llama3.2:3b
docker exec -it ollama ollama pull qwen3:4b
```

### 2. PostgreSQL avec pgvector
Créez un fichier `docker-compose.yml` à la racine du projet (ou utilisez celui inclus) avec la configuration demandée :

```yaml
services:
  postgres-db:
    image: pgvector/pgvector:pg17
    container_name: spring-ai-pgvector
    restart: always
    ports:
      - "5432:5432"
    environment:
      - POSTGRES_DB=vector_db
      - POSTGRES_USER=yann
      - POSTGRES_PASSWORD=postgresql
      - POSTGRES_INITDB_ARGS=--auth-host=scram-sha-256
    volumes:
      - E:/dev/data/pgvector:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U yann -d vector_db"]
      interval: 10s
      timeout: 5s
      retries: 5
```

Démarrez le service de base de données :
```bash
docker-compose up -d
```

---

## 🚀 Lancement de l'application

### Compilation globale (Maven)
À la racine du projet, compilez tous les modules en même temps :
```bash
mvn clean package -DskipTests
```

### Exécution (Via le script)
Un script batch est fourni pour démarrer facilement tous les microservices en parallèle dans des fenêtres séparées :
```bash
run.bat
```

*Alternativement, vous pouvez démarrer chaque module indépendamment via votre IDE ou via les JARs générés dans les dossiers `target/` respectifs.*

---

## 🧪 Test en Local

Une fois les trois microservices lancés, vous pouvez tester l'application sur votre navigateur :

1. Accédez à l'URL de la Gateway : **[http://localhost:8080](http://localhost:8080)**
2. **Importation :** Utilisez le champ "Téléverser" pour choisir un fichier PDF et cliquez sur "Téléverser et traiter".
3. **Sélection du modèle :** Choisissez l'un des modèles téléchargés (ex: `glm-4.6:cloud`) dans la liste déroulante.
4. **Chat :** Posez votre question dans la zone de texte en bas et appuyez sur "Envoyer".
5. **Réinitialisation :** Utilisez le bouton "RAZ BDD Vectorielle" pour effacer tout le contexte précédemment appris et repartir à zéro.

---

## ⚙️ Configuration
Chaque microservice possède désormais sa propre configuration dans son répertoire `src/main/resources/application.properties`.
- **Ingestion :** `document-ingestion-service/src/main/resources/application.properties`
- **Retriever :** `rag-retriever-service/src/main/resources/application.properties`
- **UI Gateway :** `ui-gateway-service/src/main/resources/application.properties`
