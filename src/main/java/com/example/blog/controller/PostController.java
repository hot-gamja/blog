package com.example.blog.controller;

import com.example.blog.domain.PostMeta;
import com.example.blog.domain.RenderedPost;
import com.example.blog.service.MarkdownPostService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/blog/posts")
public class PostController {

    private final MarkdownPostService markdownPostService;

    public PostController(MarkdownPostService markdownPostService) {
        this.markdownPostService = markdownPostService;
    }

    @GetMapping("/{slug}")
    public String postDetail(@PathVariable String slug, Model model) {
        Optional<RenderedPost> rendered = markdownPostService.renderPost(slug);

        if (rendered.isEmpty()) {
            return "error/404";
        }

        RenderedPost post = rendered.get();
        PostMeta meta = post.getMeta();

        List<PostMeta> relatedPosts = List.of();
        if (meta.getCategory() != null) {
            relatedPosts = markdownPostService.getRelatedPosts(meta.getCategory(), slug, 3);
        }

        String html = post.getHtml();

        model.addAttribute("post", meta);
        model.addAttribute("postHtml", html);
        model.addAttribute("hasMermaid", html != null && html.contains("language-mermaid"));
        model.addAttribute("relatedPosts", relatedPosts);
        model.addAttribute("pageTitle", meta.getTitle() + " - HOT GAMJA LAB");

        return "blog/post-detail";
    }
}
