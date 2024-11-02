package com.snowreplicator.filecomparator;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class FileComparator {

    public static void init(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("file-comparator-view.fxml"));

        Scene scene = new Scene(fxmlLoader.load());

        FileComparatorController fileComparatorController = fxmlLoader.getController();
        fileComparatorController.setStage(stage);

        stage.setTitle("File comparator");
        stage.setScene(scene);

        stage.show();
    }
}
