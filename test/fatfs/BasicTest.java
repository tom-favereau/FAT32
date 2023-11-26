package fatfs;



import drives.Device;
import org.junit.Test;

import java.io.FileNotFoundException;

import static org.junit.Assert.*;


public class BasicTest {

    @Test(timeout = 1000)
    public void test_FormatDisc_CreateFile() {
        String device_name = "NewEmptyDevice";
        // Create the ssd
        boolean success = Device.buildDevice(device_name,  1024);
        assertTrue(success);

        Device d = new Device();
        try {
            d.mount(device_name);
        } catch (FileNotFoundException e) {
            fail("Disc could not be mounted");
        }

        // Format
        fs.IFileSystem filesystem = null;
        //if (false){
        //    // The test should also pass if using another properly implemented filesystem class here
        //    // or during writing or reading
        //    filesystem = fs_other.FileSystemFactory.createFileSystem();
        //    filesystem.format(d, 2);
        //} else {
        filesystem = fatfs.FileSystemFactory.createFileSystem();
        filesystem.format(d, 2);
        //}

        // use another instance of filesystem, the data should be persistent !
        // if the test do not pass, but you expect it to pass, you can try to comment out
        // the following two lines
        filesystem = fatfs.FileSystemFactory.createFileSystem();
        filesystem.mount(d);

        try {
            var f = filesystem.openFile("other0.h", 'w');
            assertTrue("Did not create file (or stream creation is broken)", f!=null);
            f.close();
        } catch (Exception e) {
            fail("Exception raised when creating file");
        }

        filesystem.unmount();

        // use another instance of filesystem, the data should be persistent !
        // if the test do not pass, but you expect it to pass, you can try to comment out
        // the following two lines
        filesystem = fatfs.FileSystemFactory.createFileSystem();
        filesystem.mount(d);

        try {
            var f = filesystem.openFile("other0.h", 'r');
            assertTrue("Created file not found (or stream creation is broken)", f!=null);
            f.close();
        } catch (Exception e) {
            fail("Exception raised when looking for the created file");
        }

        filesystem.unmount();
    }

    @Test(timeout = 1000)
    public void test_FormatDisc_CreateAndWriteFullSector() {
        // if you want to pass this test be do not manage yet properly the size of a file during write operation,
        // simply create file with a size equal to a sector or cluster instead of empty

        String device_name = "NewEmptyDevice";
        // Create the ssd
        boolean success = Device.buildDevice(device_name, 1024);
        assertTrue(success);

        Device d = new Device();
        try {
            d.mount(device_name);
        } catch (FileNotFoundException e) {
            fail("Disc could not be mounted");
        }

        // Format
        fs.IFileSystem filesystem = null;
        //if (false){
        //    // The test should also pass if using another properly implemented filesystem class here
        //    // or during writing or reading
        //    filesystem = fs_other.FileSystemFactory.createFileSystem();
        //    filesystem.format(d, 2);
        //} else {
        filesystem = fatfs.FileSystemFactory.createFileSystem();
        filesystem.format(d, 2);
        //}

        // use another instance of filesystem, the data should be persistent !
        // if the test do not pass, but you expect it to pass, you can try to comment out
        // the following two lines
        filesystem = fatfs.FileSystemFactory.createFileSystem();
        filesystem.mount(d);

        try {
            var f = filesystem.openFile("other0.h", 'w');
            assertTrue("Did not create file (or stream creation is broken)", f!=null);

            byte tab[] = new byte[d.getSectorSize()];
            for (int i=0; i<d.getSectorSize(); ++i) {
                tab[i] = (byte)(i % d.getSectorSize());
            }

            int num_written = f.write(tab);
            assertEquals(d.getSectorSize(),num_written);
            f.close();
        } catch (Exception e) {
            fail("Exception raised when creating/writing file");
        }

        filesystem.unmount();

        // use another instance of filesystem, the data should be persistent !
        // if the test do not pass, but you expect it to pass, you can try to comment out
        // the following two lines
        filesystem = fatfs.FileSystemFactory.createFileSystem();
        filesystem.mount(d);

        try {
            var f = filesystem.openFile("other0.h", 'r');
            assertTrue("Created file not found (or stream creation is broken)", f!=null);

            byte tab[] = new byte[d.getSectorSize()];
            int num_read = f.read(tab);
            assertEquals(d.getSectorSize(), num_read);
            for (int i=0; i<d.getSectorSize(); ++i) {
                assertEquals("Check read back of written cluster", tab[i], (byte)(i % d.getSectorSize()));
            }

            f.close();
        } catch (Exception e) {
            fail("Exception raised when reading file");
        }
    }

