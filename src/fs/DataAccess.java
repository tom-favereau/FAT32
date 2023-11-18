package fs;

import drives.Device;
import fatfs.FatAccess;

import javax.xml.crypto.Data;
import java.util.Vector;
//import javax.xml.crypto.Data;

/**
 * permet d'acceder en lecture et en écriture a la zone data
 * pour les cas ou des repertoire sont donné a la place des fichier ou inversement
 * j'ai prix exemple sur les fonction cat et ls
 * attention il y a un comportement anbigue a gérer du au fait que le premier cluster dans la fat c'est le 3 ème
 */
public class DataAccess{

    private IDevice device;
    private FatAccess fatAccess;
    private int beginDataSector;


    public DataAccess(IDevice device, FatAccess fatAccess){
        this.device = device;
        this.fatAccess = fatAccess;
        this.beginDataSector = this.fatAccess.getBeginFatSector() + this.fatAccess.getNumberOfFat()*this.fatAccess.getSectorPerFat();
    }

    /**
     * doit imprérativement vérifier qu'il s'agit d'un repertoire
     * @param directory a DataFile which represent a directory
     * @return the liste of its sub file
     */
    public Vector<DataFile> readSubFile(DataFile directory){
        if (!directory.getAttribut()[4]){
            Vector<DataFile> res = new Vector<DataFile>();
            res.add(directory);
            return res;
        }
        else {
            int index = directory.getFirstClusterIndex();
            int nextIndex = index;
            Vector<DataFile> res = new Vector<DataFile>();
            boolean lastFile = false; //si on arrive sur un fichier 0x00
            do { //on itère sur la liste chainé
                index = nextIndex;
                for (int j = 0; j<fatAccess.getSizeCluster(); j++){ //on itère sur la taille des cluster
                    byte[] sector = device.read(beginDataSector+j+index*fatAccess.getSizeCluster()); //TODO à tester je suis vraiment pas sur
                    for (int i = 0; i<fatAccess.getSizeSector(); i+=32){ //on itère sur les sous fichier
                        byte[] byteName = new byte[8]; //créer pour le constructeur String
                        for (int k = 0; k<8; k++){
                            byteName[k] = sector[i+k];
                        }
                        if (byteName[0] == 0x00) { // on verifie si il s'agit du dernier fichier 0x00
                            lastFile = true;
                            break;
                        }
                        if (byteName[0] == (byte) 0xE5){
                            //TODO tester impérativement on sort de la plage de byte (normalement les conversion que j'ai fait dans readbyte devrai rêgler le problème mais je suis pas sur)
                            continue;
                        }
                        String name = new String(byteName);
                        String extention = new String(new byte[]{sector[i+8], sector[i+9], sector[i+10]});
                        boolean[] attribut = new boolean[8]; // faut que je vois comment faire ça
                        for (int k = 7; k>=0; k--){
                            attribut[k] = (sector[11+i] & (1 << k)) != 0;
                        }
                        int firstClusterIndex = readBytes(sector, i+20, 4);
                        int size = readBytes(sector, i+28, 4);
                        DataFile newFile = new DataFile(name, extention, attribut, firstClusterIndex, size, directory);
                        res.add(newFile);
                    }
                    if (lastFile){ // on sort de la boucle on a trouvé le dernier fichier
                        break;
                    }
                }
                if (lastFile){
                    break;
                }
                nextIndex = fatAccess.read(index);
            } while (nextIndex != 0x0FFFFFFF);
            return res;
        }
    }

    public boolean isEmpty(DataFile directory){
        return readSubFile(directory).size() == 2;
    }

    /**
     * nécessaire car on a pas accet à a un DataFile quand on commence l'arborecense
     */
    public Vector<DataFile> readSubFileRoot(){
        boolean[] rootAttribut = new boolean[8];
        rootAttribut[0] = false; rootAttribut[1] = false; rootAttribut[2] = true; rootAttribut[4] = true;
        DataFile rootFile = new DataFile("root", "", rootAttribut, fatAccess.getRootIndex(), 0, null);
        return readSubFile(rootFile);
    }

