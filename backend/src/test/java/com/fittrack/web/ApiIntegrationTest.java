package com.fittrack.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class ApiIntegrationTest {

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("DB_PATH", () -> "file:./target/api-test-fittrack.db");
		registry.add("spring.datasource.url", () -> "jdbc:sqlite:./target/api-test-fittrack.db?foreign_keys=true");
		registry.add("fittrack.jwt.secret", () -> "fittrack-test-secret-change-me-must-be-at-least-256-bits!!");
		registry.add("fittrack.seed.load-images", () -> "false");
		registry.add("fittrack.seed.download-images", () -> "false");
	}

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private ObjectMapper objectMapper;

	private MockMvc mockMvc;
	private String token;

	@BeforeEach
	void setUp() throws Exception {
		mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();
		MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"admin\",\"password\":\"admin\"}"))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode body = objectMapper.readTree(login.getResponse().getContentAsString());
		token = body.get("token").asText();
	}

	@Test
	void listsSeededCatalogExercisesAndSupportsCustomCrud() throws Exception {
		mockMvc.perform(get("/api/v1/exercises").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)))
				.andExpect(jsonPath("$.content[?(@.id=='51ababe0-e7cc-40d3-a3ef-7d6fb418fbac')].custom").value(org.hamcrest.Matchers.contains(false)));

		mockMvc.perform(get("/api/v1/equipment").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.name=='other')]").exists());

		MvcResult created = mockMvc.perform(post("/api/v1/exercises")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "My Custom Curl",
								  "level": "BEGINNER",
								  "mechanic": "ISOLATION",
								  "instructions": "Curl slowly",
								  "category": "strength",
								  "trackedParameters": 3
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.custom").value(true))
				.andReturn();
		String customId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

		mockMvc.perform(put("/api/v1/exercises/" + customId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "My Custom Curl Updated",
								  "level": "INTERMEDIATE",
								  "instructions": "Curl slowly",
								  "trackedParameters": 3
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("My Custom Curl Updated"));

		mockMvc.perform(delete("/api/v1/exercises/" + customId).header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());
	}

	@Test
	void templatesAndWorkoutsRoundTripWithClone() throws Exception {
		MvcResult templateResult = mockMvc.perform(post("/api/v1/templates")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Push Day",
								  "visibility": "PRIVATE",
								  "difficulty": "MEDIUM",
								  "sets": [
								    {"exerciseId": "51ababe0-e7cc-40d3-a3ef-7d6fb418fbac", "setNumber": 10, "reps": 12, "weightKg": 20.0},
								    {"exerciseId": "51ababe0-e7cc-40d3-a3ef-7d6fb418fbac", "setNumber": 20, "reps": 10, "weightKg": 25.0}
								  ]
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.sets.length()").value(2))
				.andExpect(jsonPath("$.setCount").value(2))
				.andReturn();
		String templateId = objectMapper.readTree(templateResult.getResponse().getContentAsString()).get("id").asText();

		MvcResult templateListResult = mockMvc.perform(get("/api/v1/templates").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode listedTemplates = objectMapper.readTree(templateListResult.getResponse().getContentAsString());
		boolean foundTemplate = false;
		for (JsonNode node : listedTemplates) {
			if (templateId.equals(node.get("id").asText())) {
				assertEquals(2, node.get("setCount").asInt());
				assertEquals(0, node.get("sets").size());
				foundTemplate = true;
			}
		}
		assertTrue(foundTemplate);

		MvcResult cloneResult = mockMvc.perform(post("/api/v1/templates/" + templateId + "/clone")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Push Day Live\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Push Day Live"))
				.andExpect(jsonPath("$.startedAt").isEmpty())
				.andExpect(jsonPath("$.endedAt").isEmpty())
				.andExpect(jsonPath("$.completed").value(false))
				.andExpect(jsonPath("$.sets.length()").value(2))
				.andExpect(jsonPath("$.setCount").value(2))
				.andExpect(jsonPath("$.sets[0].setNumber").value(10))
				.andExpect(jsonPath("$.sets[0].completed").value(false))
				.andExpect(jsonPath("$.sourceTemplateId").value(templateId))
				.andReturn();
		String workoutId = objectMapper.readTree(cloneResult.getResponse().getContentAsString()).get("id").asText();

		MvcResult listResult = mockMvc.perform(get("/api/v1/workouts").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode listed = objectMapper.readTree(listResult.getResponse().getContentAsString());
		boolean foundInList = false;
		for (JsonNode node : listed) {
			if (workoutId.equals(node.get("id").asText())) {
				assertEquals(2, node.get("setCount").asInt());
				assertEquals(0, node.get("sets").size());
				foundInList = true;
			}
		}
		assertTrue(foundInList);

		mockMvc.perform(get("/api/v1/workouts/" + workoutId).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sets[1].setNumber").value(20))
				.andExpect(jsonPath("$.setCount").value(2));

		mockMvc.perform(post("/api/v1/workouts/" + workoutId + "/start")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.startedAt").isNotEmpty())
				.andExpect(jsonPath("$.endedAt").isEmpty())
				.andExpect(jsonPath("$.completed").value(false));

		MvcResult beforeComplete = mockMvc.perform(get("/api/v1/workouts/" + workoutId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode beforeNode = objectMapper.readTree(beforeComplete.getResponse().getContentAsString());
		double expectedTotal = 0;
		for (JsonNode set : beforeNode.get("sets")) {
			if (!set.get("reps").isNull() && !set.get("weightKg").isNull()) {
				expectedTotal += set.get("reps").asInt() * set.get("weightKg").asDouble();
			}
		}

		mockMvc.perform(post("/api/v1/workouts/" + workoutId + "/complete")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.endedAt").isNotEmpty())
				.andExpect(jsonPath("$.completed").value(true))
				.andExpect(jsonPath("$.totalWeightLifted").value(expectedTotal));

		mockMvc.perform(put("/api/v1/workouts/" + workoutId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "startedAt": "2026-07-27T18:00:00Z",
								  "name": "Push Day Live",
								  "sets": [
								    {"exerciseId": "51ababe0-e7cc-40d3-a3ef-7d6fb418fbac", "setNumber": 5, "reps": 8, "weightKg": 30.0, "completed": true}
								  ]
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sets.length()").value(1))
				.andExpect(jsonPath("$.setCount").value(1))
				.andExpect(jsonPath("$.sets[0].setNumber").value(5))
				.andExpect(jsonPath("$.totalWeightLifted").value(240.0));
	}
}
