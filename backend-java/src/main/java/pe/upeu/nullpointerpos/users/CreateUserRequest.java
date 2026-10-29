package pe.upeu.nullpointerpos.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "employeeCode es obligatorio") @Size(max = 30) String employeeCode,
        @NotBlank(message = "fullName es obligatorio") @Size(max = 120) String fullName,
        @Email(message = "email no tiene un formato válido") @Size(max = 160) String email,
        @NotBlank(message = "pin es obligatorio") @Pattern(regexp = "\\d{4,8}", message = "El PIN debe tener entre 4 y 8 dígitos") String pin,
        @NotBlank(message = "role es obligatorio") @Pattern(regexp = "MOZO|CAJERO|ADMINISTRADOR|COCINERO|ALMACENERO", message = "Rol no permitido") String role) { }
