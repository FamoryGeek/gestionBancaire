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

    //CREATE
    @PostMapping
    public  User createUser(@RequestBody User user){
        return userService.createUser(user);
    }

    // Read all
    @GetMapping
    public List<User> getAllUsers(){
        return userService.getAllUsers();
    }

    //read by id
    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id){
        return userService.getUserById(id);
    }

    //UPDATE
    @PutMapping("/{id}")
    public User userUpdate(@PathVariable Long id, @RequestBody User user){
        return userService.userUpdate(id, user);
    }

    //desactiver
    @PutMapping("/off/{id}")
    public void  desactiverUser(@PathVariable Long id){
       userService.desactiverUser(id);
    }

    //activer
    @PutMapping("/on/{id}")
    public void  activerUser(@PathVariable Long id){
        userService.activerUser(id);
    }
}
