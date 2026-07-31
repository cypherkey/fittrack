package com.fittrack.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards Angular client-side routes to index.html so deep links and refreshes work
 * when the SPA is served from classpath:/static/.
 *
 * <p>Does not map /login/oauth2/** (Spring OAuth) or /api/** / actuator / swagger.
 */
@Controller
public class SpaForwardController {

	@GetMapping({
			"/",
			"/login",
			"/auth",
			"/auth/**",
			"/workouts",
			"/workouts/**",
			"/templates",
			"/templates/**",
			"/exercises",
			"/exercises/**",
			"/settings",
			"/settings/**",
			"/dashboard",
			"/dashboard/**"
	})
	public String forward() {
		return "forward:/index.html";
	}
}