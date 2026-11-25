package br.com.meu_controle;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.util.List;

import br.com.meu_controle.dados.local.SessaoUsuario;
import br.com.meu_controle.dados.modelo.Conta;
import br.com.meu_controle.dados.modelo.Lembrete;
import br.com.meu_controle.dados.modelo.TipoConta;
import br.com.meu_controle.dados.repositorio.RetornoRepositorio;
import br.com.meu_controle.telas.ListaContas;
import br.com.meu_controle.utilitarios.FormatoFinanceiro;
import br.com.meu_controle.utilitarios.NavegacaoInferior;
import br.com.meu_controle.utilitarios.PieChartView;
import br.com.meu_controle.viewmodels.SincronizacaoViewModel;
import br.com.meu_controle.viewmodels.FinancasViewModel;
import br.com.meu_controle.viewmodels.LembreteViewModel;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private SessaoUsuario sessao;
    private FinancasViewModel financasViewModel;
    private LembreteViewModel lembreteViewModel;
    private ActivityResultLauncher<String> permissaoNotificacao;
    private SincronizacaoViewModel sincronizacaoViewModel;

    // Views do Dashboard
    private TextView valorReceitas;
    private TextView valorDespesas;
    private TextView valorSaldo;
    private PieChartView pieChart;
    private String dataPrevisaoIso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessao = new SessaoUsuario(this);
        permissaoNotificacao = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), concedida ->
                        getSharedPreferences("permissoes", MODE_PRIVATE).edit()
                                .putBoolean("notificacoes_solicitadas", true).apply());
        financasViewModel = new ViewModelProvider(this).get(FinancasViewModel.class);
        lembreteViewModel = new ViewModelProvider(this).get(LembreteViewModel.class);
        sincronizacaoViewModel = new ViewModelProvider(this)
                .get(SincronizacaoViewModel.class);

        // Inicialização das Views
        valorReceitas = findViewById(R.id.dashboard_income);
        valorDespesas = findViewById(R.id.dashboard_expense);
        valorSaldo = findViewById(R.id.dashboard_balance);
        pieChart = findViewById(R.id.dashboard_pie_chart);
        dataPrevisaoIso = LocalDate.now().toString();
        financasViewModel.definirDataSelecionada(dataPrevisaoIso);
        observarResumoPrevisto();
        TextView campoData = findViewById(R.id.dashboard_forecast_date);
        campoData.setOnClickListener(view -> abrirSeletorData());
        campoData.setFocusable(true);
        campoData.setClickable(true);
        findViewById(R.id.dashboard_reminders_card).setOnClickListener(view ->
                startActivity(new Intent(this, br.com.meu_controle.telas.LembretesActivity.class)));

        configurarNavegacaoInferior();
    }

    @Override
    protected void onResume() {
        super.onResume();
        atualizarEspacoAtivo();
        carregarResumo();
        solicitarPermissaoNotificacoes();
        lembreteViewModel.reagendarPendentes(sessao.obterEmail(), sessao.obterGrupoId());
        carregarResumoLembretes();

        // NOVO: baixa/envia alterações e recarrega o resumo se deu certo
        sincronizacaoViewModel.sincronizar(new RetornoRepositorio<Void>() {
            @Override public void onSuccess(Void ignored) { carregarResumo(); carregarResumoLembretes(); }
            @Override public void onError(Exception error) { carregarResumo(); }
        });
    }

    private void solicitarPermissaoNotificacoes() {
        if (!sessao.isNotificationsEnabled()) {
            return;
        }
        if (Build.VERSION.SDK_INT < 33
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        boolean jaSolicitada = getSharedPreferences("permissoes", MODE_PRIVATE)
                .getBoolean("notificacoes_solicitadas", false);
        if (!jaSolicitada) {
            permissaoNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void carregarResumoLembretes() {
        TextView quantidade = findViewById(R.id.dashboard_reminders_count);
        TextView proximo = findViewById(R.id.dashboard_next_reminder);
        lembreteViewModel.buscarPendentes(sessao.obterEmail(), sessao.obterGrupoId(),
                new RetornoRepositorio<List<Lembrete>>() {
                    @Override
                    public void onSuccess(List<Lembrete> lembretes) {
                        quantidade.setText(getString(R.string.reminder_dashboard_count,
                                lembretes.size()));
                        if (lembretes.isEmpty()) {
                            proximo.setText(R.string.reminder_dashboard_none);
                            return;
                        }
                        Lembrete lembrete = null;
                        for (Lembrete pendente : lembretes) {
                            if (pendente.dataHora >= System.currentTimeMillis()) {
                                lembrete = pendente;
                                break;
                            }
                        }
                        if (lembrete == null) {
                            proximo.setText(R.string.reminder_dashboard_none);
                            return;
                        }
                        String quando = new SimpleDateFormat(
                                "dd/MM/yyyy 'às' HH:mm", Locale.getDefault())
                                .format(new Date(lembrete.dataHora));
                        proximo.setText(getString(R.string.reminder_dashboard_next,
                                lembrete.titulo, quando));
                    }

                    @Override
                    public void onError(Exception error) {
                        Toast.makeText(MainActivity.this, R.string.load_error,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Atualiza o cabeçalho com os dados do perfil (Individual ou Familiar)
     */
    private void atualizarEspacoAtivo() {
        TextView nomeEspaco = findViewById(R.id.dashboard_space_name);
        TextView detalheEspaco = findViewById(R.id.dashboard_space_detail);

        if (sessao.perfilFamiliar()) {
            nomeEspaco.setText(sessao.obterGrupoNome().isEmpty()
                    ? getString(R.string.active_space_family) : sessao.obterGrupoNome());
            detalheEspaco.setText(getString(R.string.active_space_members,
                    sessao.obterMembros().size()));
        } else {
            nomeEspaco.setText(R.string.active_space_individual);
            detalheEspaco.setText(R.string.active_space_private);
        }
    }

    /**
     * Busca as contas no banco e atualiza os valores do Dashboard e o gráfico
     */
    private void carregarResumo() {
        TextView lblData = findViewById(R.id.dashboard_forecast_date);
        TextView lblDataCard = findViewById(R.id.dashboard_forecast_date_label);
        String dataExibicao = FormatoFinanceiro.formatStoredDate(dataPrevisaoIso);
        if (lblData != null) lblData.setText(dataExibicao);
        if (lblDataCard != null) lblDataCard.setText("Até " + dataExibicao);

        financasViewModel.buscarTodas(sessao.obterEmail(), sessao.perfilFamiliar(), sessao.obterGrupoId(),
                new RetornoRepositorio<>() {
                    @Override
                    public void onSuccess(List<Conta> contas) {
                        financasViewModel.definirDataSelecionada(dataPrevisaoIso);
                        financasViewModel.calcularResumoPrevisto(contas);
                    }

                    @Override
                    public void onError(Exception error) {
                        Toast.makeText(MainActivity.this, R.string.load_error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Configura os cliques da barra de navegação inferior e destaca a aba atual
     */
    private void configurarNavegacaoInferior() {
        NavegacaoInferior.configurar(this,
                (BottomNavigationView) findViewById(R.id.bottom_navigation),
                R.id.nav_dashboard);
    }

    private void observarResumoPrevisto() {
        financasViewModel.observarResumoPrevisto().observe(this, resumo -> {
            if (resumo == null) return;
            valorReceitas.setText(FormatoFinanceiro.formatAmount(resumo.receitas));
            valorDespesas.setText(FormatoFinanceiro.formatAmount(resumo.despesas));
            valorSaldo.setText(FormatoFinanceiro.formatAmount(resumo.saldo));
            valorSaldo.setTextColor(getColor(resumo.saldo >= 0
                    ? R.color.green_primary : R.color.payables_accent));

            TextView statusSaldo = findViewById(R.id.dashboard_balance_status);
            TextView descSaldo = findViewById(R.id.dashboard_balance_desc);
            if (resumo.saldo >= 0) {
                statusSaldo.setText("Saldo positivo");
                statusSaldo.setTextColor(getColor(R.color.green_dark));
                descSaldo.setText("Suas receitas superam as despesas. Você tem uma sobra prevista neste período.");
                descSaldo.setTextColor(getColor(R.color.green_dark));
            } else {
                statusSaldo.setText("Saldo negativo");
                statusSaldo.setTextColor(getColor(R.color.payables_accent));
                descSaldo.setText("Suas despesas superam as receitas neste período.");
                descSaldo.setTextColor(getColor(R.color.payables_accent));
            }
            pieChart.setData((float) resumo.receitas / 100f,
                    (float) resumo.despesas / 100f);
        });
    }

    private void abrirSeletorData() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Data da previsão")
                .setSelection(FormatoFinanceiro.isoDateToDatePickerMillis(dataPrevisaoIso))
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            dataPrevisaoIso = FormatoFinanceiro.datePickerMillisToIsoDate(selection);
            financasViewModel.definirDataSelecionada(dataPrevisaoIso);
            carregarResumo();
        });
        picker.show(getSupportFragmentManager(), "dashboard_data_previsao");
    }

    /**
     * Abre a tela de listagem com o filtro de tipo correspondente
     */
    private void abrirListaContas(String tipo) {
        Intent intent = new Intent(this, ListaContas.class);
        intent.putExtra(ListaContas.EXTRA_TIPO_CONTA, tipo);
        startActivity(intent);
        finish();
    }
}