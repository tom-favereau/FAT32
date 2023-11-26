package fs;

import fatfs.FatAccess;
import java.util.Vector;

/**
 * Allows reading and writing access to the data sectors.
 */
public class DataAccess{

    private IDevice device;
    private FatAccess fatAccess;
    private int beginDataSector;


    /**
     * Constructor for an instance of DataAccess.
     * @param device a device from which access is needed.
     * @param fatAccess the FatAccess instance associated with the device.
     */
    public DataAccess(IDevice device, FatAccess fatAccess){
        this.device = device;
        this.fatAccess = fatAccess;
        this.beginDataSector = this.fatAccess.getBeginFatSector() + this.fatAccess.getNumberOfFat()*this.fatAccess.getSectorPerFat();
    }

    /**
     * From a sector, assumes a file name is at a given index and returns the byte array associated with it.
     * @param sector read sector.
     * @param index an index at which a file name is assumed to be.
     * @return the sub-array containing the file name.
     */
    private byte[] getByteName(byte[] sector, int index){
        byte[] byteName = new byte[8];
        System.arraycopy(sector, index, byteName, 0, 8);
        return byteName;
    }


    /**
     * Returns the files contained in a directory.
     * @param directory a DataFile which represent a directory.
     * @return a DataFile vector of the contents of the directory.
     */
    public Vector<DataFile> readSubFile(DataFile directory){
        // Checking if given DataFile is a directory.
        if (!directory.getAttribut()[4]){
            Vector<DataFile> res = new Vector<DataFile>();
            res.add(directory);
            return res;
        }
        else {
            int index = directory.getFirstClusterIndex();
            int nextIndex = index;
            Vector<DataFile> res = new Vector<DataFile>();
            boolean lastFile = false; //Flags if last file of the directory is reached.
            do { //Iteration over the cluster-index-linked list in the FAT.
                index = nextIndex;
                for (int j = 0; j < fatAccess.getSizeCluster(); j++){ //Iteration over the sectors.
                    byte[] sector = device.read(beginDataSector + j + index*fatAccess.getSizeCluster());
                    for (int i = 0; i < fatAccess.getSizeSector(); i += 32){ //Iteration over the records.

                        //Getting the name of recorded file.
                        byte[] byteName = getByteName(sector, i);
                        if (byteName[0] == 0x00) { //Checking if last file is reached.
                            lastFile = true;
                            break;
                        }
                        if (byteName[0] == (byte) 0xE5){
                            continue;
                        }
                        String name = new String(byteName);

                        //Extension
                        String extension = new String(new byte[]{sector[i + 8], sector[i + 9], sector[i + 10]});
                        boolean[] attribute = new boolean[8];
                        for (int k = 7; k >= 0; k--){
                            attribute[k] = (sector[11 + i] & (1 << k)) != 0;
                        }

                        //Cluster index
                        int firstClusterIndex = readBytes(sector, i + 20, 4);

                        //Size
                        int size = readBytes(sector, i + 28, 4);

                        DataFile newFile = new DataFile(name, extension, attribute, firstClusterIndex, size, directory, i, j, index);
                        res.add(newFile);

                    } if (lastFile){ //The loop ends when the last file is found.
                        break;
                    }
                } if (lastFile){
                    break;
                }
                nextIndex = fatAccess.read(index);
            } while (nextIndex != 0x0FFFFFFF);
            return res;
        }
    }

    /**
     * Checks whether a directory is empty.
     * @param directory a DataFile directory.
     * @return true if it's empty, false if not.
     */
    public boolean isEmpty(DataFile directory){
        return readSubFile(directory).size() == 2;
    }


    /**
     * Reads the content of root.
     * Necessary because there is no way to access a file without reading the root of the tree structure.
     * @return a DataFile vector of the contents of root.
     */
    public Vector<DataFile> readSubFileRoot(){
        boolean[] rootAttribut = new boolean[8];
        rootAttribut[0] = false; rootAttribut[1] = false; rootAttribut[2] = true; rootAttribut[4] = true;
        DataFile rootFile = new DataFile("root", "", rootAttribut, fatAccess.getRootIndex(), 0, null, 0, 0, 0);
        return readSubFile(rootFile);
    }

