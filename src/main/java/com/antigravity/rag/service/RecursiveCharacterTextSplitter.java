package com.antigravity.rag.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.ai.transformer.splitter.TextSplitter;

/**
 * Diviseur de texte récursif basé sur les caractères.
 * Cette classe découpe un texte en morceaux de taille définie en utilisant une
 * liste de séparateurs de manière hiérarchique.
 * Elle tente d'abord de diviser par les séparateurs de plus haut niveau (ex:
 * doubles sauts de ligne),
 * puis descend récursivement jusqu'à ce que les morceaux respectent la taille
 * maximale (chunkSize).
 */
public class RecursiveCharacterTextSplitter extends TextSplitter {

    private final List<String> separators;
    private final int chunkSize;
    private final int chunkOverlap;
    private final boolean keepSeparator;

    /**
     * Constructeur complet.
     *
     * @param chunkSize     Taille maximale d'un morceau de texte.
     * @param chunkOverlap  Superposition entre deux morceaux consécutifs (non
     *                      exploitée totalement ici).
     * @param keepSeparator Indique s'il faut conserver le séparateur dans le
     *                      morceau.
     * @param separators    Liste ordonnée de séparateurs à utiliser.
     */
    public RecursiveCharacterTextSplitter(int chunkSize, int chunkOverlap, boolean keepSeparator,
            List<String> separators) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.keepSeparator = keepSeparator;
        this.separators = separators != null ? separators : Arrays.asList("\n\n", "\n", " ", "");
    }

    /**
     * Constructeur simplifié avec séparateurs par défaut.
     *
     * @param chunkSize    Taille maximale d'un morceau.
     * @param chunkOverlap Superposition.
     */
    public RecursiveCharacterTextSplitter(int chunkSize, int chunkOverlap) {
        this(chunkSize, chunkOverlap, true, null);
    }

    /**
     * Implémentation de la méthode de séparation de texte.
     *
     * @param text Texte à diviser.
     * @return Liste de morceaux de texte.
     */
    @Override
    public List<String> splitText(String text) {
        return doSplitText(text, this.separators);
    }

    /**
     * Logique de découpage récursif utilisant une liste de séparateurs.
     *
     * @param text       Texte à découper.
     * @param separators Séparateurs à tester.
     * @return Liste de morceaux découpés.
     */
    private List<String> doSplitText(String text, List<String> separators) {
        List<String> finalChunks = new ArrayList<>();
        String separator = "";
        List<String> newSeparators = new ArrayList<>();

        boolean found = false;
        for (int i = 0; i < separators.size(); i++) {
            String s = separators.get(i);
            if (s.isEmpty()) {
                separator = s;
                found = true;
                break;
            }
            if (text.contains(s)) {
                separator = s;
                newSeparators = separators.subList(i + 1, separators.size());
                found = true;
                break;
            }
        }

        // If no separator found, use characters (empty string separator)
        if (!found) {
            separator = "";
        }

        List<String> splits = splitOnSeparator(text, separator);
        List<String> goodSplits = new ArrayList<>();

        for (String s : splits) {
            if (s.length() <= chunkSize) { // Optimisé: un morceau égal à la chunkSize est valide
                goodSplits.add(s);
            } else {
                if (!goodSplits.isEmpty()) {
                    mergeSplits(goodSplits, separator).forEach(finalChunks::add);
                    goodSplits.clear();
                }
                if (newSeparators.isEmpty()) {
                    finalChunks.add(s);
                } else {
                    finalChunks.addAll(doSplitText(s, newSeparators));
                }
            }
        }

        if (!goodSplits.isEmpty()) {
            mergeSplits(goodSplits, separator).forEach(finalChunks::add);
        }

        return finalChunks;
    }

    /**
     * Divise le texte sur un séparateur spécifique.
     *
     * @param text      Texte à diviser.
     * @param separator Séparateur utilisé.
     * @return Liste des segments obtenus.
     */
    private List<String> splitOnSeparator(String text, String separator) {
        List<String> splits;
        if (separator.isEmpty()) {
            splits = new ArrayList<>();
            for (char c : text.toCharArray()) {
                splits.add(String.valueOf(c));
            }
        } else {
            if (keepSeparator) {
                // Conserve le séparateur avec le split (lookbehind)
                // Le séparateur reste attaché au morceau précédent.
                splits = new ArrayList<>(Arrays.asList(text.split("(?<=" + Pattern.quote(separator) + ")")));
            } else {
                splits = new ArrayList<>(Arrays.asList(text.split(Pattern.quote(separator))));
            }
        }
        // Filtrer les chaînes vides
        splits.removeIf(String::isEmpty);
        return splits;
    }

    /**
     * Fusionne les segments pour former des morceaux (chunks) dont la taille est
     * au maximum chunkSize. Cette méthode implémente la fenêtre glissante (overlap)
     * et la restauration éventuelle du séparateur (si keepSeparator est faux).
     *
     * @param splits    Liste de segments à fusionner.
     * @param separator Séparateur qui était utilisé entre les segments.
     * @return Liste de morceaux fusionnés avec prise en compte du recouvrement.
     */
    private List<String> mergeSplits(List<String> splits, String separator) {
        List<String> docs = new ArrayList<>();
        List<String> currentDoc = new ArrayList<>();
        int total = 0;
        String joinStr = keepSeparator ? "" : separator;
        int joinLen = joinStr.length();

        for (String d : splits) {
            int len = d.length();
            int lenToAdd = len + (currentDoc.isEmpty() ? 0 : joinLen);

            if (total + lenToAdd > chunkSize) {
                if (total > 0) {
                    docs.add(String.join(joinStr, currentDoc));

                    // Réduction de currentDoc pour respecter le chunkOverlap (fenêtre glissante)
                    while (total > chunkOverlap
                            || (total + len + (currentDoc.isEmpty() ? 0 : joinLen) > chunkSize && total > 0)) {
                        String removed = currentDoc.remove(0);
                        int lenToRemove = removed.length() + (currentDoc.isEmpty() ? 0 : joinLen);
                        total -= lenToRemove;
                    }
                    // Recalculer lenToAdd après la réduction car currentDoc a peut-être été vidé
                    lenToAdd = len + (currentDoc.isEmpty() ? 0 : joinLen);
                }
            }
            currentDoc.add(d);
            total += lenToAdd;
        }

        if (total > 0) {
            docs.add(String.join(joinStr, currentDoc));
        }

        return docs;
    }
}
