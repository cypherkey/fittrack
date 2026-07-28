package com.fittrack.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

/**
 * Ensures the parent directory for the SQLite file exists before datasource init.
 */
public class SqliteDataDirectoryListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		Environment env = event.getEnvironment();
		String url = env.getProperty("spring.datasource.url", "");
		if (!url.startsWith("jdbc:sqlite:")) {
			return;
		}
		String pathPart = url.substring("jdbc:sqlite:".length());
		int q = pathPart.indexOf('?');
		if (q >= 0) {
			pathPart = pathPart.substring(0, q);
		}
		if (pathPart.isBlank() || ":memory:".equals(pathPart)) {
			return;
		}
		Path dbPath = Path.of(pathPart);
		Path parent = dbPath.getParent();
		if (parent != null) {
			try {
				Files.createDirectories(parent);
			}
			catch (IOException e) {
				throw new IllegalStateException("Unable to create SQLite data directory: " + parent, e);
			}
		}
	}
}
