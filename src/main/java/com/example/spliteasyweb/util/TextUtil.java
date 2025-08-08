package com.example.spliteasyweb.util;

import java.text.Normalizer;
import java.util.Locale;

public final class TextUtil {
  private TextUtil(){}

  /** Normaliza para comparar: quita tildes/diacríticos y pasa a minúsculas. */
  public static String norm(String s){
    if (s == null) return "";
    String n = Normalizer.normalize(s, Normalizer.Form.NFD);
    n = n.replaceAll("\\p{M}+", ""); // quita marcas (acentos)
    return n.toLowerCase(Locale.ROOT).trim();
  }
}
