package fs;

/** Exception raised when trying to read past the end of a file
 */
public class EndOfFileException extends Exception { 
	/** Required to prevent warning... */
	private static final long serialVersionUID = 1L;

	public EndOfFileException() {
		super("End of file reached");
	}        
}