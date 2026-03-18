package com.gestionBancaire.gestionBancaire.Service;

import com.gestionBancaire.gestionBancaire.Entity.Account;
import com.gestionBancaire.gestionBancaire.Entity.Transaction;
import com.gestionBancaire.gestionBancaire.Repository.AccountRepository;
import com.gestionBancaire.gestionBancaire.Repository.TransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    //Tous all
    public List<Transaction> getAllTransactions(){
        return transactionRepository.findAll();
    }

    //by Id
    public Transaction transactionById(Long id){
        return transactionRepository.findById(id).orElseThrow(()-> new RuntimeException("Transaction introuvable"));
    }

    // CREATE (faire un virement)
    @Transactional
    public Transaction createTransaction(Long senderId, Long receiverId, double montant, String libelle) {

        // verifie si le compte de l'envoyeur existe
        Account sender = accountRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Compte source introuvable"));

        // verifie si le compte de receveur existe
        Account receiver = accountRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Compte destinataire introuvable"));

        //verifier si le solde est suffisant avant l'envoie
        if (sender.getSolde() < montant) {
            throw new RuntimeException("Solde insuffisant");
        }

        // débite l'envoyeur
        sender.setSolde(sender.getSolde() - montant);

        // crédite le receveur
        receiver.setSolde(receiver.getSolde() + montant);

        accountRepository.save(sender);
        accountRepository.save(receiver);

        Transaction transaction = new Transaction();
        transaction.setLibelle(libelle);
        transaction.setMontant(montant);
        transaction.setSender(sender);
        transaction.setReceiver(receiver);
        transaction.setCreated_at(LocalDateTime.now());

        //Sauvegarder
        return transactionRepository.save(transaction);
    }

    //UPDATE
    public Transaction updateTransaction(Long id, Transaction transactionDetails){
        Transaction transaction = transactionRepository.findById(id).orElseThrow(()-> new RuntimeException("Transaction introuvable"));
        transaction.setLibelle(transactionDetails.getLibelle());
        transaction.setMontant(transactionDetails.getMontant());
        transaction.setSender(transactionDetails.getSender());
        transaction.setReceiver(transactionDetails.getReceiver());
        transaction.setUpdated_at(LocalDateTime.now());
        return transactionRepository.save(transactionDetails);
    }

    // DELETE
    public void deleteTransaction(Long id){

        Transaction transaction = transactionRepository.findById(id).orElseThrow(() -> new RuntimeException("Transaction introuvable"));

        transactionRepository.delete(transaction);
    }

}
