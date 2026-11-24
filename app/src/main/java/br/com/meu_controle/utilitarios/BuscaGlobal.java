package br.com.meu_controle.utilitarios;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import br.com.meu_controle.dados.modelo.Categoria;
import br.com.meu_controle.dados.modelo.Conta;
import br.com.meu_controle.dados.modelo.LembreteComConta;

public final class BuscaGlobal {
    private BuscaGlobal() {
    }

    public static List<Conta> filtrarContas(List<Conta> contas, String texto) {
        List<Conta> resultado = new ArrayList<>();
        String busca = normalizar(texto);
        for (Conta conta : contas) {
            if (conta != null && (
                    normalizar(conta.description).contains(busca)
                            || normalizar(conta.category).contains(busca))) {
                resultado.add(conta);
            }
        }
        return resultado;
    }

    public static List<Categoria> filtrarCategorias(List<Categoria> categorias, String texto) {
        List<Categoria> resultado = new ArrayList<>();
        String busca = normalizar(texto);
        for (Categoria categoria : categorias) {
            if (categoria != null && normalizar(categoria.nome).contains(busca)) {
                resultado.add(categoria);
            }
        }
        return resultado;
    }

    public static List<LembreteComConta> filtrarLembretes(
            List<LembreteComConta> lembretes, String texto) {
        List<LembreteComConta> resultado = new ArrayList<>();
        String busca = normalizar(texto);
        for (LembreteComConta item : lembretes) {
            if (item != null && item.lembrete != null
                    && (normalizar(item.lembrete.titulo).contains(busca)
                    || normalizar(item.lembrete.mensagem).contains(busca)
                    || normalizar(item.contaDescricao).contains(busca))) {
                resultado.add(item);
            }
        }
        return resultado;
    }

    private static String normalizar(String texto) {
        return texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
    }
}
