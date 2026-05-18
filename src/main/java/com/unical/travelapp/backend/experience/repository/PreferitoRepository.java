package com.unical.travelapp.backend.experience.repository;

import com.unical.travelapp.backend.experience.models.Preferito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PreferitoRepository extends JpaRepository<Preferito, Long> {

}
