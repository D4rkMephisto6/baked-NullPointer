package pe.upeu.nullpointerpos.menu;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/menu")
public class MenuController {
    private final MenuService menuService;
    public MenuController(MenuService menuService) { this.menuService = menuService; }

    @GetMapping("/categorias")
    public List<CategoryResponse> categories() { return menuService.listCategories(); }

    @GetMapping("/productos")
    public List<ProductResponse> products(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "true") boolean onlyAvailable) {
        return menuService.listProducts(category, search, onlyAvailable);
    }
}
