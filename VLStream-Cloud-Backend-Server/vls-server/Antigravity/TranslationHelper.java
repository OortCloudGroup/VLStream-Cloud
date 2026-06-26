package org.springblade.antigravity;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * TranslationHelper is a utility class to automate the process of finding
 * and replacing Chinese text in the project with English translations.
 *
 * @author Antigravity
 */
public class TranslationHelper {

    private static final String WORKSPACE = "d:\\work\\ide\\WorkSpace\\vls-open-source\\vls-server";
    private static final String DICTIONARY_PATH = WORKSPACE + "\\Antigravity\\chinese_dictionary.txt";

    /**
     * Entry point of the helper. Supports two commands: "extract" and "replace".
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please specify command: extract or replace");
            return;
        }

        String command = args[0];
        try {
            if ("extract".equalsIgnoreCase(command)) {
                extract();
            } else if ("replace".equalsIgnoreCase(command)) {
                replace();
            } else {
                System.out.println("Unknown command: " + command);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Scans the project directory for all target files, extracts Chinese texts from them,
     * and writes the unique findings to the dictionary file.
     *
     * @throws IOException if file operations fail
     */
    public static void extract() throws IOException {
        Set<String> uniqueTexts = new TreeSet<>();
        File srcDir = new File(WORKSPACE + "\\src");
        File docDir = new File(WORKSPACE + "\\doc");

        scanAndExtract(srcDir, uniqueTexts);
        scanAndExtract(docDir, uniqueTexts);

        // Save unique texts to dictionary
        List<String> lines = new ArrayList<>();
        for (String text : uniqueTexts) {
            String base64Key = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
            lines.add(base64Key + " ||| " + text.replace("\n", "\\n").replace("\r", "\\r") + " ||| ");
        }

        Files.write(Paths.get(DICTIONARY_PATH), lines, StandardCharsets.UTF_8);
        System.out.println("Extraction completed. Found " + uniqueTexts.size() + " unique Chinese strings.");
        System.out.println("Dictionary saved to: " + DICTIONARY_PATH);
    }

