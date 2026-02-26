package com.antigravity.rag;

import org.springframework.ai.autoconfigure.vectorstore.pgvector.PgVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.io.InputStream;

/**
 * Classe principale de l'application RAG (Retrieval-Augmented Generation).
 * Démarre le contexte Spring Boot et configure les correctifs nécessaires pour
 * l'intégration Ollama.
 */
@SpringBootApplication(exclude = { PgVectorStoreAutoConfiguration.class })
public class RagDemoApplication {

    /**
     * Point d'entrée principal de l'application Spring Boot.
     *
     * @param args arguments de la ligne de commande
     */
    public static void main(String[] args) {
        SpringApplication.run(RagDemoApplication.class, args);
    }

    /**
     * Bean permettant de corriger le type de contenu renvoyé par Ollama.
     * Certains appels à Ollama peuvent retourner "text/plain" au lieu de
     * "application/json",
     * ce qui provoque des erreurs de désérialisation dans Spring AI.
     * Cet intercepteur force le Content-Type à "application/json" si la réponse est
     * en "text/plain".
     *
     * @return le customizer de RestClient
     */
    @Bean
    public RestClientCustomizer ollamaContentTypeFixer() {
        return builder -> builder.requestInterceptor((request, body, execution) -> {
            ClientHttpResponse response = execution.execute(request, body);
            return new ClientHttpResponse() {
                @Override
                public org.springframework.http.HttpStatusCode getStatusCode() throws IOException {
                    return response.getStatusCode();
                }

                @Override
                public int getRawStatusCode() throws IOException {
                    return response.getStatusCode().value();
                }

                @Override
                public String getStatusText() throws IOException {
                    return response.getStatusText();
                }

                @Override
                public void close() {
                    response.close();
                }

                @Override
                public InputStream getBody() throws IOException {
                    return response.getBody();
                }

                @Override
                public HttpHeaders getHeaders() {
                    HttpHeaders headers = new HttpHeaders();
                    headers.putAll(response.getHeaders());
                    MediaType contentType = headers.getContentType();
                    if (contentType != null
                            && contentType.isCompatibleWith(MediaType.TEXT_PLAIN)) {
                        headers.setContentType(MediaType.APPLICATION_JSON);
                    }
                    return headers;
                }
            };
        });
    }
}
