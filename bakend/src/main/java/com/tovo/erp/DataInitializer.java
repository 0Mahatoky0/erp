package com.tovo.erp;

import com.tovo.erp.besoin.model.Budget;
import com.tovo.erp.besoin.model.Departement;
import com.tovo.erp.employe.model.Employe;
import com.tovo.erp.besoin.model.Poste;
import com.tovo.erp.iam.model.Role;
import com.tovo.erp.besoin.model.Service;
import com.tovo.erp.iam.model.Utilisateur;
import com.tovo.erp.iam.model.RoleNom;
import com.tovo.erp.employe.model.StatutEmploye;
import com.tovo.erp.besoin.repository.BudgetRepository;
import com.tovo.erp.besoin.repository.DepartementRepository;
import com.tovo.erp.employe.repository.EmployeRepository;
import com.tovo.erp.besoin.repository.PosteRepository;
import com.tovo.erp.iam.repository.RoleRepository;
import com.tovo.erp.besoin.repository.ServiceRepository;
import com.tovo.erp.iam.repository.UtilisateurRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Donnees de demarrage : roles, departements, services, comptes, postes, employes et budgets.
 *
 * <p>Le remplissage n'a lieu que sur une base vide : l'initialisation est idempotente
 * et n'ecrase jamais des donnees metier.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final DepartementRepository departementRepository;
    private final ServiceRepository serviceRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PosteRepository posteRepository;
    private final EmployeRepository employeRepository;
    private final BudgetRepository budgetRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                           DepartementRepository departementRepository,
                           ServiceRepository serviceRepository,
                           UtilisateurRepository utilisateurRepository,
                           PosteRepository posteRepository,
                           EmployeRepository employeRepository,
                           BudgetRepository budgetRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.departementRepository = departementRepository;
        this.serviceRepository = serviceRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.posteRepository = posteRepository;
        this.employeRepository = employeRepository;
        this.budgetRepository = budgetRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (roleRepository.count() > 0) {
            return;
        }

        Role manager = role(RoleNom.MANAGER);
        Role rh = role(RoleNom.RH);
        Role finance = role(RoleNom.FINANCE);
        Role direction = role(RoleNom.DIRECTION);

        Departement commercial = departement("Commercial");
        Departement production = departement("Production");
        Departement support = departement("Support");

        Service serviceCommercial = service(commercial, "Ventes");
        Service serviceProduction = service(production, "Atelier");
        Service serviceSupport = service(support, "Ressources Humaines");

        utilisateur("Manager Commercial", "manager@tovo.mg", "Manager@2026", manager, commercial);
        utilisateur("Responsable RH", "rh@tovo.mg", "Rh@2026", rh, support);
        utilisateur("Responsable Finance", "finance@tovo.mg", "Finance@2026", finance, support);
        utilisateur("Directeur General", "direction@tovo.mg", "Direction@2026", direction, commercial);

        Poste posteCommercial = poste(serviceCommercial, "Chargé de clientèle",
                "Accueille et suit les clients du portefeuille commercial.", 900000, 1400000);
        poste(serviceProduction, "Chef d'atelier", "Encadre l'équipe de production.", 1000000, 1500000);
        poste(serviceSupport, "Chargé de recrutement", "Pilote le processus de recrutement.", 950000, 1400000);

        employe("EMP-0001", "Rakoto", "Naina", "naina.rakoto@tovo.mg", posteCommercial, StatutEmploye.ACTIF);
        employe("EMP-0002", "Andrian", "Tiana", "tiana.andrian@tovo.mg", posteCommercial, StatutEmploye.PARTI);
        employe("EMP-0003", "Rasoa", "Faniry", "faniry.rasoa@tovo.mg", posteCommercial, StatutEmploye.ACTIF);

        int exercice = LocalDate.now().getYear();
        for (int annee : new int[] {exercice - 1, exercice, exercice + 1}) {
            budget(serviceCommercial, annee, 45000000);
            budget(serviceProduction, annee, 38000000);
            budget(serviceSupport, annee, 22000000);
        }

        System.out.println("""
                [ERP] Donnees de demarrage creees.
                   Comptes de test (mot de passe initial) :
                     manager@tovo.mg    / Manager@2026
                     rh@tovo.mg         / Rh@2026
                     finance@tovo.mg    / Finance@2026
                     direction@tovo.mg  / Direction@2026
                """);
    }

    private Role role(RoleNom nom) {
        Role role = new Role();
        role.setNom(nom.name());
        return roleRepository.save(role);
    }

    private Departement departement(String nom) {
        Departement departement = new Departement();
        departement.setNom(nom);
        return departementRepository.save(departement);
    }

    private Service service(Departement departement, String nom) {
        Service service = new Service();
        service.setDepartement(departement);
        service.setNom(nom);
        return serviceRepository.save(service);
    }

    private Utilisateur utilisateur(String nom, String email, String motDePasse, Role role,
                                    Departement departement) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(nom);
        utilisateur.setEmail(email);
        utilisateur.setMotDePasse(passwordEncoder.encode(motDePasse));
        utilisateur.setRole(role);
        utilisateur.setDepartement(departement);
        utilisateur.setActif(true);
        return utilisateurRepository.save(utilisateur);
    }

    private Poste poste(Service service, String intitule, String description,
                        long salaireMin, long salaireMax) {
        Poste poste = new Poste();
        poste.setService(service);
        poste.setIntitule(intitule);
        poste.setDescription(description);
        poste.setSalaireMin(BigDecimal.valueOf(salaireMin));
        poste.setSalaireMax(BigDecimal.valueOf(salaireMax));
        poste.setActif(true);
        return posteRepository.save(poste);
    }

    private void employe(String matricule, String nom, String prenom, String email, Poste poste,
                         StatutEmploye statut) {
        Employe employe = new Employe();
        employe.setMatricule(matricule);
        employe.setNom(nom);
        employe.setPrenom(prenom);
        employe.setEmail(email);
        employe.setPoste(poste);
        employe.setStatut(statut);
        employe.setDateEmbauche(LocalDate.now().minusYears(2));
        employeRepository.save(employe);
    }

    private void budget(Service service, int exercice, long montant) {
        Budget budget = new Budget();
        budget.setService(service);
        budget.setExercice(exercice);
        budget.setMontantAlloue(BigDecimal.valueOf(montant));
        budget.setMontantDepense(BigDecimal.ZERO);
        budget.setMontantEngage(BigDecimal.ZERO);
        budgetRepository.save(budget);
    }
}
