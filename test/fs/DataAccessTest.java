package fs;

import drives.Device;
import fatfs.FatAccess;
import org.junit.Before;
import org.junit.Test;

import java.util.Vector;

import static org.junit.Assert.*;

/**
 * les test fonction si on ne modifie pas les disques
 */
public class DataAccessTest {

    private DataAccess dataAccess1;
    private DataAccess dataAccess2;
    private DataAccess dataAccess3;

    @Before
    public void setUp() throws Exception {
        //scénario 1
        Device device1 = new Device();
        device1.mount("data/mesTests/SSD_0_CreateFiles_2.data");
        FatAccess fatAccess1 = new FatAccess(device1);
        this.dataAccess1 = new DataAccess(device1, fatAccess1);
        //scénario 2
        Device device2 = new Device();
        device2.mount("data/mesTests/SSD_1_SmallFiles_2.data");
        FatAccess fatAccess2 = new FatAccess(device2);
        this.dataAccess2 = new DataAccess(device2, fatAccess2);
        //scénario 3
        Device device3 = new Device();
        device3.mount("data/mesTests/SSD_2_CreateFiles_LargeDir_2.data");
        FatAccess fatAccess3 = new FatAccess(device3);
        this.dataAccess3 = new DataAccess(device3, fatAccess3);
    }

    @Test
    public void readSubFileRoot(){
        //scénario 1 : 6 fichier / 1 seul cluster pour root
        //Vector<DataFile> rootSubFile1 = dataAccess1.readSubFileRoot();
        //assertEquals(6, rootSubFile1.size());

        //scénario 3 : 37 fichier / 2 Cluster
        Vector<DataFile> rootSubFile3 = dataAccess3.readSubFileRoot();
        assertEquals(37, rootSubFile3.size());

    }

    @Test
    public void addFileRoot(){
        Vector<DataFile> rootSubFile1 = dataAccess1.readSubFileRoot();
        assertEquals(3, rootSubFile1.size());

        boolean[] newFileAttribut = new boolean[8];
        newFileAttribut[0] = false; newFileAttribut[1] = false; newFileAttribut[2] = true; newFileAttribut[4] = false;
        dataAccess1.addFileRoot("test1", "txt", newFileAttribut);
        rootSubFile1 = dataAccess1.readSubFileRoot();
        assertEquals(4, rootSubFile1.size());

        //on teste aussi remove et on remet le disque en état
        for (int i = 0; i < 4; i++){
            if (rootSubFile1.get(i).getName().equals("test1   ")){
                dataAccess1.removeFile(rootSubFile1.get(i));
            }
        }
        rootSubFile1 = dataAccess1.readSubFileRoot();
        assertEquals(3, rootSubFile1.size());
    }

    @Test
    public void readSubFile() { //je peux pas encore le tester car il n'y a pas de scénario qui crée des dossier mais normalement c'est idem que readSubFileRoot()

    }

    @Test
    public void readFileContent() {
        Vector<DataFile> rootSubFile = dataAccess2.readSubFileRoot();
        String alphabet_txt = dataAccess2.readFileContent(rootSubFile.get(2));
        assertEquals("ABCDEFGH", alphabet_txt);
        String nombre_txt = dataAccess2.readFileContent(rootSubFile.get(3));
        assertEquals("0123456789", nombre_txt);
        String key_layout_txt = dataAccess2.readFileContent(rootSubFile.get(4));
        assertEquals("AZERERTY", key_layout_txt);
    }

    @Test
    public void writeAppendFile() {
        Vector<DataFile> rootSubFile = dataAccess1.readSubFileRoot();
        dataAccess1.writeAppendFile(rootSubFile.get(2), "ceci est un test");
        rootSubFile = dataAccess1.readSubFileRoot();
        String content = dataAccess1.readFileContent(rootSubFile.get(2));
        assertEquals("ceci est un test", content);

        dataAccess1.writeAppendFile(rootSubFile.get(2), " et un autre test");
        rootSubFile = dataAccess1.readSubFileRoot();
        content = dataAccess1.readFileContent(rootSubFile.get(2));
        assertEquals("ceci est un test et un autre test", content);
    }

    @Test
    public void writeFile() {
       Vector<DataFile>  rootSubFile2 = dataAccess2.readSubFileRoot();
       String content = dataAccess2.readFileContent(rootSubFile2.get(2));
       assertEquals("ABCDEFGH", content);
       dataAccess2.writeFile(rootSubFile2.get(2), "ceci est un test");
       rootSubFile2 = dataAccess2.readSubFileRoot();
       content = dataAccess2.readFileContent(rootSubFile2.get(2));
       assertEquals("ceci est un test", content);
    }
}