package com.example.banking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AccountServiceTest {

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService();
    }

    @Test
    @DisplayName("Deve retornar exceção ao consultar saldo de conta inexistente (Caso de Erro)")
    void shouldThrowExceptionWhenAccountNotFoundForBalance() {
        assertThrows(AccountNotFoundException.class, () -> {
            accountService.getBalance("1234");
        });
    }

    @Test
    @DisplayName("Deve criar conta e depositar saldo com sucesso (Alteração de Estado)")
    void shouldCreateAccountAndDeposit() {
        Map<String, Object> payload = Map.of(
                "type", "deposit",
                "destination", "100",
                "amount", 10
        );

        Map<String, Object> response = accountService.processEvent(payload);

        assertNotNull(response);
        @SuppressWarnings("unchecked")
        Map<String, Object> destination = (Map<String, Object>) response.get("destination");
        
        assertEquals("100", destination.get("id"));
        assertEquals(10, destination.get("balance"));
        assertEquals(10, accountService.getBalance("100"));
    }

    @Test
    @DisplayName("Deve acumular saldo em depósito subsequente na mesma conta (Alteração de Estado)")
    void shouldAccumulateBalanceOnMultipleDeposits() {
        Map<String, Object> payload = Map.of(
                "type", "deposit",
                "destination", "100",
                "amount", 10
        );

        accountService.processEvent(payload);
        accountService.processEvent(payload);

        assertEquals(20, accountService.getBalance("100"));
    }

    @Test
    @DisplayName("Deve retornar erro ao tentar sacar de conta inexistente (Caso de Erro)")
    void shouldThrowExceptionWhenWithdrawingFromNonExistingAccount() {
        Map<String, Object> payload = Map.of(
                "type", "withdraw",
                "origin", "200",
                "amount", 10
        );

        assertThrows(AccountNotFoundException.class, () -> {
            accountService.processEvent(payload);
        });
    }

    @Test
    @DisplayName("Deve subtrair saldo ao realizar saque em conta existente (Alteração de Estado)")
    void shouldWithdrawFromExistingAccount() {
        accountService.processEvent(Map.of("type", "deposit", "destination", "100", "amount", 20));

        Map<String, Object> withdrawPayload = Map.of(
                "type", "withdraw",
                "origin", "100",
                "amount", 5
        );
        Map<String, Object> response = accountService.processEvent(withdrawPayload);

        @SuppressWarnings("unchecked")
        Map<String, Object> origin = (Map<String, Object>) response.get("origin");

        assertEquals("100", origin.get("id"));
        assertEquals(15, origin.get("balance"));
        assertEquals(15, accountService.getBalance("100"));
    }

    @Test
    @DisplayName("Deve executar fluxo completo de transferência entre contas (Fluxo Completo)")
    void shouldExecuteCompleteTransferFlow() {
        accountService.processEvent(Map.of("type", "deposit", "destination", "100", "amount", 15));

        Map<String, Object> transferPayload = Map.of(
                "type", "transfer",
                "origin", "100",
                "destination", "300",
                "amount", 15
        );
        Map<String, Object> response = accountService.processEvent(transferPayload);

        @SuppressWarnings("unchecked")
        Map<String, Object> origin = (Map<String, Object>) response.get("origin");
        @SuppressWarnings("unchecked")
        Map<String, Object> destination = (Map<String, Object>) response.get("destination");

        assertEquals(0, origin.get("balance"));
        assertEquals(15, destination.get("balance"));
        
        assertEquals(0, accountService.getBalance("100"));
        assertEquals(15, accountService.getBalance("300"));
    }

    @Test
    @DisplayName("Deve falhar ao tentar transferir de conta inexistente (Caso de Erro)")
    void shouldThrowExceptionWhenTransferringFromNonExistingAccount() {
        Map<String, Object> transferPayload = Map.of(
                "type", "transfer",
                "origin", "200",
                "destination", "300",
                "amount", 15
        );

        assertThrows(AccountNotFoundException.class, () -> {
            accountService.processEvent(transferPayload);
        });
    }

    @Test
    @DisplayName("Deve limpar o estado ao chamar reset (Alteração de Estado)")
    void shouldResetState() {
        accountService.processEvent(Map.of("type", "deposit", "destination", "100", "amount", 10));
        assertEquals(10, accountService.getBalance("100"));

        accountService.reset();

        assertThrows(AccountNotFoundException.class, () -> {
            accountService.getBalance("100");
        });
    }
}
