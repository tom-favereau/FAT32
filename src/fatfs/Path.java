package fatfs;

import fs.ForbiddenOperation;
import fs.IDevice;
import fs.DataAccess;
import fs.DataFile;

import java.nio.file.InvalidPathException;
import java.security.InvalidParameterException;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Vector;
import java.lang.String;

/**
 * Implementation of paths for our file system.
 */
public class Path {

    private Vector<DataFile> elements;
    private DataAccess data_access;
    private boolean absolute;
    private String representation;
    private int length;
    private Path working_directory;

    /**
     * Tests whether a string represents an absolute path or not
     * @param path a string representing and path
     * @return true if it represents an absolute path, false otherwise.
     */
    public boolean isAbsolute(String path){
        return path.charAt(0) == '/';
    }

    /**
     * Gets this file's string full name (name + extension)
     * @param file a data file
     * @return this file's full name in string
     */
    private String fullName(DataFile file){
        if (Objects.equals(file.getExtention(), "   ")){
            return file.getName().strip();
        } else {
            return file.getName().strip() + "." + file.getExtention().strip();
        }
    }

    /**
     * Given a directory and the name of a data file, returns the data file with said name,
     * if it is in said directory.
     *
     * @param directory data file directory in which we search this file
     * @param name string name of this file.
     * @return the data file associated with this file's name if it is in this directory.
     * @throws NoSuchElementException if this file is not in the directory.
     */
    private DataFile findInDirectory(DataFile directory, String name){

        Vector<DataFile> directory_contents;
        directory_contents = data_access.readSubFile(directory);

        // First two elements of readSubFile are . and ..
        for (int i = 0; i < directory_contents.size(); i++){
            DataFile file = directory_contents.elementAt(i);
            if (fullName(file).equals(name)){
                return file;
            }
        }
        throw new NoSuchElementException("File not found");
    }

    /**
     * Given a directory and the name of a data file, checks if this data file is in this directory.
     *
     * @param directory data file directory in which the file is searched.
     * @param file searched data file.
     * @return true if this data file was found, false otherwise.
     */
    private boolean findInDirectoryBoolean(DataFile directory, DataFile file){

        Vector<DataFile> directory_contents;
        directory_contents = data_access.readSubFile(directory);

        // First two elements of readSubFile are . and ..
        for (int i = 0; i < directory_contents.size(); i++){
            DataFile search = directory_contents.elementAt(i);
            if (fullName(search).equals(fullName(file))){
                return true;
            }
        }
        return false;
    }

    /**
     * Given a directory and the name of a data file, checks if this data file is in this directory.
     *
     * @param directory data file directory in which the file is searched.
     * @param file searched data file name
     * @return true if this data file was found, false otherwise.
     */
    protected boolean findInDirectoryBooleanString(DataFile directory, String file){

        Vector<DataFile> directory_contents;
        directory_contents = data_access.readSubFile(directory);

        // First two elements of readSubFile are . and ..
        for (int i = 0; i < directory_contents.size(); i++){
            DataFile search = directory_contents.elementAt(i);
            if (fullName(search).equals(file)){
                return true;
            }
        }
        return false;
    }


    /**
     * Returns the root path.
     * @return the root path
     */
    public Path getRoot() {
        Vector<DataFile> rootFile = new Vector<>();
        rootFile.add(data_access.rootFile());
        return new Path(data_access, "/", rootFile, true, null);
    }

    public Path(DataAccess data_access, String name, Path current_directory){
        if (isAbsolute(name)){
            setterPathAbsolute(data_access, name);
        } else {
            setterPathRelative(data_access, name, current_directory);
        }
    }

    /**
     * Constructor for absolute paths.
     * @param data_access DataAccess instance for the device for which paths will be created.
     * @param representation String representation of this absolute path in the context of data_access.
     * @throws InvalidParameterException if constructed path is not absolute.
     * @throws NoSuchElementException if given path is invalid.
     */
    public void setterPathAbsolute(DataAccess data_access, String representation){
        this.representation = representation;
        String[] string_elements = representation.split("/");
        this.data_access = data_access;
        elements = new Vector<DataFile>();

        if (representation.charAt(0) == '/'){
            absolute = true;
            //RootFILE in DataAccess!!!
            //Had to add it to make my for loop easier to read/write.
            DataFile current_directory = data_access.rootFile();
            elements.add(getRoot().getFile());
            for (int i = 1; i < string_elements.length; i++){
                current_directory = findInDirectory(current_directory, string_elements[i]);
                elements.add(current_directory);
            }
            length = elements.size();
            //By default, an absolute path's directory is null.
            this.working_directory = null;

        } else {
            throw new InvalidParameterException("This path is not absolute, missing working directory");
        }
    }

    /**
     * Returns as a data file the deepest element of this path.
     * @return a data file representing the deepest element of this path.
     */
    public DataFile getFile(){
        return elements.elementAt(length - 1);
    }

    /**
     * Constructor for relative paths. Needs a working directory to be created.
     *
     *@param data_access DataAccess instance for the device for which paths will be created.
     *@param representation String representation of this relative path in the context of data_access.
     *@param working_directory Path for the current working_directory relative to this relative path.
     *@throws InvalidParameterException if constructed path is not relative.
     *@throws NoSuchElementException if given path is invalid.
     *
     */
    private void setterPathRelative(DataAccess data_access, String representation, Path working_directory){

        this.representation = representation;
        String[] string_elements = representation.split("/");
        this.data_access = data_access;
        elements = new Vector<DataFile>();

        DataFile current_directory = working_directory.getFile();

        if(representation.charAt(0) != '/'){
            absolute = false;
            for (String element : string_elements){
                current_directory = findInDirectory(current_directory, element);
                elements.add(current_directory);
            }
            length = elements.size();
            this.working_directory = working_directory;

        } else {
            throw new InvalidParameterException("This path is not relative.");
        }
    }


