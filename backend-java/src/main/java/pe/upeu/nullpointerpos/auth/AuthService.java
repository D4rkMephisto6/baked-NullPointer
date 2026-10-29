package pe.upeu.nullpointerpos.auth;

import pe.upeu.nullpointerpos.security.JwtService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Service
public class AuthService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthService(JdbcTemplate jdbc, PasswordEncoder encoder, JwtService jwtService) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        String employeeCode = request.employeeCode().trim().toUpperCase();
        var user = jdbc.query(
                "SELECT u.user_id, u.employee_code, u.full_name, u.pin_hash, u.is_active, r.name AS role_name " +
                "FROM auth_schema.users u JOIN auth_schema.roles r ON r.role_id = u.role_id " +
                "WHERE u.employee_code = ?",
                rs -> rs.next() ? new LoginUser(rs.getLong("user_id"), rs.getString("employee_code"),
                        rs.getString("full_name"), rs.getString("pin_hash"), rs.getBoolean("is_active"),
                        rs.getString("role_name")) : null,
                employeeCode);
        boolean valid = user != null && user.active() && encoder.matches(request.pin(), user.pinHash());
        if (!valid) {
            jdbc.update("INSERT INTO auth_schema.auth_events(employee_code_attempted, event_type) VALUES (?, 'LOGIN_FAILED')",
                    employeeCode);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código de trabajador o PIN incorrecto.");
        }
        jdbc.update("INSERT INTO auth_schema.auth_events(user_id, employee_code_attempted, event_type) VALUES (?, ?, 'LOGIN_SUCCESS')",
                user.userId(), user.employeeCode());
        String token = jwtService.issue(user.userId(), user.employeeCode(), user.fullName(), user.roleName());
        return new LoginResponse(token, "Bearer", jwtService.expirationSeconds(),
                new AuthenticatedUser(user.userId(), user.employeeCode(), user.fullName(), user.roleName()));
    }

    private record LoginUser(Long userId, String employeeCode, String fullName, String pinHash,
                             boolean active, String roleName) { }
    public record AuthenticatedUser(Long userId, String employeeCode, String fullName, String role) { }
    public record LoginResponse(String token, String tokenType, long expiresInSeconds, AuthenticatedUser user) { }
}
