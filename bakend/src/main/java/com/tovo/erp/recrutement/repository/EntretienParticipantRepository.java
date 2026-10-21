package com.tovo.erp.recrutement.repository;

import com.tovo.erp.recrutement.domain.EntretienParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntretienParticipantRepository extends JpaRepository<EntretienParticipant, EntretienParticipant.Id> {
}
