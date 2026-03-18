# TP – Gestion Bancaire (API REST Spring Boot + JWT)

**Guide pas à pas** : de la création du projet jusqu’à une API fonctionnelle (utilisateurs, comptes, transactions) sécurisée par JWT.

---

## Sommaire du guide

| Partie | Contenu |
|--------|---------|
| **1** | Création du projet, dépendances, base MySQL, configuration (`application.properties`) |
| **2** | Couche **Entity** : enums (Role, Statut), User, Account, Transaction |
| **3** | Couche **Repository** : UserRepository, AccountRepository, TransactionRepository |
| **4** | Couche **Service** : UserService, AccountService, TransactionService, CustomUserDetailsService |
| **5** | Couche **Controller** : AuthController, UserController, AccountController, TransactionController |
| **6** | **JWT** : JwtUtil, AuthTokenFilter, AuthEntryPointJwt, WebSecurityConfig |
| **7** | Lancer l’application et tester l’API (inscription, connexion, requêtes protégées) |

---

## Objectifs du TP

- Créer une API REST avec Spring Boot (architecture en couches).
- Modéliser les entités **User**, **Account**, **Transaction** et les persister avec JPA/MySQL.
- Mettre en place l’authentification par **JWT** (inscription, connexion, protection des routes).
- Comprendre le rôle de chaque couche et de chaque fichier.

---

## Prérequis

Avant de commencer, installer :

| Outil | Version | Rôle |
|-------|---------|------|
| **JDK** | 21 | Compilation et exécution Java |
| **Maven** | 3.6+ | Gestion des dépendances et build (ou utiliser le wrapper `mvnw`) |
| **MySQL** | 5.7+ ou 8+ | Base de données |
| **IDE** | IntelliJ / Eclipse / VS Code | Édition du code (optionnel) |

Vérifier les installations :

```bash
java -version
mvn -version
mysql --version
```

---

# Partie 1 – Création et configuration du projet

## Étape 1.1 – Créer le projet Spring Boot

