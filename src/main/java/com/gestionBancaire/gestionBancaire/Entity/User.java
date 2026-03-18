package com.gestionBancaire.gestionBancaire.Entity;

import com.gestionBancaire.gestionBancaire.Entity.Enum.Role;
import com.gestionBancaire.gestionBancaire.Entity.Enum.Statut;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data //Permet de gérer automatiquement les getters et setters
@AllArgsConstructor //permet de gérer les automatiquement les contructeur avec parametre
@NoArgsConstructor //permet de gérer les automatiquement les contructeur sans parametre
@Entity // permet de définir que la classe est une Entité
public class User {
    @Id // definit que le champ est la cle primaire de la table
    @GeneratedValue(strategy = GenerationType.AUTO) // permet de generer automatique l'ID
    private Long id;

    private String name;
    @Column(unique = true, nullable = false, length = 255) // definit les criteres sur un champ
    private String email;
    @Column( nullable = false, length = 255)
    private String password;
    @Column(nullable = true, length = 255)
    private String adresse;
    @Enumerated(EnumType.STRING) // permet de definir le type de mes enumerations
    @Column(nullable = false, length = 20)
    private Statut statut; // Le Type Statut definis que le champ utilise momn enum Statut
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}


