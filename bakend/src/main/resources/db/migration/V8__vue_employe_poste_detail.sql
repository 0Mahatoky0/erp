-- Vue de reference en lecture seule utilisee par le module employe.
-- Les donnees restent possedees par besoin ; aucune relation JPA inter-modules.
create or replace view vue_employe_poste_detail as
select p.id_poste,
       p.intitule,
       s.id_service,
       s.nom as service_nom,
       d.id_departement,
       d.nom as departement_nom
from poste p
join service s on s.id_service = p.id_service
join departement d on d.id_departement = s.id_departement;
