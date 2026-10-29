package pe.upeu.nullpointerpos.users;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    public UserService(JdbcTemplate jdbc, PasswordEncoder encoder) { this.jdbc = jdbc; this.encoder = encoder; }

    public List<UserResponse> listUsers() {
        return jdbc.query("SELECT u.user_id, u.employee_code, u.full_name, u.email, r.name AS role_name, " +
                        "u.is_active, u.created_at FROM auth_schema.users u JOIN auth_schema.roles r ON r.role_id = u.role_id " +
                        "ORDER BY u.user_id",
                (rs, row) -> new UserResponse(rs.getLong("user_id"), rs.getString("employee_code"),
                        rs.getString("full_name"), rs.getString("email"), rs.getString("role_name"),
                        rs.getBoolean("is_active"), rs.getObject("created_at", java.time.OffsetDateTime.class)));
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request, String actorEmployeeCode) {
        String role = request.role().trim().toUpperCase();
        Integer roleCount = jdbc.queryForObject("SELECT COUNT(*) FROM auth_schema.roles WHERE name = ? AND is_active = TRUE", Integer.class, role);
        if (roleCount == null || roleCount == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol indicado no existe o está inactivo.");
        Long actorId = findUserId(actorEmployeeCode);
        String pinHash = encoder.encode(request.pin());
        jdbc.update("INSERT INTO auth_schema.users(role_id, employee_code, full_name, email, pin_hash) " +
                        "SELECT role_id, ?, ?, NULLIF(?, ''), ? FROM auth_schema.roles WHERE name = ?",
                request.employeeCode().trim().toUpperCase(), request.fullName().trim(),
                request.email() == null ? "" : request.email().trim(), pinHash, role);
        Long id = jdbc.queryForObject("SELECT user_id FROM auth_schema.users WHERE employee_code = ?", Long.class,
                request.employeeCode().trim().toUpperCase());
        jdbc.update("INSERT INTO pos_schema.audit_logs(actor_user_id, action, entity_name, entity_id, new_values, reason) " +
                        "VALUES (?, 'USER_CREATED', 'USER', ?, jsonb_build_object('employeeCode', ?, 'role', ?), 'Alta de usuario desde Sprint 1')",
                actorId, id.toString(), request.employeeCode().trim().toUpperCase(), role);
        return getUser(id);
    }

    @Transactional
    public UserResponse setActive(Long userId, boolean active, String actorEmployeeCode) {
        findUserId(actorEmployeeCode);
        int changed = jdbc.update("UPDATE auth_schema.users SET is_active = ?, updated_at = NOW() WHERE user_id = ?", active, userId);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado.");
        Long actorId = findUserId(actorEmployeeCode);
        jdbc.update("INSERT INTO pos_schema.audit_logs(actor_user_id, action, entity_name, entity_id, new_values, reason) " +
                        "VALUES (?, ?, 'USER', ?, jsonb_build_object('isActive', ?), 'Cambio de estado desde Sprint 1')",
                actorId, active ? "USER_ACTIVATED" : "USER_DEACTIVATED", userId.toString(), active);
        return getUser(userId);
    }

    private Long findUserId(String employeeCode) {
        try {
            return jdbc.queryForObject("SELECT user_id FROM auth_schema.users WHERE employee_code = ?", Long.class, employeeCode);
        } catch (EmptyResultDataAccessException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado.");
        }
    }

    private UserResponse getUser(Long id) {
        try {
            return jdbc.queryForObject("SELECT u.user_id, u.employee_code, u.full_name, u.email, r.name AS role_name, " +
                            "u.is_active, u.created_at FROM auth_schema.users u JOIN auth_schema.roles r ON r.role_id = u.role_id " +
                            "WHERE u.user_id = ?",
                    (rs, row) -> new UserResponse(rs.getLong("user_id"), rs.getString("employee_code"),
                            rs.getString("full_name"), rs.getString("email"), rs.getString("role_name"),
                            rs.getBoolean("is_active"), rs.getObject("created_at", java.time.OffsetDateTime.class)), id);
        } catch (EmptyResultDataAccessException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado.");
        }
    }
}
