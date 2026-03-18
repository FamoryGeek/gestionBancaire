package com.gestionBancaire.gestionBancaire.Repository;

import com.gestionBancaire.gestionBancaire.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    User findById(String id);
}
