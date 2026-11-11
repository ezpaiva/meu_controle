package br.com.meu_controle.dados.remoto;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import br.com.meu_controle.MeuControleApp;
import br.com.meu_controle.dados.local.BancoDados;
import br.com.meu_controle.dados.local.ContaDao;
import br.com.meu_controle.dados.local.SessaoUsuario;
import br.com.meu_controle.dados.modelo.Conta;
import br.com.meu_controle.dados.repositorio.RepositorioLembretes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class SincronizadorContas {

    public interface Retorno {
        void aoConcluir(boolean sucesso, String mensagem);
    }

    private static final String TAG = "Sync";
    private static final long TIMEOUT_SEGUNDOS = 20;

    // Uma única fila: as sincronizações nunca rodam ao mesmo tempo
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private SincronizadorContas() {
    }

    /** Dispara a sincronização sem esperar resultado. */
    public static void sincronizar() {
        sincronizar(null);
    }

    public static void sincronizar(Retorno retorno) {
        Context contexto = MeuControleApp.obterContexto();
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();

        if (contexto == null || usuario == null) {
            avisar(retorno, false, "Nenhum usuário autenticado.");
            return;
        }
        if (!temInternet(contexto)) {
            avisar(retorno, false, "Sem conexão. As alterações ficam salvas e serão enviadas depois.");
            return;
        }

        String email = new SessaoUsuario(contexto).obterEmail();

        EXECUTOR.execute(() -> {
            try {
                ContaDao dao = BancoDados.obterInstancia(contexto).contaDao();
                RepositorioLembretes lembretes = new RepositorioLembretes(contexto);
                CollectionReference colecao = FirebaseFirestore.getInstance()
                        .collection("usuarios")
                        .document(usuario.getUid())
                        .collection("contas");

                baixar(dao, colecao, lembretes);
                enviar(dao, colecao, email);
                avisar(retorno, true, "Sincronizado.");
            } catch (Exception erro) {
                Log.e(TAG, "Falha ao sincronizar", erro);
                avisar(retorno, false, "Falha ao sincronizar: " + erro.getMessage());
            }
        });
    }

    // ---------- Nuvem -> aparelho ----------
    private static void baixar(ContaDao dao, CollectionReference colecao,
                               RepositorioLembretes lembretes) throws Exception {
        // Source.SERVER evita ler um cache desatualizado
        QuerySnapshot resultado = esperar(colecao.get(Source.SERVER));

        for (DocumentSnapshot doc : resultado.getDocuments()) {
            long remotoAtualizadoEm = valor(doc.getLong("updatedAt"));
            boolean remotoExcluido = Boolean.TRUE.equals(doc.getBoolean("deleted"));
            Conta local = dao.getByRemoteId(doc.getId());

            if (local == null) {
                if (remotoExcluido) continue; // nunca existiu aqui e já foi apagada
                Conta nova = new Conta("", 0, "", "", "", "", null, null);
                nova.remoteId = doc.getId();
                preencher(nova, doc, remotoAtualizadoEm);
                nova.id = dao.insert(nova);
                lembretes.reconciliarConta(nova);
                continue;
            }

            // Vence a alteração mais recente (RNF04)
            if (remotoAtualizadoEm > local.updatedAt) {
                if (remotoExcluido) {
                    lembretes.excluirPorConta(local.id);
                    dao.deleteById(local.id);
                } else {
                    preencher(local, doc, remotoAtualizadoEm);
                    dao.update(local);
                    lembretes.reconciliarConta(local);
                }
            }
        }
    }

    // ---------- Aparelho -> nuvem ----------
    private static void enviar(ContaDao dao, CollectionReference colecao, String email)
            throws Exception {
        List<Conta> pendentes = dao.getPendentesDoDono(email);

        for (Conta conta : pendentes) {
            if (conta.remoteId == null) {
                conta.remoteId = UUID.randomUUID().toString();
                dao.update(conta);
            }

            esperar(colecao.document(conta.remoteId).set(paraMapa(conta)));

            if (conta.deleted) {
                dao.deleteById(conta.id); // o servidor já guarda o registro de exclusão
            } else {
                // Só limpa a marca se a conta não foi editada durante o envio
                dao.marcarSincronizada(conta.id, conta.updatedAt);
            }
        }
    }

    // ---------- Auxiliares ----------
    private static void preencher(Conta conta, DocumentSnapshot doc, long atualizadoEm) {
        conta.description = doc.getString("description");
        conta.amountInCents = valor(doc.getLong("amountInCents"));
        conta.dueDate = doc.getString("dueDate");
        conta.category = doc.getString("category");
        conta.type = doc.getString("type");
        conta.status = doc.getString("status");
        conta.ownerEmail = doc.getString("ownerEmail");
        conta.responsibleName = doc.getString("responsibleName");
        conta.updatedAt = atualizadoEm;
        conta.syncPending = false;
        conta.deleted = false;
    }

    private static Map<String, Object> paraMapa(Conta conta) {
        Map<String, Object> mapa = new HashMap<>();
        mapa.put("description", conta.description);
        mapa.put("amountInCents", conta.amountInCents);
        mapa.put("dueDate", conta.dueDate);
        mapa.put("category", conta.category);
        mapa.put("type", conta.type);
        mapa.put("status", conta.status);
        mapa.put("ownerEmail", conta.ownerEmail);
        mapa.put("responsibleName", conta.responsibleName);
        mapa.put("updatedAt", conta.updatedAt);
        mapa.put("deleted", conta.deleted);
        return mapa;
    }

    private static long valor(Long numero) {
        return numero != null ? numero : 0L;
    }

    private static <T> T esperar(Task<T> tarefa) throws Exception {
        return Tasks.await(tarefa, TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
    }

    private static boolean temInternet(Context contexto) {
        ConnectivityManager gerente =
                (ConnectivityManager) contexto.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (gerente == null) return false;
        Network rede = gerente.getActiveNetwork();
        if (rede == null) return false;
        NetworkCapabilities capacidades = gerente.getNetworkCapabilities(rede);
        return capacidades != null
                && capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private static void avisar(Retorno retorno, boolean sucesso, String mensagem) {
        if (retorno != null) {
            MAIN.post(() -> retorno.aoConcluir(sucesso, mensagem));
        }
    }
}