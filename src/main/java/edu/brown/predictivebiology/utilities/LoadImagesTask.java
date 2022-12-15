/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.brown.predictivebiology.utilities;

import edu.brown.predictivebiology.db.DbUtilities;
import edu.brown.predictivebiology.db.beans.Directory;
import edu.brown.predictivebiology.db.beans.Image;
import edu.brown.predictivebiology.gui.GuiUtility;
import java.awt.Toolkit;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import edu.brown.predictivebiology.utilities.IO;

/**
 *
 * @author Haitham
 */
public class LoadImagesTask extends SwingWorker<List<Image>, Void> {

    private final Directory rootDir;
    private final JDialog progressDialog;
    private final JLabel loadImagesProgressLabel;
    private final JButton loadImagesProgressButton;
    private final List<String> extensionsFilter;
    private final JList<String> directoriesList;
    private final JList<String> imagesList;

    public LoadImagesTask(
            Directory rootDir,
            JDialog progressDialog,
            JLabel loadImagesProgressLabel,
            JButton loadImagesProgressButton,
            List<String> extensionsFilter,
            JList<String> directoriesList,
            JList<String> imagesList) {
        this.rootDir = rootDir;
        this.progressDialog = progressDialog;
        this.loadImagesProgressLabel = loadImagesProgressLabel;
        this.loadImagesProgressButton = loadImagesProgressButton;
        this.extensionsFilter = extensionsFilter;
        this.directoriesList = directoriesList;
        this.imagesList = imagesList;
    }

    /*
         * Main task. Executed in background thread.
     */
    @Override
    public List<Image> doInBackground() {
        // Retrieve all the file by recursively traversing the file system
        List<File> allFiles = IO.getAllFilesRecursively(
                new File(rootDir.getPath()),
                extensionsFilter);
        // Add all the files to the database
        for (File file : allFiles) {
            try {
                Image image = new Image();
                image.setPath(file.getPath());
                image.setParentDirId(rootDir.getId());
                DbUtilities.addImage(image);
            } catch (ClassNotFoundException
                    | SQLException
                    | NoSuchMethodException
                    | InstantiationException
                    | IllegalAccessException
                    | InvocationTargetException ex) {
                Logger.getLogger(LoadImagesTask.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(
                        progressDialog,
                        String.format(
                                "Unable to insert the image to the database: %s",
                                ex.toString(),
                                "Database Error",
                                JOptionPane.ERROR_MESSAGE));
                System.exit(-1);
            }
        }
        // Update the imageslist
        try {
            GuiUtility.updateImagesList(directoriesList, imagesList);
        } catch (ClassNotFoundException
                | SQLException
                | NoSuchMethodException
                | InstantiationException
                | IllegalAccessException
                | InvocationTargetException ex) {
            Logger.getLogger(LoadImagesTask.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(
                    progressDialog,
                    String.format(
                            "Unable to load images from the database: %s",
                            ex.toString(),
                            "Database Error",
                            JOptionPane.ERROR_MESSAGE));
            System.exit(-1);
        }
        // Show that the process has been completed in the progress bar
        setProgress(100);
        if (loadImagesProgressLabel != null) {
            loadImagesProgressLabel.setText(
                    loadImagesProgressLabel.getText() + " DONE");
        }
        try {
            // Get all images of the current directory
            return DbUtilities.getImagesOfDirectory(rootDir.getId());
        } catch (ClassNotFoundException
                | SQLException
                | NoSuchMethodException
                | InstantiationException
                | IllegalAccessException
                | InvocationTargetException ex) {
            Logger.getLogger(LoadImagesTask.class.getName()).log(Level.SEVERE, null, ex);
        }
        // Shouldn't reach this point
        throw new IllegalArgumentException("Something went wrong!");
    }

    /**
     * Executed in event dispatch thread
     */
    public void done() {
        Toolkit.getDefaultToolkit().beep();
        if (loadImagesProgressButton != null) {
            loadImagesProgressButton.setEnabled(true);
        }
//        this.progressDialog.dispose();
    }
}
