
package fs;

import drives.Device;

import java.io.FileNotFoundException;

public class FileSystem implements IFileSystem{

    private Device device = new Device();

    @Override
    public void format(IDevice dev, int size_of_cluster) {

    }

    @Override
    public void mount(IDevice dev) {

    }

    @Override
    public void unmount() {

    }

    @Override
    public int totalFreeSpace() {
        return 0;
    }

    @Override
    public void setWorkingDirectory(String path) throws FileNotFoundException {

    }

    @Override
    public IFileStream openFile(String filename, char mode) {
        return null;
    }

    @Override
    public boolean removeFile(String filename) {
        return false;
    }

    @Override
    public boolean makeDirectory(String directory_name) {
        return false;
    }
}