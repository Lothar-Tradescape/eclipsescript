package org.eclipsescript.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;

public class JavaUtils {

	public static abstract class BaseRunnable implements Runnable {
		public abstract void doRun() throws Exception;

		@Override
		public final void run() {
			try {
				doRun();
			} catch (Exception e) {
				throw asRuntime(e);
			}
		}
	}

	public static class MutableObject<T> {
		public T value;
	}

	private static final String UTF8_CHARSET_NAME = "utf-8"; //$NON-NLS-1$

	public static RuntimeException asRuntime(Throwable e) {
		return (e instanceof RuntimeException) ? ((RuntimeException) e) : new RuntimeException(e);
	}

	public static void close(Closeable c) {
		if (c == null)
			return;
		try {
			c.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean isNotBlank(String s) {
		return s != null && s.length() != 0;
	}

	public static byte[] readAll(InputStream in) throws IOException {
		try {
			byte[] buffer = new byte[512];
			int n;
			ByteArrayOutputStream baous = new ByteArrayOutputStream();
			while ((n = in.read(buffer)) != -1) {
				baous.write(buffer, 0, n);
			}
			return baous.toByteArray();
		} finally {
			close(in);
		}
	}

	public static String readAllToStringAndClose(InputStream in) {
		return readAllToStringAndClose(in, UTF8_CHARSET_NAME);
	}

	public static String readAllToStringAndClose(InputStream in, String charSetName) {
		try {
			String charSetNameToUse = charSetName;
			if (charSetName == null || charSetName.isEmpty())
				charSetNameToUse = UTF8_CHARSET_NAME;
			return new String(readAll(in), charSetNameToUse);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			close(in);
		}
	}

	public static String readAllToStringAndClose(Reader in) {
		StringBuilder result = new StringBuilder();
		try {
			char[] buffer = new char[4096];
			int n;
			while ((n = in.read(buffer)) != -1) {
				result.append(buffer, 0, n);
			}
			return result.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			close(in);
		}
	}

	public static String readURL(URL url) throws Exception {
		URLConnection uc = url.openConnection();
		return readURLConnection(uc);
	}

	public static String readURLConnection(URLConnection uc) throws Exception {
		String contentType = uc.getContentType();
		int charSetNameIndex = contentType.indexOf("charset="); //$NON-NLS-1$
		String charSet;
		if (charSetNameIndex == -1) {
			charSet = UTF8_CHARSET_NAME;
		} else {
			charSet = contentType.substring(charSetNameIndex + 8);
		}
		InputStream in = uc.getInputStream();
		try {
			return readAllToStringAndClose(in, charSet);
		} finally {
			in.close();
		}
	}

}
