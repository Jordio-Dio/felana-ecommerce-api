package com.friperie.felana.orders.service;

import com.friperie.felana.orders.domain.DailySales;
import com.friperie.felana.orders.repository.DailySalesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DailySalesService {

    private final DailySalesRepository dailySalesRepository;

    @Transactional(readOnly = true)
    public DailySales findByDate(LocalDate date) {
        return dailySalesRepository.findByDate(date).orElse(null);
    }

    @Transactional
    public void ajouterVente(BigDecimal montant) {
        LocalDate aujourdhui = LocalDate.now();
        DailySales dailySales = dailySalesRepository.findByDate(aujourdhui)
                .orElseGet(() -> DailySales.builder()
                        .date(aujourdhui)
                        .montantTotal(BigDecimal.ZERO)
                        .nombreVentes(0)
                        .build());

        dailySales.setMontantTotal(dailySales.getMontantTotal().add(montant));
        dailySales.setNombreVentes(dailySales.getNombreVentes() + 1);
        dailySalesRepository.save(dailySales);
    }
}
