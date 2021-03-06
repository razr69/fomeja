package de.uni_bremen.agra.fomeja.exceptions;

/**
 * ExecutionErrorException is a runtime exception that is thrown when a process
 *  could not be executed.
 * 
 * @version 1.0.0
 * @author Max Nitze
 */
public class ProverException extends RuntimeException {
	/** serial id */
	private static final long serialVersionUID = 5057237497470329710L;

	/**
	 * Constructor for a new ExecutionErrorException.
	 * 
	 * @param message the message of the exception
	 */
	public ProverException(String message) {
		super(message);
	}
}
