package fatfs;

import drives.Device;
import fs.*;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.Vector;

import static org.junit.Assert.*;

public class FileSystemTest {

    private Device deviceToFormat;
    private Device device;
    private FileSystem fileSystem;
    @Before
    public void setUp() throws Exception {
        this.deviceToFormat = new Device();
        this.device = new Device();
        deviceToFormat.mount("data/mesTests/SSD_0_CreateFiles_2.data");
        device.mount("data/testAlicia/myDevice.data");
        this.fileSystem = new FileSystem();
        this.fileSystem.mount(device);
    }

    @Test
    public void format() {
        fileSystem.format(deviceToFormat, 2);
        DataAccess dataAccess = new DataAccess(deviceToFormat, new FatAccess(deviceToFormat));
        Vector<DataFile> rootSubFile = dataAccess.readSubFileRoot();
        assertEquals(rootSubFile.size(), 2);
        assertEquals(rootSubFile.get(0).getName(), ".       ");
        assertEquals(rootSubFile.get(1).getName(), "..      ");
    }

    @Test
    public void mount() {
    }

    @Test
    public void unmount() {
    }

    @Test
    public void totalFreeSpace() {
    }

    @Test
    public void setWorkingDirectory() throws FileNotFoundException {
        fileSystem.setWorkingDirectory("/dirbal/dir2");
        assertEquals("/dirbal/dir2",fileSystem.getCurrentDirectory().toString());
        fileSystem.setWorkingDirectory("../..");
        //TODO gérer .. et /
        Path root = fileSystem.getCurrentDirectory();
        assertEquals("../..", root.toString());
        assertTrue(root.isRootPath());
    }

    @Test
    public void openFile() throws ForbiddenOperation, EndOfFileException {
        FileStream file = (FileStream) fileSystem.openFile("file0.h", 'w');
        file.write("test".getBytes());
        file = (FileStream) fileSystem.openFile("file0.h", 'r');
        byte[] output = new byte[4];
        file.read(output);
        String outputString = new String(output);
        assertEquals(outputString, "test");

        file = (FileStream) fileSystem.openFile("file0.h", 'w');
        file.write("".getBytes());
    }

    @Test
    public void removeFile() {
    }

    @Test
    public void makeDirectory() {
    }
}