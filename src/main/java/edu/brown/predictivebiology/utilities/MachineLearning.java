/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.brown.predictivebiology.utilities;

import com.mathworks.toolbox.javabuilder.MWArray;
import com.mathworks.toolbox.javabuilder.MWCellArray;
import com.mathworks.toolbox.javabuilder.MWCharArray;
import com.mathworks.toolbox.javabuilder.MWClassID;
import com.mathworks.toolbox.javabuilder.MWException;
import com.mathworks.toolbox.javabuilder.MWLogicalArray;
import com.mathworks.toolbox.javabuilder.MWNumericArray;
import edu.brown.predictivebiology.db.DbUtilities;
import edu.brown.predictivebiology.db.beans.Coordinates;
import edu.brown.predictivebiology.db.beans.Experiment;
import edu.brown.predictivebiology.db.beans.Image;
import edu.brown.predictivebiology.gui.RowColumnFieldPlane;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Point3D;
import lumenAnalysis.LumenAnalyzer;
//import lumenAnalysis.LumenAnlyzer;
import lumenClassify.LumenClassifier;
import lumen_detection.LumenDetector;

/**
 *
 * @author Haitham
 */
public class MachineLearning {

    private LumenDetector lumenDetector;
    private LumenClassifier lumenClassifier;

    public MachineLearning() throws MWException {
        if (lumenDetector == null) {
            lumenDetector = new LumenDetector();
        }
        if (lumenClassifier == null) {
            lumenClassifier = new LumenClassifier();
        }
    }

