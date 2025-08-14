package com.example.account_transfer_api.service;

import com.example.account_transfer_api.dto.AccountDTO;
import com.example.account_transfer_api.entity.Account;
import com.example.account_transfer_api.repository.AccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountDTO createAccount(@RequestBody AccountDTO dto) {
        Account account = Account.builder()
                .name(dto.getName())
                .balance(dto.getBalance())
                .currency(dto.getCurrency())
                .build();

        Account accountSaved = accountRepository.save(account);

        return mapToDTO(accountSaved);
    }

    public List<AccountDTO> getAllAccounts() {
        return accountRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public AccountDTO getAccountById(UUID id) {
        return accountRepository.findById(id)
                .map(account -> new AccountDTO(
                        account.getId(),
                        account.getName(),
                        account.getBalance(),
                        account.getCurrency()
                ))
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + id));
    }

    private AccountDTO mapToDTO(Account account) {
        return AccountDTO.builder()
                .id(account.getId())
                .name(account.getName())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .build();
    }
}
