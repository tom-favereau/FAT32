package fs;

import java.io.FileNotFoundException;

/** IDevice interface represent a storage that require a mount operation
 * to be accessible for reading and writing. Read/write operation can only be
 * done on a per-sector basis.
 */
public interface IDevice
{
    /** Mount an existing device (should already exist)
     *
     *  @param filename name of file where the memory is stored
     */
    void mount(String filename) throws FileNotFoundException;

    /** Unmount the current device
     */
    void unmount();

    /** Get the size of a sector in byte
     * @return number of byte in a disc sector
     */
    short getSectorSize();

    /** Get the number of sectors in the mounted drive
     * @return the number of sector stored in the mounted drive, 0 if no mounted drive
     */
    long getNumberOfSectors();

    /** Read a sector from memory
     *
     * @param sectorId of the sector
     * @return a ByteBuffer which contains the data stored in requested sector
     */
    byte[] read(int sectorId);

    /** Write a sector to memory
     *
     * @param buffer content to be written (should be of size sectorSize)
     * @param sectorId address of the sector
     */
    void write(byte[] buffer, int sectorId);
}