    /**
     * Returns root directory.
     * @return root as a DataFile.
     */
    public DataFile rootFile(){
        boolean[] rootAttribut = new boolean[8];
        rootAttribut[0] = false; rootAttribut[1] = false; rootAttribut[2] = true; rootAttribut[4] = true;
        return new DataFile("root    ", "   ", rootAttribut, fatAccess.getRootIndex(), 0, null, 0, 0, 0);
    }

    /**
     * Reads the content of a file.
     * @param file a file as a DataFile.
     * @return the content of the file as a String.
     */
    public String readFileContent(DataFile file){
        // File size is required, it's given by DataFile attributes.

        //Checking if file is readable.
        if (file.getAttribut()[4]){
            return file.getName() + "/ is a directory";
        }
        else {
            int index = file.getFirstClusterIndex();
            int size = file.getSize();
            byte[] res = new byte[size];
            int nextIndex = index;
            int sizeAct = 0; //Used to check whether the file is read further than the allocated size.
            do { //Iteration over the cluster-index-linked list in the FAT.
                index = nextIndex;
                for (int j = 0; j < fatAccess.getSizeCluster(); j++) { //Iteration over the sectors.
                    byte[] sector = device.read(beginDataSector + j + index*fatAccess.getSizeCluster());
                    for (int i = 0; i < fatAccess.getSizeSector(); i++) { // Iteration within a sector.
                        if (sizeAct == size) {
                            break;
                        } else {
                            res[sizeAct] = sector[i];
                            sizeAct++;
                        }
                    }
                }
                nextIndex = fatAccess.read(index);
            } while (nextIndex != 0x0FFFFFFF); //Marker for the last link.
            return new String(res);
        }
    }

    /**
     * Reads the content of a file.
     * @param file a file as a DataFile.
     * @return the content of the file as a byte array.
     */
    public byte[] readFileByte(DataFile file) throws ForbiddenOperation {
        //Same process as in readFileContent, without the last conversion in String.
        if (file.getAttribut()[4]){
            throw new ForbiddenOperation();
        }
        else {
            int index = file.getFirstClusterIndex();
            int size = file.getSize();
            byte[] res = new byte[size];
            int nextIndex = index;
            int sizeAct = 0;
            do {
                index = nextIndex;
                for (int j = 0; j < fatAccess.getSizeCluster(); j++) {
                    byte[] sector = device.read(beginDataSector + j + index*fatAccess.getSizeCluster());
                    for (int i = 0; i < fatAccess.getSizeSector(); i++) {
                        if (sizeAct == size) {
                            break;
                        } else {
                            res[sizeAct] = sector[i];
                            sizeAct++;
                        }
                    }
                }
                nextIndex = fatAccess.read(index);
            } while (nextIndex != 0x0FFFFFFF);
            return res;
        }
    }

    /**
     * Returns a byte attribute from a boolean attribute array.
     * @param attribute a boolean attribute array of a file.
     * @return the byte corresponding to the boolean attribute array.
     */
    private byte attributeToByte(boolean[] attribute){
        byte newFileAttribut = 0;
        for (int k = 0; k < 8; k++){
            if (attribute[k]){
                //Shifting 1 by k and adding it to the current byte.
                newFileAttribut |= (byte) (1 << k);
            }
            //newFileAttribute |= (attribute[k] ? 1 : 0) << (7-k) is also possible.
        }
        return newFileAttribut;
    }

