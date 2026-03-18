package com.gestionBancaire.gestionBancaire.Service;

import com.gestionBancaire.gestionBancaire.Entity.Account;
import com.gestionBancaire.gestionBancaire.Repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountService {
    @Autowired
    private AccountRepository accountRepository;

    // CREATE
    public Account createAccount(Account account){
        return accountRepository.save(account);
    }

    // READ (tous les comptes)
    public List<Account> getAllAccounts(){
        return accountRepository.findAll();
    }

    // READ (un seul compte)
    public Account getAccountById(Long id){
        return accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Compte introuvable"));
    }

    // UPDATE
    public Account updateAccount(Long id, Account accountDetails){

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Compte introuvable"));

        account.setNumeroCompte(accountDetails.getNumeroCompte());
        account.setSolde(accountDetails.getSolde());
        account.setUpdated_at(LocalDateTime.now());

        return accountRepository.save(account);
    }

    // DELETE
    public void deleteAccount(Long id){

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Compte introuvable"));

        accountRepository.delete(account);
    }
}
