package pe.upeu.nullpointerpos.tables;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TableService {
    private final JdbcTemplate jdbc;
    public TableService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<TableResponse> listTables() {
        return jdbc.query("SELECT table_id, table_code, area_name, capacity, status, is_active " +
                "FROM pos_schema.dining_tables WHERE is_active = TRUE ORDER BY area_name, table_code",
                (rs, row) -> new TableResponse(rs.getLong("table_id"), rs.getString("table_code"),
                        rs.getString("area_name"), rs.getInt("capacity"), rs.getString("status"),
                        rs.getBoolean("is_active")));
    }
}
