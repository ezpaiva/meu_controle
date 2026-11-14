package br.com.meu_controle.dados.repositorio;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.com.meu_controle.dados.local.BancoDados;
import br.com.meu_controle.dados.local.SessaoUsuario;
import br.com.meu_controle.dados.modelo.GrupoFamiliar;
import br.com.meu_controle.dados.modelo.TipoPerfil;
import br.com.meu_controle.dados.modelo.Usuario;
import br.com.meu_controle.dados.modelo.ConviteFamiliar;
import br.com.meu_controle.dados.modelo.Conta;
import br.com.meu_controle.services.NotificationScheduler;

public class GrupoFamiliarRepository {

    public interface RetornoConvites {
        void onSuccess(List<ConviteFamiliar> convites);
        void onError(Exception error);
    }

    public interface RetornoGrupo {
        void onSuccess(GrupoFamiliar grupo, List<Usuario> membros);
        void onError(Exception error);
    }

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final SessaoUsuario sessao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Context context;

    public GrupoFamiliarRepository(Context context) {
        this.context = context.getApplicationContext();
        sessao = new SessaoUsuario(this.context);
    }

    public void carregarGrupo(RetornoGrupo retorno) {
        FirebaseUser atual = usuarioAtual(retorno);
        if (atual == null) return;
        DocumentReference usuarioRef = firestore.collection("usuarios").document(atual.getUid());
        usuarioRef.get().addOnSuccessListener(documento -> {
            if (!documento.exists()) {
                retorno.onError(new IllegalStateException("Perfil do usuário não existe no Firestore."));
                return;
            }
            String nome = valor(documento.getString("nome"), atual.getDisplayName());
            String email = valor(documento.getString("email"), atual.getEmail());
            String perfil = valor(documento.getString("perfil"),
                    valor(documento.getString("tipoPerfil"), TipoPerfil.INDIVIDUAL));
            String groupId = documento.getString("grupoId");
            sessao.salvarPerfil(nome, email, perfil, groupId, sessao.obterGrupoNome());
            if (groupId == null || groupId.trim().isEmpty()) {
                sessao.salvarGrupo(null, null);
                mainHandler.post(() -> retorno.onSuccess(null, new ArrayList<>()));
                return;
            }
            firestore.collection("grupos_familiares").document(groupId).get()
                    .addOnSuccessListener(grupoDoc -> {
                        if (!grupoDoc.exists() || !Boolean.TRUE.equals(grupoDoc.getBoolean("ativo"))) {
                            sessao.salvarPerfil(nome, email, TipoPerfil.INDIVIDUAL, null, null);
                            retorno.onError(new IllegalStateException(
                                    "O grupo familiar não existe ou está inativo."));
                            return;
                        }
                        GrupoFamiliar grupo = grupoDoc.toObject(GrupoFamiliar.class);
                        if (grupo == null) {
                            retorno.onError(new IllegalStateException(
                                    "Não foi possível ler o grupo familiar."));
                            return;
                        }
                        grupo.id = grupoDoc.getId();
                        sessao.salvarGrupo(groupId, grupo.nome);
                        List<String> ids = grupo.membros == null
                                ? new ArrayList<>() : new ArrayList<>(grupo.membros);
                        if (!ids.contains(atual.getUid())) ids.add(atual.getUid());
                        carregarMembros(ids, 0, new ArrayList<>(), grupo, atual.getUid(), retorno);
                    })
                    .addOnFailureListener(retorno::onError);
        }).addOnFailureListener(retorno::onError);
    }

