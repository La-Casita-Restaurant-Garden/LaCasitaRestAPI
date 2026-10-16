package com.service.lacasita.utils;

import com.service.lacasita.entity.PaymentMethod;


public final class PaymentMethods {


    public static final String SIN_METODO = "NO PAYMENT METHOD";

    private PaymentMethods() {
    }

    /** ¿Se cobra en efectivo? Decide si el checkout pide el vuelto. */
    public static boolean esEfectivo(PaymentMethod metodo) {
        String n = normalizar(metodo);
        return n.contains("EFECTIVO") || n.contains("CASH");
    }

    /** ¿Es fiado? Decide si el pedido carga a la cuenta del cliente. */
    public static boolean esCredito(PaymentMethod metodo) {
        String n = normalizar(metodo);
        return n.contains("CREDITO") || n.contains("FIADO");
    }


    public static boolean sinCobrar(String nombreMetodo) {
        String n = normalizar(nombreMetodo);
        return n.isEmpty() || n.equals(SIN_METODO);
    }

    /** @see #sinCobrar(String) */
    public static boolean sinCobrar(PaymentMethod metodo) {
        return sinCobrar(metodo == null ? null : metodo.getName());
    }

    /** El cobro ya se cerró: hay una forma de pago decidida. */
    public static boolean estaCobrada(PaymentMethod metodo) {
        return !sinCobrar(metodo);
    }


    private static String normalizar(PaymentMethod metodo) {
        return normalizar(metodo == null ? null : metodo.getName());
    }

    private static String normalizar(String nombre) {
        if (nombre == null) return "";
        return java.text.Normalizer
                .normalize(nombre, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('_', ' ')
                .trim()
                .toUpperCase();
    }
}
