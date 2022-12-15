/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.brown.predictivebiology.cellcount;

import edu.brown.predictivebiology.utilities.IO;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Haitham
 */
public class TestSamples {
    
    public static void main(String[] args) throws IOException {
        List<Record> records = IO.readRecords(
                new File("E:\\Harmony\\3D Harmony counts\\New Format\\"
                        + "Objects_Population - Nuclei.txt"));
//        Collections.sort(records);
//        for (Record record : records) {
//            System.out.println(record);
//        }
        Set<Integer> rowEx = new HashSet<>();
        rowEx.add(1);
        rowEx.add(3);
        rowEx.add(5);
        rowEx.add(7);
        Set<Integer> colEx = new HashSet<>();
        colEx.add(2);
        colEx.add(4);
        colEx.add(6);
        colEx.add(8);
        colEx.add(10);
        colEx.add(12);
        Set<Integer> planeEx = new HashSet<>();
        planeEx.add(1);
        planeEx.add(79);
//        Set<Integer> fieldEx = new HashSet<>();
//        fieldEx.add(1);
        Utilities.count(true, false, records, rowEx, colEx, null, null, 1);
    }
}
