package com.friperie.felana.common.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "app.magasin")
@Getter
@Setter
public class MagasinProperties {
    private String nom = "Felana";
    private String adresse = "";
    private String telephone = "";
    /** Taux de TVA/taxe en décimal (ex: 0.20 pour 20%). 0 par défaut si non applicable. */
    private BigDecimal tauxTaxe = BigDecimal.ZERO;

    /** Numéros Mobile Money affichés au client pour le paiement manuel. */
    private String mvolaNumero = "";
    private String airtelMoneyNumero = "";
    private String orangeMoneyNumero = "";

    /** Coordonnées de contact de la boutique (vitrine / reçu). */
    private String email = "";
    private String whatsapp = "";
    /** NIF/STAT (peut être vide tant que non attribué). */
    private String nifStat = "";
}