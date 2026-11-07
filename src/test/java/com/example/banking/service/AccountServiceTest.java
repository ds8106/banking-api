package com.example.banking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AccountServiceTest {

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService();
        accountService.reset();
    }

    @Test
    void testDepositNegativeAmountThrowsException() {
        Map<String, Object> payload = Map.of(
            "type", "deposit",
            "destination", "100",
            "amount", -50
        );

        assertThrows(InvalidTransactionException.class, () -> accountService.processEvent(payload));
    }

    @Test
    void testWithdrawInsufficientBalanceThrowsException() {
        accountService.processEvent(Map.of("type", "deposit", "destination", "100", "amount", 10));

        Map<String, Object> withdrawPayload = Map.of(
            "type", "withdraw",
            "origin", "100",
            "amount", 20
        );

        assertThrows(InvalidTransactionException.class, () -> accountService.processEvent(withdrawPayload));
    }

    @Test
    void testTransferInsufficientBalanceThrowsException() {
        accountService.processEvent(Map.of("type", "deposit", "destination", "100", "amount", 10));

        Map<String, Object> transferPayload = Map.of(
            "type", "transfer",
            "origin", "100",
            "destination", "200",
            "amount", 15
        );

        assertThrows(InvalidTransactionException.class, () -> accountService.processEvent(transferPayload));
    }

    @Test
    void testTransferNegativeAmountThrowsException() {
        accountService.processEvent(Map.of("type", "deposit", "destination", "100", "amount", 50));

        Map<String, Object> transferPayload = Map.of(
            "type", "transfer",
            "origin", "100",
            "destination", "200",
            "amount", -10
        );

        assertThrows(InvalidTransactionException.class, () -> accountService.processEvent(transferPayload));
    }
}