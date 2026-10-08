package com.friperie.felana.shop.dto.response;

/**
 * DTO PUBLIC des informations de la boutique (contact, paiement, identité).
 * Alimenté uniquement depuis {@code MagasinProperties} : aucune valeur en dur,
 * aucune logique métier. Consommé par la vitrine (footer, page de confirmation).
 */
public record ShopInfoResponse(
        String nom,
        String adresse,
        String telephone,
        String mvolaNumero,
        String airtelMoneyNumero,
        String orangeMoneyNumero,
        String email,
        String whatsapp,
        String nifStat
) {
}
