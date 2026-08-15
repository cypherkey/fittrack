package com.fittrack.config;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically checkpoints SQLite WAL into the main DB file so the {@code .wal} does not grow unbounded.
 */
@Component
public class SqliteWalCheckpointScheduler {

	private static final Logger log = LoggerFactory.getLogger(SqliteWalCheckpointScheduler.class);

	private final JdbcTemplate jdbc;
	private final boolean enabled;

	public SqliteWalCheckpointScheduler(DataSource dataSource, @Value("${spring.datasource.url:}") String jdbcUrl) {
		this.jdbc = new JdbcTemplate(dataSource);
		this.enabled = jdbcUrl.startsWith("jdbc:sqlite:") && jdbcUrl.toLowerCase().contains("journal_mode=wal");
	}

	@Scheduled(fixedRate = 300_000, initialDelay = 300_000)
	public void checkpointWal() {
		if (!enabled) {
			return;
		}
		try {
			// busy / log / checkpointed — TRUNCATE merges WAL into the DB and resets the WAL file.
			Integer busy = jdbc.query("PRAGMA wal_checkpoint(TRUNCATE)", rs -> {
				if (!rs.next()) {
					return null;
				}
				int busyFlag = rs.getInt(1);
				int logFrames = rs.getInt(2);
				int checkpointed = rs.getInt(3);
				log.debug("SQLite WAL checkpoint: busy={}, log={}, checkpointed={}", busyFlag, logFrames, checkpointed);
				return busyFlag;
			});
			if (busy != null && busy != 0) {
				log.warn("SQLite WAL checkpoint was busy (readers/writers held the WAL open)");
			}
		}
		catch (Exception e) {
			log.warn("SQLite WAL checkpoint failed: {}", e.getMessage());
		}
	}
}