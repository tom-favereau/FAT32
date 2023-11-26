package shell;

import drives.Device;
import fatfs.FatAccess;
import fatfs.FileStream;
import fatfs.FileSystem;
import fs.DataAccess;
import fs.DataFile;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import java.io.FileNotFoundException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Shell {
    private DataAccess dataAccess;
    private boolean continuer;
    private Scanner scan;
    private DataFile curentDirectory;
    private String path;
    private String deviceName;
    private FileSystem fileSystem;

    // Séquence d'échappement ANSI pour la couleur rouge
    private String redColor = "\u001B[31m";

    // Séquence d'échappement ANSI pour réinitialiser la couleur à la normale
    private String resetColor = "\u001B[0m";

    // Vous pouvez également combiner plusieurs séquences d'échappement pour différentes couleurs et styles
    private String yellowBold = "\u001B[33;1m";
    // Séquence d'échappement ANSI pour la couleur verte
    private String greenColor = "\u001B[32m";
    // Séquence d'échappement ANSI pour la couleur bleue
    private String blueColor = "\u001B[34m";



    public static void main(String[] args) throws FileNotFoundException {
        Shell shell = new Shell();
    }

    public Shell() throws FileNotFoundException {
        Device device = new Device();
        //device.mount("/Users/tom/Programation/dep_info/software_engineering/fat32-favereau-suspene-shao/data/mesTests/SSD_0_CreateFiles_2.data");
        device.mount("data/mesTests/SSD_0_CreateFiles_2.data");
        //device.mount("data/mesTests/SSD_1_SmallFiles_2.data");
        fileSystem = new FileSystem();
        fileSystem.format(device, 2);
        this.deviceName = "SSD_0_CreateFiles_2";
        FatAccess fatAccess = new FatAccess(device);
        this.dataAccess = new DataAccess(device, fatAccess);
        fileSystem.mount(device);

        shellLoop();
    }

    public void shellLoop(){


        System.out.println(greenColor + "welcome to our FAT32 shell ! " + resetColor);

        continuer = true;
        scan = new Scanner(System.in);

        // Définir le motif de la commande echo
        String pattern = "^echo\\s+\"([^\"]+)\"\\s+(>|>>)\\s+(.+)$";
        // Créer le motif à partir de la chaîne
        Pattern regex = Pattern.compile(pattern);
        while (continuer) {
            System.out.print(deviceName + " " + fileSystem.getCurrentDirectory().toString() + "$ ");
            String command = scan.nextLine();

            // Créer un objet Matcher
            Matcher matcher = regex.matcher(command);

            if (command.equals("exit")) {
                continuer = false;
            } else if (command.startsWith("touch")) {
                String[] parts = command.split("\\s+", 2);
                if (parts.length > 1) {
                    String fileName = parts[1];
                    String[] nameAndExtension = splitFileNameAndExtension(fileName);
                    String name = nameAndExtension[0];
                    String extention = nameAndExtension[1];
                    FileStream fileStream = (FileStream) fileSystem.openFile(fileName, 'w');
                } else {
                    System.out.println("Missing file name for touch command");
                }
            } else if (command.startsWith("mkdir")) {
                String[] parts = command.split("\\s+", 2);
                if (parts.length > 1) {
                    String fileName = parts[1];
                    fileSystem.makeDirectory(fileName);
                } else {
                    System.out.println("Missing directory name for mkdir command");
                }
            } else if (matcher.matches()) {

                // Extraire les parties de la commande
                String content = matcher.group(1);
                String operator = matcher.group(2);
                String fileName = matcher.group(3);

                String[] nameAndExtension = splitFileNameAndExtension(fileName);
                String name = nameAndExtension[0];
                String extention = nameAndExtension[1];

                if (operator.equals(">")){
                    FileStream fileStream = (FileStream) fileSystem.openFile(fileName, 'w');
                    try {
                        fileStream.write(content.getBytes());
                    }
                    catch (Exception e){
                        System.out.println("erreur lors de l'écriture");
                    }
                }
                else if (operator.equals(">>")){
                    FileStream fileStream = (FileStream) fileSystem.openFile(fileName, 'a');
                    try {
                        fileStream.write(content.getBytes());
                    }
                    catch (Exception e){
                        System.out.println("erreur lors de l'écriture");
                    }
                }



            }

            else if (command.equals("ls")){
                String fileName = fileSystem.getCurrentDirectory().toString();
                Vector<DataFile> subFile = fileSystem.listSubFile(fileName);
                for (DataFile dataFile : subFile) {
                    if (!dataFile.getAttribut()[1] && dataFile.getAttribut()[2]){ //système et non caché
                        System.out.println(yellowBold + dataFile.getName() + " " + dataFile.getExtention() + " " + dataFile.getSize() + resetColor);
                    }
                    else if (!dataFile.getAttribut()[1] && dataFile.getAttribut()[4]){ //dossier et non caché
                        System.out.println(blueColor + dataFile.getName() + " " + dataFile.getExtention() + " " + dataFile.getSize() + resetColor);
                    }
                    else if (!dataFile.getAttribut()[1]){ //fichier non caché
                        System.out.println(dataFile.getName() + " " + dataFile.getExtention() + " " + dataFile.getSize());
                    }
                }
            }

            else if (command.startsWith("ls")) {
                // Code à exécuter si la commande est "ls"
                String[] parts = command.split("\\s+", 2);
                String fileName = parts[1];
                Vector<DataFile> subFile = fileSystem.listSubFile(fileName);
                for (DataFile dataFile : subFile) {
                    if (dataFile.getAttribut()[2]){
                        System.out.println(yellowBold + dataFile.getName() + " " + dataFile.getExtention() + " " + dataFile.getSize() + resetColor);
                    }
                    else if (dataFile.getAttribut()[4]){
                        System.out.println(blueColor + dataFile.getName() + " " + dataFile.getExtention() + " " + dataFile.getSize() + resetColor);
                    }
                    else {
                        System.out.println(dataFile.getName() + " " + dataFile.getExtention() + " " + dataFile.getSize());
                    }
                }

            }



            else if (command.startsWith("less")) {
                // Code à exécuter si la commande commence par "less"
                String[] parts = command.split("\\s+", 2);
                if (parts.length > 1) {
                    String fileName = parts[1];
                    FileStream fileStream = (FileStream) fileSystem.openFile(fileName, 'r');
                    byte[] output = new byte[10000];
                    try {
                        fileStream.read(output);
                        System.out.println(convertirBytesEnChaine(output));
                    }
                    catch (Exception e){
                        System.out.println("erreur lors de la lecture");
                    }


                } else {
                    System.out.println("Missing file name for cat command");
                }

            }

            else if (command.equals("cd")){
                String fileName = "/";
                try {
                    fileSystem.setWorkingDirectory(fileName);
                }
                catch (Exception e){
                    System.out.println("erreur lors du changement de répertoire");
                }
            }

            else if (command.startsWith("cd")){
                String[] parts = command.split("\\s+", 2);
                if (parts.length > 1) {
                    String fileName = parts[1];
                    try {
                        fileSystem.setWorkingDirectory(fileName);
                    }
                    catch (Exception e){
                        System.out.println("erreur lors du changement de répertoire");
                    }
                } else {
                    System.out.println("Missing directory name for cd command");
                }
            }
            else if (command.startsWith("rm")){
                String[] parts = command.split("\\s+", 2);
                if (parts.length > 1) {
                    String fileName = parts[1];
                    boolean remove = fileSystem.removeFile(fileName);
                    if (!remove){
                        System.out.println("le fichier n'as pas pu être suprimé");
                    }
                } else {
                    System.out.println("Missing file name for rm command");
                }
            }

            else if (command.equals("pwd")){
                System.out.println(fileSystem.getCurrentDirectory().toString());
            }

            else {
                System.out.println("Unknown command: " + command);
            }
        }
    }



    public void cd(String fileName){
        Vector<DataFile> subFile = dataAccess.readSubFile(curentDirectory);
        for (int i = 0; i<subFile.size(); i++){
            if (subFile.get(i).getName().startsWith(fileName) && subFile.get(i).getAttribut()[4]){
                curentDirectory = subFile.get(i);
                if (fileName.startsWith("..")){
                    String newPath = obtenirCheminParent(path);
                    path = newPath;
                    return;
                }
                else if (fileName.startsWith(".")){
                    return;
                }
                else {
                    if (path.equals("/")){
                        path = path + fileName;
                    }
                    else{
                        path = path + "/" + fileName;
                    }
                    return;
                }

            }
        }
    }

    public void ls(){
        Vector<DataFile> subFile = dataAccess.readSubFile(curentDirectory);
        for (int i = 0; i<subFile.size(); i++){
            if (!subFile.get(i).getAttribut()[1]) {
                if (subFile.get(i).getAttribut()[2]){
                    System.out.println(yellowBold + subFile.get(i).getName() + " " + subFile.get(i).getExtention() + " " + subFile.get(i).getSize() + resetColor);
                }
                else if (subFile.get(i).getAttribut()[4]) {
                    System.out.println(blueColor + subFile.get(i).getName() + " " + subFile.get(i).getExtention() + " " + subFile.get(i).getSize() + resetColor);
                }
                else{
                    System.out.println(subFile.get(i).getName() + " " + subFile.get(i).getExtention() + " " + subFile.get(i).getSize());
                }
            }
        }
    }

    public void rm(String fileName, String extention){
        Vector<DataFile> subFile = dataAccess.readSubFile(curentDirectory);
        for (int i = 0; i<subFile.size(); i++){
            if (subFile.get(i).getName().startsWith(fileName) && !subFile.get(i).getAttribut()[4]){
                dataAccess.removeFile(subFile.get(i));
                return;
            }
        }
    }

    public void cat(String fileName){
        Vector<DataFile> subFile = dataAccess.readSubFile(curentDirectory);
        for (int i = 0; i<subFile.size(); i++){
            if (subFile.get(i).getName().startsWith(fileName)){
                String content = dataAccess.readFileContent(subFile.get(i));
                System.out.printf(content);
                break;
            }
        }
    }

    public void mkdir(String fileName){
        boolean[] attribut = new boolean[8];
        attribut[0] = false; attribut[1] = false; attribut[2] = false; attribut[4] = true;
        dataAccess.addFile(curentDirectory, fileName, "", attribut);
    }

    public void touch(String fileName, String extention){
        boolean[] attribut = new boolean[8];
        attribut[0] = false; attribut[1] = false; attribut[2] = false; attribut[4] = false;
        dataAccess.addFile(curentDirectory, fileName, extention, attribut);
    }

    public void echoAppend(String fileName, String extention, String content){
        Vector<DataFile> subFile = dataAccess.readSubFile(curentDirectory);
        for (int i = 0; i<subFile.size(); i++){
            if (subFile.get(i).getName().startsWith(fileName)){
                dataAccess.writeAppendFile(subFile.get(i), content+"\n");
                return;
            }
        }
        touch(fileName, extention);
        echoAppend(fileName, extention, content);
    }

    public void echo(String fileName, String extention, String content){
        Vector<DataFile> subFile = dataAccess.readSubFile(curentDirectory);
        for (int i = 0; i<subFile.size(); i++){
            if (subFile.get(i).getName().startsWith(fileName)){
                dataAccess.writeFile(subFile.get(i), content+"\n");
                return;
            }
        }
        touch(fileName, extention);
        echo(fileName, extention, content);
    }

    public Vector<DataFile> getSubFiles(){
        return dataAccess.readSubFile(curentDirectory);
    }

    private static String[] splitFileNameAndExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        String[] result = new String[2];

        if (dotIndex != -1) {
            result[0] = fileName.substring(0, dotIndex);
            result[1] = fileName.substring(dotIndex + 1);
        } else {
            result[0] = fileName;
            result[1] = ""; // Aucune extension trouvée
        }

        return result;
    }

    private String obtenirCheminParent(String chemin) {
        // Utiliser la classe Path pour manipuler le chemin
        Path path = Paths.get(chemin);

        // Extraire la partie souhaitée du chemin
        Path cheminParent = path.getParent();

        // Convertir le chemin parent en une chaîne
        if (cheminParent != null) {
            return cheminParent.toString();
        } else {
            // Gérer le cas où le chemin n'a pas de parent
            return "Pas de chemin parent";
        }
    }

    private String convertirBytesEnChaine(byte[] tableauBytes) {
        // Trouver l'indice du premier caractère nul (null byte)
        int indexNull = -1;
        for (int i = 0; i < tableauBytes.length; i++) {
            if (tableauBytes[i] == 0) {
                indexNull = i;
                break;
            }
        }

        // Utiliser la classe String pour convertir les bytes en chaîne jusqu'à l'indice du premier null
        String chaine = (indexNull != -1) ? new String(tableauBytes, 0, indexNull) : new String(tableauBytes);

        return chaine;
    }


}