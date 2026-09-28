package com.example.blog.service;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Service;

@Service
public class HtmlSanitizerService {

    private final PolicyFactory policy;

    public HtmlSanitizerService() {
        this.policy = new HtmlPolicyBuilder()
                .allowElements(
                        "h1", "h2", "h3", "h4", "h5", "h6",
                        "p", "br", "hr",
                        "ul", "ol", "li",
                        "a", "img",
                        "code", "pre", "blockquote",
                        "strong", "em", "b", "i", "del", "s", "ins",
                        "table", "thead", "tbody", "tfoot", "tr", "th", "td",
                        "div", "span",
                        "dl", "dt", "dd",
                        "sup", "sub",
                        "input",
                        "details", "summary"
                )
                .allowUrlProtocols("https", "http", "mailto")
                .allowAttributes("href").onElements("a")
                .allowAttributes("target", "rel").onElements("a")
                .allowAttributes("src", "alt", "width", "height", "loading").onElements("img")
                .allowAttributes("class", "id").globally()
                .allowAttributes("type", "checked", "disabled").onElements("input")
                .allowAttributes("open").onElements("details")
                .allowAttributes("align").onElements("td", "th")
                .allowAttributes("colspan", "rowspan").onElements("td", "th")
                .toFactory();
    }

    public String sanitize(String html) {
        return policy.sanitize(html);
    }
}
