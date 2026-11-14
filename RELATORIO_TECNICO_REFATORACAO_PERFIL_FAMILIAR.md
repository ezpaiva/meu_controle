# Relatório técnico — Perfil Familiar

## Resultado e validação

Foram implementados os fluxos de Perfil Familiar e de conta descritos em `REFATORACAO_PERFIL_FAMILIAR_COMPLETA.md`, preservando as alterações que já existiam no worktree. A compilação do app e do APK de testes instrumentados, os testes unitários e o lint terminaram com sucesso. Os testes unitários executados foram 5/5; o lint reportou 0 erros e 188 avisos.

O teste instrumentado foi compilado, mas não executado: não há `adb`/dispositivo disponível. Também não foi possível validar operações reais de Firebase Auth e Firestore, regras de segurança ou cenários entre dispositivos nesta sessão. Portanto, os fluxos remotos descritos abaixo estão implementados no código, mas não têm evidência de execução ponta a ponta.

## 1. Arquivos criados

- `app/src/androidTest/java/br/com/meu_controle/EditarPerfilNavigationTest.java`
- `app/src/main/java/br/com/meu_controle/EditarPerfilActivity.java`
- `app/src/main/java/br/com/meu_controle/dados/modelo/ConviteFamiliar.java`
- `app/src/main/java/br/com/meu_controle/dados/modelo/GrupoFamiliar.java`
- `app/src/main/java/br/com/meu_controle/dados/modelo/Usuario.java`
- `app/src/main/java/br/com/meu_controle/dados/remoto/SincronizadorFamiliar.java`
- `app/src/main/java/br/com/meu_controle/dados/repositorio/ContaUsuarioRepository.java`
- `app/src/main/java/br/com/meu_controle/dados/repositorio/GrupoFamiliarRepository.java`
- `app/src/main/java/br/com/meu_controle/dados/repositorio/PerfilUsuarioRepository.java`
- `app/src/main/java/br/com/meu_controle/utilitarios/NavegacaoInferior.java`
- `app/src/main/java/br/com/meu_controle/viewmodels/ContaUsuarioViewModel.java`
- `app/src/main/java/br/com/meu_controle/viewmodels/GrupoFamiliarViewModel.java`
- `app/src/main/java/br/com/meu_controle/viewmodels/PerfilUsuarioViewModel.java`
- `app/src/main/java/br/com/meu_controle/viewmodels/SincronizacaoViewModel.java`
- `app/src/main/res/color/nav_item_text_color.xml`
- `app/src/main/res/color/nav_item_tint.xml`
- `app/src/main/res/drawable/bg_nav_item_active.xml`
- `app/src/main/res/drawable/bg_nav_item_selector.xml`
- `app/src/main/res/drawable/ic_nav_back.xml`
- `app/src/main/res/drawable/ic_nav_calendar.xml`
- `app/src/main/res/drawable/ic_nav_expense.xml`
- `app/src/main/res/drawable/ic_nav_home.xml`
- `app/src/main/res/drawable/ic_nav_income.xml`
- `app/src/main/res/drawable/ic_nav_profile.xml`
- `app/src/main/res/layout/activity_editar_perfil.xml`
- `app/src/main/res/menu/menu_bottom_navigation.xml`
- `RELATORIO_TECNICO_REFATORACAO_PERFIL_FAMILIAR.md` (este relatório)

## 2. Arquivos alterados

- Aplicação e navegação: `app/src/main/AndroidManifest.xml`, `CadastroActivity.java`, `CalendarioActivity.java`, `GerenciarCategoriasActivity.java`, `LoginActivity.java`, `MainActivity.java`, `PerfilActivity.java`.
- Banco e sincronização: `BancoDados.java`, `CategoriaDao.java`, `ContaDao.java`, `LembreteDao.java`, `SessaoUsuario.java`, `Categoria.java`, `Conta.java`, `Lembrete.java`, `SincronizadorContas.java`, `RepositorioCategorias.java`, `RepositorioContas.java`, `RepositorioLembretes.java`.
- Lembretes e telas: `BootReceiver.java`, `NotificationReceiver.java`, `NotificationScheduler.java`, `LembreteAdapter.java`, `LembretesActivity.java`, `ListaContas.java`, `NovaConta.java`.
- Recursos: `activity_perfil.xml`, `item_lembrete.xml`, `layout_bottom_nav.xml`, `colors.xml`, `strings.xml`, `themes.xml`.
- O worktree já continha alterações não commitadas antes desta etapa; elas foram preservadas. `.idea/gradle.xml` também aparece alterado no status, mas não faz parte da implementação funcional documentada aqui.

