package com.example.spliteasyweb.web;

import com.example.spliteasyweb.model.ExpenseEntity;
import com.example.spliteasyweb.model.GroupEntity;
import com.example.spliteasyweb.model.PersonEntity;
import com.example.spliteasyweb.repo.ExpenseRepo;
import com.example.spliteasyweb.repo.GroupRepo;
import com.example.spliteasyweb.repo.PersonRepo;
import com.example.spliteasyweb.service.SettlementService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;

@Controller
public class GroupController {

    private final GroupRepo groups;
    private final PersonRepo people;
    private final ExpenseRepo expenses;
    private final SettlementService settlementService;

    public GroupController(GroupRepo groups, PersonRepo people, ExpenseRepo expenses,
            SettlementService settlementService) {
        this.groups = groups;
        this.people = people;
        this.expenses = expenses;
        this.settlementService = settlementService;
    }

    // ========= HOME YA NO VA ACÁ ==============
    // NADA con @GetMapping("/") en este controller

    // Crear grupo (desde el home)
    @PostMapping("/groups")
    @Transactional
    public String createGroup(@RequestParam String name, HttpSession session) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty())
            return "redirect:/";

        String base = slugify(normalized);
        String slug = base;
        int i = 1;
        while (groups.existsBySlug(slug)) {
            i++;
            slug = base + "-" + i;
        }

        GroupEntity g = new GroupEntity();
        g.setName(normalized);
        g.setSlug(slug);
        groups.save(g);

        // guardar recientes en sesión (opcional)
        @SuppressWarnings("unchecked")
        var recent = (LinkedList<String>) Optional.ofNullable(session.getAttribute("recentGroups"))
                .orElse(new LinkedList<String>());
        recent.remove(slug);
        recent.addFirst(slug);
        while (recent.size() > 5)
            recent.removeLast();
        session.setAttribute("recentGroups", recent);

        return "redirect:/g/" + slug;
    }

    // Dashboard del grupo
    @GetMapping("/g/{slug}")
    public String dashboard(@PathVariable String slug, Model model) {
        GroupEntity g = groups.findBySlug(slug).orElseThrow();
        var ppl = people.findByGroupIdOrderByNameAsc(g.getId());
        var exps = expenses.findByGroupIdOrderByDateDescIdDesc(g.getId());

        var balances = settlementService.balances(exps);
        var txs = settlementService.settle(balances);

        model.addAttribute("g", g);
        model.addAttribute("people", ppl);
        model.addAttribute("expenses", exps);
        model.addAttribute("balances", balances);
        model.addAttribute("txs", txs);
        return "group";
    }

    // Personas
    @PostMapping("/g/{slug}/people")
    @Transactional
    public String addPerson(@PathVariable String slug, @RequestParam String name) {
        GroupEntity g = groups.findBySlug(slug).orElseThrow();
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty())
            return "redirect:/g/" + slug;

        var existing = people.findByGroupIdOrderByNameAsc(g.getId());
        boolean exists = existing.stream().anyMatch(p -> norm(p.getName()).equals(norm(trimmed)));
        if (!exists) {
            PersonEntity p = new PersonEntity();
            p.setGroupId(g.getId());
            p.setName(trimmed);
            people.save(p);
        }
        return "redirect:/g/" + slug;
    }

    @PostMapping("/g/{slug}/people/{id}/delete")
    @Transactional
    public String deletePerson(@PathVariable String slug, @PathVariable Long id) {
        people.deleteById(id);
        return "redirect:/g/" + slug;
    }

    // Gastos
    @PostMapping("/g/{slug}/expenses")
    @Transactional
    public String addExpense(@PathVariable String slug,
            @RequestParam String title,
            @RequestParam String amount,
            @RequestParam(required = false) String participantsCsv,
            @RequestParam(required = false) String payersCsv) {
        GroupEntity g = groups.findBySlug(slug).orElseThrow();

        var ppl = people.findByGroupIdOrderByNameAsc(g.getId());
        Map<String, String> canon = canonicalMap(ppl);

        ExpenseEntity e = new ExpenseEntity();
        e.setGroup(g);
        e.setTitle(title == null ? "" : title.trim());
        e.setAmount(safeBigDecimal(amount));
        e.setDate(LocalDate.now());
        e.setParticipantsCsv(joinUniqueCanonical(participantsCsv, canon));
        e.setPayersCsv(joinUniqueCanonical(payersCsv, canon));

        expenses.save(e);
        return "redirect:/g/" + slug;
    }

    @PostMapping("/g/{slug}/expenses/{id}/delete")
    @Transactional
    public String deleteExpense(@PathVariable String slug, @PathVariable Long id) {
        expenses.deleteById(id);
        return "redirect:/g/" + slug;
    }

    @PostMapping("/g/{slug}/expenses/{id}/liquidate")
    @Transactional
    public String liquidateExpense(@PathVariable String slug, @PathVariable Long id) {
        expenses.deleteById(id);
        return "redirect:/g/" + slug;
    }

    // Helpers
    private static String slugify(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        n = n.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return n.isEmpty() ? "grupo" : n;
    }

    private static String norm(String s) {
        if (s == null)
            return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return n.toLowerCase(Locale.ROOT).trim();
    }

    private static Map<String, String> canonicalMap(List<PersonEntity> people) {
        Map<String, String> map = new HashMap<>();
        for (var p : people)
            map.put(norm(p.getName()), p.getName());
        return map;
    }

    private static String joinUniqueCanonical(String csv, Map<String, String> canon) {
        if (csv == null)
            return "";
        var set = new LinkedHashSet<String>();
        for (String token : csv.split(",")) {
            String raw = token.trim();
            if (raw.isEmpty())
                continue;
            String key = norm(raw);
            String canonical = canon.getOrDefault(key, raw);
            if (set.stream().noneMatch(x -> norm(x).equals(norm(canonical))))
                set.add(canonical);
        }
        return String.join(",", set);
    }

    private static BigDecimal safeBigDecimal(String s) {
        try {
            if (s == null)
                return BigDecimal.ZERO;
            s = s.trim().replace(',', '.');
            if (s.isEmpty()) return BigDecimal.ZERO;
            return new BigDecimal(s);
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }
}
