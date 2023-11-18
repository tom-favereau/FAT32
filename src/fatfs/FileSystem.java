package fatfs;


import fs.*;

import java.io.FileNotFoundException;
import java.lang.reflect.Executable;
import java.util.NoSuchElementException;
import java.util.Vector;


import fatfs.Path;

public class FileSystem implements IFileSystem {

    private DataAccess dataAccess;
    private IDevice device;
    private FatAccess fatAccess;
    private Path currentDirectory;

    public FileSystem(){
        this.device = null;
        this.dataAccess = null;
        this.fatAccess = null;
        this.currentDirectory = null;
    }

    /**
     * Format the device
     *
     * @param dev             device to be formatted
     * @param size_of_cluster
     */
    @Override
    public void format(IDevice dev, int size_of_cluster) {
        //on met tout à 0
        for (int i = 0; i<dev.getNumberOfSectors(); i++){
            byte[] sector = new byte[dev.getSectorSize()];
            dev.write(sector, i);
        }
        //on calcule la taille aloué a la fat
        long numberOfCluster = (dev.getNumberOfSectors()-32)*dev.getSectorSize()/(2*4+ (long) dev.getSectorSize() *size_of_cluster);
        long sizeFat = numberOfCluster*4;
        long numberOfSectorFat = sizeFat/dev.getSectorSize();

        //on écrit les information dans le premier secteur
        byte[] sector = dev.read(0);
        writeBytes(sector, 0x00B, 2, dev.getSectorSize());
        writeBytes(sector, 0x00D, 1, size_of_cluster);
        writeBytes(sector, 0x00E, 2, 32);
        writeBytes(sector, 0x010, 1, 2);
        writeBytes(sector, 0x020, 4, dev.getNumberOfSectors());
        writeBytes(sector, 0x024, 4, numberOfSectorFat);
        writeBytes(sector, 0x02c, 4, 2);
        dev.write(sector, 0);

        //on crée root
        //on écrit les fichier .. et .
        // dir .
        FatAccess newFatAccess = new FatAccess(dev);
        DataAccess newDataAccess = new DataAccess(dev, newFatAccess);
        byte[] newRootSector = dev.read(newDataAccess.getBeginDataSector() + newFatAccess.getSizeCluster()*newFatAccess.getRootIndex());
        String nameToByteDir = String.format("%-" + 8 + "s", "."); //on complète avec des espaces
        System.arraycopy(nameToByteDir.getBytes(), 0, newRootSector, 0, 8);
        String extentionToByteDir = String.format("%-" + 3 + "s", ""); // on complète avec des espcaes
        System.arraycopy(extentionToByteDir.getBytes(), 0, newRootSector, 0+8, 3);


        boolean[] attribut = new boolean[8];
        attribut[0] = true;
        attribut[2] = true;
        attribut[4] = true;
        byte rootAttribut = 0;
        for (int k = 0; k<8; k++){
            if (attribut[k]){
                rootAttribut |= (1 << k);
            }
            //newFileAttribut |= (attribut[k] ? 1 : 0) << (7-k);
        }

        newRootSector[0+11] = rootAttribut;
        writeBytes(newRootSector, 28+0, 4, 0);
        writeBytes(newRootSector, 20+0, 4, newFatAccess.getRootIndex());

        //dir ..
        String nameToByteDirParent = String.format("%-" + 8 + "s", ".."); //on complète avec des espaces
        System.arraycopy(nameToByteDirParent.getBytes(), 0, newRootSector, 32, 8);
        String extentionToByteDirParent = String.format("%-" + 3 + "s", ""); // on complète avec des espcaes
        System.arraycopy(extentionToByteDirParent.getBytes(), 0, newRootSector, 32+8, 3);

        newRootSector[32+11] = rootAttribut;
        writeBytes(newRootSector, 28+32, 4, 0);

        writeBytes(newRootSector, 20+32, 4, newFatAccess.getRootIndex());

        //on écrit les deux premier secteur de la fat
        newFatAccess.write(0x0FFFFFF0, 0);
        newFatAccess.write(0x0FFFFFFF, 1);
        //on écrit root dans la fat
        newFatAccess.write(0x0FFFFFFF, 2);

        //on écrit sur le disque
        dev.write(newRootSector,newDataAccess.getBeginDataSector() + newFatAccess.getSizeCluster()*newFatAccess.getRootIndex());
    }





