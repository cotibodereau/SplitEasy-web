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

    private final GroupRepo groupRepo;
    private final PersonRepo personRepo;
    private final ExpenseRepo expenseRepo;
    private final SettlementService settlementService;

    public GroupController(GroupRepo groupRepo,
                           PersonRepo personRepo,
                           ExpenseRepo expenseRepo,
                           SettlementService settlementService) {
        this.groupRepo = groupRepo;
        this.personRepo = personRepo;
        this.expenseRepo = expenseRepo;
        this.settlementService = settlementService;
    }

    // ============ HOME: muestra mis grupos por sesión ============
    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        var myGroups = groupRepo.findByOwnerSessionOrderByIdDesc(session.getId());
        model.addAttribute("groups", myGroups);
        return "home";
    }

    // Crear grupo (queda asociado a mi sesión)
    @PostMapping("/groups")
    @Transactional
    public String createGroup(@RequestParam String name, HttpSession session) {
        GroupEntity g = new GroupEntity();
        g.setName(name == null ? "Grupo" : name.trim());
        g.setSlug(UUID.randomUUID().toString().replace("-", "").substring(0, 14));
        g.setOwnerSession(session.getId());
        groupRepo.save(g);
        return "redirect:/g/" + g.getSlug();
    }

    // ============ Dashboard del grupo compartido ============
    @GetMapping("/g/{slug}")
    public String dashboard(@PathVariable String slug, Model model) {
        GroupEntity g = groupRepo.findBySlug(slug).orElseThrow();
        var people = personRepo.findByGroupIdOrderByNameAsc(g.getId());
        var exps = expenseRepo.findByGroupIdOrderByDateDescIdDesc(g.getId());

        var balances = settlementService.balances(exps);
        var txs = settlementService.settle(balances);

        model.addAttribute("g", g);
        model.addAttribute("people", people);
        model.addAttribute("expenses", exps);
        model.addAttribute("balances", balances);
        model.addAttribute("txs", txs);
        return "group";
    }

    // ============ Personas ============
    @PostMapping("/g/{slug}/people")
    @Transactional
    public String addPerson(@PathVariable String slug,
                            @RequestParam String name) {
        GroupEntity g = groupRepo.findBySlug(slug).orElseThrow();
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) return "redirect:/g/" + slug;

        var existing = personRepo.findByGroupIdOrderByNameAsc(g.getId());
        boolean exists = existing.stream().anyMatch(p ->
            norm(p.getName()).equals(norm(trimmed))
        );
        if (!exists) {
            PersonEntity p = new PersonEntity();
            p.setGroupId(g.getId());
            p.setName(trimmed);
            personRepo.save(p);
        }
        return "redirect:/g/" + slug;
    }

    @PostMapping("/g/{slug}/people/{id}/delete")
    @Transactional
    public String deletePerson(@PathVariable String slug, @PathVariable Long id) {
        personRepo.deleteById(id);
        return "redirect:/g/" + slug;
    }

    // ============ Gastos ============
    @PostMapping("/g/{slug}/expenses")
    @Transactional
    public String addExpense(@PathVariable String slug,
                             @RequestParam String title,
                             @RequestParam String amount,
                             @RequestParam(required = false) String participantsCsv,
                             @RequestParam(required = false) String payersCsv) {
        GroupEntity g = groupRepo.findBySlug(slug).orElseThrow();

        var people = personRepo.findByGroupIdOrderByNameAsc(g.getId());
        Map<String, String> canon = canonicalMap(people);

        ExpenseEntity e = new ExpenseEntity();
        e.setGroup(g);
        e.setTitle(title == null ? "" : title.trim());
        e.setAmount(safeBigDecimal(amount));
        e.setDate(LocalDate.now());
        e.setParticipantsCsv(joinUniqueCanonical(participantsCsv, canon));
        e.setPayersCsv(joinUniqueCanonical(payersCsv, canon));
        expenseRepo.save(e);

        return "redirect:/g/" + slug;
    }

    @PostMapping("/g/{slug}/expenses/{id}/delete")
    @Transactional
    public String deleteExpense(@PathVariable String slug, @PathVariable Long id) {
        expenseRepo.deleteById(id);
        return "redirect:/g/" + slug;
    }

    // ============ Helpers ============
    private static String norm(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return n.toLowerCase(Locale.ROOT).trim();
    }

    private static Map<String, String> canonicalMap(List<PersonEntity> people) {
        Map<String, String> map = new HashMap<>();
        for (var p : people) {
            map.put(norm(p.getName()), p.getName());
        }
        return map;
    }

    private static String joinUniqueCanonical(String csv, Map<String, String> canon) {
        if (csv == null) return "";
        var set = new LinkedHashSet<String>();
        for (String token : csv.split(",")) {
            String raw = token.trim();
            if (raw.isEmpty()) continue;
            String key = norm(raw);
            String canonical = canon.getOrDefault(key, raw);
            if (set.stream().noneMatch(x -> norm(x).equals(norm(canonical)))) {
                set.add(canonical);
            }
        }
        return String.join(",", set);
    }

    private static BigDecimal safeBigDecimal(String s) {
        try {
            if (s == null) return new BigDecimal("0");
            return new BigDecimal(s.trim());
        } catch (Exception ex) {
            return new BigDecimal("0");
        }
    }
}
