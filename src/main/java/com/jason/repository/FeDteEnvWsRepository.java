package com.jason.repository;

import com.jason.model.FeDteEnvWs;
import com.jason.model.FeDteEnvWsId; // Importa la clase de ID
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FeDteEnvWsRepository extends JpaRepository<FeDteEnvWs, FeDteEnvWsId> {
    Optional<FeDteEnvWs> findById(FeDteEnvWsId id);
}