    /**
     * Recursively scans directories to extract Chinese text from matching files.
     *
     * @param dir the directory to scan
     * @param uniqueTexts the set to collect unique Chinese texts
     */
    private static void scanAndExtract(File dir, Set<String> uniqueTexts) {
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                // Skip build and IDE directories
                if (file.getName().equals("target") || file.getName().equals(".idea") || file.getName().equals("Antigravity")) {
                    continue;
                }
                scanAndExtract(file, uniqueTexts);
            } else {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".java") || name.endsWith(".xml") || name.endsWith(".yml") || name.endsWith(".yaml") || name.endsWith(".sql") || name.endsWith(".properties")) {
                    try {
                        extractFromFile(file, uniqueTexts);
                    } catch (Exception e) {
                        System.err.println("Error reading file: " + file.getAbsolutePath() + " - " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Extracts Chinese strings from a single file based on its file type.
     *
     * @param file the file to extract from
     * @param uniqueTexts the set to collect unique Chinese texts
     * @throws IOException if reading the file fails
     */
    private static void extractFromFile(File file, Set<String> uniqueTexts) throws IOException {
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        String name = file.getName().toLowerCase();

        if (name.endsWith(".java")) {
            extractJava(content, uniqueTexts);
        } else if (name.endsWith(".xml")) {
            extractXml(content, uniqueTexts);
        } else if (name.endsWith(".yml") || name.endsWith(".yaml")) {
            extractYaml(content, uniqueTexts);
        } else if (name.endsWith(".sql")) {
            extractSql(content, uniqueTexts);
        } else if (name.endsWith(".properties")) {
            extractProperties(content, uniqueTexts);
        }
    }

    /**
     * Extracts Chinese comments and string literals from Java file content.
     *
     * @param content the content of the Java file
     * @param uniqueTexts the set to collect unique Chinese texts
     */
    private static void extractJava(String content, Set<String> uniqueTexts) {
        // Match string literals: "([^"\\]|\\.)*"
        Pattern strPattern = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"");
        Matcher strMatcher = strPattern.matcher(content);
        while (strMatcher.find()) {
            String str = strMatcher.group();
            // Remove outer quotes
            if (str.length() >= 2) {
                String inner = str.substring(1, str.length() - 1);
                if (hasChinese(inner)) {
                    uniqueTexts.add(inner);
                }
            }
        }

        // Match line comments: //...
        Pattern lineCommentPattern = Pattern.compile("//.*");
        Matcher lineMatcher = lineCommentPattern.matcher(content);
        while (lineMatcher.find()) {
            String comment = lineMatcher.group().substring(2).trim();
            if (hasChinese(comment)) {
                uniqueTexts.add(comment);
            }
        }

        // Match block comments: /* ... */
        Pattern blockCommentPattern = Pattern.compile("/\\*[\\s\\S]*?\\*/");
        Matcher blockMatcher = blockCommentPattern.matcher(content);
        while (blockMatcher.find()) {
            String comment = blockMatcher.group();
            // Remove /* and */
            if (comment.length() >= 4) {
                String inner = comment.substring(2, comment.length() - 2).trim();
                // Split by line to avoid importing huge multi-line blocks as a single item if they contain only small comments
                String[] lines = inner.split("\\r?\\n");
                for (String line : lines) {
                    String cleanLine = line.replaceAll("^\\s*\\*+", "").trim();
                    if (hasChinese(cleanLine)) {
                        uniqueTexts.add(cleanLine);
                    }
                }
            }
        }
    }

    /**
     * Extracts Chinese text and comments from XML file content.
     *
     * @param content the content of the XML file
     * @param uniqueTexts the set to collect unique Chinese texts
     */
    private static void extractXml(String content, Set<String> uniqueTexts) {
        // Match comments: <!-- ... -->
        Pattern commentPattern = Pattern.compile("<!--[\\s\\S]*?-->");
        Matcher commentMatcher = commentPattern.matcher(content);
        while (commentMatcher.find()) {
            String comment = commentMatcher.group();
            if (comment.length() >= 7) {
                String inner = comment.substring(4, comment.length() - 3).trim();
                if (hasChinese(inner)) {
                    uniqueTexts.add(inner);
                }
            }
        }

        // Match XML tags content: >text<
        Pattern tagPattern = Pattern.compile(">([^<\\r\\n]+)<");
        Matcher tagMatcher = tagPattern.matcher(content);
        while (tagMatcher.find()) {
            String text = tagMatcher.group(1).trim();
            if (hasChinese(text)) {
                uniqueTexts.add(text);
            }
        }

        // Match attributes: name="value" or name='value'
        Pattern attrPattern = Pattern.compile("=\\s*\"([^\"]*)\"|=\\s*'([^']*)'");
        Matcher attrMatcher = attrPattern.matcher(content);
        while (attrMatcher.find()) {
            String val = attrMatcher.group(1) != null ? attrMatcher.group(1) : attrMatcher.group(2);
            if (val != null && hasChinese(val)) {
                uniqueTexts.add(val);
            }
        }
    }

    /**
     * Extracts Chinese values and comments from YAML file content.
     *
     * @param content the content of the YAML file
     * @param uniqueTexts the set to collect unique Chinese texts
     */
    private static void extractYaml(String content, Set<String> uniqueTexts) {
        String[] lines = content.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#")) {
                String comment = trimmed.substring(1).trim();
                if (hasChinese(comment)) {
                    uniqueTexts.add(comment);
                }
            } else if (trimmed.contains(":")) {
                int colonIdx = trimmed.indexOf(":");
                String val = trimmed.substring(colonIdx + 1).trim();
                // strip quotes if any
                if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                    val = val.substring(1, val.length() - 1);
                } else if (val.startsWith("'") && val.endsWith("'") && val.length() >= 2) {
                    val = val.substring(1, val.length() - 1);
                }
                // Handle inline comments
                if (val.contains("#")) {
                    int hashIdx = val.indexOf("#");
                    String inlineComment = val.substring(hashIdx + 1).trim();
                    if (hasChinese(inlineComment)) {
                        uniqueTexts.add(inlineComment);
                    }
                    val = val.substring(0, hashIdx).trim();
                }
                if (hasChinese(val)) {
                    uniqueTexts.add(val);
                }
            }
        }
    }

    /**
     * Extracts Chinese comments and table/field comment strings from SQL file content.
     * Note: We exclude general INSERT statements to avoid translating system data.
     *
     * @param content the content of the SQL file
     * @param uniqueTexts the set to collect unique Chinese texts
     */
    private static void extractSql(String content, Set<String> uniqueTexts) {
        // Match line comments: -- ...
        Pattern linePattern = Pattern.compile("--.*");
        Matcher lineMatcher = linePattern.matcher(content);
        while (lineMatcher.find()) {
            String comment = lineMatcher.group().substring(2).trim();
            if (hasChinese(comment)) {
                uniqueTexts.add(comment);
            }
        }

        // Match column/table comments: COMMENT '...' or COMMENT "..."
        Pattern commentAttrPattern = Pattern.compile("COMMENT\\s+'([^']*)'|COMMENT\\s+\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
        Matcher commentAttrMatcher = commentAttrPattern.matcher(content);
        while (commentAttrMatcher.find()) {
            String val = commentAttrMatcher.group(1) != null ? commentAttrMatcher.group(1) : commentAttrMatcher.group(2);
            if (val != null && hasChinese(val)) {
                uniqueTexts.add(val);
            }
        }
    }

    /**
     * Extracts Chinese strings from properties file content.
     *
     * @param content the content of the properties file
     * @param uniqueTexts the set to collect unique Chinese texts
     */
    private static void extractProperties(String content, Set<String> uniqueTexts) {
        String[] lines = content.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#") || trimmed.startsWith("!")) {
                String comment = trimmed.substring(1).trim();
                if (hasChinese(comment)) {
                    uniqueTexts.add(comment);
                }
            } else if (trimmed.contains("=")) {
                int eqIdx = trimmed.indexOf("=");
                String val = trimmed.substring(eqIdx + 1).trim();
                if (hasChinese(val)) {
                    uniqueTexts.add(val);
                }
            }
        }
    }

    /**
     * Checks if a string contains at least one Chinese character.
     *
     * @param s the string to check
     * @return true if string contains Chinese character, false otherwise
     */
    public static boolean hasChinese(String s) {
        if (s == null) return false;
        for (char c : s.toCharArray()) {
            if (c >= '\u4e00' && c <= '\u9fa5') {
                return true;
            }
        }
        return false;
    }

    /**
     * TranslationPair represents a mapping from original Chinese text to its English translation.
     */
    private static class TranslationPair {
        final String original;
        final String translation;

        TranslationPair(String original, String translation) {
            this.original = original;
            this.translation = translation;
        }
    }

    /**
     * Reads the translated dictionary, decodes the translation mapping,
     * and performs substitutions across all project files.
     *
     * @throws IOException if file operations fail
     */
    public static void replace() throws IOException {
        File dictFile = new File(DICTIONARY_PATH);
        if (!dictFile.exists()) {
            System.out.println("Dictionary file not found: " + DICTIONARY_PATH);
            return;
        }

        List<TranslationPair> pairs = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(dictFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" \\|\\|\\| ");
                if (parts.length >= 2) {
                    String base64Key = parts[0].replaceAll("\\s", "");
                    String original = new String(Base64.getDecoder().decode(base64Key), StandardCharsets.UTF_8);
                    String translation = parts.length >= 3 ? parts[2].trim() : "";
                    if (!translation.isEmpty()) {
                        pairs.add(new TranslationPair(original, translation));
                    }
                }
            }
        }

        if (pairs.isEmpty()) {
            System.out.println("No translations found in dictionary.");
            return;
        }

        // CRITICAL: Sort pairs by length of original Chinese text descending.
        // This ensures longer strings are replaced before their substrings.
        pairs.sort((a, b) -> Integer.compare(b.original.length(), a.original.length()));

        File srcDir = new File(WORKSPACE + "\\src");
        File docDir = new File(WORKSPACE + "\\doc");

        int[] modifiedCount = {0};
        scanAndReplace(srcDir, pairs, modifiedCount);
        scanAndReplace(docDir, pairs, modifiedCount);

        System.out.println("Replacement completed. Modified " + modifiedCount[0] + " files.");
    }

    /**
     * Recursively scans directories to apply replacements.
     *
     * @param dir the directory to scan
     * @param pairs the sorted list of translation pairs
     * @param modifiedCount array to accumulate modified files count
     */
    private static void scanAndReplace(File dir, List<TranslationPair> pairs, int[] modifiedCount) {
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                if (file.getName().equals("target") || file.getName().equals(".idea") || file.getName().equals("Antigravity")) {
                    continue;
                }
                scanAndReplace(file, pairs, modifiedCount);
            } else {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".java") || name.endsWith(".xml") || name.endsWith(".yml") || name.endsWith(".yaml") || name.endsWith(".sql") || name.endsWith(".properties")) {
                    try {
                        if (replaceInFile(file, pairs)) {
                            modifiedCount[0]++;
                        }
                    } catch (Exception e) {
                        System.err.println("Error processing file: " + file.getAbsolutePath() + " - " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Replaces Chinese strings with translations in a single file if matches are found.
     *
     * @param file the file to modify
     * @param pairs the sorted list of translation pairs
     * @return true if the file was modified, false otherwise
     * @throws IOException if file operations fail
     */
    private static boolean replaceInFile(File file, List<TranslationPair> pairs) throws IOException {
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        String originalContent = content;

        boolean modified = false;
        for (TranslationPair pair : pairs) {
            if (content.contains(pair.original)) {
                content = content.replace(pair.original, pair.translation);
                modified = true;
            }
        }

        if (modified && !content.equals(originalContent)) {
            Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
            System.out.println("Updated: " + file.getAbsolutePath());
            return true;
        }
        return false;
    }
}