    public DataFile rootFile(){
        boolean[] rootAttribut = new boolean[8];
        rootAttribut[0] = false; rootAttribut[1] = false; rootAttribut[2] = true; rootAttribut[4] = true;
        return new DataFile("root    ", "   ", rootAttribut, fatAccess.getRootIndex(), 0, null);
    }

    /**
     *
      * @param file the information about a file
     * @return the content of the file
     */
    public String readFileContent(DataFile file){//on a besoin de la taille pour connaitre la fin du fichier (elle est donné dans les information du fichier)
        //TODO tester impérativement cette méthode
        if (file.getAttribut()[4]){
            return file.getName() + "/ is a directory";
        }
        else {
            int index = file.getFirstClusterIndex();
            int size = file.getSize();
            byte[] res = new byte[size];
            int nexIndex = index;
            int sizeAct = 0;// verifie q'on lit pas plus loins que la taille alouer
            do { // on itére sur la liste chainé
                index = nexIndex;
                for (int j = 0; j < fatAccess.getSizeCluster(); j++) { // on itére sur la taille des cluster
                    byte[] sector = device.read(beginDataSector+j+index*fatAccess.getSizeCluster()); //TODO a tester je suis vraiment pas sur
                    for (int i = 0; i < fatAccess.getSizeSector(); i++) { // on itere sur la secteur
                        if (sizeAct == size) {
                            break;
                        } else {
                            res[sizeAct] = sector[i];
                            sizeAct++;
                        }
                    }
                }
                nexIndex = fatAccess.read(index); // il y a un problème là avec la condition d'ârret
            } while (nexIndex != 0x0FFFFFFF); //signe que c'etait le dernier maillon
            return new String(res);
        }
    }

    /**
     *
     * @param file the information about a file
     * @return the content of the file
     */
    public byte[] readFileByte(DataFile file) throws ForbiddenOperation {//on a besoin de la taille pour connaître la fin du fichier (elle est donnée dans les information du fichier)
        //TODO tester impérativement cette méthode
        if (file.getAttribut()[4]){
            throw new ForbiddenOperation();
        }
        else {
            int index = file.getFirstClusterIndex();
            int size = file.getSize();
            byte[] res = new byte[size];
            int nexIndex = index;
            int sizeAct = 0;// vérifie qu'on ne lit pas plus loins que la taille alouée
            do { // on itére sur la liste chainée
                index = nexIndex;
                for (int j = 0; j < fatAccess.getSizeCluster(); j++) { // on itère sur la taille des clusters
                    byte[] sector = device.read(beginDataSector+j+index*fatAccess.getSizeCluster()); //TODO a tester je suis vraiment pas sûr
                    for (int i = 0; i < fatAccess.getSizeSector(); i++) { // on itère sur le secteur
                        if (sizeAct == size) {
                            break;
                        } else {
                            res[sizeAct] = sector[i];
                            sizeAct++;
                        }
                    }
                }
                nexIndex = fatAccess.read(index); // il y a un problème là avec la condition d'ârret
            } while (nexIndex != 0x0FFFFFFF); //signe que c'etait le dernier maillon
            return res;
        }
    }


