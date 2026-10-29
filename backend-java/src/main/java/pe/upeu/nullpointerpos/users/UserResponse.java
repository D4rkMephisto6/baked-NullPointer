package pe.upeu.nullpointerpos.users;

import java.time.OffsetDateTime;

public record UserResponse(Long userId, String employeeCode, String fullName, String email,
                           String role, boolean active, OffsetDateTime createdAt) { }
