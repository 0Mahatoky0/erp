#!/usr/bin/env bash
#
# Test local complet du Bloc 1 (fichier unique, non versionne).
#
#   ./test_local.sh            # démarre ce qui manque, lance les 2 tests, nettoie
#   ./test_local.sh --no-start # ne démarre aucun serveur (utilise ceux déjà lancés)
#
# Le script :
#   1. vérifie les pré-requis (java, node, npm, psql, curl) ;
#   2. démarre le backend (8080) et le frontend (5173) s'ils ne répondent pas ;
#   3. lance le test API (tests/api_bloc1.sh) ;
#   4. lance le test navigateur (frontend/test_ui.mjs) ;
#   5. affiche un récapitulatif et renvoie 0 si tout est vert.
#
# Sortie : 0 = tout passe, 1 = au moins un test échoue, 2 = pré-requis manquant.

set -u

RACINE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
API_URL="${API_URL:-http://localhost:8080/api}"
URL_APP="${URL_APP:-http://localhost:5173}"
DEMARRER=1
[ "${1:-}" = "--no-start" ] && DEMARRER=0

COULEUR_OK=$'\033[32m'
COULEUR_KO=$'\033[31m'
COULEUR_INFO=$'\033[36m'
COULEUR_FIN=$'\033[0m'

if [ -t 1 ]; then
	COULEUR_OK=$'\033[32m'
	COULEUR_KO=$'\033[31m'
	COULEUR_INFO=$'\033[36m'
	COULEUR_FIN=$'\033[0m'
else
	COULEUR_OK=""
	COULEUR_KO=""
	COULEUR_INFO=""
	COULEUR_FIN=""
fi

info() { printf '%s==>%s %s\n' "$COULEUR_INFO" "$COULEUR_FIN" "$1"; }
ok() { printf '%s  OK  %s %s\n' "$COULEUR_OK" "$COULEUR_FIN" "$1"; }
ko() { printf '%s ECHEC%s %s\n' "$COULEUR_KO" "$COULEUR_FIN" "$1"; }

# ---------------------------------------------------------------------------
# 0. Pre-requis
# ---------------------------------------------------------------------------
info "Verification des pre-requis"
MANQUANT=0
for OUTIL in java node npm curl psql; do
	if command -v "$OUTIL" >/dev/null 2>&1; then
		ok "$OUTIL present"
	else
		ko "$OUTIL manquant"
		MANQUANT=1
	fi
done
if [ ! -d "$RACINE/frontend/node_modules" ]; then
	ko "frontend/node_modules absent (lancer : cd frontend && npm install)"
	MANQUANT=1
fi
if [ ! -f "$RACINE/bakend/src/main/resources/application.properties" ]; then
	ko "bakend/src/main/resources/application.properties absent (copier exemple.application.properties)"
	MANQUANT=1
fi
if [ "$MANQUANT" -ne 0 ]; then
	printf '\n%sPre-requis manquants : corriger avant de lancer les tests.%s\n' "$COULEUR_KO" "$COULEUR_FIN"
	exit 2
fi

# ---------------------------------------------------------------------------
# 1. Serveurs
# ---------------------------------------------------------------------------
api_disponible() { curl -s -o /dev/null -m 3 "$API_URL/auth/login" -X POST \
	-H 'Content-Type: application/json' -d '{"email":"x","motDePasse":"y"}'; }
front_disponible() { curl -s -o /dev/null -m 3 "$URL_APP/"; }

PID_BACKEND=""
PID_FRONTEND=""

nettoyer() {
	if [ -n "$PID_BACKEND" ]; then
		kill "$PID_BACKEND" 2>/dev/null
		printf '\n%sBackend arrete (pid %s).%s\n' "$COULEUR_INFO" "$PID_BACKEND" "$COULEUR_FIN"
	fi
	if [ -n "$PID_FRONTEND" ]; then
		kill "$PID_FRONTEND" 2>/dev/null
		printf '%sFrontend arrete (pid %s).%s\n' "$COULEUR_INFO" "$PID_FRONTEND" "$COULEUR_FIN"
	fi
}
trap nettoyer EXIT

info "Verification des serveurs"
if api_disponible; then
	ok "backend deja demarre sur ${API_URL%/api}"
