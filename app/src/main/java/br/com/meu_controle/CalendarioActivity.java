package br.com.meu_controle;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import br.com.meu_controle.dados.local.BancoDados;
import br.com.meu_controle.dados.local.SessaoUsuario;
import br.com.meu_controle.dados.modelo.Conta;
import br.com.meu_controle.dados.modelo.Lembrete;
import br.com.meu_controle.dados.modelo.TipoConta;
import br.com.meu_controle.dados.remoto.FeriadosApi;
import br.com.meu_controle.dados.remoto.SincronizadorContas;
import br.com.meu_controle.dados.repositorio.RepositorioContas;
import br.com.meu_controle.dados.repositorio.RetornoRepositorio;
import br.com.meu_controle.telas.ListaContas;
import br.com.meu_controle.telas.LembretesActivity;
import br.com.meu_controle.utilitarios.FormatoFinanceiro;

public class CalendarioActivity extends AppCompatActivity {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu", PT_BR);
    private static final DateTimeFormatter FORMATO_DIA_MES =
            DateTimeFormatter.ofPattern("dd/MM", PT_BR);

    private SessaoUsuario sessao;
    private RepositorioContas repositorio;
    private final Map<LocalDate, List<Lembrete>> lembretesPorData = new HashMap<>();

    private YearMonth mesAtual = YearMonth.now();
    private LocalDate diaSelecionado = LocalDate.now();

    private final Map<LocalDate, List<Conta>> contasPorData = new HashMap<>();
    private final Map<LocalDate, String> feriados = new HashMap<>();
    private final Set<Integer> anosComFeriados = new HashSet<>();
    private String origemFeriados = "";

