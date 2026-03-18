package com.gestionBancaire.gestionBancaire.Controller;

import com.gestionBancaire.gestionBancaire.Entity.Transaction;
import com.gestionBancaire.gestionBancaire.Service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    // CREATE (faire un virement)
    @PostMapping
    public Transaction createTransaction(@RequestParam Long senderId,@RequestParam Long receiverId,@RequestParam double montant, @RequestParam String libelle){

        return transactionService.createTransaction(senderId, receiverId, montant,libelle);
    }

    // READ ALL
    @GetMapping
    public List<Transaction> getAllTransactions(){
        return transactionService.getAllTransactions();
    }

    // READ by id
    @GetMapping("/{id}")
    public Transaction getTransaction(@PathVariable Long id){
        return transactionService.transactionById(id);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public void deleteTransaction(@PathVariable Long id){
        transactionService.deleteTransaction(id);
    }

}
