package fs;

/**
 * conserve facilement les information de fichier. la fonction readSubFile de DataAccess renvera en tableau
 * de DataFile
 */
public class DataFile{

    private String name; // name of the file
    private String extention; //attention on a que 3 charactère pour l'extnetion ça veux dire qu'on peux même pas créer de fichier java
    private boolean[] attribut; // j'aurais pu metre un byte aussi mais je trouver ça plus clair
    private int firstClusterIndex; //index of the first cluster
    private int size; //taille en octet
    private DataFile parentFile; //dossier parent

    public DataFile(String name, String extention, boolean[] attribut, int firstClusterIndex, int size, DataFile parentFile){
        this.name = name;
        this.extention = extention;
        this.attribut = attribut;
        this.firstClusterIndex = firstClusterIndex;
        this.size = size;
        this.parentFile = parentFile;
    }

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
}