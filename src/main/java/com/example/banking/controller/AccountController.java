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