    /**
     * Writes a record at a given index in a given sector. A record is 32 bits.
     * @param sector target sector.
     * @param start index at which the record is written.
     * @param name name of the recorded file.
     * @param extension extension of the recorded file.
     * @param attribute attribute byte of the recorded file.
     * @param firstClusterIndex index of the first cluster of the recorded file.
     * @param size size in bytes of the recorded file.
     */
    public void writeFileInSector(byte[] sector, int start, String name, String extension, byte attribute, int firstClusterIndex, int size){
        //Name
        String nameToByteDir = String.format("%-" + 8 + "s", name); //completed by spaces.
        System.arraycopy(nameToByteDir.getBytes(), 0, sector, start, 8);
        //Extension
        String extensionToByteDir = String.format("%-" + 3 + "s", extension); //completed by spaces.
        System.arraycopy(extensionToByteDir.getBytes(), 0, sector, start + 8, 3);
        //Attribute
        sector[start + 11] = attribute;
        //Cluster index
        writeBytes(sector, start + 20, 4, firstClusterIndex);
        //Size
        writeBytes(sector, start + 28, 4, size);
    }

    /**
     * Initializes the cluster of a new file, by writing . and .. .
     * @param directory directory in which the file was created.
     * @param extension String extension of created file.
     * @param attribute boolean attribute array of created file
     * @param newFileAttribute byte attribute of created file.
     */
    private void initializeCluster(DataFile directory, String extension, boolean[] attribute, byte newFileAttribute, int firstClusterIndex) {
        byte[] newFileSector = new byte[fatAccess.getSizeSector()];
        for (int k = 0; k < fatAccess.getSizeCluster(); k++) {
            if (k == 0 && attribute[4]) {
                // dir .
                writeFileInSector(newFileSector, 0, ".", extension, newFileAttribute, firstClusterIndex, 0);

                //dir ..
                byte newFileAttributeDirParent = attributeToByte(directory.getAttribut());
                writeFileInSector(newFileSector, 32, "..", directory.getExtention(), newFileAttributeDirParent,directory.getFirstClusterIndex(), 0);
            }
            device.write(newFileSector, beginDataSector + k + firstClusterIndex * fatAccess.getSizeCluster());
        }
    }



    /**
     * Adds an empty file to a given directory.
     * @param directory DataFile directory where the file is created.
     * @param name String name of created file.
     * @param extension String extension of created file.
     * @param attribute attribute of created file as a boolean array.
     * @return the new DataFile and null if there is not enough space in the disk.
     */
    public DataFile addFile(DataFile directory, String name, String extension, boolean[] attribute){
        if (fatAccess.totalFreeSpace() == 0){
            return null; //Not enough space on the disk.
        }
        else {
            //Let's look for a spot within the directory in which the new file's information will be written.
            int index = directory.getFirstClusterIndex();
            int nextIndex = index;
            do { //Iteration over the cluster-index-linked list in the FAT.
                index = nextIndex;
                for (int j = 0; j < fatAccess.getSizeCluster(); j++){ //Iteration over the sectors.
                    byte[] sector = device.read(beginDataSector + j + index*fatAccess.getSizeCluster());
                    for (int i = 0; i < fatAccess.getSizeSector(); i += 32){ //Iteration over the records.
                        byte[] byteName = new byte[8];
                        System.arraycopy(sector, i, byteName, 0, 8);
                        if (byteName[0] == 0x00 || byteName[0] == (byte) 0xE5) { // Checking if it's the last file or a deleted file.
                            //Writing information in the directory.
                            byte newFileAttribut = attributeToByte(attribute);
                            int firstClusterIndex = fatAccess.firstFreeCluster();
                            writeFileInSector(sector, i, name, extension, newFileAttribut, firstClusterIndex, 0);

                            //Sector is marked as taken in the FAT.
                            fatAccess.write(0x0FFFFFFF, firstClusterIndex);

                            //Initializing cluster.
                            initializeCluster(directory, extension, attribute, newFileAttribut, firstClusterIndex);

                            device.write(sector, beginDataSector + j + index*fatAccess.getSizeCluster());
                            return new DataFile(name, extension, attribute, firstClusterIndex, 0, directory, i, j, index);
                        }
                    }
                }
                nextIndex = fatAccess.read(index);
            } while (nextIndex != 0x0FFFFFFF);
            //When the directory is full:
            if (fatAccess.totalFreeSpace() <= 1){
                return null; //Not enough space for the new cluster and the file.
            } else{
                // Adding a cluster to the directory.
                int newLastIndex = addCluster(directory);
                byte[] sector = device.read(beginDataSector + newLastIndex*fatAccess.getSizeCluster());

                //Writing information in the new sector.
                byte newFileAttribut = attributeToByte(attribute);
                int firstClusterIndex = fatAccess.firstFreeCluster();
                writeFileInSector(sector, 0, name, extension, newFileAttribut, firstClusterIndex, 0);

                //Sector is marked as taken in the FAT.
                fatAccess.write(0x0FFFFFFF, firstClusterIndex);

                //Initializing cluster.
                initializeCluster(directory, extension, attribute, newFileAttribut, firstClusterIndex);

                device.write(sector, beginDataSector + newLastIndex*fatAccess.getSizeCluster());
                return new DataFile(name, extension, attribute, firstClusterIndex, 0, directory, 0, 0, newLastIndex);
            }
        }
    }


/**
 * Adds an empty file in root.
 * @param name String name of created file.
 * @param extension String extension of created file.
 * @param attribute file attribute as a boolean array.
 * @return the new DataFile and null if there is not enough space in the disk.
 */
    public DataFile addFileRoot(String name, String extension, boolean[] attribute){
        return addFile(rootFile(), name, extension, attribute);
    }