    /**
     *
     * @param directory repertoire dans lequel on crée le fichier
     * @param name du fichier
     * @param extention du fichier
     * @param attribut du fichier
     * @return the new DataFile and null for a lack of space
     */
    public DataFile addFile(DataFile directory, String name, String extention, boolean[] attribut){ //TODO si tout vas bien il faudra modifier la fonction pour limiter la fragmentation
        if (fatAccess.totalFreeSpace() == 0){
            return null; // pas asser d'espace sur le disque
        }
        else{
            //dans un premier temps on vas chercher un endroit un ou écrire les information dans directory
            int index = directory.getFirstClusterIndex();
            int nextIndex = index;
            do { //on itère sur la liste chainé dans la fat
                index = nextIndex;
                for (int j = 0; j<fatAccess.getSizeCluster(); j++){ //on itère sur la taille des cluster
                    byte[] sector = device.read(beginDataSector+j+index*fatAccess.getSizeCluster()); //TODO à tester je suis vraiment pas sur
                    for (int i = 0; i<fatAccess.getSizeSector(); i+=32){ //on itère sur les sous fichier
                        byte[] byteName = new byte[8];
                        for (int k = 0; k<8; k++){
                            byteName[k] = sector[i+k];
                        }
                        if (byteName[0] == 0x00 || byteName[0] == (byte) 0xE5) { // on verifie si il s'agit du dernier fichier 0x00 ou si il s'agit d'un fichie supprimé
                            //TODO tester impérativement on sort de la plage de byte (normalement les conversion que j'ai fait dans readbyte devrai rêgler le problème mais je suis pas sur)
                            String nameToByte = String.format("%-" + 8 + "s", name); //on complète avec des espaces
                            System.arraycopy(nameToByte.getBytes(), 0, sector, i, 8);
                            String extentionToByte = String.format("%-" + 3 + "s", extention); // on complète avec des espcaes
                            System.arraycopy(extentionToByte.getBytes(), 0, sector, i+8, 3);
                            byte newFileAttribut = 0;
                            for (int k = 0; k<8; k++){
                                if (attribut[k]){
                                    newFileAttribut |= (1 << k);
                                }
                                //newFileAttribut |= (attribut[k] ? 1 : 0) << (7-k);
                            }
                            sector[i+11] = newFileAttribut;
                            int firstClusterIndex = fatAccess.firstFreeCluster();
                            writeBytes(sector, 20+i, 4, firstClusterIndex); //on écrit l'adresse dans le repertoire
                            fatAccess.write(0x0FFFFFFF, firstClusterIndex); // on déclare le secteur comme pris dans la fat
                            writeBytes(sector, 28+i, 4, 0);

                            //on vas maintenant mettre le nouveau cluster a 0
                            byte[] newFileSector = new byte[fatAccess.getSizeSector()];
                            for (int k = 0; k<fatAccess.getSizeCluster(); k++) {
                                if (k == 0 && attribut[4]){
                                    //on écrit les fichier .. et .
                                    // dir .
                                    String nameToByteDir = String.format("%-" + 8 + "s", "."); //on complète avec des espaces
                                    System.arraycopy(nameToByteDir.getBytes(), 0, newFileSector, 0, 8);
                                    String extentionToByteDir = String.format("%-" + 3 + "s", extention); // on complète avec des espcaes
                                    System.arraycopy(extentionToByteDir.getBytes(), 0, newFileSector, 0+8, 3);

                                    newFileSector[0+11] = newFileAttribut;
                                    writeBytes(newFileSector, 28+0, 4, 0);
                                    writeBytes(newFileSector, 20+0, 4, firstClusterIndex);

                                    //dir ..
                                    String nameToByteDirParent = String.format("%-" + 8 + "s", ".."); //on complète avec des espaces
                                    System.arraycopy(nameToByteDirParent.getBytes(), 0, newFileSector, 32, 8);
                                    String extentionToByteDirParent = String.format("%-" + 3 + "s", directory.getExtention()); // on complète avec des espcaes
                                    System.arraycopy(extentionToByteDirParent.getBytes(), 0, newFileSector, 32+8, 3);
                                    byte newFileAttributDirParent = 0;
                                    for (int l = 0; l<8; l++){
                                        if (directory.getAttribut()[l]){
                                            newFileAttributDirParent |= (1 << l);
                                        }
                                        //newFileAttribut |= (attribut[k] ? 1 : 0) << (7-k);
                                    }
                                    newFileSector[32+11] = newFileAttributDirParent;
                                    writeBytes(newFileSector, 28+32, 4, 0);

                                    writeBytes(newFileSector, 20+32, 4, directory.getFirstClusterIndex());

                                }
                                device.write(newFileSector, beginDataSector + k + firstClusterIndex * fatAccess.getSizeCluster());
                            }

                            device.write(sector, beginDataSector+j+index*fatAccess.getSizeCluster());
                            return new DataFile(name, extention, attribut, firstClusterIndex, 0, directory);
                        }
                    }
                }
                nextIndex = fatAccess.read(index);
            } while (nextIndex != 0x0FFFFFFF);
            // le repertoire est plein
            if (fatAccess.totalFreeSpace() <= 1){
                return null; //pas asser d'espace sur le disque (il faut un cluster pour le repertoire et pour le fichier)
            }
            else{
                int newLastIndex = addCluster(directory);
                byte[] sector = device.read(beginDataSector+newLastIndex*fatAccess.getSizeCluster());
                String nameToByte = String.format("%-" + 8 + "s", name); // on complète avec des éspcace
                System.arraycopy(nameToByte.getBytes(), 0, sector, 0, 8);
                String extentionToByte = String.format("%-" + 3 + "s", extention);
                System.arraycopy(extentionToByte.getBytes(), 0, sector, 8, 3); // on complète avec des éspcace
                byte newFileAttribut = 0;
                for (int k = 0; k<8; k++){
                    newFileAttribut |= (attribut[k] ? 1 : 0) << (7-k);
                }
                sector[11] = newFileAttribut;
                int firstClusterIndex = fatAccess.firstFreeCluster();
                writeBytes(sector, 20, 4, firstClusterIndex); //on écrit l'adresse dans le repertoire
                fatAccess.write(0x0FFFFFFF, firstClusterIndex); //on déclare le secteur comme pris dans la fat
                writeBytes(sector, 28, 4, 0);

                //on vas maintenant mettre le nouveau cluster a 0
                byte[] newFileSector = new byte[fatAccess.getSizeSector()];
                for (int k = 0; k<fatAccess.getSizeCluster(); k++) {
                    if (k == 0 && attribut[4]){
                        //on écrit les fichier .. et .
                        // dir .
                        String nameToByteDir = String.format("%-" + 8 + "s", "."); //on complète avec des espaces
                        System.arraycopy(nameToByteDir.getBytes(), 0, newFileSector, 0, 8);
                        String extentionToByteDir = String.format("%-" + 3 + "s", extention); // on complète avec des espcaes
                        System.arraycopy(extentionToByteDir.getBytes(), 0, newFileSector, 0+8, 3);

                        newFileSector[0+11] = newFileAttribut;
                        writeBytes(newFileSector, 28+0, 4, 0);
                        writeBytes(newFileSector, 20+0, 4, firstClusterIndex);

                        //dir ..
                        String nameToByteDirParent = String.format("%-" + 8 + "s", ".."); //on complète avec des espaces
                        System.arraycopy(nameToByteDirParent.getBytes(), 0, newFileSector, 32, 8);
                        String extentionToByteDirParent = String.format("%-" + 3 + "s", directory.getExtention()); // on complète avec des espcaes
                        System.arraycopy(extentionToByteDirParent.getBytes(), 0, newFileSector, 32+8, 3);
                        byte newFileAttributDirParent = 0;
                        for (int l = 0; l<8; l++){
                            if (directory.getAttribut()[l]){
                                newFileAttributDirParent |= (1 << l);
                            }
                            //newFileAttribut |= (attribut[k] ? 1 : 0) << (7-k);
                        }
                        newFileSector[32+11] = newFileAttributDirParent;
                        writeBytes(newFileSector, 28+32, 4, 0);

                        writeBytes(newFileSector, 20+32, 4, directory.getFirstClusterIndex());

                    }
                    device.write(newFileSector, beginDataSector + k + firstClusterIndex * fatAccess.getSizeCluster());
                }

                device.write(sector, beginDataSector+newLastIndex*fatAccess.getSizeCluster());
                return new DataFile(name, extention, attribut, firstClusterIndex, 0, directory);
            }
        }

    }




/**
 * idem que add file
 * @param name du file
 * @param extention du file
 * @param attribut du file
 * @return the new DataFile and null for a lack of space
 */
    public DataFile addFileRoot(String name, String extention, boolean[] attribut){
        boolean[] rootAttribut = new boolean[8];
        rootAttribut[0] = false; rootAttribut[1] = false; rootAttribut[2] = true; rootAttribut[4] = true;
        DataFile rootFile = new DataFile("root", "", rootAttribut, fatAccess.getRootIndex(), 0, null);
        return addFile(rootFile, name, extention, attribut);
    }

