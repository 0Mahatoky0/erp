#!/usr/bin/env bash
#
# Test fonctionnel de l'API du Bloc 1.
#
# Parcours complet : connexion -> referentiels -> catalogue des postes ->
# demande multi-lignes -> regles de saisie -> soumission -> circuit de validation
# RH / FINANCE / DIRECTION -> controle budgetaire -> creation du poste ->
# correction et resoumission -> perimetre MANAGER -> tableau de bord.
#
# Dependances : bash, curl et node (utilise pour lire le JSON, Equivalent de jq).
#
# Utilisation :
#   ./tests/api_bloc1.sh
#   API_URL=http://localhost:8080/api ./tests/api_bloc1.sh
#
# Sortie : 0 si toutes les verifications passent, 1 sinon.

set -u

BASE="${API_URL:-http://localhost:8080/api}"
SUFFIXE="$(date +%s)"
ok=0
ko=0

if ! command -v node >/dev/null 2>&1; then
	echo "node est necessaire pour lire les reponses JSON (remplace jq)." >&2
	exit 2
fi

# ---------------------------------------------------------------------------
# Lecteur JSON (node) : get / len / has / findid / findkey / hasid / contains
# ---------------------------------------------------------------------------
LECTEUR_JSON=$(cat <<'LECTEUR'
const fs = require('fs')
const chemin = process.argv[1] || ''
const operation = process.argv[2] || 'get'
const argument = process.argv[3]
const argument2 = process.argv[4]

let donnees = null
try {
	donnees = JSON.parse(fs.readFileSync(0, 'utf8'))
} catch (erreur) {
	process.stdout.write('')
	process.exit(0)
}

function suivre(valeur, chemin) {
	if (valeur === null || valeur === undefined) return null
	for (const partie of chemin.split('.')) {
		if (partie === '') continue
		if (valeur === null || valeur === undefined) return null
		valeur = valeur[partie]
	}
	return valeur === undefined ? null : valeur
}

function elementParId(tableau, identifiant) {
	if (!Array.isArray(tableau)) return null
	return tableau.find((element) => String(element.id) === String(identifiant)) || null
}

function elementParCle(tableau, cle, valeur) {
	if (!Array.isArray(tableau)) return null
	return tableau.find((element) => String(element[cle]) === String(valeur)) || null
}

switch (operation) {
	case 'get': {
		const valeur = suivre(donnees, chemin)
		if (valeur === null || valeur === undefined) process.stdout.write('')
		else if (typeof valeur === 'object') process.stdout.write(JSON.stringify(valeur))
		else process.stdout.write(String(valeur))
		break
	}
	case 'len': {
		const valeur = suivre(donnees, chemin)
		process.stdout.write(Array.isArray(valeur) ? String(valeur.length) : '0')
		break
	}
	case 'has': {
		const valeur = suivre(donnees, chemin)
		process.stdout.write(valeur === null || valeur === undefined ? 'false' : 'true')
		break
	}
	case 'findid': {
		const element = elementParId(suivre(donnees, chemin), argument)
		process.stdout.write(element ? JSON.stringify(element) : '')
		break
	}
	case 'findkey': {
		const element = elementParCle(suivre(donnees, chemin), argument, argument2)
		process.stdout.write(element ? JSON.stringify(element) : '')
		break
	}
	case 'hasid': {
		process.stdout.write(elementParId(suivre(donnees, chemin), argument) ? 'true' : 'false')
		break
	}
	case 'contains': {
		const valeur = suivre(donnees, chemin)
		const texte = JSON.stringify(valeur === null ? donnees : valeur).toLowerCase()
		process.stdout.write(texte.includes(String(argument).toLowerCase()) ? 'true' : 'false')
		break
	}
	default:
		process.stdout.write('')
}
LECTEUR
)

