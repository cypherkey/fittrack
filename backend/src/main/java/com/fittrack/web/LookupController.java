package com.fittrack.web;

import com.fittrack.repository.EquipmentRepository;
import com.fittrack.repository.MuscleRepository;
import com.fittrack.web.dto.EquipmentResponse;
import com.fittrack.web.dto.MuscleResponse;
import java.util.Comparator;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Lookups", description = "Equipment and muscle catalogs")
public class LookupController {

	private final EquipmentRepository equipmentRepository;
	private final MuscleRepository muscleRepository;

	public LookupController(EquipmentRepository equipmentRepository, MuscleRepository muscleRepository) {
		this.equipmentRepository = equipmentRepository;
		this.muscleRepository = muscleRepository;
	}

	@GetMapping("/equipment")
	public List<EquipmentResponse> equipment() {
		return equipmentRepository.findAll().stream()
				.sorted(Comparator.comparing(e -> e.getName().toLowerCase()))
				.map(e -> new EquipmentResponse(e.getId(), e.getName()))
				.toList();
	}

	@GetMapping("/muscles")
	public List<MuscleResponse> muscles() {
		return muscleRepository.findAll().stream()
				.sorted(Comparator.comparing(m -> m.getName().toLowerCase()))
				.map(m -> new MuscleResponse(m.getId(), m.getName()))
				.toList();
	}
}