elif [ "$DEMARRER" -eq 1 ]; then
	info "demarrage du backend (log : /tmp/backend-test-local.log)"
	(cd "$RACINE/bakend" && setsid ./mvnw -o spring-boot:run > /tmp/backend-test-local.log 2>&1 < /dev/null &)
	ATTENTE=0
	while [ "$ATTENTE" -lt 90 ]; do
		sleep 2
		ATTENTE=$((ATTENTE + 2))
		api_disponible && break
	done
	if api_disponible; then
		PID_BACKEND=$(lsof -ti tcp:8080 2>/dev/null | head -1)
		ok "backend demarre sur ${API_URL%/api}"
	else
		ko "le backend n a pas demarre (voir /tmp/backend-test-local.log)"
		exit 1
	fi
else
	ko "backend indisponible sur ${API_URL%/api} (lancer : cd bakend && ./mvnw -o spring-boot:run)"
	exit 1
fi

if front_disponible; then
	ok "frontend deja demarre sur $URL_APP"
elif [ "$DEMARRER" -eq 1 ]; then
	info "demarrage du frontend (log : /tmp/frontend-test-local.log)"
	(cd "$RACINE/frontend" && setsid npx vite --port 5173 --host > /tmp/frontend-test-local.log 2>&1 < /dev/null &)
	ATTENTE=0
	while [ "$ATTENTE" -lt 40 ]; do
		sleep 2
		ATTENTE=$((ATTENTE + 2))
		front_disponible && break
	done
	if front_disponible; then
		PID_FRONTEND=$(lsof -ti tcp:5173 2>/dev/null | head -1)
		ok "frontend demarre sur $URL_APP"
	else
		ko "le frontend n a pas demarre (voir /tmp/frontend-test-local.log)"
		exit 1
	fi
else
	ko "frontend indisponible sur $URL_APP (lancer : cd frontend && npm run dev)"
	exit 1
fi

# ---------------------------------------------------------------------------
# 2. Test API
# ---------------------------------------------------------------------------
info "Test fonctionnel de l API (tests/api_bloc1.sh)"
SORTIE_API=$("$RACINE/tests/api_bloc1.sh" 2>&1)
CODE_API=$?
printf '%s\n' "$SORTIE_API"
TOTAL_API=$(printf '%s' "$SORTIE_API" | grep -oE '[0-9]+ verifications OK' | grep -oE '^[0-9]+')
if [ "$CODE_API" -eq 0 ]; then
	ok "API : ${TOTAL_API:-0} verifications OK"
	RESULTAT_API=1
else
	ko "API : echec (voir ci-dessus)"
	RESULTAT_API=0
fi

# ---------------------------------------------------------------------------
# 3. Test navigateur
# ---------------------------------------------------------------------------
info "Test navigateur (frontend/test_ui.mjs)"
if [ -f "$RACINE/frontend/test_ui.mjs" ]; then
	SORTIE_UI=$(cd "$RACINE/frontend" && node test_ui.mjs 2>&1)
	CODE_UI=$?
	printf '%s\n' "$SORTIE_UI"
	TOTAL_UI=$(printf '%s' "$SORTIE_UI" | grep -oE '[0-9]+ verifications OK' | grep -oE '^[0-9]+')
	if [ "$CODE_UI" -eq 0 ]; then
		ok "navigateur : ${TOTAL_UI:-0} verifications OK"
		RESULTAT_UI=1
	else
		ko "navigateur : echec (voir ci-dessus)"
		RESULTAT_UI=0
	fi
else
	ko "frontend/test_ui.mjs introuvable"
	RESULTAT_UI=0
fi

# ---------------------------------------------------------------------------
# 4. Recapitulatif
# ---------------------------------------------------------------------------
printf '\n%s================ RECAPITULATIF ================%s\n' "$COULEUR_INFO" "$COULEUR_FIN"
printf '  API       : %s\n' "$([ "$RESULTAT_API" -eq 1 ] && echo "OK (${TOTAL_API:-0} verifications)" || echo "ECHEC")"
printf '  Navigateur: %s\n' "$([ "$RESULTAT_UI" -eq 1 ] && echo "OK (${TOTAL_UI:-0} verifications)" || echo "ECHEC")"
printf '  Interface : %s\n' "$URL_APP"
printf '  Comptes   : manager@tovo.mg/Manager@2026  rh@tovo.mg/Rh@2026\n'
printf '              finance@tovo.mg/Finance@2026  direction@tovo.mg/Direction@2026\n'

if [ "$RESULTAT_API" -eq 1 ] && [ "$RESULTAT_UI" -eq 1 ]; then
	printf '\n%sTOUT EST VERT%s\n' "$COULEUR_OK" "$COULEUR_FIN"
	exit 0
fi
printf '\n%sDES TESTS ONT ECHOUE%s\n' "$COULEUR_KO" "$COULEUR_FIN"
exit 1
