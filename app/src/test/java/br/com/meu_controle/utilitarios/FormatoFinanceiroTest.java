package br.com.meu_controle.utilitarios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.format.DateTimeParseException;

public class FormatoFinanceiroTest {

    // O JDK usa espaço "não separável" entre "R$" e o número; normalizamos para comparar
    private static String normalizar(String texto) {
        return texto.replace('\u00A0', ' ').replace('\u202F', ' ');
    }

    // ---------- parseAmountToCents ----------

    @Test
    public void valorInteiroViraCentavos() {
        assertEquals(1000L, FormatoFinanceiro.parseAmountToCents("10"));
    }

    @Test
    public void virgulaDecimalViraCentavos() {
        assertEquals(1050L, FormatoFinanceiro.parseAmountToCents("10,50"));
    }

    @Test
    public void pontoDeMilharComVirgulaDecimal() {
        assertEquals(123456L, FormatoFinanceiro.parseAmountToCents("1.234,56"));
    }

    @Test
    public void pontoDecimalSemVirgulaTambemEhAceito() {
        assertEquals(123456L, FormatoFinanceiro.parseAmountToCents("1234.56"));
    }

    @Test
    public void menorValorPossivelEhUmCentavo() {
        assertEquals(1L, FormatoFinanceiro.parseAmountToCents("0,01"));
    }

    @Test
    public void espacosSaoIgnorados() {
        assertEquals(1230L, FormatoFinanceiro.parseAmountToCents("  12,30  "));
        assertEquals(100000L, FormatoFinanceiro.parseAmountToCents("1 000,00"));
    }

    @Test
    public void maiorValorQueCabeEmLong() {
        assertEquals(Long.MAX_VALUE,
                FormatoFinanceiro.parseAmountToCents("92233720368547758,07"));
    }

    @Test
    public void valorQueEstouraLongEhRejeitado() {
        assertThrows(IllegalArgumentException.class,
                () -> FormatoFinanceiro.parseAmountToCents("92233720368547758,08"));
    }

    @Test
    public void textoVazioOuInvalidoEhRejeitado() {
        assertThrows(IllegalArgumentException.class,
                () -> FormatoFinanceiro.parseAmountToCents(""));
        assertThrows(IllegalArgumentException.class,
                () -> FormatoFinanceiro.parseAmountToCents("abc"));
        assertThrows(IllegalArgumentException.class,
                () -> FormatoFinanceiro.parseAmountToCents("1,2,3"));
    }

    @Test
    public void zeroENegativoSaoRejeitados() {
        assertThrows(IllegalArgumentException.class,
                () -> FormatoFinanceiro.parseAmountToCents("0"));
        assertThrows(IllegalArgumentException.class,
                () -> FormatoFinanceiro.parseAmountToCents("0,00"));
        assertThrows(IllegalArgumentException.class,
                () -> FormatoFinanceiro.parseAmountToCents("-5,00"));
    }

    @Test
    public void maisDeDuasCasasDecimaisEhRejeitado() {
        assertThrows(IllegalArgumentException.class,
                () -> FormatoFinanceiro.parseAmountToCents("10,999"));
    }

    // Documenta o comportamento atual: sem vírgula, "1.234" é lido como decimal (3 casas) e rejeitado
    @Test
    public void milharSemVirgulaDecimalEhRejeitadoPorAmbiguidade() {
        assertThrows(IllegalArgumentException.class,
                () -> FormatoFinanceiro.parseAmountToCents("1.234"));
    }

    // ---------- formatAmount ----------

    @Test
    public void formataMoedaEmReais() {
        assertEquals("R$ 1.234,56", normalizar(FormatoFinanceiro.formatAmount(123456)));
    }

    @Test
    public void formataZeroEPoucosCentavos() {
        assertEquals("R$ 0,00", normalizar(FormatoFinanceiro.formatAmount(0)));
        assertEquals("R$ 0,05", normalizar(FormatoFinanceiro.formatAmount(5)));
    }

    @Test
    public void formataMilhoes() {
        assertEquals("R$ 1.000.000,00",
                normalizar(FormatoFinanceiro.formatAmount(100_000_000L)));
    }

    @Test
    public void formataValorNegativo() {
        String texto = normalizar(FormatoFinanceiro.formatAmount(-1050));
        assertTrue(texto.startsWith("-"));
        assertTrue(texto.contains("10,50"));
    }

    @Test
    public void formatarEInterpretarSaoOperacoesInversas() {
        long centavos = 987654L;
        String formatado = normalizar(FormatoFinanceiro.formatAmount(centavos))
                .replace("R$", "").trim();
        assertEquals(centavos, FormatoFinanceiro.parseAmountToCents(formatado));
    }

    // ---------- datas ----------

    @Test
    public void converteDataDeExibicaoParaIso() {
        assertEquals("2026-12-25", FormatoFinanceiro.parseDisplayDate("25/12/2026"));
    }

    @Test
    public void aceitaDia29DeFevereiroEmAnoBissexto() {
        assertEquals("2028-02-29", FormatoFinanceiro.parseDisplayDate("29/02/2028"));
    }

    @Test
    public void rejeitaDatasQueNaoExistem() {
        assertThrows(IllegalArgumentException.class,
                () -> FormatoFinanceiro.parseDisplayDate("29/02/2027"));
        assertThrows(IllegalArgumentException.class,
                () -> FormatoFinanceiro.parseDisplayDate("31/04/2026"));
        assertThrows(IllegalArgumentException.class,
                () -> FormatoFinanceiro.parseDisplayDate("32/01/2026"));
    }

    @Test
    public void rejeitaDatasForaDoFormato() {
        assertThrows(IllegalArgumentException.class,
                () -> FormatoFinanceiro.parseDisplayDate("2026-12-25"));
        assertThrows(IllegalArgumentException.class,
                () -> FormatoFinanceiro.parseDisplayDate("1/1/2026"));
        assertThrows(IllegalArgumentException.class,
                () -> FormatoFinanceiro.parseDisplayDate(""));
    }

    @Test
    public void formataDataGuardadaParaExibicao() {
        assertEquals("25/12/2026", FormatoFinanceiro.formatStoredDate("2026-12-25"));
        assertEquals("01/01/2027", FormatoFinanceiro.formatStoredDate("2027-01-01"));
    }

    @Test
    public void dataGuardadaInvalidaLancaErroDeParse() {
        assertThrows(DateTimeParseException.class,
                () -> FormatoFinanceiro.formatStoredDate("abc"));
    }

    @Test
    public void dataDeExibicaoEGuardadaFazemIdaEVolta() {
        String guardada = FormatoFinanceiro.parseDisplayDate("07/03/2026");
        assertEquals("07/03/2026", FormatoFinanceiro.formatStoredDate(guardada));
    }
}