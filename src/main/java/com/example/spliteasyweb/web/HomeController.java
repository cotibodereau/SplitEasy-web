package com.example.spliteasyweb.web;

import com.example.spliteasyweb.model.GroupEntity;
import com.example.spliteasyweb.repo.ExpenseRepo;
import com.example.spliteasyweb.repo.GroupRepo;
import com.example.spliteasyweb.repo.PersonRepo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

@Controller
public class HomeController {
  private final GroupRepo groups;
  private final PersonRepo persons;
  private final ExpenseRepo expenses;

  public HomeController(GroupRepo groups, PersonRepo persons, ExpenseRepo expenses) {
    this.groups = groups;
    this.persons = persons;
    this.expenses = expenses;
  }

  @GetMapping("/")
  public String home(Model model){
    model.addAttribute("groups", groups.findAll());
    model.addAttribute("newGroup", new GroupEntity());
    return "home";
  }

  @PostMapping("/groups")
  public String create(@ModelAttribute GroupEntity g){
    String slug = g.getName().toLowerCase()
      .replaceAll("[^a-z0-9 ]","").trim().replace(" ","-");
    if (slug.isBlank() || groups.existsBySlug(slug)) slug += "-" + (System.currentTimeMillis()%10000);
    g.setSlug(slug);
    groups.save(g);
    return "redirect:/g/" + slug;
  }

  @PostMapping("/groups/{slug}/delete")
  @Transactional
  public String delete(@PathVariable String slug){
    var g = groups.findBySlug(slug).orElse(null);
    if (g != null) {
      expenses.deleteByGroupId(g.getId());
      persons.deleteByGroupId(g.getId());
      groups.delete(g);
    }
    return "redirect:/";
  }
}
