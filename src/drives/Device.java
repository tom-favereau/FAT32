package drives;

import fs.IDevice;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/** Device simulate a data storage device, in this case a SSD.
 *  Such device is only able to read/write a full sector of the memory at a time.
 */
public class Device implements IDevice
{
	private RandomAccessFile storage; // file stream representing the device memory
	static final short sectorSize = 512; // size of a sector in byte

	/** Create a Device instance without any mounted disc
 	 */
	public Device() {
		storage = null;
	}

	/** Create a disc image
	 *
	 *  @param filename name of file where the memory will be stored
	 *  @param size size of the memory in number of sectors
	 *  @return true if the simulated device (eg. file) was correctly created
	 */
	public static boolean buildDevice(String filename, int size) {
		//File.createNewFile()
		try {
			File file = new File(filename);
			var file_storage = new RandomAccessFile(file,"rw");
			Device d = new Device(); // required to have access to the size
			file_storage.setLength(size*sectorSize);
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}


	/** Mount an existing device (should already exist)
	 *
	 *  @param filename name of file where the memory is stored
	 */
	public void mount(String filename) throws FileNotFoundException
	{
		if (this.storage!=null) {
			unmount();
		}
		File file = new File(filename);
		if (file.exists()) {
			this.storage = new RandomAccessFile(file,"rw");
		} else {
			System.err.println("Attemp to retrieve a non-existing Device !");
			this.storage = null;
		}
	}
	
	/** Unmount the current device
	 */
	@Override
	public void unmount() {
		try {
			this.storage.close();
			this.storage = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	////////////////
	/// Accessor ///
	////////////////

	/** Get the size of a sector in byte
	 * @return number of byte in a disc sector
	 */
	@Override
	public short getSectorSize() { return sectorSize; }

	/** Get the number of sectors in the mounted drive
	 * @return the number of sector stored in the mounted drive, 0 if no mounted drive
	 */
	@Override
	public long getNumberOfSectors() {
		try {
			if(storage != null)
				return storage.length()/sectorSize;
			else
				return 0;
		} catch (IOException e) {
			return 0;
		} 
	}
	
	/////////////////////
	/// Memory Access ///
	/////////////////////
	
	/** Read a sector from memory
	 * 
	 * @param sectorNum of the sector
	 * @return a ByteBuffer which contains the data stored in requested sector
	 */
	@Override
	public byte[] read(int sectorNum) {
		try {
			this.storage.seek(sectorNum * this.sectorSize);

			byte[] buffer = new byte[this.sectorSize];
			this.storage.read(buffer);
			return buffer;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}	
	}
	
	/** Write a sector to memory
	 * 
	 * @param buffer content to be written (should be of size sectorSize)
	 * @param sectorNum address of the sector
	 */
	@Override
	public void write(byte[] buffer, int sectorNum)  {

		assert(buffer.length==this.sectorSize);

		/* test if sectorNum is within limits, or raise exception */
		
		try {
			this.storage.seek(sectorNum * this.sectorSize);
			this.storage.write(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
}
