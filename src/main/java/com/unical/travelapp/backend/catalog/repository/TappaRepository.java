package com.unical.travelapp.backend.catalog.repository;

import com.unical.travelapp.backend.catalog.entity.Tappa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TappaRepository extends JpaRepository<Tappa, Long> {
}