    /**
     * Extracts potential lumen images from all the images of an experiment.
     *
     * @param experiment the experiment under investigation
     * @param mode the MATLAB function supports two modes: "train" and
     * @param outputDirPath root directory that contains two sub-directories,
     * one for lumens and the other for non-lumens.
     * @param linearFusionWeight linear fusion weight parameter
     * @param taggingScalingFactor the scaling factor applied to the image
     * before manual tagging started
     * @param diskSizeStart start value of the size of the disk used in closing
     * operations and top hat filter
     * @param diskSizeStep step value of the size of the disk used in closing
     * operations and top hat filter
     * @param diskSizeEnd end value of the size of the disk used in closing
     * operations and top hat filter
     * @param binarizationThreshold threshold used to convert a gray-scale image
     * to a binary image
     * @param openingSize the minimum size in pixels of a connected component
     * that needs to be kept in the image (used to clear the image)
     * @param subImageMarginAreaPercent area of the disk used to create the
     * margin as a percentage of the total area of the lumen
     * @param subImageMarginMaxDiskRadius maximum radius of the disk used to
     * create the margin
     * @param displayAll display operations
     * @return a list of sub-lists. Each sub-list has the information of the
     * sub-images of one image
     * @throws MWException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public List<List<SubImageInfo>> extractLearningData(
            // Experiment
            Experiment experiment,
            // Mode
            String mode,
            // Output directory path
            String outputDirPath,
            // Linear fusion weight
            double linearFusionWeight,
            // The scaling factor used by the tagging software on images before 
            // allowing the user to manually identify images
            double taggingScalingFactor,
            // Disk size
            int diskSizeStart,
            int diskSizeStep,
            int diskSizeEnd,
            // Binarization threshold
            double binarizationThreshold,
            // Morphological Opening Size
            int openingSize,
            // Sub-image extra margin thickness
            double subImageMarginAreaPercent,
            // Sub-image margin max disk radius
            int subImageMarginMaxDiskRadius,
            // Display operations
            boolean displayAll)
            throws
            MWException,
            SQLException,
            ClassNotFoundException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        List<Image> images
                = DbUtilities.getImagesOfExperiment(experiment.getId());
        List<List<SubImageInfo>> listOfLists = new ArrayList<>();
        for (Image image : images) {
            System.out.println("Processing: " + image.getPath());
            listOfLists.add(extractLearningData(
                    mode,
                    image,
                    outputDirPath,
                    linearFusionWeight,
                    taggingScalingFactor,
                    diskSizeStart,
                    diskSizeStep,
                    diskSizeEnd,
                    binarizationThreshold,
                    openingSize,
                    subImageMarginAreaPercent,
                    subImageMarginMaxDiskRadius,
                    displayAll));
        }
        return listOfLists;
    }

    /**
     * Extracts potential lumen images from a specific image.
     *
     * @param mode the MATLAB function supports two modes: "train" & "classify"
     * @param image the image under investigation
     * @param outputDirPath root directory that contains two sub-directories,
     * one for lumens and the other for non-lumens.
     * @param linearFusionWeight linear fusion parameter
     * @param taggingScalingFactor the scaling factor applied to the image
     * before manual tagging started
     * @param diskSizeStart start value of the size of the disk used in closing
     * operations and top hat filter
     * @param diskSizeStep step value of the size of the disk used in closing
     * operations and top hat filter
     * @param diskSizeEnd end value of the size of the disk used in closing
     * operations and top hat filter
     * @param binarizationThreshold threshold used to convert a gray-scale image
     * to a binary image
     * @param openingSize the minimum size in pixels of a connected component
     * that needs to be kept in the image (used to clear the image)
     * @param subImageMarginAreaPercent area of the disk used to create the
     * margin as a percentage of the total area of the lumen
     * @param subImageMarginMaxDiskRadius maximum radius of the disk used to
     * create the margin
     * @param displayAll display operations
     * @return a list containing the information of the sub-images of (image)
     * @throws MWException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public List<SubImageInfo> extractLearningData(
            // String mode
            String mode,
            // Input image
            Image image,
            // Output directory path
            String outputDirPath,
            // Linear fusion weight
            double linearFusionWeight,
            // The scaling factor used by the tagging software on images before 
            // allowing the user to manually identify images
            double taggingScalingFactor,
            // Disk size
            int diskSizeStart,
            int diskSizeStep,
            int diskSizeEnd,
            // Binarization threshold
            double binarizationThreshold,
            // Morphological Opening Size
            int openingSize,
            // Sub-image extra margin thickness
            double subImageMarginAreaPercent,
            // Sub-image margin max disk radius
            int subImageMarginMaxDiskRadius,
            // Display operations
            boolean displayAll)
            throws
            MWException,
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        // Get the manually identified coordinates from the database
        List<Coordinates> coordinatesList
                = DbUtilities.getCoordinatesOfImage(image.getId());
        // Copy all the coordinates in a 2D array
        double[][] coordinates = new double[coordinatesList.size()][2];
        for (int i = 0; i < coordinatesList.size(); i++) {
            coordinates[i][0] = coordinatesList.get(i).getX();
            coordinates[i][1] = coordinatesList.get(i).getY();
        }
        // Extract learning data from the image
        return extractLearningData(
                mode,
                //"E:\\Dropbox\\Harmony\\3D Harmony counts\\Test Images\\test-10JUL2018\\selected\\r03c07f05\\r03c07f05p40-ch1sk1fk1fl1.tiff",
                //"E:\\Dropbox\\Harmony\\3D Harmony counts\\Test Images\\test-10JUL2018\\selected\\r03c07f05\\r03c07f05p40-ch2sk1fk1fl1.tiff",
                DbUtilities.getOriginalTiffFiles(image, "ch1").get(0).getPath(),
                DbUtilities.getOriginalTiffFiles(image, "ch2").get(0).getPath(),
                outputDirPath,
                linearFusionWeight,
                coordinates,
                taggingScalingFactor,
                diskSizeStart,
                diskSizeStep,
                diskSizeEnd,
                binarizationThreshold,
                openingSize,
                subImageMarginAreaPercent,
                subImageMarginMaxDiskRadius,
                displayAll);
    }

    /**
     * Extracts potential lumen images from a specific image.
     *
     * @param mode the MATLAB function supports two modes: "train" and
     * "preclassify". "train" should be used for processing labeled data as a
     * step towards creating the classifier, while "preclassify" should be used
     * while preparing the un-labeled data for classification.
     * @param cellsImageFilePath cells channel of the image under consideration
     * @param wallsImageFilePath walls channel of the image under consideration
     * @param outputDirPath root directory that contains two sub-directories,
     * one for lumens and the other for non-lumens.
     * @param linearFusionWeight linear fusion weight parameter
     * @param coordinates the list of (x, y) coordinates of the manually
     * identified lumens
     * @param taggingScalingFactor the scaling factor applied to the image
     * before manual tagging started
     * @param diskSizeStart start value of the size of the disk used in closing
     * operations and top hat filter
     * @param diskSizeStep step value of the size of the disk used in closing
     * operations and top hat filter
     * @param diskSizeEnd end value of the size of the disk used in closing
     * operations and top hat filter
     * @param binarizationThreshold threshold used to convert a gray-scale image
     * to a binary image
     * @param openingSize the minimum size in pixels of a connected component
     * that needs to be kept in the image (used to clear the image)
     * @param subImageMarginAreaPercent area of the disk used to create the
     * margin as a percentage of the total area of the lumen
     * @param subImageMarginMaxDiskRadius maximum radius of the disk used to
     * create the margin
     * @param displayAll display operations
     * @return a list containing the information of the sub-images of (image)
     * @throws MWException
     */
    public List<SubImageInfo> extractLearningData(
            // Mode: either "train' or preclassify
            String mode,
            // Input image path (cells channel)
            String cellsImageFilePath,
            // Input image path (walls channel)
            String wallsImageFilePath,
            // Output directory path
            String outputDirPath,
            // Image fusion parameter (weight)
            double linearFusionWeight,
            // Manually identified lumen pixel coordinates
            double[][] coordinates,
            // The scaling factor used by the tagging software on images before 
            // allowing the user to manually identify images
            double taggingScalingFactor,
            // Disk size
            int diskSizeStart,
            int diskSizeStep,
            int diskSizeEnd,
            // Binarization threshold
            double binarizationThreshold,
            // Morphological Opening Size
            int openingSize,
            // Sub-image extra margin thickness
            double subImageMarginAreaPercent,
            // Sub-image margin max disk radius
            int subImageMarginMaxDiskRadius,
            // Display operations
            boolean displayAll) throws MWException {
        // Wrap in MATLAB friendly objects
        MWCharArray modeMW = new MWCharArray(mode);
        MWCharArray cellsImageFilePathMW = new MWCharArray(cellsImageFilePath);
        MWCharArray wallsImageFilePathMW = new MWCharArray(wallsImageFilePath);
        MWCharArray outputDirMW = new MWCharArray(outputDirPath);
        MWNumericArray linearFusionWeightMW = new MWNumericArray(linearFusionWeight, MWClassID.DOUBLE);
        MWNumericArray coordinatesMW = new MWNumericArray(coordinates, MWClassID.DOUBLE);
        MWNumericArray taggingScalingFactorMW = new MWNumericArray(taggingScalingFactor, MWClassID.DOUBLE);
        MWNumericArray diskSizeStartMW = new MWNumericArray(diskSizeStart, MWClassID.DOUBLE);
        MWNumericArray diskSizeStepMW = new MWNumericArray(diskSizeStep, MWClassID.DOUBLE);
        MWNumericArray diskSizeEndMW = new MWNumericArray(diskSizeEnd, MWClassID.DOUBLE);
        MWNumericArray binarizationThresholdMW = new MWNumericArray(binarizationThreshold, MWClassID.DOUBLE);
        MWNumericArray openingSizeMW = new MWNumericArray(openingSize, MWClassID.DOUBLE);
        MWNumericArray subImageMarginAreaPercentMW = new MWNumericArray(subImageMarginAreaPercent, MWClassID.DOUBLE);
        MWNumericArray subImageMarginMaxDiskRadiusMW = new MWNumericArray(subImageMarginMaxDiskRadius, MWClassID.DOUBLE);
        MWLogicalArray displayAllMW = new MWLogicalArray(displayAll);
        System.out.println(cellsImageFilePath);
        System.out.println(wallsImageFilePath);
        // Call MATLAB
        Object[] result = lumenDetector.extractData(
                1,
                modeMW,
                cellsImageFilePathMW,
                wallsImageFilePathMW,
                outputDirMW,
                linearFusionWeightMW,
                coordinatesMW,
                taggingScalingFactorMW,
                diskSizeStartMW,
                diskSizeStepMW,
                diskSizeEndMW,
                binarizationThresholdMW,
                openingSizeMW,
                subImageMarginAreaPercentMW,
                subImageMarginMaxDiskRadiusMW,
                displayAllMW);
        MWCellArray cellArray = (MWCellArray) result[0];
        List<MWArray> cellList = cellArray.asList();
        List<SubImageInfo> subImageInfoList = new ArrayList<>();
        for (MWArray cell : cellList) {
            List<MWArray> cellElements = ((MWCellArray) cell).asList();
            String subImagePath = cellElements.get(0).toString();
            double[][] body = (double[][]) ((MWNumericArray) cellElements.get(1)).toDoubleArray();
            double[][] boundary = (double[][]) ((MWNumericArray) cellElements.get(2)).toDoubleArray();
            double[][] corners = (double[][]) ((MWNumericArray) cellElements.get(3)).toDoubleArray();
            subImageInfoList.add(new SubImageInfo(subImagePath, body, boundary, corners));
        }

//        for (MWArray cell : cellList) {
//            List<MWArray> cellElements = ((MWCellArray)cell).asList();
//            String subImagePath = cellElements.get(0).toString();
//            double[][] body = (double[][]) ((MWNumericArray)cellElements.get(1)).toDoubleArray();
//            double[][] boundary = (double[][]) ((MWNumericArray)cellElements.get(2)).toDoubleArray();
//            double[][] corners = (double[][]) ((MWNumericArray)cellElements.get(3)).toDoubleArray();
//            System.out.println("Sub-image path: " + subImagePath);
//            System.out.println("Pixels:");
//            for (int i = 0; i < body.length; i++) {
//                for (int j = 0; j < body[i].length; j++) {
//                    System.out.println(body[i][j] + " ");
//                }
//                System.out.println();
//            }
//            System.out.println("Borders:");
//            for (int i = 0; i < corners.length; i++) {
//                for (int j = 0; j < corners[i].length; j++) {
//                    System.out.println(corners[i][j] + " ");
//                }
//                System.out.println();
//            }
//            System.out.println();
//        }
        // Dispose wrapping objects
        cellsImageFilePathMW.dispose();
        outputDirMW.dispose();
        coordinatesMW.dispose();
        taggingScalingFactorMW.dispose();
        diskSizeStartMW.dispose();
        diskSizeStepMW.dispose();
        diskSizeEndMW.dispose();
        binarizationThresholdMW.dispose();
        openingSizeMW.dispose();
        subImageMarginAreaPercentMW.dispose();
        // Return the list of sub-images info that MATLAB returned
        return subImageInfoList;
    }

