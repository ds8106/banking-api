# Guia de Testes: API Bancária Simplificada (Java + Spring Boot)

Este documento fornece um passo a passo completo sobre como executar e validar os testes do projeto em diferentes ambientes e formatos.

---

## 📌 Sumário

1. [Visão Geral da Estratégia de Testes](#1-visão-geral-da-estratégia-de-testes)
2. [Executando Testes Unitários com JUnit 5](#2-executando-testes-unitários-com-junit-5)
   - [2.1 Via IntelliJ IDEA](#21-via-intellij-idea)
   - [2.2 Via Terminal (Maven)](#22-via-terminal-maven)
3. [Executando Testes de Integração e HTTP Reais](#3-executando-testes-de-integração-e-http-reais)
   - [3.1 Inicializando a Aplicação](#31-inicializando-a-aplicação)
   - [3.2 Sequência de Testes HTTP (Script Completo)](#32-sequência-de-testes-http-script-completo)
4. [Validando Cobertura e Resultados Esperados](#4-validando-cobertura-e-resultados-esperados)

---

## 1. Visão Geral da Estratégia de Testes

O projeto conta com duas formas de validação:
- **Testes Unitários:** Focados em validar isoladamente as regras de negócio (`AccountServiceTest`) sem levantar a infraestrutura Spring ou conexões de rede, garantindo alta velocidade e feedback rápido.
- **Testes de Integração Manual (HTTP):** Execução do fluxo e do script de chamadas REST via clientes HTTP (cURL, Postman, Insomnia) com a aplicação em execução.

---

## 2. Executando Testes Unitários com JUnit 5

A classe `AccountServiceTest` está localizada no caminho:
`src/test/java/com/example/banking/service/AccountServiceTest.java`

Ela cobre os seguintes cenários:
- **Casos de Erro:** Busca de saldo de conta inexistente, saque de conta inexistente e transferência a partir de conta de origem inexistente.
- **Alterações de Estado:** Depósito, depósitos sequenciais cumulativos, saque e reset do estado.
- **Fluxos Completo:** Fluxo ponta a ponta de transferência entre contas com validação de ambos os saldos.

### 2.1 Via IntelliJ IDEA

1. Abra o projeto no **IntelliJ IDEA**.
2. No painel **Project** à esquerda, navegue até `src/test/java/com/example/banking/service/AccountServiceTest.java`.
3. Clique com o botão direito do mouse sobre a classe `AccountServiceTest` e selecione **Run 'AccountServiceTest'** (ou pressione `Ctrl + Shift + F10` / `Cmd + Shift + R`).
4. A aba de resultados exibirá o indicador verde informando que todos os 8 testes unitários foram executados e aprovados.

### 2.2 Via Terminal (Maven)

No terminal da sua IDE ou sistema operacional, na raiz do projeto `banking-api`, execute:

```bash
mvn test
```

---

## 3. Executando Testes de Integração e HTTP Reais

### 3.1 Inicializando a Aplicação

Antes de disparar os testes de requisições HTTP, inicie o servidor Spring Boot:

- **Via IntelliJ IDEA:** Abra `BankingApplication.java` (`src/main/java/com/example/banking/BankingApplication.java`) e clique em **Run**.
- **Via Terminal (Maven):**
  ```bash
  mvn spring-boot:run
  ```

A aplicação estará disponível em `http://localhost:8080`.

---

### 3.2 Sequência de Testes HTTP (Script Completo)

Siga os passos na ordem exata para replicar o script de testes:

#### 1. Reset do Estado Inicial
```bash
curl -X POST http://localhost:8080/reset
```
- **Status:** `200 OK`
- **Corpo da Resposta:** `OK`

#### 2. Consultar Saldo de Conta Inexistente
```bash
curl -i -X GET "http://localhost:8080/balance?account_id=1234"
```
- **Status:** `404 Not Found`
- **Corpo da Resposta:** `0`

#### 3. Criar Conta com Saldo Inicial (Depósito)
```bash
curl -i -X POST http://localhost:8080/event \
  -H "Content-Type: application/json" \
  -d '{"type":"deposit", "destination":"100", "amount":10}'
```
- **Status:** `201 Created`
- **Corpo da Resposta:** `{"destination":{"id":"100","balance":10}}`

#### 4. Depositar em Conta Existente
```bash
curl -i -X POST http://localhost:8080/event \
  -H "Content-Type: application/json" \
  -d '{"type":"deposit", "destination":"100", "amount":10}'
```
- **Status:** `201 Created`
- **Corpo da Resposta:** `{"destination":{"id":"100","balance":20}}`

#### 5. Consultar Saldo de Conta Existente
```bash
curl -i -X GET "http://localhost:8080/balance?account_id=100"
```
- **Status:** `200 OK`
- **Corpo da Resposta:** `20`

#### 6. Sacar de Conta Inexistente
```bash
curl -i -X POST http://localhost:8080/event \
  -H "Content-Type: application/json" \
  -d '{"type":"withdraw", "origin":"200", "amount":10}'
```
- **Status:** `404 Not Found`
- **Corpo da Resposta:** `0`

#### 7. Sacar de Conta Existente
```bash
curl -i -X POST http://localhost:8080/event \
  -H "Content-Type: application/json" \
  -d '{"type":"withdraw", "origin":"100", "amount":5}'
```
- **Status:** `201 Created`
- **Corpo da Resposta:** `{"origin":{"id":"100","balance":15}}`

#### 8. Transferir entre Contas
```bash
curl -i -X POST http://localhost:8080/event \
  -H "Content-Type: application/json" \
  -d '{"type":"transfer", "origin":"100", "amount":15, "destination":"300"}'
```
- **Status:** `201 Created`
- **Corpo da Resposta:** `{"origin":{"id":"100","balance":0},"destination":{"id":"300","balance":15}}`

#### 9. Transferir a Partir de Conta Inexistente
```bash
curl -i -X POST http://localhost:8080/event \
  -H "Content-Type: application/json" \
  -d '{"type":"transfer", "origin":"200", "amount":15, "destination":"300"}'
```
- **Status:** `404 Not Found`
- **Corpo da Resposta:** `0`

---

## 4. Validando Cobertura e Resultados Esperados

Tabela de resumo das validações esperadas:

| Operação | Endpoint | Condição | Status HTTP | Resposta Esperada |
| :--- | :--- | :--- | :--- | :--- |
| **Reset** | `POST /reset` | Sempre | `200 OK` | `"OK"` |
| **Balance** | `GET /balance` | Conta Inexistente | `404 Not Found` | `0` |
| **Balance** | `GET /balance` | Conta Existente | `200 OK` | `{balance}` |
| **Event** | `POST /event` | Deposit (nova / existente) | `201 Created` | `{"destination": {...}}` |
| **Event** | `POST /event` | Withdraw (existente) | `201 Created` | `{"origin": {...}}` |
| **Event** | `POST /event` | Withdraw (inexistente) | `404 Not Found` | `0` |
| **Event** | `POST /event` | Transfer (origem existente) | `201 Created` | `{"origin": {...}, "destination": {...}}` |
| **Event** | `POST /event` | Transfer (origem inexistente) | `404 Not Found` | `0` |