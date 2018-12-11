package com.antra.tools.ui;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import com.antra.tools.searchx.SearchX;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    @FXML
    protected void initialize(){
        folderBrowseBtn.setOnMouseClicked(e->{
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Open target folder");
            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            File selectedDirectory = directoryChooser.showDialog(folderBrowseBtn.getScene().getWindow());
            folderInput.setText(selectedDirectory.getAbsolutePath());
        });
        goBtn.setOnMouseClicked(event -> {
            if(checkFolder(folderInput.getText()) && checkKeyword(keywordInput.getText())){
                try {
                    progressInd.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
                    progressInd.setVisible(true);
                    doTheSearch(folderInput.getText(),keywordInput.getText(), csChkBox.isSelected());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private void doTheSearch(String folderInput, String keywordInput, boolean selected) throws IOException {
        SearchXService service = new SearchXService(folderInput, keywordInput, selected);
        service.start();
    }

    private class SearchXService extends Service<Map<String,List<String>>> {
        private String fi;
        private String kwi;
        private boolean cs;
        public SearchXService(String folderInput, String keywordInput, boolean selected) {
            this.fi = folderInput;
            this.kwi = keywordInput;
            this.cs = selected;
        }

        @Override
        protected Task<Map<String, List<String>>> createTask() {

            Task< Map<String,List<String>>> task =  new Task< Map<String,List<String>>>() {
                @Override protected  Map<String,List<String>> call() throws Exception {
                    return SearchX.doTheSearch(fi,kwi,cs);
                }
            };

            task.setOnSucceeded((event)->{
                progressInd.setVisible(false);
                dataPane.getPanes().clear();
                Map<String,List<String>> data = (HashMap)event.getSource().getValue();
                data.entrySet().stream().forEach(entry -> {
                    TitledPane tp = new TitledPane(entry.getKey(),new Label("A"));
                    final HBox hBox = new HBox();
                    hBox.setSpacing(5);
                    TextArea text = new TextArea(entry.getValue().stream().collect(Collectors.joining("\n")));
                    Button openBtn = new Button("Open");
                    openBtn.setOnMouseClicked(e->{
                        System.out.println("try to open " + entry.getKey());
                        if( Desktop.isDesktopSupported() )
                        {
                            new Thread(() -> {
                                try {
                                    Desktop.getDesktop().open( new File( entry.getKey() ) );
                                } catch (IOException e1) {
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
            });

            return task;

        }
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
        System.out.println(msg);
    }
}
