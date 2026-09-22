# Analyse : Intégration de GitHub Copilot (via API standard) dans le projet RAG

Cette analyse détaille les modifications à apporter au projet pour intégrer GitHub Copilot (GitHub Models / API compatible OpenAI) pour la génération de réponses, tout en conservant Ollama pour la vectorisation et comme option alternative pour la génération.

L'objectif est de permettre à l'utilisateur de choisir entre un modèle local (Ollama) et un modèle distant (GitHub Copilot) depuis l'interface utilisateur.

## 1. Modifications des dépendances (Maven)

L'API de GitHub Models (GitHub Copilot) est compatible avec le standard OpenAI. Nous pouvons donc utiliser le starter OpenAI de Spring AI pour communiquer avec GitHub.

**Fichier :** `rag-retriever-service/pom.xml`

**Action :** Ajouter la dépendance Spring AI pour OpenAI.

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>
```

*(Ollama reste présent grâce à `spring-ai-ollama-spring-boot-starter` pour la vectorisation et la génération locale).*

## 2. Configuration des propriétés (`application.properties`)

Il faut configurer la clé API et l'URL de base pour pointer vers l'API de GitHub au lieu de celle d'OpenAI par défaut. Il faut également ajouter le(s) modèle(s) GitHub dans la liste des modèles disponibles pour l'interface utilisateur.

**Fichier :** `rag-retriever-service/src/main/resources/application.properties`

**Action :** Ajouter la configuration OpenAI pointant vers GitHub.

```properties
# Configuration GitHub Copilot (via API OpenAI compatible)
spring.ai.openai.api-key=${GITHUB_TOKEN}
spring.ai.openai.base-url=https://models.inference.ai.azure.com
```

**Fichier :** `ui-gateway-service/src/main/resources/application.properties`

**Action :** Ajouter un modèle GitHub (ex: `gpt-4o`) dans la liste des modèles disponibles dans l'UI.

```properties
app.available-models=glm-4.6:cloud,qwen3:4b,llama3.2:3b,gemma4:e2b,gpt-4o
```

## 3. Modification de la logique métier (`RagService.java`)

Actuellement, le `RagService` injecte un `ChatClient.Builder` qui s'appuie implicitement sur le modèle Ollama (qui est le seul présent dans le contexte Spring). En ajoutant le starter OpenAI, Spring va créer plusieurs beans de type `ChatModel` (un `OllamaChatModel` et un `OpenAiChatModel`). Il faut donc injecter explicitement les deux modèles et router la requête vers le bon modèle en fonction du choix de l'utilisateur.

**Fichier :** `rag-retriever-service/src/main/java/com/antigravity/retriever/service/RagService.java`

**Action :**
1. Injecter `OllamaChatModel` et `OpenAiChatModel`.
2. Créer deux instances de `ChatClient`, une pour Ollama et une pour GitHub (OpenAI).
3. Modifier la méthode `generateResponse(String message, String model)` pour vérifier si le modèle demandé est un modèle Ollama ou GitHub, et utiliser le client approprié avec la bonne classe d'options (`OllamaOptions` vs `OpenAiChatOptions`).

**Exemple de logique à implémenter :**

```java
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.chat.client.ChatClient;
// ...

@Service
public class RagService {

    private final ChatClient ollamaChatClient;
    private final ChatClient openAiChatClient;
    private final VectorStore vectorStore;

    public RagService(OllamaChatModel ollamaChatModel,
                      OpenAiChatModel openAiChatModel,
                      VectorStore vectorStore) {
        this.ollamaChatClient = ChatClient.builder(ollamaChatModel).build();
        this.openAiChatClient = ChatClient.builder(openAiChatModel).build();
        this.vectorStore = vectorStore;
    }

    public String generateResponse(String message, String model) {
        // ... (Recherche vectorielle inchangée) ...

        boolean isGitHubModel = model.startsWith("gpt-") || model.equals("github-model-name");

        String response;
        if (isGitHubModel) {
            response = openAiChatClient.prompt()
                    .user(message)
                    .options(OpenAiChatOptions.builder().withModel(model).build())
                    .system(s -> s.text(ragPromptTemplate)
                            .param("documents", content)
                            .param("input", message))
                    .call()
                    .content();
        } else {
            response = ollamaChatClient.prompt()
                    .user(message)
                    .options(OllamaOptions.builder().withModel(model).build())
                    .system(s -> s.text(ragPromptTemplate)
                            .param("documents", content)
                            .param("input", message))
                    .call()
                    .content();
        }

        // ... (Calcul du temps inchangé) ...
        return response + timeMessage;
    }
}
```

*Note: La gestion exacte des beans peut nécessiter l'utilisation de `@Qualifier` dans d'autres classes si elles injectent `ChatModel` directement, selon la façon dont Spring Boot auto-configure les modèles avec les multiples starters.*

## 4. Modifications dans l'Interface Utilisateur (UI)

La bonne nouvelle est que l'interface utilisateur actuelle (`chat.html`) et le `GatewayController` sont déjà dynamiques ! La boîte de sélection dans l'interface boucle sur `app.available-models` pour afficher les options.

Aucune modification n'est nécessaire dans `chat.html` ou `GatewayController.java` à part l'ajout du modèle dans `application.properties` (étape 2). Le modèle sélectionné sera envoyé en tant que paramètre `model` dans le payload JSON au backend `rag-retriever-service`.

## 5. Résumé des points d'attention
- **Vectorisation** : Elle n'est pas touchée. Spring AI utilisera toujours le `OllamaEmbeddingModel` configuré dans `VectorStoreConfig.java` pour l'ingestion et la recherche.
- **Conflits de Beans** : En ayant deux starters AI (Ollama et OpenAI), Spring Boot va tenter d'auto-configurer les deux. Il faudra s'assurer que les injections de dépendances précisent explicitement quel modèle utiliser (`OllamaChatModel` ou `OpenAiChatModel`).
- **Gestion de la clé API** : Il est recommandé de passer la clé API GitHub (`GITHUB_TOKEN`) sous forme de variable d'environnement pour des raisons de sécurité, plutôt que de la coder en dur dans `application.properties`.
