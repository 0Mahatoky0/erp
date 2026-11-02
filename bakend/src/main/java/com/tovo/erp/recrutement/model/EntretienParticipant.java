package com.tovo.erp.recrutement.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "entretien_participant")
public class EntretienParticipant {
    @EmbeddedId
    private Id id;

    @Embeddable
    public static class Id implements Serializable {
        @Column(name = "id_entretien") private Long idEntretien;
        @Column(name = "id_utilisateur") private Long idUtilisateur;
        public Id() {}
        public Id(Long idEntretien, Long idUtilisateur) {
            this.idEntretien = idEntretien; this.idUtilisateur = idUtilisateur;
        }
        public Long getIdEntretien() { return idEntretien; }
        public void setIdEntretien(Long value) { idEntretien = value; }
        public Long getIdUtilisateur() { return idUtilisateur; }
        public void setIdUtilisateur(Long value) { idUtilisateur = value; }
        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Id id)) return false;
            return Objects.equals(idEntretien, id.idEntretien) && Objects.equals(idUtilisateur, id.idUtilisateur);
        }
        @Override public int hashCode() { return Objects.hash(idEntretien, idUtilisateur); }
    }
    public Id getId() { return id; }
    public void setId(Id value) { id = value; }
}
