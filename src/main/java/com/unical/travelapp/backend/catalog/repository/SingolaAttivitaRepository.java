package com.unical.travelapp.backend.catalog.repository;
import com.unical.travelapp.backend.catalog.entity.SingolaAttivita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface SingolaAttivitaRepository extends JpaRepository<SingolaAttivita, Long> {
    Optional<SingolaAttivita> findByIdAndOrganizzatore_Id(Long id, Long organizzatoreId);
}
