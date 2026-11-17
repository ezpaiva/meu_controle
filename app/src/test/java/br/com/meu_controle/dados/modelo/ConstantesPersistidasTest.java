package br.com.meu_controle.dados.modelo;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ConstantesPersistidasTest {

    @Test
    public void tiposEStatusDeContaSaoOsValoresJaGravadosNosDados() {
        assertEquals("RECEBER", TipoConta.RECEBER);
        assertEquals("PAGAR", TipoConta.PAGAR);
        assertEquals("PENDENTE", TipoConta.PENDENTE);
        assertEquals("CONCLUIDA", TipoConta.CONCLUIDA);
    }

    @Test
    public void tiposDePerfilSaoOsValoresJaGravadosNosDados() {
        assertEquals("INDIVIDUAL", TipoPerfil.INDIVIDUAL);
        assertEquals("FAMILIAR", TipoPerfil.FAMILIAR);
    }
}