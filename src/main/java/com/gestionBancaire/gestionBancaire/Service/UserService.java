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
    // permet de faire l'injection de dependance
    @Autowired
    private UserRepository userRepository;

    //Permet de recuperer tous les users
    public List<User> getAllUsers(){
        return userRepository.findAll();
    }

    //Permet de recuperer  un seul user
    public User getUserById (Long id){
        return userRepository.findById(id).orElseThrow(()-> new RuntimeException("Utilisateur Introuvable"));
    }

    //CREER UN USER
    public User createUser(User user){
        return userRepository.save(user);
    }

    //Mettre a jour un user
    public User userUpdate(Long id, User userDetails){
        User user = userRepository.findById(id).orElseThrow(()-> new RuntimeException("Utilisateur introuvable"));
        user.setRole(userDetails.getRole());
        user.setEmail(userDetails.getEmail());
        user.setAdresse(userDetails.getAdresse());
        user.setPassword(userDetails.getPassword());
        user.setRole(userDetails.getRole());
        user.setStatut(userDetails.getStatut());
        user.setUpdated_at(LocalDateTime.now());

        return userRepository.save(user);
    }

    //desactiver user
    public void desactiverUser(Long id){
        User user = userRepository.findById(id).orElseThrow(()-> new RuntimeException("Utilisateur Introuvable"));
        user.setStatut(Statut.Inactif);
        userRepository.save(user);
    }

    //activer user
    public void activerUser(Long id){
        User user = userRepository.findById(id).orElseThrow(()-> new RuntimeException("Utilisateur Introuvable"));
        user.setStatut(Statut.Actif);
        userRepository.save(user);
    }
}
