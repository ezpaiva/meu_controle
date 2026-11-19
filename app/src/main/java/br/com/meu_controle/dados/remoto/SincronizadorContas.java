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
import com.google.firebase.FirebaseApp;
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

    /**
     * Resultado de uma tentativa de sincronização.
     *
     * - onSuccess: sincronizou de fato, ou estado normal que NÃO é erro
     *              (offline, sem sessão). A UI não deve exibir aviso.
     * - onError:   falha transitória (timeout, rede ruim). Logar, não travar tela.
     * - aoBloquearAcesso: erro de contrato (grupo removido). A UI DEVE avisar
     *              o usuário e, se aplicável, redirecionar para o perfil individual.
     */
    public interface Retorno {
        void aoConcluir(boolean sucesso, String mensagem);

        /**
         * Erro de contrato que exige feedback ao usuário.
         * A implementação padrão mantém compatibilidade com chamadores antigos.
         */
        default void aoBloquearAcesso(String motivo) {
            aoConcluir(false, motivo);
        }
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
        if (context == null) {
            // Sem contexto de aplicação: nada a fazer, mas NÃO é erro do usuário.
            avisarEstadoNormal(retorno, "Contexto indisponível.");
            return;
        }

        // Firebase pode não estar configurado (ex.: dev sem google-services.json).
        // Não é erro do usuário — loga e retorna sucesso silencioso.
        if (FirebaseApp.getApps(context).isEmpty()) {
            Log.w(TAG, "FirebaseApp não inicializado; sincronização ignorada.");
            avisarEstadoNormal(retorno, "Sincronização indisponível.");
            return;
        }

        FirebaseUser usuario;
        try {
            usuario = FirebaseAuth.getInstance().getCurrentUser();
        } catch (IllegalStateException e) {
            Log.w(TAG, "FirebaseAuth indisponível.", e);
            avisarEstadoNormal(retorno, "Sincronização indisponível.");
            return;
        }

        if (usuario == null) {
            // Usuário deslogado é estado normal; o app funciona 100% local.
            avisarEstadoNormal(retorno, "Nenhum usuário autenticado.");
            return;
        }

        if (!temInternet(context)) {
            // RNF03: offline NÃO é erro.
            avisarEstadoNormal(retorno,
                    "Sem conexão. As alterações continuam salvas localmente.");
            return;
        }

        SessaoUsuario sessao = new SessaoUsuario(context);
        String email = sessao.obterEmail();
        String groupId = sessao.obterGrupoId();
        if (email == null || email.trim().isEmpty()) {
            avisarEstadoNormal(retorno, "Sessão sem e-mail.");
            return;
        }

        final String emailFinal = email;
        final String groupIdFinal = groupId;
        final FirebaseUser usuarioFinal = usuario;

        EXECUTOR.execute(() -> {
            try {
                BancoDados db = BancoDados.obterInstancia(context);
                ContaDao dao = db.contaDao();
                if (groupIdFinal == null || groupIdFinal.trim().isEmpty()) {
                    CollectionReference contas = FirebaseFirestore.getInstance()
                            .collection("usuarios").document(usuarioFinal.getUid())
                            .collection("contas");
                    dao.adotarContasSemDono(emailFinal);
                    baixarPessoais(dao, contas, emailFinal);
                    enviarPessoais(dao, contas, emailFinal);
                } else {
                    validarAssociacao(context, db, usuarioFinal, sessao, emailFinal, groupIdFinal);
                    CollectionReference contas = FirebaseFirestore.getInstance()
                            .collection("grupos_familiares").document(groupIdFinal)
                            .collection("contas");
                    baixarDoGrupo(dao, contas, emailFinal, groupIdFinal);
                    enviarDoGrupo(dao, contas, groupIdFinal);
                    SincronizadorFamiliar.sincronizar(context, groupIdFinal);
                }
                avisarSucesso(retorno, "Sincronizado.");
            } catch (SecurityException e) {
                // Contrato violado (ex.: removido do grupo). PRECISA chegar ao usuário.
                Log.w(TAG, "Acesso bloqueado durante sincronização.", e);
                avisarBloqueio(retorno, e.getMessage() == null
                        ? "Seu acesso ao grupo familiar foi removido."
                        : e.getMessage());
            } catch (Exception e) {
                // Timeout, permission denied, rede ruim, etc.
                // Falha transitória — o dado local está intacto. Loga e segue.
                Log.w(TAG, "Falha transitória na sincronização remota: " + e.getMessage()
                        + " (dados locais preservados).", e);
                avisarFalhaTransitoria(retorno, "Falha ao sincronizar: " + e.getMessage());
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

    // ---- despacho de callbacks no main thread, com semântica explícita ----

    private static void avisarEstadoNormal(Retorno retorno, String mensagem) {
        if (retorno == null) return;
        MAIN.post(() -> retorno.aoConcluir(true, mensagem));
    }

    private static void avisarSucesso(Retorno retorno, String mensagem) {
        if (retorno == null) return;
        MAIN.post(() -> retorno.aoConcluir(true, mensagem));
    }

    private static void avisarFalhaTransitoria(Retorno retorno, String mensagem) {
        if (retorno == null) return;
        MAIN.post(() -> retorno.aoConcluir(false, mensagem));
    }

    private static void avisarBloqueio(Retorno retorno, String motivo) {
        if (retorno == null) return;
        MAIN.post(() -> retorno.aoBloquearAcesso(motivo));
    }
}