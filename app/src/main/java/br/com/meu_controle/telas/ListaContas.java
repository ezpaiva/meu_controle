package br.com.meu_controle.telas;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
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

import java.util.List;

import br.com.meu_controle.CalendarioActivity;
import br.com.meu_controle.MainActivity;
import br.com.meu_controle.PerfilActivity;
import br.com.meu_controle.R;
import br.com.meu_controle.dados.local.BancoDados;
import br.com.meu_controle.dados.local.SessaoUsuario;
import br.com.meu_controle.dados.modelo.Conta;
import br.com.meu_controle.dados.modelo.TipoConta;
import br.com.meu_controle.dados.repositorio.RepositorioContas;
import br.com.meu_controle.dados.repositorio.RepositorioLembretes;
import br.com.meu_controle.dados.repositorio.RetornoRepositorio;
import br.com.meu_controle.utilitarios.FormatoFinanceiro;
import br.com.meu_controle.utilitarios.NavegacaoInferior;
import br.com.meu_controle.viewmodels.SincronizacaoViewModel;

public class ListaContas extends AppCompatActivity {

    public static final String EXTRA_TIPO_CONTA = "tipo_conta";

    private String tipoConta;
    private RepositorioContas repositorio;
    private LinearLayout listaContas;
    private TextView mensagemVazia;
    private MaterialCardView cartaoResumo;
    private TextView rotuloResumo, valorResumo, quantidadeResumo;
    private MaterialButton botaoNovaConta;
    private View filtroContainer;
    private boolean exibindoReceitas;
    private SessaoUsuario sessao;
    private SincronizacaoViewModel sincronizacaoViewModel;

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

        atualizarEspacoAtivo();
        configurarNavegacaoInferior();

        BancoDados bancoDados = BancoDados.obterInstancia(getApplicationContext());
        repositorio = new RepositorioContas(bancoDados.contaDao(),
                new RepositorioLembretes(getApplicationContext()));

        listaContas = findViewById(R.id.accounts_container);
        mensagemVazia = findViewById(R.id.empty_accounts_message);
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (repositorio != null) {
            atualizarEspacoAtivo();
            carregarContas();

            // NOVO: baixa/envia alterações e recarrega a lista se deu certo
            sincronizacaoViewModel.sincronizar(new RetornoRepositorio<Void>() {
                @Override public void onSuccess(Void ignored) {
                    if (!isFinishing() && !isDestroyed()) {
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

    private void configurarFiltros() {
        if (exibindoReceitas) {
            filtroContainer.setVisibility(View.GONE);
        } else {
            filtroContainer.setVisibility(View.VISIBLE);
            Spinner spinner = findViewById(R.id.category_spinner);
            String[] categorias = {"Todas categorias", "Alimentação", "Transporte", "Moradia", "Saúde", "Lazer", "Educação", "Outros"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categorias);
            spinner.setAdapter(adapter);
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
        repositorio.buscarPorTipo(tipoConta, sessao.obterEmail(), sessao.perfilFamiliar(),
                sessao.obterGrupoId(), new RetornoRepositorio<List<Conta>>() {
            @Override
            public void onSuccess(List<Conta> contas) {
                listaContas.removeAllViews();
                mensagemVazia.setVisibility(contas.isEmpty() ? View.VISIBLE : View.GONE);
                long totalCentavos = 0;
                for (Conta conta : contas) totalCentavos += conta.amountInCents;

                valorResumo.setText(FormatoFinanceiro.formatAmount(totalCentavos));
                quantidadeResumo.setText(contas.size() + " registros");

                LayoutInflater inflater = LayoutInflater.from(ListaContas.this);
                for (Conta conta : contas) {
                    View linha = inflater.inflate(R.layout.item_account, listaContas, false);
                    preencherConta(linha, conta);
                    listaContas.addView(linha);
                }
            }
            @Override
            public void onError(Exception erro) {
                Toast.makeText(ListaContas.this, R.string.load_error, Toast.LENGTH_LONG).show();
            }
        });
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
                        repositorio.excluir(conta.id, new RetornoRepositorio<Void>() {
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