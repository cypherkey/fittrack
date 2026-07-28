package com.fittrack.repository;

import com.fittrack.domain.Equipment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, String> {

	Optional<Equipment> findByNameIgnoreCase(String name);
}
