package pe.upeu.nullpointerpos.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotBlank(message = "employeeCode es obligatorio") String employeeCode,
        @NotBlank(message = "pin es obligatorio")
        @Pattern(regexp = "\\d{4,8}", message = "El PIN debe tener entre 4 y 8 dígitos") String pin) { }
