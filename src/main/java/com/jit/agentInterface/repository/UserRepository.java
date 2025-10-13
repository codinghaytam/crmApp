package com.jit.agentInterface.repository;

import com.jit.agentInterface.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByNomAndPrenom(String nom, String prenom);
    Optional<User> findByEmail(String email);
}
