package com.ruoyi.vlstream.test.vlstream.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Coalesces model deltas into inert drafts. The model either writes a whole page or SEARCH/REPLACE edits
 * against the saved page. Only the saved event carries an executable revision.
 */
final class VideoSquareSceneStream {
    static final String FULL = "full";
    static final String PATCH = "patch";
    private static final int MAX_CHARS = 160 * 1024;
    private static final int MAX_PAGE_CHARS = 128 * 1024;
    private static final int PREFACE_CHARS = 600;
    private static final Pattern OPEN = Pattern.compile("(?is)^(?:<!doctype\\s+html\\s*>\\s*)?<html(?:\\s[^>]*)?>");
    private static final Pattern START = Pattern.compile("(?i)<!doctype\\s+html|<html[\\s>]");
    private static final Pattern TITLE = Pattern.compile("(?is)<title(?:\\s[^>]*)?>([^<>]{1,512})</title\\s*>");
    private static final Pattern BODY_OPEN = Pattern.compile("(?is)<body(?:\\s[^>]*)?>");
    private static final Pattern DOCUMENT_END = Pattern.compile("(?i)</body\\s*>\\s*</html\\s*>$");
    private static final Pattern CLOSE = Pattern.compile("(?i)</html\\s*>");
    private static final Pattern FENCE = Pattern.compile("(?i)^" + "```(?:html)?[ \\t]*\\r?\\n");
    private final Consumer<ObjectNode> preview;
    private final String fallbackTitle;
    private final String base;
    private final StringBuilder buffer = new StringBuilder();
    private String kind;
    private String patched;
    private int appliedBlocks;
    private int lastSent;

    VideoSquareSceneStream(String mode, Consumer<ObjectNode> preview) {
        this(mode, null, preview);
    }

    /** @param base the saved page that edits apply to; {@code null} when only a whole page is acceptable */
    VideoSquareSceneStream(String mode, String base, Consumer<ObjectNode> preview) {
        this.preview = preview;
        this.base = base;
        this.patched = base;
        this.fallbackTitle = "bim".equals(mode) ? "BIM视图" : "拓扑视图";
    }

    void accept(String delta) {
        if (delta == null || delta.isEmpty()) return;
        if (delta.length() > MAX_CHARS - buffer.length()) throw new ServiceException("模型生成的页面超过大小限制", 422);
        buffer.append(delta);
        if (kind == null && !detect()) return;
        if (PATCH.equals(kind)) {
            List<VideoSquareScenePatch.Block> blocks = VideoSquareScenePatch.parse(source());
            if (blocks.size() == appliedBlocks) return;
            applyFrom(blocks);
            send(patched, false);
            return;
        }
        if (lastSent != 0 && buffer.length() - lastSent < 1024) return;
        String html = source();
        if (!OPEN.matcher(html).find()) {
            if (html.length() > 256) throw incomplete();
            return;
        }
        send(html, false);
        lastSent = buffer.length();
    }

    /** @throws VideoSquareScenePatch.Mismatch when an edit does not fit, so the caller may ask for a whole page */
    ObjectNode finish() {
        if (kind == null && !detect()) throw incomplete();
        String html;
        if (PATCH.equals(kind)) {
            List<VideoSquareScenePatch.Block> blocks = VideoSquareScenePatch.parse(source());
            if (blocks.isEmpty()) throw new VideoSquareScenePatch.Mismatch("模型没有返回可用的修改片段");
            applyFrom(blocks);
            html = patched;
        } else {
            html = source();
        }
        Matcher body = BODY_OPEN.matcher(html);
        Matcher end = DOCUMENT_END.matcher(html);
        if (!OPEN.matcher(html).find() || !body.find() || !end.find() || body.end() >= end.start()
            || html.substring(body.end(), end.start()).trim().isEmpty()) {
            if (PATCH.equals(kind)) throw new VideoSquareScenePatch.Mismatch("修改后的页面结构不完整");
            throw incomplete();
        }
        if (html.length() > MAX_PAGE_CHARS) throw new ServiceException("生成的页面超过 128 KB，请精简需求或分步修改", 422);
        ObjectNode scene = document(html, true);
        scene.put("edit", kind);
        if (PATCH.equals(kind)) scene.put("edits", appliedBlocks);
        return scene;
    }

    String kind() {
        return kind;
    }

    private void applyFrom(List<VideoSquareScenePatch.Block> blocks) {
        for (int i = appliedBlocks; i < blocks.size(); i++) {
            patched = VideoSquareScenePatch.apply(patched, blocks.get(i), i + 1);
            appliedBlocks = i + 1;
        }
    }

    /** Models sometimes add a short preface despite the contract; skip it, but never a long explanation. */
    private boolean detect() {
        String text = buffer.toString();
        int patchAt = text.indexOf(VideoSquareScenePatch.SEARCH);
        Matcher page = START.matcher(text);
        int pageAt = page.find() ? page.start() : -1;
        if (patchAt >= 0 && patchAt <= PREFACE_CHARS && (pageAt < 0 || patchAt < pageAt)) {
            if (base == null) throw new ServiceException("模型返回了修改片段，但当前没有可修改的页面，请重新生成", 422);
            kind = PATCH;
            return true;
        }
        if (pageAt >= 0 && pageAt <= PREFACE_CHARS) {
            kind = FULL;
            buffer.delete(0, pageAt);
            return true;
        }
        if (text.trim().length() > PREFACE_CHARS) throw incomplete();
        return false;
    }

    private String source() {
        String source = buffer.toString().trim();
        if (source.startsWith("\uFEFF")) source = source.substring(1).trim();
        Matcher fence = FENCE.matcher(source);
        if (fence.find()) source = source.substring(fence.end()).trim();
        if (source.endsWith("```")) source = source.substring(0, source.length() - 3).trim();
        if (FULL.equals(kind)) {
            // Drop any closing remarks after the document instead of failing the whole generation.
            Matcher close = CLOSE.matcher(source);
            int end = -1;
            while (close.find()) end = close.end();
            if (end > 0) source = source.substring(0, end);
        }
        return source;
    }

    private void send(String html, boolean complete) {
        ObjectNode scene = document(html, complete);
        scene.put("edit", kind);
        if (PATCH.equals(kind)) scene.put("edits", appliedBlocks);
        scene.put("chars", buffer.length());
        preview.accept(scene);
    }

    private ObjectNode document(String html, boolean complete) {
        Matcher titleElement = TITLE.matcher(html);
        String title = titleElement.find() ? HtmlUtils.htmlUnescape(titleElement.group(1)).replaceAll("\\s+", " ").trim() : "";
        if (complete && title.isEmpty()) throw incomplete();
        if (title.isEmpty()) title = fallbackTitle;
        return VideoSquareSceneFormat.JSON.createObjectNode().put("format", VideoSquareSceneFormat.FORMAT)
            .put("title", title.substring(0, Math.min(title.length(), 120))).put("html", html);
    }

    private static ServiceException incomplete() {
        return new ServiceException("模型未返回含标题和正文的完整 HTML 页面，请重新生成", 422);
    }
}
