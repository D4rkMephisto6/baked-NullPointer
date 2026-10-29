package pe.upeu.nullpointerpos.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final JdbcTemplate jdbcTemplate;

    public JwtAuthenticationFilter(JwtService jwtService, JdbcTemplate jdbcTemplate) {
        this.jwtService = jwtService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        String token = header.substring(7).trim();
        try {
            Claims claims = jwtService.parse(token);
            String employeeCode = claims.getSubject();
            if (employeeCode == null || employeeCode.isBlank()) {
                chain.doFilter(request, response);
                return;
            }
            // Revalidar la cuenta en cada solicitud permite bloquear inmediatamente a usuarios desactivados.
            var account = jdbcTemplate.query(
                    "SELECT r.name AS role_name FROM auth_schema.users u " +
                    "JOIN auth_schema.roles r ON r.role_id = u.role_id " +
                    "WHERE u.employee_code = ? AND u.is_active = TRUE AND r.is_active = TRUE",
                    rs -> rs.next() ? rs.getString("role_name") : null,
                    employeeCode);
            if (account == null) {
                SecurityContextHolder.clearContext();
                chain.doFilter(request, response);
                return;
            }
            var authentication = new UsernamePasswordAuthenticationToken(
                    employeeCode, null, List.of(new SimpleGrantedAuthority("ROLE_" + account)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Token inválido o expirado\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
