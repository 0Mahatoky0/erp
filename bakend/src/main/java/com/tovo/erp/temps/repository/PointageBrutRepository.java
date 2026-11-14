package com.tovo.erp.temps.repository;

import com.tovo.erp.temps.model.PointageBrut;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PointageBrutRepository extends JpaRepository<PointageBrut, Long> {

    List<PointageBrut> findByIdEmployeAndHorodatageBetweenOrderByHorodatageAsc(
            Long idEmploye, LocalDateTime debut, LocalDateTime fin);
}