    public void criarGrupo(String nome, Conta primeiraConta, RetornoGrupo retorno) {
        FirebaseUser atual = usuarioAtual(retorno);
        if (atual == null) return;
        String nomeNormalizado = nome == null ? "" : nome.trim();
        if (nomeNormalizado.isEmpty()) {
            retorno.onError(new IllegalArgumentException("Informe o nome do grupo."));
            return;
        }
        String groupId = UUID.randomUUID().toString();
        DocumentReference usuarioRef = firestore.collection("usuarios").document(atual.getUid());
        DocumentReference grupoRef = firestore.collection("grupos_familiares").document(groupId);
        if (primeiraConta.remoteId == null || primeiraConta.remoteId.trim().isEmpty()) {
            primeiraConta.remoteId = UUID.randomUUID().toString();
        }
        primeiraConta.groupId = groupId;
        primeiraConta.updatedAt = System.currentTimeMillis();
        primeiraConta.syncPending = true;
        DocumentReference contaRef = grupoRef.collection("contas")
                .document(primeiraConta.remoteId);
        Map<String, Object> grupoDados = new HashMap<>();
        grupoDados.put("id", groupId);
        grupoDados.put("nome", nomeNormalizado);
        grupoDados.put("criadorEmail", normalizar(atual.getEmail()));
        grupoDados.put("criadorId", atual.getUid());
        grupoDados.put("dataCriacao", System.currentTimeMillis());
        grupoDados.put("ativo", true);
        grupoDados.put("membros", java.util.Collections.singletonList(atual.getUid()));

        firestore.runTransaction(transaction -> {
            DocumentSnapshot usuario = transaction.get(usuarioRef);
            if (!usuario.exists()) {
                throw new IllegalStateException("Perfil do usuário não existe no Firestore.");
            }
            String perfil = valor(usuario.getString("perfil"),
                    usuario.getString("tipoPerfil"));
            if (!TipoPerfil.FAMILIAR.equals(perfil)) {
                throw new IllegalStateException("Somente perfis familiares podem criar um grupo.");
            }
            String grupoAtual = usuario.getString("grupoId");
            if (grupoAtual != null && !grupoAtual.trim().isEmpty()) {
                throw new IllegalStateException("Este usuário já pertence a um grupo familiar.");
            }
            transaction.set(grupoRef, grupoDados);
            Map<String, Object> atualizacao = new HashMap<>();
            atualizacao.put("grupoId", groupId);
            atualizacao.put("perfil", TipoPerfil.FAMILIAR);
            atualizacao.put("tipoPerfil", TipoPerfil.FAMILIAR);
            transaction.set(usuarioRef, atualizacao, SetOptions.merge());
            transaction.set(contaRef, mapaPrimeiraConta(primeiraConta));
            return null;
        }).addOnSuccessListener(transacaoConcluida -> {
            sessao.salvarPerfil(sessao.obterNome(), atual.getEmail(),
                    TipoPerfil.FAMILIAR, groupId, nomeNormalizado);
            carregarGrupo(retorno);
        }).addOnFailureListener(retorno::onError);
    }

    private Map<String, Object> mapaPrimeiraConta(Conta conta) {
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
        dados.put("deleted", false);
        return dados;
    }

    public void adicionarMembro(String email, RetornoGrupo retorno) {
        FirebaseUser atual = usuarioAtual(retorno);
        if (atual == null) return;
        String groupId = sessao.obterGrupoId();
        String emailNormalizado = normalizar(email);
        if (groupId == null || groupId.trim().isEmpty()) {
            retorno.onError(new IllegalStateException(
                    "O grupo será criado ao salvar a primeira movimentação familiar."));
            return;
        }
        if (emailNormalizado.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS
                .matcher(emailNormalizado).matches()) {
            retorno.onError(new IllegalArgumentException("Informe um e-mail válido."));
            return;
        }
        firestore.collection("usuarios").whereEqualTo("email", emailNormalizado).limit(1).get()
                .addOnSuccessListener(resultado -> {
                    if (resultado.isEmpty()) {
                        retorno.onError(new IllegalArgumentException(
                                "Não existe usuário cadastrado com esse e-mail."));
                        return;
                    }
                    DocumentSnapshot encontrado = resultado.getDocuments().get(0);
                    String membroId = encontrado.getId();
                    if (membroId.equals(atual.getUid())) {
                        retorno.onError(new IllegalArgumentException(
                                "Você já é membro deste grupo."));
                        return;
                    }
                    String conviteId = UUID.randomUUID().toString();
                    DocumentReference grupoRef = firestore.collection("grupos_familiares")
                            .document(groupId);
                    DocumentReference donoRef = firestore.collection("usuarios")
                            .document(atual.getUid());
                    DocumentReference membroRef = firestore.collection("usuarios").document(membroId);
                    DocumentReference conviteRef = firestore.collection("convites_familiares")
                            .document(conviteId);
                    firestore.runTransaction(transaction -> {
                        DocumentSnapshot grupoDoc = transaction.get(grupoRef);
                        DocumentSnapshot donoDoc = transaction.get(donoRef);
                        DocumentSnapshot membroDoc = transaction.get(membroRef);
                        if (!grupoDoc.exists() || !Boolean.TRUE.equals(grupoDoc.getBoolean("ativo"))) {
                            throw new IllegalStateException("O grupo familiar não existe ou está inativo.");
                        }
                        if (!donoRef.getId().equals(grupoDoc.getString("criadorId"))) {
                            throw new SecurityException("Somente o criador pode adicionar membros.");
                        }
                        if (!groupId.equals(donoDoc.getString("grupoId"))) {
                            throw new SecurityException("Sua associação ao grupo não está válida.");
                        }
                        if (!membroDoc.exists()) {
                            throw new IllegalArgumentException("O perfil do usuário não existe.");
                        }
                        String grupoMembro = membroDoc.getString("grupoId");
                        if (grupoMembro != null && !grupoMembro.trim().isEmpty()) {
                            throw new IllegalArgumentException(
                                    "Este usuário já está vinculado a um grupo familiar.");
                        }
                        Map<String, Object> convite = new HashMap<>();
                        convite.put("id", conviteId);
                        convite.put("grupoId", groupId);
                        convite.put("grupoNome", grupoDoc.getString("nome"));
                        convite.put("emailDestino", emailNormalizado);
                        convite.put("emailOrigem", normalizar(atual.getEmail()));
                        convite.put("origemUid", atual.getUid());
                        convite.put("destinoUid", membroId);
                        convite.put("status", "PENDENTE");
                        convite.put("dataCriacao", System.currentTimeMillis());
                        transaction.set(conviteRef, convite);
                        return null;
                    }).addOnSuccessListener(conviteCriado -> carregarGrupo(
                            new RetornoGrupo() {
                                @Override
                                public void onSuccess(GrupoFamiliar grupo,
                                                      List<Usuario> membros) {
                                    retorno.onSuccess(grupo, membros);
                                }

                                @Override
                                public void onError(Exception error) {
                                    retorno.onError(error);
                                }
                            }))
                            .addOnFailureListener(retorno::onError);
                }).addOnFailureListener(retorno::onError);
    }