json_get() { printf '%s' "$1" | node -e "$LECTEUR_JSON" "$2" get; }
json_len() { printf '%s' "$1" | node -e "$LECTEUR_JSON" "$2" len; }
json_has() { printf '%s' "$1" | node -e "$LECTEUR_JSON" "$2" has; }
json_findid() { printf '%s' "$1" | node -e "$LECTEUR_JSON" "$2" findid "$3"; }
json_findkey() { printf '%s' "$1" | node -e "$LECTEUR_JSON" "$2" findkey "$3" "$4"; }
json_hasid() { printf '%s' "$1" | node -e "$LECTEUR_JSON" "$2" hasid "$3"; }
json_contains() { printf '%s' "$1" | node -e "$LECTEUR_JSON" "$2" contains "$3"; }

# ---------------------------------------------------------------------------
# Appels HTTP
# ---------------------------------------------------------------------------
TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT
FICHIER_REPONSE="$TMP_DIR/reponse.json"
STATUS=""
BODY=""

api() {
	local methode="$1" chemin="$2" jeton="${3:-}" corps="${4:-}"
	local args=(-s -o "$FICHIER_REPONSE" -w '%{http_code}' -X "$methode" -H 'Accept: application/json')
	[ -n "$corps" ] && args+=(-H 'Content-Type: application/json' -d "$corps")
	[ -n "$jeton" ] && args+=(-H "Authorization: Bearer $jeton")
	STATUS=$(curl "${args[@]}" "$BASE$chemin")
	BODY=$(cat "$FICHIER_REPONSE")
}

apercu() { printf '%s' "${1:0:200}"; }

verif() {
	local libelle="$1" condition="$2" detail="${3:-}"
	if eval "$condition" >/dev/null 2>&1; then
		ok=$((ok + 1))
		printf '  OK   %s\n' "$libelle"
	else
		ko=$((ko + 1))
		printf '  ECHEC %s %s\n' "$libelle" "$(apercu "$detail")"
	fi
}

connexion() {
	api POST /auth/login "" "$(printf '{"email":"%s","motDePasse":"%s"}' "$1" "$2")"
	json_get "$BODY" token
}

# ---------------------------------------------------------------------------
echo "== 1. Connexion (RM-001, RM-002) =="
api POST /auth/login "" '{"email":"manager@tovo.mg","motDePasse":"Manager@2026"}'
verif "login manager" '[ "$STATUS" = 200 ] && [ "$(json_get "$BODY" profil.role)" = MANAGER ]' "$BODY"
MGR=$(json_get "$BODY" token)

api POST /auth/login "" '{"email":"manager@tovo.mg","motDePasse":"mauvais"}'
verif "login refuse si mauvais mot de passe (RM-002)" '[ "$STATUS" = 401 ]' "$BODY"

api GET /auth/me "$MGR"
verif "profil manager" '[ "$STATUS" = 200 ] && [ "$(json_get "$BODY" email)" = manager@tovo.mg ]' "$BODY"

RH=$(connexion rh@tovo.mg Rh@2026)
FIN=$(connexion finance@tovo.mg Finance@2026)
DIR=$(connexion direction@tovo.mg Direction@2026)
verif "comptes RH / FINANCE / DIRECTION connectes" '[ -n "$RH" ] && [ -n "$FIN" ] && [ -n "$DIR" ]' ""

# ---------------------------------------------------------------------------
echo "== 2. Referentiels =="
api GET /referentiel/departements "$MGR"
verif "departements" '[ "$STATUS" = 200 ] && [ "$(json_len "$BODY" .)" = 3 ]' "$BODY"

api GET /referentiel/services "$MGR"
SERVICES="$BODY"
verif "services" '[ "$STATUS" = 200 ] && [ "$(json_len "$BODY" .)" -ge 3 ]' "$BODY"

api GET /referentiel/employes "$MGR"
EMPLOYES="$BODY"
verif "employes" '[ "$STATUS" = 200 ] && [ "$(json_len "$BODY" .)" -ge 3 ]' "$BODY"

SERVICE_VENTES_ID=$(json_get "$(json_findkey "$SERVICES" . nom Ventes)" id)
EMPLOYE_ID=$(json_get "$EMPLOYES" 0.id)
verif "service Ventes trouve" '[ -n "$SERVICE_VENTES_ID" ]' "$SERVICES"
verif "employe de remplacement disponible" '[ -n "$EMPLOYE_ID" ]' "$EMPLOYES"

