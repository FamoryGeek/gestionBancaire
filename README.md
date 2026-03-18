# Gestion Bancaire – API REST Spring Boot

Application backend de gestion bancaire (utilisateurs, comptes, transactions) sécurisée par **JWT** (JSON Web Token). Architecture en couches : **Entity**, **Repository**, **Service**, **Controller**, **Security**.

---

## Sommaire

1. [Structure du projet et utilité des fichiers](#1-structure-du-projet-et-utilité-des-fichiers)
2. [Installation et configuration de l'environnement](#2-installation-et-configuration-de-lenvironnement)
3. [Architecture en couches et explication du code](#3-architecture-en-couches-et-explication-du-code)
4. [Mise en place du JWT](#4-mise-en-place-du-jwt)

---

## 1. Structure du projet et utilité des fichiers

```
gestionBancaire/
├── pom.xml                          # Dépendances Maven (Spring Boot, JPA, MySQL, JWT, Security)
├── mvnw, mvnw.cmd                   # Wrapper Maven (exécution sans installer Maven)
├── .mvn/wrapper/                    # Fichiers du wrapper Maven
├── src/
│   ├── main/
│   │   ├── java/com/gestionBancaire/gestionBancaire/
│   │   │   ├── Application.java     # Point d'entrée Spring Boot
│   │   │   ├── Entity/              # Modèles JPA (tables en base)
│   │   │   │   ├── User.java
│   │   │   │   ├── Account.java
│   │   │   │   ├── Transaction.java
│   │   │   │   └── Enum/
│   │   │   │       ├── Role.java    # ADMIN, CLIENT
│   │   │   │       └── Statut.java  # Actif, Inactif
│   │   │   ├── Repository/          # Accès données (JpaRepository)
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── AccountRepository.java
│   │   │   │   └── TransactionRepository.java
│   │   │   ├── Service/             # Logique métier
│   │   │   │   ├── UserService.java
│   │   │   │   ├── AccountService.java
│   │   │   │   ├── TransactionService.java
│   │   │   │   ├── CustomUserDetailsService.java  # Chargement user pour Spring Security
│   │   │   │   └── JwtService.java  # Alternative JWT (extraction/validation par email)
│   │   │   ├── Controller/          # API REST
│   │   │   │   ├── AuthController.java   # Inscription / Connexion (JWT)
│   │   │   │   ├── UserController.java
│   │   │   │   ├── AccountController.java
│   │   │   │   └── TransactionController.java
│   │   │   └── Security/            # JWT + Spring Security
│   │   │       ├── JwtUtil.java           # Génération / validation des tokens
│   │   │       ├── AuthTokenFilter.java   # Filtre qui lit le JWT à chaque requête
│   │   │       ├── WebSecurityConfig.java # Règles d’accès et chaîne de filtres
│   │   │       └── AuthEntryPointJwt.java # Réponse 401 si non authentifié
│   │   └── resources/
│   │       └── application.properties    # Config DB + JWT (à créer, voir ci-dessous)
│   └── test/
│       └── java/.../ApplicationTests.java  # Test de chargement du contexte Spring
```

### Rôle des principaux fichiers

| Fichier | Rôle |
|--------|------|
| **Application.java** | Démarre Spring Boot et active le scan des composants. |
| **Entity/** | Entités JPA : mapping avec les tables `user`, `account`, `transaction`. |
| **Repository/** | Interfaces Spring Data JPA : requêtes CRUD et personnalisées. |
| **Service/** | Logique métier (création compte, virement, activation/désactivation user). |
| **Controller/** | Expose les endpoints REST ; `AuthController` gère login/register et renvoie le JWT. |
| **Security/** | Configuration JWT (génération, validation, filtre) et règles d’autorisation. |
| **pom.xml** | Déclare Spring Boot, JPA, MySQL, Security, JJWT (JWT). |

---

## 2. Installation et configuration de l'environnement

### Prérequis

- **JDK 21**
- **Maven 3.6+** (ou utiliser le wrapper `mvnw` fourni)
- **MySQL** (ou MariaDB) pour la base de données

### Étapes d’installation

1. **Cloner ou ouvrir le projet**

   ```bash
   cd gestionBancaire
   ```

2. **Créer la base MySQL**

   ```sql
   CREATE DATABASE gestion_bancaire CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

3. **Créer le fichier de configuration**

   Copier `src/main/resources/application.properties.example` vers `src/main/resources/application.properties`, puis adapter les valeurs (ce fichier est ignoré par Git pour ne pas exposer les secrets). Ou créer manuellement `application.properties` avec le contenu suivant :

   ```properties
   # Base de données
   spring.datasource.url=jdbc:mysql://localhost:3306/gestion_bancaire?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
   spring.datasource.username=VOTRE_UTILISATEUR
   spring.datasource.password=VOTRE_MOT_DE_PASSE
   spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

   # JPA / Hibernate
   spring.jpa.hibernate.ddl-auto=update
   spring.jpa.show-sql=true
   spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

   # JWT (utilisé par JwtUtil et JwtService)
   # Secret : une chaîne suffisamment longue et aléatoire (ex. 64 caractères)
   app.jwt.secret=VotreSecretJWTTresLongEtSecuriseMinimum64CaracteresPourHS256
   # Expiration du token en millisecondes (ex. 86400000 = 24 h)
   app.jwt.expiration=86400000
   ```

   - **JwtUtil** attend `app.jwt.secret` comme chaîne brute (il fait `jwtSecret.getBytes(UTF_8)`).
   - **JwtService** attend `app.jwt.secret` encodé en **Base64**. Si vous utilisez uniquement **JwtUtil** (comme dans `AuthController` et `AuthTokenFilter`), une chaîne brute suffit. Si vous utilisez **JwtService**, il faut une clé Base64 (ex. générée une fois et mise dans `app.jwt.secret`).

4. **Compiler et lancer l’application**

   ```bash
   ./mvnw spring-boot:run
   ```

   Sous Windows :

   ```bash
   mvnw.cmd spring-boot:run
   ```

5. **Vérifier**

   - L’API tourne par défaut sur `http://localhost:8080`.
   - Endpoints publics (sans JWT) : `POST /api/auth/register`, `POST /api/auth/signin`, `GET /api/test/all` (si configuré).
   - Les autres endpoints nécessitent le header : `Authorization: Bearer <token>`.

---

## 3. Architecture en couches et explication du code

### 3.1 Couche Entity (modèle de données)

- **User.java**  
  - `@Entity` : table `user`.  
  - `@Id`, `@GeneratedValue(strategy = GenerationType.AUTO)` : clé primaire auto-générée.  
  - `@Column(unique = true, nullable = false)` sur `email` ; `@Enumerated(EnumType.STRING)` pour `role` et `statut`.  
  - Lombok : `@Data`, `@AllArgsConstructor`, `@NoArgsConstructor` pour getters/setters et constructeurs.

- **Account.java**  
  - Table `account` avec `numeroCompte`, `solde`, dates.  
  - `@ManyToOne` + `@JoinColumn(name = "user_id")` : un compte appartient à un `User`.

- **Transaction.java**  
  - Table `transaction` : `libelle`, `Montant`, dates.  
  - `@ManyToOne` pour `sender` et `receiver` : une transaction lie deux comptes (débit / crédit).

- **Role.java** / **Statut.java**  
  - Énumérations pour les rôles (ADMIN, CLIENT) et le statut (Actif, Inactif).

### 3.2 Couche Repository (accès aux données)

- **UserRepository**  
  - `extends JpaRepository<User, Long>` : CRUD par défaut.  
  - `findByEmail(String email)` et `existsByEmail(String email)` pour l’auth et l’inscription.

- **AccountRepository**  
  - `findByUserId(Long userId)` : liste des comptes d’un utilisateur.

- **TransactionRepository**  
  - CRUD de base sur `Transaction`.

### 3.3 Couche Service (logique métier)

- **UserService**  
  - CRUD utilisateur, mise à jour (role, email, adresse, statut, etc.), activation/désactivation via `Statut.Actif` / `Statut.Inactif`.

- **AccountService**  
  - CRUD compte : création, lecture, mise à jour, suppression.

- **TransactionService**  
  - Liste / par id ; **création de virement** : vérification des comptes source et cible, solde suffisant, débit du `sender`, crédit du `receiver`, enregistrement de la transaction dans une même transaction métier (`@Transactional`).

- **CustomUserDetailsService**  
  - Implémente `UserDetailsService`.  
  - `loadUserByUsername(String email)` : charge un `User` depuis la BDD et le transforme en `org.springframework.security.core.userdetails.User` (username = email, password, roles) pour Spring Security.

- **JwtService**  
  - Alternative JWT : extraction du sujet (email), génération de token, vérification de validité et d’expiration. Utilise une clé Base64 (`Decoders.BASE64.decode(secretKey)`). Peut être utilisée si vous standardisez sur une clé Base64.

### 3.4 Couche Controller (API REST)

- **AuthController** (`/api/auth`)  
  - `POST /register` : vérifie que l’email n’existe pas, encode le mot de passe avec `PasswordEncoder`, crée un `User` avec `Role.CLIENT`, `Statut.Actif`, et enregistre.  
  - `POST /signin` : authentifie avec `AuthenticationManager` (email + password), puis génère un JWT via `JwtUtil.generateToken(username)` et renvoie le token en corps de réponse.

- **UserController** (`/api/users`)  
  - CRUD : POST (création), GET (liste), GET `/{id}`, PUT `/{id}`, plus PUT `off/{id}` (désactiver) et PUT `on/{id}` (activer).

- **AccountController** (`/api/accounts`)  
  - CRUD complet sur les comptes.

- **TransactionController** (`/api/transactions`)  
  - POST avec paramètres (senderId, receiverId, montant, libelle) pour créer un virement ; GET liste, GET `/{id}`, DELETE `/{id}`.

---

## 4. Mise en place du JWT

### 4.1 Flux global

1. **Inscription** : `POST /api/auth/register` → création du compte (mot de passe hashé avec BCrypt).  
2. **Connexion** : `POST /api/auth/signin` avec `email` et `password` → vérification par Spring Security → génération d’un JWT avec **JwtUtil** → renvoi du token.  
3. **Requêtes protégées** : le client envoie `Authorization: Bearer <token>`.  
4. **AuthTokenFilter** : lit le header, extrait le token, le valide avec **JwtUtil**, charge le `UserDetails` avec **CustomUserDetailsService**, et enregistre l’authentification dans le `SecurityContext`.  
5. Si le token est absent ou invalide : **AuthEntryPointJwt** renvoie 401 Unauthorized.

### 4.2 Composants JWT

#### JwtUtil (Security/JwtUtil.java)

- **Rôle** : génération, lecture et validation des tokens JWT (utilisé par `AuthController` et `AuthTokenFilter`).
- **Configuration** :  
  - `app.jwt.secret` : chaîne utilisée comme clé HMAC (elle est convertie en octets UTF-8).  
  - `app.jwt.expiration` : durée de vie du token en millisecondes.
- **@PostConstruct init()** : construit une `SecretKey` à partir de `jwtSecret` pour éviter de la recréer à chaque requête.
- **generateToken(String username)** :  
  - Construit un JWT avec sujet = username (email), date d’émission, date d’expiration, signé avec `HS256` et la clé.
- **getUsernameFromToken(String token)** :  
  - Parse le token avec `Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token)` et retourne le sujet (username).
- **validateJwtToken(String token)** :  
  - Tente de parser le token ; en cas d’erreur (signature invalide, token mal formé, expiré, etc.), log et retourne `false`.

#### AuthTokenFilter (Security/AuthTokenFilter.java)

- **Rôle** : filtre exécuté **une fois par requête** (`OncePerRequestFilter`).
- **doFilterInternal** :  
  - Appelle `parseJwt(request)` pour récupérer le token depuis le header `Authorization: Bearer <valeur>`.  
  - Si un token est présent et valide (`jwtUtils.validateJwtToken(jwt)`), extrait le username avec `jwtUtils.getUsernameFromToken(jwt)`, charge le `UserDetails` avec `userDetailsService.loadUserByUsername(username)`, crée un `UsernamePasswordAuthenticationToken` avec ces détails et les autorités, et le place dans `SecurityContextHolder.getContext().setAuthentication(...)`.  
  - Puis appelle `filterChain.doFilter(request, response)` pour continuer la chaîne.
- **parseJwt** : retourne la partie après `"Bearer "` dans le header `Authorization`, sinon `null`.

#### WebSecurityConfig (Security/WebSecurityConfig.java)

- **Rôle** : configurer la chaîne de sécurité HTTP.
- **Définition des beans** :  
  - `AuthTokenFilter` (filtre JWT).  
  - `AuthenticationManager` (pour l’authentification login dans `AuthController`).  
  - `PasswordEncoder` : `BCryptPasswordEncoder` pour hasher les mots de passe.
- **SecurityFilterChain** :  
  - Désactive CSRF et CORS (adaptable selon vos besoins).  
  - `exceptionHandling` : en cas d’échec d’authentification, utilise `AuthEntryPointJwt` (réponse 401).  
  - `sessionManagement` : `SessionCreationPolicy.STATELESS` (pas de session serveur, tout repose sur le JWT).  
  - `authorizeHttpRequests` :  
    - `/api/auth/**` et `/api/test/all` en `permitAll()`.  
    - Toute autre requête en `authenticated()`.  
  - Ajout du filtre JWT avant `UsernamePasswordAuthenticationFilter` : `addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class)`.

#### AuthEntryPointJwt (Security/AuthEntryPointJwt.java)

- **Rôle** : point d’entrée en cas d’exception d’authentification (token manquant ou invalide).
- **commence(...)** : envoie une réponse d’erreur HTTP 401 avec le message « Unauthorized ».

### 4.3 Résumé de la chaîne JWT

| Étape | Composant | Action |
|-------|-----------|--------|
| 1 | **AuthController** | Login réussi → `JwtUtil.generateToken(username)` → renvoi du token. |
| 2 | **Client** | Envoie `Authorization: Bearer <token>` sur les requêtes protégées. |
| 3 | **AuthTokenFilter** | Extrait le token, le valide avec **JwtUtil**, charge le user, met à jour le `SecurityContext`. |
| 4 | **WebSecurityConfig** | Exige une authentification pour toutes les URLs sauf `/api/auth/**` et `/api/test/all`. |
| 5 | **AuthEntryPointJwt** | Répond 401 si aucune authentification valide. |

---

## Exemple d’utilisation de l’API

**Inscription :**
```http
POST /api/auth/register
Content-Type: application/json

{"name":"Jean Dupont","email":"jean@example.com","password":"motdepasse123","adresse":"Paris"}
```

**Connexion (récupération du token) :**
```http
POST /api/auth/signin
Content-Type: application/json

{"email":"jean@example.com","password":"motdepasse123"}
```
Réponse : corps = le token JWT (chaîne).

**Requête protégée (ex. liste des comptes) :**
```http
GET /api/accounts
Authorization: Bearer <VOTRE_TOKEN_JWT>
```

---

*Documentation générée pour le projet Gestion Bancaire – Spring Boot 4.x, Java 21, JWT (JJWT 0.11.5), Spring Security.*
