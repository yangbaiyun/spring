/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */

package org.springframework.core;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Handy class for wrapping runtime Exceptions with a root cause. This time-honoured
 * technique is no longer necessary in Java 1.4, which provides built-in support for
 * exception nesting. Thus exceptions in applications written to use Java 1.4 need not
 * extend this class.
 *
 * <p>Abstract to force the programmer to extend the class.
 * printStackTrace() etc. are forwarded to the wrapped Exception.
 * The present assumption is that all application-specific exceptions that could be
 * displayed to humans (users, administrators etc.) will implement the ErrorCoded interface.
 *
 * <p>The similarity between this class and the NestedCheckedException class is unavoidable,
 * as Java forces these two classes to have different superclasses (ah, the inflexibility
 * of concrete inheritance!).
 *
 * <p>As discussed in <a href="http://www.amazon.com/exec/obidos/tg/detail/-/0764543857/">Expert One-On-One J2EE Design and Development</a>,
 * runtime exceptions are often a better alternative to checked exceptions. However, all exceptions
 * should preserve their stack trace, if caused by a lower-level exception.
 *
 * @author Rod Johnson
 * @version $Id: NestedRuntimeException.java,v 1.4 2003/11/13 11:51:26 jhoeller Exp $
 */
public abstract class NestedRuntimeException extends RuntimeException implements HasRootCause {

	/** Root cause of this nested exception */
	private Throwable rootCause;

	/**
	 * Construct a <code>ExceptionWrapperException</code> with the specified detail message.
	 * @param msg the detail message
	 */
	public NestedRuntimeException(String msg) {
		super(msg);
	}

	/**
	 * Construct a <code>RemoteException</code> with the specified detail message and
	 * nested exception.
	 * @param msg the detail message
	 * @param ex the nested exception
	 */
	public NestedRuntimeException(String msg, Throwable ex) {
		super(msg);
		rootCause = ex;
	}

	/**
	 * Return the nested cause, or null if none.
	 */
	public Throwable getRootCause() {
		return rootCause;
	}

	/**
	 * Return the detail message, including the message from the nested exception
	 * if there is one.
	 */
	public String getMessage() {
		if (this.rootCause == null)
			return super.getMessage();
		else
			return super.getMessage() + "; nested exception is: \n\t" + rootCause.toString();
	}

	/**
	 * Print the composite message and the embedded stack trace to the specified stream.
	 * @param ps the print stream
	 */
	public void printStackTrace(PrintStream ps) {
		if (this.rootCause == null) {
			super.printStackTrace(ps);
		}
		else {
			ps.println(this);
			this.rootCause.printStackTrace(ps);
		}
	}

	/**
	 * Print the composite message and the embedded stack trace to the specified writer.
	 * @param pw the print writer
	 */
	public void printStackTrace(PrintWriter pw) {
		if (this.rootCause == null) {
			super.printStackTrace(pw);
		}
		else {
			pw.println(this);
			this.rootCause.printStackTrace(pw);
		}
	}

}