    /**
     * Removes a file.
     * @param file removed file as a DataFile.
     * @return true if the file was successfully removed, false if not.
     */
    public boolean removeFile(DataFile file){
        //TODO importance moyenne : écrire 0x00 si il s'agit du dernier fichier
        //System files removal are handled in DataAccess.
        if (file.getAttribut()[2]){
            return false;
        }

        //Deletion from the FAT.
        int index = file.getFirstClusterIndex();
        int nextIndex = index;
        do { //Iteration over the cluster-index-linked list in the FAT.
            index = nextIndex;
            nextIndex = fatAccess.read(index);
            fatAccess.write(0x00000000, index);
        } while (nextIndex != 0x0FFFFFFF);

        int indexInParent = file.getIndexInParent();
        int sectorInParent = file.getSectorInParent();
        int clusterInParent = file.getClusterInParent();
        byte[] sector = device.read(beginDataSector + sectorInParent + clusterInParent * fatAccess.getSizeCluster());
        byte[] byteName = new byte[8];
        byteName[0] = (byte) 0xE5;
        System.arraycopy(byteName, 0, sector, indexInParent, 8);
        device.write(sector, beginDataSector + sectorInParent + clusterInParent * fatAccess.getSizeCluster());
        return true;
        /* cette version été moins efficace
        //Deletion from the records of parent directory.
        DataFile parentFile = file.getParentFile();
        int parentIndex = parentFile.getFirstClusterIndex();
        int nextParentIndex = parentIndex;
        do { //Iteration over the cluster-index-linked list in the FAT.
            parentIndex = nextParentIndex;
            for (int j = 0; j < fatAccess.getSizeCluster(); j++) { //Iteration over the sectors.
                byte[] sector = device.read(beginDataSector + j + parentIndex * fatAccess.getSizeCluster());
                for (int i = 0; i < fatAccess.getSizeSector(); i += 32) { //Iteration over the records.
                    //Name
                    String name = new String(getByteName(sector, i));

                    //Extension
                    String extension = new String(new byte[]{sector[i + 8], sector[i + 9], sector[i + 10]});

                    if (name.equals(file.getName()) && extension.equals(file.getExtention())) {
                        //Found the file, marking it as deleted.
                        byte[] byteName = new byte[8];
                        byteName[0] = (byte) 0xE5;
                        System.arraycopy(byteName, 0, sector, i, 8);
                        device.write(sector, beginDataSector + j + parentIndex * fatAccess.getSizeCluster());
                    }
                }
            }
            nextParentIndex = fatAccess.read(parentIndex);
        } while (nextParentIndex != 0x0FFFFFFF);
        return true;

         */
    }

