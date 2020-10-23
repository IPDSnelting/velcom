package de.aaaaaaah.velcom.shared.util;

import java.util.List;

/**
 * A collection of lines including the offset of the first one. This is useful when you are only
 * working with an excerpt but want to preserve absolute line numbers.
 */
public class LinesWithOffset {

	private final int firstLineOffset;
	private final List<String> lines;

	/**
	 * If there are three lines and the last two are included in this object, the firstLineOffset will
	 * be one (totalLength - includedLength).
	 *
	 * @param firstLineOffset the offset of the first line. 0 based.
	 * @param lines the lines
	 */
	public LinesWithOffset(int firstLineOffset, List<String> lines) {
		this.firstLineOffset = firstLineOffset;
		this.lines = lines;
	}

	public int getFirstLineOffset() {
		return firstLineOffset;
	}

	public List<String> getLines() {
		return lines;
	}
}
