package graphics;

import fatfs.FatAccess;
import fs.DataAccess;
import fs.IDevice;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javax.swing.plaf.basic.BasicButtonUI;
import java.util.Vector;

public class DiskTool {

    private Stage stage;
    private DataAccess dataAccess;
    private FatAccess fatAccess;
    private GridPane layout;

    public void start(Stage stage, DataAccess dataAccess, FatAccess fatAccess){
        this.stage = stage;
        this.dataAccess = dataAccess;
        this.fatAccess = fatAccess;

        layout = new GridPane();
        layout.setPadding(new Insets(15, 15, 15, 15));
        layout.setVgap(2);
        layout.setHgap(2);

        Button displayButton = new Button("Voir la fragmentation");
        displayButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                displayFragmentation();
            }
        });

        Button formatButton = new Button("Formater");
        formatButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                format();
            }
        });

        layout.add(displayButton, 0, 0, 6, 1);
        layout.add(formatButton, 6, 0, 6, 1);



        Scene scene = new Scene(layout, 500, 450);

        stage.setTitle("Disk Tool");
        stage.setScene(scene);

        stage.show();


    }

    private void displayFragmentation(){
        Vector<Integer> visited = new Vector<>();
        int size = fatAccess.getFatNumberOfCase() - fatAccess.totalFreeSpace() - 2; //-2 car les 2 première case ne sont pas occupé
        for (int i = 2; i < fatAccess.getFatNumberOfCase(); i++) {
            if (size == 0){
                break;
            }
            else if (fatAccess.read(i) == 0x00000000){
                // Utilisation de JavaFX pour afficher le cluster
                Color currentColor = Color.WHITE;
                Rectangle clusterRectangle = new Rectangle(30, 20); // Ajustez la taille selon vos besoins
                clusterRectangle.setFill(currentColor);
                layout.add(clusterRectangle, i%15, (i/15)+1); // Ajoutez au GridPane
            }
            else if (!visited.contains(i)) {
                int index = i;
                int nextIndex = i;

                Color currentColor = getRandomColor();

                do {
                    size--;
                    index = nextIndex;
                    visited.add(index);

                    // Utilisation de JavaFX pour afficher le cluster

                    Rectangle clusterRectangle = new Rectangle(30, 20);
                    clusterRectangle.setFill(currentColor);

                    Text text = new Text(String.valueOf(i)); // Ajoutez le texte avec la valeur de i
                    StackPane stackPane = new StackPane(clusterRectangle, text);

                    layout.add(stackPane, index%15, (index/15)+1); // Ajoutez au GridPane

                    nextIndex = fatAccess.read(index);
                } while (nextIndex != 0x0FFFFFFF);

            }
        }
    }

    private void format(){
        System.out.println("il faut formater le disque");
    }

    private Color getRandomColor() {
        double red = Math.random();
        double green = Math.random();
        double blue = Math.random();
        return new Color(red, green, blue, 1.0);
    }

}