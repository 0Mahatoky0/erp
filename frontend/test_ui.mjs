/**
 * Verification de l'interface avec un vrai navigateur (Chrome systeme).
 * Parcours : connexion, tableau de bord, catalogue, creation d'une demande,
 * soumission, validation RH / Finance / Direction, controle budgetaire.
 */
import { chromium } from 'playwright'
import fs from 'node:fs'

const URL_APP = 'http://localhost:5173'
const DOSSIER = '/tmp/opencode/captures'
fs.mkdirSync(DOSSIER, { recursive: true })

let ok = 0
let ko = 0
const erreursConsole = []

function check(label, cond, extra = '') {
  if (cond) {
    ok += 1
    console.log(`  OK   ${label}`)
  } else {
    ko += 1
    console.log(`  ECHEC ${label} ${extra}`)
  }
}

const navigateur = await chromium.launch({ executablePath: '/usr/bin/google-chrome' })
const contexte = await navigateur.newContext({ viewport: { width: 1440, height: 900 } })
const page = await contexte.newPage()

page.on('console', (message) => {
  if (message.type() === 'error') {
    erreursConsole.push(message.text())
  }
})
page.on('pageerror', (erreur) => erreursConsole.push(erreur.message))
page.on('response', async (reponse) => {
  if (reponse.url().includes('/api/') && reponse.status() >= 400) {
    const corps = await reponse.text().catch(() => '')
    console.log(`  [API ${reponse.status()}] ${reponse.url().replace('http://localhost:8080/api', '')} ${corps.slice(0, 180)}`)
  }
})

/** Attend que le tableau soit rempli ou que l'etat vide soit affiche. */
async function attendreTableau() {
  await page
    .waitForFunction(
      () => document.querySelectorAll('.tableau tbody tr').length > 0 || document.querySelector('.etat-vide'),
      null,
      { timeout: 15000 },
    )
    .catch(() => {})
  await page.waitForTimeout(300)
}

async function capturer(nom) {
  try {
    await page.screenshot({ path: `${DOSSIER}/${nom}.png` })
  } catch (e) {
    console.log(`  (capture ${nom} impossible : ${e.message.split('\n')[0]})`)
  }
}

async function connecter(email, motDePasse) {
  await page.goto(`${URL_APP}/connexion`, { waitUntil: 'networkidle' })
  await page.fill('#email', email)
  await page.fill('#motDePasse', motDePasse)
  await page.click('button[type="submit"]')
  await page.waitForURL(/tableau-bord|\/$/, { timeout: 15000 })
  await page.waitForLoadState('networkidle')
}

console.log('== 1. Page de connexion ==')
await page.goto(`${URL_APP}/connexion`, { waitUntil: 'networkidle' })
check('titre de la page', (await page.title()).length > 0, await page.title())
check('formulaire de connexion present', await page.locator('#email').isVisible())
check('liens vers les comptes de demonstration', (await page.locator('.comptes-demo__bouton').count()) === 4)
await capturer('01-connexion')

console.log('== 2. Connexion manager ==')
await connecter('manager@tovo.mg', 'Manager@2026')
check('tableau de bord affiche', await page.locator('h1').first().innerText() === 'Bonjour ' + (await page.locator('.topbar__titre').innerText()) ? true : (await page.locator('h1').first().innerText()).startsWith('Bonjour'))
check('sidebar presente', await page.locator('.sidebar').isVisible())
await page.waitForSelector('.stat', { timeout: 15000 }).catch(() => {})
check('cartes de statistiques', (await page.locator('.stat').count()) >= 4)
await capturer('02-tableau-bord-manager')

console.log('== 3. Catalogue des postes (lecture) ==')
await page.click('.sidebar__lien:has-text("Demandes")')
await attendreTableau()
check('liste des demandes', (await page.locator('.tableau tbody tr').count()) > 0)
await capturer('03-demandes')

console.log('== 4. Creation d une demande ==')
await page.click('.sidebar__lien:has-text("Nouvelle demande")')
await page.waitForLoadState('networkidle')
await page.fill('#motif', 'Recrutement teste interface ' + Date.now())
await page.selectOption('#service-0', { index: 1 })
await page.fill('#intitule-0', 'Charge de clientele premium')
await page.fill('#budget-0', '1500000')
await page.fill('#effectif-0', '1')
await capturer('04-nouvelle-demande')
await page.click('button:has-text("Enregistrer et soumettre")')
await page.waitForURL(/demandes\/\d+/, { timeout: 15000 })
await page.waitForLoadState('networkidle')
const idDemande = Number(page.url().split('/demandes/')[1])
check('detail de la demande affiche', (await page.locator('.page__entete h1').innerText()).length > 0)
check('statut en validation', (await page.locator('.badge').first().innerText()).includes('validation'))
check('ligne avec etape courante', (await page.locator('.badge').allInnerTexts()).some((t) => t.includes('Etape')))
await capturer('05-demande-detail')