    public boolean removeFile(DataFile file){
        //TODO verifier qu'il s'agit bien d'un fichier existant et qu'on a le droit de le supprimer
        //TODO importance moyenne : écrire 0x00 si il s'agit du dernier fichier
        int index = file.getFirstClusterIndex();
        int nextIndex = index;
        do {
            index = nextIndex;
            nextIndex = fatAccess.read(index);
            fatAccess.write(0x00000000, index);
        } while (nextIndex != 0x0FFFFFFF);
        //on déclare le fichier comme supprimé dans le repertoire parent
        DataFile parentFile = file.getParentFile();
        int parentIndex = parentFile.getFirstClusterIndex();
        int nextParentIndex = parentIndex;
        do {
            parentIndex = nextParentIndex;
            for (int j = 0; j < fatAccess.getSizeCluster(); j++) {
                byte[] sector = device.read(beginDataSector + j + parentIndex * fatAccess.getSizeCluster());
                for (int i = 0; i < fatAccess.getSizeSector(); i += 32) {
                    byte[] byteName = new byte[8]; //créer pour le constructeur String
                    for (int k = 0; k < 8; k++) {
                        byteName[k] = sector[i + k];
                    }
                    String name = new String(byteName);
                    byte[] byteExtention = new byte[3];
                    for (int k = 8; k<11; k++){
                        byteExtention[k-8] = sector[i+k];
                    }
                    String extention = new String(byteExtention);
                    if (name.equals(file.getName()) && extention.equals(file.getExtention())) {
                        byteName = new byte[8];
                        byteName[0] = (byte) 0xE5;
                        for (int k = 0; k < 8; k++) {
                            sector[i + k] = byteName[k];
                        }
                        device.write(sector, beginDataSector + j + parentIndex * fatAccess.getSizeCluster());
                    }
                }
            }
            nextParentIndex = fatAccess.read(parentIndex);
        } while (parentIndex != 0x0FFFFFFF);
        return true;
    }

