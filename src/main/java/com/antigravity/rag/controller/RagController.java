package com.antigravity.rag.controller;

import com.antigravity.rag.service.RagService;
import com.antigravity.rag.service.VectorIngestionService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
public class RagController {

    private final VectorIngestionService ingestionService;
    private final RagService ragService;

    /**
     * Constructeur injectant les services nécessaires pour l'ingestion et le chat.
     *
     * @param ingestionService Service gérant l'ingestion des documents vectorisés.
     * @param ragService       Service gérant les appels à l'assistant LLM.
     */
    public RagController(VectorIngestionService ingestionService, RagService ragService) {
        this.ingestionService = ingestionService;
        this.ragService = ragService;
    }

    @Value("${app.available-models}")
    private List<String> availableModels;

    /**
     * Affiche la page d'accueil avec l'interface de chat.
     * Fournit à la vue la liste des modèles LLM disponibles.
     *
     * @param model Modèle Thymeleaf pour passer des attributs à la vue.
     * @return Le nom du template de la vue (chat.html).
     */
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("availableModels", availableModels);
        return "chat";
    }

    /**
     * Point de terminaison API pour téléverser et vectoriser un fichier PDF.
     *
     * @param file Fichier PDF envoyé par l'utilisateur.
     * @return Une Map contenant le statut du traitement et un message.
     */
    @PostMapping("/api/ingest")
    @ResponseBody
    public Map<String, String> ingest(@RequestParam("file") MultipartFile file) {
        try {
            ingestionService.ingest(file);
            return Map.of("status", "success", "message", "File ingested successfully");
        } catch (IOException e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    /**
     * Point de terminaison API pour traiter les questions de l'utilisateur.
     * Récupère la réponse de l'assistant LLM sélectionné.
     *
     * @param payload Map contenant la question de l'utilisateur et le modèle
     *                choisi.
     * @return Une Map contenant la réponse générée.
     */
    @PostMapping("/api/chat")
    @ResponseBody
    public Map<String, String> chat(@RequestBody Map<String, String> payload) {
        String question = payload.get("question");
        String model = payload.getOrDefault("model", "glm-4.6:cloud");
        String answer = ragService.generateResponse(question, model);
        return Map.of("answer", answer);
    }
}
