package com.snowreplicator.filecomparator;

import com.snowreplicator.filecomparator.exceptions.DirectoryIsNotExistsException;
import com.snowreplicator.filecomparator.exceptions.FileComparatorTaskIsCanceledException;
import com.snowreplicator.filecomparator.view_model.FileInfo;
import com.snowreplicator.filecomparator.view_model.InputScanParams;

import javafx.application.Platform;
import javafx.concurrent.Task;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class FileComparatorTask extends Task<Void> {

    private final InputScanParams inputScanParams;
    private final FileComparatorController fileComparatorController;

    public FileComparatorTask(InputScanParams inputScanParams, FileComparatorController fileComparatorController) {
        this.inputScanParams = inputScanParams;
        this.fileComparatorController = fileComparatorController;
    }

    private void sendMessage(String message) {
        Platform.runLater(() -> fileComparatorController.addMessageToLogArea(message));
    }

    private void replaceLastMessage(String message) {
        Platform.runLater(() -> fileComparatorController.replaceLastMessageInLogArea(message));
    }

    @Override
    protected Void call() throws Exception {
        mainTaskFunc();
        return null;
    }

    private void checkIsCanceled() throws FileComparatorTaskIsCanceledException {
        if (isCancelled() || Thread.currentThread().isInterrupted())
            throw new FileComparatorTaskIsCanceledException();
    }

    private void mainTaskFunc() throws RuntimeException, IOException, NoSuchAlgorithmException {
        checkDirectoryExists(inputScanParams.inputDir().toString());
        checkDirectoryExists(inputScanParams.outputDir().toString());

        List<Path> inputDirFiles = scanFilesInDirectory(inputScanParams.inputDir(), inputScanParams.inputSubDirsEnabled());
        List<Path> outputDirFiles = scanFilesInDirectory(inputScanParams.outputDir(), inputScanParams.outputSubDirsEnabled());

        List<FileInfo> outputFilesInfo = calculateFilesHash(outputDirFiles);

        processInputFiles(inputDirFiles, outputFilesInfo);
    }

    private void checkDirectoryExists(String directoryPath) throws DirectoryIsNotExistsException {
        try {
            Path path = Paths.get(directoryPath);
            if (!directoryPath.trim().isEmpty() && Files.isDirectory(path))
                return;
        } catch (Exception ignored) {
        }
        throw new DirectoryIsNotExistsException(directoryPath);
    }

    private List<Path> scanFilesInDirectory(Path dir, boolean includeSubDirs) throws IOException {
        List<Path> files = new ArrayList<>();
        Deque<Path> stack = new LinkedList<>();
        stack.push(dir);

        sendMessage("");
        sendMessage("Start scanning files in directory: " + dir);

        long count = 0;
        while (!stack.isEmpty()) {
            checkIsCanceled();

            Path currentPath = stack.pop();
            if (!Files.isReadable(currentPath))
                continue;

            if (Files.isDirectory(currentPath)) {
                try (Stream<Path> stream = Files.list(currentPath)) {
                    for (Path subPath : stream.toList()) {
                        if (includeSubDirs)
                            stack.push(subPath);
                        else if (!Files.isDirectory(subPath))
                            stack.push(subPath);
                    }
                }
            } else if (Files.isRegularFile(currentPath)) {
                files.add(currentPath);
            }

            count++;
            if (count % 100 == 0) {
                if (count <= 100)
                    sendMessage("scanned files count: " + files.size());
                else
                    replaceLastMessage("scanned files count: " + files.size());
            }
        }

        sendMessage("scanned: " + files.size() + " files");
        return files;
    }

    private List<FileInfo> calculateFilesHash(List<Path> files) throws NoSuchAlgorithmException, IOException {
        sendMessage("");
        sendMessage("Calculate hash for files in output directory");

        long count = 0;
        List<FileInfo> filesInfo = new ArrayList<>();
        for (Path file : files) {
            checkIsCanceled();

            byte[] hash = calculateFileHash(file.toString());
            filesInfo.add(new FileInfo(file, hash));

            count++;
            if (count % 100 == 0) {
                if (count <= 100)
                    sendMessage("hash calculated for: " + count + " files");
                else
                    replaceLastMessage("hash calculated for: " + count + " files");
            }
        }

        replaceLastMessage("hash calculated for: " + count + " files");
        return filesInfo;
    }

    private byte[] calculateFileHash(String file) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                checkIsCanceled();
                digest.update(buffer, 0, bytesRead);
            }
        }

        return digest.digest();
    }

    private void processInputFiles(List<Path> inputFiles, List<FileInfo> outputFilesInfo) throws NoSuchAlgorithmException, IOException {
        sendMessage("");
        sendMessage("Start coping not existed files to output dir");

        long count = 0;
        for (Path inputFile : inputFiles) {
            checkIsCanceled();

            byte[] inputFileHash = calculateFileHash(inputFile.toString());

            Path existsFile = null;
            for (FileInfo outputFileInfo : outputFilesInfo) {
                checkIsCanceled();

                if (outputFileInfo != null && outputFileInfo.hash() != null && Arrays.equals(outputFileInfo.hash(), inputFileHash)) {
                    existsFile = outputFileInfo.path();
                    break;
                }
            }

            if (existsFile == null) {
                copyFileToOutputDir(inputFile);

                count++;
                if (count % 100 == 0) {
                    if (count <= 100)
                        sendMessage("Copied: " + count + " files");
                    else
                        replaceLastMessage("Copied: " + count + " files");
                }
            }
        }

        sendMessage("Was copied: " + count + " files");
    }

    private void copyFileToOutputDir(Path inputFile) throws IOException {
        Path inputFileSubPath = inputScanParams.inputDir().relativize(inputFile);
        Path outputFileFullPath = inputScanParams.outputDir().resolve(inputFileSubPath);

        Files.createDirectories(outputFileFullPath.getParent());
        Files.copy(inputFile, outputFileFullPath, StandardCopyOption.REPLACE_EXISTING);
    }

}