    /**
     *
     * @param file dans lequel on écrit
     * @param data chaine de char à write
     * @return true if no problem (false for a lack of space
     */
    public boolean writeAppendFile(DataFile file, String data){
        //TODO on fait quoi si y'a pas asser d'espace sur le dique ? on copy la parti copiable ou on copy rien du tout
        int index = file.getFirstClusterIndex();
        int nextIndex = index;
        byte[] dataByte = data.getBytes();
        int sizeCopy = dataByte.length;
        int sizeFile = file.getSize();
        do { // on itère sur la liste chainée
            if (sizeCopy == 0){
                break;
            }
            index = nextIndex;
            for (int j = 0; j<fatAccess.getSizeCluster(); j++){ // on itère sur les cluster
                if (sizeCopy == 0){
                    break;
                }
                byte[] sector = device.read(beginDataSector+j+index*fatAccess.getSizeCluster());
                for (int i = 0; i<fatAccess.getSizeSector(); i++){ //on itère dans le secteur
                    if (sizeCopy == 0){
                        break;
                    }
                    else if (sizeFile <= 0){
                        sector[i] = dataByte[dataByte.length-sizeCopy];
                        sizeCopy--;
                    }
                    else{
                        sizeFile--;
                    }
                }
                device.write(sector, beginDataSector+j+index*fatAccess.getSizeCluster());
            }
            nextIndex = fatAccess.read(index);
        } while (nextIndex != 0x0FFFFFFF);
        while (sizeCopy != 0){
            if (fatAccess.totalFreeSpace()>0) {
                int newLastIndex = addCluster(file);
                for (int j = 0; j < fatAccess.getSizeCluster(); j++) {
                    byte[] sector = device.read(beginDataSector + j + newLastIndex * fatAccess.getSizeCluster());
                    for (int i = 0; i < fatAccess.getSizeSector(); i++) {
                        if (sizeCopy == 0) {
                            break;
                        } else {
                            sector[i] = dataByte[dataByte.length-sizeCopy];
                            sizeCopy--;
                        }
                    }
                    device.write(sector, beginDataSector + j + newLastIndex * fatAccess.getSizeCluster());
                }
            }
            else{
                //on actualise la taille
                DataFile fileAct = new DataFile(file.getName(), file.getExtention(), file.getAttribut(), file.getFirstClusterIndex(), file.getSize()+data.length()-sizeCopy, file.getParentFile());
                actualiseFile(fileAct);
                return false; //pas asser d'espace sur le disque
            }
        }
        //on actualise la taille
        DataFile fileAct = new DataFile(file.getName(), file.getExtention(), file.getAttribut(), file.getFirstClusterIndex(), file.getSize()+dataByte.length-sizeCopy, file.getParentFile());
        actualiseFile(fileAct);
        //on actualise la taille

        return true;
    }

