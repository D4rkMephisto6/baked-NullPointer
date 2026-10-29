package pe.upeu.nullpointerpos;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import pe.upeu.nullpointerpos.security.JwtService;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    @Test
    void generatesSignedTokenWithEmployeeAndRole() {
        JwtService service = new JwtService("NullPointerPOS-Test-Secret-Must-Be-Long-Enough-2026!", 60);
        String token = service.issue(7L, "MOZ001", "Mozo de prueba", "MOZO");
        Claims claims = service.parse(token);
        assertThat(claims.getSubject()).isEqualTo("MOZ001");
        assertThat(claims.get("role", String.class)).isEqualTo("MOZO");
        assertThat(claims.get("uid")).isNotNull();
    }
}
