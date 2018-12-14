package com.antra.test;

import org.apache.tika.Tika;
import org.apache.tika.detect.DefaultDetector;
import org.junit.Test;

import java.io.File;

public class TestTika {
    @Test
    public void testDetect() {
//        Tika tika = new Tika(new DefaultDetector());
//        System.out.println(tika.detect("/Users/daweizhuang/Documents/Resumes/SomePDF/0175_001.pdf"));
        System.out.println(new File(System.getProperty("user.home") + "/documents/resumes").exists());
    }
}
