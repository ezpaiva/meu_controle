package br.com.meu_controle.telas;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import br.com.meu_controle.R;
import br.com.meu_controle.dados.local.BancoDados;
import br.com.meu_controle.dados.modelo.Conta;
import br.com.meu_controle.dados.modelo.TipoConta;
import br.com.meu_controle.dados.repositorio.RepositorioContas;
import br.com.meu_controle.dados.repositorio.RetornoRepositorio;
import br.com.meu_controle.utilitarios.FormatoFinanceiro;

import java.time.LocalDate;

public class NovaConta extends AppCompatActivity {

    private String tipoConta;
    private String dataVencimentoSelecionada;
    private RepositorioContas repositorio;
    private EditText campoDescricao;
    private EditText campoValor;
    private TextView campoVencimento;
    private Spinner campoCategoria;
    private Spinner campoSituacao;
    private Button botaoSalvar;

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

        repositorio = new RepositorioContas(
                BancoDados.obterInstancia(getApplicationContext()).contaDao());
        campoDescricao = findViewById(R.id.description_input);
        campoValor = findViewById(R.id.amount_input);
        campoVencimento = findViewById(R.id.due_date_input);
        campoCategoria = findViewById(R.id.category_input);
        campoSituacao = findViewById(R.id.status_input);
        botaoSalvar = findViewById(R.id.save_account_button);

        TextView titulo = findViewById(R.id.new_account_title);
        titulo.setText(TipoConta.RECEBER.equals(tipoConta)
                ? R.string.new_receivable_title
                : R.string.new_payable_title);
        campoDescricao.setHint(TipoConta.RECEBER.equals(tipoConta)
                ? R.string.receivable_description_hint
                : R.string.payable_description_hint);

        configurarListas();
        definirVencimento(LocalDate.now());

        campoVencimento.setOnClickListener(view -> mostrarSeletorData());
        botaoSalvar.setOnClickListener(view -> salvarConta());
        Button botaoCancelar = findViewById(R.id.cancel_account_button);
        botaoCancelar.setOnClickListener(view -> finish());
    }

    private void configurarListas() {
        int categorias = TipoConta.RECEBER.equals(tipoConta)
                ? R.array.income_categories
                : R.array.expense_categories;
        ArrayAdapter<CharSequence> adaptadorCategorias = ArrayAdapter.createFromResource(
                this, categorias, android.R.layout.simple_spinner_item);
        adaptadorCategorias.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        campoCategoria.setAdapter(adaptadorCategorias);

        String situacaoConcluida = getString(TipoConta.RECEBER.equals(tipoConta)
                ? R.string.status_completed_receivable
                : R.string.status_completed_payable);
        String[] situacoes = {getString(R.string.status_pending), situacaoConcluida};
        ArrayAdapter<String> adaptadorSituacao = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, situacoes);
        adaptadorSituacao.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        campoSituacao.setAdapter(adaptadorSituacao);
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
            valorEmCentavos = FormatoFinanceiro.parseAmountToCents(
                    campoValor.getText().toString());
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

        String situacao = campoSituacao.getSelectedItemPosition() == 0
                ? TipoConta.PENDENTE
                : TipoConta.CONCLUIDA;
        Conta conta = new Conta(
                descricao,
                valorEmCentavos,
                vencimento,
                campoCategoria.getSelectedItem().toString(),
                tipoConta,
                situacao);

        botaoSalvar.setEnabled(false);
        repositorio.inserir(conta, new RetornoRepositorio<Long>() {
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