    /**
     * Classifies all the sub-images as either lumens or not, collect lumen info
     * files into a mapping structure
     *
     * @param outputDir the directory containing the images to be classified
     * @return a mapping structure (row,column,field,plane) -> info-file
     * @throws com.mathworks.toolbox.javabuilder.MWException
     */
    public LumenMap classify(String outputDir) throws MWException {
        MWCharArray outputDirMW = new MWCharArray(outputDir);
        Object[] result = lumenClassifier.lumenClassify(3, outputDirMW);
        MWCellArray filesCellArr = ((MWCellArray) result[0]);
        List<MWArray> filesCellList = filesCellArr.asList();
        //MWCharArray classification = ((MWCharArray) result[1]);
        double[][] score = (double[][]) ((MWNumericArray) result[2]).toDoubleArray();
        // Collect lumen info files and add them to the map
        LumenMap lumenMap = new LumenMap();
        for (int i = 0; i < score.length; i++) {
            if (score[i][0] > score[i][1]) {
                File singleLumenImageFile = new File(
                        ((MWCharArray) filesCellList.get(i)).toString());
                int extLength
                        = IO.getFileExtension(singleLumenImageFile).length();
                StringBuilder singleLumenInfoFilePathBuider
                        = new StringBuilder(singleLumenImageFile.getPath());
                for (int j = 0; j < extLength; j++) {
                    singleLumenInfoFilePathBuider.deleteCharAt(
                            singleLumenInfoFilePathBuider.length() - 1);
                }
                singleLumenInfoFilePathBuider.append("txt");
                File singleLumenInfoFile
                        = new File(singleLumenInfoFilePathBuider.toString());
                RowColumnFieldPlane rcfp
                        = new RowColumnFieldPlane(singleLumenInfoFile);
                lumenMap.add(
                        rcfp.getRow(),
                        rcfp.getColumn(),
                        rcfp.getField(),
                        rcfp.getPlane(),
                        singleLumenInfoFile);
            }
        }
        return lumenMap;
    }

