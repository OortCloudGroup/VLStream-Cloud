package com.ruoyi.vlstream.test.vlstream.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SEARCH/REPLACE edits against the saved page. Untouched code is kept byte for byte, which is what lets
 * a follow-up such as "keep the campus, add a section view" retain earlier buildings, controls and bindings.
 */
final class VideoSquareScenePatch {
    static final String SEARCH = "<<<<<<< SEARCH";
    private static final Pattern BLOCK = Pattern.compile(
        "<<<<<<< SEARCH[ \\t]*\\r?\\n(.*?)\\r?\\n=======[ \\t]*\\r?\\n(.*?)\\r?\\n?>>>>>>> REPLACE", Pattern.DOTALL);
    private static final Pattern FENCE_LINE = Pattern.compile("(?m)^[ \\t]*```[a-zA-Z]*[ \\t]*$\\r?\\n?");

    private VideoSquareScenePatch() { }

    static final class Block {
        final String search;
        final String replace;

        Block(String search, String replace) {
            this.search = search;
            this.replace = replace;
        }
    }

    /** Signals that the model's edit does not fit the saved page, so the caller can request a whole page instead. */
    static final class Mismatch extends RuntimeException {
        Mismatch(String message) {
            super(message);
        }
    }

    /** Parses every complete block. A trailing, still-streaming block is left for the next call. */
    static List<Block> parse(String text) {
        String source = FENCE_LINE.matcher(text).replaceAll("");
        List<Block> blocks = new ArrayList<>();
        Matcher matcher = BLOCK.matcher(source);
        while (matcher.find()) blocks.add(new Block(matcher.group(1), matcher.group(2)));
        return blocks;
    }

    static String apply(String document, Block block, int index) {
        if (block.search.trim().isEmpty()) {
            throw new Mismatch("第 " + index + " 段修改缺少要替换的原始代码");
        }
        int at = document.indexOf(block.search);
        if (at >= 0) {
            if (document.indexOf(block.search, at + 1) >= 0) {
                throw new Mismatch("第 " + index + " 段修改在当前页面中出现多处，无法确定位置");
            }
            return document.substring(0, at) + block.replace + document.substring(at + block.search.length());
        }
        return applyIgnoringIndentation(document, block, index);
    }

    /** Spacing inside a line carries no meaning in the pages we generate, and models often re-flow it. */
    private static String normalized(String line) {
        return line.trim().replaceAll("\\s+", " ").replaceAll(" ?([{}()\\[\\];,:=<>+*/-]) ?", "$1");
    }

    /** Models often re-indent or re-space copied lines. Match line by line on normalized content, but only when unique. */
    private static String applyIgnoringIndentation(String document, Block block, int index) {
        String[] lines = document.split("\n", -1);
        List<String> wanted = new ArrayList<>();
        for (String line : block.search.split("\n", -1)) {
            if (!line.trim().isEmpty()) wanted.add(normalized(line));
        }
        int found = -1, end = -1;
        for (int start = 0; start < lines.length; start++) {
            int cursor = start, matched = 0;
            while (cursor < lines.length && matched < wanted.size()) {
                String line = lines[cursor].trim();
                if (line.isEmpty() && matched > 0) { cursor++; continue; }
                if (!normalized(line).equals(wanted.get(matched))) break;
                matched++;
                cursor++;
            }
            if (matched == wanted.size()) {
                if (found >= 0) throw new Mismatch("第 " + index + " 段修改在当前页面中出现多处，无法确定位置");
                found = start;
                end = cursor;
            }
        }
        if (found < 0) throw new Mismatch("第 " + index + " 段修改没有在当前页面中找到对应代码");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < found; i++) result.append(lines[i]).append('\n');
        result.append(block.replace);
        if (end < lines.length) {
            result.append('\n');
            for (int i = end; i < lines.length; i++) {
                result.append(lines[i]);
                if (i < lines.length - 1) result.append('\n');
            }
        }
        return result.toString();
    }
}