    private TextView tituloMes;
    private GridLayout grade;
    private TextView rotuloData;
    private TextView bannerFeriado;
    private TextView avisoDia;
    private LinearLayout containerMovimentacoes;
    private TextView msgSemMovimentacoes;
    private LinearLayout containerFeriados;
    private TextView statusApi;
    private LinearLayout containerLembretes;
    private TextView msgSemLembretes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendario);

        sessao = new SessaoUsuario(this);
        repositorio = new RepositorioContas(
                BancoDados.obterInstancia(getApplicationContext()).contaDao());
        TextView nome = findViewById(R.id.active_space_name_cal);
        TextView detalhe = findViewById(R.id.active_space_detail_cal);
        nome.setText(sessao.perfilFamiliar() ? "Família" : "Meu espaço individual");
        detalhe.setText(sessao.perfilFamiliar()
                ? (sessao.obterMembros().size() + 1) + " membros" : "Privado");

        tituloMes = findViewById(R.id.titulo_mes);
        grade = findViewById(R.id.grade_calendario);
        rotuloData = findViewById(R.id.rotulo_data_selecionada);
        bannerFeriado = findViewById(R.id.banner_feriado);
        avisoDia = findViewById(R.id.aviso_dia);
        containerMovimentacoes = findViewById(R.id.container_movimentacoes);
        msgSemMovimentacoes = findViewById(R.id.msg_sem_movimentacoes);
        containerFeriados = findViewById(R.id.container_feriados_mes);
        statusApi = findViewById(R.id.status_api);
        containerLembretes = findViewById(R.id.container_lembretes_dia);
        msgSemLembretes = findViewById(R.id.msg_sem_lembretes);

        BancoDados.obterInstancia(getApplicationContext()).lembreteDao()
                .observarTodos(sessao.obterEmail()).observe(this, lembretes -> {
                    lembretesPorData.clear();
                    if (lembretes != null) {
                        for (Lembrete lembrete : lembretes) {
                            LocalDate data = LocalDate.parse(lembrete.data);
                            lembretesPorData.computeIfAbsent(data, chave -> new ArrayList<>())
                                    .add(lembrete);
                        }
                    }
                    desenhar();
                });

        findViewById(R.id.btn_mes_anterior).setOnClickListener(v -> mudarMes(-1));
        findViewById(R.id.btn_mes_proximo).setOnClickListener(v -> mudarMes(1));
        findViewById(R.id.calendar_open_reminders).setOnClickListener(v ->
                startActivity(new Intent(this, LembretesActivity.class)));

        configurarNavegacaoInferior();
        desenhar();
        carregarFeriados(mesAtual.getYear());
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarContas();

        SincronizadorContas.sincronizar((sucesso, mensagem) -> {
            if (sucesso && !isFinishing() && !isDestroyed()) {
                carregarContas();
            }
        });
    }

    // ---------- Dados ----------
    private void carregarContas() {
        repositorio.buscarTodas(sessao.obterEmail(), sessao.perfilFamiliar(),
                new RetornoRepositorio<List<Conta>>() {
                    @Override
                    public void onSuccess(List<Conta> contas) {
                        contasPorData.clear();
                        for (Conta conta : contas) {
                            try {
                                LocalDate data = LocalDate.parse(conta.dueDate);
                                contasPorData.computeIfAbsent(data, k -> new ArrayList<>())
                                        .add(conta);
                            } catch (Exception ignorada) {
                                // data inválida: a conta simplesmente não aparece no calendário
                            }
                        }
                        desenhar();
                    }

                    @Override
                    public void onError(Exception error) {
                        Toast.makeText(CalendarioActivity.this,
                                R.string.load_error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void carregarFeriados(int ano) {
        if (anosComFeriados.contains(ano)) return;

        FeriadosApi.buscar(this, ano, (lista, origem) -> {
            if (isFinishing() || isDestroyed()) return;
            for (FeriadosApi.Feriado feriado : lista) {
                feriados.put(feriado.data, feriado.nome);
            }
            origemFeriados = origem;
            if (!FeriadosApi.ORIGEM_INDISPONIVEL.equals(origem)) {
                anosComFeriados.add(ano);
            }
            desenhar();
        });
    }

    private void mudarMes(int delta) {
        mesAtual = mesAtual.plusMonths(delta);
        diaSelecionado = YearMonth.now().equals(mesAtual) ? LocalDate.now() : mesAtual.atDay(1);
        carregarFeriados(mesAtual.getYear());
        desenhar();
    }

    // ---------- Desenho da tela ----------
    private void desenhar() {
        String mes = mesAtual.getMonth().getDisplayName(TextStyle.FULL, PT_BR);
        tituloMes.setText(mes.substring(0, 1).toUpperCase(PT_BR) + mes.substring(1)
                + " " + mesAtual.getYear());

        desenharGrade();
        desenharDiaSelecionado();
        desenharFeriadosDoMes();
        atualizarStatusApi();
    }

    private void desenharGrade() {
        grade.removeAllViews();

        String[] letras = {"D", "S", "T", "Q", "Q", "S", "S"};
        for (String letra : letras) {
            TextView cabecalho = new TextView(this);
            cabecalho.setText(letra);
            cabecalho.setGravity(Gravity.CENTER);
            cabecalho.setTextColor(getColor(R.color.gray_text));
            cabecalho.setTypeface(null, Typeface.BOLD);
            cabecalho.setLayoutParams(paramsCelula(dp(28)));
            grade.addView(cabecalho);
        }

        // Domingo = coluna 0 (DayOfWeek: segunda=1 ... domingo=7)
        int deslocamento = mesAtual.atDay(1).getDayOfWeek().getValue() % 7;
        for (int i = 0; i < deslocamento; i++) {
            View vazio = new View(this);
            vazio.setLayoutParams(paramsCelula(dp(48)));
            grade.addView(vazio);
        }

        for (int dia = 1; dia <= mesAtual.lengthOfMonth(); dia++) {
            grade.addView(criarCelulaDia(mesAtual.atDay(dia)));
        }
    }

    private TextView criarCelulaDia(LocalDate data) {
        boolean temConta = contasPorData.containsKey(data)
                || lembretesPorData.containsKey(data);
        boolean ehFeriado = feriados.containsKey(data);
        boolean selecionado = data.equals(diaSelecionado);
        boolean hoje = data.equals(LocalDate.now());

        TextView celula = new TextView(this);
        celula.setText(temConta
                ? data.getDayOfMonth() + "\n•"
                : String.valueOf(data.getDayOfMonth()));
        celula.setGravity(Gravity.CENTER);
        celula.setTextSize(14);

        GradientDrawable fundo = new GradientDrawable();
        fundo.setColor(Color.TRANSPARENT);
        fundo.setCornerRadius(dp(8));

        int corTexto = getColor(ehFimDeSemana(data) ? R.color.gray_text : R.color.black);

        if (ehFeriado) {
            fundo.setColor(getColor(R.color.status_pending_background));
            corTexto = getColor(R.color.status_pending_text);
        }
        if (hoje) {
            fundo.setStroke(dp(2), getColor(R.color.green_primary));
        }
        if (selecionado) {
            fundo.setColor(getColor(R.color.green_primary));
            corTexto = getColor(R.color.white);
        }

        celula.setBackground(fundo);
        celula.setTextColor(corTexto);
        celula.setLayoutParams(paramsCelula(dp(48)));
        celula.setOnClickListener(v -> {
            diaSelecionado = data;
            desenhar();
        });
        return celula;
    }

    private void desenharDiaSelecionado() {
        rotuloData.setText("Movimentações em " + diaSelecionado.format(FORMATO_DATA));

        String feriadoDoDia = feriados.get(diaSelecionado);
        bannerFeriado.setVisibility(feriadoDoDia != null ? View.VISIBLE : View.GONE);
        if (feriadoDoDia != null) {
            bannerFeriado.setText("Feriado nacional: " + feriadoDoDia);
        }

        containerMovimentacoes.removeAllViews();
        List<Conta> contas = contasPorData.get(diaSelecionado);
        boolean temContas = contas != null && !contas.isEmpty();

        msgSemMovimentacoes.setVisibility(temContas ? View.GONE : View.VISIBLE);
        if (temContas) {
            for (Conta conta : contas) {
                containerMovimentacoes.addView(criarLinhaConta(conta));
            }
        }

        containerLembretes.removeAllViews();
        List<Lembrete> lembretes = lembretesPorData.get(diaSelecionado);
        boolean temLembretes = lembretes != null && !lembretes.isEmpty();
        msgSemLembretes.setVisibility(temLembretes ? View.GONE : View.VISIBLE);
        if (temLembretes) {
            for (Lembrete lembrete : lembretes) {
                TextView linha = new TextView(this);
                String horario = lembrete.hora;
                linha.setText(horario + " • " + lembrete.titulo + "\n" + lembrete.mensagem);
                linha.setPadding(dp(12), dp(12), dp(12), dp(12));
                linha.setBackgroundResource(R.drawable.bg_card_border);
                LinearLayout.LayoutParams parametros = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                parametros.bottomMargin = dp(8);
                linha.setLayoutParams(parametros);
                containerLembretes.addView(linha);
            }
        }

        boolean diaNaoUtil = feriadoDoDia != null || ehFimDeSemana(diaSelecionado);
        avisoDia.setVisibility(temContas && diaNaoUtil ? View.VISIBLE : View.GONE);
    }

    private View criarLinhaConta(Conta conta) {
        boolean receita = TipoConta.RECEBER.equals(conta.type);

        LinearLayout linha = new LinearLayout(this);
        linha.setOrientation(LinearLayout.HORIZONTAL);
        linha.setGravity(Gravity.CENTER_VERTICAL);
        linha.setBackgroundResource(R.drawable.bg_card_border);
        linha.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(8);
        linha.setLayoutParams(params);

        TextView descricao = new TextView(this);
        descricao.setText(conta.description + "\n" + (receita ? "Receita" : "Despesa")
                + " • " + conta.category);
        descricao.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView valor = new TextView(this);
        valor.setTypeface(null, Typeface.BOLD);
        valor.setText((receita ? "" : "-") + FormatoFinanceiro.formatAmount(conta.amountInCents));
        valor.setTextColor(getColor(receita ? R.color.green_primary : R.color.payables_accent));

        linha.addView(descricao);
        linha.addView(valor);
        return linha;
    }

    private void desenharFeriadosDoMes() {
        containerFeriados.removeAllViews();

        Map<LocalDate, String> doMes = new TreeMap<>();
        for (Map.Entry<LocalDate, String> entrada : feriados.entrySet()) {
            if (YearMonth.from(entrada.getKey()).equals(mesAtual)) {
                doMes.put(entrada.getKey(), entrada.getValue());
            }
        }

        if (doMes.isEmpty()) {
            TextView vazio = new TextView(this);
            vazio.setTextColor(getColor(R.color.gray_text));
            vazio.setText(FeriadosApi.ORIGEM_INDISPONIVEL.equals(origemFeriados)
                    ? "Não foi possível carregar os feriados (sem conexão e sem dados salvos)."
                    : "Nenhum feriado nacional neste mês.");
            containerFeriados.addView(vazio);
            return;
        }

        for (Map.Entry<LocalDate, String> entrada : doMes.entrySet()) {
            TextView linha = new TextView(this);
            linha.setText(entrada.getKey().format(FORMATO_DIA_MES) + "  —  " + entrada.getValue());
            linha.setPadding(0, dp(4), 0, dp(4));
            containerFeriados.addView(linha);
        }
    }

    private void atualizarStatusApi() {
        switch (origemFeriados) {
            case FeriadosApi.ORIGEM_API:
                statusApi.setText("Feriados nacionais: BrasilAPI (online)");
                break;
            case FeriadosApi.ORIGEM_CACHE:
                statusApi.setText("Feriados nacionais: dados salvos no aparelho (offline)");
                break;
            case FeriadosApi.ORIGEM_INDISPONIVEL:
                statusApi.setText("Feriados indisponíveis no momento (sem conexão)");
                break;
            default:
                statusApi.setText("Carregando feriados...");
        }
    }

    // ---------- Auxiliares ----------
    private GridLayout.LayoutParams paramsCelula(int altura) {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED),
                GridLayout.spec(GridLayout.UNDEFINED, 1f));
        params.width = 0;
        params.height = altura;
        params.setMargins(dp(2), dp(2), dp(2), dp(2));
        return params;
    }

    private boolean ehFimDeSemana(LocalDate data) {
        DayOfWeek dia = data.getDayOfWeek();
        return dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY;
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }

    // ---------- Navegação inferior (igual à versão anterior) ----------
    private void configurarNavegacaoInferior() {
        findViewById(R.id.nav_dashboard).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        findViewById(R.id.nav_receitas).setOnClickListener(v -> abrirLista(TipoConta.RECEBER));
        findViewById(R.id.nav_despesas).setOnClickListener(v -> abrirLista(TipoConta.PAGAR));
        findViewById(R.id.nav_perfil).setOnClickListener(v -> {
            startActivity(new Intent(this, PerfilActivity.class));
            finish();
        });

        TextView navAtivo = findViewById(R.id.nav_calendario);
        navAtivo.setTextColor(getColor(R.color.receivables_accent_dark));
        navAtivo.setTypeface(null, Typeface.BOLD);
    }

    private void abrirLista(String tipo) {
        Intent i = new Intent(this, ListaContas.class);
        i.putExtra(ListaContas.EXTRA_TIPO_CONTA, tipo);
        startActivity(i);
        finish();
    }
}