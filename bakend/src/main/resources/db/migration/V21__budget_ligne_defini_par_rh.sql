-- Le manager exprime seulement le besoin : le budget (et la fourchette salariale)
-- d'une ligne de demande sont definis par le RH lors de sa validation.
-- Tant que le RH n'a pas statue, budget_estime reste NULL.
-- La contrainte chk_budget_estime (budget_estime >= 0) reste valable : un CHECK est
-- satisfait quand la valeur est NULL.
ALTER TABLE demande_recrutement_poste
    ALTER COLUMN budget_estime DROP NOT NULL;