# ---------------------------------------------------------------------------
echo "== 3. Catalogue des postes (RM-006 a RM-011) =="
api GET /postes "$RH"
verif "liste des postes" '[ "$STATUS" = 200 ] && [ "$(json_len "$BODY" .)" -ge 3 ]' "$BODY"

INTITULE="Charge de clientele premium $SUFFIXE"
api POST /postes "$RH" "$(printf '{"idService":%s,"intitule":"%s","description":"Poste de test","niveau":"A","salaireMin":1200000,"salaireMax":2500000}' "$SERVICE_VENTES_ID" "$INTITULE")"
verif "creation poste (RM-006)" '[ "$STATUS" = 200 ] && [ "$(json_get "$BODY" intitule)" = "$INTITULE" ]' "$BODY"
POSTE_ID=$(json_get "$BODY" id)

api POST /postes "$RH" "$(printf '{"idService":%s,"intitule":"  %s  ","description":"Doublon"}' "$SERVICE_VENTES_ID" "${INTITULE^^}")"
verif "doublon poste refuse (RM-010)" '[ "$STATUS" = 409 ] || [ "$STATUS" = 400 ]' "$BODY"

api POST /postes "$MGR" "$(printf '{"idService":%s,"intitule":"Poste manager %s","description":"x","niveau":"A"}' "$SERVICE_VENTES_ID" "$SUFFIXE")"
verif "creation poste reservee au RH (RM-007)" '[ "$STATUS" = 403 ]' "$BODY"

api POST /postes "$RH" "$(printf '{"intitule":"Poste sans service %s"}' "$SUFFIXE")"
verif "poste sans service refuse" '[ "$STATUS" = 400 ]' "$BODY"

api POST /postes "$RH" "$(printf '{"idService":%s,"intitule":"Poste salaires incoherents %s","description":"x","salaireMin":3000000,"salaireMax":1000000}' "$SERVICE_VENTES_ID" "$SUFFIXE")"
verif "fourchette de salaire incoherente refusee" '[ "$STATUS" = 400 ]' "$BODY"

# ---------------------------------------------------------------------------
echo "== 4. Demande multi-lignes (RM-012 a RM-020) =="
CORPS_DEMANDE=$(cat <<JSON
{"motif":"Accroissement activite $SUFFIXE","dateSouhaitee":"2026-12-01","lignes":[
 {"idService":$SERVICE_VENTES_ID,"idPoste":$POSTE_ID,"typeBesoin":"CREATION","intituleDemande":"Charge de clientele premium","descriptionPoste":"Profil commercial","profilRecherche":"3 ans d experience","effectif":1},
 {"idService":$SERVICE_VENTES_ID,"typeBesoin":"REMPLACEMENT","idEmployeRemplace":$EMPLOYE_ID,"typeRemplacement":"DEFINITIF","motifRemplacement":"Depart a la retraite","dateDebutRemplacement":"2026-12-01","dateFinRemplacement":"2027-01-31","intituleDemande":"Remplacement employe","effectif":1}]}
JSON
)
api POST /demandes "$MGR" "$CORPS_DEMANDE"
verif "creation demande multi-lignes (RM-014)" '[ "$STATUS" = 200 ] && [ "$(json_get "$BODY" nombreLignes)" = 2 ]' "$BODY"
DEMANDE_ID=$(json_get "$BODY" id)

api GET "/demandes/$DEMANDE_ID" "$MGR"
DETAIL="$BODY"
verif "statut initial BROUILLON" '[ "$(json_get "$DETAIL" statut)" = BROUILLON ]' "$DETAIL"
LIGNE1=$(json_get "$DETAIL" lignes.0.id)
LIGNE2=$(json_get "$DETAIL" lignes.1.id)
verif "budget non defini par le manager (defini par le RH)" '[ -z "$(json_get "$DETAIL" lignes.0.budgetEstime)" ] || [ "$(json_get "$DETAIL" lignes.0.budgetEstime)" = null ]' "$DETAIL"

# ---------------------------------------------------------------------------
echo "== 5. Regles de saisie (RM-015 a RM-031) =="
api PUT "/demandes/$DEMANDE_ID" "$MGR" "$(printf '{"motif":"Autre motif %s","dateSouhaitee":"2026-11-15"}' "$SUFFIXE")"
verif "modification entete en brouillon (RM-019)" '[ "$STATUS" = 200 ]' "$BODY"

