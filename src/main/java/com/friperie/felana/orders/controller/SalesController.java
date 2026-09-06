package com.friperie.felana.orders.controller;

import com.friperie.felana.orders.domain.DailySales;
import com.friperie.felana.orders.dto.response.DailySalesResponse;
import com.friperie.felana.orders.service.DailySalesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/v1/sales")
@RequiredArgsConstructor
@Tag(name = "Ventes", description = "Statistiques de ventes")
public class SalesController {

    private final DailySalesService dailySalesService;

    @Operation(summary = "Statistiques des ventes du jour")
    @GetMapping("/today")
    public ResponseEntity<DailySalesResponse> getToday() {
        LocalDate today = LocalDate.now();
        DailySales sales = dailySalesService.findByDate(today);
        if (sales == null) {
            sales = DailySales.builder()
                    .date(today)
                    .montantTotal(BigDecimal.ZERO)
                    .nombreVentes(0)
                    .build();
        }
        return ResponseEntity.ok(DailySalesResponse.from(sales));
    }
}
