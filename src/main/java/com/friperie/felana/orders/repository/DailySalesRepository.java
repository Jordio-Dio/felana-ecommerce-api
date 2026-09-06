package com.friperie.felana.orders.repository;

import com.friperie.felana.orders.domain.DailySales;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailySalesRepository extends JpaRepository<DailySales, Long> {

    Optional<DailySales> findByDate(LocalDate date);
}
