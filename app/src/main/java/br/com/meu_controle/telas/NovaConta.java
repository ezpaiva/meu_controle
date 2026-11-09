package br.com.meu_controle.telas;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import br.com.meu_controle.R;
import br.com.meu_controle.dados.local.BancoDados;
import br.com.meu_controle.dados.local.SessaoUsuario;
import br.com.meu_controle.dados.modelo.Categoria;
import br.com.meu_controle.dados.modelo.Conta;
import br.com.meu_controle.dados.modelo.TipoConta;
import br.com.meu_controle.dados.repositorio.RepositorioCategorias;
import br.com.meu_controle.dados.repositorio.RepositorioContas;
import br.com.meu_controle.dados.repositorio.RetornoRepositorio;
import br.com.meu_controle.utilitarios.FormatoFinanceiro;

public class NovaConta extends AppCompatActivity {

    private String tipoConta;
    private String dataVencimentoSelecionada;
    private RepositorioContas repositorio;
    private EditText campoDescricao;
    private EditText campoValor;
    private TextView campoVencimento;
    private Spinner campoCategoria;
    private com.google.android.material.button.MaterialButton botaoSalvar;
    private com.google.android.material.button.MaterialButton botaoCancelar;
    private TextView btnVoltar;

    // Controla edição
    private Conta contaEditando = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_account);

        tipoConta = getIntent().getStringExtra(ListaContas.EXTRA_TIPO_CONTA);
        if (!TipoConta.RECEBER.equals(tipoConta) && !TipoConta.PAGAR.equals(tipoConta)) {
            Toast.makeText(this, R.string.save_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (getIntent().hasExtra("CONTA_EDITAR")) {
            contaEditando = (Conta) getIntent().getSerializableExtra("CONTA_EDITAR");
        }

        repositorio = new RepositorioContas(
                BancoDados.obterInstancia(getApplicationContext()).contaDao());

        // Inicializa componentes
        btnVoltar = findViewById(R.id.btnVoltar);
        campoDescricao = findViewById(R.id.description_input);
        campoValor = findViewById(R.id.amount_input);
        campoVencimento = findViewById(R.id.due_date_input);
        campoCategoria = findViewById(R.id.category_input);
        botaoSalvar = findViewById(R.id.save_account_button);
        botaoCancelar = findViewById(R.id.cancel_account_button);
        TextView titulo = findViewById(R.id.new_account_title);

        configurarCoresETextos(titulo);
        configurarListas();

        // Adapta a interface dependendo do modo
        if (contaEditando != null) {
            preencherFormularioParaEdicao();
        } else {
            definirVencimento(LocalDate.now());
        }

        campoVencimento.setOnClickListener(view -> mostrarSeletorData());

        botaoSalvar.setOnClickListener(view -> salvarConta());
        botaoCancelar.setOnClickListener(view -> finish());
        btnVoltar.setOnClickListener(view -> finish());
    }

    private void configurarCoresETextos(TextView titulo) {
        boolean isReceita = TipoConta.RECEBER.equals(tipoConta);
        int corAcento = getColor(isReceita ? R.color.green_primary : R.color.payables_accent);
        String tipoTexto = isReceita ? "receitas" : "despesas";

        // Textos
        btnVoltar.setText("← Voltar para " + tipoTexto);

        if (contaEditando != null) {
            titulo.setText("Editar " + (isReceita ? "Receita" : "Despesa"));
        } else {
            titulo.setText("Nova " + (isReceita ? "Receita" : "Despesa"));
        }

        // Cores do botão Salvar (Preenchido)
        botaoSalvar.setBackgroundTintList(ColorStateList.valueOf(corAcento));

        // Cores do botão Cancelar (Vazado)
        botaoCancelar.setTextColor(corAcento);
        botaoCancelar.setStrokeColor(ColorStateList.valueOf(corAcento));

        // Cores do botão voltar
        btnVoltar.setTextColor(corAcento);
    }

    private void configurarListas() {
        RepositorioCategorias repoCategorias = new RepositorioCategorias(
                BancoDados.obterInstancia(getApplicationContext()).categoriaDao());
        SessaoUsuario sessao = new SessaoUsuario(this);

        repoCategorias.buscarPorTipo(tipoConta, sessao.obterEmail(), sessao.perfilFamiliar(),
                new RetornoRepositorio<List<Categoria>>() {
                    @Override
                    public void onSuccess(List<Categoria> categorias) {
                        if (categorias.isEmpty()) {
                            aplicarFallbackSpinner();
                        } else {
                            aplicarCategoriasNoSpinner(categorias);
                        }
                    }

                    @Override
                    public void onError(Exception error) {
                        aplicarFallbackSpinner();
                    }
                });
    }

    private void aplicarCategoriasNoSpinner(List<Categoria> categorias) {
        List<String> nomes = new ArrayList<>();
        for (Categoria c : categorias) nomes.add(c.nome);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, nomes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        campoCategoria.setAdapter(adapter);

        if (contaEditando != null) {
            int pos = nomes.indexOf(contaEditando.category);
            if (pos >= 0) campoCategoria.setSelection(pos);
        }
    }

    private void aplicarFallbackSpinner() {
        int cats = TipoConta.RECEBER.equals(tipoConta)
                ? R.array.income_categories
                : R.array.expense_categories;
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, cats, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        campoCategoria.setAdapter(adapter);

        if (contaEditando != null) {
            int pos = adapter.getPosition(contaEditando.category);
            if (pos >= 0) campoCategoria.setSelection(pos);
        }
    }

    private void preencherFormularioParaEdicao() {
        campoDescricao.setText(contaEditando.description);

        BigDecimal valorDecimal = BigDecimal.valueOf(contaEditando.amountInCents, 2);
        campoValor.setText(valorDecimal.toString().replace(".", ","));

        dataVencimentoSelecionada = contaEditando.dueDate;
        campoVencimento.setText(FormatoFinanceiro.formatStoredDate(dataVencimentoSelecionada));
        // A seleção da categoria é aplicada quando as categorias chegarem do banco
    }

    private void mostrarSeletorData() {
        LocalDate data = LocalDate.parse(dataVencimentoSelecionada);
        DatePickerDialog seletor = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) ->
                        definirVencimento(LocalDate.of(year, month + 1, dayOfMonth)),
                data.getYear(),
                data.getMonthValue() - 1,
                data.getDayOfMonth());
        seletor.show();
    }

    private void definirVencimento(LocalDate data) {
        dataVencimentoSelecionada = data.toString();
        campoVencimento.setText(FormatoFinanceiro.formatStoredDate(dataVencimentoSelecionada));
    }

    private void salvarConta() {
        String descricao = campoDescricao.getText().toString().trim();
        if (descricao.isEmpty()) {
            campoDescricao.setError(getString(R.string.validation_description));
            return;
        }

        long valorEmCentavos;
        try {
            valorEmCentavos = FormatoFinanceiro.parseAmountToCents(campoValor.getText().toString());
        } catch (IllegalArgumentException error) {
            campoValor.setError(getString(R.string.validation_amount));
            return;
        }

        String vencimento;
        try {
            vencimento = FormatoFinanceiro.parseDisplayDate(campoVencimento.getText().toString());
        } catch (IllegalArgumentException error) {
            Toast.makeText(this, R.string.validation_date, Toast.LENGTH_SHORT).show();
            return;
        }

        String categoriaSelecionada = campoCategoria.getSelectedItem().toString();
        // Como o protótipo não tem status, definimos como Pendente por padrão para novos, ou mantemos o que estava na edição
        String situacao = (contaEditando != null) ? contaEditando.status : TipoConta.PENDENTE;

        botaoSalvar.setEnabled(false);

        if (contaEditando != null) {
            contaEditando.description = descricao;
            contaEditando.amountInCents = valorEmCentavos;
            contaEditando.dueDate = vencimento;
            contaEditando.category = categoriaSelecionada;
            contaEditando.status = situacao;

            repositorio.atualizar(contaEditando, new RetornoRepositorio<Void>() {
                @Override
                public void onSuccess(Void result) {
                    finish();
                }
                @Override
                public void onError(Exception erro) {
                    botaoSalvar.setEnabled(true);
                    Toast.makeText(NovaConta.this, R.string.save_error, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            SessaoUsuario sessao = new SessaoUsuario(this);
            Conta novaConta = new Conta(
                    descricao,
                    valorEmCentavos,
                    vencimento,
                    categoriaSelecionada,
                    tipoConta,
                    situacao,
                    sessao.obterEmail(),
                    sessao.obterNome());

            repositorio.inserir(novaConta, new RetornoRepositorio<Long>() {
                @Override
                public void onSuccess(Long id) {
                    finish();
                }
                @Override
                public void onError(Exception erro) {
                    botaoSalvar.setEnabled(true);
                    Toast.makeText(NovaConta.this, R.string.save_error, Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}