    /**
     * Writes in a file in append mode.
     * If there is not enough space on the device, the copyable section is copied, but not the rest.
     * @param file DataFile in which data is written.
     * @param data String data to be written in the file.
     * @return true if the data was successfully written, false if not (lack of space).
     */
    public boolean writeAppendFile(DataFile file, String data){
        //TODO on fait quoi si y'a pas assez d'espace sur le disque ? on copie la partie copiable ou on copie rien du tout ?
        if (file.getAttribut()[0]){
            return false;
        }
        int index = file.getFirstClusterIndex();
        byte[] dataByte = data.getBytes();
        int sizeCopy = dataByte.length;
        int sizeFile = file.getSize();
        while (fatAccess.read(index) != 0x0FFFFFFF) { //Iteration over the cluster-index-linked list in the FAT.
            index = fatAccess.read(index);
            sizeFile -= fatAccess.getSizeCluster()*fatAccess.getSizeSector();
        }

        int beginSector = sizeFile/fatAccess.getSizeSector();
        sizeFile = sizeFile%fatAccess.getSizeSector();
        for (int j = beginSector; j < fatAccess.getSizeCluster(); j++){ //Iteration over the sectors.
            if (sizeCopy == 0){
                break;
            }
            byte[] sector = device.read(beginDataSector + j + index*fatAccess.getSizeCluster());
            int beginIndex;
            if (j == beginSector){
                beginIndex = sizeFile;
                sizeFile = 0;
            }
            else {
                beginIndex = 0;
            }
            for (int i = beginIndex; i < fatAccess.getSizeSector(); i++){ // Iteration within a sector.

                if (sizeCopy == 0){
                    break;

                }
                sector[i] = dataByte[dataByte.length - sizeCopy];
                sizeCopy--;
            }
            device.write(sector, beginDataSector + j + index*fatAccess.getSizeCluster());
        }
        //If there is still data to be written:
        while (sizeCopy != 0){
            //Checking if there is enough space to write the rest.
            if (fatAccess.totalFreeSpace() > 0) {
                //Adding a new cluster for the data left to be written.
                int newLastIndex = addCluster(file);
                for (int j = 0; j < fatAccess.getSizeCluster(); j++) { //Iteration over the sectors.
                    byte[] sector = device.read(beginDataSector + j + newLastIndex*fatAccess.getSizeCluster());
                    for (int i = 0; i < fatAccess.getSizeSector(); i++) { // Iteration within a sector.
                        //Stopping when there is nothing left to be written.
                        if (sizeCopy == 0) {
                            break;
                        } else {
                            //Continuing writing.
                            sector[i] = dataByte[dataByte.length - sizeCopy];
                            sizeCopy--;
                        }
                    }
                    device.write(sector, beginDataSector + j + newLastIndex * fatAccess.getSizeCluster());
                }
            } else {
                //Case where there is not enough space on the disk.
                //Updating the file's size nonetheless.
                DataFile fileAct = new DataFile(file.getName(), file.getExtention(), file.getAttribut(), file.getFirstClusterIndex(), file.getSize() + data.length() - sizeCopy, file.getParentFile(), file.getIndexInParent(), file.getSectorInParent(), file.getClusterInParent());
                actualiseFile(fileAct);
                return false;
            }
        }
        //Updating the file's size.
        DataFile fileAct = new DataFile(file.getName(), file.getExtention(), file.getAttribut(), file.getFirstClusterIndex(), file.getSize()+dataByte.length - sizeCopy, file.getParentFile(), file.getIndexInParent(), file.getSectorInParent(), file.getClusterInParent());
        actualiseFile(fileAct);
        return true;
    }

