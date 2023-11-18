package fs;

import java.io.FileNotFoundException;

/** IFileSystem interface allows the manipulation of formatted device.
 *
 * For the sake of simplicity, it allows manipulation of files from a working directory (associated
 * to the FileSystem instance and NOT to a given process or thread).
 */
public interface IFileSystem
{
    /** Format the device
     *
     * @param dev device to be formatted
     */
    void format(IDevice dev, int size_of_cluster);

    /** Mount a device (assume it has been properly formatted before)
     *
     * @param dev device to be mounted
     */
    void mount(IDevice dev);

    /** Unmount a device
     */
    void unmount();

    /** Get the amount of free space
     *
     * @return the amount of free space in the currently mounted device, 0 if no device is mounted
     */
    int totalFreeSpace();

    /** Set the working directory in the mounted device
     *
     * @param path path to new working directory
     */
    void setWorkingDirectory(String path) throws FileNotFoundException;

    /** Open a stream to a file.
     *
     *  @param filename name of the file to open
     * 	@param mode if 'r' open the file in read-only mode
     *  else if 'w' open the file in write mode (erase its current content).
     *  else if 'a' open the file in append mode (start writing at the end of the file)
     *
     *  For both 'r' and 'w', the file descriptor is set at the beginning of the file.
     *  For 'a', the file descriptor is set at the end of the file.
     *
     *  @return return the stream object if open succeed, a null reference otherwise
     */
    IFileStream openFile(String filename, char mode);

    /** Delete a file or empty directory
     *
     *  @param filename name of the file to delete
     *  @return false if trying to delete a non-empty directory or a system file
     */
    boolean removeFile(String filename);

    /** Create a directory
     *
     *  @param directory_name name of the directory to create
     *  @return false if the directory cannot be created
     */
    boolean makeDirectory(String directory_name);
}
