package br.com.meu_controle.utilitarios;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ValidadorContaTest {

    @Test
    public void contaComDadosValidos_deveSerAceita() {
        assertTrue(ValidadorConta.isContaValida(
                "Pagamento da energia", "250,75", "20/11/2026"));
    }

    @Test
    public void descricaoNormal_deveSerAceita() {
        assertTrue(ValidadorConta.isDescricaoValida("Mercado"));
    }

    @Test
    public void descricaoVazia_naoDeveSerAceita() {
        assertFalse(ValidadorConta.isDescricaoValida("   "));
    }

    @Test
    public void descricaoNula_naoDeveSerAceita() {
        assertFalse(ValidadorConta.isDescricaoValida(null));
    }

    @Test
    public void valorPositivoMinimo_deveSerAceito() {
        assertTrue(ValidadorConta.isValorValido("0,01"));
    }

    @Test
    public void valorZero_naoDeveSerAceito() {
        assertFalse(ValidadorConta.isValorValido("0"));
    }

    @Test
    public void valorNegativo_naoDeveSerAceito() {
        assertFalse(ValidadorConta.isValorValido("-10,00"));
    }

    @Test
    public void dataValidaDeveManterFormatoArmazenado() {
        assertEquals("2026-11-20",
                FormatoFinanceiro.parseDisplayDate("20/11/2026"));
    }

    @Test
    public void dataInvalida_naoDeveSerAceita() {
        assertFalse(ValidadorConta.isDataValida("31/02/2026"));
    }

    @Test
    public void dataNula_naoDeveSerAceita() {
        assertFalse(ValidadorConta.isDataValida(null));
    }
}
