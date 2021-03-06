package com.antra.tools.realtime;

import com.antra.tools.searchx.CaseSensitive;
import com.antra.tools.searchx.SearchXFileHelper;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import org.apache.tika.Tika;
import org.apache.tika.detect.DefaultDetector;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Controller {

    @FXML GridPane searchArea;
    @FXML Accordion dataPane;
    @FXML TextField folderInput;
    @FXML TextField keywordInput;
    @FXML Button folderBrowseBtn;
    @FXML Button goBtn;
    @FXML Button cancelBtn;
    @FXML CheckBox csChkBox;
    @FXML ProgressIndicator progressInd;
    @FXML Label btmStatus;
    @FXML MenuItem aboutMenu;
    long timestamp = 0l;
    FlowPane typePane;
    final List<String> selectedType = new ArrayList<>();
    SearchXService service;
    @FXML
    protected void initialize(){


        typePane = new FlowPane();
        typePane.alignmentProperty().setValue(Pos.CENTER_LEFT);
        searchArea.add(typePane,2,2,1,1);
        for (String str : SearchXFileHelper.supportedTypes.keySet()) {
            CheckBox c = new CheckBox(str);
            c.setStyle("-fx-padding: 10");
            c.setSelected(true);
            typePane.getChildren().add(c);
        }

        if (new File(System.getProperty("user.home") + "/documents/resumes").exists()) {
            folderInput.setText(System.getProperty("user.home")+File.separator + "documents" +File.separator+"resumes");
        }
        folderBrowseBtn.setOnMouseClicked(e->{
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Open target folder");
            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            File selectedDirectory = directoryChooser.showDialog(folderBrowseBtn.getScene().getWindow());
            if(selectedDirectory != null) {
                folderInput.setText(selectedDirectory.getAbsolutePath().trim());
            }
        });

        goBtn.setOnMouseClicked(event -> {
            if(checkFolder(folderInput.getText().trim()) && checkKeyword(keywordInput.getText()) && setAndCheckType()){
                try {
                    cancelBtn.setVisible(true);
                    progressInd.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
                    progressInd.setVisible(true);
                    goBtn.setDisable(true);
                    timestamp = System.nanoTime();
                    btmStatus.setText("");
                    dataPane.getPanes().clear();
                    doTheSearch(folderInput.getText().trim(),keywordInput.getText(), csChkBox.isSelected());
                } catch (IOException e) {
                    e.printStackTrace();
                    alarmError(e.getLocalizedMessage());
                }
            }
        });

        cancelBtn.setOnMouseClicked(event -> {if(service != null || service.isRunning()) service.cancel();});
        aboutMenu.setOnAction((event -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("About");
            alert.setHeaderText("The search will open each file under the specified folder \nand search line by line with exact match of the keyword. \nMaybe running slow if the files are too many.");
            alert.setContentText("Created by Dawei Z.(zdwrzz4@gmail.com)\nSearched by Apache Tika ");
            alert.showAndWait();
        }));

    }

    private void doTheSearch(String folderInput, String keywordInput, boolean selected) throws IOException {
        service = new SearchXService(folderInput, keywordInput, selected);
        service.start();
    }

    private class SearchXService extends Service<Void> {
        private String fi;
        private String kwi;
        private boolean cs;
        private int totalFileNo;
        private AtomicInteger fileProcessed = new AtomicInteger(0);
        private int resultNo;
        private Task task;

        @Override
        public boolean cancel() {
            if (task != null) {
                return task.cancel(false);
            } else {
                return false;
            }
        }

        public SearchXService(String folderInput, String keywordInput, boolean selected) {
            this.fi = folderInput;
            this.kwi = keywordInput;
            this.cs = selected;
        }

        @Override
        protected Task<Void> createTask() {

            task =  new Task<Void>() {

                @Override
                protected Void call() throws Exception {
                    List<File>[] files = SearchXFileHelper.getFilesInFolder(fi, selectedType);
                    List<File> flatFiles = new ArrayList<>();
                    for (List<File> fl : files) {
                        flatFiles.addAll(fl);
                        totalFileNo += fl.size();
                    }
                    findFiles(kwi, flatFiles, cs?CaseSensitive.YES:CaseSensitive.NO);
                    return null;
                }
            };

            EventHandler eventHandlerCancel = e->{
                progressInd.setVisible(false);
                goBtn.setDisable(false);
//                dataPane.getPanes().clear();
                cancelBtn.setVisible(false);
                btmStatus.setText("Result: " + resultNo + "/" + fileProcessed + "    Keyword is \""+kwi + "\"    Time Elapsed: " + new DecimalFormat("0.00").format((TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timestamp)) / 1000.0) + " seconds.");
            };

            task.setOnCancelled(eventHandlerCancel);
            task.setOnFailed(eventHandlerCancel);
            task.setOnSucceeded((event)->{
                if (totalFileNo == 0) {
                      progressInd.setVisible(false);
                }
                goBtn.setDisable(false);
                btmStatus.setText("Result: " + resultNo + "/" + fileProcessed + "    Keyword is \""+kwi + "\"    Time Elapsed: " + new DecimalFormat("0.00").format((TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timestamp)) / 1000.0) + " seconds.");
                cancelBtn.setVisible(false);
            });

            return task;

        }

        private int findFiles(String searchTerm, List<File> filesToSearch, CaseSensitive cs){
            int total = 0;
            Tika tika = new Tika(new DefaultDetector());
            if(cs == CaseSensitive.NO){
                searchTerm = searchTerm.toLowerCase();
            }
            for(int i = 0; i < filesToSearch.size(); i++){
                File f = filesToSearch.get(i);
                fileProcessed.incrementAndGet();
//                if(selectedType.stream().map(supportedTypes::get).allMatch(type->!tika.detect(f.getAbsolutePath()).contains(type))){
//                    updateUI(null, fileProcessed.get(), totalFileNo);
//                    continue;
//                }
//                System.out.println(Thread.currentThread() + " : "+"Working on " + f.getAbsolutePath());
//                BufferedReader reader = null;
                List<String> matchLines = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(tika.parse(f))){

                    String tempStr = null;
                    String originStr = null;

                    while ((originStr = reader.readLine()) != null && !task.isCancelled()) {
                        if(cs == CaseSensitive.NO){
                            tempStr = originStr.toLowerCase();
                        }else{
                            tempStr = originStr;
                        }
                        if(tempStr.contains(searchTerm)){
                            matchLines.add(originStr.trim().replaceAll(" +", " ").replaceAll("\t+", " "));
                        }
                    }
                    SearchResultPojo onePiece = null;
                    if (matchLines.size() > 0) {
                        onePiece = new SearchResultPojo(f.getAbsolutePath(), matchLines);
                        total++;
                        resultNo++;
                    }
                    if(!task.isCancelled()) {
                        updateUI(onePiece, fileProcessed.get(), totalFileNo);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return total;
        }
    }

    private void updateUI(SearchResultPojo onePiece, int current, int total) {
        Platform.runLater(()->{
            progressInd.setProgress((double)current/total);
            if(onePiece == null) return;
            TitledPane tp = new TitledPane(onePiece.getFileName(),new Label("A"));
            final HBox hBox = new HBox();
            hBox.setSpacing(5);
            TextArea text = new TextArea(onePiece.getMatchingLines().stream().collect(Collectors.joining("\n")));
            text.setPrefSize(670, 200);
            Button openBtn = new Button("Open");
            openBtn.setOnMouseClicked(e->{
                System.out.println("try to open " +onePiece.getFileName());
                if( Desktop.isDesktopSupported() )
                {
                    new Thread(() -> {
                        try {
                            Desktop.getDesktop().open( new File( onePiece.getFileName()) );
                        } catch (IOException e1) {
                            alarmError("Cannot Open File " + onePiece.getFileName());
                            e1.printStackTrace();
                        }
                    }).start();
                }
            });
            hBox.getChildren().add(openBtn);
            hBox.getChildren().add(text);
            tp.setContent(hBox);
            dataPane.getPanes().add(tp);
        });
    }

    private boolean checkKeyword(String text) {
        if(text != null && !text.trim().isEmpty()) {
            return true;
        }
        else {
            alarmError("Invalid Keyword");
            return false;
        }
    }

    private boolean setAndCheckType() {
        selectedType.clear();
        typePane.getChildren().stream().filter(n -> n instanceof CheckBox).map(n->(CheckBox)n).filter(checkBox -> checkBox.isSelected()).forEach(cb->{
            selectedType.add(cb.getText());
        });
        if(selectedType.size() > 0){
            return true;
        }else{
            alarmError("No File Type is selected");
            return false;
        }
    }


    private boolean checkFolder(String text) {
        if(text != null && !text.trim().isEmpty() && Files.isDirectory(new File(text).toPath(), LinkOption.NOFOLLOW_LINKS)){
            return true;
        }else{
            alarmError("Invalid Folder Path");
            return false;
        }
    }

    private void alarmError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error Info");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

}
