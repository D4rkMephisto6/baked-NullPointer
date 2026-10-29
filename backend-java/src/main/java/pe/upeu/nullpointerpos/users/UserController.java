package pe.upeu.nullpointerpos.users;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/usuarios")
public class UserController {
    private final UserService userService;
    public UserController(UserService userService) { this.userService = userService; }

    @GetMapping
    public List<UserResponse> list() { return userService.listUsers(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request, Authentication authentication) {
        return userService.createUser(request, authentication.getName());
    }

    @PatchMapping("/{id}/desactivar")
    public UserResponse deactivate(@PathVariable Long id, Authentication authentication) {
        return userService.setActive(id, false, authentication.getName());
    }

    @PatchMapping("/{id}/activar")
    public UserResponse activate(@PathVariable Long id, Authentication authentication) {
        return userService.setActive(id, true, authentication.getName());
    }
}
