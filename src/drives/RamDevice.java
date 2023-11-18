package drives;


import fs.IDevice;

import java.io.FileNotFoundException;

/**
 * implémente un disque sous forme d'un byte[] cette classe est utile pour les test
 */
public class RamDevice implements IDevice {

    private final short sectorSize = 16;
    private byte[] storage;
    private int numberSector;

    public RamDevice(int numberSector){
        this.numberSector = numberSector;
        storage = new byte[numberSector*sectorSize];
    }

    //TODO : implémenter la méthode pas (utile tout dessuite)
    @Override
    public void mount(String filename) throws FileNotFoundException {

    }

    //TODO : implémenter la méthode (pas utile tout dessuite)
    @Override
    public void unmount() {

    }

    @Override
    public short getSectorSize() {
        return sectorSize;
    }

    @Override
    public long getNumberOfSectors() {
        return numberSector;
    }

    @Override
    public byte[] read(int sectorId) {
        byte[] res = new byte[sectorSize];
        for (int i=sectorId; i<sectorId+sectorSize; i++){
            res[i] = storage[i];
        }
        return res;
    }

    @Override
    public void write(byte[] buffer, int sectorId) {
        for (int i=sectorId; i<sectorId+sectorSize; i++){
            storage[i] = buffer[i];
        }
    }
}