console.log('== 5. Validation RH ==')
await page.click('.sidebar__deconnexion, .topbar button:has-text("Deconnexion")')
await page.waitForURL(/connexion/, { timeout: 10000 })
await connecter('rh@tovo.mg', 'Rh@2026')
await page.click('.sidebar__lien:has-text("File de validation")')
await attendreTableau()
check('file de validation non vide', (await page.locator('.tableau tbody tr').count()) > 0)
await capturer('06-file-validation-rh')
await page.locator('.tableau tbody tr', { hasText: `Demande #${idDemande}` }).locator('button:has-text("Traiter")').click()
await page.waitForSelector('.modale', { timeout: 5000 })
check('modale de decision ouverte', await page.locator('.modale').isVisible())
await page.selectOption('#decision-file', 'APPROUVE')
await page.fill('#commentaire-file', 'Valide par RH via l interface')
await capturer('07-modale-decision')
await page.click('button:has-text("Enregistrer la decision")')
await page.waitForTimeout(1500)
check('notification de succes', (await page.locator('.notification').count()) > 0)
await capturer('08-apres-decision-rh')

console.log('== 6. Validation finance ==')
await page.click('.topbar button:has-text("Deconnexion")')
await page.waitForURL(/connexion/, { timeout: 10000 })
await connecter('finance@tovo.mg', 'Finance@2026')
await page.click('.sidebar__lien:has-text("Budgets")')
await attendreTableau()
check('tableau des budgets', (await page.locator('.tableau tbody tr').count()) > 0)
check('jauge de consommation', (await page.locator('.jauge__valeur').count()) > 0)
await capturer('09-budgets-finance')

await page.click('.sidebar__lien:has-text("File de validation")')
await attendreTableau()
await page.locator('.tableau tbody tr', { hasText: `Demande #${idDemande}` }).locator('button:has-text("Traiter")').click()
await page.waitForSelector('.modale', { timeout: 5000 })
check('champ montant verifie pour la finance', await page.locator('#montant-file').isVisible())
await page.fill('#commentaire-file', 'Budget verifie')
await page.click('button:has-text("Enregistrer la decision")')
await page.waitForTimeout(1500)
await capturer('10-apres-decision-finance')

console.log('== 7. Validation direction ==')
await page.click('.topbar button:has-text("Deconnexion")')
await page.waitForURL(/connexion/, { timeout: 10000 })
await connecter('direction@tovo.mg', 'Direction@2026')
await page.click('.sidebar__lien:has-text("File de validation")')
await attendreTableau()
const ligneDirection = page.locator('.tableau tbody tr', { hasText: `Demande #${idDemande}` })
check('file direction', (await ligneDirection.count()) > 0)
if ((await ligneDirection.count()) > 0) {
  await ligneDirection.locator('button:has-text("Traiter")').click()
  await page.waitForSelector('.modale', { timeout: 5000 })
  await page.fill('#commentaire-file', 'Decision finale de la direction')
  await page.click('button:has-text("Enregistrer la decision")')
  await page.waitForTimeout(1500)
}
await capturer('11-apres-decision-direction')

console.log('== 8. Catalogue des postes (RH) ==')
await page.click('.topbar button:has-text("Deconnexion")')
await page.waitForURL(/connexion/, { timeout: 10000 })
await connecter('rh@tovo.mg', 'Rh@2026')
await page.click('.sidebar__lien:has-text("Catalogue des postes")')
await attendreTableau()
check('catalogue des postes', (await page.locator('.tableau tbody tr').count()) > 0)
check('bouton de creation present', await page.locator('button:has-text("Nouveau poste")').isVisible())
await capturer('12-catalogue-postes')

await page.locator('button:has-text("Nouveau poste")').click()
await page.waitForSelector('.modale', { timeout: 5000 })
await page.fill('#intitule-form', 'Poste cree via l interface ' + Date.now())
await page.selectOption('#service-form', { index: 1 })
await capturer('13-modale-poste')
await page.click('.modale__pied button:has-text("Enregistrer")')
await page.waitForTimeout(1200)
check('notification de creation', (await page.locator('.notification').count()) > 0)
await capturer('14-catalogue-apres-creation')

