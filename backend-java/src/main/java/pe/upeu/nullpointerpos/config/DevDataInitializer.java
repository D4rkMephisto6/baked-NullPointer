package pe.upeu.nullpointerpos.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DevDataInitializer {
    @Bean
    CommandLineRunner seedSprintOneDemoData(JdbcTemplate jdbc, PasswordEncoder encoder) {
        return args -> {
            ensureUser(jdbc, encoder, "ADM001", "Administrador de prueba", "ADMINISTRADOR");
            ensureUser(jdbc, encoder, "MOZ001", "Mozo de prueba", "MOZO");
            ensureUser(jdbc, encoder, "MOZ002", "Mozo de prueba 2", "MOZO");
            ensureUser(jdbc, encoder, "COC001", "Cocinero de prueba", "COCINERO");
            ensureUser(jdbc, encoder, "CAJ001", "Cajero de prueba", "CAJERO");

            ensureProduct(jdbc, "POLLO-025", "Pollo a la brasa 1/4", "Pollos", "COCINA", "22.90");
            ensureProduct(jdbc, "POLLO-050", "Pollo a la brasa 1/2", "Pollos", "COCINA", "39.90");
            ensureProduct(jdbc, "BEB-COLA", "Gaseosa personal", "Bebidas", "BEBIDAS", "4.00");
            ensureProduct(jdbc, "GUAR-PAPA", "Papas fritas", "Guarniciones", "COCINA", "8.00");
            ensureProduct(jdbc, "COMBO-001", "Combo personal", "Combos", "COCINA", "26.90");
        };
    }

    private void ensureUser(JdbcTemplate jdbc, PasswordEncoder encoder, String code, String name, String role) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM auth_schema.users WHERE employee_code = ?", Integer.class, code);
        if (count != null && count > 0) return;
        String encodedPin = encoder.encode("1234");
        int inserted = jdbc.update("INSERT INTO auth_schema.users(role_id, employee_code, full_name, pin_hash) " +
                        "SELECT role_id, ?, ?, ? FROM auth_schema.roles WHERE name = ? AND is_active = TRUE " +
                        "AND NOT EXISTS (SELECT 1 FROM auth_schema.users WHERE employee_code = ?)",
                code, name, encodedPin, role, code);
        if (inserted == 0) throw new IllegalStateException("No se pudo crear usuario de demostración para rol " + role);
    }

    private void ensureProduct(JdbcTemplate jdbc, String sku, String name, String category, String station, String price) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM pos_schema.products WHERE sku = ?", Integer.class, sku);
        if (count != null && count > 0) return;
        int inserted = jdbc.update("INSERT INTO pos_schema.products(category_id, kitchen_station_id, sku, name, description, unit_price) " +
                        "SELECT c.category_id, s.kitchen_station_id, ?, ?, ?, ?::numeric " +
                        "FROM pos_schema.product_categories c CROSS JOIN pos_schema.kitchen_stations s " +
                        "WHERE c.name = ? AND s.name = ? AND NOT EXISTS (SELECT 1 FROM pos_schema.products WHERE sku = ?)",
                sku, name, "Producto de demostración para pruebas del Sprint 1", price, category, station, sku);
        if (inserted == 0) throw new IllegalStateException("No se pudo crear el producto de demostración " + sku);
    }
}
