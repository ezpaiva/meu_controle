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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import br.com.meu_controle.MeuControleApp;
import br.com.meu_controle.dados.local.BancoDados;
import br.com.meu_controle.dados.local.ContaDao;
import br.com.meu_controle.dados.local.SessaoUsuario;
import br.com.meu_controle.dados.modelo.Conta;
import br.com.meu_controle.dados.modelo.Lembrete;
import br.com.meu_controle.dados.modelo.TipoPerfil;
import br.com.meu_controle.services.NotificationScheduler;

public final class SincronizadorContas {

    public interface Retorno {
        void aoConcluir(boolean sucesso, String mensagem);
    }

    private static final String TAG = "Sync";
    private static final long TIMEOUT_SEGUNDOS = 20;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private SincronizadorContas() {
    }

    public static void sincronizar() {
        sincronizar(null);
    }

    public static void sincronizar(Retorno retorno) {
        Context context = MeuControleApp.obterContexto();
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (context == null || usuario == null) {
            avisar(retorno, false, "Nenhum usuário autenticado.");
            return;
        }
        if (!temInternet(context)) {
            avisar(retorno, false, "Sem conexão. As alterações continuam salvas localmente.");
            return;
        }

        SessaoUsuario sessao = new SessaoUsuario(context);
        String email = sessao.obterEmail();
        String groupId = sessao.obterGrupoId();
        if (email == null || email.trim().isEmpty()) {
            avisar(retorno, false, "Sessão sem e-mail.");
            return;
        }

        EXECUTOR.execute(() -> {
            try {
                BancoDados db = BancoDados.obterInstancia(context);
                ContaDao dao = db.contaDao();
                if (groupId == null || groupId.trim().isEmpty()) {
                    CollectionReference contas = FirebaseFirestore.getInstance()
                            .collection("usuarios").document(usuario.getUid())
                            .collection("contas");
                    dao.adotarContasSemDono(email);
                    baixarPessoais(dao, contas, email);
                    enviarPessoais(dao, contas, email);
                } else {
                    validarAssociacao(context, db, usuario, sessao, email, groupId);
                    CollectionReference contas = FirebaseFirestore.getInstance()
                            .collection("grupos_familiares").document(groupId)
                            .collection("contas");
                    baixarDoGrupo(dao, contas, email, groupId);
                    enviarDoGrupo(dao, contas, groupId);
                    SincronizadorFamiliar.sincronizar(context, groupId);
                }
                avisar(retorno, true, "Sincronizado.");
            } catch (Exception error) {
                Log.e(TAG, "Falha ao sincronizar", error);
                avisar(retorno, false, "Falha ao sincronizar: " + error.getMessage());
            }
        });
    }

    private static void validarAssociacao(Context context, BancoDados db, FirebaseUser usuario,
                                          SessaoUsuario sessao, String email, String groupId)
            throws Exception {
        DocumentSnapshot perfil = esperar(FirebaseFirestore.getInstance()
                .collection("usuarios").document(usuario.getUid()).get(Source.SERVER));
        if (perfil.exists() && groupId.equals(perfil.getString("grupoId"))) return;

        for (Lembrete lembrete : db.lembreteDao().getDoGrupo(groupId)) {
            new NotificationScheduler(context).cancelNotification(lembrete.id);
        }
        db.lembreteDao().excluirCacheDoGrupo(groupId);
        db.contaDao().excluirCacheDoGrupo(groupId);
        db.categoriaDao().excluirCacheDoGrupo(groupId);
        sessao.salvarPerfil(sessao.obterNome(), email, TipoPerfil.INDIVIDUAL, null, null);
        throw new SecurityException("Seu acesso ao grupo familiar foi removido.");
    }

    private static void baixarPessoais(ContaDao dao, CollectionReference colecao, String email)
            throws Exception {
        QuerySnapshot resultado = esperar(colecao.get(Source.SERVER));
        for (DocumentSnapshot doc : resultado.getDocuments()) {
            long atualizado = valor(doc.getLong("updatedAt"));
            boolean excluida = Boolean.TRUE.equals(doc.getBoolean("deleted"));
            Conta local = dao.getByPersonalRemoteId(doc.getId());
            if (local == null) {
                if (excluida) continue;
                Conta nova = new Conta("", 0, "", "", "", "", email, null);
                nova.remoteId = doc.getId();
                preencher(nova, doc, atualizado);
                nova.groupId = null;
                dao.insert(nova);
            } else if (atualizado > local.updatedAt) {
                if (excluida) dao.deleteById(local.id);
                else {
                    preencher(local, doc, atualizado);
                    local.groupId = null;
                    dao.update(local);
                }
            }
        }
    }

