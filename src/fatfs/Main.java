package fatfs;

import drives.Device;

import java.io.FileNotFoundException;

public class Main {

    public static void main(String[] args)
    {
        // Create the ssd
        boolean success = Device.buildDevice("SSD.data", (2<<10)); 
        if(!success) return;
        
        Device d = new Device();
        try {
            d.mount("SSD.data");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        // Create the file system
        fs.IFileSystem file_system = FileSystemFactory.createFileSystem();

        if (file_system==null) {
            System.err.println("You need to implement the factory method!");
            return;
        }

        //TODO do something with the device and the filesystem...
    }
}