    /**
     * Writes in a file in append mode.
     * If there is not enough space on the device, the copyable section is copied, but not the rest.
     * @param file DataFile in which data is written.
     * @param dataByte byte array of data to be written in the file.
     * @return number bytes effectively written.
     */
    public int writeAppendFileByte(DataFile file, byte[] dataByte){
        //Same process as in writeAppendFile.
        if (file.getAttribut()[0]){
            return 0;
        }
        int index = file.getFirstClusterIndex();
        int sizeCopy = dataByte.length;
        int sizeFile = file.getSize();

        while (fatAccess.read(index) != 0x0FFFFFFF) { //Iteration over the cluster-index-linked list in the FAT.
            index = fatAccess.read(index);
            sizeFile -= fatAccess.getSizeCluster()*fatAccess.getSizeSector();
        }
        int beginSector = sizeFile/fatAccess.getSizeSector();
        sizeFile = sizeFile%fatAccess.getSizeSector();
        for (int j = beginSector; j < fatAccess.getSizeCluster(); j++){
            if (sizeCopy == 0){
                break;
            }
            byte[] sector = device.read(beginDataSector + j + index*fatAccess.getSizeCluster());
            int beginIndex;
            if (j == beginSector){
                beginIndex = sizeFile;
                sizeFile = 0;
            }
            else {
                beginIndex = 0;
            }
            for (int i = beginIndex; i<fatAccess.getSizeSector(); i++){
                if (sizeCopy == 0){
                    break;
                }
                sector[i] = dataByte[dataByte.length - sizeCopy];
                sizeCopy--;


            }
            device.write(sector, beginDataSector + j + index*fatAccess.getSizeCluster());
        }
        while (sizeCopy != 0){
            if (fatAccess.totalFreeSpace()>0) {
                int newLastIndex = addCluster(file);
                for (int j = 0; j < fatAccess.getSizeCluster(); j++) {
                    byte[] sector = device.read(beginDataSector + j + newLastIndex * fatAccess.getSizeCluster());
                    for (int i = 0; i < fatAccess.getSizeSector(); i++) {
                        if (sizeCopy == 0) {
                            break;
                        } else {
                            sector[i] = dataByte[dataByte.length - sizeCopy];
                            sizeCopy--;
                        }
                    }
                    device.write(sector, beginDataSector + j + newLastIndex*fatAccess.getSizeCluster());
                }
            } else {
                DataFile fileAct = new DataFile(file.getName(), file.getExtention(), file.getAttribut(), file.getFirstClusterIndex(), file.getSize()+dataByte.length-sizeCopy, file.getParentFile(), file.getIndexInParent(), file.getSectorInParent(), file.getClusterInParent());
                actualiseFile(fileAct);
                return dataByte.length - sizeCopy;
            }
        }
        DataFile fileAct = new DataFile(file.getName(), file.getExtention(), file.getAttribut(), file.getFirstClusterIndex(), file.getSize()+dataByte.length-sizeCopy, file.getParentFile(), file.getIndexInParent(), file.getSectorInParent(), file.getClusterInParent());
        actualiseFile(fileAct);
        return dataByte.length - sizeCopy;
    }

    /**
     * Writes in a file in write mode.
     * @param file DataFile in which data is written.
     * @param data String data to be written in the file.
     * @return true if the data was successfully written, false if not (lack of space).
     */
    public boolean writeFile(DataFile file, String data){
        //Deleting file and writing in append mode in a new file.
        removeFile(file);
        DataFile newFile = addFile(file.getParentFile(), file.getName(), file.getExtention(), file.getAttribut());
        return writeAppendFile(newFile, data);
    }

    /**
     * Writes in a file in write mode.
     * @param file DataFile in which data is written.
     * @param dataByte byte array of data to be written in the file.
     * @return number of bytes effectively written.
     */
    public int writeFileByte(DataFile file, byte[] dataByte){
        //Same process as in writeFile.
        removeFile(file);
        DataFile newFile = addFile(file.getParentFile(), file.getName(), file.getExtention(), file.getAttribut());
        return writeAppendFileByte(newFile, dataByte);
    }


