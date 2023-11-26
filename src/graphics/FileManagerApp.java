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
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;

import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Vector;
import javafx.concurrent.Task;


public class FileManagerApp extends Application{

    private DataFile curentDirectory;
    private DataAccess dataAccess;
    private FatAccess fatAccess;
    private Scene scene;
    private GridPane layout;
    private String selectedFile;
    private Text selectedButtonText;
    private static String arguments[];
    private String path;
    private Stage Mystage;

    public static void startApp(String args[]) throws FileNotFoundException {
        arguments = args;
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        //initialisation
        Device device = new Device();
        device.mount("data/testAlicia/myDevice.data");
        fatAccess = new FatAccess(device);
        dataAccess = new DataAccess(device, fatAccess);
        boolean[] rootAttribut = new boolean[8];
        rootAttribut[0] = false; rootAttribut[1] = false; rootAttribut[2] = true; rootAttribut[4] = true;
        this.curentDirectory = new DataFile("/", "", rootAttribut, fatAccess.getRootIndex(), 0, null, 0, 0, 0);

        path = "/";
        this.Mystage = stage;
        stage.setTitle("File Manager: " + path);



        layout = new GridPane();
        layout.setPadding(new Insets(10, 10, 10, 10));
        layout.setVgap(30);
        layout.setHgap(30);

        // Définir la couleur de fond du GridPane
        BackgroundFill backgroundFill = new BackgroundFill(Color.GRAY, null, null);
        Background background = new Background(backgroundFill);
        layout.setBackground(background);

        //on affiche les icone et les application
        displayIconFile();
        displayToolBar();

        // Créer un menu contextuel dans le cas du clic droit
        ContextMenu contextMenu = new ContextMenu();
        MenuItem newDir = new MenuItem("Nouveau Dossier");
        newDir.setOnAction(e -> mkdir());
        MenuItem newFile = new MenuItem("Nouveau Fichier");
        newFile.setOnAction(e -> touch());
        MenuItem delete = new MenuItem("Supprimer");
        delete.setOnAction(e -> rm());
        contextMenu.getItems().addAll(newDir, newFile, delete);


        // Associer le menu contextuel à layout en utilisant le gestionnaire d'événements
        layout.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(layout, event.getScreenX(), event.getScreenY());
            }
            else if (event.getButton() == MouseButton.PRIMARY  && !(event.getTarget() instanceof Button)){
                //on cache le menu
                contextMenu.hide();
                //on deselection le fichier ou l'aplication actuelle
                if (selectedButtonText != null) {
                    selectedButtonText.setFill(Color.valueOf("#020000"));
                    selectedButtonText = null;
                    selectedFile = null;
                }

            }
        });

        scene = new Scene(layout, 710, 400);
        scene.setFill(Color.GREY);
        stage.setScene(scene);
        stage.show();

    }

    // Méthode pour créer une icône de fichier
    private void createDirectoryIcon(String name, int x, int y) {
        Image fileImage = new Image("file:ressources/folder.png");
        ImageView imageView = new ImageView(fileImage);
        imageView.setFitWidth(50);
        imageView.setFitHeight(50);

        Text nameFile = new Text(name);

        VBox box = new VBox();
        box.getChildren().addAll(imageView, nameFile);
        box.setAlignment(Pos.CENTER);
        //couleur de la box
        box.setBackground(new Background(new BackgroundFill(Color.GRAY, null, null)));


        Button button = new Button();
        button.setGraphic(box);
        // Définir la couleur de fond du bouton
        BackgroundFill backgroundFill = new BackgroundFill(Color.GRAY, null, null);
        Background background = new Background(backgroundFill);
        button.setBackground(background);
        //utton.setStyle("-fx-background-color: #2730c4; -fx-text-fill: white;");

        //methode cd
        button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {

                if (name.equals(selectedFile)) {
                    changeDirectory(name);
                }
                else{
                    selectedFile = name;
                    if (selectedButtonText == null){
                        selectedButtonText = nameFile;
                        selectedButtonText.setFill(Color.valueOf("#2730c4"));
                    }
                    else {
                        selectedButtonText.setFill(Color.valueOf("#020000"));
                        selectedButtonText = nameFile;
                        selectedButtonText.setFill(Color.valueOf("#2730c4"));
                    }
                }
            }
        });

        GridPane.setConstraints(button, x, y);
        layout.getChildren().add(button);

    }

    private void createFileIcon(String name, int x, int y){
        Image fileImage = new Image("file:ressources/directory.png");
        ImageView imageView = new ImageView(fileImage);
        imageView.setFitWidth(50);
        imageView.setFitHeight(50);

        Text nameFile = new Text(name);

        VBox box = new VBox();
        box.getChildren().addAll(imageView, nameFile);
        box.setAlignment(Pos.CENTER);
        //couleur de la box
        //box.setBackground(new Background(new BackgroundFill(Color.GRAY, null, null)));

        Button button = new Button();
        button.setGraphic(box);
        // Définir la couleur de fond du bouton
        BackgroundFill backgroundFill = new BackgroundFill(Color.GRAY, null, null);
        Background background = new Background(backgroundFill);
        button.setBackground(background);

        //methode open
        button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (name.equals(selectedFile)){
                    openTextEditor(name);
                }
                else{
                    selectedFile = name;
                    if (selectedButtonText == null){
                        selectedButtonText = nameFile;
                        selectedButtonText.setFill(Color.valueOf("#2730c4"));
                    }
                    else {
                        selectedButtonText.setFill(Color.valueOf("#020000"));
                        selectedButtonText = nameFile;
                        selectedButtonText.setFill(Color.valueOf("#2730c4"));
                    }
                }
            }
        });

        GridPane.setConstraints(button, x, y);
        layout.getChildren().add(button);
    }

    private void displayToolBar(){
        //on affiche le terminal
        Image terminalImage = new Image("file:ressources/terminal.png");
        ImageView imageViewTerminal = new ImageView(terminalImage);
        imageViewTerminal.setFitWidth(50);
        imageViewTerminal.setFitHeight(50);

        Text nameTerminal = new Text("terminal");

        VBox boxTerminal = new VBox();
        boxTerminal.getChildren().addAll(imageViewTerminal, nameTerminal);
        boxTerminal.setAlignment(Pos.CENTER);

        Button buttonTerminal = new Button();
        buttonTerminal.setGraphic(boxTerminal);
        // Définir la couleur de fond du bouton
        BackgroundFill backgroundFill = new BackgroundFill(Color.GRAY, null, null);
        Background background = new Background(backgroundFill);
        buttonTerminal.setBackground(background);

        GridPane.setConstraints(buttonTerminal, 0, 0);
        layout.getChildren().add(buttonTerminal);

        buttonTerminal.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if ("terminal".equals(selectedFile)){
                    openTerminal();
                }
                else{
                    selectedFile = "terminal";
                    if (selectedButtonText == null){
                        selectedButtonText = nameTerminal;
                        selectedButtonText.setFill(Color.valueOf("#2730c4"));
                    }
                    else {
                        selectedButtonText.setFill(Color.valueOf("#020000"));
                        selectedButtonText = nameTerminal;
                        selectedButtonText.setFill(Color.valueOf("#2730c4"));
                    }
                }
            }
        });


        //on s'occupe du l'utilitaire de disque
        Image diskImage = new Image("file:ressources/hard-disk.png");
        ImageView imageViewDisk = new ImageView(diskImage);
        imageViewDisk.setFitWidth(50);
        imageViewDisk.setFitHeight(50);

        Text nameDisk = new Text("Disk Tool");

        VBox boxDisk = new VBox();
        boxDisk.getChildren().addAll(imageViewDisk, nameDisk);
        boxDisk.setAlignment(Pos.CENTER);

        Button buttonDisk = new Button();
        buttonDisk.setGraphic(boxDisk);
        // Définir la couleur de fond du bouton
        buttonDisk.setBackground(background);

        GridPane.setConstraints(buttonDisk, 0, 1);
        layout.getChildren().add(buttonDisk);

        buttonDisk.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if ("Disk Tool".equals(selectedFile)){
                    openDiskTool();
                }
                else{
                    selectedFile = "Disk Tool";
                    if (selectedButtonText == null){
                        selectedButtonText = nameDisk;
                        selectedButtonText.setFill(Color.valueOf("#2730c4"));
                    }
                    else {
                        selectedButtonText.setFill(Color.valueOf("#020000"));
                        selectedButtonText = nameDisk;
                        selectedButtonText.setFill(Color.valueOf("#2730c4"));
                    }
                }
            }
        });


    }

    private void displayIconFile(){
        //on clear la scene
        layout.getChildren().clear();

        //on affiche les fichiers
        Vector<DataFile> subFile = dataAccess.readSubFile(curentDirectory);

        int nbFile = 0;
        for (int i = 0; i<6; i++){
            if (nbFile == subFile.size()){
                break;
            }
            for (int j = 2; j<8; j++){
                if (nbFile == subFile.size()){
                    break;
                }
                if (subFile.get(nbFile).getAttribut()[4] && !subFile.get(nbFile).getAttribut()[1]) {
                    String nameAndExtention;
                    int indexOfSpace = subFile.get(nbFile).getName().indexOf(" ");
                    if (indexOfSpace != 1){
                        nameAndExtention = subFile.get(nbFile).getName().substring(0, indexOfSpace);// retire les espace vide pour l'affichage
                    }
                    else {
                        nameAndExtention = subFile.get(nbFile).getName();
                    }
                    createDirectoryIcon(nameAndExtention, j, i);
                }
                else if (!subFile.get(nbFile).getAttribut()[4] && !subFile.get(nbFile).getAttribut()[1]){
                    String nameAndExtention;
                    int indexOfSpace = subFile.get(nbFile).getName().indexOf(" ");
                    if (indexOfSpace != -1){
                        nameAndExtention = subFile.get(nbFile).getName().substring(0, indexOfSpace) + "." + subFile.get(nbFile).getExtention();
                    }
                    else{
                        nameAndExtention = subFile.get(nbFile).getName() + "." + subFile.get(nbFile).getExtention();
                    }
                    createFileIcon(nameAndExtention, j, i);
                }
                nbFile++;
            }
        }

    }

    private void changeDirectory(String name){
        Vector<DataFile> subFile = dataAccess.readSubFile(curentDirectory);
        for (int i = 0; i< subFile.size(); i++){
            if (subFile.get(i).getName().startsWith(name) && subFile.get(i).getAttribut()[4]){
                curentDirectory = subFile.get(i);
                displayIconFile();
                displayToolBar();
                if (name.startsWith("..")){
                    String newPath = obtenirCheminParent(path);
                    path = newPath;
                    Mystage.setTitle(path);
                    return;
                }
                else if (name.startsWith(".")){
                    return;
                }
                else {
                    if (path.equals("/")){
                        path = path + name;
                        Mystage.setTitle(path);
                    }
                    else{
                        path = path + "/" + name;
                        Mystage.setTitle(path);
                    }
                    return;
                }
            }
        }
    }



    private void openTextEditor(String name) {
        Stage nouvelleFenetre = new Stage();
        nouvelleFenetre.setTitle(name);

        // Créer un champ de texte
        TextArea textArea = new TextArea();
        textArea.setPrefSize(600, 400);
        // Définir la couleur de fond du de la zonne de texte
        textArea.setStyle("-fx-control-inner-background: grey;");


        //on récupère le contenus du fichier
        String[] namePlusExtention = name.split("\\.+");
        Vector<DataFile> subFile = dataAccess.readSubFile(curentDirectory);
        DataFile openFile = new DataFile("name", "ext", new boolean[8], 2, 0, null, 0, 0, 0); //c'est juste pour l'initialiser sinon il m'embête
        for (int i = 0; i<subFile.size(); i++){
            if (subFile.get(i).getName().startsWith(namePlusExtention[0]) && subFile.get(i).getExtention().startsWith(namePlusExtention[1])){
                String content = dataAccess.readFileContent(subFile.get(i));
                textArea.setText(content);
                openFile = subFile.get(i);
            }
        }

        //command save
        Button buttonSave = new Button();
        Image saveImage = new Image("file:ressources/floppy-disk.png");
        ImageView imageView = new ImageView(saveImage);
        imageView.setFitHeight(20);
        imageView.setFitWidth(20);
        buttonSave.setGraphic(imageView);
        DataFile finalOpenFile = openFile;
        buttonSave.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                // Changement de style au début du traitement
                buttonSave.setStyle("-fx-background-color: #2730c4;");

                Task<Void> saveTask = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        // Traitement long (saveFile) à effectuer dans un thread séparé
                        saveFile(finalOpenFile, textArea);
                        Thread.sleep(200);
                        return null;
                    }
                };

                // Définir ce qui doit être fait lorsque le traitement est terminé
                saveTask.setOnSucceeded(event -> {
                    // Changement de style à la fin du traitement
                    buttonSave.setStyle("-fx-background-color: grey;");
                });

                // Lancer le thread
                new Thread(saveTask).start();
            }
        });
        // Définir la couleur de fond du bouton
        buttonSave.setStyle("-fx-background-color: grey;");

        // Créer une disposition VBox pour organiser les éléments verticalement
        VBox vbox = new VBox();
        vbox.getChildren().addAll(buttonSave, textArea);
        vbox.setBackground(new Background(new BackgroundFill(Color.GRAY, null, null)));

        Scene newScene = new Scene(vbox, 500, 350);

        nouvelleFenetre.setScene(newScene);
        nouvelleFenetre.show();
    }

    private void saveFile(DataFile file, TextArea text){
        dataAccess.writeFile(file, text.getText());
    }


    private void openTerminal(){
       ShellGraphics shell = new ShellGraphics();
       Stage newStage = new Stage();

       shell.start(newStage, dataAccess, curentDirectory, path);

    }

    private void openDiskTool(){
        DiskTool diskTool = new DiskTool();
        Stage newStage = new Stage();

        diskTool.start(newStage, dataAccess, fatAccess);
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

    private void mkdir(){
        Stage myStage = new Stage();

        // Création des éléments de l'interface
        TextField nomFichierTextField = new TextField();
        Button validerButton = new Button("Valider");

        // Configuration du bouton pour afficher le nom du fichier dans la console
        validerButton.setOnAction(e -> {
            String nomFichier = nomFichierTextField.getText();
            System.out.println("Nom du dossier saisi : " + nomFichier);
            // Vous pouvez ajouter ici la logique pour créer le fichier ou effectuer d'autres actions.
        });

        // Création de la mise en page
        VBox vbox = new VBox(10); // Espacement vertical de 10 pixels entre les éléments
        vbox.getChildren().addAll(nomFichierTextField, validerButton);

        // Création de la scène
        Scene scene = new Scene(vbox, 300, 150);

        // Configuration de la fenêtre
        myStage.setScene(scene);
        myStage.setTitle("Nouveau Dossier");
        myStage.show();
    }



    private void touch(){
        Stage myStage = new Stage();

        // Création des éléments de l'interface
        TextField nomFichierTextField = new TextField();
        Button validerButton = new Button("Valider");

        // Configuration du bouton pour afficher le nom du fichier dans la console
        validerButton.setOnAction(e -> {
            String nomFichier = nomFichierTextField.getText();
            System.out.println("Nom du fichier saisi : " + nomFichier);
            // Vous pouvez ajouter ici la logique pour créer le fichier ou effectuer d'autres actions.
        });

        // Création de la mise en page
        VBox vbox = new VBox(10); // Espacement vertical de 10 pixels entre les éléments
        vbox.getChildren().addAll(nomFichierTextField, validerButton);

        // Création de la scène
        Scene scene = new Scene(vbox, 300, 150);

        // Configuration de la fenêtre
        myStage.setScene(scene);
        myStage.setTitle("Nouveau Fichier");
        myStage.show();
    }

    private void rm(){

    }

}