    /**
     * Calculates the volumes and surface areas of the argument lumens.
     *
     * @param lumenBoundaryFilesList each file represents one 3D lumen
     * @param plotEach if true, a figure will be generated for each lumen
     * @param plotAll if true, a figure will be generated for all lumens
     */
    public double calculateVolumesAndAreas(
            List<File> lumenBoundaryFilesList,
            boolean plotEach,
            boolean plotAll) throws MWException, IOException, URISyntaxException {
        MWCellArray filePathCellArrMW = null;
        LumenAnalyzer lumenAnlyzer = null;
        try {
            if (!lumenBoundaryFilesList.isEmpty()) {
                filePathCellArrMW = new MWCellArray(
                        lumenBoundaryFilesList.size(),
                        1);
                for (int i = 0; i < lumenBoundaryFilesList.size(); i++) {
                    filePathCellArrMW.set(new int[]{i + 1, 1}, lumenBoundaryFilesList.get(i).getPath());
                }
                System.out.println(filePathCellArrMW.toString());
                lumenAnlyzer = new LumenAnalyzer();
                Object[] result = lumenAnlyzer.calculateVolumeAndArea(2,
                        filePathCellArrMW, plotEach, plotAll);
                double[][] volumes = (double[][]) ((MWNumericArray) result[0]).toDoubleArray();
                double[][] areas = (double[][]) ((MWNumericArray) result[1]).toDoubleArray();
                // Save volumes and surface areas
                for (int i = 0; i < lumenBoundaryFilesList.size(); i++) {
                    System.out.println(lumenBoundaryFilesList.get(i));
                }
                for (int i = 0; i < lumenBoundaryFilesList.size(); i++) {
                    File file = lumenBoundaryFilesList.get(i);
                    double v = volumes[i][0];
                    double a = areas[i][0];
                    IO.writeVolumesAndSurfaceAreas(
                            file,
                            v,
                            a);
                }
                // Generate MATLAB scripts
                IO.writeMatlabVolumetricPlots(lumenBoundaryFilesList.get(0).getParentFile());
                // return total volume
                return Utilities.sum(volumes);
            } else {
                return 0.0;
            }
        } finally {
            if (filePathCellArrMW != null) {
                filePathCellArrMW.dispose();
            }
            if (lumenAnlyzer != null) {
                lumenAnlyzer.dispose();
            }
        }
    }

