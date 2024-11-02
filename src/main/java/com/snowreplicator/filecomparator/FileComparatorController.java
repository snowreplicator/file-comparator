package com.snowreplicator.filecomparator;

import com.snowreplicator.filecomparator.view_model.InputScanParams;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class FileComparatorController implements Initializable {

    public TextField inputDirectoryText;
    public CheckBox inputDirectoryCheckbox;
    public TextField outputDirectoryText;
    public CheckBox outputDirectoryCheckbox;
    public TextArea logTextArea;
    public Button inputDirectoryButton;
    public Button outputDirectoryButton;
    public Button startScanButton;
    public Button stopScanButton;
    public Button exitScanButton;

    private Stage stage;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Future<?> future;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logTextArea.setEditable(false);

        addMessageToLogArea("Log area:");
        addMessageToLogArea("program started");

        Path path = Paths.get(".");
        inputDirectoryText.setText(path.toAbsolutePath().normalize().toString());
        outputDirectoryText.setText(path.toAbsolutePath().normalize().toString());

        inputDirectoryCheckbox.setSelected(true);
        outputDirectoryCheckbox.setSelected(true);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        stage.setOnCloseRequest(this::onCloseRequest);
    }

    private static void shutdownAndAwaitTermination(ExecutorService pool) {
        if (pool == null)
            return;

        pool.shutdown();
        try {
            if (!pool.awaitTermination(1, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void addMessageToLogArea(String message) {
        System.out.println(message);
        String text = logTextArea.getText();
        if (text.lastIndexOf('\n') != 1 && !text.isEmpty())
            logTextArea.appendText("\n");
        logTextArea.appendText(message);
    }

    public void replaceLastMessageInLogArea(String message) {
        String text = logTextArea.getText();
        int lastLineStart = text.lastIndexOf('\n');

        if (text.isEmpty()) {
            addMessageToLogArea(message);
        } else if (lastLineStart != -1) {
            logTextArea.deleteText(lastLineStart, text.length());
            addMessageToLogArea(message);
        }
    }

    private void onCloseRequest(WindowEvent event) {
        shutdownAndAwaitTermination(executorService);
        Platform.runLater(Platform::exit);
    }

    @FXML
    private void onInputDirectoryButtonClick() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File("./"));

        File dirFile = directoryChooser.showDialog(stage);
        if (dirFile != null)
            inputDirectoryText.setText(dirFile.getAbsolutePath());
    }

    @FXML
    private void onOutputDirectoryButtonClick(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setInitialDirectory(new File("./"));

        File dirFile = directoryChooser.showDialog(stage);
        if (dirFile != null)
            outputDirectoryText.setText(dirFile.getAbsolutePath());
    }

    @FXML
    private void onExitButtonClick(ActionEvent actionEvent) {
        shutdownAndAwaitTermination(executorService);
        Platform.runLater(Platform::exit);
    }

    @FXML
    private void onStopButtonClick(ActionEvent actionEvent) {
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
    }

    @FXML
    private void onStartButtonClick(ActionEvent actionEvent) {
        InputScanParams inputScanParams = new InputScanParams(
                Paths.get(inputDirectoryText.getText()),
                Paths.get(outputDirectoryText.getText()),
                inputDirectoryCheckbox.isSelected(),
                outputDirectoryCheckbox.isSelected()
        );

        startFileComparatorTask(inputScanParams);
    }


    private void startFileComparatorTask(InputScanParams inputScanParams) {
        FileComparatorTask fileComparatorTask = new FileComparatorTask(inputScanParams, this);

        fileComparatorTask.exceptionProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> {
                addMessageToLogArea("");
                addMessageToLogArea("Error: ");
                addMessageToLogArea(newValue.toString());
                StackTraceElement[] stacktrace = newValue.getStackTrace();
                for (StackTraceElement stackTraceElement : stacktrace) {
                    addMessageToLogArea(stackTraceElement.toString());
                }

                disableButtonsForStoppedTask();
            });
        });

        fileComparatorTask.messageProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> addMessageToLogArea(newValue));
        });

        fileComparatorTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                addMessageToLogArea("");
                addMessageToLogArea("Compare completed.");
                disableButtonsForStoppedTask();
            });
        });


        fileComparatorTask.setOnCancelled(e -> {
            Platform.runLater(() -> {
                addMessageToLogArea("");
                addMessageToLogArea("Canceled.");
                disableButtonsForStoppedTask();
            });
        });

        future = executorService.submit(fileComparatorTask);
        disableButtonsForRunningTask();
    }

    private void disableButtonsForRunningTask() {
        startScanButton.setDisable(true);
        stopScanButton.setDisable(false);
        exitScanButton.setDisable(false);
        inputDirectoryButton.setDisable(true);
        outputDirectoryButton.setDisable(true);
    }

    private void disableButtonsForStoppedTask() {
        startScanButton.setDisable(false);
        stopScanButton.setDisable(true);
        exitScanButton.setDisable(false);
        inputDirectoryButton.setDisable(false);
        outputDirectoryButton.setDisable(false);
    }

}