**Option A – Spring Initializr (recommandé)**  
1. Aller sur [start.spring.io](https://start.spring.io).  
2. Choisir : **Project** Maven, **Language** Java, **Spring Boot** 4.x (ou 3.x).  
3. **Metadata** : Group `com.gestionBancaire`, Artifact `gestionBancaire`, Package name `com.gestionBancaire.gestionBancaire`.  
4. **Dependencies** : ajouter **Spring Web**, **Spring Data JPA**, **MySQL Driver**, **Spring Security**, **Lombok**.  
5. Générer le projet et l’extraire dans un dossier.

**Option B – Projet existant**  
Si le projet existe déjà, ouvrir le dossier contenant `pom.xml` et passer à l’étape 1.2.

---

## Étape 1.2 – Ajouter les dépendances JWT dans `pom.xml`

Ouvrir `pom.xml` et ajouter les dépendances **JJWT** pour la génération et la validation des tokens :

```xml
<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
```

**Pourquoi ?**  
- `jjwt-api` : API pour construire et parser les JWT.  
- `jjwt-impl` et `jjwt-jackson` : implémentation et sérialisation JSON (runtime uniquement).

Vérifier aussi la présence de : **spring-boot-starter-web**, **spring-boot-starter-data-jpa**, **spring-boot-starter-security**, **mysql-connector-j**, **lombok**.

---

## Étape 1.3 – Créer la base de données MySQL

Dans MySQL (ligne de commande ou outil type MySQL Workbench) :

```sql
CREATE DATABASE gestion_bancaire CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

On utilisera cette base dans la configuration Spring.

---

## Étape 1.4 – Configurer l’application

Créer le fichier **`src/main/resources/application.properties`** (ou copier `application.properties.example` puis le renommer/adapter).

```properties
# Base de données
spring.datasource.url=jdbc:mysql://localhost:3306/gestion_bancaire?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=VOTRE_MOT_DE_PASSE
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA / Hibernate : création/mise à jour des tables à partir des entités
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# JWT
app.jwt.secret=VotreSecretJWTTresLongEtSecuriseMinimum64CaracteresPourHS256
app.jwt.expiration=86400000
```

**Explication :**  
- **ddl-auto=update** : Hibernate crée ou met à jour les tables selon les entités (pratique en dev).  
- **app.jwt.secret** : clé utilisée pour signer les JWT (en production, utiliser une vraie clé secrète).  
- **app.jwt.expiration** : durée de vie du token en ms (86400000 = 24 h).

---

## Étape 1.5 – Point d’entrée de l’application

La classe **`Application.java`** à la racine du package permet de lancer Spring Boot :

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

**Rôle de `@SpringBootApplication`** : active la configuration automatique, le scan des composants et le démarrage du serveur web intégré.

À ce stade, vous pouvez lancer l’application (`mvnw spring-boot:run` ou depuis l’IDE). Elle démarrera mais sans encore d’entités ni de contrôleurs métier.

---

# Partie 2 – Couche Entity (modèle de données)

On définit les **entités JPA** qui correspondent aux tables de la base.

**Ordre de création des fichiers (à respecter) :**

| Ordre | Fichier à créer | Utilité |
|-------|-----------------|---------|
| **1** | `Entity/Enum/Role.java` | Énumération des rôles utilisateur (ADMIN, CLIENT). |
| **2** | `Entity/Enum/Statut.java` | Énumération du statut du compte (Actif, Inactif). |
| **3** | `Entity/User.java` | Entité utilisateur : authentification et propriétaire des comptes. |
| **4** | `Entity/Account.java` | Entité compte bancaire, liée à un User. |
| **5** | `Entity/Transaction.java` | Entité virement entre deux comptes (sender, receiver). |

**Prérequis :** package `com.gestionBancaire.gestionBancaire.Entity` et sous-package `Entity.Enum`.

---

## Étape 2.1 – Créer Role (1er fichier)

**Emplacement :** `src/main/java/com/gestionBancaire/gestionBancaire/Entity/Enum/Role.java`

**Utilité :** Définir les rôles possibles d’un utilisateur. Utilisé dans l’entité `User` et par Spring Security pour les autorisations.

**Code complet :**

```java
package com.gestionBancaire.gestionBancaire.Entity.Enum;

public enum Role {
    ADMIN,
    CLIENT
}
```

**Explication :** Une enum limite les valeurs en base à ADMIN ou CLIENT et évite les erreurs de saisie.

---

## Étape 2.2 – Créer Statut (2e fichier)

**Emplacement :** `src/main/java/com/gestionBancaire/gestionBancaire/Entity/Enum/Statut.java`

**Utilité :** Définir si un compte utilisateur est actif ou inactif. Utilisé dans l’entité `User` pour activer/désactiver un utilisateur.

**Code complet :**

```java
package com.gestionBancaire.gestionBancaire.Entity.Enum;

public enum Statut {
    Actif,
    Inactif
}
```

---

## Étape 2.3 – Créer User (3e fichier)

**Emplacement :** `src/main/java/com/gestionBancaire/gestionBancaire/Entity/User.java`

**Utilité :** Représenter un utilisateur en base (table `user`). Sert à l’authentification (email + password), au chargement par **CustomUserDetailsService**, et comme propriétaire des comptes (relation avec `Account`).

**Code complet :**

```java
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
    @Column(nullable = false, length = 255)
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
```

**Explication par partie :**

| Élément | Rôle |
|--------|------|
| `@Data` (Lombok) | Génère getters, setters, toString, equals, hashCode. |
| `@AllArgsConstructor` / `@NoArgsConstructor` | Constructeurs avec et sans paramètres (requis par JPA). |
| `@Entity` | Classe mappée vers une table en base (nom de table = `user` par défaut). |
| `@Id` + `@GeneratedValue(strategy = GenerationType.AUTO)` | Clé primaire auto-générée. |
| `@Column(unique = true, nullable = false)` sur email | Unicité et obligation de l’email. |
| `@Enumerated(EnumType.STRING)` | Stocke les enums en chaîne en BDD (lisible). |

---

## Étape 2.4 – Créer Account (4e fichier)

**Emplacement :** `src/main/java/com/gestionBancaire/gestionBancaire/Entity/Account.java`

**Utilité :** Représenter un compte bancaire (table `account`). Chaque compte appartient à un utilisateur et peut être source ou cible d’une transaction.

**Code complet :**

```java
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
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(unique = true, nullable = false, length = 25)
    private String numeroCompte;
    private double solde;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
```

**Explication par partie :**

| Élément | Rôle |
|--------|------|
| `numeroCompte` | Numéro unique du compte (ex. IBAN ou numéro interne). |
| `solde` | Solde actuel du compte. |
| `@ManyToOne` + `@JoinColumn(name = "user_id")` | Relation : plusieurs comptes peuvent appartenir à un même User ; la table `account` a une colonne `user_id` (clé étrangère). |

---

## Étape 2.5 – Créer Transaction (5e fichier)

**Emplacement :** `src/main/java/com/gestionBancaire/gestionBancaire/Entity/Transaction.java`

**Utilité :** Représenter un virement entre deux comptes (table `transaction`). Lie un compte émetteur (débité) et un compte destinataire (crédité).

**Code complet :**

```java
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

    @ManyToOne
    private Account sender;

    @ManyToOne
    private Account receiver;
}
```

**Explication par partie :**

| Élément | Rôle |
|--------|------|
| `libelle` | Description du virement (ex. "Remboursement"). |
| `Montant` | Montant transféré. |
| `@ManyToOne sender` | Compte débité (source du virement). |
| `@ManyToOne receiver` | Compte crédité (destinataire). |

---

# Partie 3 – Couche Repository (accès aux données)

Les **repositories** sont des interfaces qui étendent **`JpaRepository<Entité, TypeId>`**. Spring fournit automatiquement les méthodes **findAll**, **findById**, **save**, **delete**, etc.

## Étape 3.1 – UserRepository

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

- **findByEmail** : utilisé pour charger l’utilisateur à l’authentification (Spring Security + JWT).  
- **existsByEmail** : utilisé à l’inscription pour refuser un email déjà pris.

## Étape 3.2 – AccountRepository

```java
public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUserId(Long userId);
}
```

- **findByUserId** : récupère tous les comptes d’un utilisateur (nom de méthode dérivé de la propriété `user.id`).

## Étape 3.3 – TransactionRepository

```java
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
```

Le CRUD de base suffit ; les requêtes métier (virements) sont dans la couche **Service**.

---

# Partie 4 – Couche Service (logique métier)

Les **services** contiennent la logique métier et utilisent les **repositories**. Ils sont marqués par **`@Service`**.

**Ordre de création des fichiers (à respecter) :**

| Ordre | Fichier à créer | Utilité |
|-------|-----------------|---------|
| **1** | `Service/CustomUserDetailsService.java` | Charge un User depuis la BDD pour Spring Security (login + JWT). À créer avant les contrôleurs d’auth. |
| **2** | `Service/UserService.java` | CRUD utilisateurs + activation/désactivation. |
| **3** | `Service/AccountService.java` | CRUD comptes. |
| **4** | `Service/TransactionService.java` | Liste/lecture transactions + création de virement (débit/crédit). |

**Prérequis :** Les **Repository** (Partie 3) et les **Entity** (Partie 2) doivent exister.

---

## Étape 4.1 – Créer CustomUserDetailsService (1er fichier)

**Emplacement :** `src/main/java/com/gestionBancaire/gestionBancaire/Service/CustomUserDetailsService.java`

**Utilité :** Implémenter **UserDetailsService** pour Spring Security. Ce service est appelé lors du login (email + mot de passe) et par **AuthTokenFilter** après validation du JWT pour charger l’utilisateur et remplir le SecurityContext.

**Code complet :**

```java
package com.gestionBancaire.gestionBancaire.Service;

