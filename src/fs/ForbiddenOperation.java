package fs;

/** Exception raised when trying to perform a forbidden operation.
 */
public class ForbiddenOperation extends Exception {
    public ForbiddenOperation() {
        super("Operation forbidden on this object instance");
    }
}
