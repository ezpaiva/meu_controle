package br.com.meu_controle.telas;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import br.com.meu_controle.CalendarioActivity;
import br.com.meu_controle.MainActivity;
import br.com.meu_controle.PerfilActivity;
import br.com.meu_controle.R;
import br.com.meu_controle.dados.local.SessaoUsuario;
import br.com.meu_controle.dados.modelo.Categoria;
import br.com.meu_controle.dados.modelo.Conta;
import br.com.meu_controle.dados.modelo.TipoConta;
import br.com.meu_controle.dados.repositorio.RetornoRepositorio;
import br.com.meu_controle.utilitarios.FiltroContas;
import br.com.meu_controle.utilitarios.FormatoFinanceiro;
import br.com.meu_controle.utilitarios.NavegacaoInferior;
import br.com.meu_controle.utilitarios.BuscaGlobal;
import br.com.meu_controle.viewmodels.SincronizacaoViewModel;
import br.com.meu_controle.viewmodels.FinancasViewModel;

public class ListaContas extends AppCompatActivity {

    public static final String EXTRA_TIPO_CONTA = "tipo_conta";

    private String tipoConta;
    private FinancasViewModel financasViewModel;
    private LinearLayout listaContas;
    private TextView mensagemVazia;
    private MaterialCardView cartaoResumo;
    private TextView rotuloResumo, valorResumo, quantidadeResumo;
    private MaterialButton botaoNovaConta;
    private View filtroContainer;
    private boolean exibindoReceitas;
    private SessaoUsuario sessao;
    private SincronizacaoViewModel sincronizacaoViewModel;

