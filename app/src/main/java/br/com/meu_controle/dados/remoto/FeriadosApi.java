package br.com.meu_controle.dados.remoto;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class FeriadosApi {

    public static final String ORIGEM_API = "api";
    public static final String ORIGEM_CACHE = "cache";
    public static final String ORIGEM_INDISPONIVEL = "indisponivel";

    private static final String TAG = "Feriados";
    private static final String URL_BASE = "https://brasilapi.com.br/api/feriados/v1/";
    private static final String PREFS = "cache_feriados";
    private static final int TIMEOUT_MS = 8000;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static class Feriado {
        public final LocalDate data;
        public final String nome;

        public Feriado(LocalDate data, String nome) {
            this.data = data;
            this.nome = nome;
        }
    }

    public interface Retorno {
        void aoConcluir(List<Feriado> feriados, String origem);
    }

    private FeriadosApi() {
    }

    /**
     * Busca os feriados nacionais do ano. Tenta a API; se falhar, usa a cópia
     * salva no aparelho; se não houver cópia, devolve lista vazia.
     */
    public static void buscar(Context contexto, int ano, Retorno retorno) {
        Context app = contexto.getApplicationContext();

        EXECUTOR.execute(() -> {
            List<Feriado> lista = new ArrayList<>();
            String origem = ORIGEM_INDISPONIVEL;
            String chave = "ano_" + ano;

            try {
                String json = baixar(ano);
                lista = interpretar(json);
                app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .edit().putString(chave, json).apply();
                origem = ORIGEM_API;
            } catch (Exception erro) {
                Log.w(TAG, "API indisponível, tentando cache: " + erro.getMessage());
                String salvo = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .getString(chave, null);
                if (salvo != null) {
                    try {
                        lista = interpretar(salvo);
                        origem = ORIGEM_CACHE;
                    } catch (Exception erroCache) {
                        Log.e(TAG, "Cache inválido", erroCache);
                    }
                }
            }

            final List<Feriado> resultado = lista;
            final String origemFinal = origem;
            MAIN.post(() -> retorno.aoConcluir(resultado, origemFinal));
        });
    }

    private static String baixar(int ano) throws IOException {
        HttpURLConnection conexao =
                (HttpURLConnection) new URL(URL_BASE + ano).openConnection();
        try {
            conexao.setRequestMethod("GET");
            conexao.setConnectTimeout(TIMEOUT_MS);
            conexao.setReadTimeout(TIMEOUT_MS);
            conexao.setRequestProperty("Accept", "application/json");

            int codigo = conexao.getResponseCode();
            if (codigo != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + codigo);
            }

            try (BufferedReader leitor = new BufferedReader(
                    new InputStreamReader(conexao.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder texto = new StringBuilder();
                String linha;
                while ((linha = leitor.readLine()) != null) {
                    texto.append(linha);
                }
                return texto.toString();
            }
        } finally {
            conexao.disconnect();
        }
    }

    private static List<Feriado> interpretar(String json) throws Exception {
        JSONArray vetor = new JSONArray(json);
        List<Feriado> lista = new ArrayList<>();
        for (int i = 0; i < vetor.length(); i++) {
            JSONObject item = vetor.getJSONObject(i);
            lista.add(new Feriado(
                    LocalDate.parse(item.getString("date")),
                    item.getString("name")));
        }
        return lista;
    }
}