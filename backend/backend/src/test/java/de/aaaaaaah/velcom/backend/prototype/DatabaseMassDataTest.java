package de.aaaaaaah.velcom.backend.prototype;

import static org.jooq.codegen.db.Tables.KNOWN_COMMIT;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class DatabaseMassDataTest {

	private static final int REPO_COUNT = 100;
	private static final int COMMIT_PER_REPO_COUNT = 100000;
	private static final int REPEAT_SELECT_COUNT = 10000;

	HikariDataSource dataSource;
	Random r = new Random();

	@BeforeEach
	public void init() {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:sqlite:file:data/data.db");

		dataSource = new HikariDataSource(config);
	}

	@AfterEach
	public void cleanup() {
		dataSource.close();
	}

	@Test
	@Disabled
	public void generateData() throws IOException {
		Path p = Paths.get("data/commits.dat");
		Files.deleteIfExists(p);

		try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(p))) {
			StringBuilder b = new StringBuilder(128);

			for (long i = 0; i < REPO_COUNT * COMMIT_PER_REPO_COUNT; i++) {
				b.delete(0, b.length());

				b.append(i % REPO_COUNT);
				b.append('|');
				b.append(getHash(i));
				b.append('\n');

				out.write(b.toString().getBytes());

				if (i % 1000L == 0L) {
					out.flush();
				}
			}
		}
	}

	@Test
	@Disabled
	public void testSelect() throws IOException {
		try (DSLContext db = DSL.using(dataSource, SQLDialect.SQLITE)) {
			long start, end;

			List<Long> data = new ArrayList<>();

			for (long i = 0; i < REPEAT_SELECT_COUNT; i++) {
				String repoId = UUID.randomUUID().toString();
				String hash = getHash(r.nextLong());

				start = System.nanoTime();

				db.selectCount().from(KNOWN_COMMIT)
					.where(KNOWN_COMMIT.REPO_ID.eq(repoId).and(KNOWN_COMMIT.HASH.eq(hash)))
					.fetchOne();

				end = System.nanoTime();

				data.add(end - start);
			}

			String fileContent = data.stream()
				.map(String::valueOf)
				.collect(Collectors.joining(","));

			Path p = Paths.get("data/durations.txt");
			Files.deleteIfExists(p);
			Files.createFile(p);

			Files.write(p, fileContent.getBytes(StandardCharsets.UTF_8));
		}
	}

	String getHash(long number) {
		// Generate string with length 40
		int width = 40;
		char fill = '0';

		String toPad = Long.toString(number, 36);
		return new String(new char[width - toPad.length()]).replace('\0', fill) + toPad;
	}

}
