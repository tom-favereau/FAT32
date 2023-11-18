package fatfs;

import drives.Device;
import fs.EndOfFileException;
import fs.ForbiddenOperation;
import org.junit.Before;
import org.junit.Test;

import javax.swing.plaf.FileChooserUI;
import java.io.File;

import static org.junit.Assert.*;

public class FileStreamTest {

    private FileSystem fileSystem;
    private FileStream fileStreamR;
    private FileStream fileStreamA;
    private FileStream fileStreamW;

    @Before
    public void setUp() throws Exception {
        Device device = new Device();
        device.mount("data/testAlicia/myDevice.data");
        this.fileSystem = new FileSystem();
        this.fileSystem.mount(device);
        this.fileStreamR = (FileStream) this.fileSystem.openFile("file0.h", 'r');
        this.fileStreamA = (FileStream) this.fileSystem.openFile("file0.h", 'a');
        this.fileStreamW = (FileStream) this.fileSystem.openFile("file0.h", 'w');
    }

    @Test
    public void close() {
    }

    @Test
    public void read() throws ForbiddenOperation, EndOfFileException {
    }

    @Test
    public void write() throws ForbiddenOperation, EndOfFileException {
        fileStreamW.write("test bonjour".getBytes());
        fileStreamR = (FileStream) fileSystem.openFile("file0.h", 'r');
        byte[] output = new byte[20];
        fileStreamR.read(output);
        String outputString = new String(output);
        assertTrue(outputString.startsWith("test bonjour"));
        fileStreamW = (FileStream) fileSystem.openFile("file0.h", 'w');
        fileStreamW.write("test".getBytes());
        fileStreamA = (FileStream) fileSystem.openFile("file0.h", 'a');
        fileStreamA.write("test".getBytes());
        fileStreamR = (FileStream) fileSystem.openFile("file0.h", 'r');
        output = new byte[20];
        fileStreamR.read(output);
        outputString = new String(output);
        assertTrue(outputString.startsWith("testtest"));
        fileStreamW = (FileStream) fileSystem.openFile("file0.h", 'w');
        fileStreamW.write("test".getBytes());

    }

    @Test
    public void writeLongFile() throws ForbiddenOperation, EndOfFileException {
        fileStreamW = (FileStream) fileSystem.openFile("file0.h", 'w');
        fileStreamW.write("In computing, a file system or filesystem is used to control how data is stored and retrieved. Without a file system, information placed in".getBytes());
        byte[] output = new byte[1000];
        fileStreamR = (FileStream) fileSystem.openFile("file0.h", 'r');
        fileStreamR.read(output);
        String outputString = new String(output);
        assertTrue(outputString.startsWith("In computing, a file system or filesystem is used to control how data is stored and retrieved. Without a file system, information placed in"));
        fileStreamW = (FileStream) fileSystem.openFile("file0.h", 'w');
        fileStreamW.write("test".getBytes());
    }

    @Test
    public void createAndRemoveFile() throws ForbiddenOperation, EndOfFileException {
        fileStreamW = (FileStream) fileSystem.openFile("abcde.txt", 'w');
        fileStreamW.write("abcdefghijklmnopqrstuvwxyz".getBytes());
        byte[] output = new byte[40];
        fileStreamR = (FileStream) fileSystem.openFile("abcde.txt", 'r');
        fileStreamR.read(output);
        String outputString = new String(output);
        assertTrue(outputString.startsWith("abcdefghijklmnopqrstuvwxyz"));
        fileSystem.removeFile("abcde.txt");
    }


}