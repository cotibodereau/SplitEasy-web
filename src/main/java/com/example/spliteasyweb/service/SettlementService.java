package com.example.spliteasyweb.service;

import com.example.spliteasyweb.model.ExpenseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SettlementService {

  public Map<String, Double> balances(List<ExpenseEntity> expenses){
    Map<String, Double> bal = new HashMap<>();

    for (var e: expenses){
      List<String> parts  = parse(e.getParticipantsCsv());
      List<String> payers = parse(e.getPayersCsv()); // puede venir null -> parse lo maneja
      int nParts  = parts.size();

      if (nParts == 0) continue;

      double amount = (e.getAmount() == null ? 0.0 : e.getAmount().doubleValue());
      if (amount == 0.0) continue;

      // Fallback: si no hay pagadores explícitos, usar el primer participante
      if (payers.isEmpty() && !parts.isEmpty()) {
        payers = List.of(parts.get(0));
      }

      int nPayers = payers.size(); // ya nunca es 0 por el fallback

      double sharePerPart  = amount / nParts;
      double sharePerPayer = amount / nPayers;

      // crédito a cada pagador
      for (String payer: payers) {
        bal.merge(payer, sharePerPayer, Double::sum);
      }

      // débito a cada participante
      for (String p: parts) {
        bal.merge(p, -sharePerPart, Double::sum);
      }
    }

    // redondeo a 2 decimales
    bal.replaceAll((k,v)-> Math.round(v*100.0)/100.0);
    return new TreeMap<>(bal);
  }

  public record Transfer(String from, String to, double amount){}
  private record Entry(String person, double amount){}

  public List<Transfer> settle(Map<String, Double> bal){
    PriorityQueue<Entry> debt = new PriorityQueue<>(Comparator.comparingDouble(e->e.amount));
    PriorityQueue<Entry> cred = new PriorityQueue<>((a,b)->Double.compare(b.amount,a.amount));

    for (var e: bal.entrySet()){
      double v=e.getValue();
      if (v<-0.01) debt.add(new Entry(e.getKey(), -v));
      else if (v>0.01) cred.add(new Entry(e.getKey(), v));
    }

    List<Transfer> out=new ArrayList<>();
    while(!debt.isEmpty() && !cred.isEmpty()){
      var d=debt.poll(); var c=cred.poll();
      double m=Math.min(d.amount,c.amount); m=round(m);
      out.add(new Transfer(d.person(), c.person(), m));
      if (d.amount>m) debt.add(new Entry(d.person(), round(d.amount-m)));
      if (c.amount>m) cred.add(new Entry(c.person(), round(c.amount-m)));
    }
    return out;
  }

  public static List<String> parse(String csv){
    if (csv==null || csv.isBlank()) return List.of();
    var set = new LinkedHashSet<String>();
    for (var s: csv.split(",")){
      var t=s.trim();
      if(!t.isEmpty()) set.add(t);
    }
    return new ArrayList<>(set);
  }

  public static String join(List<String> list){
    return String.join(",", list);
  }

  private double round(double x){ return Math.round(x*100.0)/100.0; }
}
