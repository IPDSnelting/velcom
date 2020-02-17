package de.aaaaaaah.velcom.runner.shared.util.compression;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PermissionsHelperTest {

	@ParameterizedTest(name = "Mode {0} maps to \"{1}\"")
	@CsvSource({
		"0100, --x------",
		"0111, --x--x--x",
		"0444, r--r--r--",
		"0222, -w--w--w-",
		"0421, r---w---x",
		"0461, r--rw---x",
		"0467, r--rw-rwx",
	})
	void fewBaseTests(int mode, String permissionString) {
		Set<PosixFilePermission> permissions = PosixFilePermissions.fromString(permissionString);
		assertEquals(
			permissions,
			PermissionsHelper.fromOctal(mode)
		);
		assertEquals(
			mode,
			PermissionsHelper.toOctal(permissions)
		);
	}
}