## 3. Entidades, DAOs, repositórios e ViewModels

- Entidades existentes alteradas: `Conta`, `Categoria` e `Lembrete`, com escopo de grupo/identidade remota e campos de sincronização.
- Modelos Firestore criados: `Usuario`, `GrupoFamiliar` e `ConviteFamiliar`. Não são entidades Room.
- DAOs alterados: `ContaDao`, `CategoriaDao` e `LembreteDao`, incluindo consultas por escopo familiar, atualização de e-mail e limpeza de cache/dados do proprietário ou grupo.
- Repositórios criados: `PerfilUsuarioRepository`, `GrupoFamiliarRepository` e `ContaUsuarioRepository`.
- Repositórios alterados: `RepositorioContas`, `RepositorioCategorias` e `RepositorioLembretes`.
- ViewModels criados: `PerfilUsuarioViewModel`, `GrupoFamiliarViewModel`, `ContaUsuarioViewModel` e `SincronizacaoViewModel`. `LembreteViewModel` foi alterado.
- Sincronização: `SincronizadorContas` foi ajustado e `SincronizadorFamiliar` criado para categorias e lembretes do grupo.

## 4. Migração Room

`BancoDados` foi atualizado da versão 7 para a 8 com a migração `MIGRACAO_7_8`. Ela adiciona `groupId` às contas, categorias e lembretes, além dos identificadores/campos de sincronização necessários. O schema compila; não foi exercitada a migração de um banco real de cada versão em dispositivo/emulador.

## 5. Coleções Firestore

- `usuarios/{uid}` e a subcoleção já utilizada `usuarios/{uid}/contas`.
- `grupos_familiares/{groupId}` e subcoleções `contas`, `categorias` e `lembretes`.
- `convites_familiares`, com grupo, remetente/destinatário por UID e e-mail, estado e datas.

Não foram criadas nem implantadas regras Firestore neste checkout.

## 6. Fluxos implementados

- Bottom navigation apenas textual, com área de toque maior, contraste e destaque do item ativo. Opções sem ação (“Configurações” e “Ajuda”) e criação manual do grupo no Perfil foram removidas.
- O primeiro lançamento familiar solicita o nome do grupo e cria, em transação Firestore, o grupo, o vínculo do usuário e o documento remoto da primeira conta.
- Convites são associados a e-mail e UID, aparecem ao destinatário no login e podem ser aceitos ou recusados; as transações verificam status, identidade e vínculo antes de atualizar o grupo.
- Contas, categorias e lembretes usam escopo de grupo para sincronização; dashboard e calendário consomem os dados do escopo atual.
- Sair do grupo remove o vínculo remoto, limpa os dados e notificações compartilhados locais e transfere a administração se o criador sair. Se não restarem membros, tenta apagar subcoleções, convites pendentes e grupo.
- Exclusão de conta solicita confirmação inicial, a palavra `EXCLUIR` e senha; reautentica, remove a associação familiar, transfere a propriedade dos dados compartilhados quando necessário, exclui convites/dados locais e remotos e encerra a sessão.
- Alteração de e-mail exige senha e confirmação final; atualiza Firebase Auth, sessão, e-mail proprietário local, perfil remoto, convites pendentes e documentos do usuário/grupo. A UI retorna ao login em caso de sucesso.

O convite é endereçado ao e-mail cadastrado e apresentado dentro do app no login. Não há integração com serviço de envio de e-mail externo, portanto não é enviada nem comprovada uma mensagem à caixa postal.

## 7. Problemas corrigidos