import com.gestionBancaire.gestionBancaire.Entity.User;
import com.gestionBancaire.gestionBancaire.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé : " + email));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
    }
}
```

**Explication par partie :**

| Élément | Rôle |
|--------|------|
| `implements UserDetailsService` | Interface Spring Security : “charger un utilisateur par son identifiant (ici l’email)”. |
| `loadUserByUsername(String email)` | Cherche le `User` en base ; si absent, lance `UsernameNotFoundException`. |
| `User.withUsername(...).password(...).roles(...).build()` | Construit un `UserDetails` Spring (username = email, password, rôles) utilisé pour l’authentification et les autorisations. |

---

## Étape 4.2 – Créer UserService (2e fichier)

**Emplacement :** `src/main/java/com/gestionBancaire/gestionBancaire/Service/UserService.java`

**Utilité :** Logique métier des utilisateurs : liste, lecture par id, création, mise à jour, activation et désactivation. Utilisé par **UserController**.

**Code complet :**

```java
package com.gestionBancaire.gestionBancaire.Service;

import com.gestionBancaire.gestionBancaire.Entity.Enum.Statut;
import com.gestionBancaire.gestionBancaire.Entity.User;
import com.gestionBancaire.gestionBancaire.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("Utilisateur Introuvable"));
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public User userUpdate(Long id, User userDetails) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        user.setRole(userDetails.getRole());
        user.setEmail(userDetails.getEmail());
        user.setAdresse(userDetails.getAdresse());
        user.setPassword(userDetails.getPassword());
        user.setStatut(userDetails.getStatut());
        user.setUpdated_at(LocalDateTime.now());
        return userRepository.save(user);
    }

    public void desactiverUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Utilisateur Introuvable"));
        user.setStatut(Statut.Inactif);
        userRepository.save(user);
    }

    public void activerUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Utilisateur Introuvable"));
        user.setStatut(Statut.Actif);
        userRepository.save(user);
    }
}
```

**Explication par méthode :**

| Méthode | Rôle |
|--------|------|
| `getAllUsers()` | Retourne tous les utilisateurs. |
| `getUserById(id)` | Retourne un utilisateur ou lance une exception si absent. |
| `createUser(user)` | Enregistre un nouvel utilisateur. |
| `userUpdate(id, userDetails)` | Charge l’utilisateur, met à jour les champs, sauvegarde avec `updated_at`. |
| `desactiverUser(id)` | Passe le statut à `Statut.Inactif`. |
| `activerUser(id)` | Passe le statut à `Statut.Actif`. |

---

## Étape 4.3 – Créer AccountService (3e fichier)

**Emplacement :** `src/main/java/com/gestionBancaire/gestionBancaire/Service/AccountService.java`

**Utilité :** CRUD des comptes bancaires. Utilisé par **AccountController**.

**Code complet :**

```java
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

    public Account createAccount(Account account) {
        return accountRepository.save(account);
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Account getAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Compte introuvable"));
    }

    public Account updateAccount(Long id, Account accountDetails) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Compte introuvable"));
        account.setNumeroCompte(accountDetails.getNumeroCompte());
        account.setSolde(accountDetails.getSolde());
        account.setUpdated_at(LocalDateTime.now());
        return accountRepository.save(account);
    }

    public void deleteAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Compte introuvable"));
        accountRepository.delete(account);
    }
}
```

**Explication par méthode :**

| Méthode | Rôle |
|--------|------|
| `createAccount(account)` | Enregistre un nouveau compte. |
| `getAllAccounts()` | Liste tous les comptes. |
| `getAccountById(id)` | Retourne un compte ou lance une exception. |
| `updateAccount(id, accountDetails)` | Met à jour numeroCompte, solde et updated_at. |
| `deleteAccount(id)` | Supprime le compte. |

---

## Étape 4.4 – Créer TransactionService (4e fichier)

**Emplacement :** `src/main/java/com/gestionBancaire/gestionBancaire/Service/TransactionService.java`

**Utilité :** Liste et lecture des transactions ; **création d’un virement** : vérification des comptes, solde suffisant, débit du sender, crédit du receiver, enregistrement de la transaction. Utilisé par **TransactionController**.

**Code complet :**

```java
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

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public Transaction transactionById(Long id) {
        return transactionRepository.findById(id).orElseThrow(() -> new RuntimeException("Transaction introuvable"));
    }

    @Transactional
    public Transaction createTransaction(Long senderId, Long receiverId, double montant, String libelle) {

        Account sender = accountRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Compte source introuvable"));

        Account receiver = accountRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Compte destinataire introuvable"));

        if (sender.getSolde() < montant) {
            throw new RuntimeException("Solde insuffisant");
        }

        sender.setSolde(sender.getSolde() - montant);
        receiver.setSolde(receiver.getSolde() + montant);

        accountRepository.save(sender);
        accountRepository.save(receiver);

        Transaction transaction = new Transaction();
        transaction.setLibelle(libelle);
        transaction.setMontant(montant);
        transaction.setSender(sender);
        transaction.setReceiver(receiver);
        transaction.setCreated_at(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }

    public Transaction updateTransaction(Long id, Transaction transactionDetails) {
        Transaction transaction = transactionRepository.findById(id).orElseThrow(() -> new RuntimeException("Transaction introuvable"));
        transaction.setLibelle(transactionDetails.getLibelle());
        transaction.setMontant(transactionDetails.getMontant());
        transaction.setSender(transactionDetails.getSender());
        transaction.setReceiver(transactionDetails.getReceiver());
        transaction.setUpdated_at(LocalDateTime.now());
        return transactionRepository.save(transaction);
    }

    public void deleteTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id).orElseThrow(() -> new RuntimeException("Transaction introuvable"));
        transactionRepository.delete(transaction);
    }
}
```

**Explication par partie :**

| Élément | Rôle |
|--------|------|
| `@Transactional` sur `createTransaction` | Toute la méthode s’exécute dans une seule transaction : en cas d’exception, débit et crédit sont annulés. |
| Vérification `sender.getSolde() < montant` | Refuse le virement si solde insuffisant. |
| `sender.setSolde(... - montant)` / `receiver.setSolde(... + montant)` | Débite l’émetteur et crédite le destinataire. |
| Sauvegarde de `Transaction` | Enregistre l’historique du virement (sender, receiver, montant, libelle). |

---

## Résumé : ordre des services et liens

```
1. CustomUserDetailsService → Utilisé par : Spring Security (login), AuthTokenFilter (après JWT).
2. UserService             → Utilisé par : UserController.
3. AccountService          → Utilisé par : AccountController.
4. TransactionService     → Utilisé par : TransactionController ; utilise AccountRepository pour les virements.
```

---

# Partie 5 – Couche Controller (API REST)

Les **contrôleurs** exposent les endpoints HTTP. Ils sont annotés **`@RestController`** et **`@RequestMapping("...")**.

