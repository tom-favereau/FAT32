package fatfs;

import fs.IFileSystem;

public class FileSystemFactory {
    /** Factory method creating an instance of a concrete FileSystem class.
     */
    public static IFileSystem createFileSystem() {
        return new FileSystem();
    }
}