- Erro de compilação causado por um caractere `0` solto em `FeriadosApi.java:46` (corrigido antes desta etapa).
- Navegação inferior com ícones, baixa visibilidade e opções sem funcionalidade.
- Criação manual de grupo desacoplada da primeira movimentação familiar.
- Convite sem estado de resposta/validação transacional e ausência de fluxos de recusa e saída.
- Escopo e limpeza local de contas, categorias e lembretes familiares.
- Ausência de fluxo completo de alteração de e-mail e exclusão autenticada da conta.
- Falha parcial na troca de e-mail poderia deixar os dados locais com o endereço antigo; a atualização local agora é tentada independentemente da sincronização remota, e a falha parcial é reportada explicitamente.

## 8. Evidências e cenários

| Verificação | Evidência | Resultado |
|---|---|---|
| Compilação do app | `:app:assembleDebug` | Passou |
| Testes unitários | `:app:testDebugUnitTest`; 1 teste de `RepositorioLembretesTest` e 4 de `NotificationSchedulerTest` | 5 passaram |
| Lint | `:app:lintDebug` | Passou; 0 erros, 188 avisos |
| Compilação do teste instrumentado | `:app:assembleDebugAndroidTest` | Passou |
| Execução do teste instrumentado | Sem `adb`/dispositivo disponível | Não executado |
| Migração Room em banco real | Sem emulador/dispositivo e banco de teste de versões anteriores | Não executada |
| Firestore/Auth, convites e cenários entre membros | Sem execução conectada a Firebase/emulador e sem regras implantadas verificáveis | Não executados ponta a ponta |

Assim, os cenários funcionais exigidos têm implementação e validação estática/compilação, mas os cenários remotos de criação/aceite/recusa/saída/exclusão/alteração de e-mail, sincronização entre membros e ausência de órfãos ainda precisam de testes de integração conectados.

## 9. Impacto nos requisitos R1–R14

O arquivo de especificação fornecido não nomeia requisitos como R1–R14. Para rastreabilidade, a tabela abaixo agrupa o checklist do pedido em 14 frentes; não representa IDs existentes no documento.

| ID de rastreio | Requisito agrupado | Situação |
|---|---|---|
| R1 | Navegação inferior textual, visível e com seleção clara | Implementado; teste instrumentado compilado |
| R2 | Remover controles sem funcionalidade | Implementado para as opções identificadas no Perfil |
| R3 | Criar grupo somente na primeira movimentação familiar | Implementado com transação Firestore |
| R4 | Adicionar membro por e-mail | Convite registrado e apresentado no app; envio externo de e-mail não integrado |
| R5 | Aceitar ou recusar convite | Implementado com verificação de UID/e-mail/status |
| R6 | Compartilhar contas e categorias | Implementado no escopo e sincronizador do grupo; sem teste Firebase E2E |
| R7 | Compartilhar dashboard e calendário | Implementado por escopo de sessão/grupo; sem teste Firebase E2E |
| R8 | Compartilhar e sincronizar lembretes | Implementado no escopo do grupo; sem teste entre dispositivos |
| R9 | Sair do grupo e limpar dados compartilhados locais | Implementado; remoto condicionado à disponibilidade/permissões do Firestore |
| R10 | Excluir conta com dupla confirmação e `EXCLUIR` | Implementado com reautenticação; operações remotas sequenciais |
| R11 | Remover usuário/convidados e dados associados na exclusão | Implementado no cliente; não é uma operação global atômica entre Auth, Firestore e Room |
| R12 | Alterar e-mail com confirmação e sincronização | Implementado; falhas parciais são informadas, sem teste conectado |
| R13 | Consistência de entidades, Room e sincronização | Implementado com schema v8 e migração 7→8; migração real não exercitada |
| R14 | Segurança, integridade e validação ponta a ponta | Parcial: sem regras Firestore/server-side e sem ambiente de integração para comprovar ausência de órfãos |

## 10. Limitações e melhorias futuras

Room, Firestore e Firebase Auth não compartilham uma transação. As exclusões e a alteração de e-mail são sequências de operações; falhas intermediárias podem deixar trabalho remoto pendente. Para garantir consistência forte, recomenda-se mover operações administrativas para Cloud Functions/backend idempotente, adicionar regras Firestore que validem UID e associação ao grupo e testar esses fluxos com Firebase Emulator Suite. Para entrega por caixa postal, integrar um provedor de e-mail ou extensão do Firebase, mantendo a notificação in-app como alternativa.