api POST "/demandes/$DEMANDE_ID/lignes" "$MGR" "$(printf '{"idService":%s,"typeBesoin":"REMPLACEMENT","idEmployeRemplace":%s,"typeRemplacement":"DEFINITIF","intituleDemande":"Ligne invalide","effectif":1}' "$SERVICE_VENTES_ID" "$EMPLOYE_ID")"
verif "remplacement sans dates refuse (RM-028)" '[ "$STATUS" = 400 ]' "$BODY"

api POST "/demandes/$DEMANDE_ID/lignes" "$MGR" "$(printf '{"idService":%s,"typeBesoin":"CREATION","intituleDemande":"Ligne sans effectif","effectif":0}' "$SERVICE_VENTES_ID")"
verif "effectif nul refuse (RM-022)" '[ "$STATUS" = 400 ]' "$BODY"

# ---------------------------------------------------------------------------
echo "== 6. Soumission (RM-032, RM-041 a RM-047) =="
api POST "/demandes/$DEMANDE_ID/soumettre" "$MGR"
verif "soumission acceptee" '[ "$STATUS" = 200 ] && [ "$(json_get "$BODY" statut)" = EN_VALIDATION ]' "$BODY"

api PUT "/demandes/$DEMANDE_ID/lignes/$LIGNE1" "$MGR" '{"intituleDemande":"modif","effectif":2}'
verif "ligne non modifiable apres soumission (RM-019)" '[ "$STATUS" = 409 ] || [ "$STATUS" = 400 ]' "$BODY"

api GET "/demandes/$DEMANDE_ID" "$MGR"
verif "etape courante = RH" '[ "$(json_get "$BODY" lignes.0.etapeCourante)" = RH ]' "$BODY"

# ---------------------------------------------------------------------------
echo "== 7. Circuit de validation (RM-033 a RM-043) =="
api GET /validation/file "$RH"
verif "file de validation RH non vide" '[ "$STATUS" = 200 ] && [ "$(json_hasid "$BODY" . "$LIGNE1")" = true ]' "$BODY"

api GET /validation/file "$FIN"
verif "file de validation FINANCE limitee a son etape (RM-039)" '[ "$STATUS" = 200 ] && [ "$(json_hasid "$BODY" . "$LIGNE1")" = false ]' "$BODY"

api POST "/validation/lignes/$LIGNE1/decision" "$FIN" '{"decision":"APPROUVE","commentaire":"par finance"}'
verif "validation refusee si mauvais role (RM-039)" '[ "$STATUS" = 403 ]' "$BODY"

api POST "/validation/lignes/$LIGNE1/decision" "$RH" '{"decision":"APPROUVE","commentaire":"RH sans budget"}'
verif "approbation RH sans budget refusee (budget defini par le RH)" '[ "$STATUS" = 400 ]' "$BODY"

api POST "/validation/lignes/$LIGNE1/decision" "$RH" '{"decision":"APPROUVE","commentaire":"RH salaires incoherents","budgetEstime":1500000,"salaireMinEstime":3000000,"salaireMaxEstime":1000000}'
verif "approbation RH avec salaires incoherents refusee" '[ "$STATUS" = 400 ]' "$BODY"

api POST "/validation/lignes/$LIGNE1/decision" "$RH" '{"decision":"APPROUVE","commentaire":"RH valide","budgetEstime":1500000,"salaireMinEstime":1200000,"salaireMaxEstime":2500000}'
verif "validation RH acceptee" '[ "$STATUS" = 200 ] && [ "$(json_get "$BODY" statut)" = EN_ATTENTE_FINANCE ]' "$BODY"

api POST "/validation/lignes/$LIGNE1/decision" "$RH" '{"decision":"APPROUVE","commentaire":"doublon"}'
verif "double validation refusee (RM-040)" '[ "$STATUS" = 403 ] || [ "$STATUS" = 409 ]' "$BODY"