console.log('== 9. Employes et utilisateurs ==')
await page.click('.sidebar__lien:has-text("Employes")')
await attendreTableau()
check('liste des employes', (await page.locator('.tableau tbody tr').count()) > 0)
await capturer('15-employes')

await page.click('.sidebar__lien:has-text("Utilisateurs")')
await attendreTableau()
check('liste des utilisateurs', (await page.locator('.tableau tbody tr').count()) > 0)
await capturer('16-utilisateurs')

console.log('== 10. Correction d une ligne (RM-021, RM-037) ==')
await page.setViewportSize({ width: 1440, height: 900 })
await page.click('.topbar button:has-text("Deconnexion")')
await page.waitForURL(/connexion/, { timeout: 10000 })
await connecter('manager@tovo.mg', 'Manager@2026')
await page.click('.sidebar__lien:has-text("Nouvelle demande")')
await page.waitForLoadState('networkidle')
await page.fill('#motif', 'Demande a corriger ' + Date.now())
await page.selectOption('#service-0', { index: 1 })
await page.fill('#intitule-0', 'Poste a corriger')
await page.fill('#budget-0', '800000')
await page.fill('#effectif-0', '1')
await page.click('button:has-text("Enregistrer et soumettre")')
await page.waitForURL(/demandes\/\d+/, { timeout: 15000 })
const idCorrection = Number(page.url().split('/demandes/')[1])

await page.click('.topbar button:has-text("Deconnexion")')
await page.waitForURL(/connexion/, { timeout: 10000 })
await connecter('rh@tovo.mg', 'Rh@2026')
await page.click('.sidebar__lien:has-text("File de validation")')
await attendreTableau()
await page
  .locator('.tableau tbody tr', { hasText: `Demande #${idCorrection}` })
  .locator('button:has-text("Traiter")')
  .click()
await page.waitForSelector('.modale', { timeout: 5000 })
await page.selectOption('#decision-file', 'A_CORRIGER')
await page.fill('#commentaire-file', 'Precisez le profil recherche')
await page.click('button:has-text("Enregistrer la decision")')
await page.waitForTimeout(1500)
check('demande de correction enregistree', (await page.locator('.notification').count()) > 0)
await capturer('18-correction-demandee')

await page.click('.topbar button:has-text("Deconnexion")')
await page.waitForURL(/connexion/, { timeout: 10000 })
await connecter('manager@tovo.mg', 'Manager@2026')
await page.goto(`${URL_APP}/demandes/${idCorrection}`, { waitUntil: 'networkidle' })
await attendreTableau()
check('bouton de correction propose au demandeur', await page.locator('button:has-text("Corriger")').isVisible())
check('encart de correction visible', await page.locator('.carte--accent').isVisible())
await page.locator('button:has-text("Corriger")').first().click()
await page.waitForSelector('.modale', { timeout: 5000 })
await page.fill('#corrige-intitule', 'Poste a corriger (profile renseigne)')
await page.fill('#corrige-profil', 'Comptable senior')
await page.fill('#corrige-budget', '900000')
await capturer('19-modale-correction')
await page.click('button:has-text("Enregistrer la correction")')
await page.waitForTimeout(1500)
check('ligne corrigee par le demandeur', (await page.locator('.notification').count()) > 0)
await page.locator('button:has-text("Resoumettre")').first().click()
await page.waitForTimeout(1500)
const badges = await page.locator('.badge').allInnerTexts()
check('ligne resoumise en validation', badges.some((t) => t.includes('En validation') || t.includes('Etape')))
await capturer('20-apres-resoumission')

console.log('== 11. Responsive mobile ==')
await page.setViewportSize({ width: 420, height: 860 })
await page.goto(`${URL_APP}/demandes`, { waitUntil: 'networkidle' })
await capturer('21-mobile-demandes')
check('rendu mobile sans erreur', erreursConsole.length === 0, erreursConsole.join(' | '))

await navigateur.close()

console.log()
console.log(`Resultat : ${ok} verifications OK, ${ko} echecs`)
if (erreursConsole.length) {
  console.log('Erreurs console :')
  erreursConsole.slice(0, 10).forEach((erreur) => console.log('  - ' + erreur))
}
process.exit(ko ? 1 : 0)
