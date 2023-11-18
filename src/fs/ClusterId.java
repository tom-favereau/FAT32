package fs;

/** A read-only cluster id.
 */
public class ClusterId {
    private int id;

    public ClusterId(int id) {
        this.id = id;
    }

    public int get() { return this.id; }
}