    /**
     *
     * @param file dans lequel on écrit
     * @param dataByte tableau à écrire
     * @return number of bytes effectively written
     */
    public int writeAppendFileByte(DataFile file, byte[] dataByte){
        //TODO on fait quoi si y'a pas asser d'espace sur le dique ? on copy la parti copiable ou on copy rien du tout
        int index = file.getFirstClusterIndex();
        int nextIndex = index;
        int sizeCopy = dataByte.length;
        int sizeFile = file.getSize();
        do { // on itère sur la liste chainée
            if (sizeCopy == 0){
                break;
            }
            index = nextIndex;
            for (int j = 0; j<fatAccess.getSizeCluster(); j++){ // on itère sur les cluster
                if (sizeCopy == 0){
                    break;
                }
                byte[] sector = device.read(beginDataSector+j+index*fatAccess.getSizeCluster());
                for (int i = 0; i<fatAccess.getSizeSector(); i++){ //on itère dans le secteur
                    if (sizeCopy == 0){
                        break;
                    }
                    else if (sizeFile <= 0){
                        sector[i] = dataByte[dataByte.length-sizeCopy];
                        sizeCopy--;
                    }
                    else{
                        sizeFile--;
                    }
                }
                device.write(sector, beginDataSector+j+index*fatAccess.getSizeCluster());
            }
            nextIndex = fatAccess.read(index);
        } while (nextIndex != 0x0FFFFFFF);
        while (sizeCopy != 0){
            if (fatAccess.totalFreeSpace()>0) {
                int newLastIndex = addCluster(file);
                for (int j = 0; j < fatAccess.getSizeCluster(); j++) {
                    byte[] sector = device.read(beginDataSector + j + newLastIndex * fatAccess.getSizeCluster());
                    for (int i = 0; i < fatAccess.getSizeSector(); i++) {
                        if (sizeCopy == 0) {
                            break;
                        } else {
                            sector[i] = dataByte[dataByte.length-sizeCopy];
                            sizeCopy--;
                        }
                    }
                    device.write(sector, beginDataSector + j + newLastIndex * fatAccess.getSizeCluster());
                }
            }
            else{
                //on actualise la taille
                DataFile fileAct = new DataFile(file.getName(), file.getExtention(), file.getAttribut(), file.getFirstClusterIndex(), file.getSize()+dataByte.length-sizeCopy, file.getParentFile());
                actualiseFile(fileAct);
                return dataByte.length - sizeCopy; //pas asser d'espace sur le disque
            }
        }
        //on actualise la taille
        DataFile fileAct = new DataFile(file.getName(), file.getExtention(), file.getAttribut(), file.getFirstClusterIndex(), file.getSize()+dataByte.length-sizeCopy, file.getParentFile());
        actualiseFile(fileAct);
        //on actualise la taille

        return dataByte.length - sizeCopy;
    }

    /**
     *
     * @param file dans lequel on écrit
     * @param data chaine de char à write
     * @return true if no problem (false for a lack of space)
     */
    public boolean writeFile(DataFile file, String data){
        //TODO : importance moyenne : suprimer les cluster désormais vide : je pense qu'on fera une fonction ramasse miette qui liberera les cluster vide et qu'on lancera de temps en temps sur le disque
        //maybe on supprime juste le fichier -> on le recrée et on écrit en  mode append
        removeFile(file);
        DataFile newFile = addFile(file.getParentFile(), file.getName(), file.getExtention(), file.getAttribut());
        return writeAppendFile(newFile, data);
    }

