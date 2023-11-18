package examples;

import drives.Device;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

/** This class is only here to provide an example showing how to read boot sector
 *
 */
public class BootSectorInformation {
    static final int address_BOOT = 0; // first sector of partition
    static  final int offset_bytes_per_sector    = 0x00B;
    static  final int size_bytes_per_sector    = 2;
    static  final int offset_sectors_per_cluster = 0x00D;
    static  final int size_sectors_per_cluster = 1;
    static  final int offset_nb_reserved_sectors =  0x00E;
    static  final int size_nb_reserved_sectors = 2;
    static  final int offset_nb_file_allocation_tables = 0x010;
    static  final int size_nb_file_allocation_tables = 1;
    static  final int offset_nb_logical_sectors   = 0x020;
    static  final int size_nb_logical_sectors   = 4;
    static  final int offset_sectors_per_FAT      = 0x024;
    static  final int size_sectors_per_FAT      = 4;
    static  final int offset_root_directory_start = 0x02C;
    static  final int size_root_directory_start = 4;

    static void printBootSectorInformation(fs.IDevice dev)
    {
        // Get values from Boot Volume

        ByteBuffer bb = ByteBuffer.wrap(dev.read(0));

        int sector_size = bb.getShort(address_BOOT +offset_bytes_per_sector);
        assert(size_bytes_per_sector==2);
        int cluster_size = bb.get(address_BOOT +offset_sectors_per_cluster);
        assert(size_sectors_per_cluster==1);
        int address_FAT0 = bb.getShort(address_BOOT +offset_nb_reserved_sectors);
        assert(size_nb_reserved_sectors==2);
        int nb_FAT = bb.get(address_BOOT +offset_nb_file_allocation_tables);
        assert(size_nb_file_allocation_tables==1);
        int nb_sector = bb.getInt(address_BOOT +offset_nb_logical_sectors);
        assert(size_nb_logical_sectors==4);
        int nb_FAT_sector = bb.getInt(address_BOOT +offset_sectors_per_FAT);
        assert(size_sectors_per_FAT==4);
        int root_cluster = bb.getInt(address_BOOT +offset_root_directory_start);
        assert(size_root_directory_start==4);

        System.out.println("Device Information");
        System.out.println("Size of a sector (in bytes)       = " + sector_size);
        System.out.println("Size of a cluster (in sectors)    = " + cluster_size);
        System.out.println("First sector of first FAT         = " + address_FAT0);
        System.out.println("Total number of sectors in device = " + nb_sector);
        System.out.println("Number of sectors in a single FAT = " + nb_FAT_sector);
        System.out.println("Index of root cluster             = " + root_cluster);
    }

    public static void main(String[] args) throws IOException
    {
        // get an existing device
        Device d = new Device();
        try {
            d.mount("SSD_TestGrading_0_CreateFiles_2.data"); //SSD_TestGrading_2_LargeFiles_2
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        printBootSectorInformation(d);
    }
}
