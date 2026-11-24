package br.com.meu_controle;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.List;

import br.com.meu_controle.dados.local.SessaoUsuario;
import br.com.meu_controle.dados.modelo.Categoria;
import br.com.meu_controle.dados.modelo.TipoConta;
import br.com.meu_controle.dados.repositorio.RetornoRepositorio;
import br.com.meu_controle.telas.ListaContas;
import br.com.meu_controle.utilitarios.NavegacaoInferior;
import br.com.meu_controle.viewmodels.FinancasViewModel;
import br.com.meu_controle.utilitarios.BuscaGlobal;

public class GerenciarCategoriasActivity extends AppCompatActivity {

    private SessaoUsuario sessao;
    private FinancasViewModel financasViewModel;
    private LinearLayout listaCategorias;
    private TextView mensagemVazia;

    private String tipoAtual = TipoConta.PAGAR; // aba inicial: Despesas
    private String buscaTexto = "";
    private final java.util.List<Categoria> todasCategorias = new java.util.ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gerenciar_categorias);

        sessao = new SessaoUsuario(this);
        financasViewModel = new ViewModelProvider(this).get(FinancasViewModel.class);

        listaCategorias = findViewById(R.id.categorias_container);
        mensagemVazia = findViewById(R.id.empty_categorias_message);

        // ADICIONADO: Ação do botão voltar
        findViewById(R.id.btn_voltar).setOnClickListener(v -> finish());

        atualizarEspacoAtivo();
        configurarToggle();

        // REMOVIDO: configurarNavegacaoInferior();

        findViewById(R.id.new_categoria_button)
                .setOnClickListener(v -> abrirDialogCategoria(null));
        com.google.android.material.textfield.TextInputEditText busca =
                findViewById(R.id.category_search_input);
        busca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                buscaTexto = s.toString();
                renderizar(BuscaGlobal.filtrarCategorias(todasCategorias, buscaTexto));
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (financasViewModel != null) {
            carregarCategorias();
        }
    }

    private void atualizarEspacoAtivo() {
        TextView nome = findViewById(R.id.active_space_name);
        TextView detalhe = findViewById(R.id.active_space_detail);
        nome.setText(sessao.perfilFamiliar() ? "Família" : "Meu espaço individual");
        detalhe.setText(sessao.perfilFamiliar()
                ? sessao.obterMembros().size() + " membros"
                : "Privado");
    }

    private void configurarToggle() {
        MaterialButtonToggleGroup toggle = findViewById(R.id.toggle_tipo);
        MaterialButton btnNovaCategoria = findViewById(R.id.new_categoria_button);

        // Define a aba de despesas como inicial e já pinta o botão de vermelho
        toggle.check(R.id.toggle_despesas);
        atualizarCorBotaoNovaCategoria(btnNovaCategoria, TipoConta.PAGAR);

        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            tipoAtual = checkedId == R.id.toggle_receitas
                    ? TipoConta.RECEBER : TipoConta.PAGAR;

            // Muda a cor do botão "+ Nova categoria" dependendo da aba
            atualizarCorBotaoNovaCategoria(btnNovaCategoria, tipoAtual);

            carregarCategorias();
        });
    }

    // Método auxiliar para mudar a cor do botão
    private void atualizarCorBotaoNovaCategoria(MaterialButton botao, String tipo) {
        int cor = getColor(tipo.equals(TipoConta.PAGAR)
                ? R.color.payables_accent
                : R.color.green_primary);

        botao.setBackgroundTintList(android.content.res.ColorStateList.valueOf(cor));
        botao.setTextColor(getColor(R.color.white));
    }

    private void carregarCategorias() {
        financasViewModel.buscarCategorias(tipoAtual, sessao.obterEmail(), sessao.perfilFamiliar(),
                sessao.obterGrupoId(),
                new RetornoRepositorio<List<Categoria>>() {
                    @Override
                    public void onSuccess(List<Categoria> categorias) {
                        todasCategorias.clear();
                        todasCategorias.addAll(categorias);
                        renderizar(BuscaGlobal.filtrarCategorias(todasCategorias, buscaTexto));
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
                        financasViewModel.atualizarCategoria(categoriaExistente, new RetornoRepositorio<Void>() {
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
                        nova.groupId = sessao.perfilFamiliar() ? sessao.obterGrupoId() : null;
                        financasViewModel.inserirCategoria(nova, new RetornoRepositorio<Long>() {
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
                        financasViewModel.excluirCategoria(categoria, new RetornoRepositorio<Void>() {
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

}