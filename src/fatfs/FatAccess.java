package fatfs;

import drives.Device;
import fs.IDevice;

import javax.naming.Binding;
import java.util.zip.DeflaterInputStream;

public class FatAccess{
    /**
     * on ne s'interesse qu'a la Fat. on la lit et on ecrit dedans, entre autres.*/

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
 * les attributs correspondent aux informations données par le reserved sector du disque virtuel device
 * */
    public FatAccess(IDevice device){
        byte[] firstSector = device.read(0);
        this.device = device;
        this.beginFatSector = readBytes(firstSector, 0x00E, 2); //l'index de la première fat est stocké en 3 + 2 bytes
        this.sizeSector = readBytes(firstSector, 0x00B, 2);
        this.sizeCluster = readBytes(firstSector, 0x00D, 1);
        this.totalNumberSector = readBytes(firstSector, 0x020, 4);
        this.numberOfFat = readBytes(firstSector, 0x010, 1);
        this.sectorPerFat = readBytes(firstSector, 0x024, 4);
        //this.fatNumberOfCase = (totalNumberSector-beginFatSector-numberOfFat*sectorPerFat)/sizeCluster; version ou le dernier secteur n'est pas complet
        this.fatNumberOfCase = this.sizeSector*sectorPerFat/4;
        this.rootIndex = readBytes(firstSector, 0x02C, 4);
    }


    public int read(int index){
        int byteIndex = 4*index;
        int indexSector = byteIndex/sizeSector;
        int indexInSector = byteIndex%sizeSector;
        byte[] sector = device.read(beginFatSector+indexSector);
        int res = readBytes(sector, indexInSector, 4);
        return res;
    }

    public void write(int data, int index){
        int byteIndex = 4*index;
        int indexSector = byteIndex/sizeSector;
        int indexInSector = byteIndex%sizeSector;
        byte[] sector = device.read(beginFatSector+indexSector);
        writeBytes(sector, indexInSector, 4, data);
        device.write(sector, beginFatSector+indexSector);
    }

    public void remove(int index){
        write(0x00000000, index);
    }

    public boolean isEmpty(int index){
        int byteIndex = 4*index;
        int indexSector = byteIndex/sizeSector;
        int indexInSector = byteIndex%sizeSector;
        byte[] sector = device.read(beginFatSector+indexSector);
        return readBytes(sector, indexInSector, 4) == 0x00000000;
    }

    /**
     *
     * @return le nombre de cluster disponible
     */
    public int totalFreeSpace(){
        int freeSpace = 0;
        for (int i = 2; i<fatNumberOfCase; i++){
            if (read(i) == 0x00000000){
                freeSpace++;
            }
        }
        return freeSpace;
    }

    /**
     *
     * @return the index of first free cluster
     */
    public int firstFreeCluster(){
        for (int i = 0; i<fatNumberOfCase; i++){
            if (read(i) == 0x00000000){
                return i;
            }
        }
        return 0; //ce cas n'est pas sensé arrivé puisqu'on appelle pas cette fonction avant d'avoir verifié l'espcae de stockage
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
    private void writeBytes(byte[] sector, int index, int size, int data){
        for (int i=0; i<size; i++){
            sector[size+index-i-1] = (byte) ((data >> 8*i) & 0xFF);
        }
    }

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