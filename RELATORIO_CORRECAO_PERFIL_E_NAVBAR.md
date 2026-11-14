# Relatório: correção do perfil e navegação inferior

## Resultado

A tela **Editar perfil** foi conectada ao botão do Perfil, criada e registrada no
Manifest. A correção remove a causa de a tela não abrir. Também foi substituída
a navegação inferior anterior por uma `BottomNavigationView` compartilhada e
mais visível. A compilação e o teste instrumentado de abertura e retorno
passaram.

## Causa encontrada

O botão de edição em `activity_perfil.xml` não tinha um ID utilizável nem um
listener na `PerfilActivity`. Além disso, não existiam uma Activity e um layout
para edição, e a tela não estava registrada no `AndroidManifest.xml`. Portanto,
o fluxo de navegação não tinha destino implementado.

## Correção do perfil

- Criada `EditarPerfilActivity`, registrada no Manifest e ligada ao botão
  **Editar perfil**.
- Criado formulário com nome, e-mail de contato e preferência de notificações.
  Nome e e-mail são validados antes do envio; e-mail inválido ou nome vazio
  impedem o salvamento e apresentam erro no campo.
- A edição usa `Activity → ViewModel → Repository → Firestore`. O perfil é
  gravado com `SetOptions.merge()` e a sessão local é atualizada após sucesso.
  Ao retornar, o Perfil exibe os dados atualizados.
- A ação de voltar da Toolbar encerra a edição e retorna ao Perfil.
- O e-mail editável é tratado como **e-mail de contato**. O e-mail de
  autenticação permanece separado para preservar a identidade da conta.
- A gravação mantém os dados do grupo familiar e salva a preferência de
  notificações na nuvem e na sessão local.

## Navegação inferior

A antiga barra com ícones foi substituída por uma `BottomNavigationView` com
destinos compartilhados para Início, Receitas, Despesas, Calendário e Perfil.
O roteamento centralizado mantém os IDs de navegação e seleciona o destino
correspondente; Receitas e Despesas abrem a lista com o tipo de conta adequado.

| Propriedade | Medida/configuração |
|---|---|
| Altura e altura mínima | 88dp |
| Ícones | Removidos |
| Rótulos | 14sp, sempre visíveis; ativo em negrito |
| Padding vertical dos itens | Centralizado pela barra sem ícones |
| Fundo do item ativo | inset de 7dp nas laterais e 10dp em cima/baixo; cantos de 18dp |
| Fundo da barra | branco, elevação de 10dp |
| Contraste/seleção | ativo verde escuro sobre fundo verde claro; inativos cinza |
| Área de toque | barra de 88dp de altura, superior ao mínimo Android de 48dp |

O formulário usa `ScrollView`, largura adaptável (`match_parent`) e dimensões
em `dp`/`sp`. Os itens da barra e o botão de retorno têm descrições acessíveis;
os campos declaram dicas de autofill.

## Arquivos da correção

### Criados

- `app/src/main/java/br/com/meu_controle/EditarPerfilActivity.java`
- `app/src/main/java/br/com/meu_controle/utilitarios/NavegacaoInferior.java`
- `app/src/main/res/layout/activity_editar_perfil.xml`
- `app/src/main/res/menu/menu_bottom_navigation.xml`
- `app/src/main/res/color/nav_item_tint.xml`
- `app/src/main/res/color/nav_item_text_color.xml`
- `app/src/main/res/drawable/bg_nav_item_active.xml`
- `app/src/main/res/drawable/bg_nav_item_selector.xml`
- `app/src/main/res/drawable/ic_nav_back.xml`
- `app/src/main/res/drawable/ic_nav_home.xml`
- `app/src/main/res/drawable/ic_nav_income.xml`
- `app/src/main/res/drawable/ic_nav_expense.xml`
- `app/src/main/res/drawable/ic_nav_calendar.xml`
- `app/src/main/res/drawable/ic_nav_profile.xml`
- `app/src/androidTest/java/br/com/meu_controle/EditarPerfilNavigationTest.java`

### Modificados para esta correção

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/br/com/meu_controle/PerfilActivity.java`
- `app/src/main/java/br/com/meu_controle/MainActivity.java`
- `app/src/main/java/br/com/meu_controle/CalendarioActivity.java`
- `app/src/main/java/br/com/meu_controle/GerenciarCategoriasActivity.java`
- `app/src/main/java/br/com/meu_controle/telas/ListaContas.java`
- `app/src/main/java/br/com/meu_controle/dados/local/SessaoUsuario.java`
- `app/src/main/java/br/com/meu_controle/dados/modelo/Usuario.java`
- `app/src/main/java/br/com/meu_controle/dados/repositorio/PerfilUsuarioRepository.java`
- `app/src/main/java/br/com/meu_controle/viewmodels/PerfilUsuarioViewModel.java`
- `app/src/main/res/layout/activity_perfil.xml`
- `app/src/main/res/layout/layout_bottom_nav.xml`
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values/themes.xml`

O diretório de trabalho já continha alterações não relacionadas antes desta
correção. A lista acima cobre os arquivos desta funcionalidade e não atribui à
correção as demais alterações existentes.

## Validações e evidências

- **Compilação:** `:app:assembleDebug` concluído com sucesso.
- **Lint:** `:app:lintDebug` concluído com sucesso. Restaram avisos de atualização
  de dependências, pluralização de textos com contagem e alocações em
  `PieChartView`; nenhum impediu a compilação.
- **Navegação Perfil → Edição → Perfil:** teste instrumentado
  `abreEdicaoRetornaAoPerfilEExibeNavegacaoAcessivel` passou no emulador Medium
  Phone, Android 17 (1 teste, 0 falhas). O teste clica no botão real, confirma a
  Activity aberta e os campos, aciona o retorno da Toolbar e confirma o Perfil.
- **Barra/acessibilidade:** o mesmo teste confirma cinco itens, seleção do
  Perfil, descrições acessíveis, visibilidade e altura/ícones mínimos. O
  roteamento dos destinos está centralizado em `NavegacaoInferior`; não foi
  executado um teste instrumentado individual de clique para cada destino.
- **Responsividade:** a implementação usa dimensões escaláveis e rolagem; a
  verificação instrumentada foi feita em um único emulador/configuração. Não
  houve validação em múltiplos tamanhos de tela ou auditoria com TalkBack.
- **Salvamento/persistência:** o código valida os dados e grava por merge no
  Firestore, atualizando a sessão local em sucesso. Não foi possível validar
  uma gravação real ponta a ponta: não foi configurada uma conta Firebase
  autenticada dedicada para o teste.
- **Testes unitários locais:** `:app:testDebugUnitTest` passou (5 testes, 0
  falhas). O `PATH` do ambiente contém aspas desbalanceadas, que quebram a
  inicialização dos workers Java; a execução foi concluída usando um `PATH`
  mínimo apenas no processo de teste, sem alterar a configuração do sistema.
- **Compilação do teste instrumentado:** `:app:compileDebugAndroidTestJavaWithJavac`
  concluído com sucesso. A execução no emulador registrada acima ocorreu antes
  desta retomada; não foi possível repeti-la neste ambiente, que não tem `adb`
  disponível.

Assim, a abertura, o retorno, a exibição da barra e sua acessibilidade básica
foram demonstrados no teste instrumentado. A persistência remota e a navegação
individual em todos os destinos ainda dependem das validações indicadas acima.
