package com.gestionBancaire.gestionBancaire.Repository;

import com.gestionBancaire.gestionBancaire.Entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
