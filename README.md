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
- **Banco de Dados (a definir)** — persistência local;
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