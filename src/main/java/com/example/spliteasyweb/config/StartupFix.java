package com.example.spliteasyweb.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.ResultSet;

@Component
public class StartupFix implements CommandLineRunner {
  private final DataSource ds;
  public StartupFix(DataSource ds){ this.ds = ds; }

  @Override
  public void run(String... args) throws Exception {
    try (var conn = ds.getConnection()) {
      // 1) Asegurar que la columna nueva exista (si falló antes su creación)
      try (var st = conn.createStatement()) {
        st.execute("ALTER TABLE IF EXISTS EXPENSES ADD COLUMN IF NOT EXISTS PAYERS_CSV VARCHAR(255)");
      }

      // 2) Si existe la columna vieja PAYER, copiar sus valores -> PAYERS_CSV
      boolean hasLegacyPayer;
      try (ResultSet rs = conn.getMetaData().getColumns(null, null, "EXPENSES", "PAYER")) {
        hasLegacyPayer = rs.next();
      }
      if (hasLegacyPayer) {
        try (var ps = conn.prepareStatement(
            "UPDATE EXPENSES SET PAYERS_CSV = PAYER WHERE PAYERS_CSV IS NULL AND PAYER IS NOT NULL")) {
          ps.executeUpdate();
        }
      }

      // 3) Para cualquier fila que quede sin valor, dejar string vacío (evita nulls)
      try (var ps = conn.prepareStatement(
          "UPDATE EXPENSES SET PAYERS_CSV = '' WHERE PAYERS_CSV IS NULL")) {
        ps.executeUpdate();
      }
    }
  }
}
