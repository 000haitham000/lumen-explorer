/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.brown.predictivebiology.gui;

import com.haithamseada.components.ImagePanel;
import com.haithamseada.components.ScrollImagePanel;
import edu.brown.predictivebiology.db.DbUtilities;
import edu.brown.predictivebiology.db.beans.Coordinates;
import edu.brown.predictivebiology.db.beans.Directory;
import edu.brown.predictivebiology.db.beans.Experiment;
import edu.brown.predictivebiology.db.beans.Image;
import edu.brown.predictivebiology.db.beans.WellDescription;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import edu.brown.predictivebiology.utilities.FileTraversal;
import edu.brown.predictivebiology.utilities.IO;
import edu.brown.predictivebiology.utilities.LoadImagesTask;
import edu.brown.predictivebiology.utilities.LumenMap;
import edu.brown.predictivebiology.utilities.MachineLearning;

/**
 *
 * @author Haitham
 */
public class GuiUtility {

    private static FileTraversal fileTraversal;
    public static Image currentlyDisplayedImage;
    private static final List<String> EXTENSIONS_FILTER;

    static {
        fileTraversal = new FileTraversal();
        // Initialize the list of image extensions we are interested in
        EXTENSIONS_FILTER = new ArrayList<>();
        EXTENSIONS_FILTER.add("png");
    }

