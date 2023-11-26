package fs;

/**
 * Encapsulates information about files.
 */
public class DataFile{

    private String name; //Name of the file.
    private String extention; //Extension.
    private boolean[] attribut; //A boolean array is clearer and easier to manipulate.
    private int firstClusterIndex; //Index of the first cluster.
    private int size; //Size in bytes.
    private DataFile parentFile; //Parent directory.
    private int indexInParent;
    private int sectorInParent;
    private int clusterInParent;

    /**
     * Constructor for DataFile.
     * @param name String name.
     * @param extention String extension.
     * @param attribute Boolean array of the file's attribute.
     * @param firstClusterIndex int index of the first cluster.
     * @param size int size in bytes.
     * @param parentFile DataFile of parent directory, null for root.
     */
    public DataFile(String name, String extention, boolean[] attribute, int firstClusterIndex, int size, DataFile parentFile, int indexInParent, int sectorInParent, int clusterInParent){
        this.name = name;
        this.extention = extention;
        this.attribut = attribute;
        this.firstClusterIndex = firstClusterIndex;
        this.size = size;
        this.parentFile = parentFile;
        this.indexInParent = indexInParent;
        this.sectorInParent = sectorInParent;
        this.clusterInParent = clusterInParent;
    }

    //Accessors

    public String getName() {
        return name;
    }

    public String getExtention() {
        return extention;
    }

    public boolean[] getAttribut() {
        return attribut;
    }

    public int getFirstClusterIndex() {
        return firstClusterIndex;
    }

    public int getSize() {
        return size;
    }

    public DataFile getParentFile() {
        return parentFile;
    }

    public int getIndexInParent(){
        return indexInParent;
    }

    public int getSectorInParent(){
        return sectorInParent;
    }

    public int getClusterInParent(){
        return clusterInParent;
    }

    //mutators

    public void setSize(int newSize){
        size = newSize;
    }
}