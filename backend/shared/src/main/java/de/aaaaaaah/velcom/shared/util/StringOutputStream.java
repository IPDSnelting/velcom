package de.aaaaaaah.velcom.shared.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * A synchronized {@link OutputStream} that writes to a String.
 */
public class StringOutputStream extends OutputStream {

	private final ByteArrayOutputStream byteArrayOutputStream;

	public StringOutputStream() {
		this.byteArrayOutputStream = new ByteArrayOutputStream();
	}

	@Override
	public synchronized void write(int b) {
		byteArrayOutputStream.write(b);
	}

	@Override
	public synchronized void write(byte[] b) throws IOException {
		byteArrayOutputStream.write(b);
	}

	@Override
	public synchronized void write(byte[] b, int off, int len) {
		byteArrayOutputStream.write(b, off, len);
	}

	/**
	 * Returns the underlying read string.
	 *
	 * @return the underlying string
	 */
	public synchronized String getString() {
		return byteArrayOutputStream.toString(StandardCharsets.UTF_8);
	}
}
