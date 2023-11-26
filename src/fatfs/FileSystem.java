package fatfs;


import fs.*;

import java.io.FileNotFoundException;
import java.util.NoSuchElementException;
import java.util.Vector;

/**
 * Implementation of IFileSystem.
 */
public class FileSystem implements IFileSystem {

    private DataAccess dataAccess;
    private IDevice device;
    private FatAccess fatAccess;
    private Path currentDirectory;

    /**
     * Initialization of a FileSystem instance.
     * No mounted device.
     */
    public FileSystem(){
        this.device = null;
        this.dataAccess = null;
        this.fatAccess = null;
        this.currentDirectory = null;
    }

    /**
     * Formats the device
     *
     * @param dev device to be formatted.
     * @param size_of_cluster size of a cluster in sectors in the device.
     */
    @Override
    public void format(IDevice dev, int size_of_cluster) {
        //Resetting the device.
        for (int i = 0; i<dev.getNumberOfSectors(); i++){
            byte[] sector = new byte[dev.getSectorSize()];
            dev.write(sector, i);
        }
        //Computing the FAT allocation.
        long numberOfCluster = (dev.getNumberOfSectors() - 32)*dev.getSectorSize()/(2*4 + (long) dev.getSectorSize()*size_of_cluster);
        long sizeFat = numberOfCluster*4;
        long numberOfSectorFat = sizeFat/dev.getSectorSize();

        //Writing information in the first cluster.
        byte[] sector = dev.read(0);
        writeBytes(sector, 0x00B, 2, dev.getSectorSize());
        writeBytes(sector, 0x00D, 1, size_of_cluster);
        writeBytes(sector, 0x00E, 2, 32);
        writeBytes(sector, 0x010, 1, 2);
        writeBytes(sector, 0x020, 4, dev.getNumberOfSectors());
        writeBytes(sector, 0x024, 4, numberOfSectorFat);
        writeBytes(sector, 0x02c, 4, 2);
        dev.write(sector, 0);

        //TODO Compact this.
        //Root
        // dir .
        FatAccess newFatAccess = new FatAccess(dev);
        DataAccess newDataAccess = new DataAccess(dev, newFatAccess);
        byte[] newRootSector = dev.read(newDataAccess.getBeginDataSector() + newFatAccess.getSizeCluster()*newFatAccess.getRootIndex());
        //Name
        String nameToByteDir = String.format("%-" + 8 + "s", ".");
        System.arraycopy(nameToByteDir.getBytes(), 0, newRootSector, 0, 8);
        //Extension
        String extensionToByteDir = String.format("%-" + 3 + "s", "");
        System.arraycopy(extensionToByteDir.getBytes(), 0, newRootSector, 0+8, 3);
        //Attributes
        boolean[] attribut = new boolean[8];
        attribut[0] = true; attribut[2] = true; attribut[4] = true;
        byte rootAttribut = 0;
        for (int k = 0; k < 8; k++){
            if (attribut[k]){
                rootAttribut |= (1 << k);
            }
        }
        newRootSector[0+11] = rootAttribut;

        writeBytes(newRootSector, 28+0, 4, 0);
        writeBytes(newRootSector, 20+0, 4, newFatAccess.getRootIndex());

        //dir ..
        //Name
        String nameToByteDirParent = String.format("%-" + 8 + "s", ".."); //on complète avec des espaces
        System.arraycopy(nameToByteDirParent.getBytes(), 0, newRootSector, 32, 8);
        //Extension
        String extensionToByteDirParent = String.format("%-" + 3 + "s", ""); // on complète avec des espcaes
        System.arraycopy(extensionToByteDirParent.getBytes(), 0, newRootSector, 32+8, 3);

        //Attributes
        newRootSector[32+11] = rootAttribut;

        writeBytes(newRootSector, 28+32, 4, 0);
        writeBytes(newRootSector, 20+32, 4, newFatAccess.getRootIndex());

        //First two sectors of the FAT are marked as taken.
        newFatAccess.write(0x0FFFFFF0, 0);
        newFatAccess.write(0x0FFFFFFF, 1);
        //Writing Root in the FAT.
        newFatAccess.write(0x0FFFFFFF, 2);

        //Writing in the device.
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
     * Get the amount of free space.
     *
     * @return the amount of free space in the currently mounted device, 0 if no device is mounted.
     */
    @Override
    public int totalFreeSpace() {
        return fatAccess.totalFreeSpace();
    }

    /**
     * Set the working directory in the mounted device.
     *
     * @param path path to new working directory.
     */
    @Override
    public void setWorkingDirectory(String path) throws FileNotFoundException {
        try {
            //Attempting to create a path from given string path.
            Path directory = new Path(dataAccess, getAbsolutePath(currentDirectory.toString(), path), null);
            //Checking if the path is a directory.
            if (directory.getFile().getAttribut()[4]){
                this.currentDirectory = directory;
            } else {
                throw new FileNotFoundException("Is not directory.");
            }
        } catch (Exception e){
            //Case where the given path is invalid.
            throw new FileNotFoundException("Invalid path.");
        }
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
        try {
            //Attempting to create a path from filename.
            Path file = new Path(dataAccess, getAbsolutePath(currentDirectory.toString(), filename), null);
            return new FileStream(file.getFile(), mode, dataAccess);
        } catch (NoSuchElementException noSuchElementException){
            //If the file doesn't exist:
            if (mode == 'w' || mode == 'a'){
                try {
                    //Creating the file.
                    String[] components = removeLastElement(filename);
                    //Path to the directory containing the new file.
                    Path path = new Path(dataAccess, getAbsolutePath(currentDirectory.toString(), components[0]), null);
                    DataFile directory = path.getFile();
                    //Name of created file.
                    String[] nameAndExtension = splitFileNameAndExtension(components[1]);
                    //Attributes
                    boolean[] attribut = new boolean[8];
                    //Adding the file to the data sectors.
                    dataAccess.addFile(directory, nameAndExtension[0], nameAndExtension[1], attribut);
                    return openFile(filename, mode);
                }
                catch (Exception e){
                    return null;
                }
            } else{
                //A file that does not exist can't be read.
                return null;
            }
        }
    }

    /**
     * Retrieves the name and the extension from a file name of type "name.extension".
     * @param fileName file name to be split.
     * @return a string array of length 2. The first element is the name and the second, the extension.
     * The dot is removed.
     */
    private static String[] splitFileNameAndExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        String[] result = new String[2];

        if (dotIndex != -1) {
            result[0] = fileName.substring(0, dotIndex);
            result[1] = fileName.substring(dotIndex + 1);
        } else {
            result[0] = fileName;
            result[1] = ""; //No extension.
        }
        return result;
    }

