package de.aaaaaaah.velcom.shared.util.systeminfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A small parser for the /proc/cpuinfo file format.
 */
class CoreInfoParser {

	/**
	 * Extracts the core information out of the cpu info file.
	 *
	 * @param input the input lines
	 * @return the resulting core infos
	 */
	public List<CoreInfo> coreInfos(List<String> input) {
		return toCpuHunks(input).stream()
			.map(this::hunkToCoreInfo)
			.collect(Collectors.toList());
	}

	private List<List<String>> toCpuHunks(List<String> input) {
		List<List<String>> hunks = new ArrayList<>();
		List<String> current = new ArrayList<>();

		for (String line : input) {
			if (line.isBlank()) {
				hunks.add(current);
				current = new ArrayList<>();
			} else {
				current.add(line);
			}
		}

		if (!current.isEmpty()) {
			hunks.add(current);
		}

		return hunks;
	}

	private CoreInfo hunkToCoreInfo(List<String> hunk) {
		return new CoreInfo(
			findIntLine(hunk, "processor").orElse(-1),
			findIntLine(hunk, "core id").orElse(-1),
			findIntLine(hunk, "physical id").orElse(-1),
			findIntLine(hunk, "cpu cores").orElse(-1),
			findLine(hunk, "model name").orElse("N/A")
		);
	}

	private Optional<String> findLine(List<String> hunk, String header) {
		return hunk.stream()
			.filter(it -> it.strip().startsWith(header))
			.map(it -> it.replaceAll(".+:", "").strip())
			.findFirst();
	}

	private Optional<Integer> findIntLine(List<String> hunk, String header) {
		return findLine(hunk, header).flatMap(CoreInfoParser::parseInt);
	}

	private static Optional<Integer> parseInt(String input) {
		try {
			return Optional.of(Integer.parseInt(input));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

}
