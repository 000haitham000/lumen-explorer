/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.brown.predictivebiology.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Haitham
 */
public class Utilities {

    /**
     * Sum a 2D double array
     *
     * @param arr a two dimensional double array
     * @return sum of argument array elements
     */
    public static double sum(double[][] arr) {
        double sum = 0;
        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr[i].length; j++) {
                sum += arr[i][j];
            }
        }
        return sum;
    }

    public static String readProperty(String propertyName) {
        BufferedReader reader = null;
        try {
            File propertiesFile = new File(IO.class.getResource("/configuration/properties.properties").toURI());
            reader = new BufferedReader(new FileReader(propertiesFile));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] splits = line.split("=");
                if (splits[0].trim().equals(propertyName)) {
                    return splits[1].trim();
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Utilities.class.getName()).log(Level.SEVERE, null, ex);
        } catch (URISyntaxException ex) {
            Logger.getLogger(Utilities.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    Logger.getLogger(Utilities.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return null;
    }
    
    public static void main(String[] args) {
        System.out.println(readProperty("working-dir"));
    }
}
