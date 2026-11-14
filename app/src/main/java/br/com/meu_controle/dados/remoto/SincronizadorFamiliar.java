package br.com.meu_controle.dados.remoto;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import br.com.meu_controle.dados.local.BancoDados;
import br.com.meu_controle.dados.local.CategoriaDao;
import br.com.meu_controle.dados.local.ContaDao;
import br.com.meu_controle.dados.local.LembreteDao;
import br.com.meu_controle.dados.modelo.Categoria;
import br.com.meu_controle.dados.modelo.Conta;
import br.com.meu_controle.dados.modelo.Lembrete;
import br.com.meu_controle.services.NotificationScheduler;

final class SincronizadorFamiliar {

    private static final long TIMEOUT_SEGUNDOS = 20;

    private SincronizadorFamiliar() {
    }

    static void sincronizar(Context context, String groupId) throws Exception {
        BancoDados db = BancoDados.obterInstancia(context);
        String base = "grupos_familiares";
        CollectionReference categorias = FirebaseFirestore.getInstance().collection(base)
                .document(groupId).collection("categorias");
        CollectionReference lembretes = FirebaseFirestore.getInstance().collection(base)
                .document(groupId).collection("lembretes");
        sincronizarCategorias(db.categoriaDao(), categorias, groupId);
        sincronizarLembretes(context, db.lembreteDao(), db.contaDao(), lembretes, groupId);
    }

    private static void sincronizarCategorias(CategoriaDao dao, CollectionReference colecao,
                                             String groupId) throws Exception {
        List<Categoria> pendentes = dao.getPendentesDoGrupo(groupId);
        for (Categoria categoria : pendentes) {
            if (categoria.remoteId == null || categoria.remoteId.trim().isEmpty()) {
                categoria.remoteId = UUID.randomUUID().toString();
                dao.update(categoria);
            }
            esperar(colecao.document(categoria.remoteId).set(mapa(categoria)));
            if (categoria.deleted) dao.deleteById(categoria.id);
            else dao.marcarSincronizada(categoria.id, categoria.updatedAt);
        }

        QuerySnapshot resultado = esperar(colecao.get(Source.SERVER));
        for (DocumentSnapshot doc : resultado.getDocuments()) {
            boolean excluida = Boolean.TRUE.equals(doc.getBoolean("deleted"));
            long atualizado = valor(doc.getLong("updatedAt"));
            Categoria local = dao.getByRemoteIdAndGroup(doc.getId(), groupId);
            if (excluida) {
                if (local != null && atualizado >= local.updatedAt) dao.deleteById(local.id);
                continue;
            }
            if (local != null && atualizado <= local.updatedAt) continue;
            Categoria categoria = local == null
                    ? new Categoria(valor(doc.getString("nome"), ""),
                    valor(doc.getString("tipo"), ""), doc.getString("ownerEmail"))
                    : local;
            categoria.nome = valor(doc.getString("nome"), "");
            categoria.tipo = valor(doc.getString("tipo"), "");
            categoria.ownerEmail = doc.getString("ownerEmail");
            categoria.groupId = groupId;
            categoria.remoteId = doc.getId();
            categoria.isPadrao = false;
            categoria.updatedAt = atualizado;
            categoria.syncPending = false;
            categoria.deleted = false;
            if (local == null) dao.insert(categoria);
            else dao.update(categoria);
        }
    }

