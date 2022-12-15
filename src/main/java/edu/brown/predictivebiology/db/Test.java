/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.brown.predictivebiology.db;

import edu.brown.predictivebiology.db.beans.Image;
import edu.brown.predictivebiology.db.beans.Coordinates;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Haitham
 */
public class Test {

    private static final List<File> images;

    static {
        images = new ArrayList<>();
        images.add(new File("Z:\\BM_JM_PredictiveBiology\\Boekelheide Lab\\Haitham\\2018-3-28 stainless steel mold MCF7\\polished\\r04c03f02/combined-r04c03f02p19.png"));
        images.add(new File("Z:\\BM_JM_PredictiveBiology\\Boekelheide Lab\\Haitham\\2018-3-28 stainless steel mold MCF7\\polished\\r04c03f02/combined-r04c03f02p20.png"));
        images.add(new File("Z:\\BM_JM_PredictiveBiology\\Boekelheide Lab\\Haitham\\2018-3-28 stainless steel mold MCF7\\polished\\r04c03f02/combined-r04c03f02p21.png"));
        images.add(new File("Z:\\BM_JM_PredictiveBiology\\Boekelheide Lab\\Haitham\\2018-3-28 stainless steel mold MCF7\\polished\\r04c03f02/combined-r04c03f02p22.png"));
        images.add(new File("Z:\\BM_JM_PredictiveBiology\\Boekelheide Lab\\Haitham\\2018-3-28 stainless steel mold MCF7\\polished\\r04c03f02/combined-r04c03f02p23.png"));
    }

//    public static void main(String[] args) throws ClassNotFoundException, SQLException {
//        DbUtilities.deleteDb();
//        DbUtilities.createDb();
//        addData();
//        displayEverything();
//        displayImageOccurrences(images.get(0).getPath());
////        System.out.println("---");
////        removeSomeCoordinatesData();
////        displayEverything();
//        System.out.println("---");
//        DbUtilities.deleteImage(images.get(0).getPath());
//        displayEverything();
//        displayImageOccurrences(images.get(0).getPath());
//    }
//
//    private static void displayImageOccurrences(String imageFilePath) throws ClassNotFoundException, SQLException {
//        List<Image> imageOccurences = DbUtilities.getImageOccurence(imageFilePath);
//        System.out.format("(%s) Exists (%d) times.%n", imageFilePath, imageOccurences.size());
//        for(Image imageRecord : imageOccurences) {
//            System.out.println(imageRecord);
//        }
//    }
//
//    private static void removeSomeCoordinatesData()
//            throws ClassNotFoundException, SQLException {
//        DbUtilities.clearImageCoordinates(images.get(0).getPath());
//        DbUtilities.clearImageCoordinates(images.get(1).getPath());
//    }
//
//    private static void addData() throws ClassNotFoundException, SQLException {
//        for (File file : images) {
//            DbUtilities.addImage(file.getParentFile().getPath(), file.getPath());
//        }
//        // Add coordinates to some images
//        // Image 0
//        DbUtilities.addCoordinates(11, 22, images.get(0).getPath());
//        DbUtilities.addCoordinates(33, 44, images.get(0).getPath());
//        DbUtilities.addCoordinates(55, 66, images.get(0).getPath());
//        // Image 1
//        DbUtilities.addCoordinates(111, 222, images.get(1).getPath());
//        // We assume that Image 2 has no lumens
//        // Image 3
//        DbUtilities.addCoordinates(11111, 22222, images.get(3).getPath());
//        DbUtilities.addCoordinates(33333, 44444, images.get(3).getPath());
//    }
//
//    private static void displayEverything() throws SQLException, ClassNotFoundException {
//        try {
//            List<Image> allImages = DbUtilities.getAllImages();
//            for (Image image : allImages) {
//                System.out.println(image);
//            }
//            List<Coordinates> allCoordinates = DbUtilities.getAllCoordinates();
//            for (Coordinates coordinates : allCoordinates) {
//                System.out.println(coordinates);
//            }
//        } catch (NoSuchMethodException ex) {
//            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (InstantiationException ex) {
//            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (IllegalAccessException ex) {
//            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (InvocationTargetException ex) {
//            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
}