package com.antigravity.rag.service;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RecursiveCharacterTextSplitterTest {

    @Test
    void testSplitByParagraphs() {
        RecursiveCharacterTextSplitter splitter = new RecursiveCharacterTextSplitter(5, 0);
        // "Hello\n\nWorld" -> "Hello\n\n" (7) > 5. "World" (5).
        // "Hello\n\n" -> "Hello\n" (6) > 5. "\n" (1).
        // "Hello\n" -> "Hello" (5). "\n" (1).
        // So: "Hello", "\n", "\n", "World".

        List<String> chunks = splitter.splitText("Hello\n\nWorld");

        assertEquals(4, chunks.size());
        assertEquals("Hello", chunks.get(0));
        assertEquals("\n", chunks.get(1));
        assertEquals("\n", chunks.get(2));
        assertEquals("World", chunks.get(3));
    }

    @Test
    void testSimpleSplit() {
        RecursiveCharacterTextSplitter splitter = new RecursiveCharacterTextSplitter(100, 0);
        String text = "This is a paragraph.\n\nThis is another paragraph.";
        List<String> chunks = splitter.splitText(text);

        assertEquals(1, chunks.size());
        assertEquals(text, chunks.get(0));
    }

    @Test
    void testSplitBehavior() {
        String text = "Paragraph 1.\n\nPara 2.";
        RecursiveCharacterTextSplitter splitter = new RecursiveCharacterTextSplitter(15, 0);
        List<String> chunks = splitter.splitText(text);

        assertEquals(2, chunks.size());
        assertEquals("Paragraph 1.\n\n", chunks.get(0));
        assertEquals("Para 2.", chunks.get(1));
    }

    @Test
    void testWordSplit() {
        String text = "A very long sentence";
        RecursiveCharacterTextSplitter splitter = new RecursiveCharacterTextSplitter(5, 0);
        List<String> chunks = splitter.splitText(text);

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() >= 4);
    }
}