    private static void sincronizarLembretes(Context context, LembreteDao dao, ContaDao contaDao,
                                             CollectionReference colecao, String groupId)
            throws Exception {
        List<Lembrete> pendentes = dao.getPendentesDoGrupo(groupId);
        for (Lembrete lembrete : pendentes) {
            if (lembrete.remoteId == null || lembrete.remoteId.trim().isEmpty()) {
                lembrete.remoteId = UUID.randomUUID().toString();
                dao.atualizar(lembrete);
            }
            esperar(colecao.document(lembrete.remoteId).set(mapa(lembrete, contaDao)));
            if (lembrete.deleted) dao.deleteById(lembrete.id);
            else {
                lembrete.syncPending = false;
                dao.atualizar(lembrete);
            }
        }

        QuerySnapshot resultado = esperar(colecao.get(Source.SERVER));
        for (DocumentSnapshot doc : resultado.getDocuments()) {
            long atualizado = valor(doc.getLong("updatedAt"));
            Lembrete local = dao.getByRemoteIdAndGroup(doc.getId(), groupId);
            boolean excluido = Boolean.TRUE.equals(doc.getBoolean("deleted"));
            if (excluido) {
                if (local != null && atualizado >= local.updatedAt) {
                    new NotificationScheduler(context).cancelNotification(local.id);
                    dao.deleteById(local.id);
                }
                continue;
            }
            if (local != null && atualizado <= local.updatedAt) continue;
            long dataHora = valor(doc.getLong("dataHora"));
            String titulo = valor(doc.getString("titulo"), "");
            String mensagem = valor(doc.getString("mensagem"), "");
            boolean ativo = Boolean.TRUE.equals(doc.getBoolean("ativo"));
            boolean notificado = Boolean.TRUE.equals(doc.getBoolean("notificado"));
            String ownerEmail = doc.getString("ownerEmail");
            Lembrete lembrete = local == null
                    ? new Lembrete(titulo, mensagem, dataHora, ativo, null,
                    atualizado, notificado, ownerEmail)
                    : local;
            lembrete.titulo = titulo;
            lembrete.mensagem = mensagem;
            lembrete.dataHora = dataHora;
            lembrete.ativo = ativo;
            lembrete.notificado = notificado;
            lembrete.ownerEmail = ownerEmail;
            lembrete.groupId = groupId;
            lembrete.createdByName = doc.getString("createdByName");
            lembrete.contaRemoteId = doc.getString("contaRemoteId");
            lembrete.contaId = lembrete.contaRemoteId == null ? null
                    : contaId(contaDao, lembrete.contaRemoteId, groupId);
            lembrete.remoteId = doc.getId();
            lembrete.criadoEm = valor(doc.getLong("criadoEm"));
            java.time.ZonedDateTime horario = java.time.Instant.ofEpochMilli(dataHora)
                    .atZone(java.time.ZoneId.systemDefault());
            lembrete.data = horario.toLocalDate().toString();
            lembrete.hora = horario.toLocalTime().format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            lembrete.updatedAt = atualizado;
            lembrete.syncPending = false;
            lembrete.deleted = false;
            if (local == null) dao.inserir(lembrete);
            else dao.atualizar(lembrete);
            new NotificationScheduler(context).scheduleNotification(lembrete);
        }
    }

    private static Long contaId(ContaDao dao, String remoteId, String groupId) {
        Conta conta = dao.getByRemoteIdAndGroup(remoteId, groupId);
        return conta == null ? null : conta.id;
    }

    private static Map<String, Object> mapa(Categoria categoria) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", categoria.nome);
        dados.put("tipo", categoria.tipo);
        dados.put("ownerEmail", categoria.ownerEmail);
        dados.put("groupId", categoria.groupId);
        dados.put("updatedAt", categoria.updatedAt);
        dados.put("deleted", categoria.deleted);
        return dados;
    }

    private static Map<String, Object> mapa(Lembrete lembrete, ContaDao contaDao) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("titulo", lembrete.titulo);
        dados.put("mensagem", lembrete.mensagem);
        dados.put("dataHora", lembrete.dataHora);
        dados.put("ativo", lembrete.ativo);
        dados.put("notificado", lembrete.notificado);
        dados.put("ownerEmail", lembrete.ownerEmail);
        dados.put("groupId", lembrete.groupId);
        dados.put("createdByName", lembrete.createdByName);
        dados.put("criadoEm", lembrete.criadoEm);
        dados.put("updatedAt", lembrete.updatedAt);
        dados.put("deleted", lembrete.deleted);
        String contaRemoteId = lembrete.contaRemoteId;
        if (contaRemoteId == null && lembrete.contaId != null) {
            Conta conta = contaDao.getById(lembrete.contaId);
            if (conta != null) contaRemoteId = conta.remoteId;
        }
        dados.put("contaRemoteId", contaRemoteId);
        return dados;
    }

    private static long valor(Long numero) {
        return numero == null ? 0L : numero;
    }

    private static String valor(String texto, String alternativa) {
        return texto == null ? alternativa : texto;
    }

    private static <T> T esperar(Task<T> tarefa) throws Exception {
        return Tasks.await(tarefa, TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
    }
}
