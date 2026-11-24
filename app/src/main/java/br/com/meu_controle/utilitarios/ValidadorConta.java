package br.com.meu_controle.utilitarios;

public final class ValidadorConta {

    private ValidadorConta() {
    }

    public static boolean isDescricaoValida(String descricao) {
        return descricao != null && !descricao.trim().isEmpty();
    }

    public static boolean isValorValido(String valor) {
        try {
            FormatoFinanceiro.parseAmountToCents(valor);
            return true;
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    public static boolean isDataValida(String dataExibicao) {
        if (dataExibicao == null || dataExibicao.trim().isEmpty()) {
            return false;
        }
        try {
            FormatoFinanceiro.parseDisplayDate(dataExibicao);
            return true;
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    public static boolean isContaValida(String descricao, String valor,
                                        String dataExibicao) {
        return isDescricaoValida(descricao)
                && isValorValido(valor)
                && isDataValida(dataExibicao);
    }
}