    /**
     *
     * @param file dans lequel on écrit
     * @param dataByte byte à écrire
     * @return the number of bytes effectively written
     */
    public int writeFileByte(DataFile file, byte[] dataByte){
        //TODO : importance moyenne : suprimer les cluster désormais vide : je pense qu'on fera une fonction ramasse miette qui liberera les cluster vide et qu'on lancera de temps en temps sur le disque
        //maybe on supprime juste le fichier -> on le recrée et on écrit en  mode append
        removeFile(file);
        DataFile newFile = addFile(file.getParentFile(), file.getName(), file.getExtention(), file.getAttribut());
        return writeAppendFileByte(newFile, dataByte);
    }


    /**
     * ajoute un cluster à un file lorsqu'il est plein
     * @param file un fichier pleins
     * @return the new last index
     */
    private int addCluster(DataFile file){
        if (fatAccess.totalFreeSpace() == 0){
            return 0; // pas asser d'espace sur le disque
        }
        else{
            int firstFreeCluster = fatAccess.firstFreeCluster();
            //on itère sur la liste chainé de file
            int index = file.getFirstClusterIndex();
            int nextIndex = index;
            do {
                index = nextIndex;
                nextIndex = fatAccess.read(index);
            } while(nextIndex != 0x0FFFFFFF);
            fatAccess.write(firstFreeCluster, index);
            fatAccess.write(0x0FFFFFFF, firstFreeCluster); //on déclare le cluster comme utilisé

            //on vas maintenant mettre le nouveau cluster a 0
            byte[] newFileSector = new byte[fatAccess.getSizeSector()];
            device.write(newFileSector, beginDataSector+firstFreeCluster*fatAccess.getSizeCluster());
            device.write(newFileSector, beginDataSector+1+firstFreeCluster*fatAccess.getSizeCluster());

            return firstFreeCluster;
        }
    }

    /**
     * convert into integer a sub array of a sector
     * @param sector the sector we crurently read
     * @param index the index of information
     * @param size the size of the information
     * @return the decimal value
     */
    private int readBytes(byte[] sector, int index, int size){
        int res = 0;
        for (int i = 0; i < size; i++) {
            res |= (sector[i + index] & 0xFF) << ((size - 1 - i) * 8);
        }
        return res;
    }

    /**
     * écrit sur un sector a l'index index et sur une taille size un data entière (adresse d'un élément ou autre)
     * @param sector sector sur lequel on écrit
     * @param index index dans le sector
     * @param size size de la data longeur sur laquel on écrit
     * @param data data à écrire
     */
    private void writeBytes(byte[] sector, int index, int size, int data){
        for (int i=0; i<size; i++){
            sector[size+index-i-1] = (byte) ((data >> 8*i) & 0xFF);
        }
    }

    /**
     * pour l'instant n'est utilisé que pour actualiser la taille du fichier
     * @param file a actualiser
     */
    private void actualiseFile(DataFile file){
        DataFile directory = file.getParentFile();
        int index = directory.getFirstClusterIndex();
        int nextIndex = index;
        boolean findFile = false;
        do { // on itère sur la liste chainé
            index = nextIndex;
            if (findFile){
                break;
            }
            for (int j = 0; j<fatAccess.getSizeCluster(); j++){
                if (findFile){
                    break;
                }
                byte[] sector = device.read(beginDataSector+j+index*fatAccess.getSizeCluster());
                for (int i = 0; i<fatAccess.getSizeSector(); i+=32){
                    byte[] byteName = new byte[8]; //créer pour le constructeur String
                    for (int k = 0; k < 8; k++) {
                        byteName[k] = sector[i + k];
                    }
                    String name = new String(byteName);
                    byte[] byteExtention = new byte[3];
                    for (int k = 8; k<11; k++){
                        byteExtention[k-8] = sector[i + k];
                    }
                    String extention = new String(byteExtention);
                    if (name.equals(file.getName()) && extention.equals(file.getExtention())) {
                        writeBytes(sector, i+28, 4, file.getSize());
                        break;
                    }
                }
                device.write(sector, beginDataSector+j+index*fatAccess.getSizeCluster());
            }
            nextIndex = fatAccess.read(index);
        } while (nextIndex != 0x0FFFFFFF);
    }

    public int getBeginDataSector() {
        return beginDataSector;
    }
}