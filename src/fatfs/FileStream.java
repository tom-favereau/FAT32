package fatfs;
import fs.*;

import java.util.Arrays;

public class FileStream implements IFileStream {

    Character mode;
    DataFile file;
    DataAccess data_access;

    public FileStream(DataFile file, Character mode, DataAccess data_access){
        this.mode = mode;
        this.file = file;
        this.data_access = data_access;
    }

    @Override
    public void close() {
        this.mode = null;
        this.file = null;
        this.data_access = null;
    }

    @Override
    public int read(byte[] output) throws EndOfFileException, ForbiddenOperation {
        if (mode != 'r' || file.getAttribut()[4]) {
            throw new ForbiddenOperation();
        } else {
            byte[] full_byte = data_access.readFileByte(this.file);
            int read = full_byte.length;
            System.arraycopy(full_byte, 0, output, 0, read);
            return read;
        }
    }

    @Override
    public int write(byte[] input) throws ForbiddenOperation {
        if (mode == 'w'){
            return data_access.writeFileByte(file, input);
        } else if (mode == 'a'){
            return data_access.writeAppendFileByte(file, input);
        } else {
            throw new ForbiddenOperation();
        }
    }
}
