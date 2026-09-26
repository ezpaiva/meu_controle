package br.com.meu_controle.dados.repositorio;

public interface RetornoRepositorio<T> {
    void onSuccess(T result);

    void onError(Exception error);
}
