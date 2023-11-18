package fs;

/** A read-only sector id.
 * */
public class SectorId {
    private int id;

    public SectorId(int id) {
        this.id = id;
    }

    public int get() { return this.id; }
}