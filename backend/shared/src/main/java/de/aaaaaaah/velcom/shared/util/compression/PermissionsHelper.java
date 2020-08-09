package de.aaaaaaah.velcom.shared.util.compression;

import java.nio.file.attribute.PosixFilePermission;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * A small helper for reading and manipulating linux file permissions.
 */
public class PermissionsHelper {

	private static final Map<Integer, PosixFilePermission> INDEX_PERMISSION_MAP = Map.ofEntries(
		Map.entry(0, PosixFilePermission.OTHERS_EXECUTE),
		Map.entry(1, PosixFilePermission.OTHERS_WRITE),
		Map.entry(2, PosixFilePermission.OTHERS_READ),

		Map.entry(3, PosixFilePermission.GROUP_EXECUTE),
		Map.entry(4, PosixFilePermission.GROUP_WRITE),
		Map.entry(5, PosixFilePermission.GROUP_READ),

		Map.entry(6, PosixFilePermission.OWNER_EXECUTE),
		Map.entry(7, PosixFilePermission.OWNER_WRITE),
		Map.entry(8, PosixFilePermission.OWNER_READ)
	);

	/**
	 * Converts an file mode int to a set of {@link java.nio.file.attribute.PosixFilePermission}s.
	 *
	 * @param mode the file mode (e.g. 644, 744, etc.)
	 * @return the equivalent posix file permissions
	 */
	public static Set<PosixFilePermission> fromOctal(int mode) {
		Set<PosixFilePermission> permissions = EnumSet.noneOf(PosixFilePermission.class);

		for (int i = 0; i < INDEX_PERMISSION_MAP.size(); i++) {
			if (isBitSet(i, mode)) {
				permissions.add(INDEX_PERMISSION_MAP.get(i));
			}
		}

		return permissions;
	}

	/**
	 * Converts a set of {@link java.nio.file.attribute.PosixFilePermission}s to the octal posix
	 * mode.
	 *
	 * @param permissions the permissions
	 * @return the octal posix mode
	 */
	public static int toOctal(Collection<PosixFilePermission> permissions) {
		int result = 0;

		for (int i = 0; i < INDEX_PERMISSION_MAP.size(); i++) {
			if (permissions.contains(INDEX_PERMISSION_MAP.get(i))) {
				result |= 1 << i;
			}
		}
		return result;
	}

	private static boolean isBitSet(int offset, int number) {
		return ((number >> offset) & 0x1) == 1;
	}
}