**Ordre de création des fichiers (à respecter) :**

| Ordre | Fichier à créer | Utilité |
|-------|-----------------|---------|
| **1** | `Controller/AuthController.java` | Inscription (register) et connexion (signin) ; renvoie le JWT après login. |
| **2** | `Controller/UserController.java` | CRUD utilisateurs + activation/désactivation. |
| **3** | `Controller/AccountController.java` | CRUD comptes. |
| **4** | `Controller/TransactionController.java` | Création de virement + liste/lecture/suppression de transactions. |

**Prérequis :** Les **Service** (Partie 4), **JwtUtil** et **WebSecurityConfig** (Partie 6) pour AuthController.

---

## Étape 5.1 – Créer AuthController (1er fichier)

**Emplacement :** `src/main/java/com/gestionBancaire/gestionBancaire/Controller/AuthController.java`

**Utilité :** Exposer **POST /api/auth/register** (inscription) et **POST /api/auth/signin** (connexion). Après un login réussi, renvoie le token JWT dans le corps de la réponse. Ces URLs sont publiques (pas de token requis).

**Code complet :**

```java
package com.gestionBancaire.gestionBancaire.Controller;

import com.gestionBancaire.gestionBancaire.Entity.Enum.Role;
import com.gestionBancaire.gestionBancaire.Entity.Enum.Statut;
import com.gestionBancaire.gestionBancaire.Entity.User;
import com.gestionBancaire.gestionBancaire.Repository.UserRepository;
import com.gestionBancaire.gestionBancaire.Security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder encoder;
    @Autowired
    JwtUtil jwtUtils;

    @PostMapping("/signin")
    public ResponseEntity<String> authenticateUser(@RequestBody User user) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword()));
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtils.generateToken(userDetails.getUsername());
        return ResponseEntity.ok(token);
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            return ResponseEntity.badRequest().body("Erreur : cet email est déjà utilisé !");
        }
        User newUser = new User();
        newUser.setName(user.getName());
        newUser.setEmail(user.getEmail());
        newUser.setPassword(encoder.encode(user.getPassword()));
        newUser.setAdresse(user.getAdresse());
        newUser.setRole(Role.CLIENT);
        newUser.setStatut(Statut.Actif);
        newUser.setCreated_at(LocalDateTime.now());
        newUser.setUpdated_at(LocalDateTime.now());
        userRepository.save(newUser);
        return ResponseEntity.status(201).body("Compte créé avec succès !");
    }
}
```

