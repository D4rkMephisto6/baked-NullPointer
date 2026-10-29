package pe.upeu.nullpointerpos.tables;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mesas")
public class TableController {
    private final TableService tableService;
    public TableController(TableService tableService) { this.tableService = tableService; }
    @GetMapping
    public List<TableResponse> listTables() { return tableService.listTables(); }
}
