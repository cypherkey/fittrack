package com.fittrack.service;

import com.fittrack.domain.Exercise;
import com.fittrack.domain.User;
import com.fittrack.domain.UserExerciseNote;
import com.fittrack.repository.UserExerciseNoteRepository;
import com.fittrack.web.dto.UserExerciseNotesResponse;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserExerciseNoteService {

	private final UserExerciseNoteRepository noteRepository;
	private final ExerciseService exerciseService;

	public UserExerciseNoteService(UserExerciseNoteRepository noteRepository, ExerciseService exerciseService) {
		this.noteRepository = noteRepository;
		this.exerciseService = exerciseService;
	}

	@Transactional(readOnly = true)
	public UserExerciseNotesResponse get(User user, String exerciseId) {
		exerciseService.requireUsableBy(user, exerciseId);
		String notes = noteRepository.findByUser_IdAndExercise_Id(user.getId(), exerciseId)
				.map(UserExerciseNote::getNotes)
				.orElse(null);
		return new UserExerciseNotesResponse(exerciseId, notes);
	}

	@Transactional(readOnly = true)
	public Map<String, String> notesByExerciseIds(String userId, Collection<String> exerciseIds) {
		Map<String, String> map = new HashMap<>();
		if (exerciseIds == null || exerciseIds.isEmpty()) {
			return map;
		}
		for (UserExerciseNote note : noteRepository.findByUser_IdAndExercise_IdIn(userId, exerciseIds)) {
			map.put(note.getExercise().getId(), note.getNotes());
		}
		return map;
	}

	@Transactional
	public UserExerciseNotesResponse upsert(User user, String exerciseId, String notes) {
		Exercise exercise = exerciseService.requireUsableBy(user, exerciseId);
		String trimmed = StringUtils.hasText(notes) ? notes.trim() : null;
		var existing = noteRepository.findByUser_IdAndExercise_Id(user.getId(), exerciseId);
		if (trimmed == null) {
			existing.ifPresent(noteRepository::delete);
			return new UserExerciseNotesResponse(exerciseId, null);
		}
		UserExerciseNote note = existing.orElseGet(() -> {
			UserExerciseNote created = new UserExerciseNote();
			created.setUser(user);
			created.setExercise(exercise);
			return created;
		});
		note.setNotes(trimmed);
		noteRepository.save(note);
		return new UserExerciseNotesResponse(exerciseId, trimmed);
	}
}