package de.aaaaaaah.velcom.backend.access;

import static org.jooq.codegen.db.tables.Repository.REPOSITORY;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.aaaaaaah.velcom.backend.access.entities.AuthToken;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.nio.file.Path;
import java.sql.SQLException;
import org.jooq.DSLContext;
import org.jooq.codegen.db.tables.records.RepositoryRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TokenAccessTest {

	private static final int HASH_MEMORY = 5120;
	private static final int HASH_ITERATIONS = 50;
	private static final AuthToken ADMIN_TOKEN = new AuthToken("12345");
	private static final AuthToken FIRST_TOKEN = new AuthToken("first");
	private static final AuthToken SECOND_TOKEN = new AuthToken("second");

	@TempDir
	Path testDir;
	DatabaseStorage dbStorage;

	TokenWriteAccess tokenAccess;
	RepoId firstId = new RepoId(), secondId = new RepoId();

	@BeforeEach
	void setUp() throws SQLException {
		dbStorage = new DatabaseStorage("jdbc:sqlite:file:" + testDir.resolve("data.db"));
		tokenAccess = new TokenWriteAccess(dbStorage, ADMIN_TOKEN, HASH_MEMORY, HASH_ITERATIONS);

		// Insert repo into db
		try (DSLContext db = dbStorage.acquireContext()) {
			RepositoryRecord record = db.newRecord(REPOSITORY);
			record.setId(firstId.getId().toString());
			record.setName("bla");
			record.setRemoteUrl("bla");
			record.insert();

			record.setId(secondId.getId().toString());
			record.insert();
		}
	}

	@AfterEach
	void breakDown() {
		if (dbStorage != null) {
			dbStorage.close();
		}
	}

	@Test
	public void testHasToken() {
		assertFalse(tokenAccess.hasToken(firstId));
		assertFalse(tokenAccess.hasToken(secondId));
		tokenAccess.setToken(firstId, FIRST_TOKEN);
		assertTrue(tokenAccess.hasToken(firstId));
		assertFalse(tokenAccess.hasToken(secondId));
		assertThrows(NullPointerException.class, () -> tokenAccess.hasToken(null));
	}

	@ParameterizedTest
	@ValueSource(strings = {"1", "1234", "123456", "12345 ", " 12345", "SYH9dsH|6'cP", "", " "})
	public void testIsValidAdminToken(String tokenStr) {
		assertTrue(tokenAccess.isValidAdminToken(ADMIN_TOKEN));
		assertFalse(tokenAccess.isValidAdminToken(new AuthToken(tokenStr)));
	}

	@ParameterizedTest
	@ValueSource(strings = {"first ", " first", "123456", "SYH9dsH|6'cP", "", " "})
	public void testIsValidToken(String tokenStr) {
		assertFalse(tokenAccess.isValidToken(firstId, FIRST_TOKEN));
		assertFalse(tokenAccess.isValidToken(firstId, new AuthToken(tokenStr)));
		tokenAccess.setToken(firstId, FIRST_TOKEN);
		assertTrue(tokenAccess.isValidToken(firstId, FIRST_TOKEN));
		assertFalse(tokenAccess.isValidToken(firstId, new AuthToken(tokenStr)));
	}

	@Test
	public void testIsValidTokenWithNull() {
		assertThrows(NullPointerException.class,
			() -> tokenAccess.isValidToken(null, null));
		assertThrows(NullPointerException.class,
			() -> tokenAccess.isValidToken(firstId, null));
		assertThrows(NullPointerException.class,
			() -> tokenAccess.isValidToken(null, FIRST_TOKEN));

		assertThrows(NullPointerException.class,
			() -> tokenAccess.isValidAdminToken(null));
	}

	@Test
	public void testSetToken() {
		assertThrows(NullPointerException.class, () -> tokenAccess.setToken(null, FIRST_TOKEN));
		assertFalse(tokenAccess.hasToken(firstId));
		tokenAccess.setToken(firstId, FIRST_TOKEN);
		assertTrue(tokenAccess.hasToken(firstId));
		assertTrue(tokenAccess.isValidToken(firstId, FIRST_TOKEN));
		tokenAccess.setToken(firstId, null); // removes the token
		assertFalse(tokenAccess.hasToken(firstId));
		assertFalse(tokenAccess.isValidToken(firstId, FIRST_TOKEN));

		tokenAccess.setToken(secondId, SECOND_TOKEN);
		assertFalse(tokenAccess.hasToken(firstId));
		assertFalse(tokenAccess.isValidToken(firstId, FIRST_TOKEN));
		assertTrue(tokenAccess.isValidToken(secondId, SECOND_TOKEN));

		tokenAccess.setToken(firstId, FIRST_TOKEN);
		assertTrue(tokenAccess.hasToken(firstId));
		assertTrue(tokenAccess.isValidToken(firstId, FIRST_TOKEN));
		assertTrue(tokenAccess.isValidToken(secondId, SECOND_TOKEN));
		assertFalse(tokenAccess.isValidToken(firstId, SECOND_TOKEN));
		assertFalse(tokenAccess.isValidToken(secondId, FIRST_TOKEN));
	}

}