    /**
     * Groups related lumen slices that form one 3D lumen.
     *
     * @param lumenMap a map structure mapping row, column, field and plane to
     * their corresponding files
     * @param verticalInterPlaneGap the distance in microns between two
     * consecutive planes
     * @param pixelLengthInMicrons the length represented by a pixel in any
     * dimension (notice that a pixel is assumed to represent the same distance
     * in all directions.
     * @return a list of files each containing the 3D point of an independent
     * lumen
     */
    public List<File> groupRelatedLumenSlices(
            LumenMap lumenMap,
            double verticalInterPlaneGap,
            double pixelLengthInMicrons,
            String outputDirPath) throws IOException {
        List<File> lumenBoundaryFilesList = new ArrayList<>();
        int lumenCounter = 0;
        for (int row = lumenMap.getMinRow();
                row <= lumenMap.getMaxRow();
                row++) {
            for (int column = lumenMap.getMinColumn();
                    column <= lumenMap.getMaxColumn();
                    column++) {
                for (int field = lumenMap.getMinField();
                        field <= lumenMap.getMaxField();
                        field++) {
                    List<List<File>> lumenInfoFiles
                            = lumenMap.getLumenInfoFileAllPlanesLowerToHigher(
                                    row, column, field);
                    while (hasMoreFiles(lumenInfoFiles)) {
                        double maxBoxArea = Double.MIN_VALUE;
                        int listIndex = -1;
                        int subListIndex = -1;
                        // Get all corner point files.
                        List<List<List<Point>>> allCornerPoints = new ArrayList<>();
                        for (int i = 0; i < lumenInfoFiles.size(); i++) {
                            allCornerPoints.add(new ArrayList<>());
                            if (lumenInfoFiles.get(i) != null) {
                                for (int j = 0; j < lumenInfoFiles.get(i).size(); j++) {
//                                List<Point> cornerPoints = IO.getCornerPoints(
//                                        lumenInfoFiles.get(i).get(j));
                                    List<Point> cornerPoints = getTightCorners(
                                            IO.getOutlinePoints(lumenInfoFiles.get(i).get(j)));
                                    allCornerPoints.get(allCornerPoints.size() - 1)
                                            .add(cornerPoints);
                                    // Identify the index of the lumen whose
                                    // bounding box has the largest area.
                                    if (getRectangleArea(cornerPoints) > maxBoxArea) {
                                        maxBoxArea = getRectangleArea(cornerPoints);
                                        listIndex = i;
                                        subListIndex = j;
                                    }
                                }
                            }
                        }
                        // Pick all those lumens whose boxes share a portion
                        // with the largest lumen box.
                        List<List<File>> overlappingLumenInfoFiles
                                = new ArrayList<>();
                        List<List<List<Point>>> overlappingCornersPoints
                                = new ArrayList<>();
                        List<List<File>> remainingLumenInfoFiles
                                = new ArrayList<>();
                        List<List<List<Point>>> remainingCornersPoints
                                = new ArrayList<>();
                        for (int i = 0; i < allCornerPoints.size(); i++) {
                            overlappingLumenInfoFiles.add(new ArrayList<>());
                            overlappingCornersPoints.add(new ArrayList<>());
                            remainingLumenInfoFiles.add(new ArrayList<>());
                            remainingCornersPoints.add(new ArrayList<>());
                            for (int j = 0; j < allCornerPoints.get(i).size(); j++) {
                                if (areOverlapping(
                                        allCornerPoints
                                                .get(i)
                                                .get(j),
                                        allCornerPoints
                                                .get(listIndex)
                                                .get(subListIndex))) {
                                    overlappingLumenInfoFiles
                                            .get(overlappingLumenInfoFiles.size() - 1)
                                            .add(lumenInfoFiles.get(i).get(j));
                                    overlappingCornersPoints
                                            .get(overlappingLumenInfoFiles.size() - 1)
                                            .add(allCornerPoints.get(i).get(j));
                                } else {
                                    remainingLumenInfoFiles
                                            .get(overlappingLumenInfoFiles.size() - 1)
                                            .add(lumenInfoFiles.get(i).get(j));
                                    remainingCornersPoints
                                            .get(overlappingLumenInfoFiles.size() - 1)
                                            .add(allCornerPoints.get(i).get(j));
                                }
                            }
                        }
                        // Remove these lumen slices and their corners from the
                        // original structures
                        lumenInfoFiles = remainingLumenInfoFiles;
                        allCornerPoints = remainingCornersPoints;
                        // Create output directory if it does not exist
                        File outputDir = new File(outputDirPath);
                        if (!outputDir.exists()) {
                            outputDir.mkdirs();
                        }
                        // Get lumen boudary information from all overlapping
                        // info file.
                        List<Point3D> lumen3dPoints = new ArrayList<>();
                        for (List<File> samePlaneLumenInfoFiles : overlappingLumenInfoFiles) {
                            for (File lumenInfoFile : samePlaneLumenInfoFiles) {
                                List<Point> outlinePoints
                                        = IO.getOutlinePoints(lumenInfoFile);
                                RowColumnFieldPlane rowColumnFieldPlaneInfo
                                        = new RowColumnFieldPlane(lumenInfoFile);
                                for (Point outlinePoint : outlinePoints) {
                                    lumen3dPoints.add(
                                            new Point3D(
                                                    outlinePoint.getX(),
                                                    outlinePoint.getY(),
                                                    rowColumnFieldPlaneInfo.getPlane()));
                                }
                            }
                        }
                        // Print overlapping lumen boundaries to a file
                        PrintWriter printer = null;
                        try {
                            File lumenBoundaryFile = new File(
                                    String.format(
                                            outputDir.getPath() + File.separator + "lumen-boundaries-%02d.txt",
                                            lumenCounter + 1));
                            lumenBoundaryFilesList.add(lumenBoundaryFile);
                            printer = new PrintWriter(lumenBoundaryFile);
                            for (int i = 0; i < lumen3dPoints.size(); i++) {
                                // Notice how X, Y and Z coordinates are
                                // adjusted to their actual micron measurements.
                                printer.format("%7.3f %7.3f %7.3f%n",
                                        lumen3dPoints.get(i).getX() * pixelLengthInMicrons,
                                        lumen3dPoints.get(i).getY() * pixelLengthInMicrons,
                                        (lumen3dPoints.get(i).getZ() - 1) * verticalInterPlaneGap);
                            }
                        } catch (IOException ex) {
                            System.out.println(ex.toString());
                        } finally {
                            if (printer != null) {
                                printer.close();
                            }
                        }
                        // Print overlapping lumen corners to a file
                        printer = null;
                        try {
                            printer = new PrintWriter(new File(
                                    String.format(
                                            outputDir.getPath() + File.separator + "lumen-corners-%02d.txt",
                                            lumenCounter + 1)));
                            for (int i = 0; i < overlappingCornersPoints.size(); i++) {
                                for (int j = 0; j < overlappingCornersPoints.get(i).size(); j++) {
                                    for (int k = 0; k < overlappingCornersPoints.get(i).get(j).size(); k++) {
                                        RowColumnFieldPlane rowColumnFieldPlaneInfo
                                                = new RowColumnFieldPlane(overlappingLumenInfoFiles.get(i).get(j));
                                        // Notice how X, Y and Z coordinates are
                                        // adjusted to their actual micron
                                        // measurements.
                                        printer.format("%7.3f %7.3f %7.3f%n",
                                                overlappingCornersPoints.get(i).get(j).get(k).getX() * pixelLengthInMicrons,
                                                overlappingCornersPoints.get(i).get(j).get(k).getY() * pixelLengthInMicrons,
                                                1.0 * (rowColumnFieldPlaneInfo.getPlane() - 1)
                                                * verticalInterPlaneGap);
                                    }
                                }
                            }
                        } catch (IOException ex) {
                            System.out.println(ex.toString());
                        } finally {
                            if (printer != null) {
                                printer.close();
                            }
                        }
                        lumenCounter++;
                    }
                }
            }
        }
        return lumenBoundaryFilesList;
    }

