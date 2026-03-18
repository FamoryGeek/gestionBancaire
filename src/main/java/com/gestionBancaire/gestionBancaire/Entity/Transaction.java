package com.gestionBancaire.gestionBancaire.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(nullable = true, length = 255)
    private String libelle;
    private double Montant;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;

    @ManyToOne // une transaction appatient a un compte
    private Account sender;// celui qui sera debiter

    @ManyToOne//une transaction appatient a un compte
    private Account receiver; // celui qui sera crediteur


}
