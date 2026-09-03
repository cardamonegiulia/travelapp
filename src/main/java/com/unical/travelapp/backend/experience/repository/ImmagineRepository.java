package com.unical.travelapp.backend.experience.repository;

import com.unical.travelapp.backend.experience.models.Immagine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImmagineRepository extends JpaRepository<Immagine, Long> {

    Page<Immagine> findByProprietario_Id(Long proprietarioId, Pageable pageable);
}
