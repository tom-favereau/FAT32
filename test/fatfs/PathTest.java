package fatfs;

import drives.Device;
import org.junit.Before;
import fs.DataAccess;
import fs.DataFile;
import org.junit.Test;

import java.security.InvalidParameterException;
import java.util.NoSuchElementException;
import java.util.Vector;

import static org.junit.Assert.*;

public class PathTest {
    private DataAccess data_access;
    @Before
    public void setUp() throws Exception {
        Device device = new Device();
        device.mount("data/testAlicia/myDevice.data");
        FatAccess fat_access1= new FatAccess(device);
        this.data_access= new DataAccess(device, fat_access1);
    }

    public String[] elementsToName(Vector<DataFile> elements){
        String[] res = new String[elements.size()];
        int i = 0;
        for (DataFile element : elements){
            res[i] = element.getName().strip();
            i++;
        }
        return res;
    }
    @Test
    public void absoluteConstructorTest(){

      Path path = new Path(data_access, "/dirbal/dir2/racoon/file6.cpp", null);

      Vector<DataFile> elements = path.getElements();
      assertEquals(4, path.getNameCount());

      String[] expected = {"root", "dirbal", "dir2", "racoon", "file6"};
      assertEquals(expected, elementsToName(elements));
      assertTrue(path.isAbsolute());
      assertEquals("/dirbal/dir2/racoon/file6.cpp", path.toString());
    }

    @Test(expected = NoSuchElementException.class)
    public void absoluteConstructorInvalidPathTest(){
        Path path = new Path(data_access, "/dirbal/dir2/raccoon", null);
    }

    @Test
    public void relativeConstructor(){
        Path wd = new Path(data_access, "/dirbal/dir2", null);
        Path path = new Path(data_access, "racoon/file6.cpp", wd);

        assertEquals(2, path.getNameCount());
        String[] expected = {"racoon", "file6"};
        assertEquals(expected, elementsToName(path.getElements()));
        assertFalse(path.isAbsolute());
        assertEquals("racoon/file6.cpp", path.toString());
    }

    @Test (expected = NoSuchElementException.class)
    public void relativeConstructorInvalidWDTest(){
        Path wd = new Path(data_access, "/dirbal/dir3", null);
        Path path = new Path(data_access, "racoon/file6.cpp", wd);
    }

    @Test (expected = NoSuchElementException.class)
    public void relativeConstructorInvalidPathTest(){
        Path wd = new Path(data_access, "/dirbal/dir2", null);
        Path path = new Path(data_access, "racoon/file7.java", wd);
    }

    @Test
    public void getParentAbsolute(){
        Path path = new Path(data_access, "/dirbal/dir2/racoon/file6.cpp", null);
        Path parent = path.getParent();

        Vector <DataFile> elements = parent.getElements();
        assertEquals(parent.toString(), "/dirbal/dir2/racoon");

        assertTrue(parent.isAbsolute());
        assertEquals(3, parent.getNameCount());
        String[] expected = {"root", "dirbal", "dir2", "racoon"};
        assertEquals(expected, elementsToName(elements));
    }

    @Test
    public void getParentRelative(){
        Path wd = new Path(data_access, "/dirbal/dir2", null);
        Path path = new Path(data_access, "racoon/file6.cpp", wd);
        Path parent = path.getParent();

        assertTrue(parent.toString().equals("racoon"));

        assertFalse(parent.isAbsolute());
        assertEquals(1, parent.getNameCount());

        String[] expected = {"racoon"};
        assertEquals(expected, elementsToName(parent.getElements()));
    }

    @Test
    public void subPathRelative(){
        Path wd = new Path(data_access, "/dirbal", null);
        Path path = new Path(data_access, "dir2/racoon/file6.cpp", wd);

        Path subpath = path.subpath(1,2);

        assertEquals(1, subpath.getNameCount());
        assertEquals("racoon", subpath.toString());

        Path actual_wd = subpath.getWorkingDirectory();

        assertEquals(2, actual_wd.getNameCount());
        assertEquals("/dirbal/dir2", actual_wd.toString());
    }

    @Test
    public void concatenationAbsoluteRelativeTest(){
        Path path_1 = new Path(data_access, "/dirbal/dir2", null);
        Path path_2 = new Path(data_access,"racoon/file6.cpp", path_1);
        Path path = path_1.concatenation(path_2);

        assertEquals(4, path.getNameCount());
        assertTrue(path.isAbsolute());
        assertEquals("/dirbal/dir2/racoon/file6.cpp", path.toString());
    }

    @Test
    public void concatenationRelativeRelativeTest(){
        Path wd_1 = new Path(data_access, "/dirbal", null);
        Path wd_2 = new Path(data_access, "/dirbal/dir2", null);
        Path path1 = new Path(data_access, "dir2", wd_1);
        Path path2 = new Path(data_access, "racoon/file6.cpp", wd_2);

        Path path = path1.concatenation(path2);

        assertEquals(3, path.getNameCount());
        assertFalse(path.isAbsolute());
        assertEquals("dir2/racoon/file6.cpp", path.toString());
        assertEquals("/dirbal", path1.getWorkingDirectory().toString());
    }

    @Test
    public void toAbsolutePathRelative(){
        Path wd = new Path(data_access, "/dirbal/dir2", null);
        Path path = new Path(data_access, "racoon/file6.cpp", wd);

        Path abs = path.toAbsolutePath();
        assertTrue(abs.isAbsolute());
        assertEquals("/dirbal/dir2/racoon/file6.cpp", abs.toString());

        String[] expected = {"root", "dirbal", "dir2", "racoon", "file6"};
        assertEquals(expected, elementsToName(abs.getElements()));
    }

    @Test
    public void isRootPathTest(){
        Path wd = new Path(data_access, "/dirbal/dir2", null);
        Path root = wd.getRoot();
        assertTrue(root.isRootPath());
    }
}
