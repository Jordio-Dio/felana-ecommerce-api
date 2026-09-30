# Déploiement

```
pull request ──> Tests
push sur main ─> Tests ─> Image Docker (GHCR) ─> Déploiement (Coolify) ─> sonde de santé
```

- **Tests** : `mvn verify` à chaque pull request et à chaque push sur `main`.
- **Image** : construite par GitHub Actions et publiée sur
  `ghcr.io/fortico261-sketch/felana-backend`, avec deux étiquettes : `latest` et
  `sha-<commit>`. Le VPS ne compile rien : il tire l'image.
- **Déploiement** : `scripts/deploy-coolify.sh` demande à Coolify de redéployer,
  attend la fin, puis interroge `/api/actuator/health`. Coolify ne bascule le trafic
  sur le nouveau conteneur que si sa sonde répond : un déploiement raté laisse
  l'ancienne version en ligne.

Tant que `COOLIFY_APP_UUID` n'est pas renseignée, l'étape de déploiement est sautée :
on peut fusionner la CI avant d'avoir configuré le serveur.

## Mise en place (une seule fois)

L'ordre compte : l'image doit exister sur GHCR avant que Coolify puisse la tirer.

### 1. Première image

Fusionner sur `main`. Le workflow publie l'image ; le déploiement est sauté.

### 2. Accès du serveur à l'image (privée)

Créer un jeton GitHub **classique** avec la seule permission `read:packages`
(les jetons « fine-grained » ne marchent pas avec GHCR), puis sur le VPS :

```sh
docker login ghcr.io -u <compte-github>   # coller le jeton comme mot de passe
```

Coolify réutilise cette connexion pour tirer l'image.

### 3. Dans Coolify

1. **Base** : New resource → PostgreSQL 16. Activer les sauvegardes planifiées.
2. **Application** : New resource → Docker Image →
   `ghcr.io/fortico261-sketch/felana-backend`, étiquette `latest`.
   - Port exposé : `8080`.
   - Domaine : l'adresse HTTPS de l'API (Coolify gère le certificat).
   - Healthcheck : chemin `/api/actuator/health`, port `8080`.
   - Limite mémoire : `768m` (le tas Java est plafonné à 384 Mo dans l'image).
   - Variables : `SPRING_PROFILES_ACTIVE=prod` et toutes les variables `${...}`
     de `src/main/resources/application-prod.properties` (base, JWT, SMTP, gérant).
     L'URL de la base est l'URL interne donnée par Coolify, au format
     `jdbc:postgresql://<hôte>:5432/<base>`.
3. **Jeton d'API** : Keys & Tokens → API tokens, permissions `deploy` et `read`
   (le script lit l'état du déploiement).
4. Déployer une première fois à la main et vérifier
   `https://<domaine>/api/actuator/health` → `{"status":"UP"}`.

### 4. Dans GitHub (le propriétaire du dépôt)

Sur un dépôt personnel, seuls les propriétaires peuvent gérer secrets et variables.
Settings → Secrets and variables → Actions :

| Type | Nom | Valeur |
|---|---|---|
| Secret | `COOLIFY_TOKEN` | le jeton de l'étape 3.3 |
| Variable | `COOLIFY_URL` | ex. `https://coolify.wanna-group.com` |
| Variable | `COOLIFY_APP_UUID` | l'uuid de l'application (dans son URL Coolify) |
| Variable | `HEALTHCHECK_URL` | `https://<domaine>/api/actuator/health` |

À partir de là, chaque push sur `main` déploie.

## Au quotidien

- **Redéployer sans changement** : Actions → CI/CD → Run workflow (sur `main`).
- **Revenir en arrière** : relancer (« Re-run all jobs ») une exécution plus ancienne
  réussie sur `main` ; elle republie l'image de son commit puis la déploie. Plus
  rapide : dans Coolify, passer l'étiquette de `latest` à `sha-<commit>` et
  redéployer (penser à la remettre à `latest` ensuite).
- **Logs de l'application** : dans Coolify, onglet Logs de l'application.

## Changer de VPS

Tout ce qui précède le déploiement (tests, image, GHCR) ne dépend pas du serveur.

1. **Base** : `pg_dump -Fc` sur l'ancien serveur, `pg_restore` sur le nouveau.
2. **Nouveau VPS avec Coolify** : refaire l'étape 3, puis changer `COOLIFY_URL`,
   `COOLIFY_APP_UUID` et `COOLIFY_TOKEN`. Rien d'autre à modifier.
3. **Autre panneau** (Dokploy…) ou **Docker seul** : remplacer l'étape « Déployer »
   du workflow par l'appel équivalent (API du panneau, ou
   `ssh … docker compose pull && docker compose up -d`). L'image reste la même.
4. Basculer le DNS du domaine de l'API, puis couper l'ancien.
