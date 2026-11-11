package br.com.meu_controle.telas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import br.com.meu_controle.R;
import br.com.meu_controle.dados.modelo.LembreteComConta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LembreteAdapter extends RecyclerView.Adapter<LembreteAdapter.LembreteViewHolder> {

    public interface Acoes {
        void editar(LembreteComConta lembrete);
        void excluir(LembreteComConta lembrete);
        void alterarAtivo(LembreteComConta lembrete, boolean ativo);
    }

    private final Acoes acoes;
    private final List<LembreteComConta> lembretes = new ArrayList<>();

    public LembreteAdapter(Acoes acoes) {
        this.acoes = acoes;
    }

    public void definirLembretes(List<LembreteComConta> novosLembretes) {
        lembretes.clear();
        if (novosLembretes != null) {
            lembretes.addAll(novosLembretes);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LembreteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lembrete, parent, false);
        return new LembreteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LembreteViewHolder holder, int position) {
        LembreteComConta item = lembretes.get(position);
        br.com.meu_controle.dados.modelo.Lembrete lembrete = item.lembrete;
        holder.titulo.setText(lembrete.titulo);
        holder.mensagem.setText(lembrete.mensagem);
        if (item.contaDescricao == null) {
            holder.conta.setText(R.string.reminder_account_none);
        } else {
            holder.conta.setText(holder.itemView.getContext().getString(
                    R.string.reminder_account_label, item.contaDescricao));
        }
        holder.dataHora.setText(new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm",
                new Locale("pt", "BR")).format(new Date(lembrete.dataHora)));
        holder.estado.setText(lembrete.notificado ? R.string.reminder_status_completed
                : lembrete.ativo ? R.string.reminder_status_active
                : R.string.reminder_status_disabled);
        holder.switchAtivo.setOnCheckedChangeListener(null);
        holder.switchAtivo.setChecked(lembrete.ativo);
        holder.switchAtivo.setEnabled(!lembrete.notificado);
        holder.switchAtivo.setOnCheckedChangeListener((button, checked) ->
                acoes.alterarAtivo(item, checked));
        holder.editar.setContentDescription(holder.itemView.getContext()
                .getString(R.string.reminder_edit_accessibility, lembrete.titulo));
        holder.excluir.setContentDescription(holder.itemView.getContext()
                .getString(R.string.reminder_delete_accessibility, lembrete.titulo));
        holder.editar.setOnClickListener(view -> acoes.editar(item));
        holder.excluir.setOnClickListener(view -> acoes.excluir(item));
    }

    @Override
    public int getItemCount() {
        return lembretes.size();
    }

    static class LembreteViewHolder extends RecyclerView.ViewHolder {
        final TextView titulo;
        final TextView mensagem;
        final TextView dataHora;
        final TextView estado;
        final TextView conta;
        final SwitchMaterial switchAtivo;
        final MaterialButton editar;
        final MaterialButton excluir;

        LembreteViewHolder(@NonNull View itemView) {
            super(itemView);
            titulo = itemView.findViewById(R.id.reminder_title);
            mensagem = itemView.findViewById(R.id.reminder_message);
            dataHora = itemView.findViewById(R.id.reminder_date_time);
            estado = itemView.findViewById(R.id.reminder_status);
            conta = itemView.findViewById(R.id.reminder_account);
            switchAtivo = itemView.findViewById(R.id.reminder_active_switch);
            editar = itemView.findViewById(R.id.reminder_edit);
            excluir = itemView.findViewById(R.id.reminder_delete);
        }
    }
}
