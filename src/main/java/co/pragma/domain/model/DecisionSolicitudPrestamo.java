package co.pragma.domain.model;

import lombok.Builder;

@Builder
public record DecisionSolicitudPrestamo(
        String codigoSolicitud,
        String decision
) {}
