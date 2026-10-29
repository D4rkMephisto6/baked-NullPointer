package pe.upeu.nullpointerpos.menu;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MenuService {
    private final JdbcTemplate jdbc;
    public MenuService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<CategoryResponse> listCategories() {
        return jdbc.query("SELECT category_id, name, description, display_order FROM pos_schema.product_categories " +
                        "WHERE is_active = TRUE ORDER BY display_order, name",
                (rs, row) -> new CategoryResponse(rs.getLong("category_id"), rs.getString("name"),
                        rs.getString("description"), rs.getInt("display_order")));
    }

    public List<ProductResponse> listProducts(String category, String search, boolean onlyAvailable) {
        StringBuilder sql = new StringBuilder("SELECT p.product_id, p.sku, p.name, p.description, p.image_url, " +
                "p.unit_price, c.name AS category_name, ks.name AS kitchen_station, p.is_available " +
                "FROM pos_schema.products p JOIN pos_schema.product_categories c ON c.category_id = p.category_id " +
                "LEFT JOIN pos_schema.kitchen_stations ks ON ks.kitchen_station_id = p.kitchen_station_id " +
                "WHERE p.is_active = TRUE AND c.is_active = TRUE ");
        java.util.ArrayList<Object> args = new java.util.ArrayList<>();
        if (onlyAvailable) sql.append("AND p.is_available = TRUE ");
        if (category != null && !category.isBlank()) {
            sql.append("AND LOWER(c.name) = LOWER(?) "); args.add(category.trim());
        }
        if (search != null && !search.isBlank()) {
            sql.append("AND (LOWER(p.name) LIKE LOWER(?) OR LOWER(COALESCE(p.sku, '')) LIKE LOWER(?)) ");
            String term = "%" + search.trim() + "%"; args.add(term); args.add(term);
        }
        sql.append("ORDER BY c.display_order, p.name");
        return jdbc.query(sql.toString(), (rs, row) -> new ProductResponse(
                rs.getLong("product_id"), rs.getString("sku"), rs.getString("name"),
                rs.getString("description"), rs.getString("image_url"), rs.getBigDecimal("unit_price"),
                rs.getString("category_name"), rs.getString("kitchen_station"), rs.getBoolean("is_available")), args.toArray());
    }
}