api GET "/demandes/$DEMANDE_ID" "$RH"
verif "ligne 1 passee a l etape FINANCE" '[ "$(json_get "$BODY" lignes.0.etapeCourante)" = FINANCE ]' "$BODY"

# ---------------------------------------------------------------------------
echo "== 8. Controle budgetaire (RM-048 a RM-056) =="
api GET /budgets "$FIN"
BUDGETS="$BODY"
verif "liste des budgets" '[ "$STATUS" = 200 ] && [ "$(json_len "$BODY" .)" -ge 1 ]' "$BODY"
verif "budget du service Ventes present" '[ -n "$(json_findkey "$BUDGETS" . service Ventes)" ]' "$BUDGETS"

api POST "/validation/lignes/$LIGNE1/decision" "$FIN" '{"decision":"APPROUVE","commentaire":"Finance valide","montantVerifie":999}'
verif "montant verifie different refuse (RM-054)" '[ "$STATUS" = 400 ]' "$BODY"

api POST "/validation/lignes/$LIGNE1/decision" "$FIN" '{"decision":"APPROUVE","commentaire":"Finance valide","montantVerifie":1500000}'
verif "validation FINANCE acceptee" '[ "$STATUS" = 200 ]' "$BODY"

api GET "/demandes/$DEMANDE_ID" "$FIN"
verif "ligne 1 passee a l etape DIRECTION" '[ "$(json_get "$BODY" lignes.0.etapeCourante)" = DIRECTION ]' "$BODY"

# ---------------------------------------------------------------------------
echo "== 8bis. Budget insuffisant (RM-051, RM-052) =="
api POST /demandes "$MGR" "$(printf '{"motif":"Budget trop important %s","dateSouhaitee":"2026-12-01","lignes":[{"idService":%s,"typeBesoin":"CREATION","intituleDemande":"Poste hors budget","effectif":1}]}' "$SUFFIXE" "$SERVICE_VENTES_ID")"
verif "demande hors budget creee" '[ "$STATUS" = 200 ]' "$BODY"
GROS_ID=$(json_get "$BODY" id)
api POST "/demandes/$GROS_ID/soumettre" "$MGR" >/dev/null
api GET "/demandes/$GROS_ID" "$RH"
GROS_LIGNE=$(json_get "$BODY" lignes.0.id)
api POST "/validation/lignes/$GROS_LIGNE/decision" "$RH" '{"decision":"APPROUVE","commentaire":"RH","budgetEstime":900000000}' >/dev/null
api POST "/validation/lignes/$GROS_LIGNE/decision" "$FIN" '{"decision":"APPROUVE","commentaire":"Finance"}'
verif "budget insuffisant refuse (RM-051)" '([ "$STATUS" = 400 ] || [ "$STATUS" = 409 ]) && [ "$(json_contains "$BODY" . budget)" = true ]' "$BODY"
api GET "/demandes/$GROS_ID" "$FIN"
verif "ligne hors budget non acceptee" '[ "$(json_get "$BODY" lignes.0.statut)" != ACCEPTEE ]' "$BODY"

# ---------------------------------------------------------------------------
echo "== 9. Decision finale et creation du poste (RM-009, RM-066) =="
api POST "/validation/lignes/$LIGNE1/decision" "$DIR" '{"decision":"APPROUVE","commentaire":"Direction valide"}'
verif "validation DIRECTION acceptee" '[ "$STATUS" = 200 ] && [ "$(json_get "$BODY" statut)" = ACCEPTEE ]' "$BODY"

api GET "/demandes/$DEMANDE_ID" "$DIR"
LIGNE=$(json_findid "$BODY" lignes "$LIGNE1")
verif "ligne acceptee" '[ "$(json_get "$LIGNE" statut)" = ACCEPTEE ]' "$LIGNE"
verif "poste cree et rattache (RM-009/RM-066)" '[ "$(json_get "$LIGNE" idPoste)" != "" ]' "$LIGNE"

api GET /postes "$RH"
verif "poste present au catalogue" '[ "$(json_hasid "$BODY" . "$(json_get "$LIGNE" idPoste)")" = true ]' "$BODY"

