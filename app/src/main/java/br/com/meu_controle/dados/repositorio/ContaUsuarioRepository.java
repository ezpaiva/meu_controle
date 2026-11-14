package br.com.meu_controle.dados.repositorio;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import br.com.meu_controle.dados.local.BancoDados;
import br.com.meu_controle.dados.local.SessaoUsuario;
import br.com.meu_controle.dados.modelo.GrupoFamiliar;
import br.com.meu_controle.dados.modelo.TipoPerfil;
import br.com.meu_controle.dados.modelo.Lembrete;
import br.com.meu_controle.services.NotificationScheduler;

public final class ContaUsuarioRepository {

    public interface Retorno {
        void onSuccess();
        void onError(Exception error);
    }

    private static final long TIMEOUT_SEGUNDOS = 25;
    private static final int TAMANHO_LOTE = 400;

    private final Context context;
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final SessaoUsuario sessao;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ContaUsuarioRepository(Context context) {
        this.context = context.getApplicationContext();
        sessao = new SessaoUsuario(this.context);
    }

    public void alterarEmail(String novoEmail, String senha, Retorno retorno) {
        FirebaseUser usuario = auth.getCurrentUser();
        if (usuario == null || usuario.getEmail() == null) {
            retorno.onError(new IllegalStateException("É necessário estar autenticado."));
            return;
        }
        String emailAnterior = usuario.getEmail();
        executor.execute(() -> {
            try {
                Tasks.await(usuario.reauthenticate(
                        EmailAuthProvider.getCredential(emailAnterior, senha)),
                        TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
                Tasks.await(usuario.updateEmail(normalizar(novoEmail)),
                        TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
                Exception falhaFirestore = null;
                try {
                    atualizarEmailLocal(emailAnterior, normalizar(novoEmail));
                } catch (Exception erro) {
                    falhaFirestore = erro;
                }
                try {
                    atualizarEmailRemoto(usuario.getUid(), emailAnterior,
                            normalizar(novoEmail));
                } catch (Exception erro) {
                    if (falhaFirestore == null) {
                        falhaFirestore = erro;
                    } else {
                        falhaFirestore.addSuppressed(erro);
                    }
                }
                Exception erroFirestoreFinal = falhaFirestore;
                String nome = sessao.obterNome();
                String grupoId = sessao.obterGrupoId();
                String grupoNome = sessao.obterGrupoNome();
                String emailContato = sessao.obterEmailContato();
                main.post(() -> {
                    sessao.salvarPerfil(nome, normalizar(novoEmail), sessao.obterTipo(),
                            grupoId, grupoNome, emailContato);
                    if (erroFirestoreFinal == null) retorno.onSuccess();
                    else retorno.onError(new IllegalStateException(
                            "O e-mail de autenticação mudou, mas nem todos os dados foram sincronizados.",
                            erroFirestoreFinal));
                });
            } catch (Exception error) {
                main.post(() -> retorno.onError(error));
            }
        });
    }

    public void excluirConta(String senha, Retorno retorno) {
        FirebaseUser usuario = auth.getCurrentUser();
        if (usuario == null || usuario.getEmail() == null) {
            retorno.onError(new IllegalStateException("É necessário estar autenticado."));
            return;
        }
        String email = usuario.getEmail();
        executor.execute(() -> {
            try {
                Tasks.await(usuario.reauthenticate(
                        EmailAuthProvider.getCredential(email, senha)),
                        TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
                String groupId = removerVinculoFamiliar(usuario.getUid());
                if (groupId != null) {
                    transferirDadosCompartilhados(groupId, email);
                }
                excluirSubcolecao(firestore.collection("usuarios")
                        .document(usuario.getUid()).collection("contas"));
                excluirConvites(usuario.getUid());
                limparDadosLocais(email, groupId == null
                        ? sessao.obterGrupoId() : groupId);
                Tasks.await(firestore.collection("usuarios").document(usuario.getUid()).delete(),
                        TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
                Tasks.await(usuario.delete(), TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
                main.post(() -> {
                    sessao.sair();
                    auth.signOut();
                    retorno.onSuccess();
                });
            } catch (Exception error) {
                main.post(() -> retorno.onError(error));
            }
        });
    }

    private void atualizarEmailRemoto(String uid, String emailAnterior,
                                      String novoEmail) throws Exception {
        DocumentReference userRef = firestore.collection("usuarios").document(uid);
        String groupId = Tasks.await(firestore.runTransaction(transaction -> {
            DocumentSnapshot usuario = transaction.get(userRef);
            if (!usuario.exists()) throw new IllegalStateException("Perfil remoto não encontrado.");
            DocumentReference groupRef = null;
            DocumentSnapshot group = null;
            String grupoDoPerfil = usuario.getString("grupoId");
            if (grupoDoPerfil != null && !grupoDoPerfil.trim().isEmpty()) {
                groupRef = firestore.collection("grupos_familiares").document(grupoDoPerfil);
                group = transaction.get(groupRef);
            }
            Map<String, Object> atualizacao = new HashMap<>();
            atualizacao.put("email", novoEmail);
            transaction.set(userRef, atualizacao, SetOptions.merge());
            if (group != null) {
                if (group.exists() && uid.equals(group.getString("criadorId"))) {
                    transaction.update(groupRef, "criadorEmail", novoEmail);
                }
            }
            return grupoDoPerfil;
        }), TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
        atualizarEmailConvites(uid, novoEmail);
        atualizarEmailNaColecao(userRef.collection("contas"), emailAnterior, novoEmail);
        if (groupId != null && !groupId.trim().isEmpty()) {
            DocumentReference groupRef = firestore.collection("grupos_familiares")
                    .document(groupId);
            atualizarEmailNaColecao(groupRef.collection("contas"), emailAnterior, novoEmail);
            atualizarEmailNaColecao(groupRef.collection("categorias"), emailAnterior, novoEmail);
            atualizarEmailNaColecao(groupRef.collection("lembretes"), emailAnterior, novoEmail);
        }
    }

    private void atualizarEmailLocal(String emailAnterior, String novoEmail) {
        BancoDados db = BancoDados.obterInstancia(context);
        db.contaDao().atualizarEmailDono(emailAnterior, novoEmail);
        db.categoriaDao().atualizarEmailDono(emailAnterior, novoEmail);
        db.lembreteDao().atualizarEmailDono(emailAnterior, novoEmail);
    }

    private void atualizarEmailNaColecao(CollectionReference colecao, String emailAnterior,
                                         String novoEmail) throws Exception {
        QuerySnapshot documentos = Tasks.await(colecao
                .whereEqualTo("ownerEmail", normalizar(emailAnterior))
                .get(Source.SERVER), TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
        List<DocumentSnapshot> encontrados = documentos.getDocuments();
        for (int inicio = 0; inicio < encontrados.size(); inicio += TAMANHO_LOTE) {
            WriteBatch batch = firestore.batch();
            int fim = Math.min(inicio + TAMANHO_LOTE, encontrados.size());
            for (int i = inicio; i < fim; i++) {
                batch.update(encontrados.get(i).getReference(), "ownerEmail", novoEmail,
                        "updatedAt", System.currentTimeMillis());
            }
            Tasks.await(batch.commit(), TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
        }
    }

    private void atualizarEmailConvites(String uid, String novoEmail) throws Exception {
        CollectionReference convites = firestore.collection("convites_familiares");
        QuerySnapshot recebidos = Tasks.await(
                convites.whereEqualTo("destinoUid", uid).get(Source.SERVER),
                TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
        QuerySnapshot enviados = Tasks.await(
                convites.whereEqualTo("origemUid", uid).get(Source.SERVER),
                TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
        List<DocumentSnapshot> documentos = new ArrayList<>();
        documentos.addAll(recebidos.getDocuments());
        documentos.addAll(enviados.getDocuments());
        for (int inicio = 0; inicio < documentos.size(); inicio += TAMANHO_LOTE) {
            WriteBatch batch = firestore.batch();
            int fim = Math.min(inicio + TAMANHO_LOTE, documentos.size());
            for (int i = inicio; i < fim; i++) {
                DocumentSnapshot convite = documentos.get(i);
                if (!"PENDENTE".equals(convite.getString("status"))) continue;
                if (uid.equals(convite.getString("destinoUid"))) {
                    batch.update(convite.getReference(), "emailDestino", novoEmail);
                }
                if (uid.equals(convite.getString("origemUid"))) {
                    batch.update(convite.getReference(), "emailOrigem", novoEmail);
                }
            }
            Tasks.await(batch.commit(), TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
        }
    }

    private String removerVinculoFamiliar(String uid) throws Exception {
        DocumentReference usuarioRef = firestore.collection("usuarios").document(uid);
        DocumentSnapshot perfil = Tasks.await(usuarioRef.get(Source.SERVER),
                TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
        if (!perfil.exists()) return null;
        String groupId = perfil.getString("grupoId");
        if (groupId == null || groupId.trim().isEmpty()) return null;
        DocumentReference grupoRef = firestore.collection("grupos_familiares").document(groupId);
        Boolean ultimoMembro = Tasks.await(firestore.runTransaction(transaction -> {
            DocumentSnapshot grupo = transaction.get(grupoRef);
            DocumentSnapshot usuario = transaction.get(usuarioRef);
            if (!grupo.exists() || !groupId.equals(usuario.getString("grupoId"))) {
                throw new IllegalStateException("A associação familiar está inconsistente.");
            }
            List<String> membros = membrosDe(grupo);
            membros.remove(uid);
            boolean eraCriador = uid.equals(grupo.getString("criadorId"));
            String novoCriadorId = null;
            DocumentSnapshot novoCriador = null;
            if (eraCriador && !membros.isEmpty()) {
                novoCriadorId = membros.get(0);
                novoCriador = transaction.get(
                        firestore.collection("usuarios").document(novoCriadorId));
                if (!novoCriador.exists()
                        || !groupId.equals(novoCriador.getString("grupoId"))) {
                    throw new IllegalStateException("Não foi possível transferir o grupo familiar.");
                }
            }
            if (membros.isEmpty()) {
                transaction.update(grupoRef, "membros", membros, "ativo", false);
            } else {
                Map<String, Object> atualizacao = new HashMap<>();
                atualizacao.put("membros", membros);
                if (eraCriador) {
                    atualizacao.put("criadorId", novoCriadorId);
                    atualizacao.put("criadorEmail", novoCriador.getString("email"));
                }
                transaction.update(grupoRef, atualizacao);
            }
            return membros.isEmpty();
        }), TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(ultimoMembro)) {
            excluirSubcolecao(grupoRef.collection("contas"));
            excluirSubcolecao(grupoRef.collection("categorias"));
            excluirSubcolecao(grupoRef.collection("lembretes"));
            cancelarConvitesDoGrupo(groupId);
            Tasks.await(grupoRef.delete(), TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
        }
        return groupId;
    }

    private void cancelarConvitesDoGrupo(String groupId) throws Exception {
        QuerySnapshot encontrados = Tasks.await(firestore.collection("convites_familiares")
                .whereEqualTo("grupoId", groupId).get(Source.SERVER),
                TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
        WriteBatch batch = firestore.batch();
        boolean alterou = false;
        for (DocumentSnapshot convite : encontrados.getDocuments()) {
            if ("PENDENTE".equals(convite.getString("status"))) {
                batch.update(convite.getReference(), "status", "CANCELADO");
                alterou = true;
            }
        }
        if (alterou) Tasks.await(batch.commit(), TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
    }

    private void transferirDadosCompartilhados(String groupId, String emailAnterior)
            throws Exception {
        DocumentSnapshot grupo = Tasks.await(firestore.collection("grupos_familiares")
                .document(groupId).get(Source.SERVER), TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
        if (!grupo.exists() || !Boolean.TRUE.equals(grupo.getBoolean("ativo"))) return;
        String emailResponsavel = grupo.getString("criadorEmail");
        if (emailResponsavel == null || emailResponsavel.trim().isEmpty()) {
            String uidResponsavel = grupo.getString("criadorId");
            if (uidResponsavel != null) {
                DocumentSnapshot responsavel = Tasks.await(firestore.collection("usuarios")
                        .document(uidResponsavel).get(Source.SERVER),
                        TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
                emailResponsavel = responsavel.getString("email");
            }
        }
        if (emailResponsavel == null || emailResponsavel.trim().isEmpty()) return;
        DocumentReference groupRef = firestore.collection("grupos_familiares")
                .document(groupId);
        atualizarEmailNaColecao(groupRef.collection("contas"), emailAnterior, emailResponsavel);
        atualizarEmailNaColecao(groupRef.collection("categorias"), emailAnterior, emailResponsavel);
        atualizarEmailNaColecao(groupRef.collection("lembretes"), emailAnterior, emailResponsavel);
    }

    private void excluirConvites(String uid) throws Exception {
        CollectionReference convites = firestore.collection("convites_familiares");
        QuerySnapshot recebidos = Tasks.await(
                convites.whereEqualTo("destinoUid", uid).get(Source.SERVER),
                TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
        QuerySnapshot enviados = Tasks.await(
                convites.whereEqualTo("origemUid", uid).get(Source.SERVER),
                TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
        List<DocumentSnapshot> documentos = new ArrayList<>();
        documentos.addAll(recebidos.getDocuments());
        documentos.addAll(enviados.getDocuments());
        excluirDocumentos(documentos);
    }

    private void excluirSubcolecao(CollectionReference colecao) throws Exception {
        while (true) {
            QuerySnapshot pagina = Tasks.await(colecao.limit(TAMANHO_LOTE).get(Source.SERVER),
                    TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
            if (pagina.isEmpty()) return;
            excluirDocumentos(pagina.getDocuments());
        }
    }

    private void excluirDocumentos(List<DocumentSnapshot> documentos) throws Exception {
        for (int inicio = 0; inicio < documentos.size(); inicio += TAMANHO_LOTE) {
            WriteBatch batch = firestore.batch();
            int fim = Math.min(inicio + TAMANHO_LOTE, documentos.size());
            for (int i = inicio; i < fim; i++) {
                batch.delete(documentos.get(i).getReference());
            }
            Tasks.await(batch.commit(), TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
        }
    }

    private void limparDadosLocais(String email, String groupId) {
        BancoDados db = BancoDados.obterInstancia(context);
        for (Lembrete lembrete : db.lembreteDao().listarTodos(email)) {
            new NotificationScheduler(context).cancelNotification(lembrete.id);
        }
        for (Lembrete lembrete : db.lembreteDao().listarTodosPorEscopo(email, groupId)) {
            new NotificationScheduler(context).cancelNotification(lembrete.id);
        }
        db.lembreteDao().excluirPessoaisDoDono(email);
        db.contaDao().excluirPessoaisDoDono(email);
        db.categoriaDao().excluirPessoaisDoDono(email);
        if (groupId != null) {
            db.lembreteDao().excluirCacheDoGrupo(groupId);
            db.contaDao().excluirCacheDoGrupo(groupId);
            db.categoriaDao().excluirCacheDoGrupo(groupId);
        }
    }

    private String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private List<String> membrosDe(DocumentSnapshot documento) {
        List<String> membros = new ArrayList<>();
        Object campo = documento.get("membros");
        if (campo instanceof List<?>) {
            for (Object id : (List<?>) campo) {
                if (id instanceof String) membros.add((String) id);
            }
        }
        return membros;
    }
}
