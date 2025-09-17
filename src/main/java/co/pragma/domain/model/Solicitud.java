package co.pragma.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record Solicitud(
        UUID id,
        String codigo,
        BigDecimal monto,
        Integer plazoEnMeses,
        UUID tipoPrestamoId,
        BigDecimal tasaInteresAnual
) {}
