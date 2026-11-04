package br.com.meu_controle;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.List;

import br.com.meu_controle.dados.local.BancoDados;
import br.com.meu_controle.dados.local.SessaoUsuario;
import br.com.meu_controle.dados.modelo.Categoria;
import br.com.meu_controle.dados.modelo.TipoConta;
import br.com.meu_controle.dados.repositorio.RepositorioCategorias;
import br.com.meu_controle.dados.repositorio.RetornoRepositorio;
import br.com.meu_controle.telas.ListaContas;

public class GerenciarCategoriasActivity extends AppCompatActivity {

    private SessaoUsuario sessao;
    private RepositorioCategorias repositorio;
    private LinearLayout listaCategorias;
    private TextView mensagemVazia;

    private String tipoAtual = TipoConta.PAGAR; // aba inicial: Despesas

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gerenciar_categorias);

        sessao = new SessaoUsuario(this);
        repositorio = new RepositorioCategorias(
                BancoDados.obterInstancia(getApplicationContext()).categoriaDao());

        listaCategorias = findViewById(R.id.categorias_container);
        mensagemVazia = findViewById(R.id.empty_categorias_message);

        atualizarEspacoAtivo();
        configurarToggle();
        configurarNavegacaoInferior();

        findViewById(R.id.new_categoria_button)
                .setOnClickListener(v -> abrirDialogCategoria(null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (repositorio != null) {
            carregarCategorias();
        }
    }

    private void atualizarEspacoAtivo() {
        TextView nome = findViewById(R.id.active_space_name);
        TextView detalhe = findViewById(R.id.active_space_detail);
        nome.setText(sessao.perfilFamiliar() ? "Família" : "Meu espaço individual");
        detalhe.setText(sessao.perfilFamiliar()
                ? (sessao.obterMembros().size() + 1) + " membros"
                : "Privado");
    }

    private void configurarToggle() {
        MaterialButtonToggleGroup toggle = findViewById(R.id.toggle_tipo);
        toggle.check(R.id.toggle_despesas);
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            tipoAtual = checkedId == R.id.toggle_receitas
                    ? TipoConta.RECEBER : TipoConta.PAGAR;
            carregarCategorias();
        });
    }

    private void carregarCategorias() {
        repositorio.buscarPorTipo(tipoAtual, sessao.obterEmail(), sessao.perfilFamiliar(),
                new RetornoRepositorio<List<Categoria>>() {
                    @Override
                    public void onSuccess(List<Categoria> categorias) {
                        renderizar(categorias);
                    }
                    @Override
                    public void onError(Exception error) {
                        Toast.makeText(GerenciarCategoriasActivity.this,
                                R.string.load_error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void renderizar(List<Categoria> categorias) {
        listaCategorias.removeAllViews();
        mensagemVazia.setVisibility(categorias.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Categoria categoria : categorias) {
            View linha = inflater.inflate(R.layout.item_categoria, listaCategorias, false);
            preencherLinha(linha, categoria);
            listaCategorias.addView(linha);
        }
    }

    private void preencherLinha(View linha, Categoria categoria) {
        TextView nome = linha.findViewById(R.id.categoria_nome);
        TextView badge = linha.findViewById(R.id.categoria_badge_padrao);
        TextView btnEditar = linha.findViewById(R.id.btn_edit_categoria);
        TextView btnExcluir = linha.findViewById(R.id.btn_delete_categoria);

        nome.setText(categoria.nome);
        badge.setVisibility(categoria.isPadrao ? View.VISIBLE : View.GONE);

        int visibilidade = categoria.isPadrao ? View.GONE : View.VISIBLE;
        btnEditar.setVisibility(visibilidade);
        btnExcluir.setVisibility(visibilidade);

        btnEditar.setOnClickListener(v -> abrirDialogCategoria(categoria));
        btnExcluir.setOnClickListener(v -> confirmarExclusao(categoria));
    }

    private void abrirDialogCategoria(Categoria categoriaExistente) {
        View conteudo = getLayoutInflater().inflate(R.layout.dialog_categoria, null);
        EditText inputNome = conteudo.findViewById(R.id.categoria_nome_input);

        boolean editando = categoriaExistente != null;
        if (editando) inputNome.setText(categoriaExistente.nome);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(editando ? "Editar categoria" : "Nova categoria")
                .setView(conteudo)
                .setPositiveButton("Salvar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String nome = inputNome.getText().toString().trim();
                    if (nome.isEmpty()) {
                        inputNome.setError("Informe o nome da categoria");
                        return;
                    }
                    if (editando) {
                        categoriaExistente.nome = nome;
                        repositorio.atualizar(categoriaExistente, new RetornoRepositorio<Void>() {
                            @Override public void onSuccess(Void r) {
                                dialog.dismiss();
                                carregarCategorias();
                                Toast.makeText(GerenciarCategoriasActivity.this,
                                        "Categoria atualizada", Toast.LENGTH_SHORT).show();
                            }
                            @Override public void onError(Exception e) {
                                Toast.makeText(GerenciarCategoriasActivity.this,
                                        e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        Categoria nova = new Categoria(nome, tipoAtual, sessao.obterEmail());
                        repositorio.inserir(nova, new RetornoRepositorio<Long>() {
                            @Override public void onSuccess(Long id) {
                                dialog.dismiss();
                                carregarCategorias();
                                Toast.makeText(GerenciarCategoriasActivity.this,
                                        "Categoria criada", Toast.LENGTH_SHORT).show();
                            }
                            @Override public void onError(Exception e) {
                                Toast.makeText(GerenciarCategoriasActivity.this,
                                        e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                })
        );
        dialog.show();
    }

    private void confirmarExclusao(Categoria categoria) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir categoria")
                .setMessage("Deseja excluir '" + categoria.nome + "'?")
                .setPositiveButton("Sim", (d, which) ->
                        repositorio.excluir(categoria, new RetornoRepositorio<Void>() {
                            @Override public void onSuccess(Void r) {
                                carregarCategorias();
                                Toast.makeText(GerenciarCategoriasActivity.this,
                                        "Categoria excluída", Toast.LENGTH_SHORT).show();
                            }
                            @Override public void onError(Exception e) {
                                Toast.makeText(GerenciarCategoriasActivity.this,
                                        e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        })
                )
                .setNegativeButton("Não", null)
                .show();
    }

    private void configurarNavegacaoInferior() {
        findViewById(R.id.nav_dashboard).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class)); finish();
        });
        findViewById(R.id.nav_receitas).setOnClickListener(v -> {
            Intent i = new Intent(this, ListaContas.class);
            i.putExtra(ListaContas.EXTRA_TIPO_CONTA, TipoConta.RECEBER);
            startActivity(i); finish();
        });
        findViewById(R.id.nav_despesas).setOnClickListener(v -> {
            Intent i = new Intent(this, ListaContas.class);
            i.putExtra(ListaContas.EXTRA_TIPO_CONTA, TipoConta.PAGAR);
            startActivity(i); finish();
        });
        findViewById(R.id.nav_calendario).setOnClickListener(v -> {
            startActivity(new Intent(this, CalendarioActivity.class)); finish();
        });
        findViewById(R.id.nav_perfil).setOnClickListener(v -> {
            startActivity(new Intent(this, PerfilActivity.class)); finish();
        });

        TextView navAtivo = findViewById(R.id.nav_perfil);
        navAtivo.setTextColor(getColor(R.color.receivables_accent_dark));
        navAtivo.setTypeface(null, Typeface.BOLD);
    }
}