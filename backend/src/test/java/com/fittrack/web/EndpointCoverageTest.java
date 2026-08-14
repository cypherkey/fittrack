package com.fittrack.web;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fittrack.domain.User;
import com.fittrack.repository.UserRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class EndpointCoverageTest {

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> "jdbc:sqlite:./target/endpoint-coverage.db?foreign_keys=true&journal_mode=WAL");
		registry.add("fittrack.jwt.secret", () -> "fittrack-test-secret-change-me-must-be-at-least-256-bits!!");
		registry.add("fittrack.seed.load-images", () -> "false");
		registry.add("fittrack.seed.download-images", () -> "false");
	}

	@Autowired
	private WebApplicationContext context;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;

	private MockMvc mockMvc;
	private String adminToken;
	private String otherToken;

	@BeforeEach
	void setUp() throws Exception {
		mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
		adminToken = login("admin", "admin");
		if (userRepository.findByUsername("other").isEmpty()) {
			User other = new User();
			other.setUsername("other");
			other.setPasswordHash(passwordEncoder.encode("otherpass"));
			other.setDisplayName("Other");
			other.setEmail("other@localhost");
			userRepository.save(other);
		}
		otherToken = login("other", "otherpass");
	}

	private String login(String username, String password) throws Exception {
		MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
				.andExpect(status().isOk())
				.andReturn();
		return objectMapper.readTree(login.getResponse().getContentAsString()).get("token").asText();
	}

	@Test
	void authMeAndActuatorAndOpenApi() throws Exception {
		mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("admin"))
				.andExpect(jsonPath("$.admin").value(true))
				.andExpect(jsonPath("$.useMetric").value(true));

		mockMvc.perform(patch("/api/v1/me")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"useMetric\":false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.useMetric").value(false));

		mockMvc.perform(patch("/api/v1/me")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"useMetric\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.useMetric").value(true));

		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
		mockMvc.perform(get("/actuator/info")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
	}

	@Test
	void adminUserManagement() throws Exception {
		mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + otherToken))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.username=='admin')].admin").value(org.hamcrest.Matchers.hasItem(true)));

		MvcResult created = mockMvc.perform(post("/api/v1/users")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"coach","password":"coachpass","displayName":"Coach","email":"coach@localhost","admin":false}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username").value("coach"))
				.andExpect(jsonPath("$.admin").value(false))
				.andReturn();
		String coachId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

		mockMvc.perform(put("/api/v1/users/" + coachId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"displayName":"Head Coach","password":"coachpass2"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.displayName").value("Head Coach"));

		String coachToken = login("coach", "coachpass2");
		mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + coachToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("coach"))
				.andExpect(jsonPath("$.admin").value(false));

		mockMvc.perform(delete("/api/v1/users/" + coachId).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNoContent());

		mockMvc.perform(delete("/api/v1/users/" + objectMapper.readTree(
						mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + adminToken))
								.andReturn().getResponse().getContentAsString()).get("id").asText())
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isBadRequest());
	}

	@Test
	void lookupsExercisesTemplatesWorkoutsAndReorder() throws Exception {
		mockMvc.perform(get("/api/v1/equipment").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/muscles").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/exercise").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray());
		mockMvc.perform(get("/api/v1/exercise/51ababe0-e7cc-40d3-a3ef-7d6fb418fbac").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value("51ababe0-e7cc-40d3-a3ef-7d6fb418fbac"));

		MvcResult custom = mockMvc.perform(post("/api/v1/exercise")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Coverage Curl","level":"BEGINNER","instructions":"x","trackedParameters":3}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String customId = objectMapper.readTree(custom.getResponse().getContentAsString()).get("id").asText();

		mockMvc.perform(put("/api/v1/exercise/" + customId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Coverage Curl 2","level":"BEGINNER","instructions":"x","trackedParameters":3}
								"""))
				.andExpect(status().isOk());

		MvcResult template = mockMvc.perform(post("/api/v1/templates")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name":"Coverage T",
								  "visibility":"PRIVATE",
								  "sets":[
								    {"exerciseId":"51ababe0-e7cc-40d3-a3ef-7d6fb418fbac","setNumber":1,"reps":10,"weightKg":10.0},
								    {"exerciseId":"51ababe0-e7cc-40d3-a3ef-7d6fb418fbac","setNumber":2,"reps":8,"weightKg":12.0}
								  ]
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		JsonNode tBody = objectMapper.readTree(template.getResponse().getContentAsString());
		String templateId = tBody.get("id").asText();
		String setA = tBody.get("sets").get(0).get("id").asText();
		String setB = tBody.get("sets").get(1).get("id").asText();

		mockMvc.perform(get("/api/v1/templates").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/templates/" + templateId).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk());

		mockMvc.perform(patch("/api/v1/templates/" + templateId + "/sets/reorder")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"items":[{"setId":"%s","setNumber":20},{"setId":"%s","setNumber":10}]}
								""".formatted(setA, setB)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sets[0].setNumber").value(10));

		mockMvc.perform(put("/api/v1/templates/" + templateId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Coverage T2","visibility":"PRIVATE","sets":[{"exerciseId":"51ababe0-e7cc-40d3-a3ef-7d6fb418fbac","setNumber":1,"reps":5}]}
								"""))
				.andExpect(status().isOk());

		MvcResult clone = mockMvc.perform(post("/api/v1/templates/" + templateId + "/clone")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Coverage Clone " + java.util.UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.startedAt").isEmpty())
				.andExpect(jsonPath("$.endedAt").isEmpty())
				.andExpect(jsonPath("$.completed").value(false))
				.andReturn();
		String workoutId = objectMapper.readTree(clone.getResponse().getContentAsString()).get("id").asText();

		String defaultName = "Workout " + java.time.LocalDate.now();
		mockMvc.perform(post("/api/v1/templates/" + templateId + "/clone")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"" + defaultName + "\"}"));
		mockMvc.perform(post("/api/v1/templates/" + templateId + "/clone")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"" + defaultName + "\"}"))
				.andExpect(status().isConflict());

		mockMvc.perform(post("/api/v1/workouts/" + workoutId + "/start")
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.startedAt").isNotEmpty())
				.andExpect(jsonPath("$.completed").value(false));

		String startedAt = objectMapper.readTree(mockMvc.perform(get("/api/v1/workouts/" + workoutId)
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString()).get("startedAt").asText();

		mockMvc.perform(post("/api/v1/workouts/" + workoutId + "/start")
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.startedAt").value(startedAt));

		mockMvc.perform(post("/api/v1/workouts/" + workoutId + "/complete")
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.endedAt").isNotEmpty())
				.andExpect(jsonPath("$.completed").value(true))
				.andExpect(jsonPath("$.startedAt").value(startedAt));

		MvcResult workout = mockMvc.perform(get("/api/v1/workouts/" + workoutId).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andReturn();
		objectMapper.readTree(workout.getResponse().getContentAsString());

		mockMvc.perform(get("/api/v1/workouts").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/exercise/51ababe0-e7cc-40d3-a3ef-7d6fb418fbac/history")
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].setNumber").exists())
				.andExpect(jsonPath("$[0].reps").exists());

		String directName = "Direct " + java.util.UUID.randomUUID();
		mockMvc.perform(post("/api/v1/workouts")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "startedAt":"2026-07-27T13:00:00Z",
								  "name":"%s",
								  "sets":[
								    {"exerciseId":"51ababe0-e7cc-40d3-a3ef-7d6fb418fbac","setNumber":1,"reps":3,"weightKg":5.0},
								    {"exerciseId":"51ababe0-e7cc-40d3-a3ef-7d6fb418fbac","setNumber":2,"reps":3,"weightKg":5.0}
								  ]
								}
								""".formatted(directName)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.useMetric").value(true));

		mockMvc.perform(post("/api/v1/workouts")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "startedAt":"2026-07-27T15:00:00Z",
								  "name":"%s",
								  "sets":[
								    {"exerciseId":"51ababe0-e7cc-40d3-a3ef-7d6fb418fbac","setNumber":1,"reps":1}
								  ]
								}
								""".formatted(directName)))
				.andExpect(status().isConflict());

		MvcResult createdW = mockMvc.perform(get("/api/v1/workouts")
						.header("Authorization", "Bearer " + adminToken)
						.param("from", "2026-07-27T13:00:00Z")
						.param("to", "2026-07-27T14:00:00Z"))
				.andExpect(status().isOk())
				.andReturn();
		String directId = objectMapper.readTree(createdW.getResponse().getContentAsString()).get(0).get("id").asText();
		MvcResult directDetail = mockMvc.perform(get("/api/v1/workouts/" + directId).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode dSets = objectMapper.readTree(directDetail.getResponse().getContentAsString()).get("sets");
		String d0 = dSets.get(0).get("id").asText();
		String d1 = dSets.get(1).get("id").asText();

		mockMvc.perform(patch("/api/v1/workouts/" + directId + "/sets/reorder")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"items":[{"setId":"%s","setNumber":99},{"setId":"%s","setNumber":1}]}
								""".formatted(d0, d1)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sets[0].setNumber").value(1));

		mockMvc.perform(patch("/api/v1/workouts/" + directId + "/sets/" + d0)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"completed\":false}"))
				.andExpect(status().isOk());
		JsonNode afterUnset = objectMapper.readTree(mockMvc.perform(get("/api/v1/workouts/" + directId)
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString());
		boolean unsetFound = false;
		for (JsonNode set : afterUnset.get("sets")) {
			if (d0.equals(set.get("id").asText())) {
				org.junit.jupiter.api.Assertions.assertFalse(set.get("completed").asBoolean());
				unsetFound = true;
			}
		}
		org.junit.jupiter.api.Assertions.assertTrue(unsetFound);

		mockMvc.perform(patch("/api/v1/workouts/" + directId + "/sets/" + d0)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"completed\":true,\"rpe\":\"EASY\",\"reps\":5}"))
				.andExpect(status().isOk());
		JsonNode afterSet = objectMapper.readTree(mockMvc.perform(get("/api/v1/workouts/" + directId)
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString());
		boolean setFound = false;
		String exerciseIdForNotes = null;
		for (JsonNode set : afterSet.get("sets")) {
			if (d0.equals(set.get("id").asText())) {
				org.junit.jupiter.api.Assertions.assertTrue(set.get("completed").asBoolean());
				org.junit.jupiter.api.Assertions.assertEquals("EASY", set.get("rpe").asText());
				org.junit.jupiter.api.Assertions.assertEquals(5, set.get("reps").asInt());
				org.junit.jupiter.api.Assertions.assertTrue(set.has("trackedParameters"));
				org.junit.jupiter.api.Assertions.assertTrue(set.has("exerciseNotes"));
				exerciseIdForNotes = set.get("exerciseId").asText();
				setFound = true;
			}
		}
		org.junit.jupiter.api.Assertions.assertTrue(setFound);
		org.junit.jupiter.api.Assertions.assertNotNull(exerciseIdForNotes);

		mockMvc.perform(put("/api/v1/exercise/" + exerciseIdForNotes + "/notes")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"notes\":\"Felt strong\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.notes").value("Felt strong"));
		mockMvc.perform(get("/api/v1/exercise/" + exerciseIdForNotes + "/notes")
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.notes").value("Felt strong"));
		JsonNode afterNotes = objectMapper.readTree(mockMvc.perform(get("/api/v1/workouts/" + directId)
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString());
		boolean notesFound = false;
		for (JsonNode set : afterNotes.get("sets")) {
			if (exerciseIdForNotes.equals(set.get("exerciseId").asText())) {
				org.junit.jupiter.api.Assertions.assertEquals("Felt strong", set.get("exerciseNotes").asText());
				notesFound = true;
			}
		}
		org.junit.jupiter.api.Assertions.assertTrue(notesFound);

		MvcResult clearedNotes = mockMvc.perform(put("/api/v1/exercise/" + exerciseIdForNotes + "/notes")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"notes\":null}"))
				.andExpect(status().isOk())
				.andReturn();
		org.junit.jupiter.api.Assertions.assertTrue(
				objectMapper.readTree(clearedNotes.getResponse().getContentAsString()).get("notes").isNull());
		JsonNode afterClearNotes = objectMapper.readTree(mockMvc.perform(get("/api/v1/workouts/" + directId)
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString());
		boolean notesCleared = false;
		for (JsonNode set : afterClearNotes.get("sets")) {
			if (d0.equals(set.get("id").asText())) {
				org.junit.jupiter.api.Assertions.assertTrue(set.get("exerciseNotes").isNull());
				notesCleared = true;
			}
		}
		org.junit.jupiter.api.Assertions.assertTrue(notesCleared);

		mockMvc.perform(put("/api/v1/workouts/" + directId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"startedAt":"2026-07-27T13:00:00Z","name":"Direct2 %s","useMetric":false,"sets":[{"exerciseId":"51ababe0-e7cc-40d3-a3ef-7d6fb418fbac","setNumber":1,"reps":1}]}
								""".formatted(java.util.UUID.randomUUID())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.useMetric").value(false));

		mockMvc.perform(delete("/api/v1/workouts/" + directId).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNoContent());
		mockMvc.perform(delete("/api/v1/workouts/" + workoutId).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNoContent());
		mockMvc.perform(delete("/api/v1/templates/" + templateId).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNoContent());
		mockMvc.perform(delete("/api/v1/exercise/" + customId).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNoContent());

		// authz smoke: other cannot get admin-deleted resources; catalog update forbidden
		mockMvc.perform(put("/api/v1/exercise/51ababe0-e7cc-40d3-a3ef-7d6fb418fbac")
						.header("Authorization", "Bearer " + otherToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Nope","level":"BEGINNER","instructions":"x","trackedParameters":1}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void authorizationEdgesForPrivateTemplatesAndWorkouts() throws Exception {
		MvcResult custom = mockMvc.perform(post("/api/v1/exercise")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Secret Move","level":"BEGINNER","instructions":"x","trackedParameters":1}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String customId = objectMapper.readTree(custom.getResponse().getContentAsString()).get("id").asText();

		MvcResult publicT = mockMvc.perform(post("/api/v1/templates")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name":"Public With Custom",
								  "visibility":"PUBLIC",
								  "sets":[{"exerciseId":"%s","setNumber":1,"reps":1}]
								}
								""".formatted(customId)))
				.andExpect(status().isCreated())
				.andReturn();
		String publicId = objectMapper.readTree(publicT.getResponse().getContentAsString()).get("id").asText();

		MvcResult privateT = mockMvc.perform(post("/api/v1/templates")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name":"Private Only",
								  "visibility":"PRIVATE",
								  "sets":[{"exerciseId":"51ababe0-e7cc-40d3-a3ef-7d6fb418fbac","setNumber":1,"reps":1}]
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String privateId = objectMapper.readTree(privateT.getResponse().getContentAsString()).get("id").asText();

		mockMvc.perform(get("/api/v1/templates/" + privateId).header("Authorization", "Bearer " + otherToken))
				.andExpect(status().isNotFound());

		MvcResult workout = mockMvc.perform(post("/api/v1/workouts")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"startedAt":"2026-07-28T10:00:00Z","name":"Admin W %s","sets":[]}
								""".formatted(java.util.UUID.randomUUID())))
				.andExpect(status().isCreated())
				.andReturn();
		String workoutId = objectMapper.readTree(workout.getResponse().getContentAsString()).get("id").asText();
		mockMvc.perform(get("/api/v1/workouts/" + workoutId).header("Authorization", "Bearer " + otherToken))
				.andExpect(status().isForbidden());

		mockMvc.perform(delete("/api/v1/workouts/" + workoutId).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNoContent());
		mockMvc.perform(delete("/api/v1/templates/" + privateId).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNoContent());
		mockMvc.perform(delete("/api/v1/templates/" + publicId).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNoContent());
		mockMvc.perform(delete("/api/v1/exercise/" + customId).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNoContent());
	}
}
