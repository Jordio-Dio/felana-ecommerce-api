#!/usr/bin/env bash
# Demande à Coolify de redéployer l'application (il tire l'image :latest de GHCR),
# attend la fin du déploiement, puis vérifie la sonde de santé publique.
#
# Variables : COOLIFY_URL, COOLIFY_APP_UUID, COOLIFY_TOKEN, HEALTHCHECK_URL (facultative)
set -euo pipefail

: "${COOLIFY_URL:?COOLIFY_URL manquante}"
: "${COOLIFY_APP_UUID:?COOLIFY_APP_UUID manquante}"
: "${COOLIFY_TOKEN:?COOLIFY_TOKEN manquant}"

api() {
  curl -fsS -H "Authorization: Bearer $COOLIFY_TOKEN" "$@"
}

deployment=$(api -X POST "$COOLIFY_URL/api/v1/deploy?uuid=$COOLIFY_APP_UUID&force=false" |
  jq -r '.deployments[0].deployment_uuid // empty')
if [ -z "$deployment" ]; then
  echo "::error::Coolify n'a lancé aucun déploiement pour $COOLIFY_APP_UUID"
  exit 1
fi
echo "Déploiement $deployment lancé"

# 60 × 10 s : 10 minutes au plus
status=""
for _ in $(seq 1 60); do
  status=$(api "$COOLIFY_URL/api/v1/deployments/$deployment" | jq -r '.status')
  echo "  statut : $status"
  case "$status" in
    finished) break ;;
    failed | cancelled*)
      echo "::error::Déploiement $status : voir les logs dans Coolify"
      exit 1
      ;;
  esac
  sleep 10
done

if [ "$status" != finished ]; then
  echo "::error::Déploiement toujours « $status » après 10 minutes"
  exit 1
fi

if [ -n "${HEALTHCHECK_URL:-}" ]; then
  curl -fsS --retry 6 --retry-delay 10 --retry-all-errors "$HEALTHCHECK_URL"
  echo
fi