    /**
     * Retrieves the contents of a directory, given as a filename.
     * @param filename String file name to a directory.
     * @return a DataFile vector of the contents of the directory,
     * a DataFile vector containing the DataFile of the filename if it is a file,
     * or an empty vector if the file name is invalid.
     */
    public Vector<DataFile> listSubFile(String filename){
        //Converting the filename to a Path instance, a read the sub files of the associated file.
        try {
            Path file = new Path(dataAccess, getAbsolutePath(currentDirectory.toString(), filename), null);
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
            Path file = new Path(dataAccess, getAbsolutePath(currentDirectory.toString(), filename), null);
            //Checking if to-be-removed file is an empty directory.
            if (file.getFile().getAttribut()[4]){
                remove = dataAccess.isEmpty(file.getFile());
            }
            if (remove) {
                remove = dataAccess.removeFile(file.getFile());
            }
            return remove;
        } catch (Exception e){
            //Case where the filename is invalid.
            return false;
        }
    }

    /**
     * Retrieves the directory and the name from a file name representing a new file.
     * @param filename a filename representing a new file to be added.
     * @return a String array of length two.
     * The first element is the string path to the parent directory of the new file.
     * The second is the full name of the added file (name, dot and extension).
     */
    private String[] removeLastElement(String filename){
        //Splitting the filename to path components.
        String[] string_elements = filename.split("/");
        //Case where only the file's full name has been given.
        if (string_elements.length == 1){
            return new String[]{currentDirectory.toString(), filename};
        }
        //Reconstructing the string representation of the directory.
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
        //Splitting the name of the directory from its parent path.
        String[] components = removeLastElement(directory_name);
        try {
            //Attempting to create a path from the parent path.
            Path path = new Path(dataAccess, getAbsolutePath(currentDirectory.toString(), components[0]), null);
            DataFile directory = path.getFile();
            //TODO faire ça en moins moche
            //If parent file is a directory and doesn't contain a directory with the same name, the directory is created.
            if (path.getFile().getAttribut()[4] && !path.findInDirectoryBooleanString(directory, components[1])){
                boolean[] attribut = new boolean[8];
                attribut[4] = true;
                dataAccess.addFile(directory, components[1], "", attribut);
                return true;
            } else {
                return false;
            }
        } catch (Exception e){
            //Case where the directory name is invalid.
            return false;
        }
    }

    /**
     * From an absolute path and a relative path, gives the associated absolute path.
     * Supports . and .. in the given string representations.
     * @param currentPath an absolute path.
     * @param relativePath a relative path to the absolute path.
     * @return the resulting absolute path as a string.
     */
    private String getAbsolutePath(String currentPath, String relativePath) {
        //Checking if given relative path is already absolute.
        if (relativePath.startsWith("/")) {
            java.nio.file.Path chemin = java.nio.file.Paths.get(relativePath).normalize();

            return chemin.toString().replace("\\", "/");
        }

        //Using java.nio.file.Paths to resolve the new path
        java.nio.file.Path cheminActuelPath = java.nio.file.Paths.get(currentPath);
        java.nio.file.Path cheminRelatifPath = java.nio.file.Paths.get(relativePath);
        java.nio.file.Path cheminAbsoluPath = cheminActuelPath.resolve(cheminRelatifPath).normalize();

        //Necessary to convert \ to / because the package automatically checks for the system's OS.
        return cheminAbsoluPath.toString().replace("\\", "/");
    }

    /**
     * Writes in a sector starting a given index, data of a given size.
     * @param sector sector in which data is written.
     * @param index starting index in the sector.
     * @param size size of written data.
     * @param data int data to be written
     */
    private void writeBytes(byte[] sector, int index, int size, long data){
        for (int i=0; i<size; i++){
            sector[size+index-i-1] = (byte) ((data >> 8*i) & 0xFF);
        }
    }


    //Accessors

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