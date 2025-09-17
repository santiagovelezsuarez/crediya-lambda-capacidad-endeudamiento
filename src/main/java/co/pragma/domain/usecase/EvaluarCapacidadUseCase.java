package co.pragma.domain.usecase;

import co.pragma.domain.model.Cliente;
import co.pragma.domain.model.PrestamoActivo;
import co.pragma.domain.model.DecisionSolicitudPrestamo;
import co.pragma.domain.model.Solicitud;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

public class EvaluarCapacidadUseCase {

    private static final BigDecimal FACTOR_CAPACIDAD = BigDecimal.valueOf(0.35);

    public DecisionSolicitudPrestamo evaluar(Solicitud solicitud, Cliente cliente, List<PrestamoActivo> prestamos) {
        BigDecimal capacidadMax = cliente.salarioBase().multiply(FACTOR_CAPACIDAD);

        BigDecimal deudaActual = prestamos.stream()
                .map(p -> calcularCuota(p.monto(), p.tasaInteresAnual(), p.plazoEnMeses()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal capacidadDisponible = capacidadMax.subtract(deudaActual);

        BigDecimal cuotaNuevo = calcularCuota(
                solicitud.monto(),
                solicitud.tasaInteresAnual(),
                solicitud.plazoEnMeses());

        String decision;
        if (cuotaNuevo.compareTo(capacidadDisponible) <= 0) {
            decision = (solicitud.monto().compareTo(cliente.salarioBase().multiply(BigDecimal.valueOf(5))) > 0)
                    ? "REVISION_MANUAL" : "APROBADA";
        } else {
            decision = "RECHAZADA";
        }

        return new DecisionSolicitudPrestamo(solicitud.codigo(), decision);
    }

    /**
     * Ref: HU7
     * Calcula la cuota mensual de un préstamo.
     * Fórmula de Cuota: P * (i * (1 + i)^n) / ((1 + i)^n - 1)
     * Donde:
     * - P: Monto del préstamo.
     * - i: Tasa de interés mensual.
     * - n: Plazo en meses.     *
     * @param tasaInteresAnual La tasa de interés anual en porcentaje.
     * @return La cuota mensual calculada con dos decimales.
     */
    public BigDecimal calcularCuota(BigDecimal monto, BigDecimal tasaInteresAnual, int plazoEnMeses) {
        MathContext mc = new MathContext(20, RoundingMode.HALF_UP);

        BigDecimal tasaMensual = tasaInteresAnual.divide(BigDecimal.valueOf(100), mc).divide(BigDecimal.valueOf(12), mc);

        if (tasaMensual.compareTo(BigDecimal.ZERO) == 0)
            return monto.divide(BigDecimal.valueOf(plazoEnMeses), 2, RoundingMode.HALF_UP);

        BigDecimal unoMasTasa = BigDecimal.ONE.add(tasaMensual, mc);
        BigDecimal potencia = unoMasTasa.pow(-plazoEnMeses, mc);

        BigDecimal numerador = monto.multiply(tasaMensual, mc);
        BigDecimal denominador = BigDecimal.ONE.subtract(potencia, mc);

        return numerador.divide(denominador, 2, RoundingMode.HALF_UP);
    }
}
