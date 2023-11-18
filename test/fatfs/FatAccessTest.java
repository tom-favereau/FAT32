package fatfs;

import drives.Device;
import drives.RamDevice;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class FatAccessTest {

    private  FatAccess fatAccess;
    @Before
    public void setUp() throws Exception {
        Device device = new Device();
        device.mount("/Users/tom/Programation/dep_info/software_engineering/fat32-favereau-suspene-shao/data/mesTests/SSD_0_CreateFiles_2.data");
        fatAccess = new FatAccess(device);
    }

    @Test
    public void corectInformation(){
        assertEquals(fatAccess.getSizeSector(), 512);
        assertEquals(fatAccess.getBeginFatSector(), 32);
        assertEquals(fatAccess.getSizeCluster(), 2);
        assertEquals(fatAccess.getTotalNumberSector(), 1024);
        assertEquals(fatAccess.getFatNumberOfCase(), 384); //je comprend pas bien 1024 secteur - 32 secteur resevé - 2*3 secteur pour les fat le tout /2 ça fait 493 cluster pourquoi il en a 100 de moins
        assertEquals(fatAccess.getNumberOfFat(), 2);
        assertEquals(fatAccess.getSectorPerFat(), 3);
        assertEquals(fatAccess.getRootIndex(), 2);
    }

    @Test
    public void read() {
        assertEquals(fatAccess.read(fatAccess.getRootIndex()), 0x0FFFFFFF);
    }

    @Test
    public void write() {
        int previous = fatAccess.read(fatAccess.getRootIndex()+1);
        fatAccess.write(0x00000000, fatAccess.getRootIndex()+1);
        assertTrue(fatAccess.isEmpty(fatAccess.getRootIndex()+1));
        fatAccess.write(0x0FFFFFFF, fatAccess.getRootIndex()+1);
        assertFalse(fatAccess.isEmpty(fatAccess.getRootIndex()+1));
        assertEquals(fatAccess.read(fatAccess.getRootIndex()+1), previous);
    }

    @Test
    public void remove() {
        int previous = fatAccess.read(fatAccess.getRootIndex()+1);
        fatAccess.remove(fatAccess.getRootIndex()+1);
        assertTrue(fatAccess.isEmpty(fatAccess.getRootIndex()+1));
        fatAccess.write(previous, fatAccess.getRootIndex()+1);
    }

    @Test
    public void isEmpty() {
        assertFalse(fatAccess.isEmpty(fatAccess.getRootIndex()));
    }
}