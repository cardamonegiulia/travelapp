package com.unical.travelapp.backend.catalog.repository;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface ItinerarioRepository extends JpaRepository<Itinerario, Long> {
    Optional<Itinerario> findByIdAndOrganizzatore_Id(Long id, Long organizzatoreId);
}