    public void listarConvitesPendentes(RetornoConvites retorno) {
        FirebaseUser atual = usuarioAtualConvites(retorno);
        if (atual == null) return;
        String email = normalizar(atual.getEmail());
        firestore.collection("convites_familiares")
                .whereEqualTo("emailDestino", email)
                .get()
                .addOnSuccessListener(resultado -> {
                    List<ConviteFamiliar> pendentes = new ArrayList<>();
                    for (DocumentSnapshot documento : resultado.getDocuments()) {
                        ConviteFamiliar convite = documento.toObject(ConviteFamiliar.class);
                        if (convite != null && "PENDENTE".equals(convite.status)
                                && atual.getUid().equals(convite.destinoUid)) {
                            convite.id = documento.getId();
                            pendentes.add(convite);
                        }
                    }
                    retorno.onSuccess(pendentes);
                })
                .addOnFailureListener(retorno::onError);
    }

    public void aceitarConvite(ConviteFamiliar convite, RetornoGrupo retorno) {
        FirebaseUser atual = usuarioAtual(retorno);
        if (atual == null) return;
        DocumentReference conviteRef = firestore.collection("convites_familiares")
                .document(convite.id);
        DocumentReference usuarioRef = firestore.collection("usuarios")
                .document(atual.getUid());
        DocumentReference grupoRef = firestore.collection("grupos_familiares")
                .document(convite.grupoId);
        firestore.runTransaction(transaction -> {
            DocumentSnapshot conviteDoc = transaction.get(conviteRef);
            DocumentSnapshot usuarioDoc = transaction.get(usuarioRef);
            DocumentSnapshot grupoDoc = transaction.get(grupoRef);
            if (!conviteDoc.exists() || !"PENDENTE".equals(conviteDoc.getString("status"))) {
                throw new IllegalStateException("Este convite não está mais pendente.");
            }
            if (!atual.getUid().equals(conviteDoc.getString("destinoUid"))
                    || !normalizar(atual.getEmail()).equals(
                    normalizar(conviteDoc.getString("emailDestino")))) {
                throw new SecurityException("O convite foi enviado para outro e-mail.");
            }
            if (!usuarioDoc.exists() || !grupoDoc.exists()
                    || !Boolean.TRUE.equals(grupoDoc.getBoolean("ativo"))) {
                throw new IllegalStateException("O perfil ou grupo não está disponível.");
            }
            String grupoAtual = usuarioDoc.getString("grupoId");
            if (grupoAtual != null && !grupoAtual.trim().isEmpty()) {
                throw new IllegalStateException("Saia do grupo atual antes de aceitar outro convite.");
            }
            List<String> membros = membrosDe(grupoDoc);
            if (!membros.contains(atual.getUid())) membros.add(atual.getUid());
            transaction.update(grupoRef, "membros", membros);
            Map<String, Object> perfil = new HashMap<>();
            perfil.put("grupoId", convite.grupoId);
            perfil.put("perfil", TipoPerfil.FAMILIAR);
            perfil.put("tipoPerfil", TipoPerfil.FAMILIAR);
            transaction.set(usuarioRef, perfil, SetOptions.merge());
            transaction.update(conviteRef, "status", "ACEITO",
                    "dataResposta", System.currentTimeMillis());
            return null;
        }).addOnSuccessListener(transacaoConcluida -> {
            sessao.salvarPerfil(sessao.obterNome(), atual.getEmail(),
                    TipoPerfil.FAMILIAR, convite.grupoId, convite.grupoNome);
            carregarGrupo(retorno);
        }).addOnFailureListener(retorno::onError);
    }