# ---------------------------------------------------------------------------
echo "== 10. Separation des responsabilites (RM-005) =="
api POST /demandes "$RH" "$(printf '{"motif":"Auto validation %s","dateSouhaitee":"2026-12-01","lignes":[{"idService":%s,"typeBesoin":"CREATION","intituleDemande":"Poste RH","effectif":1}]}' "$SUFFIXE" "$SERVICE_VENTES_ID")"
verif "demande RH creee" '[ "$STATUS" = 200 ]' "$BODY"
AUTO_ID=$(json_get "$BODY" id)
api POST "/demandes/$AUTO_ID/soumettre" "$RH"
verif "soumission par RH acceptee" '[ "$STATUS" = 200 ]' "$BODY"
api GET "/demandes/$AUTO_ID" "$RH"
AUTO_LIGNE=$(json_get "$BODY" lignes.0.id)
api POST "/validation/lignes/$AUTO_LIGNE/decision" "$RH" '{"decision":"APPROUVE","commentaire":"auto"}'
verif "auto validation refusee (RM-005)" '[ "$STATUS" = 403 ]' "$BODY"

# ---------------------------------------------------------------------------
echo "== 11. Correction et resoumission (RM-036 a RM-038) =="
api POST "/validation/lignes/$LIGNE2/decision" "$RH" '{"decision":"A_CORRIGER","commentaire":"Preciser le poste"}'
verif "demande de correction acceptee" '[ "$STATUS" = 200 ] && [ "$(json_get "$BODY" statut)" = A_CORRIGER ]' "$BODY"

api GET "/demandes/$DEMANDE_ID" "$RH"
verif "statut global A_CORRIGER" '[ "$(json_get "$BODY" statut)" = A_CORRIGER ]' "$BODY"
L2=$(json_findid "$BODY" lignes "$LIGNE2")
verif "ligne 2 en correction, episode 1" '[ "$(json_get "$L2" statut)" = A_CORRIGER ] && [ "$(json_get "$L2" episode)" = 1 ]' "$L2"

api PUT "/demandes/$DEMANDE_ID/lignes/$LIGNE2" "$MGR" '{"intituleDemande":"Remplacement employe corrige","effectif":1,"motifRemplacement":"Depart a la retraite","dateDebutRemplacement":"2026-12-01","dateFinRemplacement":"2027-01-31"}'
verif "ligne A_CORRIGER modifiable par le demandeur (RM-037)" '[ "$STATUS" = 200 ]' "$BODY"

api POST "/demandes/$DEMANDE_ID/lignes/$LIGNE2/resoumettre" "$MGR"
verif "resoumission acceptee" '[ "$STATUS" = 200 ]' "$BODY"

api GET "/demandes/$DEMANDE_ID" "$RH"
L2=$(json_findid "$BODY" lignes "$LIGNE2")
verif "episode incrementee (RM-040)" '[ "$(json_get "$L2" episode)" = 2 ]' "$L2"
verif "retour a l etape RH (RM-038)" '[ "$(json_get "$L2" etapeCourante)" = RH ]' "$L2"

# ---------------------------------------------------------------------------
echo "== 12. Perimetre MANAGER (RM-067) =="
api GET /demandes "$MGR"
verif "manager ne voit que ses demandes" '[ "$(json_hasid "$BODY" . "$AUTO_ID")" = false ]' "$BODY"
api GET /demandes "$RH"
verif "RH voit toutes les demandes" '[ "$(json_hasid "$BODY" . "$AUTO_ID")" = true ]' "$BODY"

# ---------------------------------------------------------------------------
echo "== 13. Tableau de bord =="
api GET /dashboard "$RH"
verif "tableau de bord" '[ "$STATUS" = 200 ] && [ "$(json_contains "$BODY" . demandes)" = true ]' "$BODY"

# ---------------------------------------------------------------------------
echo "== 14. Gestion des utilisateurs (RM-004) =="
api GET /utilisateurs "$RH"
verif "liste des utilisateurs" '[ "$STATUS" = 200 ] && [ "$(json_len "$BODY" .)" -ge 4 ]' "$BODY"

# ---------------------------------------------------------------------------
echo
echo "Resultat : $ok verifications OK, $ko echecs"
[ "$ko" -eq 0 ] || exit 1