    public static void loadExperiments(
            JList<String> experimentsList,
            JList<String> directoriesList) {
        try {
            // Create default models of the two lists
            experimentsList.setModel(new DefaultListModel<>());
            directoriesList.setModel(new DefaultListModel<>());
            // Add all the experiments from the database
            List<Experiment> allExperiments = DbUtilities.getAllExperiments();
            addExperiments2List(allExperiments, experimentsList);
            // If there is at least one experiment in the databse
            if (allExperiments.size() > 0) {
                // Select the first item (experiment) in the experiments list
                experimentsList.setSelectedIndex(0);
                // Retrieve the directories of the first experiment
                List<Directory> directoriesOfExperiment
                        = DbUtilities.getDirectoriesOfExperiment(
                                allExperiments.get(0).getId());
                // Empty the directories list then add the retrieved items to it
                ((DefaultListModel<String>) directoriesList.getModel()).clear();
                addDirectories2List(directoriesOfExperiment, directoriesList);
            }
        } catch (ClassNotFoundException
                | SQLException
                | NoSuchMethodException
                | InstantiationException
                | IllegalAccessException
                | InvocationTargetException ex) {
            Logger.getLogger(GuiUtility.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void addExperiments2List(
            List<Experiment> experiments, JList<String> list) {
        for (Experiment experiment : experiments) {
            ((DefaultListModel<String>) list.getModel()).addElement(
                    String.format(
                            "%s|%s",
                            experiment.getId().toString(),
                            experiment.getName()));
        }
        list.validate();
    }

    public static void addDirectories2List(
            List<Directory> directories, JList<String> list) {
        for (Directory directory : directories) {
            ((DefaultListModel<String>) list.getModel()).addElement(
                    String.format(
                            "%s|%s",
                            directory.getId().toString(),
                            directory.getPath()));
        }
        list.validate();
    }

    public static void addImages2List(
            List<Image> images, JList<String> list) {
        System.out.println("Images Count : " + images.size());
        int counter = 1;
        for (Image image : images) {
            ((DefaultListModel<String>) list.getModel()).addElement(
                    String.format(
                            "%s|%s",
                            image.getId().toString(),
                            image.getPath()));
            System.out.println(counter++);
        }
        list.validate();
    }

    public static void updateButtonsStatus(
            JList<String> experimentsList,
            JList<String> directoriesList,
            JList<String> imagesList,
            JButton removeExperimentButton,
            JButton addDirectoryButton,
            JButton removeDirectoryButton,
            //JButton addImageButton,
            JButton removeImageButton,
            JButton filterButton,
            JButton fixPathButton) {
        if (experimentsList.isSelectionEmpty()) {
            removeExperimentButton.setEnabled(false);
            addDirectoryButton.setEnabled(false);
            fixPathButton.setEnabled(false);
        } else {
            removeExperimentButton.setEnabled(true);
            fixPathButton.setEnabled(true);
            if (experimentsList.getSelectedIndices().length == 1) {
                addDirectoryButton.setEnabled(true);
            } else {
                addDirectoryButton.setEnabled(false);
            }
        }
        if (directoriesList.isSelectionEmpty()) {
            removeDirectoryButton.setEnabled(false);
            //addImageButton.setEnabled(false);
        } else {
            removeDirectoryButton.setEnabled(true);
            if (directoriesList.getSelectedIndices().length == 1) {
                //addImageButton.setEnabled(true);
            } else {
                //addImageButton.setEnabled(false);
            }
        }
        if (imagesList.isSelectionEmpty()) {
            removeImageButton.setEnabled(false);
        } else {
            removeImageButton.setEnabled(true);
        }
        if (experimentsList.getSelectedIndices().length == 1) {
            filterButton.setEnabled(true);
        } else {
            filterButton.setEnabled(false);
        }
    }

    /**
     * Update the GUI list of experiments with all the currently available
     * experiments in the database.
     *
     * @param experimentsList GUI list of experiments
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static void updateExperimentsList(JList experimentsList)
            throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        ((DefaultListModel<String>) experimentsList.getModel()).clear();
        List<Experiment> allExperiments = DbUtilities.getAllExperiments();
        addExperiments2List(allExperiments, experimentsList);
    }

    /**
     * Update the GUI list of directories according to the current selection
     * status of the GUI list of experiments. If no experiments are selected, or
     * if more one experiment are selected the GUI list of directories is
     * cleared, otherwise (i.e. if only one experiment is selected), all its
     * directories should be listed.
     *
     * @param experimentsList GUI list of experiments
     * @param directoriesList GUI list of directories
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static void updateDirectoriesList(
            JList<String> experimentsList,
            JList<String> directoriesList)
            throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        ((DefaultListModel<String>) directoriesList.getModel()).clear();
        List<Integer> expIds = GuiUtility.getSelectedIds(experimentsList);
        if (expIds.size() == 1) {
            List<Directory> directoriesOfExperiment
                    = DbUtilities.getDirectoriesOfExperiment(expIds.get(0));
            addDirectories2List(directoriesOfExperiment, directoriesList);
        }
    }

    /**
     * Update the GUI list of images based on the selection status of the GUI
     * list of directories. If one or more directories are selected, all their
     * images are listed, otherwise (i.e. if no directories are selected), the
     * GUI list of images is cleared.
     *
     * @param directoriesList GUI list of directories
     * @param imagesList GUI list of images
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static void updateImagesList(
            JList<String> directoriesList,
            JList<String> imagesList)
            throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        DefaultListModel<String> model = new DefaultListModel<>();
        List<Integer> dirIds = GuiUtility.getSelectedIds(directoriesList);
        if (dirIds.size() > 0) {
            for (Integer dirId : dirIds) {
                List<Image> imagesOfDirectory
                        = DbUtilities.getImagesOfDirectory(dirId);
                for (Image image : imagesOfDirectory) {
                    model.addElement(String.format(
                            "%d|%s",
                            image.getId(),
                            image.getPath()));
                }
            }
        }
        imagesList.setModel(model);
    }

    /**
     * Updates the three GUI lists; experiments, directories and images.
     * Experiments GUI list is first updated according to the experiments
     * currently available (stored) in the database. The GUI list of directories
     * is then updated based on the selection status of the GUI list of
     * experiments and what directories are available in the database. Finally,
     * the GUI list of images is updated based on the selection status of the
     * GUI list of directories and what images are available in the database.
     *
     * @param experimentsList GUI list of experiments
     * @param directoriesList GUI list of directories
     * @param imagesList GUI list of images
     * @throws java.lang.ClassNotFoundException
     * @throws java.sql.SQLException
     * @throws java.lang.NoSuchMethodException
     * @throws java.lang.InstantiationException
     * @throws java.lang.IllegalAccessException
     * @throws java.lang.reflect.InvocationTargetException
     */
    public static void updateExpDirImageLists(
            JList<String> experimentsList,
            JList<String> directoriesList,
            JList<String> imagesList)
            throws
            ClassNotFoundException,
            SQLException,
            NoSuchMethodException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException {
        GuiUtility.updateExperimentsList(experimentsList);
        GuiUtility.updateDirectoriesList(experimentsList, directoriesList);
        GuiUtility.updateImagesList(directoriesList, imagesList);
    }

    static void clearList(JList<String> list) {
        list.setModel(new DefaultListModel<>());
    }

    static void centerDialogOnScreen(JDialog dialog) {
        final Toolkit toolkit = Toolkit.getDefaultToolkit();
        final Dimension screenSize = toolkit.getScreenSize();
        final int x = (screenSize.width - dialog.getWidth()) / 2;
        final int y = (screenSize.height - dialog.getHeight()) / 2;
        dialog.setLocation(x, y);
    }

    /**
     * Makes sure the cell underlying the right click is selected. The rest of
     * the code updates the enable/disable state of all pop-up menu items
     * according to the selection.
     *
     * @param wellMapTable the table showing the plate map
     * @param wellPopupMenu the pop-up menu of wellMapTable
     */
    static void updateWellPlateSelectionAndPopupMenuItems(
            JTable wellMapTable,
            JPopupMenu wellPopupMenu) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Get the coordinates of the top left corner of the pop-up menu
                // on the underlying table
                Point pointOnTable = SwingUtilities.convertPoint(wellPopupMenu,
                        new Point(0, 0),
                        wellMapTable);
                int rowAtPoint = wellMapTable.rowAtPoint(pointOnTable);
                int colAtPoint = wellMapTable.columnAtPoint(pointOnTable);
                if (rowAtPoint > -1 && colAtPoint > -1) {
                    wellMapTable.addRowSelectionInterval(rowAtPoint, rowAtPoint);
                    wellMapTable.addColumnSelectionInterval(colAtPoint, colAtPoint);
                }
                // If nothing is slected disable all pop-up menu items. This
                // case should never happen unless the code above is changed.
                // Until the time of writing this code, the code above will make
                // sure that at least one cell is selected (the cell at which
                // the pop-up menu right click is made)
                if (wellMapTable.getSelectedRowCount() == 0
                        && wellMapTable.getSelectedRowCount() == 0) {
                    // Disable all pop-up menu items
                    wellPopupMenu.getComponent(0).setEnabled(false); // Edit
                    wellPopupMenu.getComponent(1).setEnabled(false); // Copy
                    wellPopupMenu.getComponent(2).setEnabled(false); // Paste
                    wellPopupMenu.getComponent(3).setEnabled(false); // Delete
                } else {
                    // If more than one item are selected disable the copy menu
                    // item and the edit
                    if (wellMapTable.getSelectedRowCount() > 1
                            || wellMapTable.getSelectedColumnCount() > 1) {
                        // Enable all pop-up menu items except (copy info.)
                        wellPopupMenu.getComponent(0).setEnabled(false); // Edit
                        wellPopupMenu.getComponent(1).setEnabled(false); // Copy
                    } else {
                        // Enable all pop-up menu items
                        wellPopupMenu.getComponent(0).setEnabled(true); // Edit
                        wellPopupMenu.getComponent(1).setEnabled(true); // Copy
                    }
                    // Highlight (paste) only if something has been copied
                    if (((WellTableModel) wellMapTable.getModel()).getCopiedWell() != null) {
                        wellPopupMenu.getComponent(2).setEnabled(true); // Paste
                    } else {
                        wellPopupMenu.getComponent(2).setEnabled(false); // Paste
                    }
                    wellPopupMenu.getComponent(3).setEnabled(true); // Delete
                }
            }
        });
    }

    /**
     * Makes sure that the closest list item to the right-click is selected,
     * then updates the pop-up menu items accordingly.
     *
     * @param experimentsList the GUI list of experiments
     * @param experimentPopupMenu the pop-up menu of the experimentsList
     */
    static void updateExperimentListSelectionAndPopupMenuItems(
            JList experimentsList,
            JPopupMenu experimentPopupMenu) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Get the coordinates of the top left corner of the pop-up menu
                // on the underlying table
                Point pointOnTable = SwingUtilities.convertPoint(
                        experimentPopupMenu,
                        new Point(0, 0),
                        experimentsList);
                int itemIndexAtPoint = experimentsList.locationToIndex(
                        pointOnTable);
                if (itemIndexAtPoint != -1) {
                    experimentsList.setSelectedIndex(itemIndexAtPoint);
                }
                // If nothing is slected disable all pop-up menu items. This
                // case should never happen unless the code above is changed.
                // Until the time of writing this code, the code above will make
                // sure that at least one cell is selected (the cell at which
                // the pop-up menu right click is made)
                if (experimentsList.getSelectedIndices().length == 1) {
                    // Disable all pop-up menu items
                    experimentPopupMenu.getComponent(0).setEnabled(true); // Open Plate Map
                    experimentPopupMenu.getComponent(1).setEnabled(true); // Edit Plate Info
                } else {
                    // Enable all pop-up menu items
                    experimentPopupMenu.getComponent(0).setEnabled(false); // Open Plate Map
                    experimentPopupMenu.getComponent(1).setEnabled(false); // Edit Plate Info
                }
            }
        });
    }

    static void saveWellInfo(
            JList<String> experimentsList,
            JTable wellMapTable,
            JTextField wellExperimentIdTextField,
            JTextField wellCompoundTextField,
            JFormattedTextField wellConcentrationFormattedTextField,
            JTextField wellCellTypeTextField,
            JFormattedTextField wellCellCountFormattedTextField)
            throws NumberFormatException {
        // Create an object using all the information inserted in the form
        WellDescription newWell = new WellDescription();
        newWell.setPlateId(new Integer(
                experimentsList.getSelectedValue().split("\\|")[0]));
        newWell.setRow(wellMapTable.getSelectedRow() + 1);
        newWell.setCol(wellMapTable.getSelectedColumn() + 1);
        newWell.setCompound(wellCompoundTextField.getText().trim());
        newWell.setConcentration(
                (Double) wellConcentrationFormattedTextField.getValue());
        newWell.setCellType(wellCellTypeTextField.getText().trim());
        newWell.setCellCount(
                (Integer) wellCellCountFormattedTextField.getValue());
        try {
            // Load the same well from the database
            WellDescription dbWell = DbUtilities.getWell(
                    newWell.getPlateId(), newWell.getRow(), newWell.getCol());
            // If the well exists, update its information, otherwise insert it
            if (dbWell != null) {
                DbUtilities.updateWellDescription(dbWell, newWell);
            } else {
                DbUtilities.addWellDescription(newWell);
            }
            // Update the GUI table
            wellMapTable.getModel().setValueAt(
                    newWell,
                    wellMapTable.getSelectedRow(),
                    wellMapTable.getSelectedColumn());
        } catch (ClassNotFoundException
                | SQLException
                | NoSuchMethodException
                | InstantiationException
                | IllegalAccessException
                | InvocationTargetException ex) {
            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Event code for clicking the open plate map menu item in the pop-up menu
     * of the GUI list of experiments.
     *
     * @param expList GUI list of experiments
     * @param wellMapTable the table the displays plate map
     * @param wellMapDialog the whole well plate map dialog
     * @param parentDialog the parent dialog of all messages to be displayed
     */
    static void openWellMapDialog(
            JList<String> expList,
            JTable wellMapTable,
            JDialog wellMapDialog,
            JDialog parentDialog) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Double-click detected
                int experimentId = new Integer(((String) expList
                        .getSelectedValue()).split("\\|")[0]);
                // Set cell renderer
                wellMapTable.setDefaultRenderer(
                        WellDescription.class,
                        new WellRenderer(experimentId));
                // Load well info. from the databse
                try {
                    Experiment experiment
                            = DbUtilities.getExperimentById(experimentId);
                    if (experiment.getRowCount() == 0
                            || experiment.getColCount() == 0) {
                        JOptionPane.showMessageDialog(
                                parentDialog,
                                "Please, edit plate info first. Neither the "
                                + "row count nor the column count can be zero.",
                                "Cannot Open Plate Map",
                                JOptionPane.ERROR_MESSAGE);
                    } else {
                        WellDescription[][] wells
                                = new WellDescription[experiment.getRowCount()][experiment.getColCount()];
                        List<WellDescription> wellsOfExperiment
                                = DbUtilities.getWellsOfExperiment(experimentId);
                        for (WellDescription well : wellsOfExperiment) {
                            wells[well.getRow() - 1][well.getCol() - 1] = well;
                        }
                        // Create the model then use it to update the table model
                        wellMapTable.setModel(new WellTableModel(wells));
                        // Change plate dialog size
                        wellMapDialog.setPreferredSize(new Dimension(800, 600));
                        for (int i = 0; i < wellMapTable.getColumnCount(); i++) {
                            wellMapTable.getColumnModel().getColumn(i)
                                    .setMinWidth(150);
                        }
                        wellMapDialog.pack();
                        // Center the dialog in the middle of the screen
                        GuiUtility.centerDialogOnScreen(wellMapDialog);
                        // Diplay the dialog
                        wellMapDialog.setVisible(true);
                    }
                } catch (ClassNotFoundException
                        | SQLException
                        | NoSuchMethodException
                        | InstantiationException
                        | IllegalAccessException
                        | InvocationTargetException ex) {
                    Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    static void wellEditMenuItemAction(
            JList<String> experimentsList,
            JTable wellMapTable,
            JDialog wellEditDialog,
            JTextField wellExperimentTextField,
            JTextField wellRowColumnTextField,
            JTextField wellCompoundTextField,
            JFormattedTextField wellConcentrationFormattedTextField,
            JTextField wellCellTypeTextField,
            JFormattedTextField wellCellCountFormattedTextField) throws NumberFormatException {
        // Get the ID of the selected Experiment
        String expName = experimentsList.getSelectedValue().split("\\|")[1];
        // Retreive all the wells of
        int selectedRow = wellMapTable.getSelectedRow();
        int selectedCol = wellMapTable.getSelectedColumn();
        // Update the information in the read-only field of the well info dialog
        wellExperimentTextField.setText(expName);
        wellRowColumnTextField.setText(String.format(
                "%d x %d", selectedRow + 1, selectedCol + 1));
        // Retreive other information - if any - from the model
        WellDescription well
                = (WellDescription) wellMapTable.getModel().
                        getValueAt(selectedRow, selectedCol);
        if (well != null) {
            wellCompoundTextField.setText(well.getCompound());
            wellConcentrationFormattedTextField.setValue(well.getConcentration());
            wellCellTypeTextField.setText(well.getCellType());
            wellCellCountFormattedTextField.setValue(well.getCellCount());
        }
        // Center the dialog
        GuiUtility.centerDialogOnScreen(wellEditDialog);
        // Display the dialog
        wellEditDialog.setVisible(true);
    }

    /**
     * Event code for clicking the copy item in the wells pop-up menu.
     *
     * @param wellMapTable the table showing the plate map
     */
    static void wellCopyMenuItemAction(JTable wellMapTable) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                WellDescription copiedWell
                        = (WellDescription) wellMapTable.getModel().getValueAt(
                                wellMapTable.getSelectedRow(),
                                wellMapTable.getSelectedColumn());
                ((WellTableModel) wellMapTable.getModel()).setCopiedWell(copiedWell);
            }
        });
    }

    /**
     * Displays the selected image after updating the image traversal.
     *
     * @param experimentsList the GUI list of experiments
     * @param imagesList the GUI list of images
     * @param previousImageButton previous image button
     * @param nextImageButton next image button
     * @param clearImageButton clear image button
     * @param experimentsExplorerDialog the whole experiments explorer dialog
     * @param lumenCounterLabel the label displaying current lumen count
     * @param scrollImagePanel the image display panel
     */
    static void jumpToSelectionAction(
            JList<String> experimentsList,
            JList<String> imagesList,
            JButton previousImageButton,
            JButton nextImageButton,
            JButton clearImageButton,
            JDialog experimentsExplorerDialog,
            JLabel lumenCounterLabel,
            ScrollImagePanel scrollImagePanel) {
        if (imagesList.getSelectedIndices().length == 1) {
            // Reset file traversal
            fileTraversal.resetIndex();
            // Walk through the traversal until the currently selected image is
            // reached
            while (fileTraversal.hasNext()) {
                fileTraversal.next();
                // Path of the current file in the traversal
                String currentTraversalPath = fileTraversal.getCurrent().getPath();
                // Path of the currently selected image (notice that the path is
                // boxed in a File object then extracted again. This ensures the
                // two pathes will have the same format if they are really the
                // same, in terms of presence of a last "/" for example)
                String currentSelectedImagePath = new File(
                        imagesList.getSelectedValue().split("\\|")[1]).getPath();
                if (currentTraversalPath.equals(currentSelectedImagePath)) {
                    if (fileTraversal.hasPrevious()) {
                        fileTraversal.previous();
                    } else {
                        fileTraversal.resetIndex();
                    }
                    break;
                }
            }
            // Jump to the selected image
            displayNext(fileTraversal, scrollImagePanel);
            // Update main dialog buttons
            previousImageButton.setEnabled(true);
            nextImageButton.setEnabled(true);
            clearImageButton.setEnabled(true);
            // Refresh lumen markings
            try {
                refreshLumenCountDisplay(experimentsList, lumenCounterLabel);
                refreshCoordinatesPlot(fileTraversal, scrollImagePanel);
            } catch (SQLException
                    | ClassNotFoundException ex) {
                Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
            // Close the dialog
            experimentsExplorerDialog.setVisible(false);
        }
    }

    /**
     * Display the next image in the current file traversal, or display an error
     * message if there are not more images to display.
     */
    static void displayNext(
            FileTraversal fileTraversal,
            ScrollImagePanel scrollImagePanel) {
        if (fileTraversal != null) {
            if (fileTraversal.size() == 0) {
                JOptionPane.showMessageDialog(
                        scrollImagePanel,
                        "There are no images in the input directory.",
                        "No input images",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (fileTraversal.hasNext()) {
                try {
                    currentlyDisplayedImage = fileTraversal.next();
                    System.out.println(currentlyDisplayedImage.getPath());
                    scrollImagePanel.setImage(ImageIO.read(
                            new File(currentlyDisplayedImage.getPath())));
                    addImageAnnotations(scrollImagePanel);
                    scrollImagePanel.validate();
                    scrollImagePanel.repaint();
                } catch (IOException ex) {
                    Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                JOptionPane.showMessageDialog(
                        scrollImagePanel,
                        "You have reached the last image in your input "
                        + "directory.",
                        "Last image reached",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    static void refreshDisplayedImage(
            FileTraversal fileTraversal,
            ScrollImagePanel scrollImagePanel) {
        if (fileTraversal != null) {
            if (fileTraversal.size() == 0) {
                JOptionPane.showMessageDialog(
                        scrollImagePanel,
                        "There are no images in the input directory.",
                        "No input images",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            try {
                currentlyDisplayedImage = fileTraversal.getCurrent();
                System.out.println(currentlyDisplayedImage.getPath());
                scrollImagePanel.setImage(ImageIO.read(
                        new File(currentlyDisplayedImage.getPath())));
                addImageAnnotations(scrollImagePanel);
                scrollImagePanel.validate();
                scrollImagePanel.repaint();
            } catch (IOException ex) {
                Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Display the next image in the current file traversal, or display an error
     * message if there are not more images to display.
     */
    static void displayPrevious(
            FileTraversal fileTraversal,
            ScrollImagePanel scrollImagePanel) {
        if (fileTraversal != null) {
            if (fileTraversal.size() == 0) {
                JOptionPane.showMessageDialog(
                        scrollImagePanel,
                        "There are no images in the input directory.",
                        "No input images",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (fileTraversal.hasPrevious()) {
                try {
                    currentlyDisplayedImage = fileTraversal.previous();
                    scrollImagePanel.setImage(ImageIO.read(
                            new File(currentlyDisplayedImage.getPath())));
                    addImageAnnotations(scrollImagePanel);
                    scrollImagePanel.validate();
                    scrollImagePanel.repaint();
                } catch (IOException ex) {
                    Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                JOptionPane.showMessageDialog(
                        scrollImagePanel,
                        "You have reached the first image in your input "
                        + "directory.",
                        "First image reached",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    /**
     * Tag a lumenal space on the current image by adding a marker to the image
     * and storing its location in the database.
     *
     * @param evt the mouse click event object
     */
    static void tagLumen(
            MouseEvent evt,
            ScrollImagePanel scrollImagePanel,
            JList<String> experimentsList,
            JLabel lumenCounterLabel) {
        try {
            Image image = DbUtilities.getImageById(fileTraversal.getCurrent().getId());
            // Store the tagged location in the database. Notice that 
            // xDisplacement and yDisplacement are subtracted from the actual
            // click coordinates becuase the image is displayed in the center of
            // the canvas instead of starting at the top left corner, this
            // causes displacements in x and y that needs to be accounted for
            // (i.e. subtracted) before storing the coordinates in the database.
            int xDisplacement = ((scrollImagePanel.getCanvas().getWidth() - scrollImagePanel.getImage().getWidth()) / 2);
            int yDisplacement = ((scrollImagePanel.getCanvas().getHeight() - scrollImagePanel.getImage().getHeight()) / 2);
            // Add coordinates to the database
            Coordinates coordinates = new Coordinates();
            coordinates.setImageId(image.getId());
            coordinates.setX(evt.getX() - xDisplacement);
            coordinates.setY(evt.getY() - yDisplacement);
            DbUtilities.addCoordinates(coordinates);
            // Re-plot all coordinates including the new one
            refreshCoordinatesPlot(GuiUtility.fileTraversal, scrollImagePanel);
            refreshLumenCountDisplay(experimentsList, lumenCounterLabel);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    static void refreshLumenCountDisplay(
            JList<String> experimentsList,
            JLabel lumenCounterLabel) {
        try {
            if (!experimentsList.isSelectionEmpty()) {
                // Get the ID of the selected Experiment
                Integer expId = new Integer(experimentsList.getSelectedValue().split("\\|")[0]);
                // Display current lumens count
                lumenCounterLabel.setText(String.format(
                        "%d lumens counted so far.", DbUtilities.getLumenCount(expId)));
            } else {
                lumenCounterLabel.setText("");
            }
        } catch (ClassNotFoundException
                | SQLException
                | NoSuchMethodException
                | InstantiationException
                | IllegalAccessException
                | InvocationTargetException ex) {
            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    static void refreshCoordinatesPlot(
            FileTraversal fileTraversal,
            ScrollImagePanel scrollImagePanel)
            throws SQLException, ClassNotFoundException {
        if (fileTraversal != null && fileTraversal.size() > 0) {
            try {
                // Retrieve all the tags of this image from the database
                List<Coordinates> imageCoordinates
                        = DbUtilities.getCoordinatesOfImage(
                                fileTraversal.getCurrent().getId());
                // Replot everything
                List<Point> taggingPoints = new ArrayList<>();
                for (Coordinates r : imageCoordinates) {
                    taggingPoints.add(new Point(r.getX(), r.getY()));
                }
                scrollImagePanel.setTaggingCoordinates(taggingPoints);
                scrollImagePanel.repaint();
                scrollImagePanel.validate();
            } catch (NoSuchMethodException ex) {
                Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvocationTargetException ex) {
                Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    static void refreshOutlinePlot(
            FileTraversal fileTraversal,
            ScrollImagePanel scrollImagePanel,
            LumenMap lumenMap)
            throws SQLException, ClassNotFoundException, IOException {
        if (lumenMap != null) {
            if (fileTraversal != null && fileTraversal.size() > 0) {
                RowColumnFieldPlane rcfp = new RowColumnFieldPlane(
                        new File(fileTraversal.getCurrent().getPath()));
                List<File> lumenInfoFileList
                        = lumenMap.getLumenInfoFiles(
                                rcfp.getRow(),
                                rcfp.getColumn(),
                                rcfp.getField(),
                                rcfp.getPlane());
                if (lumenInfoFileList != null) {
                    List<Point> outlinePoints = new ArrayList<>();
                    for (File lumenInfoFile : lumenInfoFileList) {
                        outlinePoints.addAll(IO.getOutlinePoints(lumenInfoFile));
                    }
                    scrollImagePanel.setOutlinePoints(outlinePoints);
                } else {
                    scrollImagePanel.clearOutlinePoints();

                }
                scrollImagePanel.repaint();
                scrollImagePanel.validate();
            }
        }
    }

    /**
     * Event code for clicking the paste item in the wells pop-up menu
     *
     * @param wellMapTable the table showing the plate map
     * @param experimentsList the GUI list of experiments
     */
    static void wellPasteMenuItemAction(
            JTable wellMapTable,
            JList<String> experimentsList) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                WellDescription copiedWell
                        = ((WellTableModel) wellMapTable.getModel()).getCopiedWell();
                int[] selectedRows = wellMapTable.getSelectedRows();
                int[] selectedColumns = wellMapTable.getSelectedColumns();
                try {
                    for (int i = 0; i < selectedRows.length; i++) {
                        for (int j = 0; j < selectedColumns.length; j++) {
                            int expId = new Integer(
                                    experimentsList.getSelectedValue().split("\\|")[0]);
                            int tableRow = selectedRows[i];
                            int tableCol = selectedColumns[j];
                            int dbRow = tableRow + 1;
                            int dbCol = tableCol + 1;
                            // Load the same well from the database
                            WellDescription dbWell = DbUtilities.getWell(
                                    expId,
                                    dbRow,
                                    dbCol);
                            WellDescription newWell = null;
                            // If you copied an EMPTY well then the paste becomes
                            // a delete
                            if (copiedWell == null) {
                                /* This part of the code is not reachable anymore after
                        changing the GUI of the pop-up menu. The change simply
                        disables the "Paste Information" pop-up menu item unless
                        some non-empty cell is copied. Which means that if
                        (copiedWell) is null, this whole method will not have
                        a chance to be called in the first place. However, this
                        part is left just in case any future changes to the GUI
                        allows copying an empty cell.
                                 */
                                // Delete the well if it already existed
                                if (dbWell != null) {
                                    DbUtilities.deleteWellDescription(dbWell);
                                }
                            } else {
                                newWell = new WellDescription();
                                newWell.setPlateId(expId);
                                newWell.setRow(dbRow);
                                newWell.setCol(dbCol);
                                newWell.setCompound(copiedWell.getCompound());
                                newWell.setConcentration(copiedWell.getConcentration());
                                newWell.setCellType(copiedWell.getCellType());
                                newWell.setCellCount(copiedWell.getCellCount());
                                // If the well exists, update its information, otherwise
                                // insert it
                                if (dbWell != null) {
                                    DbUtilities.updateWellDescription(dbWell, newWell);
                                } else {
                                    DbUtilities.addWellDescription(newWell);
                                }
                            }
                            wellMapTable.getModel().setValueAt(newWell, tableRow, tableCol);
                        }
                    }
                    ((AbstractTableModel) wellMapTable.getModel()).fireTableDataChanged();
                } catch (ClassNotFoundException
                        | SQLException
                        | NoSuchMethodException
                        | InstantiationException
                        | IllegalAccessException
                        | InvocationTargetException ex) {
                    Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    /**
     * Event code for clicking the delete item in the wells pop-up menu.
     *
     * @param wellMapTable the table showing the plate map
     * @param experimentsList the GUI list of experiments
     */
    static void wellDeleteMenuItemAction(
            JTable wellMapTable,
            JList<String> experimentsList) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                int[] selectedRows = wellMapTable.getSelectedRows();
                int[] selectedColumns = wellMapTable.getSelectedColumns();
                try {
                    for (int i = 0; i < selectedRows.length; i++) {
                        for (int j = 0; j < selectedColumns.length; j++) {
                            int expId = new Integer(
                                    experimentsList.getSelectedValue().split("\\|")[0]);
                            int tableRow = selectedRows[i];
                            int tableCol = selectedColumns[j];
                            int dbRow = tableRow + 1;
                            int dbCol = tableCol + 1;
                            // Delete the current cell from the database
                            WellDescription wellToBeDeleted = new WellDescription();
                            wellToBeDeleted.setPlateId(expId);
                            wellToBeDeleted.setRow(dbRow);
                            wellToBeDeleted.setCol(dbCol);
                            DbUtilities.deleteWellDescription(wellToBeDeleted);
                            wellMapTable.getModel().setValueAt(null, tableRow, tableCol);
                        }
                    }
                    ((AbstractTableModel) wellMapTable.getModel()).fireTableDataChanged();
                } catch (ClassNotFoundException
                        | SQLException
                        | NoSuchMethodException
                        | InstantiationException
                        | IllegalAccessException
                        | InvocationTargetException ex) {
                    Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    static String convert2Regex(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            switch (text.charAt(i)) {
                case '*':
                    sb.append(".*");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '.':
                    sb.append("\\.");
                    break;
                default:
                    sb.append(text.charAt(i));
                    break;
            }
        }
        return sb.toString();
    }

    public static List<Image> getSelectedImageIdsFromList(
            JList<String> filterList) throws NumberFormatException {
        List<Image> selectedIds = new ArrayList<>();
        for (int i = 0; i < filterList.getModel().getSize(); i++) {
            if (filterList.isSelectedIndex(i)) {
                String imageElement = filterList.getModel().getElementAt(i);
                Image image = new Image();
                image.setId(new Integer(imageElement.split("\\|")[0]));
                selectedIds.add(image);
            }
        }
        return selectedIds;
    }

    /**
     * Updates the GUI list of filter images according to the currently selected
     * experiment and directories (either one specific directory or all
     * directories at once).
     *
     * @param experimentsList the GUI list of experiments
     * @param filterDirectoryCombobox the combo box of directories
     * @param filterImageList the GUI list of filter images
     */
    public static void updateFilterImageList(
            JList<String> experimentsList,
            JComboBox<String> filterDirectoryCombobox,
            JList<String> filterImageList) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    Integer dirId = new Integer(
                            ((String) filterDirectoryCombobox.getSelectedItem())
                                    .split("\\|")[0]);
                    List<Image> images;
                    if (dirId == -1) {
                        // Fill in the list with all the images of the current
                        // experiment
                        Integer expId = new Integer(
                                experimentsList.getSelectedValue()
                                        .split("\\|")[0]);
                        images = DbUtilities.getImagesOfExperiment(expId);
                    } else {
                        images = DbUtilities.getImagesOfDirectory(dirId);
                    }
                    // Create a new model
                    DefaultListModel<String> model = new DefaultListModel<>();
                    for (Image image : images) {
                        model.addElement(
                                String.format(
                                        "%d|%s",
                                        image.getId(),
                                        image.getPath()));
                    }
                    // Set the list model
                    filterImageList.setModel(model);
                } catch (ClassNotFoundException
                        | SQLException
                        | NoSuchMethodException
                        | InstantiationException
                        | IllegalAccessException
                        | InvocationTargetException ex) {
                    Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    public static void updateFilterDirectoryCombobox(
            JList<String> experimentsList,
            JComboBox<String> filterDirectoryComboBox)
            throws
            ClassNotFoundException,
            InstantiationException,
            IllegalAccessException,
            InvocationTargetException,
            NoSuchMethodException,
            SQLException {
        Integer expId
                = new Integer(((String) experimentsList.getSelectedValue()).
                        split("\\|")[0]);
        // Load directories of the selected experiment
        List<Directory> directories
                = DbUtilities.getDirectoriesOfExperiment(expId);
        // Add the item "All Directories" to the combobox then fill the
        // combobox with directories of the current experiment.
        DefaultComboBoxModel<String> comboBoxModel
                = new DefaultComboBoxModel<>();
        comboBoxModel.addElement("-1|All Directories");
        for (Directory directory : directories) {
            comboBoxModel.addElement(String.format(
                    "%d|%s", directory.getId(), directory.getPath()));
        }
        filterDirectoryComboBox.setModel(comboBoxModel);
    }

    /**
     * Gets the name of the selected experiment from the experiments list.
     *
     * @param expList the GUI list of experiments
     * @return selected experiment name
     */
    public static String getSelectedExpName(JList<String> expList) {
        return expList.getSelectedValue().split("\\|")[1];
    }

    /**
     * Gets the ID(s) of the selected item(s) from the GUI list.
     *
     * @param list GUI list
     * @return selected ID(s)
     */
    public static List<Integer> getSelectedIds(JList<String> list) {
        List<String> selectedValues = list.getSelectedValuesList();
        List<Integer> ids = new ArrayList<>();
        for (String selectedValue : selectedValues) {
            ids.add(new Integer(selectedValue.split("\\|")[0]));
        }
        return ids;
    }

    /**
     * Gets the Name(s) of the selected item(s) from the GUI list.
     *
     * @param list GUI list
     * @return selected Name(s)
     */
    public static List<String> getSelectedNames(JList<String> list) {
        List<String> selectedValues = list.getSelectedValuesList();
        List<String> names = new ArrayList<>();
        for (String selectedValue : selectedValues) {
            names.add(selectedValue.split("\\|")[1]);
        }
        return names;
    }

    /**
     * Select all the experiments whose name matches the argument. This should
     * be only one experiment since experiment name is unique.
     *
     * @param experimentsList the GUI list of experiments
     * @param expName name of the experiment to select
     */
    static void setSelectedExperiment(
            JList<String> experimentsList,
            String expName) {
        for (int i = 0; i < experimentsList.getModel().getSize(); i++) {
            String currentExpName
                    = experimentsList.getModel().getElementAt(i)
                            .split("\\|")[1];
            if (currentExpName.equals(expName)) {
                experimentsList.setSelectedIndex(i);
            }
        }
    }

    /**
     * extract the integer ID from a string item following the format ID|NAME
     *
     * @param item string item (usually a list item or a combo box item)
     * @return ID
     */
    static Integer extractIdFromItem(String item) {
        return new Integer(item.split("\\|")[0]);
    }

    /**
     * extract the string Name from a string item following the format ID|NAME
     *
     * @param item string item (usually a list item or a combo box item)
     * @return Name
     */
    static String extractNameFromItem(String item) {
        return item.split("\\|")[1];
    }

    /**
     * If the manual set check box is not checked, this method generates an
     * automatic plate name for the experiment in hand, Otherwise, it leaves
     * plate name as is.
     *
     * @param newPlateInfoExpNameTextField Experiment name text field
     * @param newPlateInfoRowCountFormattedTextField Row count text field
     * @param newPlateInfoColCountFormattedTextField Column count text field
     * @param newPlateInfoManualSetCheckBox Manual set check box
     */
    static void updatePlateInfoPlateNameTextFiled(
            JTextField newPlateInfoExpNameTextField,
            JFormattedTextField newPlateInfoRowCountFormattedTextField,
            JFormattedTextField newPlateInfoColCountFormattedTextField,
            JTextField newPlateInfoPlateNameTextField,
            JCheckBox newPlateInfoManualSetCheckBox) {
        if (!newPlateInfoManualSetCheckBox.isSelected()) {
            String generatedPlateName = String.format(
                    "%s (%dx%d) Plate",
                    newPlateInfoExpNameTextField.getText(),
                    (Integer) newPlateInfoRowCountFormattedTextField.getValue(),
                    (Integer) newPlateInfoColCountFormattedTextField.getValue());
            newPlateInfoPlateNameTextField.setText(generatedPlateName);
        }
    }

    /**
     * Enable/Disable new plate info "Save" button based on the values inserted
     * as row count, column count and plate number.
     *
     * @param newPlateInfoSaveButton the button to be either disabled or enabled
     * @param newPlateInfoRowCountFormattedTextField row count text field
     * @param newPlateInfoColCountFormattedTextField col count text field
     * @param newPlateInfoPlateNameTextField plate name text field
     */
    static void updatePlateInfoSaveButtonStatus(
            JButton newPlateInfoSaveButton,
            JFormattedTextField newPlateInfoRowCountFormattedTextField,
            JFormattedTextField newPlateInfoColCountFormattedTextField,
            JTextField newPlateInfoPlateNameTextField) {
        int rowCount
                = (Integer) newPlateInfoRowCountFormattedTextField.getValue();
        int colCount
                = (Integer) newPlateInfoColCountFormattedTextField.getValue();
        if (rowCount == 0
                || colCount == 0
                || newPlateInfoPlateNameTextField.getText().trim().equals("")) {
            // Disable
            newPlateInfoSaveButton.setEnabled(false);
        } else {
            // Enable
            newPlateInfoSaveButton.setEnabled(true);
        }
    }

    /**
     * Open the "Edit Plate Info" dialog
     *
     * @param experimentsList the GUI list of experiments
     * @param newPlateInfoExpNameTextField the text field that displays
     * experiment name in the "Edit Plate Info" dialog.
     * @param plateInfoDialog the "Edit Plate Info" dialog
     */
    static void openEditPlateInfoDialog(
            JList<String> experimentsList,
            JTextField newPlateInfoExpNameTextField,
            JFormattedTextField newPlateInfoRowCountFormattedTextField,
            JFormattedTextField newPlateInfoColCountFormattedTextField,
            JTextField newPlateInfoPlateNameTextField,
            JCheckBox newPlateInfoManualSetCheckBox,
            JDialog plateInfoDialog) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    String selectedExpName
                            = GuiUtility.getSelectedExpName(experimentsList);
                    newPlateInfoExpNameTextField.setText(selectedExpName);
                    // The GUI makes sure that this dialog will only be
                    // displayed if only one experiment is selected
                    Integer expId = GuiUtility.getSelectedIds(experimentsList)
                            .get(0);
                    // Update displayed row and column counts
                    Integer rowCount = DbUtilities
                            .getExperimentById(expId).getRowCount();
                    Integer colCount = DbUtilities
                            .getExperimentById(expId).getColCount();
                    newPlateInfoRowCountFormattedTextField.setValue(rowCount);
                    newPlateInfoColCountFormattedTextField.setValue(colCount);
                    // Update displayed plate name
                    GuiUtility.updatePlateInfoPlateNameTextFiled(
                            newPlateInfoExpNameTextField,
                            newPlateInfoRowCountFormattedTextField,
                            newPlateInfoColCountFormattedTextField,
                            newPlateInfoPlateNameTextField,
                            newPlateInfoManualSetCheckBox);
                    // Center the PlateInfo dialog on the screen
                    GuiUtility.centerDialogOnScreen(plateInfoDialog);
                    // Open the PlateInfo Dialog to update plate information
                    plateInfoDialog.setVisible(true);
                } catch (ClassNotFoundException
                        | SQLException
                        | NoSuchMethodException
                        | InstantiationException
                        | IllegalAccessException
                        | InvocationTargetException ex) {
                    Logger.getLogger(GuiUtility.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    /**
     * Ensures that the displayed text in a formatted text field is the same as
     * its value.
     *
     * @param formattedTextField a formatted text field
     */
    public static void ensureIntegralValueDisplayed(
            JFormattedTextField formattedTextField) {
        try {
            formattedTextField.commitEdit();
            String actualText = formattedTextField
                    .getText();
            int intValue = (Integer) formattedTextField
                    .getValue();
            if (!actualText.equals(String.valueOf(intValue))) {
                formattedTextField.setText(
                        String.valueOf(intValue));
            }
        } catch (ParseException ex) {
            Logger.getLogger(GuiUtility.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Parse Exception: " + ex.toString());
        }
    }

    /**
     * Add text listeners to both row count and column count formatted text
     * fields. This is to ensure that the automated plate name is up to date and
     * the status of the save button (enabled/disabled) is also up to date.
     *
     * @param newPlateInfoExpNameTextField experiment name text field
     * @param newPlateInfoRowCountFormattedTextField row count text field
     * @param newPlateInfoColCountFormattedTextField column count text field
     * @param newPlateInfoPlateNameTextField plate name text field
     * @param newPlateInfoManualSetCheckBox plate name manual set check box
     * @param newPlateInfoSaveButton save button
     */
    static void addTextChangeListeners(
            JTextField newPlateInfoExpNameTextField,
            JFormattedTextField newPlateInfoRowCountFormattedTextField,
            JFormattedTextField newPlateInfoColCountFormattedTextField,
            JTextField newPlateInfoPlateNameTextField,
            JCheckBox newPlateInfoManualSetCheckBox,
            JButton newPlateInfoSaveButton) {
        // Add a text listener to the row count text field
        newPlateInfoRowCountFormattedTextField.getDocument()
                .addDocumentListener(new DocumentListener() {
                    @Override
                    public void changedUpdate(DocumentEvent e) {
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        updateUI();
                    }

                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        updateUI();
                    }

                    private void updateUI() {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                GuiUtility.ensureIntegralValueDisplayed(
                                        newPlateInfoRowCountFormattedTextField);
                                GuiUtility.updatePlateInfoPlateNameTextFiled(
                                        newPlateInfoExpNameTextField,
                                        newPlateInfoRowCountFormattedTextField,
                                        newPlateInfoColCountFormattedTextField,
                                        newPlateInfoPlateNameTextField,
                                        newPlateInfoManualSetCheckBox);
                                GuiUtility.updatePlateInfoSaveButtonStatus(
                                        newPlateInfoSaveButton,
                                        newPlateInfoRowCountFormattedTextField,
                                        newPlateInfoColCountFormattedTextField,
                                        newPlateInfoPlateNameTextField);
                            }
                        });
                    }
                });
        // Add a text listener to the column count text field
        newPlateInfoColCountFormattedTextField.getDocument()
                .addDocumentListener(new DocumentListener() {

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        updateUI();
                    }

                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        updateUI();
                    }

                    private void updateUI() {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                GuiUtility.ensureIntegralValueDisplayed(
                                        newPlateInfoColCountFormattedTextField);
                                GuiUtility.updatePlateInfoPlateNameTextFiled(
                                        newPlateInfoExpNameTextField,
                                        newPlateInfoRowCountFormattedTextField,
                                        newPlateInfoColCountFormattedTextField,
                                        newPlateInfoPlateNameTextField,
                                        newPlateInfoManualSetCheckBox);
                                GuiUtility.updatePlateInfoSaveButtonStatus(
                                        newPlateInfoSaveButton,
                                        newPlateInfoRowCountFormattedTextField,
                                        newPlateInfoColCountFormattedTextField,
                                        newPlateInfoPlateNameTextField);
                            }
                        });
                    }
                });
    }

    /**
     * Replaces each back slash in the input string with a slash. Used is fixing
     * strings to maintain portability across different operating systems.
     *
     * @param text input string (usually a path)
     * @return the same input string after replacing the back slashes with
     * slashes
     */
    static String replaceBackSlashWithSlash(String text) {
        StringBuilder slashedText = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\\') {
                slashedText.append("/");
            } else {
                slashedText.append(text.charAt(i));
            }
        }
        return slashedText.toString();
    }

    /**
     * Subtracts (subtracted) from (subtractedFrom) and returns the difference.
     * Notice that these image objects are only compared based on their IDs. So,
     * they do not need to be full objects and if they are two objects having
     * the same ID are considered equal even if they differ in other fields.
     *
     * @param subtractFrom the set to subtract from
     * @param subtracted the set to be subtracted
     * @return the difference between the two sets (subtractedFrom minus
     * subtracted)
     */
    static List<Image> getImageDifference(
            List<Image> subtractFrom,
            List<Image> subtracted) {
        List<Image> diff = new ArrayList<>();
        outerLoop:
        for (Image imageToCheck : subtractFrom) {
            for (Image imageToBeSubtracted : subtracted) {
                if (imageToCheck.getId().equals(imageToBeSubtracted.getId())) {
                    continue outerLoop;
                }
            }
            diff.add(imageToCheck);
        }
        return diff;
    }

    /**
     * Event code for clicking the remove experiment button.
     *
     * @param experimentsList GUI list of experiments
     * @param directoriesList GUI list of directories
     * @param imagesList GUI list of images
     * @param removeExperimentButton button for removing experiment(s)
     * @param addDirectoryButton button for adding a new directory
     * @param removeDirectoryButton button for removing director(y/ies)
     * @param removeImageButton button for removing image(s)
     * @param filterImageButton button for filtering images
     * @param fixPathsButton button for fixing paths
     * @param experimentsExplorerDialog the whole experiments explorer dialog
     */
    static void removeExperimentAction(
            JList<String> experimentsList,
            JList<String> directoriesList,
            JList<String> imagesList,
            JButton removeExperimentButton,
            JButton addDirectoryButton,
            JButton removeDirectoryButton,
            JButton removeImageButton,
            JButton filterImageButton,
            JButton fixPathsButton,
            JDialog experimentsExplorerDialog) {
        int showConfirmDialogResult = JOptionPane.showConfirmDialog(
                experimentsExplorerDialog,
                "Are you sure you want to remove all selected experiment along"
                + " with all their directories and images from the database?");
        if (showConfirmDialogResult == JOptionPane.YES_OPTION) {
            try {
                List<Integer> selectedIds
                        = GuiUtility.getSelectedIds(experimentsList);
                for (Integer selectedId : selectedIds) {
                    Experiment experiment = new Experiment();
                    experiment.setId(selectedId);
                    DbUtilities.deleteExperiment(experiment);
                }
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Refresh experiments, directories and images lists
                            GuiUtility.updateExpDirImageLists(
                                    experimentsList,
                                    directoriesList,
                                    imagesList);
                            // Refresh button
                            GuiUtility.updateButtonsStatus(
                                    experimentsList,
                                    directoriesList,
                                    imagesList,
                                    removeExperimentButton,
                                    addDirectoryButton,
                                    removeDirectoryButton,
                                    //addImageButton,
                                    removeImageButton,
                                    filterImageButton,
                                    fixPathsButton);
                        } catch (ClassNotFoundException
                                | SQLException
                                | NoSuchMethodException
                                | InstantiationException
                                | IllegalAccessException
                                | InvocationTargetException ex) {
                            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                });
            } catch (ClassNotFoundException
                    | SQLException
                    | NoSuchMethodException
                    | InstantiationException
                    | IllegalAccessException
                    | InvocationTargetException ex) {
                Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Event code for clicking the remove directory button.
     *
     * @param experimentsList GUI list of experiments
     * @param directoriesList GUI list of directories
     * @param imagesList GUI list of images
     * @param removeExperimentButton button for removing experiment(s)
     * @param addDirectoryButton button for adding a new directory
     * @param removeDirectoryButton button for removing director(y/ies)
     * @param removeImageButton button for removing image(s)
     * @param filterImageButton button for filtering images
     * @param fixPathsButton button for fixing paths
     * @param experimentsExplorerDialog the whole experiments explorer dialog
     */
    static void removeDirectoryButtonAction(
            JList<String> experimentsList,
            JList<String> directoriesList,
            JList<String> imagesList,
            JButton removeExperimentButton,
            JButton addDirectoryButton,
            JButton removeDirectoryButton,
            JButton removeImageButton,
            JButton filterImageButton,
            JButton fixPathsButton,
            JDialog experimentsExplorerDialog,
            ScrollImagePanel scrollImagePanel) {
        int showConfirmDialogResult = JOptionPane.showConfirmDialog(
                experimentsExplorerDialog,
                "Are you sure you want to remove all selected directories along"
                + " with all their images from the database?");
        if (showConfirmDialogResult == JOptionPane.YES_OPTION) {
            try {
                List<String> selectedValuesList = directoriesList.getSelectedValuesList();
                for (String selectedValue : selectedValuesList) {
                    Directory directory = new Directory();
                    directory.setId(new Integer(selectedValue.split("\\|")[0]));
                    DbUtilities.deleteDirectory(directory);
                }
                // Update the image traversal
                GuiUtility.updateFileTraversal(experimentsList, scrollImagePanel);
                // Refresh GUI
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Refresh Directories list
                            GuiUtility.updateDirectoriesList(
                                    experimentsList, directoriesList);
                            // Refresh images list
                            GuiUtility.updateImagesList(
                                    directoriesList, imagesList);
                            // Refresh buttons
                            GuiUtility.updateButtonsStatus(
                                    experimentsList,
                                    directoriesList,
                                    imagesList,
                                    removeExperimentButton,
                                    addDirectoryButton,
                                    removeDirectoryButton,
                                    //addImageButton,
                                    removeImageButton,
                                    filterImageButton,
                                    fixPathsButton);
                        } catch (ClassNotFoundException
                                | SQLException
                                | NoSuchMethodException
                                | InstantiationException
                                | IllegalAccessException
                                | InvocationTargetException ex) {
                            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                });
            } catch (ClassNotFoundException
                    | SQLException
                    | NoSuchMethodException
                    | InstantiationException
                    | IllegalAccessException
                    | InvocationTargetException ex) {
                Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Event code for clicking the add directory button.
     *
     * @param imagesDirChooser file chooser to choose the directory to be added
     * @param experimentsList GUI list of experiments
     * @param directoriesList GUI list of directories
     * @param imagesList GUI list of images
     * @param removeExperimentButton button for removing experiment(s)
     * @param addDirectoryButton button for adding a new directory
     * @param removeDirectoryButton button for removing director(y/ies)
     * @param removeImageButton button for removing image(s)
     * @param filterImageButton button for filtering images
     * @param fixPathsButton button for fixing paths
     * @param experimentsExplorerDialog the whole experiments explorer dialog
     */
    static void addDirectoryButtonAction(
            JFileChooser imagesDirChooser,
            JList<String> experimentsList,
            JList<String> directoriesList,
            JList<String> imagesList,
            JButton removeExperimentButton,
            JButton addDirectoryButton,
            JButton removeDirectoryButton,
            JButton removeImageButton,
            JButton filterImageButton,
            JButton fixPathsButton,
            JDialog experimentsExplorerDialog,
            JDialog loadImagesProgressDialog,
            JLabel loadingImagesProgressLabel,
            JButton loadingImagesProgressButton,
            JProgressBar loadImagesProgressBar,
            JButton nextImageButton,
            JButton previousImageButton,
            JButton clearImageButton,
            ScrollImagePanel scrollImagePanel
    ) {
        if (imagesDirChooser.showOpenDialog(experimentsExplorerDialog)
                == JFileChooser.APPROVE_OPTION) {
            try {
                Integer expId = new Integer(
                        GuiUtility.getSelectedIds(experimentsList).get(0));
                Directory directory = new Directory();
                directory.setPath(imagesDirChooser.getSelectedFile().getPath());
                directory.setExperimentId(expId);
                DbUtilities.addDirectory(directory);
                // Refresh Directories list
                GuiUtility.updateDirectoriesList(
                        experimentsList, directoriesList);
                // Select the directory just added
                directoriesList.setSelectedIndex(
                        directoriesList.getModel().getSize() - 1);
                // load images in this directory
                Directory dir = DbUtilities.getDirectorById(
                        new Integer(directoriesList.getSelectedValue()
                                .split("\\|")[0]));
                loadDirectoryImages(
                        dir,
                        loadImagesProgressDialog,
                        loadingImagesProgressLabel,
                        loadingImagesProgressButton,
                        loadImagesProgressBar,
                        directoriesList,
                        imagesList,
                        nextImageButton,
                        previousImageButton,
                        clearImageButton,
                        scrollImagePanel
                );
                // Update the image traversal
                GuiUtility.updateFileTraversal(experimentsList, scrollImagePanel);
                // Refresh images list
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            GuiUtility.updateImagesList(
                                    directoriesList, imagesList);
                            GuiUtility.updateButtonsStatus(
                                    experimentsList,
                                    directoriesList,
                                    imagesList,
                                    removeExperimentButton,
                                    addDirectoryButton,
                                    removeDirectoryButton,
                                    removeImageButton,
                                    filterImageButton,
                                    fixPathsButton);
                        } catch (ClassNotFoundException
                                | SQLException
                                | NoSuchMethodException
                                | InstantiationException
                                | IllegalAccessException
                                | InvocationTargetException ex) {
                            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                });
            } catch (ClassNotFoundException
                    | SQLException
                    | NoSuchMethodException
                    | InstantiationException
                    | IllegalAccessException
                    | InvocationTargetException ex) {
                Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Browse for an input directory and recursively load all the files located
     * anywhere in the subtree whose root is this input directory.
     */
    private static void loadDirectoryImages(
            Directory rootDir,
            JDialog loadImagesProgressDialog,
            JLabel loadingImagesProgressLabel,
            JButton loadingImagesProgressButton,
            JProgressBar loadImagesProgressBar,
            JList<String> directoriesList,
            JList<String> imagesList,
            JButton nextImageButton,
            JButton previousImageButton,
            JButton clearImageButton,
            ScrollImagePanel scrollImagePanel
    ) {
        // Create and start images loading task. Notice that instances of:
        // javax.swing.SwingWorker are not reusuable, so we create a new
        // each time the user is pressed.
        LoadImagesTask loadImagesTask = new LoadImagesTask(
                rootDir,
                loadImagesProgressDialog,
                loadingImagesProgressLabel,
                loadingImagesProgressButton,
                EXTENSIONS_FILTER,
                directoriesList,
                imagesList);
        loadImagesTask.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ("progress" == evt.getPropertyName()) {
                    int progress = (Integer) evt.getNewValue();
                    loadImagesProgressBar.setIndeterminate(false);
                    loadImagesProgressBar.setValue(progress);
                }
            }
        });
        loadImagesTask.execute();
        // The following separate thread opens the dialog and waits at the call
        // to loadImagesTask.get() until the doInBackground() method of the task
        // object completes its work.
        new Thread() {
            @Override
            public void run() {
                loadingImagesProgressLabel.setText(
                        "Loading images to be processed ...");
                loadingImagesProgressButton.setEnabled(false);
                loadImagesProgressBar.setIndeterminate(true);
                GuiUtility.centerDialogOnScreen(loadImagesProgressDialog);
                loadImagesProgressDialog.setVisible(true);
                try {
                    // Notice that the following method will block all events 
                    // including repaint until it returns.
                    fileTraversal.append(loadImagesTask.get());
                    // Select the last directory in the GUI list of directories
                    if (directoriesList.getModel().getSize() > 0) {
                        directoriesList.setSelectedIndex(
                                directoriesList.getModel().getSize() - 1);
                    }
                    // Enable disabled buttons
                    if (fileTraversal != null && fileTraversal.size() > 0) {
                        nextImageButton.setEnabled(true);
                        previousImageButton.setEnabled(true);
                        clearImageButton.setEnabled(true);
                    }
                    // load the first image from the file traversal
                    GuiUtility.displayNext(fileTraversal, scrollImagePanel);
                    try {
                        GuiUtility.refreshCoordinatesPlot(fileTraversal, scrollImagePanel);
                    } catch (SQLException | ClassNotFoundException ex) {
                        Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();
    }

    /**
     * Event code for clicking the import to database button.
     *
     * @param dbDumpFileChooser the file chooser used to choose the file to be
     * imported
     * @param parentFrame the parent frame of all message to be displayed
     */
    static void importFileToDbButtonAction(
            JFileChooser dbDumpFileChooser,
            JFrame parentFrame) {
        if (dbDumpFileChooser.showSaveDialog(parentFrame)
                == JFileChooser.APPROVE_OPTION) {
            try {
                File selectedFile = dbDumpFileChooser.getSelectedFile();
                DbUtilities.importDb(selectedFile);
                JOptionPane.showMessageDialog(
                        parentFrame,
                        "All data in the file is add to the database "
                        + "successfully.",
                        "Data imported successfully",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException
                    | ClassNotFoundException
                    | SQLException
                    | NoSuchMethodException
                    | InstantiationException
                    | IllegalAccessException
                    | InvocationTargetException ex) {
                Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Event code for clicking the next image button.
     *
     * @param scrollImagePanel the panel used to display images
     */
    static void displayNextImageButtonAction(
            ScrollImagePanel scrollImagePanel,
            LumenMap lumenMap) {
        GuiUtility.displayNext(fileTraversal, scrollImagePanel);
        refreshAnnotations(scrollImagePanel, lumenMap);
    }

    /**
     * Event code for clicking the previous image button.
     *
     * @param scrollImagePanel the panel used to display images
     */
    static void displayPreviousImageButtonAction(
            ScrollImagePanel scrollImagePanel,
            LumenMap lumenMap) {
        GuiUtility.displayPrevious(GuiUtility.fileTraversal, scrollImagePanel);
        refreshAnnotations(scrollImagePanel, lumenMap);
    }

    /**
     * This is a utility method that refreshes the annotations (lumen tags and
     * lumen outlines) on an image
     *
     * @param scrollImagePanel
     * @param lumenMap
     * @throws IOException
     */
    private static void refreshAnnotations(
            ScrollImagePanel scrollImagePanel,
            LumenMap lumenMap) {
        try {
            GuiUtility.refreshCoordinatesPlot(fileTraversal, scrollImagePanel);
            GuiUtility.refreshOutlinePlot(fileTraversal, scrollImagePanel, lumenMap);
        } catch (SQLException
                | ClassNotFoundException ex) {
            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(GuiUtility.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Event code for clicking the experiments explorer button.
     *
     * @param experimentsList GUI list of experiments
     * @param directoriesList GUI list of directories
     * @param experimentsExplorerDialog the whole experiments explorer dialog
     */
    static void experimentsExplorerButtonAction(
            JList<String> experimentsList,
            JList<String> directoriesList,
            JList<String> imagesList,
            JDialog experimentsExplorerDialog,
            ScrollImagePanel scrollImagePanel) {
        if (currentlyDisplayedImage == null) {
            // Load all experiments
            GuiUtility.loadExperiments(experimentsList, directoriesList);
            // Update fileFilter
            GuiUtility.updateFileTraversal(experimentsList, scrollImagePanel);
        } else {
            GuiUtility.selectCurrentlyDisplayedImage(
                    experimentsList,
                    directoriesList,
                    imagesList);
        }
        // Center the dialog on the screen
        GuiUtility.centerDialogOnScreen(experimentsExplorerDialog);
        // Display the dialog
        experimentsExplorerDialog.setVisible(true);
    }

    /**
     * Event code for clicking the clear image button.
     *
     * @param scrollImagePanel the panel used to display images
     * @param experimentsList the GUI list of experiments
     * @param lumenCounterLabel the label that displys current lumen count
     */
    static void clearTags(
            ScrollImagePanel scrollImagePanel,
            JList<String> experimentsList,
            JLabel lumenCounterLabel) {
        try {
            DbUtilities.clearCoordinatesOfImage(GuiUtility.fileTraversal.getCurrent().getId());
            GuiUtility.refreshCoordinatesPlot(GuiUtility.fileTraversal, scrollImagePanel);
            GuiUtility.refreshLumenCountDisplay(experimentsList, lumenCounterLabel);
        } catch (ClassNotFoundException
                | SQLException
                | NoSuchMethodException
                | InstantiationException
                | IllegalAccessException
                | InvocationTargetException ex) {
            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Event code for when the list of directories changes.
     *
     * @param experimentsList the GUI list of experiments
     * @param directoriesList the GUI list of directories
     * @param imagesList the GUI list of images
     * @param removeExperimentButton the remove experiments button
     * @param addDirectoryButton the add directory button
     * @param removeDirectoryButton the remove directories button
     * @param removeImageButton the remove images button
     * @param filterImageButton the filter images button
     * @param fixPathsButton the fix paths button
     */
    static void directoriesListValueChangedAction(
            JList<String> experimentsList,
            JList<String> directoriesList,
            JList<String> imagesList,
            JButton removeExperimentButton,
            JButton addDirectoryButton,
            JButton removeDirectoryButton,
            JButton removeImageButton,
            JButton filterImageButton,
            JButton fixPathsButton) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    GuiUtility.updateImagesList(
                            directoriesList, imagesList);
                    GuiUtility.updateButtonsStatus(
                            experimentsList,
                            directoriesList,
                            imagesList,
                            removeExperimentButton,
                            addDirectoryButton,
                            removeDirectoryButton,
                            //addImageButton,
                            removeImageButton,
                            filterImageButton,
                            fixPathsButton);
                } catch (ClassNotFoundException
                        | SQLException
                        | NoSuchMethodException
                        | InstantiationException
                        | IllegalAccessException
                        | InvocationTargetException ex) {
                    Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    /**
     * Updates the internal file(image) traversal by emptying it then adding all
     * the images of the current experiment.
     *
     * @param experimentsList the GUI list of experiments
     * @param scrollImagePanel the panel displaying the current image
     */
    @SuppressWarnings("empty-statement")
    public static void updateFileTraversal(
            JList<String> experimentsList,
            ScrollImagePanel scrollImagePanel) {
        try {
            List<Integer> selectedExpIds
                    = GuiUtility.getSelectedIds(experimentsList);
            boolean imagesExist = false;
            if (selectedExpIds.size() == 1) {
                // Create the new traversal
                FileTraversal newTraversal = new FileTraversal();
                newTraversal.append(
                        DbUtilities.getImagesOfExperiment(
                                selectedExpIds.get(0)));
                if (newTraversal.hasNext()) {
                    imagesExist = true;
                    // If there is no currently displayed image, update the 
                    // traversal directly, otherwise account for the possibility
                    // that the currently diplayed image has been deleted
                    // (possibly among others).
                    if (currentlyDisplayedImage == null) {
                        GuiUtility.fileTraversal = newTraversal;
                    } else {
                        // Create a boolean list to mark which images are
                        // deleted
                        List<Boolean> stillExists = new ArrayList<>();
                        // The following variable will hold the index of your
                        // currently displayed image (whether it is deleted or
                        // not)
                        GuiUtility.fileTraversal.resetIndex();
                        int currentImageIndex = -1;
                        while (GuiUtility.fileTraversal.hasNext()) {
                            currentImageIndex++;
                            if (GuiUtility.fileTraversal.next().getId()
                                    .equals(currentlyDisplayedImage.getId())) {
                                break;
                            }
                        }
                        // Loop over the old (longer traversal) and mark all
                        // deleted images in the boolean array
                        GuiUtility.fileTraversal.resetIndex();
                        Image currentImageInNewTraversal
                                = newTraversal.next();
                        while (GuiUtility.fileTraversal.hasNext()) {
                            Image currentImageInOldTraversal
                                    = GuiUtility.fileTraversal.next();
                            if (currentImageInOldTraversal.getId()
                                    .equals(currentImageInNewTraversal
                                            .getId())) {
                                stillExists.add(true);
                                if (newTraversal.hasNext()) {
                                    currentImageInNewTraversal
                                            = newTraversal.next();
                                }
                            } else {
                                stillExists.add(false);
                            }
                        }
                        // Count the number of still existing images before the
                        // deleted image
                        if (stillExists.get(currentImageIndex) == false) {
                            int countBefore = 0;
                            for (int i = 0; i < currentImageIndex; i++) {
                                if (stillExists.get(i) == true) {
                                    countBefore++;
                                }
                            }
                            // Move the new traversal to the right position
                            newTraversal.resetIndex();
                            for (int i = 0; i < countBefore - 1; i++) {
                                newTraversal.next();
                            }
                            // Update the old file traversal
                            GuiUtility.fileTraversal = newTraversal;
                            // Display the nest image
                            displayNext(fileTraversal, scrollImagePanel);
                        } else {
                            newTraversal.resetIndex();
                            while (!newTraversal.next().getId().equals(
                                    currentlyDisplayedImage.getId()));
                            GuiUtility.fileTraversal = newTraversal;
                        }
                    }
                }
            }
            if (!imagesExist) {
                GuiUtility.fileTraversal.clearTraversal();
                // Delete current image
                currentlyDisplayedImage = null;
                scrollImagePanel.clearImage();
            }
        } catch (SQLException
                | ClassNotFoundException
                | NoSuchMethodException
                | InstantiationException
                | IllegalAccessException
                | InvocationTargetException ex) {
            Logger.getLogger(GuiUtility.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Adjust the selection made in the GUI lists of experiments, directories
     * and images to reflect the currently selected images.
     *
     * @param experimentsList the GUI list of experiments
     * @param directoriesList the GUI list of directories
     * @param imagesList the GUI list of images
     */
    static void selectCurrentlyDisplayedImage(
            JList<String> experimentsList,
            JList<String> directoriesList,
            JList<String> imagesList) {
        try {
            if (currentlyDisplayedImage != null) {
                // Get the directory of the current displayed image
                Directory dir = DbUtilities.getDirectorById(
                        currentlyDisplayedImage.getParentDirId());
                // Get the experiment of the currently selected image
                Experiment exp = DbUtilities.getExperimentById(
                        dir.getExperimentId());
                // Select this experiment in the GUI list of experiments
                for (int i = 0; i < experimentsList.getModel().getSize(); i++) {
                    if (GuiUtility.extractIdFromItem(
                            experimentsList.getModel().getElementAt(i))
                            .equals(exp.getId())) {
                        experimentsList.setSelectedIndex(i);
                        break;
                    }
                }
                // Select this directory in the GUI list of directories
                for (int i = 0; i < directoriesList.getModel().getSize(); i++) {
                    if (GuiUtility.extractIdFromItem(
                            directoriesList.getModel().getElementAt(i))
                            .equals(dir.getId())) {
                        directoriesList.setSelectedIndex(i);
                        break;
                    }
                }
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Load images of the selected dir
                            GuiUtility.updateImagesList(
                                    directoriesList, imagesList);
                            // Select this image in the GUI list of
                            // images
                            for (int i = 0;
                                    i < imagesList.getModel().getSize();
                                    i++) {
                                if (GuiUtility.extractIdFromItem(
                                        imagesList.getModel()
                                                .getElementAt(i))
                                        .equals(currentlyDisplayedImage
                                                .getId())) {
                                    imagesList.setSelectedIndex(i);
                                    break;
                                }
                            }
                        } catch (ClassNotFoundException
                                | SQLException
                                | NoSuchMethodException
                                | InstantiationException
                                | IllegalAccessException
                                | InvocationTargetException ex) {
                            Logger.getLogger(GuiUtility.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                });
            }
        } catch (ClassNotFoundException
                | SQLException
                | NoSuchMethodException
                | InstantiationException
                | IllegalAccessException
                | InvocationTargetException ex) {
            Logger.getLogger(GuiUtility.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Event code for when the list of experiments changes.
     *
     * @param experimentsList the GUI list of experiments
     * @param directoriesList the GUI list of directories
     * @param imagesList the GUI list of images
     * @param removeExperimentButton the remove experiments button
     * @param addDirectoryButton the add directory button
     * @param removeDirectoryButton the remove directories button
     * @param removeImageButton the remove images button
     * @param filterImageButton the filter images button
     * @param fixPathsButton the fix paths button
     */
    static void experimentsListValueChangedAction(
            JComboBox<String> experimentsCombobox,
            JList<String> experimentsList,
            JList<String> directoriesList,
            JList<String> imagesList,
            JButton removeExperimentButton,
            JButton addDirectoryButton,
            JButton removeDirectoryButton,
            JButton removeImageButton,
            JButton filterImageButton,
            JButton fixPathsButton,
            ScrollImagePanel scrollImagePanel,
            LumenMap lumenMap) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    // Update the image traversal
                    GuiUtility.updateFileTraversal(
                            experimentsList, scrollImagePanel);
                    // Refresh Directories list
                    GuiUtility.updateDirectoriesList(
                            experimentsList,
                            directoriesList);
                    // Refresh images list
                    GuiUtility.updateImagesList(
                            directoriesList,
                            imagesList);
                    // Refresh buttons
                    GuiUtility.updateButtonsStatus(
                            experimentsList,
                            directoriesList,
                            imagesList,
                            removeExperimentButton,
                            addDirectoryButton,
                            removeDirectoryButton,
                            //addImageButton,
                            removeImageButton,
                            filterImageButton,
                            fixPathsButton);
                    // Refresh experiments combobox
                    GuiUtility.updateExperimentsCombobox(
                            experimentsCombobox,
                            experimentsList,
                            directoriesList,
                            scrollImagePanel,
                            lumenMap);
                } catch (ClassNotFoundException
                        | SQLException
                        | NoSuchMethodException
                        | InstantiationException
                        | IllegalAccessException
                        | InvocationTargetException ex) {
                    Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    /**
     * Event code for when the list of images changes.
     *
     * @param experimentsList the GUI list of experiments
     * @param directoriesList the GUI list of directories
     * @param imagesList the GUI list of images
     * @param removeExperimentButton the remove experiments button
     * @param addDirectoryButton the add directory button
     * @param removeDirectoryButton the remove directories button
     * @param removeImageButton the remove images button
     * @param filterImageButton the filter images button
     * @param fixPathsButton the fix paths button
     * @param jumpToSelectionButton the jump to selected image button
     */
    static void imagesListValueChangedAction(
            JList<String> experimentsList,
            JList<String> directoriesList,
            JList<String> imagesList,
            JButton removeExperimentButton,
            JButton addDirectoryButton,
            JButton removeDirectoryButton,
            JButton removeImageButton,
            JButton filterImageButton,
            JButton fixPathsButton,
            JButton jumpToSelectionButton) {
        GuiUtility.updateButtonsStatus(
                experimentsList,
                directoriesList,
                imagesList,
                removeExperimentButton,
                addDirectoryButton,
                removeDirectoryButton,
                removeImageButton,
                filterImageButton,
                fixPathsButton);
        if (imagesList.isSelectionEmpty()) {
            jumpToSelectionButton.setEnabled(false);
        } else {
            jumpToSelectionButton.setEnabled(true);
        }
    }

    /**
     * Event code for clicking the save button in the wells pop-up menu.
     *
     * @param experimentsList the GUI list of experiments
     * @param wellMapTable the table showing the plate map
     * @param wellExperimentTextField the text field showing the name of the
     * experiment
     * @param wellCompoundTextField the text field showing the compound in this
     * well
     * @param wellConcentrationFormattedTextField the text field showing the
     * concentration in this well
     * @param wellCellTypeTextField the text field showing the cell type in this
     * well
     * @param wellCellCountFormattedTextField the text field showing the cell
     * count in this well
     * @param wellEditDialog the whole well edit dialog
     */
    static void wellSaveButtonAction(
            JList<String> experimentsList,
            JTable wellMapTable,
            JTextField wellExperimentTextField,
            JTextField wellCompoundTextField,
            JFormattedTextField wellConcentrationFormattedTextField,
            JTextField wellCellTypeTextField,
            JFormattedTextField wellCellCountFormattedTextField,
            JDialog wellEditDialog) {
        GuiUtility.saveWellInfo(
                experimentsList,
                wellMapTable,
                wellExperimentTextField,
                wellCompoundTextField,
                wellConcentrationFormattedTextField,
                wellCellTypeTextField,
                wellCellCountFormattedTextField);
        wellEditDialog.setVisible(false);
    }

    /**
     * Event code for clicking the export database to file button.
     *
     * @param dbDumpFileChooser the file chooser used to idetify the output file
     * @param parentFrame the parent frame of all messages to be displayed
     */
    static void exportDbToFileButtonAction(
            JFileChooser dbDumpFileChooser,
            JFrame parentFrame) {
        if (dbDumpFileChooser.showSaveDialog(parentFrame)
                == JFileChooser.APPROVE_OPTION) {
            try {
                File selectedFile = dbDumpFileChooser.getSelectedFile();
                DbUtilities.exportDb(selectedFile);
                JOptionPane.showMessageDialog(
                        parentFrame,
                        "All data in the database is exported successfully.",
                        "Data exported successfully",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (FileNotFoundException
                    | ClassNotFoundException
                    | SQLException
                    | NoSuchMethodException
                    | InstantiationException
                    | IllegalAccessException
                    | InvocationTargetException ex) {
                Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Event code for clicking the filter image button.
     *
     * @param experimentsList the GUI list of experiments
     * @param filterExperimentTextField the text field showing the name of the
     * experiment in the image filter dialog
     * @param filterDirectoryComboBox the combo box showing directories in the
     * image filter dialog
     * @param filterImageFilterMaskTextField the text field showing the filter
     * mask in the image filter dialog
     * @param filterImageList the list of images to be filter (in or out)
     * @param imageFilterDialog the whole image filter dialog
     */
    static void filterImageButtonAction(
            JList<String> experimentsList,
            JTextField filterExperimentTextField,
            JComboBox<String> filterDirectoryComboBox,
            JTextField filterImageFilterMaskTextField,
            JList<String> filterImageList,
            JDialog imageFilterDialog) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    String expName
                            = ((String) experimentsList.getSelectedValue()).
                                    split("\\|")[1];
                    // Update GUI
                    filterExperimentTextField.setText(expName);
                    GuiUtility.updateFilterDirectoryCombobox(
                            experimentsList,
                            filterDirectoryComboBox);
                    // Display the generic filter *.* which displays all images
                    // (as if no filter is applied)
                    filterImageFilterMaskTextField.setText("*.*");
                    GuiUtility.updateFilterImageList(experimentsList,
                            filterDirectoryComboBox,
                            filterImageList);
                    // Diplay filter dialog
                    GuiUtility.centerDialogOnScreen(imageFilterDialog);
                    // Select all items in the list
                    filterImageList.setSelectionInterval(
                            0, filterImageList.getModel().getSize() - 1);
                    imageFilterDialog.setVisible(true);
                } catch (ClassNotFoundException
                        | SQLException
                        | NoSuchMethodException
                        | InstantiationException
                        | IllegalAccessException
                        | InvocationTargetException ex) {
                    Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    /**
     * Event code for clicking the apply filter button.
     *
     * @param experimentsList the GUI list of experiments
     * @param filterImageFilterMaskTextField the text field showing the filter
     * mask in the image filter dialog
     * @param filterImageList the list of images to be filter (in or out)
     */
    static void filterApplyFilterAction(
            JList<String> experimentsList,
            JTextField filterImageFilterMaskTextField,
            JList<String> filterImageList) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    Pattern regex = Pattern.compile(GuiUtility.convert2Regex(
                            filterImageFilterMaskTextField.getText().trim()));
                    DefaultListModel<String> newModel
                            = new DefaultListModel<>();
                    Integer expId
                            = GuiUtility.getSelectedIds(experimentsList).get(0);
                    List<Image> images
                            = DbUtilities.getImagesOfExperiment(expId);
                    for (Image image : images) {
                        if (regex.matcher(image.getPath()).matches()) {
                            newModel.addElement(String.format(
                                    "%d|%s", image.getId(), image.getPath()));
                        }
                    }
                    filterImageList.setModel(newModel);
                    // Select all items in the list
                    filterImageList.setSelectionInterval(
                            0,
                            filterImageList.getModel().getSize() - 1);
                } catch (SQLException
                        | ClassNotFoundException
                        | NoSuchMethodException
                        | InstantiationException
                        | IllegalAccessException
                        | InvocationTargetException ex) {
                    Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    /**
     * Event code for clicking the delete selected images button.
     *
     * @param experimentsList the GUI list of experiments
     * @param filterImageFilterMaskTextField the text field showing the filter
     * mask in the image filter dialog
     * @param filterImageList the list of images to be filter (in or out)
     * @param parentDialog the parent dialog of all messages to be displayed
     */
    static void filterDeleteSelectedImagesButtonAction(
            JList<String> experimentsList,
            JComboBox<String> filterDirectoryComboBox,
            JList<String> filterImageList,
            JDialog parentDialog,
            ScrollImagePanel scrollImagePanel) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                int showConfirmDialogResult = JOptionPane.showConfirmDialog(
                        parentDialog,
                        "Are you sure you want to remove all selected images from the "
                        + "database?");
                if (showConfirmDialogResult == JOptionPane.YES_OPTION) {
                    try {
                        List<Image> images = GuiUtility.getSelectedImageIdsFromList(filterImageList);
                        DbUtilities.deleteImages(images);
                        GuiUtility.updateFilterImageList(
                                experimentsList,
                                filterDirectoryComboBox,
                                filterImageList);
                        // Update the image traversal
                        GuiUtility.updateFileTraversal(
                                experimentsList, scrollImagePanel);
                    } catch (SQLException
                            | ClassNotFoundException
                            | NoSuchMethodException
                            | InstantiationException
                            | IllegalAccessException
                            | InvocationTargetException ex) {
                        Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
    }

    /**
     * Event code for clicking the keep selected images button.
     *
     * @param experimentsList the GUI list of experiments
     * @param filterImageFilterMaskTextField the text field showing the filter
     * mask in the image filter dialog
     * @param filterImageList the list of images to be filter (in or out)
     * @param parentDialog the parent dialog of all messages to be displayed
     */
    static void filterKeepSelectedImagesButtonAction(
            JList<String> experimentsList,
            JComboBox<String> filterDirectoryComboBox,
            JList<String> filterImageList,
            JDialog parentDialog,
            ScrollImagePanel scrollImagePanel) {
        int showConfirmDialogResult = JOptionPane.showConfirmDialog(
                parentDialog,
                "Are you sure you want to remove all other images from the "
                + "database?");
        if (showConfirmDialogResult == JOptionPane.YES_OPTION) {
            try {
                List<Image> imageIds = GuiUtility.getSelectedImageIdsFromList(
                        filterImageList);
                Integer selectedDirectoryId = new Integer(
                        ((String) filterDirectoryComboBox.getSelectedItem())
                                .split("\\|")[0]);
                List<Image> diff = new ArrayList<>();
                if (selectedDirectoryId == -1) {
                    Integer expId = new Integer(
                            GuiUtility.getSelectedIds(experimentsList).get(0));
                    List<Image> imagesOfExperiment
                            = DbUtilities.getImagesOfExperiment(expId);
                    diff = GuiUtility.getImageDifference(
                            imagesOfExperiment, imageIds);
                } else {
                    Integer dirId = new Integer(
                            ((String) filterDirectoryComboBox.getSelectedItem())
                                    .split("\\|")[0]);
                    List<Image> imagesOfDirectory
                            = DbUtilities.getImagesOfDirectory(dirId);
                    diff = GuiUtility.getImageDifference(
                            imagesOfDirectory, imageIds);
                }
                DbUtilities.deleteImages(diff);
                GuiUtility.updateFilterImageList(
                        experimentsList,
                        filterDirectoryComboBox,
                        filterImageList);
                // Update the image traversal
                GuiUtility.updateFileTraversal(experimentsList, scrollImagePanel);
            } catch (SQLException
                    | ClassNotFoundException
                    | NoSuchMethodException
                    | InstantiationException
                    | IllegalAccessException
                    | InvocationTargetException ex) {
                Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Event code for clicking add experiment button.
     *
     * @param experimentsList the GUI list of experiments
     * @param newPlateInfoExpNameTextField the text field showing the name of
     * the experiment in the plate info dialog
     * @param newPlateInfoRowCountFormattedTextField the text field showing the
     * row count in the plate info dialog
     * @param newPlateInfoColCountFormattedTextField the text field showing the
     * column count in the plate info dialog
     * @param newPlateInfoPlateNameTextField the text field showing the name of
     * the plate in the plate info dialog
     * @param newPlateInfoManualSetCheckBox the check box that allows users to
     * set plate name manually
     * @param plateInfoDialog the whole plate info dialog
     * @param experimentsExplorerDialog the whole experiments explorer dialog
     */
    static void addExperimentButtonAction(
            JList<String> experimentsList,
            JTextField newPlateInfoExpNameTextField,
            JFormattedTextField newPlateInfoRowCountFormattedTextField,
            JFormattedTextField newPlateInfoColCountFormattedTextField,
            JTextField newPlateInfoPlateNameTextField,
            JCheckBox newPlateInfoManualSetCheckBox,
            JDialog plateInfoDialog,
            JDialog experimentsExplorerDialog) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                boolean repeatDialogFlag = false;
                do {
                    String expName = JOptionPane.showInputDialog(
                            experimentsExplorerDialog,
                            "Please, enter the name of the experiments:");
                    if (expName != null) {
                        expName = expName.trim();
                        try {
                            // Check if an experiment with the same name exists
                            Experiment alreadyExistingExpName
                                    = DbUtilities.getExperimentByName(expName);
                            if (alreadyExistingExpName == null) {
                                // Do not repeat this dialog anymore
                                repeatDialogFlag = false;
                                // Add the new experiment to the database
                                Experiment exp = new Experiment();
                                exp.setName(expName);
                                exp.setRowCount(0);
                                exp.setColCount(0);
                                DbUtilities.addExperiment(exp);
                                // Refresh the experiments list
                                GuiUtility.updateExperimentsList(
                                        experimentsList);
                                // Select the last experiment just added
                                GuiUtility.setSelectedExperiment(
                                        experimentsList, expName);
                                // Open the edit plate info dialog
                                GuiUtility.openEditPlateInfoDialog(
                                        experimentsList,
                                        newPlateInfoExpNameTextField,
                                        newPlateInfoRowCountFormattedTextField,
                                        newPlateInfoColCountFormattedTextField,
                                        newPlateInfoPlateNameTextField,
                                        newPlateInfoManualSetCheckBox,
                                        plateInfoDialog);
                            } else {
                                // Repeat this dialog
                                repeatDialogFlag = true;
                                // Display an error message
                                JOptionPane.showMessageDialog(
                                        experimentsExplorerDialog,
                                        "An experiment with the same name "
                                        + "already exists.",
                                        "Experiment Not Added",
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        } catch (ClassNotFoundException
                                | SQLException
                                | NoSuchMethodException
                                | InstantiationException
                                | IllegalAccessException
                                | InvocationTargetException ex) {
                            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {
                        // The user pressed the "Cancel" button. Do not display
                        // the input dialog once more.
                        repeatDialogFlag = false;
                    }
                } while (repeatDialogFlag);
            }
        });
    }

    /**
     * Event code for checking/un-checking the manual set plate info check box.
     *
     * @param newPlateInfoExpNameTextField the text field showing the name of
     * the experiment in the plate info dialog
     * @param newPlateInfoRowCountFormattedTextField the text field showing the
     * row count in the plate info dialog
     * @param newPlateInfoColCountFormattedTextField the text field showing the
     * column count in the plate info dialog
     * @param newPlateInfoPlateNameTextField the text field showing the name of
     * the plate in the plate info dialog
     * @param newPlateInfoManualSetCheckBox the check box that allows users to
     * set plate name manually
     */
    static void newPlateInfoManualSetCheckBoxAction(
            JTextField newPlateInfoExpNameTextField,
            JFormattedTextField newPlateInfoRowCountFormattedTextField,
            JFormattedTextField newPlateInfoColCountFormattedTextField,
            JTextField newPlateInfoPlateNameTextField,
            JCheckBox newPlateInfoManualSetCheckBox) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (newPlateInfoManualSetCheckBox.isSelected()) {
                    newPlateInfoPlateNameTextField.setEditable(true);
                } else {
                    newPlateInfoPlateNameTextField.setEditable(false);
                    GuiUtility.updatePlateInfoPlateNameTextFiled(
                            newPlateInfoExpNameTextField,
                            newPlateInfoRowCountFormattedTextField,
                            newPlateInfoColCountFormattedTextField,
                            newPlateInfoPlateNameTextField,
                            newPlateInfoManualSetCheckBox);
                }
            }
        });
    }

    /**
     * Event code for clicking the save button in the plate info dialog.
     *
     * @param experimentsList the GUI list of experiments
     * @param newPlateInfoRowCountFormattedTextField the text field showing the
     * row count in the plate info dialog
     * @param newPlateInfoColCountFormattedTextField the text field showing the
     * column count in the plate info dialog
     * @param newPlateInfoPlateNameTextField the text field showing the name of
     * the plate in the plate info dialog
     * @param plateInfoDialog the whole plate info dialog
     */
    static void newPlateInfoSaveButtonAction(
            JList<String> experimentsList,
            JFormattedTextField newPlateInfoRowCountFormattedTextField,
            JFormattedTextField newPlateInfoColCountFormattedTextField,
            JTextField newPlateInfoPlateNameTextField,
            JDialog plateInfoDialog) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Integer selectedExpId
                        = GuiUtility.getSelectedIds(experimentsList).get(0);
                try {
                    // Update the experiment object
                    Experiment expToBeUpdated
                            = DbUtilities.getExperimentById(selectedExpId);
                    Experiment updatedExperiment = new Experiment();
                    Integer rowCount
                            = (Integer) newPlateInfoRowCountFormattedTextField
                                    .getValue();
                    updatedExperiment.setRowCount(rowCount);
                    Integer colCount
                            = (Integer) newPlateInfoColCountFormattedTextField
                                    .getValue();
                    updatedExperiment.setColCount(colCount);
                    updatedExperiment.setPlateName(
                            newPlateInfoPlateNameTextField.getText().trim());
                    DbUtilities.updateExperiment(
                            expToBeUpdated, updatedExperiment);
                    // Remove any additional well that are no longer part of the
                    // plate (this happens in case of downsizing the plate i.e.
                    // reducing either its rows or columns)
                    List<WellDescription> wells = DbUtilities
                            .getWellsOfExperiment(expToBeUpdated.getId());
                    for (WellDescription well : wells) {
                        if (well.getRow() > rowCount
                                || well.getCol() > colCount) {
                            DbUtilities.deleteWellDescription(well);
                        }
                    }
                    // Close the dialog
                    plateInfoDialog.setVisible(false);
                } catch (ClassNotFoundException
                        | SQLException
                        | NoSuchMethodException
                        | InstantiationException
                        | IllegalAccessException
                        | InvocationTargetException ex) {
                    Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    /**
     * Event code for clicking fix paths button.
     *
     * @param experimentsList the GUI list of experiments
     * @param fixPathsExperimentsTextField the text field showing the names of
     * the experiments currently being considered in fixing
     * @param fixPathsCurrentPrefixTextField the text field showing (by default)
     * the longest common prefix among all paths of the experiment(s) under
     * consideration
     * @param fixPathsDialog the whole fix paths dialog
     */
    static void fixPathsButtonAction(
            JList<String> experimentsList,
            JTextField fixPathsExperimentsTextField,
            JTextField fixPathsCurrentPrefixTextField,
            JDialog fixPathsDialog) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    List<String> expNames
                            = GuiUtility.getSelectedNames(experimentsList);
                    fixPathsExperimentsTextField.setText("");
                    for (String expName : expNames) {
                        String commaOrNot;
                        if (fixPathsExperimentsTextField.getText().equals("")) {
                            commaOrNot = "";
                        } else {
                            commaOrNot = ", ";
                        }
                        fixPathsExperimentsTextField.setText(
                                String.format(
                                        "%s%s%s",
                                        fixPathsExperimentsTextField.getText(),
                                        commaOrNot,
                                        expName));
                    }
                    String commonPath = DbUtilities.getExperimentCommonPath(
                            GuiUtility.getSelectedIds(experimentsList));
                    fixPathsCurrentPrefixTextField.setText(commonPath);
                    GuiUtility.centerDialogOnScreen(fixPathsDialog);
                    fixPathsDialog.setVisible(true);
                } catch (ClassNotFoundException
                        | SQLException
                        | NoSuchMethodException
                        | InstantiationException
                        | IllegalAccessException
                        | InvocationTargetException ex) {
                    Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    /**
     * Event code for clicking the shorten button in the fix paths dialog.
     *
     * @param fixPathsCurrentPrefixTextField the text field showing (by default)
     * the longest common prefix among all paths of the experiment(s) under
     * consideration
     */
    static void fixPathsShortenPrefixButtonAction(
            JTextField fixPathsCurrentPrefixTextField) {
        String currentPrefix = fixPathsCurrentPrefixTextField.getText();
        if (currentPrefix.length() > 0) {
            fixPathsCurrentPrefixTextField.setText(
                    currentPrefix.substring(0, currentPrefix.length() - 1));
        }
    }

    /**
     * Event code for clicking the fix button in the fix paths dialog.
     *
     * @param experimentsList the GUI list of experiments
     * @param directoriesList the GUI list of directories
     * @param imagesList the GUI list of images
     * @param fixPathsCurrentPrefixTextField the text field showing (by default)
     * the longest common prefix among all paths of the experiment(s) under
     * consideration
     * @param fixPathsNewPrefixTextField the new prefix to substitute
     * fixPathsCurrentPrefixTextField
     * @param parentDialog parent dialog of all messages to be displayed
     */
    static void fixPathFixButtonAction(
            JList<String> experimentsList,
            JList<String> directoriesList,
            JList<String> imagesList,
            JTextField fixPathsCurrentPrefixTextField,
            JTextField fixPathsNewPrefixTextField,
            JDialog parentDialog) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    String commonPathPrefix
                            = fixPathsCurrentPrefixTextField.getText();
                    String newPathPrefix
                            = fixPathsNewPrefixTextField.getText();
                    // Get all current selected experiments
                    List<Integer> expIds = GuiUtility.getSelectedIds(
                            experimentsList);
                    for (Integer expId : expIds) {
                        // Loop over the directories of the current experiment
                        List<Directory> dirs
                                = DbUtilities.getDirectoriesOfExperiment(expId);
                        for (Directory originalDir : dirs) {
                            // Update the path of the current directory. Notice
                            // that all back slashes are automatically replaced
                            // with slashes to maintain compatability across
                            // different operating systems.
                            Directory updatedDir = new Directory();
                            updatedDir.setPath(
                                    GuiUtility.replaceBackSlashWithSlash(
                                            newPathPrefix + originalDir.getPath()
                                                    .substring(commonPathPrefix.length())));
                            DbUtilities.updateDirectory(
                                    originalDir, updatedDir);
                            // Loop over the images of the current directory
                            List<Image> images
                                    = DbUtilities.getImagesOfDirectory(
                                            originalDir.getId());
                            for (Image originalImage : images) {
                                // Update the path of the current image. Notice
                                // that all back slashes are automatically
                                // replaced with slashes to maintain
                                // compatability across different operating
                                // systems.
                                Image updatedImage = new Image();
                                updatedImage.setPath(
                                        GuiUtility.replaceBackSlashWithSlash(
                                                newPathPrefix + originalImage.getPath()
                                                        .substring(commonPathPrefix.length())));
                                DbUtilities.updateImage(
                                        originalImage, updatedImage);
                            }
                        }
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                // Update directories list
                                GuiUtility.updateDirectoriesList(
                                        experimentsList, directoriesList);
                                // If the directories list has any items select 
                                // all of them.
                                if (directoriesList.getModel().getSize() > 0) {
                                    directoriesList.setSelectionInterval(
                                            0,
                                            directoriesList.getModel().getSize()
                                            - 1);
                                }
                                // Update images list
                                GuiUtility.updateImagesList(
                                        directoriesList, imagesList);
                            } catch (ClassNotFoundException
                                    | SQLException
                                    | NoSuchMethodException
                                    | InstantiationException
                                    | IllegalAccessException
                                    | InvocationTargetException ex) {
                                Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    });
                    // Display successful message
                    JOptionPane.showMessageDialog(
                            parentDialog,
                            "Paths fixed successfully",
                            "Paths fixed",
                            JOptionPane.INFORMATION_MESSAGE);
                    parentDialog.setVisible(false);
                } catch (ClassNotFoundException
                        | SQLException
                        | NoSuchMethodException
                        | InstantiationException
                        | IllegalAccessException
                        | InvocationTargetException ex) {
                    Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    /**
     * Event code for clicking the remove image button.
     *
     * @param experimentsList GUI list of experiments
     * @param directoriesList GUI list of directories
     * @param imagesList GUI list of images
     * @param removeExperimentButton button for removing experiment(s)
     * @param addDirectoryButton button for adding a new directory
     * @param removeDirectoryButton button for removing director(y/ies)
     * @param removeImageButton button for removing image(s)
     * @param filterImageButton button for filtering images
     * @param fixPathsButton button for fixing paths
     */
    static void removeImageButtonAction(
            JList<String> experimentsList,
            JList<String> directoriesList,
            JList<String> imagesList,
            JButton removeExperimentButton,
            JButton addDirectoryButton,
            JButton removeDirectoryButton,
            JButton removeImageButton,
            JButton filterImageButton,
            JButton fixPathsButton,
            ScrollImagePanel scrollImagePanel,
            JDialog parentDialog) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                int showConfirmDialogResult = JOptionPane.showConfirmDialog(
                        parentDialog,
                        "Are you sure you want to remove all selected images "
                        + "from the database?");
                if (showConfirmDialogResult == JOptionPane.YES_OPTION) {
                    try {
                        // Get all selected images IDs
                        List<Integer> imageIds
                                = GuiUtility.getSelectedIds(imagesList);
                        // Delete all selected images
                        for (Integer imageId : imageIds) {
                            Image image = new Image();
                            image.setId(imageId);
                            DbUtilities.deleteImage(image);
                        }
                        // Update the image traversal
                        GuiUtility.updateFileTraversal(
                                experimentsList, scrollImagePanel);
                        // Refresh the GUI list of images
                        GuiUtility.updateImagesList(directoriesList, imagesList);
                        // Refresh buttons (update their enable/disable status)
                        GuiUtility.updateButtonsStatus(
                                experimentsList,
                                directoriesList,
                                imagesList,
                                removeExperimentButton,
                                addDirectoryButton,
                                removeDirectoryButton,
                                removeImageButton,
                                filterImageButton,
                                fixPathsButton);
                    } catch (ClassNotFoundException
                            | SQLException
                            | NoSuchMethodException
                            | InstantiationException
                            | IllegalAccessException
                            | InvocationTargetException ex) {
                        Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
    }

    /**
     * Add well information to the display
     *
     * @param scrollImagePanel the panel used to display current image
     */
    private static void addImageAnnotations(
            ScrollImagePanel scrollImagePanel) {
        try {
            // Get directory of currently displayed image
            Directory dir = DbUtilities.getDirectorById(
                    currentlyDisplayedImage.getParentDirId());
            // Get currently displayed image information
            Experiment exp = DbUtilities.getExperimentById(
                    dir.getExperimentId());
            int row = getWellRowFromImage(currentlyDisplayedImage);
            int col = getWellColFromImage(currentlyDisplayedImage);
            ImagePanel imagePanel = (ImagePanel) scrollImagePanel.getCanvas();
            imagePanel.clearComments();
            imagePanel.addComment(
                    String.format("%s: %s",
                            "Experiment",
                            exp.getName()));
            imagePanel.addComment(
                    String.format("%s: %s",
                            "Image",
                            new File(currentlyDisplayedImage.getPath())
                                    .getName()));
            if (row == -1 || col == -1) {
                String message = "Image file name must have row and column "
                        + "information in the format *rX*cY* where X and Y "
                        + "represent row and column indices resprectively and "
                        + "the asterik represents any number of characters.";
                // Add the error message to the comments of the displayed image
                imagePanel.addComment(String.format(
                        "%s: %s",
                        "Error",
                        message));
                // Display error message (dialog)
                JOptionPane.showMessageDialog(
                        scrollImagePanel,
                        message,
                        "Image File Name Error",
                        JOptionPane.ERROR_MESSAGE);
            } else {
                if (row > exp.getRowCount() || col > exp.getColCount()) {
                    String message = String.format(
                            "Image well location (%dx%d) exceeds "
                            + "experiment %s plate size (%dx%d)",
                            row, col,
                            exp.getName(),
                            exp.getRowCount(), exp.getColCount());
                    // Add the error message to the comments of the displayed image
                    imagePanel.addComment(String.format(
                            "%s: %s",
                            "Error",
                            message));
                    // Display error message (dialog)
                    JOptionPane.showMessageDialog(
                            scrollImagePanel,
                            message,
                            "Well Location Error",
                            JOptionPane.ERROR_MESSAGE);
                } else {
                    // Get the well corresponding to the current displayed image
                    WellDescription well = DbUtilities.getWell(exp.getId(), row, col);
                    if (well == null) {
                        // Add the no-info message to the comments of the displayed
                        // image
                        imagePanel.addComment(String.format(
                                "%s: %s",
                                "Warning",
                                "No information available"));
                    } else {
                        // Impose the information over the displayed image
                        imagePanel.addComment(
                                String.format("%s: %dx%d",
                                        "Well",
                                        well.getRow(),
                                        well.getCol()));
                        imagePanel.addComment(
                                String.format("%s: %s",
                                        "Compound",
                                        well.getCompound()));
                        imagePanel.addComment(
                                String.format("%s: %f",
                                        "Concentration",
                                        well.getConcentration()));
                        imagePanel.addComment(
                                String.format("%s: %s",
                                        "Cell Type",
                                        well.getCellType()));
                        imagePanel.addComment(
                                String.format("%s: %d",
                                        "Cell Count",
                                        well.getCellCount()));
                    }
                }
            }
        } catch (ClassNotFoundException
                | SQLException
                | NoSuchMethodException
                | InstantiationException
                | IllegalAccessException
                | InvocationTargetException ex) {
            Logger.getLogger(GuiUtility.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Get row index from image path
     *
     * @param currentlyDisplayedImage an image
     * @return row index (starting at 1) or -1 if no row information is provided
     */
    private static Integer getWellRowFromImage(Image image) {
        String fileName = new File(image.getPath()).getName();
        Pattern pattern = Pattern.compile("r\\d+");
        Matcher matcher = pattern.matcher(fileName);
        if (matcher.find()) {
            String rowIndexString = matcher.group();
            return new Integer(rowIndexString.substring(1));
        } else {
            return -1;
        }
    }

    /**
     * Get column index from image path
     *
     * @param currentlyDisplayedImage an image
     * @return column index (starting at 1) or -1 if no column information is
     * provided
     */
    private static Integer getWellColFromImage(Image image) {
        String fileName = new File(image.getPath()).getName();
        Pattern pattern = Pattern.compile("c\\d+");
        Matcher matcher = pattern.matcher(fileName);
        if (matcher.find()) {
            String rowIndexString = matcher.group();
            return new Integer(rowIndexString.substring(1));
        } else {
            return -1;
        }
    }

    /**
     * Update experiments combo box.
     *
     * @param experimentsCombobox
     * @param experimentsList
     * @param directoriesList
     * @param scrollImagePanel
     */
    static void updateExperimentsCombobox(
            JComboBox<String> experimentsCombobox,
            JList<String> experimentsList,
            JList<String> directoriesList,
            ScrollImagePanel scrollImagePanel,
            LumenMap lumenMap) {
        try {
            // Load all experiments to the experiments combobox if 
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            List<Experiment> experiments = DbUtilities.getAllExperiments();
            for (Experiment exp : experiments) {
                model.addElement(String.format(
                        "%d|%s", exp.getId(), exp.getName()));
            }
            experimentsCombobox.setModel(model);
            // If an image is displayed change the combo box selection do that
            // it matches the experiment of the displayed image
            if (currentlyDisplayedImage != null) {
                Directory dir = DbUtilities.getDirectorById(
                        currentlyDisplayedImage.getParentDirId());
                Experiment exp = DbUtilities.getExperimentById(
                        dir.getExperimentId());
                for (int i = 0;
                        i < experimentsCombobox.getModel().getSize();
                        i++) {
                    Integer comboboxId = GuiUtility.extractIdFromItem(
                            experimentsCombobox.getModel().getElementAt(i));
                    if (comboboxId.equals(exp.getId())) {
                        experimentsCombobox.setSelectedIndex(i);
                        break;
                    }
                }
            } else {
                List<Integer> selectedIds = GuiUtility.getSelectedIds(experimentsList);
                if (selectedIds.isEmpty()) {
                    // If no image is displayed select the first experiment in 
                    // both the combobox and the GUI list of experiments and 
                    // display its first image
                    // Load all experiments
                    GuiUtility.loadExperiments(experimentsList, directoriesList);
                    // Update fileFilter
                    GuiUtility.updateFileTraversal(
                            experimentsList, scrollImagePanel);
                    // Display the nextImage
                    if (fileTraversal.hasNext()) {
                        displayNext(fileTraversal, scrollImagePanel);
                        refreshAnnotations(scrollImagePanel, lumenMap);
                    }
                } else {
                    Integer selectedId = selectedIds.get(0);
                    for (int i = 0;
                            i < experimentsCombobox.getModel().getSize();
                            i++) {
                        if (GuiUtility.extractIdFromItem(
                                experimentsCombobox.getModel().getElementAt(i))
                                .equals(selectedId)) {
                            experimentsCombobox.setSelectedIndex(i);
                        }
                    }
                }
            }
        } catch (ClassNotFoundException
                | SQLException
                | NoSuchMethodException
                | InstantiationException
                | IllegalAccessException
                | InvocationTargetException ex) {
            Logger.getLogger(GuiUtility.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Event code for changing selection in the experiments combo box.
     *
     * @param experimentsComboBox
     * @param experimentsList
     */
    static void experimentsComboBoxAction(
            JComboBox<String> experimentsComboBox,
            JList<String> experimentsList) {
        Integer comboboxId = GuiUtility.extractIdFromItem(
                (String) experimentsComboBox.getModel().getSelectedItem());
        for (int i = 0; i < experimentsList.getModel().getSize(); i++) {
            Integer listId = GuiUtility.extractIdFromItem(
                    experimentsList.getModel().getElementAt(i));
            if (listId.equals(comboboxId)) {
                experimentsList.setSelectedIndex(i);
                break;
            }
        }
    }

    /**
     * Used to check if an image is currently displayed.
     *
     * @return true if an image is currently displayed, false otherwise
     */
    static boolean isImageCurrentlyDisplayed() {
        return currentlyDisplayedImage != null;
    }

    /**
     * Load channels 1 and 2 od each combined image
     *
     * @param experimentsList
     * @param parentDialog
     */
    static void loadChannels(
            JList<String> experimentsList,
            JDialog parentDialog) {
        List<Integer> expIds = GuiUtility.getSelectedIds(experimentsList);
        if (expIds.size() == 1) {
            try {
                List<Image> images
                        = DbUtilities.getImagesOfExperiment(expIds.get(0));
                for (Image image : images) {
                    addChannel(image, 1);
                    addChannel(image, 2);
                }
            } catch (SQLException
                    | ClassNotFoundException
                    | NoSuchMethodException
                    | InstantiationException
                    | IllegalAccessException
                    | InvocationTargetException ex) {
                Logger.getLogger(GuiUtility.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            JOptionPane.showMessageDialog(
                    parentDialog,
                    "Only one epxeriment must be selected",
                    "Select One Experiment",
                    JOptionPane.ERROR);
        }
    }

    private static void addChannel(Image image, int chIndex)
            throws
            NoSuchMethodException,
            ClassNotFoundException,
            IllegalAccessException,
            InvocationTargetException,
            SQLException,
            InstantiationException {
        File imageFile = new File(image.getPath());
        if (imageFile.getName().contains("combined")) {
            Pattern regex = Pattern.compile("r\\d+c\\d+f\\d+p\\d+");
            String[] splits = imageFile.getName().split("\\.");
            String extension = splits[splits.length - 1];
            Matcher matcher = regex.matcher(imageFile.getName());
            if (matcher.find()) {
                // Add channel 1 to the database if it exists
                String chFileName = String.format(
                        "%s-ch%d.%s", matcher.group(), chIndex, extension);
                File chFile = new File(
                        imageFile.getParent()
                        + File.separator
                        + chFileName);
                if (chFile.exists()) {
                    Image chImage = new Image();
                    chImage.setParentDirId(image.getParentDirId());
                    chImage.setPath(chFile.getPath());
                    DbUtilities.addImage(chImage);
                }
            }
        } else {
            throw new IllegalArgumentException(
                    String.format("%s does not follow the naming convention of "
                            + "a combined image.", imageFile.getPath()));
        }
    }

    /**
     * Export all the images to an output directory
     *
     * @param scrollImagePanel the panel used to display images
     * @param lumenMap a nested map of automatically identified lumen
     */
    static void exportAllImages(
            ScrollImagePanel scrollImagePanel,
            LumenMap lumenMap) {
        JFileChooser dirChooser = new JFileChooser(new File("F:\\data\\19JUN2020_truncated"));
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (dirChooser.showOpenDialog(scrollImagePanel) == JFileChooser.APPROVE_OPTION) {
            new Thread() {
                public void run() {
                    // Loop over files and write them one by one
                    fileTraversal.resetIndex();
                    while (fileTraversal.hasNext()) {
                        // Display the next image
                        displayNext(fileTraversal, scrollImagePanel);
                        // Make sure the annotations are displayed correctly
                        refreshAnnotations(scrollImagePanel, lumenMap);
                        // Get your output file
                        String fileName = new File(
                                fileTraversal.getCurrent().getPath()).getName();
                        File outFile = new File(
                                dirChooser.getSelectedFile().getPath()
                                + File.separator
                                + fileName);
                        // Write the current image to a file
//                        FileOutputStream fos = null;
                        try {
                            BufferedImage awtImage = new BufferedImage(
                                    scrollImagePanel.getWidth(),
                                    scrollImagePanel.getHeight(),
                                    BufferedImage.TYPE_INT_RGB);
                            Graphics g = awtImage.getGraphics();
                            scrollImagePanel.printAll(g);

                            ImageIO.write(awtImage, "jpg", outFile);

//                            fos = new FileOutputStream(outFile);
//                            JPEGImageEncoderImpl encoder = new JPEGImageEncoderImpl(fos);
//                            encoder.encode(awtImage);
//                            fos.close();
                        } catch (IOException ex) {
                            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
                        }
//                        finally {
//                            try {
//                                fos.close();
//                            } catch (IOException ex) {
//                                Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
//                            }
//                        }
                    }
                    JOptionPane.showMessageDialog(
                            scrollImagePanel,
                            "All Images Exported Successfully.",
                            "Images Exported",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }.start();
        }
    }

    /**
     * Return a copy of the lumen map that does not have lumens that have no
     * coinciding lumens directly up or down.
     *
     * @param lumenMap the lumen map to be filtered
     * @return a single lumen free lumen map
     * @throws IOException
     */
    static LumenMap filterOutSingularLumens(LumenMap lumenMap) throws IOException {
        // Filtered lumen map
        LumenMap filteredLumenMap = new LumenMap();
        // Check the plane stack of all row-column-field combinations
        for (int row = lumenMap.getMinRow(); row <= lumenMap.getMaxRow(); row++) {
            for (int col = lumenMap.getMinColumn(); col <= lumenMap.getMaxColumn(); col++) {
                for (int field = lumenMap.getMinField(); field <= lumenMap.getMaxField(); field++) {
                    List<List<File>> lumenInfoFilesAllPlanes
                            = lumenMap.getLumenInfoFileAllPlanesLowerToHigher(
                                    row, col, field);
                    // Get all corner point files.
                    List<List<List<Point>>> allCornerPoints = new ArrayList<>();
                    for (int i = 0; i < lumenInfoFilesAllPlanes.size(); i++) {
                        allCornerPoints.add(new ArrayList<>());
                        if (lumenInfoFilesAllPlanes.get(i) != null) {
                            for (int j = 0; j < lumenInfoFilesAllPlanes.get(i).size(); j++) {
                                List<Point> cornerPoints = MachineLearning.getTightCorners(
                                        IO.getOutlinePoints(lumenInfoFilesAllPlanes.get(i).get(j)));
                                allCornerPoints.get(allCornerPoints.size() - 1)
                                        .add(cornerPoints);
                            }
                        }
                    }
                    // Loop over all planes
                    for (int i = 0; i < allCornerPoints.size(); i++) {
                        // Loop over the lumen corners of the current plane
                        for (int j = 0; j < allCornerPoints.get(i).size(); j++) {
                            boolean hasCoincidingUpperLumen = false;
                            boolean hasCoincidingLowerLumen = false;
                            if (i < allCornerPoints.size() - 1) {
                                // Check the upper plane
                                List<List<Point>> upperLumensCorners = allCornerPoints.get(i + 1);
                                for (List<Point> singleUpperLumenCorners : upperLumensCorners) {
                                    if (MachineLearning.areOverlapping(
                                            allCornerPoints
                                                    .get(i)
                                                    .get(j),
                                            singleUpperLumenCorners)) {
                                        hasCoincidingUpperLumen = true;
                                        break;
                                    }
                                }
                            }
                            if (!hasCoincidingUpperLumen) {
                                if (i > 0) {
                                    // Check the lower plane
                                    List<List<Point>> lowerLumensCorners = allCornerPoints.get(i - 1);
                                    for (List<Point> singleLowerLumenCorners : lowerLumensCorners) {
                                        if (MachineLearning.areOverlapping(
                                                allCornerPoints
                                                        .get(i)
                                                        .get(j),
                                                singleLowerLumenCorners)) {
                                            hasCoincidingLowerLumen = true;
                                            break;
                                        }
                                    }
                                }
                            }
                            // If the lumen does not have both lower and upper
                            if (hasCoincidingUpperLumen || hasCoincidingLowerLumen) {
                                RowColumnFieldPlane rcfp = new RowColumnFieldPlane(
                                        lumenInfoFilesAllPlanes.get(i).get(j));
                                filteredLumenMap.add(row, col, field, rcfp.getPlane(),
                                        lumenInfoFilesAllPlanes.get(i).get(j));
                            }
                        }
                    }
                }
            }
        }
        // Return the filtered map
        return filteredLumenMap;
    }

    static void clearIdentifiedLumens(
            LumenMap lumenMap, 
            ScrollImagePanel scrollImagePanel) {
        if (lumenMap != null && currentlyDisplayedImage != null) {
            RowColumnFieldPlane rcfp = new RowColumnFieldPlane(
                    new File(currentlyDisplayedImage.getPath()));
            lumenMap.removeAll(
                    rcfp.getRow(),
                    rcfp.getColumn(),
                    rcfp.getField(),
                    rcfp.getPlane());
            refreshDisplayedImage(fileTraversal, scrollImagePanel);
            refreshAnnotations(scrollImagePanel, lumenMap);
        }
    }
}
