package br.com.meu_controle.dados.modelo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GrupoFamiliarTest {

    @Test
    public void grupoNovoComecaSemMembrosENaoAtivo() {
        GrupoFamiliar grupo = new GrupoFamiliar();

        assertNotNull(grupo.membros);
        assertTrue(grupo.membros.isEmpty());
        assertFalse(grupo.isAtivo());
    }

    @Test
    public void listaDeMembrosNaoEhCompartilhadaEntreGrupos() {
        GrupoFamiliar a = new GrupoFamiliar();
        GrupoFamiliar b = new GrupoFamiliar();

        a.membros.add("uid-1");

        assertEquals(1, a.membros.size());
        assertTrue(b.membros.isEmpty());
    }

    @Test
    public void gettersRetornamOQueFoiDefinido() {
        GrupoFamiliar grupo = new GrupoFamiliar();
        grupo.setId("g1");
        grupo.setNome("Família Souza");
        grupo.setCriadorEmail("ana@example.com");
        grupo.setDataCriacao(1_700_000_000_000L);
        grupo.setAtivo(true);

        assertEquals("g1", grupo.getId());
        assertEquals("Família Souza", grupo.getNome());
        assertEquals("ana@example.com", grupo.getCriadorEmail());
        assertEquals(1_700_000_000_000L, grupo.getDataCriacao());
        assertTrue(grupo.isAtivo());
    }
}