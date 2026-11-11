UPDATE employe e
SET id_utilisateur = u.id_utilisateur
FROM utilisateur u
WHERE e.id_utilisateur IS NULL
  AND e.email IS NOT NULL
  AND lower(e.email) = lower(u.email)
  AND u.actif = TRUE
  AND EXISTS (
      SELECT 1
      FROM role r
      WHERE r.id_role = u.id_role
        AND r.nom = 'EMPLOYE'
  );
