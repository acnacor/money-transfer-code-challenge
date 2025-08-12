package com.example.account_transfer_api.controller;

import com.example.account_transfer_api.dto.AccountDTO;
import com.example.account_transfer_api.entity.Account;
import com.example.account_transfer_api.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountRepository accountRepository;

    @PostMapping
    public AccountDTO createAccount(@RequestBody AccountDTO dto) {
        Account account = Account.builder()
                .name(dto.getName())
                .balance(dto.getBalance())
                .currency(dto.getCurrency())
                .build();
        Account saved = accountRepository.save(account);
        return AccountDTO.builder()
                .id(saved.getId())
                .name(saved.getName())
                .balance(saved.getBalance())
                .currency(saved.getCurrency())
                .build();
    }

    @GetMapping
    public List<AccountDTO> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(a -> AccountDTO.builder()
                        .id(a.getId())
                        .name(a.getName())
                        .balance(a.getBalance())
                        .currency(a.getCurrency())
                        .build())
                .collect(Collectors.toList());
    }
}