    /**
     * Mount a device (assume it has been properly formatted before)
     *
     * @param dev device to be mounted
     */
    @Override
    public void mount(IDevice dev) {
        this.device = dev;
        this.fatAccess = new FatAccess(this.device);
        this.dataAccess = new DataAccess(this.device, this.fatAccess);
        Vector<DataFile> rootFile = new Vector<>();
        rootFile.add(dataAccess.rootFile());
        this.currentDirectory = new Path(dataAccess, "/", rootFile, true, null);
    }

    /**
     * Unmount a device
     */
    @Override
    public void unmount() {
        this.device = null;
        this.dataAccess = null;
        this.fatAccess = null;
        this.currentDirectory = null;
    }

    /**
     * Get the amount of free space
     *
     * @return the amount of free space in the currently mounted device, 0 if no device is mounted
     */
    @Override
    public int totalFreeSpace() {
        return fatAccess.totalFreeSpace();
    }

    private void setWorkingDirectoryRoot(){

    }
    /**
     * Set the working directory in the mounted device
     *
     * @param path path to new working directory
     */
    @Override
    public void setWorkingDirectory(String path) throws FileNotFoundException {
        try {
            //Path directory = filenameToPath(path);
            Path directory = new Path(dataAccess, obtenirCheminAbsolu(currentDirectory.toString(), path), null);
            if (directory.getFile().getAttribut()[4]){
                this.currentDirectory = directory;
            } else {
                throw new FileNotFoundException("Is not directory.");
            }
        } catch (Exception e){
            throw new FileNotFoundException("Invalid path.");
        }
    }
    /**
     * Returns a Path from a filename
     * @return a path from  the filename or null if invalid path
     */
    private Path filenameToPath(String filename) {
        Path file;
        if (currentDirectory.isAbsolute(filename)) {
            file = new Path(dataAccess, filename, null);
        } else {
            file = new Path(dataAccess, filename, currentDirectory);
        }
        return file;
    }

    /**
     * Open a stream to a file.
     *
     * @param filename name of the file to open
     * @param mode     if 'r' open the file in read-only mode
     *                 else if 'w' open the file in write mode (erase its current content).
     *                 else if 'a' open the file in append mode (start writing at the end of the file)
     *                 <p>
     *                 For both 'r' and 'w', the file descriptor is set at the beginning of the file.
     *                 For 'a', the file descriptor is set at the end of the file.
     * @return return the stream object if open succeed, a null reference otherwise
     */
    @Override
    public IFileStream openFile(String filename, char mode) {
        //TODO changer cette HORREUR
        try {
            //Path file = filenameToPath(filename);
            Path file = new Path(dataAccess, obtenirCheminAbsolu(currentDirectory.toString(), filename), null);
            return new FileStream(file.getFile(), mode, dataAccess);
        } catch (NoSuchElementException noSuchElementException){
            if (mode == 'w' || mode == 'a'){
                try {
                    String[] components = removeLastElement(filename);
                    Path path = new Path(dataAccess, obtenirCheminAbsolu(currentDirectory.toString(), components[0]), null);
                    DataFile directory = path.getFile();
                    String[] nameAndExtension = splitFileNameAndExtension(components[1]);
                    boolean[] attribut = new boolean[8];
                    dataAccess.addFile(directory, nameAndExtension[0], nameAndExtension[1], attribut);
                    return openFile(filename, mode);
                }
                catch (Exception e){
                    return null;
                }
            }
            else{
                return null;
            }
        }
    }

    private static String[] splitFileNameAndExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        String[] result = new String[2];

        if (dotIndex != -1) {
            result[0] = fileName.substring(0, dotIndex);
            result[1] = fileName.substring(dotIndex + 1);
        } else {
            result[0] = fileName;
            result[1] = ""; // Aucune extension trouvée
        }

