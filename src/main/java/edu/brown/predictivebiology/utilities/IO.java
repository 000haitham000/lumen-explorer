/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.brown.predictivebiology.utilities;

import edu.brown.predictivebiology.cellcount.Record;
import java.awt.Point;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Haitham
 */
public class IO {

    /**
     * Parses an input text file into a lists of Record objects
     *
     * @param file text input file
     * @return List of Record objects
     * @throws IOException If an I/O problem occurs during parsing
     */
    public static List<Record> readRecords(File file) throws IOException {
        BufferedReader reader = null;
        try {
            // Create the list to be returned.
            List<Record> records = new ArrayList<>();
            // Open a stream to the input file.
            reader = new BufferedReader(new FileReader(file));
            // Skip all the lines before the actual records (until the [Data]
            // tag is encountered (case in-sensitive)).
            while (!reader.readLine().equalsIgnoreCase("[data]")) {
            }
            // Now the [data] tag in encountered. The next line has the
            // columns headers. Go ahead and parse them.
            String[] colHeaders = reader.readLine().split("\\t+");
            // Identify the order of each of the four main columns (row,
            // column, plane and field) in the input data.
            int rowHeaderIndex = getIndexIgnoreCase("row", colHeaders);
            int colHeaderIndex = getIndexIgnoreCase("column", colHeaders);
            int planeHeaderIndex = getIndexIgnoreCase("plane", colHeaders);
            int fieldHeaderIndex = getIndexIgnoreCase("field", colHeaders);
            // Create a record object from each line of the input text.
            String line;
            while ((line = reader.readLine()) != null) {
                String[] lineData = line.split("\\t+");
                Record record = new Record(
                        Integer.parseInt(lineData[rowHeaderIndex]),
                        Integer.parseInt(lineData[colHeaderIndex]),
                        Integer.parseInt(lineData[planeHeaderIndex]),
                        Integer.parseInt(lineData[fieldHeaderIndex]));
                // Add meta information.
                for (int i = 0; i < lineData.length; i++) {
                    if (i != rowHeaderIndex
                            && i != colHeaderIndex
                            && i != planeHeaderIndex
                            && i != fieldHeaderIndex) {
                        record.setMetaInfo(colHeaders[i], lineData[i]);
                    }
                }
                // Add the completed record to the list of records.
                records.add(record);
            }
            // Return the list of records
            return records;
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    /**
     * Get the index of the specified column header among all other headers
     *
     * @param colHeader the header of the column in question
     * @param colHeaders all headers
     * @return the index of colHeader in colHeaders
     */
    private static int getIndexIgnoreCase(String colHeader, String[] colHeaders) {
        int index = 0;
        for (String header : colHeaders) {
            if (colHeader.equalsIgnoreCase(header)) {
                break;
            }
            index++;
        }
        if (index == colHeaders.length) {
            return -1;
        } else {
            return index;
        }
    }

    /**
     * Returns all the files in a directory and in its direct and indirect
     * subdirectories, whose extension is specified in the list of extensions
     * specified in the filter.
     *
     * @param root the root directory. If root is a file it is added to the list
     * and the list will be returned having only one file.
     * @param extensionsFilter a list of allowed extensions. Can be null if no
     * files are to be excluded based on their extension.
     * @return all the files found in the subtree of the specified directory
     * filtered by their extensions.
     */
    public static List<File> getAllFilesRecursively(
            File root,
            List<String> extensionsFilter) {
        List<File> allFiles = new ArrayList<>();
        getAllFilesRecursively(root, allFiles, extensionsFilter);
        return allFiles;
    }

    /**
     * The utility function doing all the work for its interface (public)
     * counterpart function.
     */
    private static void getAllFilesRecursively(
            File root,
            List<File> allFiles,
            List<String> extensionsFilter) {
        if (root.isFile()) {
            if (extensionsFilter == null
                    || extensionsFilter.contains(
                            getFileExtension(root).toLowerCase())) {
                allFiles.add(root);
            }
        } else if (root.isDirectory()) {
            File[] subFiles = root.listFiles();
            // listFiles() can return null if the abstract filename does not
            // denote a directory. I encountered such a case with one of the
            // subdirectories of $RECYCLE.BIN
            if (subFiles != null) {
                for (File each : subFiles) {
                    getAllFilesRecursively(each, allFiles, extensionsFilter);
                }
            }
        }
    }

    /**
     * Gets the extension of a file e.g. for mine.txt it returns txt
     *
     * @param file the file whose extension is sought
     * @return the extension of a file
     */
    public static String getFileExtension(File file) {
        if (file.isFile()) {
            StringBuilder extSb = new StringBuilder();
            int index = file.getName().length() - 1;
            while (index >= 0 && file.getName().charAt(index) != '.') {
                if (file.getName().charAt(index) == File.separatorChar) {
                    return "";
                }
                extSb.append(file.getName().charAt(index--));
            }
            return extSb.reverse().toString();
        } else {
            throw new IllegalArgumentException(
                    "\"" + file.getPath() + "\" is not a file.");
        }
    }

    /**
     * Parses a pre-classified lumen info to extract its outline points
     *
     * @param lumenInfoFile a pre-classify lumen information file
     * @return a list of points outlining the lumen
     * @throws java.io.FileNotFoundException
     */
    public static List<Point> getOutlinePoints(File lumenInfoFile)
            throws
            FileNotFoundException, IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(lumenInfoFile));
            List<Point> pointsList = new ArrayList<>();
            String line;
            int dashedLinesEncountered = 0;
            while ((line = reader.readLine()) != null) {
                if (line.equals("---")) {
                    dashedLinesEncountered++;
                    continue;
                }
                if (dashedLinesEncountered == 2) {
                    String[] splits = line.split(" ");
                    pointsList.add(
                            new Point(
                                    Integer.parseInt(splits[0]),
                                    Integer.parseInt(splits[1])));
                }
            }
            return pointsList;
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    public static List<Point> getCornerPoints(File lumenInfoFile)
            throws
            FileNotFoundException, IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(lumenInfoFile));
            List<Point> pointsList = new ArrayList<>();
            String line;
            int dashedLinesEncountered = 0;
            while ((line = reader.readLine()) != null) {
                if (line.equals("---")) {
                    dashedLinesEncountered++;
                    continue;
                }
                if (dashedLinesEncountered == 3) {
                    String[] splits = line.split(" ");
                    pointsList.add(
                            new Point(
                                    Integer.parseInt(splits[0]),
                                    Integer.parseInt(splits[1])));
                }
            }
            return pointsList;
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    public static void writeMatlabVolumetricPlots(File outDir) throws FileNotFoundException, IOException, URISyntaxException {
        // Collect MATLAB files (and their license)
        File[] filesToCopy = new File[7];
        filesToCopy[0] = new File(IO.class.getResource("/matlab-files/groupIntoLayers.m").toURI());
        filesToCopy[1] = new File(IO.class.getResource("/matlab-files/getPointsDiff.m").toURI());
        filesToCopy[2] = new File(IO.class.getResource("/matlab-files/getAllPoints.m").toURI());
        filesToCopy[3] = new File(IO.class.getResource("/matlab-files/composeExtraLayer.m").toURI());
        filesToCopy[4] = new File(IO.class.getResource("/matlab-files/complement.m").toURI());
        filesToCopy[5] = new File(IO.class.getResource("/matlab-files/calculateVolumeAndArea.m").toURI());
        filesToCopy[6] = new File(IO.class.getResource("/matlab-files/main_script.m").toURI());
        // Write the files to the file system
        for (File inFile : filesToCopy) {
            File outFile = new File(outDir.getPath() + File.separator + inFile.getName());
            Files.copy(inFile.toPath(), outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static void writeVolumesAndSurfaceAreas(
            File bounadyFile,
            double volume, 
            double surfaceArea) throws IOException {
        PrintWriter printer = null;
        try {
            Pattern intPattern = Pattern.compile("\\d+");
            Matcher intMatcher = intPattern.matcher(bounadyFile.getName());
            intMatcher.find();
            printer = new PrintWriter(new File(
                    bounadyFile.getParentFile().getPath()
                    + File.separator
                    + String.format("lumen-volumetric-analysis-%s.txt",
                            intMatcher.group())));
            printer.println(String.format("volume = %-14.7f", volume));
            printer.println(String.format("area = %-14.7f", surfaceArea));
        } finally {
            if (printer != null) {
                printer.close();
            }
        }
    }
}