    public void recusarConvite(ConviteFamiliar convite, Runnable sucesso,
                               java.util.function.Consumer<Exception> falha) {
        FirebaseUser atual = FirebaseAuth.getInstance().getCurrentUser();
        if (atual == null) {
            falha.accept(new IllegalStateException("É necessário estar autenticado."));
            return;
        }
        DocumentReference referencia = firestore.collection("convites_familiares")
                .document(convite.id);
        firestore.runTransaction(transaction -> {
            DocumentSnapshot documento = transaction.get(referencia);
            if (!documento.exists() || !"PENDENTE".equals(documento.getString("status"))) {
                throw new IllegalStateException("Este convite não está mais pendente.");
            }
            if (!atual.getUid().equals(documento.getString("destinoUid"))
                    || !normalizar(atual.getEmail()).equals(
                    normalizar(documento.getString("emailDestino")))) {
                throw new SecurityException("O convite foi enviado para outro e-mail.");
            }
            transaction.update(referencia, "status", "RECUSADO",
                    "dataResposta", System.currentTimeMillis());
            return null;
        }).addOnSuccessListener(ignorado -> sucesso.run())
                .addOnFailureListener(falha::accept);
    }

    public void sairDoGrupo(RetornoGrupo retorno) {
        FirebaseUser atual = usuarioAtual(retorno);
        if (atual == null) return;
        String groupId = sessao.obterGrupoId();
        if (groupId == null || groupId.trim().isEmpty()) {
            retorno.onError(new IllegalStateException("Você não pertence a um grupo familiar."));
            return;
        }
        DocumentReference grupoRef = firestore.collection("grupos_familiares").document(groupId);
        DocumentReference usuarioRef = firestore.collection("usuarios").document(atual.getUid());
        firestore.runTransaction(transaction -> {
            DocumentSnapshot grupoDoc = transaction.get(grupoRef);
            DocumentSnapshot usuarioDoc = transaction.get(usuarioRef);
            if (!grupoDoc.exists() || !groupId.equals(usuarioDoc.getString("grupoId"))) {
                throw new IllegalStateException("A associação familiar não está mais válida.");
            }
            List<String> membros = membrosDe(grupoDoc);
            membros.remove(atual.getUid());
            boolean eraCriador = atual.getUid().equals(grupoDoc.getString("criadorId"));
            String novoCriadorId = null;
            DocumentSnapshot novoCriador = null;
            if (eraCriador && !membros.isEmpty()) {
                novoCriadorId = membros.get(0);
                novoCriador = transaction.get(
                        firestore.collection("usuarios").document(novoCriadorId));
                if (!novoCriador.exists() || !groupId.equals(novoCriador.getString("grupoId"))) {
                    throw new IllegalStateException("Não foi possível transferir a administração do grupo.");
                }
            }
            if (membros.isEmpty()) {
                transaction.update(grupoRef, "membros", membros, "ativo", false);
            } else {
                Map<String, Object> atualizacaoGrupo = new HashMap<>();
                atualizacaoGrupo.put("membros", membros);
                if (eraCriador) {
                    atualizacaoGrupo.put("criadorId", novoCriadorId);
                    atualizacaoGrupo.put("criadorEmail", novoCriador.getString("email"));
                }
                transaction.update(grupoRef, atualizacaoGrupo);
                if (eraCriador) {
                    transaction.update(firestore.collection("usuarios").document(novoCriadorId),
                            "criadorGrupo", true);
                }
            }
            transaction.update(usuarioRef, "grupoId", null,
                    "perfil", TipoPerfil.INDIVIDUAL, "tipoPerfil", TipoPerfil.INDIVIDUAL);
            return membros.isEmpty();
        }).addOnSuccessListener(saidaConcluida -> {
            sessao.salvarPerfil(sessao.obterNome(), atual.getEmail(),
                    TipoPerfil.INDIVIDUAL, null, null);
            limparCacheGrupo(groupId, erroCache -> {
                if (erroCache != null) {
                    retorno.onError(erroCache);
                    return;
                }
                if (Boolean.TRUE.equals(saidaConcluida)) {
                    excluirColecaoGrupo(groupId, erroExclusao -> {
                        if (erroExclusao != null) {
                            retorno.onError(new IllegalStateException(
                                    "Você saiu do grupo, mas não foi possível limpar os dados remotos.",
                                    erroExclusao));
                        } else {
                            grupoRef.delete()
                                    .addOnSuccessListener(grupoExcluido ->
                                            retorno.onSuccess(null, new ArrayList<>()))
                                    .addOnFailureListener(retorno::onError);
                        }
                    });
                } else {
                    retorno.onSuccess(null, new ArrayList<>());
                }
            });
        }).addOnFailureListener(retorno::onError);
    }

