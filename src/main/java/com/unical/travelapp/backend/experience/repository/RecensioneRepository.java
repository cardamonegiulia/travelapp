package com.unical.travelapp.backend.experience.repository;

import com.unical.travelapp.backend.experience.models.Recensione;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecensioneRepository extends JpaRepository<Recensione, Long> {

}
