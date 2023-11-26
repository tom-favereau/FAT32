package fatfs;

import drives.Device;
import fs.IDevice;


/**
 * Allows reading and writing in a device's FAT.
 */
public class FatAccess{

    private IDevice device;
    private int beginFatSector;
    private int sizeSector;
    private int sizeCluster;
    private int totalNumberSector;
    private int fatNumberOfCase; // la fat prend 4 octet par cluster donc on fait numberCluster*4octer)//sizeSector +1
    private int numberOfFat;
    private int sectorPerFat;
    private int rootIndex;

    /**
     * Constructor for a FatAccess instance.
     * Attributes are given by the reserved sectors of the device.
    * @param device Device from which the FAT is read.
     * */
    public FatAccess(IDevice device){
        byte[] firstSector = device.read(0);
        this.device = device;
        //The index of the first FAT is written in 3 + 2 bytes.
        this.beginFatSector = readBytes(firstSector, 0x00E, 2);
        this.sizeSector = readBytes(firstSector, 0x00B, 2);
        this.sizeCluster = readBytes(firstSector, 0x00D, 1);
        this.totalNumberSector = readBytes(firstSector, 0x020, 4);
        this.numberOfFat = readBytes(firstSector, 0x010, 1);
        this.sectorPerFat = readBytes(firstSector, 0x024, 4);
        //In a FAT, a cluster is four bytes.
        this.fatNumberOfCase = this.sizeSector*sectorPerFat/4;
        this.rootIndex = readBytes(firstSector, 0x02C, 4);
    }


    /**
     * Reads the index contained in the FAT at a given FAT index.
     * @param index at which the index is read.
     * @return the index contained in the FAT at the given index.
     */
    public int read(int index){
        int byteIndex = 4*index;
        int indexSector = byteIndex/sizeSector;
        int indexInSector = byteIndex%sizeSector;
        byte[] sector = device.read(beginFatSector + indexSector);
        int res = readBytes(sector, indexInSector, 4);
        return res;
    }

    /**
     * Writes data in the FAT, at a given index.
     * @param data int data to be written, in practice, an index.
     * @param index index at which data is written.
     */
    public void write(int data, int index){
        int byteIndex = 4*index;
        int indexSector = byteIndex/sizeSector;
        int indexInSector = byteIndex%sizeSector;
        byte[] sector = device.read(beginFatSector + indexSector);
        writeBytes(sector, indexInSector, 4, data);
        device.write(sector, beginFatSector + indexSector);
    }

    /**
     * Removes information written in the FAT, at a given index.
     * @param index index at which data is erased from the FAT.
     */
    public void remove(int index){
        write(0x00000000, index);
    }

    /**
     * Checks if the FAT is empty at a given index.
     * @param index at which emptiness is tested.
     * @return true if the FAT is empty at this given index, false if not.
     */
    public boolean isEmpty(int index){
        int byteIndex = 4*index;
        int indexSector = byteIndex/sizeSector;
        int indexInSector = byteIndex%sizeSector;
        byte[] sector = device.read(beginFatSector + indexSector);
        return readBytes(sector, indexInSector, 4) == 0x00000000;
    }

    /**
     * Returns the number of free clusters.
     * A free cluster is represented by 0x00000000.
     * @return the number of free clusters.
     */
    public int totalFreeSpace(){
        int freeSpace = 0;
        for (int i = 2; i < fatNumberOfCase; i++){
            if (read(i) == 0x00000000){
                freeSpace++;
            }
        }
        return freeSpace;
    }

    /**
     * Returns the index of the first free cluster.
     * @return the index of first free cluster
     */
    public int firstFreeCluster(){

        for (int i = 2; i<fatNumberOfCase; i++){
            if (read(i) == 0x00000000){
                return i;
            }
        }
        //It should not happen, because firstFreeCluster is always called after checking
        //if the device is not full.
        return 0;
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

    //Accessors

    public int getBeginFatSector() {
        return beginFatSector;
    }

    public int getSizeSector() {
        return sizeSector;
    }

    public int getSizeCluster() {
        return sizeCluster;
    }

    public int getTotalNumberSector() {
        return totalNumberSector;
    }

    public int getFatNumberOfCase() {
        return fatNumberOfCase;
    }

    public int getNumberOfFat() {
        return numberOfFat;
    }

    public int getSectorPerFat() {
        return sectorPerFat;
    }

    public int getRootIndex() {
        return rootIndex;
    }
}