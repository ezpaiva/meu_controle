# Meu Controle

Aplicativo móvel para controle simples de receitas e despesas pessoais.

## Sobre o projeto

O **Meu Controle** é uma aplicação móvel desenvolvida como parte do Projeto Integrador, atividade curricular voltada à aplicação prática dos conhecimentos desenvolvidos ao longo do período.

O Projeto Integrador tem como proposta o desenvolvimento, em equipe, de uma aplicação móvel de média a alta complexidade, abrangendo o ciclo completo de desenvolvimento de software, desde o levantamento e especificação de requisitos até a implementação, testes, documentação e distribuição.

Dentro desse contexto, o Meu Controle tem como domínio o controle de receitas e despesas pessoais. A aplicação busca oferecer uma forma simples e organizada de registrar e acompanhar movimentações financeiras, utilizando regras de negócio, persistência de dados, integração com serviços externos e recursos nativos do dispositivo.

O desenvolvimento é realizado de forma incremental, com utilização de prototipação, modelagem, controle de versão, documentação técnica e testes.

## Problema

Muitas pessoas têm dificuldade para organizar suas receitas e despesas do dia a dia, acompanhar seus gastos e manter um registro claro de suas movimentações financeiras, especialmente em relação a valores, categorias e datas.

## Solução

O aplicativo oferecerá uma forma simples e organizada de registrar, consultar e acompanhar receitas e despesas pessoais em um único lugar, facilitando o controle das movimentações financeiras do usuário.

## Público-alvo

- Estudantes;
- Trabalhadores;
- Pessoas que desejam organizar e acompanhar suas finanças pessoais.

## Funcionalidades previstas

- Cadastro e login de usuários;
- Controle de acesso por perfil de usuário;
- Cadastro, consulta, edição e exclusão de receitas;
- Cadastro, consulta, edição e exclusão de despesas;
- Classificação das movimentações por categoria;
- Consulta de movimentações por data;
- Calendário financeiro;
- Lembretes de compromissos financeiros;
- Notificações no dispositivo;
- Persistência local dos dados;
- Sincronização de dados;
- Integração com API externa de data/calendário.

## Tecnologias

- **Java** — linguagem principal;
- **Android Studio** — ambiente de desenvolvimento;
- **XML** — construção das interfaces;
- **GitHub** — controle de versão, hospedagem do código e colaboração;
- **Banco de Dados SQLite** — persistência local;
- **API (a definir)** — comunicação com serviços externos.

> As tecnologias e bibliotecas poderão ser atualizadas durante o desenvolvimento do projeto.

## Arquitetura

O projeto será organizado em camadas, buscando separar as responsabilidades da aplicação:

```text
Interface
   ↓
Lógica de apresentação
   ↓
Regras de negócio
   ↓
Persistência e serviços
```
---

## Especificação de Requisitos

### Convenções
- **Identificadores:** `RFxx` para Requisitos Funcionais e `RNFxx` para Requisitos Não Funcionais.
- **Prioridades:** 
  - **Alta:** Indispensável ao funcionamento do aplicativo e aos requisitos mínimos do projeto.
  - **Média:** Importante, mas pode ser entregue em uma etapa posterior.
- **Origem:** `README` (previsto inicialmente) ou `Derivado` (deduzido dos objetivos/fluxos).

---

### Requisitos Funcionais (RF)

| ID | Requisito | Prioridade | Origem |
| :--- | :--- | :--- | :--- |
| **Autenticação e Perfis** | | | |
| **RF01** | O sistema deve permitir o cadastro de usuários informando nome, e-mail e senha. | Alta | README |
| **RF02** | O sistema deve permitir que o usuário faça login e logout. | Alta | README |
| **RF03** | O sistema deve restringir as funcionalidades conforme o perfil do usuário (individual ou familiar/compartilhado). | Alta | README |
| **RF04** | O usuário familiar deve poder convidar ou vincular membros a um grupo compartilhado. | Média | Derivado |
| **Receitas** | | | |
| **RF05** | O sistema deve permitir cadastrar uma receita com valor, descrição, data e categoria. | Alta | README |
| **RF06** | O sistema deve permitir consultar e listar as receitas cadastradas. | Alta | README |
| **RF07** | O sistema deve permitir editar uma receita. | Alta | README |
| **RF08** | O sistema deve permitir excluir uma receita, mediante confirmação. | Alta | README |
| **Despesas** | | | |
| **RF09** | O sistema deve permitir cadastrar uma despesa com valor, descrição, data e categoria. | Alta | README |
| **RF10** | O sistema deve permitir consultar e listar as despesas cadastradas. | Alta | README |
| **RF11** | O sistema deve permitir editar uma despesa. | Alta | README |
| **RF12** | O sistema deve permitir excluir uma despesa, mediante confirmação. | Alta | README |
| **Categorias e Consultas** | | | |
| **RF13** | O sistema deve permitir classificar cada receita e despesa em uma categoria. | Alta | README |
| **RF14** | O sistema deve oferecer categorias padrão e permitir criar, editar e excluir categorias personalizadas. | Média | Derivado |
| **RF15** | O sistema deve permitir consultar movimentações por data ou por período. | Alta | README |
| **RF16** | O sistema deve exibir o resumo financeiro do período (total de receitas, total de despesas e saldo). | Média | Derivado |
| **Calendário e Lembretes** | | | |
| **RF17** | O sistema deve exibir um calendário financeiro com as movimentações e os compromissos de cada dia. | Média | README |
| **RF18** | O sistema deve permitir cadastrar, editar e excluir lembretes de compromissos financeiros (ex: vencimento de contas). | Alta | README |
| **RF19** | O sistema deve disparar notificações no dispositivo na data e hora dos lembretes. | Alta | README |
| **Dados e Integração** | | | |
| **RF20** | O sistema deve armazenar os dados localmente no dispositivo. | Alta | README |
| **RF21** | O sistema deve sincronizar os dados locais com um servidor remoto. | Alta | README |
| **RF22** | O sistema deve consumir uma API externa de data/calendário (ex: feriados nacionais, para sinalizar vencimentos em dias não úteis). | Alta | README |

