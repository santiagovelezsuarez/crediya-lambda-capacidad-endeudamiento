package co.pragma.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record Cliente(
        UUID id,
        String nombres,
        String apellidos,
        String email,
        BigDecimal salarioBase,
        String fullName
) {}
