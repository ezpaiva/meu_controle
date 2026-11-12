package br.com.meu_controle;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.List;

import br.com.meu_controle.dados.local.BancoDados;
import br.com.meu_controle.dados.local.SessaoUsuario;
import br.com.meu_controle.dados.modelo.Conta;
import br.com.meu_controle.dados.modelo.Lembrete;
import br.com.meu_controle.dados.modelo.TipoConta;
import br.com.meu_controle.dados.remoto.SincronizadorContas; // NOVO
import br.com.meu_controle.dados.repositorio.RepositorioContas;
import br.com.meu_controle.dados.repositorio.RepositorioLembretes;
import br.com.meu_controle.dados.repositorio.RetornoRepositorio;
import br.com.meu_controle.telas.ListaContas;
import br.com.meu_controle.utilitarios.FormatoFinanceiro;
import br.com.meu_controle.utilitarios.PieChartView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private SessaoUsuario sessao;
    private RepositorioContas repositorio;
    private RepositorioLembretes repositorioLembretes;
    private ActivityResultLauncher<String> permissaoNotificacao;

    // Views do Dashboard
    private TextView valorReceitas;
    private TextView valorDespesas;
    private TextView valorSaldo;
    private PieChartView pieChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessao = new SessaoUsuario(this);
        permissaoNotificacao = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), concedida ->
                        getSharedPreferences("permissoes", MODE_PRIVATE).edit()
                                .putBoolean("notificacoes_solicitadas", true).apply());
        repositorioLembretes = new RepositorioLembretes(getApplicationContext());
        repositorio = new RepositorioContas(
                BancoDados.obterInstancia(getApplicationContext()).contaDao());

        // Inicialização das Views
        valorReceitas = findViewById(R.id.dashboard_income);
        valorDespesas = findViewById(R.id.dashboard_expense);
        valorSaldo = findViewById(R.id.dashboard_balance);
        pieChart = findViewById(R.id.dashboard_pie_chart);
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
        repositorioLembretes.reagendarPendentes(sessao.obterEmail());
        carregarResumoLembretes();

        // NOVO: baixa/envia alterações e recarrega o resumo se deu certo
        SincronizadorContas.sincronizar((sucesso, mensagem) -> {
            if (sucesso && !isFinishing() && !isDestroyed()) {
                carregarResumo();
            }
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
        repositorioLembretes.buscarPendentes(sessao.obterEmail(),
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
            nomeEspaco.setText("Família");
            detalheEspaco.setText(getString(R.string.active_space_members, sessao.obterMembros().size() + 1));
        } else {
            nomeEspaco.setText(R.string.active_space_individual);
            detalheEspaco.setText(R.string.active_space_private);
        }
    }

    /**
     * Busca as contas no banco e atualiza os valores do Dashboard e o gráfico
     */
    private void carregarResumo() {
        // Pega a data de hoje para exibir no protótipo
        String dataHoje = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().getTime());
        TextView lblData = findViewById(R.id.dashboard_forecast_date);
        TextView lblDataCard = findViewById(R.id.dashboard_forecast_date_label);

        if(lblData != null) lblData.setText(dataHoje);
        if(lblDataCard != null) lblDataCard.setText("Até " + dataHoje);

        repositorio.buscarTodas(sessao.obterEmail(), sessao.perfilFamiliar(),
                new RetornoRepositorio<>() {
                    @Override
                    public void onSuccess(List<Conta> contas) {
                        long totalReceitas = 0;
                        long totalDespesas = 0;

                        for (Conta conta : contas) {
                            if (TipoConta.RECEBER.equals(conta.type)) {
                                totalReceitas += conta.amountInCents;
                            } else if (TipoConta.PAGAR.equals(conta.type)) {
                                totalDespesas += conta.amountInCents;
                            }
                        }

                        long saldo = totalReceitas - totalDespesas;

                        valorReceitas.setText(FormatoFinanceiro.formatAmount(totalReceitas));
                        valorDespesas.setText(FormatoFinanceiro.formatAmount(totalDespesas));
                        valorSaldo.setText(FormatoFinanceiro.formatAmount(saldo));
                        valorSaldo.setTextColor(getColor(saldo >= 0 ? R.color.green_primary : R.color.payables_accent));

                        TextView statusSaldo = findViewById(R.id.dashboard_balance_status);
                        TextView descSaldo = findViewById(R.id.dashboard_balance_desc);

                        if (saldo >= 0) {
                            statusSaldo.setText("Saldo positivo");
                            descSaldo.setText("Suas receitas superam as despesas. Você tem uma sobra prevista neste período.");
                        } else {
                            statusSaldo.setText("Saldo negativo");
                            statusSaldo.setTextColor(getColor(R.color.payables_accent));
                            descSaldo.setText("Suas despesas superam as receitas neste período.");
                            descSaldo.setTextColor(getColor(R.color.payables_accent));
                        }

                        pieChart.setData((float) totalReceitas / 100f, (float) totalDespesas / 100f);
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
        // Ação do Dashboard (A própria tela atual, sem ação necessária)
        TextView navDashboard = findViewById(R.id.nav_dashboard);
        navDashboard.setTextColor(getColor(R.color.receivables_accent_dark));
        navDashboard.setTypeface(null, Typeface.BOLD);

        // Ação de Receitas
        findViewById(R.id.nav_receitas).setOnClickListener(v ->
                abrirListaContas(TipoConta.RECEBER));

        // Ação de Despesas
        findViewById(R.id.nav_despesas).setOnClickListener(v ->
                abrirListaContas(TipoConta.PAGAR));

        // Ação de Calendário
        findViewById(R.id.nav_calendario).setOnClickListener(v -> {
            startActivity(new Intent(this, CalendarioActivity.class));
            finish();
        });

        // Ação de Perfil
        findViewById(R.id.nav_perfil).setOnClickListener(v -> {
            startActivity(new Intent(this, PerfilActivity.class));
            finish();
        });
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