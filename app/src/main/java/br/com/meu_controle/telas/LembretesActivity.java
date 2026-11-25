package br.com.meu_controle.telas;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;

import br.com.meu_controle.R;
import br.com.meu_controle.dados.local.SessaoUsuario;
import br.com.meu_controle.dados.modelo.Lembrete;
import br.com.meu_controle.dados.modelo.LembreteComConta;
import br.com.meu_controle.dados.repositorio.RetornoRepositorio;
import br.com.meu_controle.viewmodels.LembreteViewModel;
import br.com.meu_controle.utilitarios.BuscaGlobal;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LembretesActivity extends AppCompatActivity implements LembreteAdapter.Acoes {

    private static final String TAG = "LEMBRETES";

    private LembreteViewModel viewModel;
    private LembreteAdapter adapter;
    private TextView mensagemVazia;
    private MaterialButton filtroAtivos;
    private MaterialButton filtroConcluidos;
    private String email;
    private String groupId;
    private boolean mostrarConcluidos;
    private List<LembreteComConta> todos = new ArrayList<>();
    private String buscaTexto = "";

    /** Snackbar ativo, para garantir que só exista um por vez e para dispensá-lo ao sair. */
    private Snackbar snackbarAtual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lembretes);
        // Mantém o conteúdo (e o botão Voltar) fora da barra de status e da barra de navegação
        View raiz = findViewById(R.id.reminders_root);
        int esquerda = raiz.getPaddingLeft();
        int topo = raiz.getPaddingTop();
        int direita = raiz.getPaddingRight();
        int base = raiz.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, windowInsets) -> {
            Insets barras = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(esquerda + barras.left, topo + barras.top,
                    direita + barras.right, base + barras.bottom);
            return windowInsets;
        });

        SessaoUsuario sessao = new SessaoUsuario(this);
        email = sessao.obterEmail();
        groupId = sessao.obterGrupoId();
        if (email == null) email = "";
        if (groupId == null) groupId = "";

        viewModel = new ViewModelProvider(this).get(LembreteViewModel.class);

        RecyclerView lista = findViewById(R.id.reminders_list);
        lista.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LembreteAdapter(this);
        lista.setAdapter(adapter);

        mensagemVazia = findViewById(R.id.reminders_empty);
        // Garante que a mensagem vazia nunca intercepte toques (mesmo que o layout a coloque por cima).
        mensagemVazia.setClickable(false);
        mensagemVazia.setFocusable(false);
        mensagemVazia.setFocusableInTouchMode(false);

        filtroAtivos = findViewById(R.id.reminders_filter_active);
        filtroConcluidos = findViewById(R.id.reminders_filter_completed);

        filtroAtivos.setOnClickListener(view -> {
            mostrarConcluidos = false;
            atualizarLista();
        });
        filtroConcluidos.setOnClickListener(view -> {
            mostrarConcluidos = true;
            atualizarLista();
        });
        com.google.android.material.textfield.TextInputEditText busca =
                findViewById(R.id.reminder_search_input);
        busca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                buscaTexto = s.toString();
                atualizarLista();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        FloatingActionButton adicionar = findViewById(R.id.reminders_add);
        adicionar.setContentDescription(getString(R.string.reminder_add_accessibility));
        adicionar.setOnClickListener(view -> abrirEditor(null));

        // Botão "voltar" da tela: passa por sairDaTela() para dispensar Snackbar antes.
        View botaoVoltar = findViewById(R.id.reminders_back);
        if (botaoVoltar != null) {
            botaoVoltar.setOnClickListener(view -> {
                Log.d(TAG, "Botão voltar da tela clicado.");
                sairDaTela();
            });
        } else {
            Log.w(TAG, "R.id.reminders_back não encontrado no layout.");
        }

        // Botão físico / gesto "voltar" também respeita sairDaTela().
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Log.d(TAG, "Back do sistema acionado.");
                sairDaTela();
            }
        });

        // Fonte de verdade: Room. Sempre.
        viewModel.observarLembretesComConta(email, groupId).observe(this, lembretes -> {
            todos = lembretes == null ? new ArrayList<>() : lembretes;
            atualizarLista();
        });

        // Sincronização best-effort (RNF03): só mostra Snackbar em erro de contrato.
        boolean podeSincronizar = !email.isEmpty() && !groupId.isEmpty();
        if (podeSincronizar) {
            viewModel.sincronizar(new RetornoRepositorio<Void>() {
                @Override
                public void onSuccess(Void result) {
                    // estado normal ou sucesso — silencioso.
                }

                @Override
                public void onError(Exception error) {
                    if (isFinishing() || isDestroyed()) return;

                    String msg = (error != null
                            && error.getMessage() != null
                            && !error.getMessage().isEmpty())
                            ? error.getMessage()
                            : getString(R.string.load_error);

                    mostrarSnackbar(msg);
                }
            });
        } else {
            Log.d(TAG, "Sincronização ignorada: email ou groupId ausente "
                    + "(email=" + email + ", groupId=" + groupId + ")");
        }
    }

    /**
     * Exibe um Snackbar único. Se já houver um na tela, é dispensado primeiro para
     * evitar fila e garantir que só uma instância bloqueie toques por vez.
     */
    private void mostrarSnackbar(String mensagem) {
        if (isFinishing() || isDestroyed()) return;

        if (snackbarAtual != null && snackbarAtual.isShown()) {
            snackbarAtual.dismiss();
        }

        View ancora = obterAncoraSnackbar();
        if (ancora == null) {
            Log.w(TAG, "Sem âncora válida para Snackbar; usando Toast.");
            Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show();
            return;
        }

        snackbarAtual = Snackbar.make(ancora, mensagem, Snackbar.LENGTH_LONG)
                .setAction(R.string.reminder_cancel, v -> { /* dispensa */ });
        snackbarAtual.show();
    }

    /**
     * Ancora o Snackbar no primeiro filho do content view (o root do nosso layout),
     * nunca diretamente em android.R.id.content, para não inflar a região de toque
     * sobre o container inteiro da janela.
     */
    private View obterAncoraSnackbar() {
        ViewGroup content = findViewById(android.R.id.content);
        if (content != null && content.getChildCount() > 0) {
            return content.getChildAt(0);
        }
        // Fallback: qualquer view conhecida do layout.
        View lista = findViewById(R.id.reminders_list);
        return lista != null ? lista : content;
    }

    /** Fecha a Activity limpando qualquer Snackbar pendente antes. */
    private void sairDaTela() {
        if (snackbarAtual != null && snackbarAtual.isShown()) {
            snackbarAtual.dismiss();
            snackbarAtual = null;
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        // Evita segurar a Activity em memória via callback de Snackbar.
        if (snackbarAtual != null) {
            snackbarAtual.dismiss();
            snackbarAtual = null;
        }
        super.onDestroy();
    }

    private void atualizarLista() {
        List<LembreteComConta> filtrados = new ArrayList<>();
        for (LembreteComConta item : todos) {
            if (item == null || item.lembrete == null) continue;
            boolean concluido = item.lembrete.notificado;
            if (concluido == mostrarConcluidos) {
                filtrados.add(item);
            }
        }
        filtrados = BuscaGlobal.filtrarLembretes(filtrados, buscaTexto);
        adapter.definirLembretes(filtrados);
        mensagemVazia.setText(mostrarConcluidos
                ? R.string.reminders_empty_completed : R.string.reminders_empty_active);
        mensagemVazia.setVisibility(filtrados.isEmpty() ? View.VISIBLE : View.GONE);
        filtroAtivos.setSelected(!mostrarConcluidos);
        filtroConcluidos.setSelected(mostrarConcluidos);
    }

    @Override
    public void editar(LembreteComConta item) {
        abrirEditor(item.lembrete);
    }

    @Override
    public void excluir(LembreteComConta item) {
        Lembrete lembrete = item.lembrete;
        new AlertDialog.Builder(this)
                .setTitle(R.string.reminder_delete_title)
                .setMessage(getString(R.string.reminder_delete_confirmation, lembrete.titulo))
                .setPositiveButton(R.string.reminder_delete, (dialog, which) ->
                        viewModel.excluir(lembrete, retornoVoid()))
                .setNegativeButton(R.string.reminder_cancel, null)
                .show();
    }

    @Override
    public void alterarAtivo(LembreteComConta item, boolean ativo) {
        Lembrete original = item.lembrete;
        Lembrete atualizado = copiar(original, original.dataHora, ativo, original.notificado);
        viewModel.atualizar(atualizado, new RetornoRepositorio<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(LembretesActivity.this,
                        ativo ? R.string.reminder_enabled_saved : R.string.reminder_disabled_saved,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception error) {
                adapter.notifyDataSetChanged();
                Toast.makeText(LembretesActivity.this, R.string.reminder_save_error,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void abrirEditor(Lembrete original) {
        long[] selecionado = {original == null
                ? System.currentTimeMillis() + 60 * 60 * 1000L : original.dataHora};
        LinearLayout formulario = new LinearLayout(this);
        formulario.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        formulario.setPadding(padding, padding, padding, 0);

        TextInputLayout tituloLayout = new TextInputLayout(this);
        tituloLayout.setHint(getString(R.string.reminder_title_hint));
        TextInputEditText titulo = new TextInputEditText(tituloLayout.getContext());
        titulo.setSingleLine(true);
        titulo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        tituloLayout.addView(titulo);
        formulario.addView(tituloLayout);

        TextInputLayout mensagemLayout = new TextInputLayout(this);
        mensagemLayout.setHint(getString(R.string.reminder_message_hint));
        TextInputEditText mensagem = new TextInputEditText(mensagemLayout.getContext());
        mensagem.setMinLines(2);
        mensagem.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        mensagemLayout.addView(mensagem);
        formulario.addView(mensagemLayout);

        TextView dataHora = new TextView(this);
        dataHora.setPadding(0, padding, 0, padding);
        dataHora.setTextColor(getColor(R.color.green_primary));
        dataHora.setFocusable(true);
        dataHora.setClickable(true);
        dataHora.setContentDescription(getString(R.string.reminder_choose_date_accessibility));
        formulario.addView(dataHora);
        atualizarDataHora(dataHora, selecionado[0]);
        dataHora.setOnClickListener(view -> {
            if (original == null) {
                escolherDataHora(dataHora, selecionado);
            } else {
                escolherHorario(dataHora, selecionado);
            }
        });

        if (original != null) {
            titulo.setText(original.titulo);
            mensagem.setText(original.mensagem);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(original == null ? R.string.reminder_new_title : R.string.reminder_edit_title)
                .setView(formulario)
                .setPositiveButton(R.string.reminder_save, null)
                .setNegativeButton(R.string.reminder_cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> salvar(dialog, original, titulo, mensagem, selecionado[0])));
        dialog.show();
    }

    private void salvar(AlertDialog dialog, Lembrete original, EditText titulo,
                        EditText mensagem, long dataHora) {
        String tituloValor = titulo.getText().toString().trim();
        String mensagemValor = mensagem.getText().toString().trim();
        if (tituloValor.isEmpty()) {
            titulo.setError(getString(R.string.reminder_validation_title));
            return;
        }
        if (mensagemValor.isEmpty()) {
            mensagem.setError(getString(R.string.reminder_validation_message));
            return;
        }
        if (dataHora <= System.currentTimeMillis()
                && (original == null || original.contaId == null)) {
            Toast.makeText(this, R.string.reminder_error_past, Toast.LENGTH_LONG).show();
            return;
        }

        boolean ativo = original == null || original.ativo;
        Lembrete lembrete = new Lembrete(tituloValor, mensagemValor, dataHora, ativo,
                original == null ? null : original.contaId,
                original == null ? System.currentTimeMillis() : original.criadoEm,
                original != null && original.notificado,
                original == null ? email : original.ownerEmail);
        lembrete.groupId = original == null ? groupId : original.groupId;
        lembrete.createdByName = original == null
                ? new SessaoUsuario(this).obterNome() : original.createdByName;
        lembrete.remoteId = original == null ? null : original.remoteId;
        lembrete.contaRemoteId = original == null ? null : original.contaRemoteId;
        if (original == null) {
            viewModel.inserir(lembrete, new RetornoRepositorio<Long>() {
                @Override
                public void onSuccess(Long result) {
                    Toast.makeText(LembretesActivity.this,
                            R.string.reminder_saved, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Exception error) {
                    Toast.makeText(LembretesActivity.this,
                            error.getMessage() == null ? getString(R.string.reminder_save_error)
                                    : error.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            lembrete.id = original.id;
            viewModel.atualizar(lembrete, new RetornoRepositorio<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.makeText(LembretesActivity.this,
                            R.string.reminder_saved, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Exception error) {
                    Toast.makeText(LembretesActivity.this,
                            error.getMessage() == null ? getString(R.string.reminder_save_error)
                                    : error.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
        dialog.dismiss();
    }

    private void escolherDataHora(TextView destino, long[] selecionado) {
        Calendar calendario = Calendar.getInstance();
        calendario.setTimeInMillis(selecionado[0]);
        new DatePickerDialog(this, (picker, ano, mes, dia) -> {
            Calendar data = Calendar.getInstance();
            data.setTimeInMillis(selecionado[0]);
            data.set(Calendar.YEAR, ano);
            data.set(Calendar.MONTH, mes);
            data.set(Calendar.DAY_OF_MONTH, dia);
            new TimePickerDialog(this, (timePicker, hora, minuto) -> {
                data.set(Calendar.HOUR_OF_DAY, hora);
                data.set(Calendar.MINUTE, minuto);
                data.set(Calendar.SECOND, 0);
                data.set(Calendar.MILLISECOND, 0);
                selecionado[0] = data.getTimeInMillis();
                atualizarDataHora(destino, selecionado[0]);
            }, calendario.get(Calendar.HOUR_OF_DAY), calendario.get(Calendar.MINUTE), true).show();
        }, calendario.get(Calendar.YEAR), calendario.get(Calendar.MONTH),
                calendario.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void escolherHorario(TextView destino, long[] selecionado) {
        Calendar calendario = Calendar.getInstance();
        calendario.setTimeInMillis(selecionado[0]);
        new TimePickerDialog(this, (timePicker, hora, minuto) -> {
            calendario.set(Calendar.HOUR_OF_DAY, hora);
            calendario.set(Calendar.MINUTE, minuto);
            calendario.set(Calendar.SECOND, 0);
            calendario.set(Calendar.MILLISECOND, 0);
            selecionado[0] = calendario.getTimeInMillis();
            atualizarDataHora(destino, selecionado[0]);
        }, calendario.get(Calendar.HOUR_OF_DAY), calendario.get(Calendar.MINUTE), true).show();
    }

    private Lembrete copiar(Lembrete original, long dataHora, boolean ativo, boolean notificado) {
        Lembrete lembrete = new Lembrete(original.titulo, original.mensagem, dataHora,
                ativo, original.contaId, original.criadoEm, notificado, original.ownerEmail);
        lembrete.id = original.id;
        lembrete.groupId = original.groupId;
        lembrete.createdByName = original.createdByName;
        lembrete.remoteId = original.remoteId;
        lembrete.contaRemoteId = original.contaRemoteId;
        return lembrete;
    }

    private void atualizarDataHora(TextView destino, long instante) {
        destino.setText(new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm",
                new Locale("pt", "BR")).format(new Date(instante)));
    }

    private RetornoRepositorio<Void> retornoVoid() {
        return new RetornoRepositorio<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(LembretesActivity.this,
                        R.string.reminder_deleted, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(LembretesActivity.this,
                        R.string.reminder_delete_error, Toast.LENGTH_LONG).show();
            }
        };
    }
}