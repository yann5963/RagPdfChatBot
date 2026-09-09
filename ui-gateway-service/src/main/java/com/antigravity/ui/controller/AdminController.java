package com.antigravity.ui.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final String LOG_FILE_PATH = "logs/gateway.log";

    @GetMapping("/logs")
    public String viewLogs(Model model) {
        List<String> logs;
        try {
            Path path = Paths.get(LOG_FILE_PATH);
            if (Files.exists(path)) {
                List<String> allLines = Files.readAllLines(path);
                int start = Math.max(0, allLines.size() - 100);
                logs = allLines.subList(start, allLines.size());
            } else {
                logs = Collections.singletonList("Le fichier de log " + LOG_FILE_PATH + " n'existe pas encore.");
            }
        } catch (IOException e) {
            logs = Collections.singletonList("Erreur lors de la lecture des logs : " + e.getMessage());
        }

        model.addAttribute("logs", String.join("\n", logs));
        return "logs";
    }
}
