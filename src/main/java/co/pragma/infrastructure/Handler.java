package co.pragma.infrastructure;

import co.pragma.domain.model.DecisionSolicitudPrestamo;
import co.pragma.domain.model.SolicitudValidacionAutoEvent;
import co.pragma.domain.usecase.EvaluarCapacidadUseCase;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import java.math.BigDecimal;

@Slf4j
public class Handler implements RequestHandler<SQSEvent, Void> {

    private final ObjectMapper mapper;
    private final EvaluarCapacidadUseCase evaluarCapacidadUseCase;
    private final SqsClient sqsClient;

    public Handler() {
        this.mapper = new ObjectMapper();
        this.evaluarCapacidadUseCase = new EvaluarCapacidadUseCase();
        this.sqsClient = SqsClient.create();
    }

    @Override
    public Void handleRequest(SQSEvent sqsEvent, Context context) {
        log.info("Iniciando validación automatica de solicitud de prestamo");
        try {
            for (SQSEvent.SQSMessage msg : sqsEvent.getRecords()) {
                SolicitudValidacionAutoEvent solicitudEvento = mapper.readValue(msg.getBody(), SolicitudValidacionAutoEvent.class);

                BigDecimal ingresos = solicitudEvento.getCliente().salarioBase();
                BigDecimal capacidadMax = ingresos.multiply(BigDecimal.valueOf(0.35));

                BigDecimal deudaMensualActual = solicitudEvento.getPrestamosActivos().stream()
                        .map(p -> evaluarCapacidadUseCase.calcularCuota(p.monto(), p.tasaInteresAnual(), p.plazoEnMeses()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal capacidadDisponible = capacidadMax.subtract(deudaMensualActual);

                BigDecimal cuotaNueva = evaluarCapacidadUseCase.calcularCuota(
                        solicitudEvento.getSolicitud().monto(),
                        solicitudEvento.getSolicitud().tasaInteresAnual(),
                        solicitudEvento.getSolicitud().plazoEnMeses()
                );

                String decision;
                if (cuotaNueva.compareTo(capacidadDisponible) <= 0) {
                    BigDecimal cincoSalarios = ingresos.multiply(BigDecimal.valueOf(5));
                    if (solicitudEvento.getSolicitud().monto().compareTo(cincoSalarios) > 0) {
                        log.info("RESULTADO: REVISION_MANUAL");
                        decision = "REVISION_MANUAL";
                    } else {
                        log.info("RESULTADO: APROBADA");
                        decision = "APROBADA";
                    }
                } else {
                    log.info("RESULTADO: RECHAZADA");
                    decision = "RECHAZADA";
                }

                DecisionSolicitudPrestamo resultado =  DecisionSolicitudPrestamo.builder()
                        .codigoSolicitud(solicitudEvento.getSolicitud().codigo())
                        .decision(decision)
                        .build();

                String jsonResultado = mapper.writeValueAsString(resultado);
                sqsClient.sendMessage(SendMessageRequest.builder()
                        .queueUrl("https://sqs.us-east-2.amazonaws.com/488340573632/queueCrediyaResultadoValidacionAuto")
                        .messageBody(jsonResultado)
                        .build());

                log.info("Resultado publicado en cola de salida: {}", jsonResultado);
            }
        } catch (Exception e) {
            log.info("Error procesando evento: {}", e.getMessage());
        }
        return null;
    }
}
