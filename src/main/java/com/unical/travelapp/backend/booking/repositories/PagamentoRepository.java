package com.unical.travelapp.backend.booking.repositories;

import com.unical.travelapp.backend.booking.entity.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    //grazie jpa

}