**Explication :** `POST /signin` authentifie via AuthenticationManager (CustomUserDetailsService), puis JwtUtil génère le token. `POST /register` vérifie l’email, hashe le mot de passe (PasswordEncoder), crée un User CLIENT/Actif et sauvegarde. Le client envoie ensuite le token dans **Authorization: Bearer &lt;token&gt;** pour les autres endpoints.

## Étape 5.2 – Créer UserController (2e fichier)

**Emplacement :** `src/main/java/com/gestionBancaire/gestionBancaire/Controller/UserController.java`

**Utilité :** Exposer le CRUD des utilisateurs et les actions activer/désactiver. Routes **protégées** (JWT requis).

**Code complet :**

```java
package com.gestionBancaire.gestionBancaire.Controller;

import com.gestionBancaire.gestionBancaire.Entity.User;
import com.gestionBancaire.gestionBancaire.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/users")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PutMapping("/{id}")
    public User userUpdate(@PathVariable Long id, @RequestBody User user) {
        return userService.userUpdate(id, user);
    }

    @PutMapping("/off/{id}")
    public void desactiverUser(@PathVariable Long id) {
        userService.desactiverUser(id);
    }

    @PutMapping("/on/{id}")
    public void activerUser(@PathVariable Long id) {
        userService.activerUser(id);
    }
}
```

**Endpoints :** POST /api/users, GET /api/users, GET /api/users/{id}, PUT /api/users/{id}, PUT /api/users/off/{id}, PUT /api/users/on/{id}.

## Étape 5.3 – Créer AccountController (3e fichier)

**Emplacement :** `src/main/java/com/gestionBancaire/gestionBancaire/Controller/AccountController.java`

**Utilité :** Exposer le CRUD des comptes bancaires. Routes **protégées** (JWT requis).

**Code complet :**

```java
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

    @PostMapping
    public Account createAccount(@RequestBody Account account) {
        return accountService.createAccount(account);
    }

    @GetMapping
    public List<Account> getAllAccounts() {
        return accountService.getAllAccounts();
    }

    @GetMapping("/{id}")
    public Account getAccount(@PathVariable Long id) {
        return accountService.getAccountById(id);
    }

    @PutMapping("/{id}")
    public Account updateAccount(@PathVariable Long id, @RequestBody Account account) {
        return accountService.updateAccount(id, account);
    }

    @DeleteMapping("/{id}")
    public void deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
    }
}
```