        return result;
    }

    public Vector<DataFile> listSubFile(String filename){
        try {
            Path file = new Path(dataAccess, obtenirCheminAbsolu(currentDirectory.toString(), filename), null);
            return dataAccess.readSubFile(file.getFile());
        }
        catch (Exception e){
            return new Vector<>();
        }
    }

    /**
     * Delete a file or empty directory
     *
     * @param filename name of the file to delete
     * @return false if trying to delete a non-empty directory or a system file
     */
    @Override
    public boolean removeFile(String filename) {
        try{
            boolean remove = true;
            Path file = new Path(dataAccess, obtenirCheminAbsolu(currentDirectory.toString(), filename), null);
            if (file.getFile().getAttribut()[4]){
                remove = dataAccess.isEmpty(file.getFile());
            }
            if (remove) {
                dataAccess.removeFile(file.getFile());
            }
            return remove;
        } catch (Exception e){
            return false;
        }
    }

    /**
     *
     */
    private String[] removeLastElement(String filename){
        String[] string_elements = filename.split("/");
        if (string_elements.length == 1){
            return new String[]{currentDirectory.toString(), filename};
        }
        String res = "";
        for (int i = 0; i < string_elements.length - 1; i++){
            res += string_elements[i] + '/';
        }
        res = res.substring(0, res.length() - 1);
        return new String[]{res, string_elements[string_elements.length - 1]};
    }

    /**
     * Create a directory
     *
     * @param directory_name name of the directory to create
     * @return false if the directory cannot be created
     */
    @Override
    public boolean makeDirectory(String directory_name) {
        String[] components = removeLastElement(directory_name);
        try {
            //Path path = filenameToPath(components[0]);
            Path path = new Path(dataAccess, obtenirCheminAbsolu(currentDirectory.toString(), components[0]), null);
            DataFile directory = path.getFile();
            //TODO faire ça en moins moche
            if (path.getFile().getAttribut()[4] && !path.findInDirectoryBooleanString(directory, components[1])){
                boolean[] attribut = new boolean[8];
                attribut[4] = true;
                dataAccess.addFile(directory, components[1], "", attribut);
                return true;
            } else {
                return false;
            }
        } catch (Exception e){
            return false;
        }
    }

    private String obtenirCheminAbsolu(String cheminActuel, String cheminRelatif) {
        // Vérifier si le chemin relatif est déjà absolu
        if (cheminRelatif.startsWith("/")) {
            return cheminRelatif;
        }

        // Utiliser Paths pour manipuler les chemins
        java.nio.file.Path cheminActuelPath = java.nio.file.Paths.get(cheminActuel);
        java.nio.file.Path cheminRelatifPath = java.nio.file.Paths.get(cheminRelatif);

        // Résoudre le chemin relatif par rapport au chemin actuel
        java.nio.file.Path cheminAbsoluPath = cheminActuelPath.resolve(cheminRelatifPath).normalize();

        // Convertir le chemin absolu en une chaîne

        return cheminAbsoluPath.toString();
    }



    /**
     * convert into integer a sub array of a sector
     * @param sector the sector we crurently read
     * @param index the index of information
     * @param size the size of the information
     * @return the decimal value
     */
    private int readBytes(byte[] sector, int index, int size){
        int res = 0;
        for (int i = 0; i < size; i++) {
            res |= (sector[i + index] & 0xFF) << ((size - 1 - i) * 8);
        }
        return res;
    }

    /**
     * écrit sur un sector a l'index index et sur une taille size un data entière (adresse d'un élément ou autre)
     * @param sector sector sur lequel on écrit
     * @param index index dans le sector
     * @param size size de la data longeur sur laquel on écrit
     * @param data data à écrire
     */
    private void writeBytes(byte[] sector, int index, int size, long data){
        for (int i=0; i<size; i++){
            sector[size+index-i-1] = (byte) ((data >> 8*i) & 0xFF);
        }
    }

    public DataAccess getDataAccess() {
        return dataAccess;
    }

    public IDevice getDevice() {
        return device;
    }

    public FatAccess getFatAccess() {
        return fatAccess;
    }

    public Path getCurrentDirectory() {
        return currentDirectory;
    }
}