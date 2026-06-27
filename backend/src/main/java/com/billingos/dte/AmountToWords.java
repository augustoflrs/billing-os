package com.billingos.dte;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Converts a monetary amount to Spanish words as required by MH DTE spec.
 * Covers 0.00 – 999,999,999.99 USD.
 */
final class AmountToWords {

    private static final String[] UNITS = {
        "", "UN", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
        "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISÉIS",
        "DIECISIETE", "DIECIOCHO", "DIECINUEVE"
    };

    private static final String[] TENS = {
        "", "", "VEINTE", "TREINTA", "CUARENTA", "CINCUENTA",
        "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"
    };

    private static final String[] HUNDREDS = {
        "", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS",
        "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"
    };

    private AmountToWords() {}

    static String convert(BigDecimal amount) {
        BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);
        long intPart  = rounded.longValue();
        int  cents    = rounded.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(100))
                              .abs().setScale(0, RoundingMode.HALF_UP).intValue();

        String words = intPart == 0 ? "CERO" : integerToWords(intPart);
        return words + " " + String.format("%02d", cents) + "/100 DÓLARES";
    }

    private static String integerToWords(long n) {
        if (n < 0) return "MENOS " + integerToWords(-n);
        if (n < 20) return UNITS[(int) n];
        if (n == 20) return "VEINTE";
        if (n < 30) {
            String rest = UNITS[(int) (n - 20)];
            return "VEINTI" + (rest.isEmpty() ? "" : rest.charAt(0) + rest.substring(1).toLowerCase());
        }
        if (n < 100) {
            String ten  = TENS[(int) (n / 10)];
            String unit = n % 10 == 0 ? "" : " Y " + UNITS[(int) (n % 10)];
            return ten + unit;
        }
        if (n == 100) return "CIEN";
        if (n < 1_000) {
            String hundred = HUNDREDS[(int) (n / 100)];
            String rest    = n % 100 == 0 ? "" : " " + integerToWords(n % 100);
            return hundred + rest;
        }
        if (n < 2_000) return "MIL" + (n % 1_000 == 0 ? "" : " " + integerToWords(n % 1_000));
        if (n < 1_000_000) {
            long thousands = n / 1_000;
            long rest      = n % 1_000;
            return integerToWords(thousands) + " MIL" + (rest == 0 ? "" : " " + integerToWords(rest));
        }
        if (n == 1_000_000) return "UN MILLÓN";
        if (n < 2_000_000) return "UN MILLÓN " + integerToWords(n - 1_000_000);
        if (n < 1_000_000_000L) {
            long millions = n / 1_000_000;
            long rest     = n % 1_000_000;
            return integerToWords(millions) + " MILLONES" + (rest == 0 ? "" : " " + integerToWords(rest));
        }
        return String.valueOf(n); // fallback for very large amounts
    }
}