**Endpoints :** POST /api/accounts, GET /api/accounts, GET /api/accounts/{id}, PUT /api/accounts/{id}, DELETE /api/accounts/{id}.

## Étape 5.4 – Créer TransactionController (4e fichier)

**Emplacement :** `src/main/java/com/gestionBancaire/gestionBancaire/Controller/TransactionController.java`

**Utilité :** Créer un virement (POST avec paramètres) et exposer liste/lecture/suppression des transactions. Routes **protégées** (JWT requis).

**Code complet :**

```java
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

    @PostMapping
    public Transaction createTransaction(@RequestParam Long senderId, @RequestParam Long receiverId, @RequestParam double montant, @RequestParam String libelle) {
        return transactionService.createTransaction(senderId, receiverId, montant, libelle);
    }

    @GetMapping
    public List<Transaction> getAllTransactions() {
        return transactionService.getAllTransactions();
    }

    @GetMapping("/{id}")
    public Transaction getTransaction(@PathVariable Long id) {
        return transactionService.transactionById(id);
    }

    @DeleteMapping("/{id}")
    public void deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
    }
}
```

**Endpoints :** POST /api/transactions?senderId=...&receiverId=...&montant=...&libelle=..., GET /api/transactions, GET /api/transactions/{id}, DELETE /api/transactions/{id}.

---

## Résumé Partie 5 : ordre des contrôleurs et liens

| Contrôleur | Utilise | Protégé par JWT |
|------------|---------|-----------------|
| AuthController | UserRepository, PasswordEncoder, JwtUtil, AuthenticationManager | Non (URLs publiques) |
| UserController | UserService | Oui |
| AccountController | AccountService | Oui |
| TransactionController | TransactionService | Oui |

---

# Partie 6 – Mise en place du JWT (sécurité)

## Vue d’ensemble

**Objectif :** protéger l’API sans session serveur. Le client envoie un **token JWT** dans le header `Authorization: Bearer <token>`. Le serveur valide ce token et sait qui est connecté.

**Ordre de création des fichiers (à respecter) :**

| Ordre | Fichier à créer | Utilité |
|-------|-----------------|---------|
| **1** | `Security/JwtUtil.java` | Génère le token après login, extrait le username du token, vérifie si le token est valide. |
| **2** | `Security/AuthEntryPointJwt.java` | Envoie une réponse **401 Unauthorized** quand l’utilisateur n’est pas authentifié (pas de token ou token invalide). |
| **3** | `Security/AuthTokenFilter.java` | À chaque requête : lit le token dans le header, le valide avec JwtUtil, charge l’utilisateur et le met dans le SecurityContext. |
| **4** | `Security/WebSecurityConfig.java` | Configure Spring Security : quelles URLs sont publiques, quelle URL exige un token, enregistre le filtre JWT et le gestionnaire 401. |

**Prérequis avant de commencer la Partie 6 :**
- Le **CustomUserDetailsService** (Partie 4) doit exister : le filtre JWT en a besoin pour charger l’utilisateur à partir de l’email contenu dans le token.
- Le fichier **application.properties** doit contenir `app.jwt.secret` et `app.jwt.expiration` (voir Partie 1).

---

## Étape 6.1 – Créer JwtUtil (1er fichier)

**Emplacement :** `src/main/java/com/gestionBancaire/gestionBancaire/Security/JwtUtil.java`

**Utilité :**
- **Générer** un token JWT après un login réussi (appelé par AuthController).
- **Extraire** le username (email) depuis un token (appelé par AuthTokenFilter).
- **Valider** un token (signature, expiration, format) (appelé par AuthTokenFilter).

**Code complet :**

```java
package com.gestionBancaire.gestionBancaire.Security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    @Value("${app.jwt.expiration}")
    private int jwtExpirationMs;
    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key).build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateJwtToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (SecurityException e) {
            System.out.println("Invalid JWT signature: " + e.getMessage());
        } catch (MalformedJwtException e) {
            System.out.println("Invalid JWT token: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            System.out.println("JWT token is expired: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.out.println("JWT token is unsupported: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("JWT claims string is empty: " + e.getMessage());
        }
        return false;
    }
}
```

**Explication par partie :**

| Élément | Rôle |
|--------|------|
| `@Component` | Permet à Spring d’instancier cette classe et de l’injecter (dans AuthController et AuthTokenFilter). |
| `@Value("${app.jwt.secret}")` | Lit la clé secrète depuis `application.properties`. |
| `@Value("${app.jwt.expiration}")` | Lit la durée de vie du token en millisecondes. |
| `@PostConstruct init()` | Appelé une fois après l’injection : construit la `SecretKey` une seule fois (évite de la recalculer à chaque requête). |
| `generateToken(username)` | Construit un JWT avec le sujet = username (email), date d’émission, date d’expiration, signé en HS256. Retourne la chaîne du token. |
| `getUsernameFromToken(token)` | Parse le token avec la clé, récupère le “subject” (l’email). |
| `validateJwtToken(token)` | Tente de parser le token ; si une exception est levée (signature invalide, expiré, mal formé), retourne `false`, sinon `true`. |