    protected Path(DataAccess data_access, String representation, Vector<DataFile> elements, boolean absolute, Path working_directory){
        this.absolute = absolute;
        this.representation = representation;
        this.data_access = data_access;
        this.elements = elements;
        this.length = elements.size();
        this.working_directory = working_directory;
    }

    /**
     * Checks if this path is absolute
     * @return true if this path is absolute, false if it's relative.
     */
    public boolean isAbsolute() {
        return absolute;
    }

    /**
     * Returns this path's deepest file as a path.
     * @return this path's deepest file as a path.
     */
    public Path getFileName() {
        DataFile file = getFile();
        String name = fullName(file);
        Vector<DataFile> res = new Vector<DataFile>();
        res.add(file);
        Path new_wd = getParent();
        return new Path(data_access, name, res, true, new_wd);
    }

    /**
     * Checks if this path is the root path.
     * @return true if this path is the root path, false if it's not.
     */
    public boolean isRootPath(){
        //TODO gérer différent de 2
        return this.getFile().getFirstClusterIndex() == 2;
    }

    private String elementsToRepresentation(Vector<DataFile> elements, boolean willBeAbsolute){
        String res = "";

        if (willBeAbsolute){
            res += '/';
            for (int i = 1; i<elements.size(); i++){
                res += fullName(elements.elementAt(i));
                res += '/';
            }
        }
        else {
            for (int i = 0; i < elements.size(); i++) {
                res += fullName(elements.elementAt(i));
                res += '/';
            }
        }

        return res.substring(0, res.length() - 1);
    }

    /**
     * Returns a sub-path of this path.
     * @param beginIndex >= 0, first element of the new sub-path.
     * @param endIndex < number of elements in this path, last element of the new sub-path.
     * @return this path's sub-path starting at beginIndex and ending at endIndex.
     */
    public Path subpath(int beginIndex, int endIndex) {
        if (isRootPath()){
            throw new NoSuchElementException("This path is the root path.");

        } else if (beginIndex < 0 || endIndex > length - 1 || endIndex < beginIndex || beginIndex > length - 1 || endIndex == beginIndex){
            throw new InvalidParameterException("Chosen range invalid.");

        } else {
            boolean willBeAbsolute;
            Vector<DataFile> sub_elements;
            if (absolute) {
                sub_elements = new Vector<>(elements.subList(beginIndex, endIndex+1));
                willBeAbsolute = beginIndex == 0 && absolute;
            }
            else{
                sub_elements = new Vector<>(elements.subList(beginIndex, endIndex));
                willBeAbsolute = beginIndex == 0 && absolute;
            }

            Path new_wd;
            if (willBeAbsolute){
                new_wd = null;
            } else {
                if (absolute){
                    Vector<DataFile> ante_elements = new Vector<>(elements.subList(0, beginIndex+1));
                    new_wd = new Path(data_access, elementsToRepresentation(ante_elements, true), ante_elements, true, null);
                } else {
                    Vector<DataFile> end_of_wd = new Vector<>(elements.subList(0, beginIndex));
                    Vector<DataFile> start_of_wd = this.working_directory.elements;
                    Vector<DataFile> new_wd_elements = new Vector<>();
                    new_wd_elements.addAll(start_of_wd);
                    new_wd_elements.addAll(end_of_wd);
                    new_wd = new Path(data_access, elementsToRepresentation(new_wd_elements, true), new_wd_elements, true, null);
                }
            }
            return new Path(data_access, elementsToRepresentation(sub_elements, willBeAbsolute), sub_elements, willBeAbsolute, new_wd);
        }
    }

    /**
     * Returns this path's parent path.
     * @return parent path.
     */
    public Path getParent() {
        if (isRootPath()){
            throw new NoSuchElementException("Root directory has no parent directory.");
        } else {
            return subpath(0, getNameCount() - 1);
        }
    }

    /**
     * If concatenation is valid, concatenates this path to another path.
     * @param path path to concatenate to this path.
     * @return this path + path
     */
    public Path concatenation(Path path){
        if (path.isAbsolute()){
            throw new InvalidParameterException("Second path cannot be absolute.");
        }
        else {
            DataFile path_first_element = path.elements.elementAt(0);
            if (findInDirectoryBoolean(this.getFile(), path_first_element)){
                String representation = this.representation + '/'+ path.representation;
                Vector<DataFile> elements = new Vector<>();
                elements.addAll(this.elements);
                elements.addAll(path.elements);
                return new Path(data_access, representation, elements, this.absolute, this.working_directory);
            } else {
                throw new InvalidParameterException("Invalid path.");
            }
        }

    }

    public int getNameCount() {
        if (isAbsolute()){
            return length-1;
        }
        else {
            return length;
        }
    }

    /**
     * Converts a path to an absolute path.
     * If this path is absolute, returns this path.
     * @return the absolute path version of this path.
     */
    public Path toAbsolutePath() {
        if (absolute) {
            return this;
        } else {
            return this.working_directory.concatenation(this);
        }
    }

    public Path getWorkingDirectory(){
        return working_directory;
    }

    public String toString(){
        return representation;
    }

    public Vector<DataFile> getElements(){
        return elements;
    }


}
