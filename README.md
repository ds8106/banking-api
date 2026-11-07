# Documentação Técnica: API Bancária Simplificada em Java (Spring Boot)

Este documento apresenta uma explicação detalhada e passo a passo sobre a estrutura, arquitetura e implementação da API Bancária desenvolvida em Java 17 com Spring Boot.

---

## 📌 Sumário
1. [Visão Geral da Arquitetura](#1-visão-geral-da-arquitetura)
2. [Padrões de Qualidade e Princípios Aplicados](#2-padrões-de-qualidade-e-princípios-aplicados)
3. [Detalhamento do Código-Fonte](#3-detalhamento-do-código-fonte)
   - [3.1 Modelo: Account](#31-modelo-account)
   - [3.2 Exceção Customizada: AccountNotFoundException](#32-exceção-customizada-accountnotfoundexception)
   - [3.3 Camada de Serviço: AccountService](#33-camada-de-serviço-accountservice)
   - [3.4 Camada de Controle: AccountController](#34-camada-de-controle-accountcontroller)
4. [Detalhamento dos Testes Unitários](#4-detalhamento-dos-testes-unitários)
5. [Como Executar o Projeto](#5-como-executar-o-projeto)

---

## 1. Visão Geral da Arquitetura

A aplicação foi projetada para atender aos requisitos de um script de testes bancários REST. Para manter o código simples, performático e sem dependências externas complexas de banco de dados, utilizamos o armazenamento **in-memory** (em memória), garantindo alta velocidade nos testes e execução.

```
+-------------------------------------------------------+
|                 AccountController                     |
|  (Gerencia endpoints HTTP: /reset, /balance, /event) |
+---------------------------+---------------------------+
                            |
                            v
+-------------------------------------------------------+
|                   AccountService                      |
|  (Regras de negócio: credit, debit, transfer, reset)  |
|  (Gerencia o armazenamento Map<String, Account>)     |
+---------------------------+---------------------------+
                            |
                            v
+-------------------------------------------------------+
|                    Account (Model)                    |
|       (Representa a entidade Conta e Saldo)          |
+-------------------------------------------------------+
```

---

## 2. Padrões de Qualidade e Princípios Aplicados

- **SRP (Single Responsibility Principle):**
  - `Account`: Contém apenas a estrutura de dados de uma conta bancária e operações diretas de alteração do próprio saldo (`credit`/`debit`).
  - `AccountService`: Centraliza exclusivamente as regras de negócio das transações financeiras.
  - `AccountController`: Trata única e exclusivamente da camada de comunicação HTTP (parse de parâmetros, conversão de JSON e códigos de resposta HTTP).
- **Tratamento Elegante de Exceções:** Uso da exceção `AccountNotFoundException` para controlar falhas de domínio e traduzi-las diretamente no status HTTP `404 Not Found`.
- **Thread-Safety:** Uso do `ConcurrentHashMap` para garantir concorrência segura na manipulação dos dados em memória.

---

## 3. Detalhamento do Código-Fonte

### 3.1 Modelo: `Account.java`

Representa a entidade de domínio. A classe encapsula os campos `id` e `balance` (saldo), disponibilizando métodos seguros para alterar seu valor de forma controlada.

```java
package com.example.banking.model;

public class Account {
    private String id;
    private int balance;

    public Account(String id, int balance) {
        this.id = id;
        this.balance = balance;
    }

    public String getId() { return id; }
    public int getBalance() { return balance; }

    // Incrementa o saldo da conta
    public void credit(int amount) {
        this.balance += amount;
    }

    // Decrementa o saldo da conta
    public void debit(int amount) {
        this.balance -= amount;
    }
}
```

---

### 3.2 Exceção Customizada: `AccountNotFoundException.java`

Uma exceção unchecked usada quando uma operação tenta acessar uma conta que não existe.

```java
package com.example.banking.service;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}
```

---

### 3.3 Camada de Serviço: `AccountService.java`

É o coração da aplicação. Armazena o estado das contas em memória e processa as operações: `deposit`, `withdraw` e `transfer`.

#### Métodos Explicados:
1. **`reset()`**: Limpa o mapa `accounts`, restaurando o estado inicial do sistema.
2. **`getBalance(String accountId)`**: Busca a conta. Se não encontrar, lança `AccountNotFoundException`.
3. **`processEvent(...)`**: Atua como uma fábrica/roteador de eventos com base na propriedade `"type"` do payload.

```java
package com.example.banking.service;

import com.example.banking.model.Account;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AccountService {

    // Armazenamento em memória concorrente (Thread-safe)
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    public void reset() {
        accounts.clear();
    }

    public int getBalance(String accountId) {
        Account account = accounts.get(accountId);
        if (account == null) {
            throw new AccountNotFoundException("Account not found");
        }
        return account.getBalance();
    }

    public Map<String, Object> processEvent(Map<String, Object> payload) {
        String type = (String) payload.get("type");

        switch (type) {
            case "deposit":
                return handleDeposit(payload);
            case "withdraw":
                return handleWithdraw(payload);
            case "transfer":
                return handleTransfer(payload);
            default:
                throw new IllegalArgumentException("Invalid event type");
        }
    }

    private Map<String, Object> handleDeposit(Map<String, Object> payload) {
        String destinationId = (String) payload.get("destination");
        int amount = (int) payload.get("amount");

        // Cria a conta com saldo 0 caso ela ainda não exista
        Account account = accounts.computeIfAbsent(destinationId, id -> new Account(id, 0));
        account.credit(amount);

        return Map.of("destination", Map.of("id", account.getId(), "balance", account.getBalance()));
    }

    private Map<String, Object> handleWithdraw(Map<String, Object> payload) {
        String originId = (String) payload.get("origin");
        int amount = (int) payload.get("amount");

        Account account = accounts.get(originId);
        if (account == null) {
            throw new AccountNotFoundException("Account not found");
        }

        account.debit(amount);

        return Map.of("origin", Map.of("id", account.getId(), "balance", account.getBalance()));
    }

    private Map<String, Object> handleTransfer(Map<String, Object> payload) {
        String originId = (String) payload.get("origin");
        String destinationId = (String) payload.get("destination");
        int amount = (int) payload.get("amount");

        Account originAccount = accounts.get(originId);
        if (originAccount == null) {
            throw new AccountNotFoundException("Origin account not found");
        }

        Account destinationAccount = accounts.computeIfAbsent(destinationId, id -> new Account(id, 0));

        originAccount.debit(amount);
        destinationAccount.credit(amount);

        return Map.of(
            "origin", Map.of("id", originAccount.getId(), "balance", originAccount.getBalance()),
            "destination", Map.of("id", destinationAccount.getId(), "balance", destinationAccount.getBalance())
        );
    }
}
```

---

### 3.4 Camada de Controle: `AccountController.java`

Mapeia as requisições HTTP para chamadas do `AccountService` e define o status de retorno (Ex.: `200 OK`, `201 Created` ou `404 Not Found`).

```java
package com.example.banking.controller;

import com.example.banking.service.AccountNotFoundException;
import com.example.banking.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/reset")
    public ResponseEntity<String> reset() {
        accountService.reset();
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/balance")
    public ResponseEntity<Object> getBalance(@RequestParam("account_id") String accountId) {
        try {
            int balance = accountService.getBalance(accountId);
            return ResponseEntity.ok(balance);
        } catch (AccountNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(0);
        }
    }

    @PostMapping("/event")
    public ResponseEntity<Object> handleEvent(@RequestBody Map<String, Object> payload) {
        try {
            Map<String, Object> result = accountService.processEvent(payload);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (AccountNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(0);
        }
    }
}
```

---

## 4. Detalhamento dos Testes Unitários

A classe `AccountServiceTest` realiza testes diretos sobre as regras de negócio sem dependências externas:

- **Casos de Erro:**
  - Consulta de saldo em conta inexistente (espera `AccountNotFoundException`).
  - Tentativa de saque em conta inexistente.
  - Tentativa de transferência a partir de uma conta de origem inexistente.
- **Alterações de Estado:**
  - Depósito em nova conta e verificação do incremento do saldo.
  - Múltiplos depósitos sequenciais garantindo acúmulo correto.
  - Saque em conta existente e validação do saldo restante.
  - Reset limpando todas as contas do mapa.
- **Fluxo Completo:**
  - Transferência de valores entre duas contas (origem e destino) validando o impacto em ambas.

---

## 5. Como Executar o Projeto

1. Certifique-se de ter o **Java 17+** e o **Maven** instalados (ou abra diretamente no IntelliJ IDEA).
2. Para rodar a aplicação:
   ```bash
   mvn spring-boot:run
   ```
3. Para rodar os testes unitários:
   ```bash
   mvn test
   ```