---

### Requisitos Não Funcionais (RNF)

| ID | Categoria | Requisito | Verificação |
| :--- | :--- | :--- | :--- |
| **RNF01** | Usabilidade | Interface simples e consistente, com navegação estruturada entre as telas (mínimo de 6 telas funcionais) e registro de movimentação em poucos toques. | Teste de uso e conferência com o protótipo. |
| **RNF02** | Desempenho | Consultas e listagens locais devem responder em até 2 segundos; o aplicativo deve abrir em até 3 segundos em dispositivo de entrada. | Medição em dispositivo físico. |
| **RNF03** | Disponibilidade Offline | Todas as operações de CRUD devem funcionar sem internet, com sincronização posterior quando houver conexão. | Teste no modo avião. |
| **RNF04** | Consistência | A sincronização deve tratar conflitos de edição (prevalecendo a alteração mais recente) sem perda de dados. | Edição simultânea do mesmo registro em dois dispositivos. |
| **RNF05** | Segurança | Senhas armazenadas apenas com hash, comunicação com o servidor via HTTPS e dados isolados por usuário ou grupo. | Revisão de código e inspeção do tráfego. |
| **RNF06** | Privacidade | Tratar os dados financeiros conforme a LGPD, coletando apenas o necessário e permitindo excluir a conta e os dados. | Checklist de LGPD e teste de exclusão. |
| **RNF07** | Integridade dos Dados | Valores monetários sem erro de arredondamento (`BigDecimal` ou inteiros em centavos), com validação de campos e datas. | Testes unitários com valores-limite. |
| **RNF08** | Compatibilidade | Funcionar em Android a partir de uma versão mínima definida (API 26+) e em diferentes tamanhos de tela. | Execução em emuladores e dispositivos físicos. |
| **RNF09** | Persistência | Uso do Room (SQLite) como banco de dados local. | Inspeção das entidades e DAOs. |
| **RNF10** | Manutenibilidade | Arquitetura em camadas (interface, lógica de apresentação, regras de negócio e persistência/serviços), em Java e XML. | Revisão de código. |
| **RNF11** | Testabilidade | Regras de negócio cobertas por testes unitários e fluxos principais cobertos por testes de interface. | Relatório de testes do Android Studio. |
| **RNF12** | Versionamento | Código hospedado no GitHub, com histórico de commits de todos os integrantes. | Histórico de contribuições do repositório. |
| **RNF13** | Distribuição | Geração de pacote instalável (APK ou AAB), com execução comprovada em dispositivo físico. | Instalação e execução em dispositivo físico. |
| **RNF14** | Localização | Textos em português (`pt-BR`), moeda em real (`R$`) e datas no formato `dd/mm/aaaa`. | Inspeção das telas e do `strings.xml`. |

---

## Rastreabilidade com os Requisitos Mínimos do Projeto

| Requisito Mínimo do Projeto Integrador | Atendido Por |
| :--- | :--- |
| **Mínimo de 6 telas funcionais com navegação estruturada** | `RNF01`, `RF01` a `RF19` (Login/Cadastro, Receitas, Despesas, Categorias, Calendário, Lembretes e Resumo) |
| **CRUD completo em pelo menos 2 entidades** | `RF05` a `RF08` (Receitas) e `RF09` a `RF12` (Despesas) |
| **Consumo de pelo menos 1 serviço ou API externa relevante** | `RF22` |
| **Repositório Git com histórico de contribuições da equipe** | `RNF12` |
| **Autenticação com pelo menos 2 perfis de usuário distintos** | `RF01` a `RF04` |
| **Persistência local e remota com sincronização de dados** | `RF20`, `RF21`, `RNF03`, `RNF04`, `RNF09` |
| **Uso de pelo menos 1 recurso nativo do dispositivo** | `RF19` (Notificações do dispositivo) |
| **Pacote instalável (APK ou AAB) em dispositivo físico** | `RNF13` |

---

## Premissas e Pendências

1. **Servidor Remoto:** O `RF21` exige um backend para sincronização (ex: Firebase ou API própria em Node.js/Java), ainda a ser definido. A escolha afetará os requisitos `RF03`, `RF04` e `RNF05`.
2. **API Externa:** A API de data/calendário do `RF22` precisa ser finalizada e confirmada no ecossistema do projeto.
3. **Requisitos Derivados:** Os requisitos `RF04`, `RF14` e `RF16` foram derivados e devem ser revalidados formalmente pela equipe.
4. **Metas Numéricas:** As metas de tempo de resposta (`RNF02`) e versão mínima do Android (`RNF08`) deverão ser validadas durante os testes em dispositivos físicos.