    /**
     * Return the area of a rectangle.
     *
     * @param corners corner point of a rectangle
     * @return area of the rectangle
     */
    private double getRectangleArea(List<Point> corners) {
        if (corners.size() != 4) {
            throw new UnsupportedOperationException(
                    "Box corners must be 4 distinct points.");
        }
        return Math.abs(corners.get(0).getX() - corners.get(1).getX())
                * Math.abs(corners.get(1).getY() - corners.get(2).getY());
    }

    public static boolean areOverlapping(
            List<Point> box1Corners,
            List<Point> box2Corners) {
        if (box1Corners.get(0).getX() > box2Corners.get(1).getX()
                || box2Corners.get(0).getX() > box1Corners.get(1).getX()
                || box1Corners.get(0).getY() > box2Corners.get(2).getY()
                || box2Corners.get(0).getY() > box1Corners.get(2).getY()) {
            return false;
        }
        return true;
    }

    /**
     * Dispose the lumenDetector (connection to MATLAB)
     */
    public void finalize() {
        if (lumenDetector != null) {
            lumenDetector.dispose();
        }
    }

    public static List<Point> getTightCorners(List<Point> outlinePoints) {
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;
        for (int i = 0; i < outlinePoints.size(); i++) {
            if (outlinePoints.get(i).getX() < minX) {
                minX = outlinePoints.get(i).getX();
            }
            if (outlinePoints.get(i).getX() > maxX) {
                maxX = outlinePoints.get(i).getX();
            }
            if (outlinePoints.get(i).getY() < minY) {
                minY = outlinePoints.get(i).getY();
            }
            if (outlinePoints.get(i).getY() > maxY) {
                maxY = outlinePoints.get(i).getY();
            }
        }
        List<Point> tightCorners = new ArrayList<>();
        tightCorners.add(new Point((int) minX, (int) minY));
        tightCorners.add(new Point((int) maxX, (int) minY));
        tightCorners.add(new Point((int) maxX, (int) maxY));
        tightCorners.add(new Point((int) minX, (int) maxY));
        return tightCorners;
    }

    private boolean hasMoreFiles(List<List<File>> lumenInfoFiles) {
        for (List<File> sameLayerlumenInfoFileList : lumenInfoFiles) {
            if (sameLayerlumenInfoFileList != null && !sameLayerlumenInfoFileList.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