    private void limparCacheGrupo(String groupId,
                                  java.util.function.Consumer<Exception> concluido) {
        executor.execute(() -> {
            try {
                BancoDados db = BancoDados.obterInstancia(context);
                for (br.com.meu_controle.dados.modelo.Lembrete lembrete
                        : db.lembreteDao().getDoGrupo(groupId)) {
                    new NotificationScheduler(context).cancelNotification(lembrete.id);
                }
                db.lembreteDao().excluirCacheDoGrupo(groupId);
                db.contaDao().excluirCacheDoGrupo(groupId);
                db.categoriaDao().excluirCacheDoGrupo(groupId);
                mainHandler.post(() -> concluido.accept(null));
            } catch (Exception error) {
                mainHandler.post(() -> concluido.accept(new IllegalStateException(
                        "Não foi possível limpar os dados locais do grupo.", error)));
            }
        });
    }

    private void excluirColecaoGrupo(String groupId,
                                     java.util.function.Consumer<Exception> falha) {
        excluirColecaoEmLotes(firestore.collection("grupos_familiares").document(groupId)
                .collection("contas"), error -> {
            if (error != null) { falha.accept(error); return; }
            excluirColecaoEmLotes(firestore.collection("grupos_familiares").document(groupId)
                    .collection("categorias"), errorCategorias -> {
                if (errorCategorias != null) { falha.accept(errorCategorias); return; }
                excluirColecaoEmLotes(firestore.collection("grupos_familiares").document(groupId)
                        .collection("lembretes"), erroLembretes -> {
                    if (erroLembretes != null) { falha.accept(erroLembretes); return; }
                    cancelarConvitesDoGrupo(groupId, falha);
                });
            });
        });
    }

    private void cancelarConvitesDoGrupo(String groupId,
                                     java.util.function.Consumer<Exception> concluido) {
        firestore.collection("convites_familiares").whereEqualTo("grupoId", groupId).get()
            .addOnSuccessListener(resultado -> {
                WriteBatch batch = firestore.batch();
                boolean alterou = false;
                for (DocumentSnapshot convite : resultado.getDocuments()) {
                    if ("PENDENTE".equals(convite.getString("status"))) {
                        batch.update(convite.getReference(), "status", "CANCELADO");
                        alterou = true;
                    }
                }
                if (!alterou) {
                    concluido.accept(null);
                } else {
                    batch.commit().addOnSuccessListener(ignorado -> concluido.accept(null))
                            .addOnFailureListener(concluido::accept);
                }
            }).addOnFailureListener(concluido::accept);
    }

    private void excluirColecaoEmLotes(com.google.firebase.firestore.CollectionReference colecao,
                                       java.util.function.Consumer<Exception> concluido) {
        colecao.limit(400).get().addOnSuccessListener(resultado -> {
            if (resultado.isEmpty()) { concluido.accept(null); return; }
            WriteBatch batch = firestore.batch();
            for (DocumentSnapshot documento : resultado.getDocuments()) {
                batch.delete(documento.getReference());
            }
            batch.commit().addOnSuccessListener(ignorado -> excluirColecaoEmLotes(colecao, concluido))
                    .addOnFailureListener(concluido::accept);
        }).addOnFailureListener(concluido::accept);
    }

