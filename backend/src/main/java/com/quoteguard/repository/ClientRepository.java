package com.quoteguard.repository;

import com.quoteguard.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findByUserId(Long userId);
    long countByUserId(Long userId);
}
