package com.antigravity.rag.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

public class RecursiveCharacterTextSplitter extends TextSplitter {

    private final List<String> separators;
    private final int chunkSize;
    private final int chunkOverlap;
    private final boolean keepSeparator;

    public RecursiveCharacterTextSplitter(int chunkSize, int chunkOverlap, boolean keepSeparator, List<String> separators) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.keepSeparator = keepSeparator;
        this.separators = separators != null ? separators : Arrays.asList("\n\n", "\n", " ", "");
    }

    public RecursiveCharacterTextSplitter(int chunkSize, int chunkOverlap) {
        this(chunkSize, chunkOverlap, true, null);
    }

    @Override
    public List<String> splitText(String text) {
        return doSplitText(text, this.separators);
    }

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
            if (s.length() < chunkSize) {
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

    private List<String> splitOnSeparator(String text, String separator) {
        List<String> splits;
        if (separator.isEmpty()) {
            splits = new ArrayList<>();
            for (char c : text.toCharArray()) {
                splits.add(String.valueOf(c));
            }
        } else {
            if (keepSeparator) {
                // Keep separator with the split (lookbehind)
                // This splits *after* the separator, so the separator stays attached to the *previous* chunk.
                splits = new ArrayList<>(Arrays.asList(text.split("(?<=" + Pattern.quote(separator) + ")")));
            } else {
                splits = new ArrayList<>(Arrays.asList(text.split(Pattern.quote(separator))));
            }
        }
        // Filter empty strings
        splits.removeIf(String::isEmpty);
        return splits;
    }

    private List<String> mergeSplits(List<String> splits, String separator) {
        List<String> docs = new ArrayList<>();
        StringBuilder currentDoc = new StringBuilder();
        int total = 0;

        for (String d : splits) {
            int len = d.length();
            if (total + len > chunkSize) {
                if (total > 0) {
                    docs.add(currentDoc.toString());
                }
                currentDoc = new StringBuilder(d);
                total = len;
            } else {
                currentDoc.append(d);
                total += len;
            }
        }
        if (total > 0) {
            docs.add(currentDoc.toString());
        }
        return docs;
    }
}
