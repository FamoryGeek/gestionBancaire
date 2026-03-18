package com.gestionBancaire.gestionBancaire.Entity;

import com.gestionBancaire.gestionBancaire.Entity.Enum.Role;
import com.gestionBancaire.gestionBancaire.Entity.Enum.Statut;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;
    @Column(unique = true, nullable = false, length = 255)
    private String email;
    @Column( nullable = false, length = 255)
    private String password;
    @Column(nullable = true, length = 255)
    private String adresse;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Statut statut;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}


