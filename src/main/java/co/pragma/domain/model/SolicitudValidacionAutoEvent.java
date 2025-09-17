package co.pragma.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudValidacionAutoEvent {
    private Solicitud solicitud;
    private Cliente cliente;
    private List<PrestamoActivo> prestamosActivos;
}
