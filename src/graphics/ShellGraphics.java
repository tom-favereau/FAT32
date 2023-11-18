package graphics;

import drives.Device;
import fatfs.FatAccess;
import fs.DataAccess;
import fs.DataFile;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Vector;
import java.util.regex.Matcher;

import javafx.concurrent.Task;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ShellGraphics{
    private TextArea terminal;
    private TextField inputField;
    private DataAccess dataAccess;
    private DataFile curentDirectory;
    private Pattern regex; //pour la commande echo
    private String path;
    private Stage stage;

    // Séquence d'échappement ANSI pour la couleur rouge
    private final String redColor = "\u001B[31m";

    // Séquence d'échappement ANSI pour réinitialiser la couleur à la normale
    private final String resetColor = "\u001B[0m";

    // Vous pouvez également combiner plusieurs séquences d'échappement pour différentes couleurs et styles
    private final String yellowBold = "\u001B[33;1m";
    // Séquence d'échappement ANSI pour la couleur verte
    private final String greenColor = "\u001B[32m";
    // Séquence d'échappement ANSI pour la couleur bleue
    private final String blueColor = "\u001B[34m";

    public void start(Stage stage, DataAccess dataAccess, DataFile curentDirectory, String path) {
        // Créer une TextArea pour le terminal
        this.dataAccess = dataAccess;
        this.curentDirectory = curentDirectory;
        this.stage = stage;
        this.path = path;
        // Définir le motif de la commande echo
        String pattern = "^echo\\s+\"([^\"]+)\"\\s+(>>|>)\\s+([a-zA-Z0-9_]+\\.[a-zA-Z0-9_]+)$";
        // Créer le motif à partir de la chaîne
        regex = Pattern.compile(pattern);


        terminal = new TextArea();
        terminal.setEditable(false);
        terminal.setPrefSize(400, 300);
        // Définir la couleur de fond du de la zonne de texte
        terminal.setStyle("-fx-control-inner-background: black;");

        // Créer un champ de texte pour la saisie de l'utilisateur
        inputField = new TextField();
        inputField.setOnAction(event -> handleCommand());
        inputField.setStyle("-fx-control-inner-background: #444444;");

        // Créer une boîte verticale pour contenir la TextArea et le TextField
        VBox root = new VBox(terminal, inputField);

        // Créer une scène
        Scene scene = new Scene(root, 400, 300);

        // Configurer la scène et afficher la fenêtre
        this.stage.setScene(scene);
        this.stage.setTitle(path);
        this.stage.show();

        // Exemple : Ajouter des messages au terminal
        appendMessage("Welcome to our fat32 shell");
    }

    // Méthode pour ajouter un message au terminal
    private void appendMessage(String message) {
        terminal.appendText(message + "\n");
    }

    // Méthode appelée lorsque l'utilisateur appuie sur Entrée dans le champ de texte
    private void handleCommand() {
        String command = inputField.getText();
        //appendMessage("Commande saisie : " + command);



        // Créer un objet Matcher
        Matcher matcher = regex.matcher(command);


        if (command.startsWith("touch")) {
            String[] parts = command.split("\\s+", 2);
            if (parts.length > 1) {
                String fileName = parts[1];
                String[] nameAndExtension = splitFileNameAndExtension(fileName);
                String name = nameAndExtension[0];
                String extention = nameAndExtension[1];
                touch(name, extention);
            } else {
                appendMessage("Missing file name for touch command");
            }
        } else if (command.startsWith("mkdir")) {
            String[] parts = command.split("\\s+", 2);
            if (parts.length > 1) {
                String fileName = parts[1];
                mkdir(fileName);
            } else {
                appendMessage("Missing directory name for mkdir command");
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
                echo(name, extention, content);
            }
            else if (operator.equals(">>")){
                echoAppend(name, extention, content);
            }



        } else if (command.equals("ls")) {
            // Code à exécuter si la commande est "ls"
            ls();
        } else if (command.startsWith("cat")) {
            // Code à exécuter si la commande commence par "cat"
            String[] parts = command.split("\\s+", 2);
            if (parts.length > 1) {
                String fileName = parts[1];
                String[] nameAndExtension = splitFileNameAndExtension(fileName);
                String name = nameAndExtension[0];
                String extention = nameAndExtension[1];
                cat(name);
            } else {
                appendMessage("Missing file name for cat command");
            }

        }
        else if (command.startsWith("cd")){
            String[] parts = command.split("\\s+", 2);
            if (parts.length > 1) {
                String fileName = parts[1];
                cd(fileName);
            } else {
                appendMessage("Missing directory name for cd command");
            }
        }
        else if (command.startsWith("rm")){
            String[] parts = command.split("\\s+", 2);
            if (parts.length > 1) {
                String fileName = parts[1];
                String[] nameAndExtension = splitFileNameAndExtension(fileName);
                String name = nameAndExtension[0];
                String extention = nameAndExtension[1];
                rm(name, extention);
            } else {
                appendMessage("Missing file name for rm command");
            }
        }
        else {
            appendMessage("Unknown command: " + command);
        }


        inputField.clear();
    }



    public void cd(String fileName){
        Vector<DataFile> subFile = dataAccess.readSubFile(curentDirectory);
        for (int i = 0; i<subFile.size(); i++){
            if (subFile.get(i).getName().startsWith(fileName) && subFile.get(i).getAttribut()[4]){
                curentDirectory = subFile.get(i);
                if (fileName.startsWith("..")){
                    String newPath = obtenirCheminParent(path);
                    path = newPath;
                    stage.setTitle(path);
                    return;
                }
                else if (fileName.startsWith(".")){
                    return;
                }
                else {
                    if (path.equals("/")){
                        path = path + fileName;
                        stage.setTitle(path);
                    }
                    else{
                        path = path + "/" + fileName;
                        stage.setTitle(path);
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
                    appendMessage(subFile.get(i).getName() + " " + subFile.get(i).getExtention() + " " + subFile.get(i).getSize());
                }
                else if (subFile.get(i).getAttribut()[4]) {
                    appendMessage(subFile.get(i).getName() + " " + subFile.get(i).getExtention() + " " + subFile.get(i).getSize());
                }
                else{
                    appendMessage(subFile.get(i).getName() + " " + subFile.get(i).getExtention() + " " + subFile.get(i).getSize());
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
                appendMessage(content);
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



}