package com.service.lacasita.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Costo promedio ponderado de un insumo.
 * @author Jason Nunez
 */
public final class CostoPromedio {

    private static final int ESCALA = 4;

    private CostoPromedio() {
    }

  
    public static BigDecimal recalcular(BigDecimal saldoAnterior, BigDecimal costoAnterior,
            BigDecimal cantidadEntrada, BigDecimal costoEntrada) {


        if (costoEntrada == null || cantidadEntrada == null || cantidadEntrada.signum() <= 0) {
            return costoAnterior;
        }

        if (saldoAnterior == null || saldoAnterior.signum() <= 0 || costoAnterior == null) {
            return costoEntrada.setScale(ESCALA, RoundingMode.HALF_UP);
        }

        BigDecimal valorPrevio = saldoAnterior.multiply(costoAnterior);
        BigDecimal valorEntrada = cantidadEntrada.multiply(costoEntrada);
        BigDecimal saldoTotal = saldoAnterior.add(cantidadEntrada);

        return valorPrevio.add(valorEntrada).divide(saldoTotal, ESCALA, RoundingMode.HALF_UP);
    }

    public static BigDecimal porUnidadDeConsumo(BigDecimal costoPresentacion, BigDecimal factor) {

        if (costoPresentacion == null) {
            return null;
        }

        if (factor == null || factor.signum() <= 0) {
            return costoPresentacion.setScale(ESCALA, RoundingMode.HALF_UP);
        }

        return costoPresentacion.divide(factor, ESCALA, RoundingMode.HALF_UP);
    }
}