    private FirebaseUser usuarioAtualConvites(RetornoConvites retorno) {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) retorno.onError(
                new IllegalStateException("É necessário estar autenticado."));
        return usuario;
    }

    public void removerMembro(String membroId, RetornoGrupo retorno) {
        FirebaseUser atual = usuarioAtual(retorno);
        if (atual == null) return;
        String groupId = sessao.obterGrupoId();
        if (groupId == null || groupId.trim().isEmpty()) {
            retorno.onError(new IllegalStateException("Você não pertence a um grupo familiar."));
            return;
        }
        if (atual.getUid().equals(membroId)) {
            retorno.onError(new IllegalArgumentException("O criador não pode remover a si mesmo."));
            return;
        }
        DocumentReference grupoRef = firestore.collection("grupos_familiares").document(groupId);
        DocumentReference donoRef = firestore.collection("usuarios").document(atual.getUid());
        DocumentReference membroRef = firestore.collection("usuarios").document(membroId);
        firestore.runTransaction(transaction -> {
            DocumentSnapshot grupoDoc = transaction.get(grupoRef);
            DocumentSnapshot donoDoc = transaction.get(donoRef);
            DocumentSnapshot membroDoc = transaction.get(membroRef);
            if (!grupoDoc.exists()) {
                throw new IllegalStateException("O grupo familiar não existe.");
            }
            if (!atual.getUid().equals(grupoDoc.getString("criadorId"))) {
                throw new SecurityException("Somente o criador pode remover membros.");
            }
            if (!membroDoc.exists() || !groupId.equals(membroDoc.getString("grupoId"))) {
                throw new IllegalArgumentException("O usuário não pertence a este grupo.");
            }
            if (!groupId.equals(donoDoc.getString("grupoId"))) {
                throw new SecurityException("Sua associação ao grupo não está válida.");
            }
            transaction.update(grupoRef, "membros", FieldValue.arrayRemove(membroId));
            Map<String, Object> atualizacao = new HashMap<>();
            atualizacao.put("grupoId", null);
            atualizacao.put("perfil", TipoPerfil.INDIVIDUAL);
            atualizacao.put("tipoPerfil", TipoPerfil.INDIVIDUAL);
            transaction.set(membroRef, atualizacao, SetOptions.merge());
            return null;
        }).addOnSuccessListener(ignorado -> carregarGrupo(retorno))
                .addOnFailureListener(retorno::onError);
    }

    private void carregarMembros(List<String> ids, int indice, List<Usuario> membros,
                                 GrupoFamiliar grupo, String uidAtual, RetornoGrupo retorno) {
        if (indice >= ids.size()) {
            List<String> emails = new ArrayList<>();
            for (Usuario usuario : membros) {
                if (usuario.email != null) emails.add(usuario.email);
            }
            sessao.salvarMembros(emails);
            mainHandler.post(() -> retorno.onSuccess(grupo, membros));
            return;
        }
        String id = ids.get(indice);
        firestore.collection("usuarios").document(id).get()
                .addOnSuccessListener(documento -> {
                    if (documento.exists()) {
                        Usuario usuario = new Usuario();
                        usuario.id = documento.getId();
                        usuario.nome = valor(documento.getString("nome"), documento.getString("email"));
                        usuario.email = documento.getString("email");
                        usuario.perfil = valor(documento.getString("perfil"),
                                documento.getString("tipoPerfil"));
                        usuario.grupoId = documento.getString("grupoId");
                        usuario.criador = id.equals(grupo.criadorId);
                        if (grupo.criadorId == null && normalizar(usuario.email)
                                .equals(normalizar(grupo.criadorEmail))) {
                            usuario.criador = true;
                        }
                        if (grupo.id.equals(usuario.grupoId) || id.equals(uidAtual)) {
                            membros.add(usuario);
                        }
                    }
                    carregarMembros(ids, indice + 1, membros, grupo, uidAtual, retorno);
                }).addOnFailureListener(retorno::onError);
    }

    private FirebaseUser usuarioAtual(RetornoGrupo retorno) {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            retorno.onError(new IllegalStateException("É necessário estar autenticado."));
        }
        return usuario;
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

    private String valor(String valor, String alternativa) {
        return valor == null || valor.trim().isEmpty() ? alternativa : valor;
    }
}
