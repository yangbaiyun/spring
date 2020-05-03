package org.springframework.web.multipart;

import javax.servlet.ServletException;

/**
 * Exception thrown on multipart resolution
 * @author Trevor D. Cook
 * @since 29-Sep-2003
 * @see MultipartResolver#resolveMultipart
 */
public class MultipartException extends ServletException {

	public MultipartException() {
		super();
	}

	public MultipartException(String msg) {
		super(msg);
	}

	public MultipartException(String msg, Throwable t) {
		super(msg, t);
	}

	public MultipartException(Throwable t) {
		super(t);
	}

}
