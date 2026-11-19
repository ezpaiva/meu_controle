package br.com.meu_controle.utilitarios;

import br.com.meu_controle.dados.modelo.Conta;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Regras do filtro de categorias da tela de listagem (sem dependência do Android). */
public final class FiltroContas {

    public static final String TODAS_CATEGORIAS = "Todas categorias";

    private FiltroContas() {
    }

    /** Contas da categoria informada. Categoria nula (ou "Todas categorias") devolve todas. */
    public static List<Conta> porCategoria(List<Conta> contas, String categoria) {
        List<Conta> resultado = new ArrayList<>();
        boolean semFiltro = categoria == null || TODAS_CATEGORIAS.equals(categoria);
        for (Conta conta : contas) {
            if (semFiltro || categoria.equals(conta.category)) {
                resultado.add(conta);
            }
        }
        return resultado;
    }

    /**
     * Opções do filtro: "Todas categorias", depois as categorias cadastradas (na ordem
     * recebida) e, por fim, categorias usadas por contas mas que não estão mais cadastradas.
     */
    public static List<String> opcoesDeCategoria(List<String> cadastradas, List<Conta> contas) {
        List<String> opcoes = new ArrayList<>();
        opcoes.add(TODAS_CATEGORIAS);

        for (String nome : cadastradas) {
            if (nome != null && !nome.trim().isEmpty() && !opcoes.contains(nome)) {
                opcoes.add(nome);
            }
        }

        TreeSet<String> usadasSemCadastro = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Conta conta : contas) {
            String categoria = conta.category;
            if (categoria != null && !categoria.trim().isEmpty() && !opcoes.contains(categoria)) {
                usadasSemCadastro.add(categoria);
            }
        }
        opcoes.addAll(usadasSemCadastro);
        return opcoes;
    }
}