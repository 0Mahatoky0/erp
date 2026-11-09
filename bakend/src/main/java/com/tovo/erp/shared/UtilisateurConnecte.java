package com.tovo.erp.shared;

/**
 * Identite technique de l'utilisateur authentifie.
 *
 * <p>Ce type appartient a {@code shared} : il ne reference aucune entite metier
 * et peut donc etre utilise par tous les modules sans creer de dependance vers
 * {@code iam}.
 */
public final class UtilisateurConnecte {

    private final Long id;
    private final String nom;
    private final String email;
    private final String role;
    private final Long idDepartement;
    private final String departementNom;

    public UtilisateurConnecte(Long id, String nom, String email, String role,
                               Long idDepartement, String departementNom) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.role = role;
        this.idDepartement = idDepartement;
        this.departementNom = departementNom;
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getEmail() {
        return email;
    }

    public String getRoleNom() {
        return role;
    }

    public RoleConnecte getRole() {
        return new RoleConnecte(role);
    }

    public DepartementConnecte getDepartement() {
        return departementNom == null ? null : new DepartementConnecte(idDepartement, departementNom);
    }

    /** Reference minimale du role, compatible avec les lectures existantes. */
    public record RoleConnecte(String nom) {

        public String getNom() {
            return nom;
        }
    }

    /** Reference minimale du departement, sans dependance vers le module besoin. */
    public record DepartementConnecte(Long id, String nom) {

        public Long getId() {
            return id;
        }

        public String getNom() {
            return nom;
        }
    }
}
