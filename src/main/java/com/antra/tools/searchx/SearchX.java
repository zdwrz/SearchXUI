package com.antra.tools.searchx;

import org.apache.tika.Tika;
import org.apache.tika.detect.DefaultDetector;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SearchX {

    public static Map<String, List<String>> doTheSearch(String folderName, String keyword, boolean caseSensitive) throws IOException {

        List<File>[] files = SearchXFileHelper.getFilesInFolder(folderName);
        ExecutorService es = Executors.newFixedThreadPool(4);
        List<Future<Map<String, List<String>>>> futures = new ArrayList<>();
        for (List<File> fl : files) {
            Future<Map<String, List<String>>> future = es.submit(() -> {
                    return findFiles(keyword, fl, caseSensitive?CaseSensitive.YES:CaseSensitive.NO);
            });
            futures.add(future);
        }
        Map<String, List<String>> finalResult = new HashMap<>();
        futures.stream().forEach(f-> {
            try {
                finalResult.putAll(f.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        });
        es.shutdown();

        return finalResult;
    }

    public static Map<String, List<String>> findFiles(String searchTerm, List<File> filesToSearch, CaseSensitive cs){
        Map<String, List<String>> result = new HashMap<>();
        Tika tika = new Tika(new DefaultDetector());
        if(cs == CaseSensitive.NO){
            searchTerm = searchTerm.toLowerCase();
        }
        for(File f : filesToSearch) {

            if(!tika.detect(f.getAbsolutePath()).contains("office") && !tika.detect(f.getAbsolutePath()).contains("text/plain")){
                continue;
            }
            System.out.println(Thread.currentThread() + " : "+"Working on " + f.getAbsolutePath());
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
                if (matchLines.size() > 0) {
                    result.put(f.getAbsolutePath(), matchLines);
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