    /**
     * Adds a new cluster to a file when its clusters are full.
     * @param file DataFile which clusters are full.
     * @return index of the new cluster.
     */
    private int addCluster(DataFile file){
        if (fatAccess.totalFreeSpace() == 0){
            return 0; //Not enough space on the disk.
        }
        else {
            //Iteration over the cluster-index-linked list in the FAT,
            //in order to find the last cluster.
            int firstFreeCluster = fatAccess.firstFreeCluster();
            int index = file.getFirstClusterIndex();
            int nextIndex = index;
            do {
                index = nextIndex;
                nextIndex = fatAccess.read(index);
            } while(nextIndex != 0x0FFFFFFF);
            //The former last cluster points to the new cluster.
            fatAccess.write(firstFreeCluster, index);
            //The new cluster is marked as taken.
            fatAccess.write(0x0FFFFFFF, firstFreeCluster);

            //Resetting the new cluster's sectors.
            byte[] newFileSector = new byte[fatAccess.getSizeSector()];
            device.write(newFileSector, beginDataSector + firstFreeCluster*fatAccess.getSizeCluster());
            device.write(newFileSector, beginDataSector + 1 + firstFreeCluster*fatAccess.getSizeCluster());

            return firstFreeCluster;
        }
    }

    /**
     * Converts a sub array of a sector into integer.
     * @param sector the read sector.
     * @param index starting index at which the sector is read.
     * @param size reading size in the sector.
     * @return the decimal value represented by the chosen sub array.
     */
    private int readBytes(byte[] sector, int index, int size){
        int res = 0;
        for (int i = 0; i < size; i++) {
            res |= (sector[i + index] & 0xFF) << ((size - 1 - i) * 8);
        }
        return res;
    }

    /**
     * Writes in a sector starting a given index, data of a given size.
     * @param sector sector in which data is written.
     * @param index starting index in the sector.
     * @param size size of written data.
     * @param data int data to be written
     */
    private void writeBytes(byte[] sector, int index, int size, int data){
        for (int i = 0; i < size; i++){
            sector[size + index - i - 1] = (byte) ((data >> 8*i) & 0xFF);
        }
    }

    /**
     * Updates a file's size.
     * @param file DataFile to be updated.
     */
    private void actualiseFile(DataFile file){
        DataFile directory = file.getParentFile();
        int index = directory.getFirstClusterIndex();
        int nextIndex = index;
        boolean findFile = false;
        int indexInParent = file.getIndexInParent();
        int sectorInParent = file.getSectorInParent();
        int clusterInParent = file.getClusterInParent();
        byte[] sector = device.read(beginDataSector + sectorInParent + clusterInParent*fatAccess.getSizeCluster());
        writeBytes(sector, indexInParent + 28, 4, file.getSize());
        device.write(sector, beginDataSector + sectorInParent + clusterInParent*fatAccess.getSizeCluster());
        /*
        do { //Iteration over the cluster-index-linked list in the FAT.
            index = nextIndex;
            if (findFile){
                break;
            }
            for (int j = 0; j < fatAccess.getSizeCluster(); j++){ //Iteration over the sectors.
                if (findFile){
                    break;
                }
                byte[] sector = device.read(beginDataSector + j + index*fatAccess.getSizeCluster());
                for (int i = 0; i < fatAccess.getSizeSector(); i += 32){ //Iteration over the records.

                    //Name
                    String name = new String(getByteName(sector, i));

                    //Extension
                    String extension = new String(new byte[]{sector[i + 8], sector[i + 9], sector[i + 10]});

                    //If file is found, the size is updated.
                    if (name.equals(file.getName()) && extension.equals(file.getExtention())) {
                        writeBytes(sector, i + 28, 4, file.getSize());
                        findFile = true;
                        break;
                    }
                }
                device.write(sector, beginDataSector + j + index*fatAccess.getSizeCluster());
            }
            nextIndex = fatAccess.read(index);
        } while (nextIndex != 0x0FFFFFFF);

         */
    }

    public int getBeginDataSector() {
        return beginDataSector;
    }
}