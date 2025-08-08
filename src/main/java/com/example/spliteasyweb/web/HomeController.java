package com.example.spliteasyweb.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Optional;

@Controller
public class HomeController {

  @GetMapping("/")
  public String home(Model model, HttpSession session) {
    @SuppressWarnings("unchecked")
    var recent = (List<String>) Optional.ofNullable(session.getAttribute("recentGroups")).orElse(List.of());
    model.addAttribute("recentGroups", recent);
    return "home";
  }
}
