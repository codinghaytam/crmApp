package com.jit.agentInterface.repository;

import com.jit.agentInterface.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
}