## Étape 6.2 – Créer AuthEntryPointJwt (2e fichier)

**Emplacement :** `src/main/java/com/gestionBancaire/gestionBancaire/Security/AuthEntryPointJwt.java`

**Utilité :** Quand Spring Security détecte qu’une requête doit être authentifiée et qu’aucun utilisateur n’est connecté (pas de token ou token invalide), c’est cette classe qui est appelée. Elle envoie une réponse HTTP **401 Unauthorized** au client.

**Code complet :**

```java
package com.gestionBancaire.gestionBancaire.Security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Error: Unauthorized");
    }
}
```

**Explication :**

| Élément | Rôle |
|--------|------|
| `implements AuthenticationEntryPoint` | Interface Spring Security : “que faire quand l’accès est refusé (non authentifié) ?” |
| `commence(...)` | Méthode appelée dans ce cas : on envoie une erreur 401 avec le message "Error: Unauthorized". |

On crée ce fichier **avant** le filtre et la config pour pouvoir l’injecter dans **WebSecurityConfig**.

---

## Étape 6.3 – Créer AuthTokenFilter (3e fichier)

**Emplacement :** `src/main/java/com/gestionBancaire/gestionBancaire/Security/AuthTokenFilter.java`

**Utilité :** Ce filtre s’exécute **à chaque requête HTTP**. Il lit le header `Authorization: Bearer <token>`, valide le token avec **JwtUtil**, récupère l’utilisateur avec **CustomUserDetailsService**, et met cet utilisateur dans le **SecurityContext** pour que Spring Security considère la requête comme authentifiée.

**Code complet :**

```java
package com.gestionBancaire.gestionBancaire.Security;

import com.gestionBancaire.gestionBancaire.Service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtil jwtUtils;
    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUsernameFromToken(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            System.out.println("Cannot set user authentication: " + e);
        }
        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
```

**Explication par bloc :**

| Bloc | Rôle |
|------|------|
| `extends OncePerRequestFilter` | Filtre exécuté **une fois par requête** (pas plusieurs fois dans une même chaîne). |
| `parseJwt(request)` | Lit le header `Authorization` ; si c’est de la forme `Bearer <token>`, retourne la partie `<token>`, sinon `null`. |
| `jwt != null && jwtUtils.validateJwtToken(jwt)` | On ne fait la suite que si un token est présent et valide. |
| `getUsernameFromToken(jwt)` | On récupère l’email (username) stocké dans le token. |
| `userDetailsService.loadUserByUsername(username)` | On charge l’utilisateur depuis la BDD (CustomUserDetailsService + UserRepository). |
| `UsernamePasswordAuthenticationToken(...)` | On crée l’objet d’authentification Spring (utilisateur + rôles). |
| `SecurityContextHolder.getContext().setAuthentication(...)` | On met cet utilisateur dans le contexte : la requête est considérée comme authentifiée. |
| `filterChain.doFilter(request, response)` | On continue la chaîne de filtres (vers le contrôleur, etc.). |

## Étape 6.4 – Créer WebSecurityConfig (4e fichier)

**Emplacement :** `src/main/java/com/gestionBancaire/gestionBancaire/Security/WebSecurityConfig.java`

**Utilité :**
- Déclarer quelles URLs sont **publiques** (sans token) et lesquelles exigent une **authentification** (token JWT).
- Enregistrer le **filtre JWT** (AuthTokenFilter) dans la chaîne de filtres.
- Déclarer le **gestionnaire 401** (AuthEntryPointJwt).
- Fournir le **PasswordEncoder** (BCrypt) et l’**AuthenticationManager** (pour le login dans AuthController).

**Code complet :**