    @Test(timeout = 1000)
    public void test_ExistingDisc_CreateFile() {

        String device_name = "SSD_0_CreateFiles_2_copy.data";
        //Path originalPath = original.toPath();

        // Create a copy of the disc file to keep the original unchanged
        try {
            java.nio.file.Path copied = java.nio.file.Paths.get("SSD_0_CreateFiles_2_copy.data");
            System.out.println(copied);
            java.nio.file.Path original = java.nio.file.Paths.get("data/discs/SSD_0_CreateFiles_2.data");
            System.out.println(original);
            //original.toPath();
            java.nio.file.Files.copy(original, copied, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            fail("Error in disc copy");
        }

        Device d = new Device();
        try {
            d.mount(device_name);
        } catch (FileNotFoundException e) {
            fail("Disc could not be mounted");
        }

        fs.IFileSystem filesystem = null;
        filesystem = fatfs.FileSystemFactory.createFileSystem();
        filesystem.mount(d);

        try {
            var f = filesystem.openFile("other0.h", 'w');
            assertTrue("Did not create file (or stream creation is broken)", f!=null);
            f.close();
        } catch (Exception e) {
            fail("Exception raised when creating file");
        }

        filesystem.unmount();

        // use another instance of filesystem, the data should be persistent !
        filesystem = fatfs.FileSystemFactory.createFileSystem();
        filesystem.mount(d);

        try {
            var f = filesystem.openFile("other0.h", 'r');
            assertTrue("Created file not found (or stream creation is broken)", f!=null);
            f.close();
        } catch (Exception e) {
            fail("Exception raised when looking for the created file");
        }

        filesystem.unmount();
    }

    @Test(timeout = 1000)
    public void test_ExistingDisc_CreateAndWriteFullSector() {
        // if you want to pass this test be do not manage yet properly the size of a file during write operation,
        // simply create file with a size equal to a sector or cluster instead of empty

        String device_name = "SSD_0_CreateFiles_2_copy.data";
        //Path originalPath = original.toPath();

        // Create a copy of the disc file to keep the original unchanged
        try {
            java.nio.file.Path copied = java.nio.file.Paths.get("SSD_0_CreateFiles_2_copy.data");
            java.nio.file.Path original = java.nio.file.Paths.get("data/discs/SSD_0_CreateFiles_2.data");
            //original.toPath();
            java.nio.file.Files.copy(original, copied, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            fail("Error in disc copy");
        }

        Device d = new Device();
        try {
            d.mount(device_name);
        } catch (FileNotFoundException e) {
            fail("Disc could not be mounted");
        }

        fs.IFileSystem filesystem = null;
        filesystem = fatfs.FileSystemFactory.createFileSystem();
        filesystem.mount(d);

        try {
            var f = filesystem.openFile("other0.h", 'w');
            assertTrue("Did not create file (or stream creation is broken)", f!=null);

            byte tab[] = new byte[d.getSectorSize()];
            for (int i=0; i<d.getSectorSize(); ++i) {
                tab[i] = (byte)(i % d.getSectorSize());
            }
            int num_written = f.write(tab);
            assertEquals(d.getSectorSize(),num_written);
            f.close();
        } catch (Exception e) {
            fail("Exception raised when creating/writing file");
        }

        filesystem.unmount();

        // use another instance of filesystem, the data should be persistent !
        filesystem = fatfs.FileSystemFactory.createFileSystem();
        filesystem.mount(d);

        try {
            var f = filesystem.openFile("other0.h", 'r');
            assertTrue("Created file not found (or stream creation is broken)", f!=null);

            byte tab[] = new byte[d.getSectorSize()];
            int num_read = f.read(tab);
            assertEquals(d.getSectorSize(), num_read);
            for (int i=0; i<d.getSectorSize(); ++i) {
                assertEquals("Check read back of written cluster", tab[i], (byte)(i % d.getSectorSize()));
            }

            f.close();
        } catch (Exception e) {
            fail("Exception raised when reading file");
        }
    }
}

