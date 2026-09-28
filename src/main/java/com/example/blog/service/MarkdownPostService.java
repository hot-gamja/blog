package com.example.blog.service;

import com.example.blog.domain.PostMeta;
import com.example.blog.domain.RenderedPost;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.emoji.EmojiImageType;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.AttributeProvider;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.IndependentAttributeProviderFactory;
import com.vladsch.flexmark.html.renderer.AttributablePart;
import com.vladsch.flexmark.html.renderer.LinkResolverContext;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.html.MutableAttributes;
import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MarkdownPostService {

    private static final Logger log = LoggerFactory.getLogger(MarkdownPostService.class);

    private final ResourcePatternResolver resourceResolver;
    private final HtmlSanitizerService sanitizerService;
    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;
    private final Yaml yaml = new Yaml();

    private List<PostMeta> allMetas = new ArrayList<>();
    private Map<String, String> markdownBodies = new HashMap<>();
    private Map<String, PostMeta> metaBySlug = new HashMap<>();

    public MarkdownPostService(ResourcePatternResolver resourceResolver,
                               HtmlSanitizerService sanitizerService) {
        this.resourceResolver = resourceResolver;
        this.sanitizerService = sanitizerService;

        MutableDataSet options = new MutableDataSet();
        options.set(EmojiExtension.USE_IMAGE_TYPE, EmojiImageType.UNICODE_ONLY);
        options.set(Parser.EXTENSIONS, List.of(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                TaskListExtension.create(),
                AutolinkExtension.create(),
                FootnoteExtension.create(),
                EmojiExtension.create(),
                AnchorLinkExtension.create()
        ));
        this.markdownParser = Parser.builder(options).build();
        this.htmlRenderer = HtmlRenderer.builder(options)
                .attributeProviderFactory(new CodeLanguageAttributeProvider.Factory())
                .build();
    }

    @PostConstruct
    public void init() {
        try {
            loadPosts();
        } catch (IOException e) {
            log.error("Failed to load markdown posts", e);
        }
    }

    private void loadPosts() throws IOException {
        allMetas.clear();
        markdownBodies.clear();
        metaBySlug.clear();

        Resource[] resources = resourceResolver.getResources("classpath*:content/posts/*.md");
        for (Resource resource : resources) {
            try {
                String filename = resource.getFilename();
                if (filename == null || !filename.endsWith(".md")) continue;

                String slug = filename.substring(0, filename.length() - 3);

                String content;
                try (InputStream is = resource.getInputStream()) {
                    content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }

                String[] parts = splitFrontMatter(content);
                String yamlStr = parts[0];
                String body = parts[1];

                Map<String, Object> frontMatter = yamlStr.isEmpty()
                        ? new HashMap<>()
                        : yaml.load(yamlStr);
                if (frontMatter == null) frontMatter = new HashMap<>();

                PostMeta meta = buildPostMeta(slug, frontMatter, body);

                allMetas.add(meta);
                metaBySlug.put(slug, meta);
                markdownBodies.put(slug, body);

                log.info("Loaded post: {}", slug);
            } catch (Exception e) {
                log.warn("Failed to parse post: {} - {}", resource.getFilename(), e.getMessage());
            }
        }

        allMetas.sort((a, b) -> {
            if (a.getDate() == null && b.getDate() == null) return 0;
            if (a.getDate() == null) return 1;
            if (b.getDate() == null) return -1;
            return b.getDate().compareTo(a.getDate());
        });

        log.info("Loaded {} markdown posts", allMetas.size());
    }

    private String[] splitFrontMatter(String content) {
        if (content.startsWith("---")) {
            int end = content.indexOf("---", 3);
            if (end != -1) {
                String yamlPart = content.substring(3, end).trim();
                String bodyPart = content.substring(end + 3).trim();
                return new String[]{yamlPart, bodyPart};
            }
        }
        return new String[]{"", content};
    }

    private PostMeta buildPostMeta(String slug, Map<String, Object> fm, String markdownBody) {
        String title = getStr(fm, "title", slug);

        LocalDate date = parseDate(fm.get("date"));

        List<String> tags = getList(fm, "tags");
        String category = getStr(fm, "category", null);
        String summary = getStr(fm, "summary", null);
        String description = getStr(fm, "description", null);
        String thumbnailUrl = getStr(fm, "thumbnailUrl", null);

        if (description == null) {
            description = summary;
        }
        if (description == null) {
            description = extractFirstParagraph(markdownBody, 200);
        }
        if (summary == null) {
            summary = extractFirstParagraph(markdownBody, 300);
        }

        PostMeta meta = new PostMeta();
        meta.setSlug(slug);
        meta.setTitle(title);
        meta.setDate(date);
        meta.setTags(tags);
        meta.setCategory(category);
        meta.setSummary(summary);
        meta.setDescription(description);
        meta.setThumbnailUrl(thumbnailUrl);
        return meta;
    }

    private LocalDate parseDate(Object dateObj) {
        if (dateObj instanceof java.util.Date d) {
            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } else if (dateObj instanceof String ds) {
            try {
                return LocalDate.parse(ds);
            } catch (DateTimeParseException e) {
                log.warn("Invalid date format: {}", ds);
                return null;
            }
        } else if (dateObj instanceof LocalDate ld) {
            return ld;
        }
        return null;
    }

    private String getStr(Map<String, Object> map, String key, String defaultVal) {
        Object v = map.get(key);
        return v != null ? v.toString() : defaultVal;
    }

    private List<String> getList(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof List<?> list) {
            return list.stream().map(Object::toString).collect(Collectors.toList());
        }
        return List.of();
    }

    private String extractFirstParagraph(String markdown, int maxLength) {
        String[] lines = markdown.split("\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("```")
                    || trimmed.startsWith("---") || trimmed.startsWith("|")) {
                if (!sb.isEmpty()) break;
                continue;
            }
            sb.append(trimmed).append(" ");
            if (sb.length() >= maxLength) break;
        }
        String result = sb.toString().trim();
        result = result.replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                .replaceAll("\\*(.+?)\\*", "$1")
                .replaceAll("`(.+?)`", "$1")
                .replaceAll("\\[(.+?)]\\(.+?\\)", "$1");
        if (result.length() > maxLength) {
            result = result.substring(0, maxLength) + "...";
        }
        return result.isEmpty() ? null : result;
    }

    // ===== Public API =====

    public List<PostMeta> listPosts() {
        return Collections.unmodifiableList(allMetas);
    }

    public Optional<RenderedPost> renderPost(String slug) {
        PostMeta meta = metaBySlug.get(slug);
        if (meta == null) return Optional.empty();

        String markdown = markdownBodies.get(slug);
        if (markdown == null) return Optional.empty();

        String html = htmlRenderer.render(markdownParser.parse(markdown));
        String safeHtml = sanitizerService.sanitize(html);

        return Optional.of(new RenderedPost(meta, safeHtml));
    }

    public List<PostMeta> listByCategory(String category) {
        return allMetas.stream()
                .filter(m -> category.equalsIgnoreCase(m.getCategory()))
                .collect(Collectors.toList());
    }

    public List<PostMeta> listByTag(String tag) {
        return allMetas.stream()
                .filter(m -> m.getTags() != null &&
                        m.getTags().stream().anyMatch(t -> t.equalsIgnoreCase(tag)))
                .collect(Collectors.toList());
    }

    public List<PostMeta> getRelatedPosts(String category, String excludeSlug, int limit) {
        return allMetas.stream()
                .filter(m -> category.equalsIgnoreCase(m.getCategory()))
                .filter(m -> !m.getSlug().equals(excludeSlug))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * AttributeProvider that adds language-xxx class to code blocks for syntax highlighting
     */
    static class CodeLanguageAttributeProvider implements AttributeProvider {
        @Override
        public void setAttributes(@NotNull Node node, @NotNull AttributablePart part, @NotNull MutableAttributes attributes) {
            if (node instanceof FencedCodeBlock) {
                FencedCodeBlock codeBlock = (FencedCodeBlock) node;
                String info = codeBlock.getInfo().toString().trim();
                // Add language-xxx class to the <code> element for highlight.js
                if (!info.isEmpty()) {
                    attributes.addValue("class", "language-" + info);
                }
            }
        }

        static class Factory extends IndependentAttributeProviderFactory {
            @Override
            public @NotNull AttributeProvider apply(@NotNull LinkResolverContext context) {
                return new CodeLanguageAttributeProvider();
            }
        }
    }
}
