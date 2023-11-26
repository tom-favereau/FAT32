# SE-TemplateInit-FAT32

# Team
FAVEREAU Tom

SHAO Alicia 

SUSPENE Elise 

# Overview
Here is our FAT-32 file system implementation. 

The mandatory interfaces IFileStream and IFileSystem have been implemented in FileStream and FileSystem respectively.
In addition, our file system relies on these classes:
- FatAccess 
- DataFile 
- DataAccess
- Path
- Shell

**N.B. To open a shell on a formatted device, please run the Shell class in the package Shell.**

A graphical file browser was implemented by Tom Favereau. Several more classes are involved in this extra part of the project:
- DiskTool 
- FileManagerApp 
- MainFileManager 
- ShellGraphics

**N.B. In order to open the file browser, run the MainFileManger class at NOT the FileManagerApp.**

# In Depth Description of core classes
## FatAccess (Tom Favereau)
This class interacts with the File Allocation Table of a disk, and allows to read and write in it.

A FatAccess instance has 9 attributes, which all have getters:
- The device from which the FAT is read.
- The index of the first sector allocated for the FAT.
- The size of a sector.
- The size of a cluster.
- The total number of sectors.
- The number of cases in the FAT.
- The number of FATs.
- The number of sectors per FAT.
- The index of the root sector.

Constructor:

A FatAccess instance is constructed via a device: FatAccess(IDevice device).

Methods:
- read(int index): reading at given index in the FAT.
- write (int data, int index): writing data at a given index in the FAT.
- remove(int index): mark the cluster at a given index as free in the FAT.
- isEmpty(int index): checks if a cluster at a given index is free in the FAT.
- totalFreeSpace(): returns the number of current free clusters in the FAT.
- firstFreeCluster(): returns the index of the first free cluster of the FAT.

## DataFile (Tom Favereau)
DataFile encapsulates data associated with a file:
- its name
- its extension
- its attributes
- its size
- the index of its first cluster
- its parent directory

## DataAccess (Tom Favereau, Alicia Shao)
This class allows interaction with the data sectors of a given disk by reading and writing in them.

Requires FatAccess.

Constructor
Requires a device and a FatAccess instance: DataAccess(IDevice device, FatAccess fatAccess)

Getters

getBeginDataSector(): returns the index of the first data sector.

Methods
- readSubFile(DataFile directory): returns a DataFile vector of all the files contained by a directory.
- readSubFileRoot(): returns the contents of the root directory as a DataFile vector.
- isEmpty(DataFile directory): checks whether a directory is empty.
- rootFile() returns the DataFile associated with root.
- readFileContent(DataFile file) returns the content of a file as a String.
- readFileByte(DataFile file) returns the content of a file as a byte array.
- addFile(DataFile directory, String name, String extension, boolean[] attribute): adds an empty file with a given name, extension and given attributes to a given directory.
-   addFileRoot(String name, String extension, boolean[] attribute): same thing as addFile, but insie root directory.
-   removeFile(DataFile file): removes a file from its parent directory.
-   writeAppendFile(DataFile file, String data): appends data to a file.
-   writeAppendFileByte(DataFile file, byte[] data): appends data in binary to a file.
-   writeFile(DataFile file, String data): overwrites a file and writes data.
-   writeFileByte(DataFile file, byte[] data) same thing but with bytes.

## Path (Alicia Shao)
Path manages paths in a given device. An instance of Path is basically the accumulation of data files which make up the path. Supports the construction of a relative path, assuming its complement path to root is given.

Requires DataFile and DataAccess for a given device.

Constructor 

A single constructor is used for both relative and absolute Paths: a relative path requires the path to the directory from which it's relative two, it's null for an absolute path.

Path(DataAccess dataAcces, String path, Path currentDirectory)

Methods:
- getFile(): returns the data file at which the path points.
- getFileName(): returns the data file at which the path points as a path.
- isAbsolute(): checks whether a path is absolute.
- isRootPath(): checks whether a path is the root.
- subpath(int startIndex, int endIndex): returns a subpath starting at the startIndexth and ending at the endIndexth excluded element of a path.
- getParent(): returns the parent directory of a file as a path.
- concatenation(Path path): concatenates a path with an another, if it's possible.
- getNameCount(): returns the number of elements which constitute the path.

## FileSystem (Tom Favereau, Alicia Shao)
FileSystem implements the IFileSystem interface given in the instructions.

Relies on a device for which a file system is built, its associated DataAccess instance and FatAccess instance, as well as a Path instance representing the working directory of the file system.

Methods:
- format(IDevice device, int size_of_cluster): formats a device by writing or file system on the device, with a given cluster size.
- mount(IDevice device): mounts a previously formatted device to a FileSystem.
- unmount(): unmounts a device from a FileSystem instance by resetting FileSystem's attributes to null.
- totaFreeSpace(): returns the amount of free space in number of clusters.
- setWorkingDirectory(String path): sets the working directory of the file system, if the path is valid.
- openFile(String filename, char mode): returns a FileStream instance of a given file in a given mode, if it's possible. Adds a new file if it doesn't exist in write and append mode.
- listSubFile(String filename): lists the contents of a directory, or returns the file if given file is not a directory.
- removeFile(String filename): deletes a file or a directory if it's empty.
- makeDirectory(String directory_name): creates a directory.

## FileStream (Alicia Shao)
FileStream implements the IFileStream interface given in the instruction. 
Allows you to open a file in read 'r', write 'w' or append 'a' mode and performs read and write operations using DataAccess.

Relies on DataAccess to access the data area of a device and DataFile: the file to be opened.

Methods:
- close() : closes a FileStream and resets the attributes to null.
- read(byte[] output) : writes the file content in output and returns the number of bytes effectively read.
- write(byte[] input) : writes the input in the file and returns the number of bytes effectively written.


## Shell (Tom Favereau, Elise Suspène)

Implements a basic shell that supports absolute and relative paths and responds to the commands :
- ls arg : lists the sub files of arg.
- pwd : prints the working directory.
- cd arg : changes the working directory to arg.
- echo "foo" > foo.h : writes "foo" in foo.h and creates foo.h if it doesn't exist.
- echo "foo" >> foo.h : writes "foo" at the end of foo.h and creates foo.h if it doesn't exist.
- less arg : prints the content of arg.
- mkdir arg : creates the directory arg.
- touch arg : creates the file arg.
- exit : exits the shell loop.

Shell works by calling the appropriate functions of a FileSystem instance after the parsing the user's commands.

To open a shell on a formatted device, please run the class Shell.