    private static void enviarPessoais(ContaDao dao, CollectionReference colecao, String email)
            throws Exception {
        List<Conta> pendentes = dao.getPendentesDoDono(email);
        for (Conta conta : pendentes) {
            if (conta.groupId != null) continue;
            if (conta.remoteId == null) {
                conta.remoteId = UUID.randomUUID().toString();
                dao.update(conta);
            }
            esperar(colecao.document(conta.remoteId).set(paraMapa(conta)));
            if (conta.deleted) dao.deleteById(conta.id);
            else dao.marcarSincronizada(conta.id, conta.updatedAt);
        }
    }

    private static void baixarDoGrupo(ContaDao dao, CollectionReference colecao, String email,
                                      String groupId) throws Exception {
        QuerySnapshot resultado = esperar(colecao.get(Source.SERVER));
        for (DocumentSnapshot doc : resultado.getDocuments()) {
            long atualizado = valor(doc.getLong("updatedAt"));
            boolean excluida = Boolean.TRUE.equals(doc.getBoolean("deleted"));
            Conta local = dao.getByRemoteIdAndGroup(doc.getId(), groupId);
            if (local == null) {
                if (excluida) {
                    excluirLembretesDoGrupo(groupId, doc.getId());
                    continue;
                }
                Conta nova = new Conta("", 0, "", "", "", "", email, null);
                nova.remoteId = doc.getId();
                nova.groupId = groupId;
                preencher(nova, doc, atualizado);
                nova.groupId = groupId;
                dao.insert(nova);
            } else if (atualizado > local.updatedAt) {
                if (excluida) {
                    excluirLembretesDoGrupo(groupId, doc.getId());
                    dao.deleteById(local.id);
                }
                else {
                    preencher(local, doc, atualizado);
                    local.groupId = groupId;
                    dao.update(local);
                }
            }
        }
    }

    private static void enviarDoGrupo(ContaDao dao, CollectionReference colecao, String groupId)
            throws Exception {
        List<Conta> pendentes = dao.getPendentesDoGrupo(groupId);
        for (Conta conta : pendentes) {
            if (conta.remoteId == null) {
                conta.remoteId = UUID.randomUUID().toString();
                dao.update(conta);
            }
            esperar(colecao.document(conta.remoteId).set(paraMapa(conta)));
            if (conta.deleted) {
                excluirLembretesDoGrupo(groupId, conta.remoteId);
                dao.deleteById(conta.id);
            }
            else dao.marcarSincronizada(conta.id, conta.updatedAt);
        }
    }

    private static void excluirLembretesDoGrupo(String groupId, String contaRemoteId)
            throws Exception {
        CollectionReference lembretes = FirebaseFirestore.getInstance()
                .collection("grupos_familiares").document(groupId).collection("lembretes");
        QuerySnapshot relacionados = esperar(lembretes
                .whereEqualTo("contaRemoteId", contaRemoteId).get(Source.SERVER));
        long agora = System.currentTimeMillis();
        for (DocumentSnapshot lembrete : relacionados.getDocuments()) {
            esperar(lembrete.getReference().update("deleted", true, "updatedAt", agora));
        }
    }

    private static void preencher(Conta conta, DocumentSnapshot doc, long atualizado) {
        conta.description = doc.getString("description");
        conta.amountInCents = valor(doc.getLong("amountInCents"));
        conta.dueDate = doc.getString("dueDate");
        conta.category = doc.getString("category");
        conta.type = doc.getString("type");
        conta.status = doc.getString("status");
        conta.ownerEmail = doc.getString("ownerEmail");
        conta.responsibleName = doc.getString("responsibleName");
        conta.groupId = doc.getString("groupId");
        conta.updatedAt = atualizado;
        conta.syncPending = false;
        conta.deleted = false;
    }

    private static Map<String, Object> paraMapa(Conta conta) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("description", conta.description);
        dados.put("amountInCents", conta.amountInCents);
        dados.put("dueDate", conta.dueDate);
        dados.put("category", conta.category);
        dados.put("type", conta.type);
        dados.put("status", conta.status);
        dados.put("ownerEmail", conta.ownerEmail);
        dados.put("responsibleName", conta.responsibleName);
        dados.put("groupId", conta.groupId);
        dados.put("updatedAt", conta.updatedAt);
        dados.put("deleted", conta.deleted);
        return dados;
    }

    private static long valor(Long numero) {
        return numero == null ? 0L : numero;
    }

    private static <T> T esperar(Task<T> tarefa) throws Exception {
        return Tasks.await(tarefa, TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
    }

    private static boolean temInternet(Context context) {
        ConnectivityManager manager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) return false;
        Network network = manager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private static void avisar(Retorno retorno, boolean sucesso, String mensagem) {
        if (retorno != null) MAIN.post(() -> retorno.aoConcluir(sucesso, mensagem));
    }
}
