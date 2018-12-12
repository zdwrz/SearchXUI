package com.antra.tools.realtime;

import com.antra.tools.searchx.CaseSensitive;
import com.antra.tools.searchx.SearchXFileHelper;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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
    @FXML CheckBox csChkBox;
    @FXML ProgressIndicator progressInd;
    @FXML Label btmStatus;
    long timestamp = 0l;


    @FXML
    protected void initialize(){
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
            if(checkFolder(folderInput.getText().trim()) && checkKeyword(keywordInput.getText())){
                try {
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
    }


    private void doTheSearch(String folderInput, String keywordInput, boolean selected) throws IOException {
        SearchXService service = new SearchXService(folderInput, keywordInput, selected);
        service.start();
    }

    private class SearchXService extends Service<Void> {
        private String fi;
        private String kwi;
        private boolean cs;
        private int totalFileNo;
        private AtomicInteger fileProcessed = new AtomicInteger(0);

        public SearchXService(String folderInput, String keywordInput, boolean selected) {
            this.fi = folderInput;
            this.kwi = keywordInput;
            this.cs = selected;
        }

        @Override
        protected Task<Void> createTask() {

            Task<Void> task =  new Task<Void>() {

                @Override
                protected Void call() throws Exception {
                    List<File>[] files = SearchXFileHelper.getFilesInFolder(fi);
                    for (List<File> fl : files) {
                        totalFileNo += fl.size();
                    }
                    for (List<File> fileList : files) {
                        findFiles(kwi, fileList, cs?CaseSensitive.YES:CaseSensitive.NO);
                    }
                    return null;
                }
            };
            task.setOnFailed(e->{
                progressInd.setVisible(false);
                goBtn.setDisable(false);
                dataPane.getPanes().clear();
            });
            task.setOnSucceeded((event)->{
              //  progressInd.setVisible(false);
                goBtn.setDisable(false);
                btmStatus.setText("Result: " + fileProcessed + "    Keyword is \""+kwi + "\"    Time Elapsed: " + new DecimalFormat("0.00").format((TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timestamp)) / 1000.0) + " seconds.");
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
                if(!tika.detect(f.getAbsolutePath()).contains("office") && !tika.detect(f.getAbsolutePath()).contains("text/plain")){
                    updateUI(null, fileProcessed.get(), totalFileNo);
                    continue;
                }
//                System.out.println(Thread.currentThread() + " : "+"Working on " + f.getAbsolutePath());
                BufferedReader reader = null;
                List<String> matchLines = new ArrayList<>();
                try {
                    reader = new BufferedReader(tika.parse(f));

                    String tempStr = null;
                    String originStr = null;

                    while ((originStr = reader.readLine()) != null) {
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
                    }

                    updateUI(onePiece, fileProcessed.get(), totalFileNo);

                    reader.close();
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
