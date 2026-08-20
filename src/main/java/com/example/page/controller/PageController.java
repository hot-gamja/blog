package com.example.page.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/projects")
    public String projects(Model model) {
        model.addAttribute("pageTitle", "Projects - Hot Gamja Lab");
        return "projects/index";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("pageTitle", "About - Hot Gamja Lab");
        return "about";
    }

    @GetMapping("/sokcho")
    public String sokcho(Model model) {
        model.addAttribute("pageTitle", "속초 맛집 - Hot Gamja Lab");
        return "sokcho/index";
    }
}
