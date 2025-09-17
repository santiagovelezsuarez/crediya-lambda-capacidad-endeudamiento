package co.pragma.domain.model;

import java.math.BigDecimal;

public record PrestamoActivo(
        BigDecimal monto,
        Integer plazoEnMeses,
        BigDecimal tasaInteresAnual
) {}
