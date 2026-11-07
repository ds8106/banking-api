package com.example.banking.service;

import com.example.banking.model.Account;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AccountService {

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

        if (amount < 0) {
            throw new InvalidTransactionException("Deposit amount cannot be negative");
        }

        Account account = accounts.computeIfAbsent(destinationId, id -> new Account(id, 0));
        account.credit(amount);

        return Map.of("destination", Map.of("id", account.getId(), "balance", account.getBalance()));
    }

    private Map<String, Object> handleWithdraw(Map<String, Object> payload) {
        String originId = (String) payload.get("origin");
        int amount = (int) payload.get("amount");

        if (amount < 0) {
            throw new InvalidTransactionException("Withdraw amount cannot be negative");
        }

        Account account = accounts.get(originId);
        if (account == null) {
            throw new AccountNotFoundException("Account not found");
        }

        if (account.getBalance() - amount < 0) {
            throw new InvalidTransactionException("Insufficient funds");
        }

        account.debit(amount);

        return Map.of("origin", Map.of("id", account.getId(), "balance", account.getBalance()));
    }

    private Map<String, Object> handleTransfer(Map<String, Object> payload) {
        String originId = (String) payload.get("origin");
        String destinationId = (String) payload.get("destination");
        int amount = (int) payload.get("amount");

        if (amount < 0) {
            throw new InvalidTransactionException("Transfer amount cannot be negative");
        }

        Account originAccount = accounts.get(originId);
        if (originAccount == null) {
            throw new AccountNotFoundException("Origin account not found");
        }

        if (originAccount.getBalance() - amount < 0) {
            throw new InvalidTransactionException("Insufficient funds in origin account");
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