package com.friperie.felana.orders.dto.response;

import com.friperie.felana.orders.domain.StatutCommande;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Vue complète, prête à être imprimée/affichée comme un reçu/facture.
 * Volontairement "à plat" (pas de sous-objets imbriqués profonds) pour
 * rester simple à consommer côté frontend (impression, génération PDF
 * client-side avec jsPDF par exemple).
 */
public record InvoiceResponse(
        String numeroFacture,
        Instant dateEmission,

        String magasinNom,
        String magasinAdresse,

        String magasinEmail,
        String magasinWhatsapp,
        String magasinTelephone,
        String magasinNifStat,

        String mvolaNumero,
        String airtelMoneyNumero,
        String orangeMoneyNumero,

        String clientNomComplet,
        String clientTelephone,
        String clientEmail,

        String vendeurNom,

        List<InvoiceLigneResponse> lignes,

        BigDecimal sousTotal,
        BigDecimal remise,
        BigDecimal tauxTaxe,
        BigDecimal montantTaxe,
        BigDecimal total,

        String modePaiement,

        StatutCommande statutPaiement
) {
}