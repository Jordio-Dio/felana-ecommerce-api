package com.friperie.felana.orders.dto.response;

import com.friperie.felana.orders.domain.DailySales;

import java.math.BigDecimal;

public record DailySalesResponse(
        BigDecimal montantTotal,
        int nombreVentes
) {
    public static DailySalesResponse from(DailySales sales) {
        return new DailySalesResponse(sales.getMontantTotal(), sales.getNombreVentes());
    }
}