    // ---------- Filtro de categorias ----------
    private Spinner spinnerCategoria;
    private CharSequence textoVazioPadrao;
    private final List<Conta> todasContas = new ArrayList<>();
    private final List<String> categoriasCadastradas = new ArrayList<>();
    private final List<String> opcoesFiltro = new ArrayList<>();
    private String categoriaFiltro = null; // null = todas as categorias
    private String buscaTexto = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_list);

        tipoConta = getIntent().getStringExtra(EXTRA_TIPO_CONTA);
        if (!TipoConta.RECEBER.equals(tipoConta) && !TipoConta.PAGAR.equals(tipoConta)) {
            finish();
            return;
        }
        exibindoReceitas = TipoConta.RECEBER.equals(tipoConta);
        sessao = new SessaoUsuario(this);
        sincronizacaoViewModel = new ViewModelProvider(this)
                .get(SincronizacaoViewModel.class);
        financasViewModel = new ViewModelProvider(this).get(FinancasViewModel.class);

        atualizarEspacoAtivo();
        configurarNavegacaoInferior();


        listaContas = findViewById(R.id.accounts_container);
        mensagemVazia = findViewById(R.id.empty_accounts_message);
        textoVazioPadrao = mensagemVazia.getText();
        cartaoResumo = findViewById(R.id.accounts_summary_card);
        rotuloResumo = findViewById(R.id.accounts_summary_label);
        valorResumo = findViewById(R.id.accounts_summary_amount);
        quantidadeResumo = findViewById(R.id.accounts_summary_count);
        filtroContainer = findViewById(R.id.filter_container);

        TextView titulo = findViewById(R.id.accounts_title);
        titulo.setText(exibindoReceitas ? "Receitas" : "Despesas");

        botaoNovaConta = findViewById(R.id.new_account_button);

        configurarResumoEAcao();
        configurarFiltros();

        botaoNovaConta.setOnClickListener(view -> abrirNovaConta());
        com.google.android.material.textfield.TextInputEditText busca =
                findViewById(R.id.account_search_input);
        busca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                buscaTexto = s.toString();
                exibirContas();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (financasViewModel != null) {
            atualizarEspacoAtivo();
            carregarCategorias();
            carregarContas();

            // Baixa/envia alterações e recarrega a lista (mantendo o filtro escolhido)
            sincronizacaoViewModel.sincronizar(new RetornoRepositorio<Void>() {
                @Override public void onSuccess(Void ignored) {
                    if (!isFinishing() && !isDestroyed()) {
                        carregarCategorias();
                        carregarContas();
                    }
                }
                @Override public void onError(Exception error) {
                    if (!isFinishing() && !isDestroyed()) {
                        carregarContas();
                    }
                }
            });
        }
    }

    private void configurarResumoEAcao() {
        int corAcento = getColor(exibindoReceitas ? R.color.green_primary : R.color.payables_accent);

        cartaoResumo.setCardBackgroundColor(corAcento);
        rotuloResumo.setText(exibindoReceitas ? "Total de receitas" : "Total de despesas");
        botaoNovaConta.setText(exibindoReceitas ? "+ Adicionar receita" : "+ Adicionar despesa");

        if (exibindoReceitas) {
            // Botão Preenchido (Receitas)
            botaoNovaConta.setBackgroundTintList(ColorStateList.valueOf(corAcento));
            botaoNovaConta.setTextColor(getColor(R.color.white));
        } else {
            // Botão Vazado (Despesas)
            botaoNovaConta.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.white)));
            botaoNovaConta.setTextColor(corAcento);
            botaoNovaConta.setStrokeColor(ColorStateList.valueOf(corAcento));
            botaoNovaConta.setStrokeWidth(2);
        }
    }

    // ---------- Filtro de categorias ----------
    private void configurarFiltros() {
        filtroContainer.setVisibility(View.VISIBLE);
        spinnerCategoria = findViewById(R.id.category_spinner);
        atualizarOpcoesFiltro(); // começa só com "Todas categorias"

        spinnerCategoria.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int posicao, long id) {
                String escolhida = (posicao <= 0 || posicao >= opcoesFiltro.size())
                        ? null : opcoesFiltro.get(posicao);
                if (!Objects.equals(escolhida, categoriaFiltro)) {
                    categoriaFiltro = escolhida;
                    exibirContas();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    /** Busca as categorias de despesa do usuário (ou da família) para montar o filtro. */
    private void carregarCategorias() {
        financasViewModel.buscarCategorias(tipoConta, sessao.obterEmail(),
                sessao.perfilFamiliar(), sessao.obterGrupoId(),
                new RetornoRepositorio<List<Categoria>>() {
                    @Override
                    public void onSuccess(List<Categoria> categorias) {
                        if (isFinishing() || isDestroyed()) return;
                        categoriasCadastradas.clear();
                        for (Categoria categoria : categorias) {
                            if (categoria.nome != null) categoriasCadastradas.add(categoria.nome);
                        }
                        atualizarOpcoesFiltro();
                    }

                    @Override
                    public void onError(Exception error) {
                        // Sem as cadastradas, o filtro usa as categorias das próprias despesas
                    }
                });
    }

    /** Recria as opções do filtro só se mudaram, preservando a categoria escolhida. */
    private void atualizarOpcoesFiltro() {
        if (spinnerCategoria == null) return;

        List<String> novas = FiltroContas.opcoesDeCategoria(categoriasCadastradas, todasContas);

        // A categoria filtrada pode ter sido renomeada ou excluída
        if (categoriaFiltro != null && !novas.contains(categoriaFiltro)) {
            categoriaFiltro = null;
        }

        if (!novas.equals(opcoesFiltro)) {
            opcoesFiltro.clear();
            opcoesFiltro.addAll(novas);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, new ArrayList<>(opcoesFiltro));
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCategoria.setAdapter(adapter);
        }

        int indice = categoriaFiltro == null ? 0 : opcoesFiltro.indexOf(categoriaFiltro);
        if (spinnerCategoria.getSelectedItemPosition() != indice) {
            spinnerCategoria.setSelection(indice);
        }
    }

    private void atualizarEspacoAtivo() {
        TextView nomeEspaco = findViewById(R.id.active_space_name);
        TextView detalheEspaco = findViewById(R.id.active_space_detail);
        nomeEspaco.setText(sessao.perfilFamiliar() ? "Família" : "Meu espaço individual");
        detalheEspaco.setText(sessao.perfilFamiliar()
                ? sessao.obterMembros().size() + " membros" : "Privado");
    }

    private void abrirNovaConta() {
        Intent intent = new Intent(this, NovaConta.class);
        intent.putExtra(EXTRA_TIPO_CONTA, tipoConta);
        startActivity(intent);
    }

    private void carregarContas() {
        financasViewModel.buscarPorTipo(tipoConta, sessao.obterEmail(), sessao.perfilFamiliar(),
                sessao.obterGrupoId(), new RetornoRepositorio<List<Conta>>() {
                    @Override
                    public void onSuccess(List<Conta> contas) {
                        if (isFinishing() || isDestroyed()) return;
                        todasContas.clear();
                        todasContas.addAll(contas);
                        atualizarOpcoesFiltro();
                        exibirContas();
                    }
                    @Override
                    public void onError(Exception erro) {
                        Toast.makeText(ListaContas.this, R.string.load_error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** Desenha a lista aplicando o filtro de categoria às contas já carregadas. */
    private void exibirContas() {
        List<Conta> contas = FiltroContas.porCategoria(todasContas, categoriaFiltro);
        contas = BuscaGlobal.filtrarContas(contas, buscaTexto);

        listaContas.removeAllViews();
        mensagemVazia.setText(todasContas.isEmpty() || categoriaFiltro == null
                ? textoVazioPadrao : "Nenhuma movimentação nesta categoria");
        mensagemVazia.setVisibility(contas.isEmpty() ? View.VISIBLE : View.GONE);

        long totalCentavos = 0;
        for (Conta conta : contas) totalCentavos += conta.amountInCents;

        String rotulo = exibindoReceitas ? "Total de receitas" : "Total de despesas";
        if (categoriaFiltro != null) rotulo += " · " + categoriaFiltro;
        rotuloResumo.setText(rotulo);
        valorResumo.setText(FormatoFinanceiro.formatAmount(totalCentavos));
        quantidadeResumo.setText(contas.size() + (contas.size() == 1 ? " registro" : " registros"));

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Conta conta : contas) {
            View linha = inflater.inflate(R.layout.item_account, listaContas, false);
            preencherConta(linha, conta);
            listaContas.addView(linha);
        }
    }

    private void preencherConta(View linha, Conta conta) {
        TextView descricao = linha.findViewById(R.id.account_description);
        TextView valor = linha.findViewById(R.id.account_amount);
        TextView metadata = linha.findViewById(R.id.account_metadata);
        TextView btnEditar = linha.findViewById(R.id.btn_edit_item);
        TextView btnExcluir = linha.findViewById(R.id.btn_delete_item);

        descricao.setText(conta.description);

        // Receitas mostram valor verde positivo, Despesas mostram valor vermelho negativo
        if (exibindoReceitas) {
            valor.setText(FormatoFinanceiro.formatAmount(conta.amountInCents));
            valor.setTextColor(getColor(R.color.green_primary));
        } else {
            valor.setText("-" + FormatoFinanceiro.formatAmount(conta.amountInCents));
            valor.setTextColor(getColor(R.color.payables_accent));
        }

        String metaTexto = conta.category + " • " + FormatoFinanceiro.formatStoredDate(conta.dueDate);
        if (sessao.perfilFamiliar() && conta.responsibleName != null) {
            metaTexto += " • " + conta.responsibleName;
        }
        metadata.setText(metaTexto);

        btnEditar.setOnClickListener(v -> {
            Intent intent = new Intent(ListaContas.this, NovaConta.class);
            intent.putExtra(EXTRA_TIPO_CONTA, tipoConta);
            intent.putExtra("CONTA_EDITAR", conta);
            startActivity(intent);
        });

        btnExcluir.setOnClickListener(v -> {
            new AlertDialog.Builder(ListaContas.this)
                    .setTitle("Excluir")
                    .setMessage("Deseja excluir '" + conta.description + "'?")
                    .setPositiveButton("Sim", (dialog, which) -> {
                        financasViewModel.excluirConta(conta.id, new RetornoRepositorio<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                carregarContas();
                                Toast.makeText(ListaContas.this, "Excluído", Toast.LENGTH_SHORT).show();
                            }
                            @Override
                            public void onError(Exception error) {
                                Toast.makeText(ListaContas.this, "Erro ao excluir", Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("Não", null)
                    .show();
        });
    }

    private void configurarNavegacaoInferior() {
        NavegacaoInferior.configurar(this,
                (BottomNavigationView) findViewById(R.id.bottom_navigation),
                exibindoReceitas ? R.id.nav_receitas : R.id.nav_despesas);
    }

    private void abrirContas(String tipo) {
        if (tipo.equals(tipoConta)) return;
        Intent intent = new Intent(this, ListaContas.class);
        intent.putExtra(EXTRA_TIPO_CONTA, tipo);
        startActivity(intent);
        finish();
    }
}