```java
package com.gestionBancaire.gestionBancaire.Security;

import com.gestionBancaire.gestionBancaire.Service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class WebSecurityConfig {
    @Autowired
    CustomUserDetailsService userDetailsService;
    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling.authenticationEntryPoint(unauthorizedHandler)
                )
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers("/api/auth/**", "/api/test/all").permitAll()
                                .anyRequest().authenticated()
                );
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

**Explication par partie :**

| Élément | Rôle |
|--------|------|
| `@Configuration` | Cette classe configure Spring (Security). |
| `authenticationEntryPoint(unauthorizedHandler)` | En cas d’accès non authentifié → appeler **AuthEntryPointJwt** (réponse 401). |
| `SessionCreationPolicy.STATELESS` | Pas de session serveur : l’état “connecté” vient uniquement du JWT à chaque requête. |
| `requestMatchers("/api/auth/**", "/api/test/all").permitAll()` | Ces URLs sont **accessibles sans token** (inscription, connexion, test). |
| `anyRequest().authenticated()` | Toutes les **autres** URLs exigent une authentification (donc un JWT valide). |
| `addFilterBefore(authenticationJwtTokenFilter(), ...)` | On insère **AuthTokenFilter** dans la chaîne, avant le filtre d’authentification par formulaire, pour que le JWT soit lu et le SecurityContext rempli avant les autres vérifications. |
| `PasswordEncoder` (BCrypt) | Utilisé à l’inscription pour hasher le mot de passe et au login pour le comparer. |
| `AuthenticationManager` | Utilisé dans **AuthController** pour authentifier email + mot de passe lors du **POST /api/auth/signin**. |

---

## Résumé : ordre des fichiers et liens entre eux

```
1. JwtUtil           → Génère / valide / extrait le username du token.
                       Utilisé par : AuthController (génération), AuthTokenFilter (validation + extraction).

2. AuthEntryPointJwt → Répond 401 si pas authentifié.
                       Utilisé par : WebSecurityConfig (exceptionHandling).

3. AuthTokenFilter   → À chaque requête : lit le token, le valide (JwtUtil), charge l’user (CustomUserDetailsService),
                       met l’user dans le SecurityContext.
                       Utilisé par : WebSecurityConfig (addFilterBefore).

4. WebSecurityConfig → Définit : URLs publiques vs protégées, filtre JWT, gestionnaire 401,
                       PasswordEncoder, AuthenticationManager.
```

**Flux complet côté requête protégée :**

1. Le client envoie `Authorization: Bearer <token>`.
2. **AuthTokenFilter** s’exécute : extrait le token, appelle **JwtUtil.validateJwtToken** puis **getUsernameFromToken**, puis **CustomUserDetailsService.loadUserByUsername**, puis remplit le **SecurityContext**.
3. Spring Security vérifie les règles de **WebSecurityConfig** : pour une URL protégée, il exige une authentification ; elle est maintenant présente grâce au filtre.
4. Si à une étape le token est absent ou invalide, le filtre ne remplit pas le contexte → Spring Security appelle **AuthEntryPointJwt** → réponse **401**.

---

# Partie 7 – Lancer et tester l’API

## Étape 7.1 – Lancer l’application

À la racine du projet (où se trouve `pom.xml`) :

```bash
./mvnw spring-boot:run
```

Sous Windows :

```bash
mvnw.cmd spring-boot:run
```

L’API est disponible sur **http://localhost:8080**.

## Étape 7.2 – Tester avec un client HTTP (Postman, curl, etc.)

**1. Inscription**

```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "name": "Jean Dupont",
  "email": "jean@example.com",
  "password": "motdepasse123",
  "adresse": "Paris"
}
```

Réponse attendue : **201** avec un message du type « Compte créé avec succès ! ».

**2. Connexion (récupérer le token)**

```http
POST http://localhost:8080/api/auth/signin
Content-Type: application/json

{
  "email": "jean@example.com",
  "password": "motdepasse123"
}
```

Réponse : corps = **une chaîne** (le token JWT). Copier ce token.

**3. Requête protégée (ex. liste des comptes)**

```http
GET http://localhost:8080/api/accounts
Authorization: Bearer VOTRE_TOKEN_ICI
```

Sans token ou avec un token invalide → **401 Unauthorized**. Avec un token valide → **200** et liste des comptes (ou tableau vide).

## Étape 7.3 – Récapitulatif des endpoints

| Méthode | URL | Auth | Rôle |
|---------|-----|------|------|
| POST | /api/auth/register | Non | Inscription |
| POST | /api/auth/signin | Non | Connexion (retourne le JWT) |
| GET/POST/PUT/DELETE | /api/users, /api/accounts, /api/transactions | Oui (JWT) | CRUD métier |

---

# Récapitulatif de l’architecture

| Couche | Rôle | Fichiers principaux |
|--------|------|----------------------|
| **Entity** | Modèle de données (tables) | User, Account, Transaction, Role, Statut |
| **Repository** | Accès BDD (CRUD + requêtes dérivées) | UserRepository, AccountRepository, TransactionRepository |
| **Service** | Logique métier | UserService, AccountService, TransactionService, CustomUserDetailsService |
| **Controller** | API REST | AuthController, UserController, AccountController, TransactionController |
| **Security** | JWT + règles d’accès | JwtUtil, AuthTokenFilter, WebSecurityConfig, AuthEntryPointJwt |

En suivant ce guide du début à la fin, vous obtenez un **TP complet** : création du projet, configuration, entités, repositories, services, contrôleurs, et mise en place du JWT de bout en bout.
