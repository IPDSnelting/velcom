package de.aaaaaaah.velcom.runner.shared.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * An {@link OutputStream} that writes to a String.
 */
public class StringOutputStream extends OutputStream {

	private ByteArrayOutputStream byteArrayOutputStream;

	public StringOutputStream() {
		this.byteArrayOutputStream = new ByteArrayOutputStream();
	}

	@Override
	public void write(int b) {
		byteArrayOutputStream.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		byteArrayOutputStream.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) {
		byteArrayOutputStream.write(b, off, len);
	}

	/**
	 * Returns the underlying read string.
	 *
	 * @return the underlying string
	 */
	public String getString() {
		return new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
	}
}
