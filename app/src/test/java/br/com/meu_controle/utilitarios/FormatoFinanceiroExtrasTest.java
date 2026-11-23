package br.com.meu_controle.utilitarios;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FormatoFinanceiroExtrasTest {

    /**
     * Normaliza espaços não separáveis (U+00A0) e espaços finos (U+202F)
     * para espaço comum, para que a asserção não dependa do tipo de espaço
     * que o NumberFormat da JVM/Android escolheu emitir.
     */
    private static String normalizar(String valor) {
        return valor
                .replace('\u00A0', ' ')   // NBSP
                .replace('\u202F', ' ');  // narrow NBSP
    }

    @Test
    public void zero_formataComoZeroReais() {
        assertEquals("R$ 0,00", normalizar(FormatoFinanceiro.formatAmount(0L)));
    }

    @Test
    public void umCentavo_formataComDuasCasas() {
        assertTrue(normalizar(FormatoFinanceiro.formatAmount(1L)).contains("0,01"));
    }

    @Test
    public void noventaENoveCentavos_formataComDuasCasas() {
        assertTrue(normalizar(FormatoFinanceiro.formatAmount(99L)).contains("0,99"));
    }

    @Test
    public void umReal_formataComDuasCasas() {
        assertTrue(normalizar(FormatoFinanceiro.formatAmount(100L)).contains("1,00"));
    }

    @Test
    public void valoresGrandes_usamSeparadorDeMilhar() {
        String formatado = normalizar(FormatoFinanceiro.formatAmount(1_234_567L));
        // 1.234.567 centavos = R$ 12.345,67
        assertTrue("Esperado separador de milhar em: " + formatado,
                formatado.contains("."));
        assertTrue(formatado.contains(",67"));
    }

    @Test
    public void valoresNegativos_temSinalVisivel() {
        String formatado = normalizar(FormatoFinanceiro.formatAmount(-1_500L));
        assertTrue("Valor negativo deve conter '-': " + formatado,
                formatado.contains("-"));
    }
}