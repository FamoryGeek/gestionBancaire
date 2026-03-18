package com.gestionBancaire.gestionBancaire.Controller;

import com.gestionBancaire.gestionBancaire.Entity.Account;
import com.gestionBancaire.gestionBancaire.Service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/accounts")
public class AccountController {
    @Autowired
    private AccountService accountService;

    // CREATE
    @PostMapping
    public Account createAccount(@RequestBody Account account){
        return accountService.createAccount(account);
    }

    // READ ALL
    @GetMapping
    public List<Account> getAllAccounts(){
        return accountService.getAllAccounts();
    }

    // READ BY ID
    @GetMapping("/{id}")
    public Account getAccount(@PathVariable Long id){
        return accountService.getAccountById(id);
    }

    // UPDATE
    @PutMapping("/{id}")
    public Account updateAccount(@PathVariable Long id, @RequestBody Account account){
        return accountService.updateAccount(id, account);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public void deleteAccount(@PathVariable Long id){
        accountService.deleteAccount(id);
    }
}
