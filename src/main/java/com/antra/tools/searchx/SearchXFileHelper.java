package com.antra.tools.searchx;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SearchXFileHelper {

    private static int start = 0;
    private static int total = 0;
    public static List<File>[] getFilesInFolder(String folderName) {
        start = 0;
        total = 0;
        List<File>[] files = new ArrayList[SearchX.CHUNK_NUM];
        for (int i = 0; i < SearchX.CHUNK_NUM; i++) {
            files[i] = new ArrayList<>();
        }
        File folder = new File(folderName);
        if(folder.isDirectory()){
            assembleFiles(folder, files);
        };
        System.out.println(total + " files added");
        return files;
    }

    private static void assembleFiles(File folder, List<File>[] files) {
        for (File f : folder.listFiles()) {
            if (f.isDirectory() && !f.isHidden()) {
                assembleFiles(f, files);
            }else if(!f.isHidden() && f.getTotalSpace() > 10 && !f.getName().startsWith("~")){
                files[start].add(f);
                total++;
                if (start == SearchX.CHUNK_NUM - 1) {
                    start = 0;
                } else {
                    start++;
                }
            }
        }
    }
}
