package com.antra.tools.searchx;

import org.apache.tika.Tika;
import org.apache.tika.detect.DefaultDetector;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchXFileHelper {

    private static int start = 0;
    private static int total = 0;
    public final static Map<String, String> supportedTypes = new HashMap<>();
    private static Tika tika = new Tika(new DefaultDetector());

    static {
        supportedTypes.put("Office", "office");
        supportedTypes.put("Text", "text");
        supportedTypes.put("PDF", "application/pdf");
    }
    public static List<File>[] getFilesInFolder(String folderName, List<String> types) {
        start = 0;
        total = 0;
        List<File>[] files = new ArrayList[SearchX.CHUNK_NUM];
        for (int i = 0; i < SearchX.CHUNK_NUM; i++) {
            files[i] = new ArrayList<>();
        }
        File folder = new File(folderName);
        if(folder.isDirectory()){
            assembleFiles(folder, files, types);
        };
        System.out.println(total + " files added");
        return files;
    }
    private static void assembleFiles(File folder, List<File>[] files, List<String> types) {
        for (File f : folder.listFiles()) {
            if (f.isDirectory() && !f.isHidden()) {
                assembleFiles(f, files, types);
            }else if(!f.isHidden() && f.getTotalSpace() > 1 && !f.getName().startsWith("~")){
                if(types.stream().map(supportedTypes::get).anyMatch(type->tika.detect(f.getAbsolutePath()).contains(type))) {
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

//    public static List<File>[] getFilesInFolder(String folderName) {
//        start = 0;
//        total = 0;
//        List<File>[] files = new ArrayList[SearchX.CHUNK_NUM];
//        for (int i = 0; i < SearchX.CHUNK_NUM; i++) {
//            files[i] = new ArrayList<>();
//        }
//        File folder = new File(folderName);
//        if(folder.isDirectory()){
//            assembleFiles(folder, files);
//        };
//        System.out.println(total + " files added");
//        return files;
//    }
//    private static void assembleFiles(File folder, List<File>[] files) {
//        for (File f : folder.listFiles()) {
//            if (f.isDirectory() && !f.isHidden()) {
//                assembleFiles(f, files);
//            }else if(!f.isHidden() && f.getTotalSpace() > 1 && !f.getName().startsWith("~")){
//                files[start].add(f);
//                total++;
//                if (start == SearchX.CHUNK_NUM - 1) {
//                    start = 0;
//                } else {
//                    start++;
//                }
//            }
//        }
//    }
}
