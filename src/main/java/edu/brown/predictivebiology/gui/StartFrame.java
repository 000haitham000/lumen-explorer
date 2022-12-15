/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.brown.predictivebiology.gui;

import edu.brown.predictivebiology.utilities.IO;
import com.mathworks.toolbox.javabuilder.MWException;
import edu.brown.predictivebiology.db.DbUtilities;
import edu.brown.predictivebiology.db.beans.Experiment;
import edu.brown.predictivebiology.cellcount.CheckBoxOfTableColumn;
import edu.brown.predictivebiology.cellcount.Record;
import edu.brown.predictivebiology.cellcount.Utilities;
import edu.brown.predictivebiology.db.beans.Image;
import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import edu.brown.predictivebiology.utilities.LumenMap;
import edu.brown.predictivebiology.utilities.MachineLearning;
import java.net.URISyntaxException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author Haitham
 */
public class StartFrame extends javax.swing.JFrame {

    private static final String INT_REGEX = "[0-9]+"; // Integer regex
    private static final String RANGE_REGEX = "[0-9]+\\s*\\-\\s*[0-9]+"; // Range regex (arbitrary within spaces accepted)
    private static final String SINGLE = "(" + INT_REGEX + "|" + RANGE_REGEX + ")"; // A single integer or range regex
    private static final String MULTIPLE_COMMA_SEPARATED = "(" + SINGLE + "|" + "(" + SINGLE + "\\s*,\\s*)+" + SINGLE + ")"; // This regex represents either a single or a comma separated list of integers and ranges with any order (aribitrary within spaces accepted)
    private static final String ALL_IN_ONE_REGEX = MULTIPLE_COMMA_SEPARATED + "?"; // Same as multipleCommaSeparated with only the addition of accepting empty strings
    private static final Pattern INT_PATTERN = Pattern.compile(INT_REGEX);
    private static final Pattern EXCLUSIONS_PATTERN = Pattern.compile(ALL_IN_ONE_REGEX);
    private static BufferedImage lumenTaggingImage = null;

    static {
        try {
            DbUtilities.createDb();
            lumenTaggingImage = ImageIO.read(
                    StartFrame.class.getResource("/images/lumenTaggingImages-small.png"));
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private final ProgressPanel extractPotentialLumensProgressPanel;

    // List of all checkboxes along with their corresponding table columns
    List<CheckBoxOfTableColumn> checkBoxs = new ArrayList<>();
    Vector<String> originalColumnLabels = new Vector<>();
    Vector<Vector<String>> originalData = new Vector<>();
    // A mapping structure storing all the identidfied lumen info-files
    LumenMap lumenMap;

    private void resetAll() {
        // Remove all checkBoxes
        clearCheckBoxes();
        // Clear the table
        viewTable.setModel(new DefaultTableModel());
        // Clear column lables vector
        originalColumnLabels.clear();
        // Clear original data
        originalData.clear();
    }

    private void clearCriteriaComboBoxes() {
        // Clear Comboboxes
        ((DefaultComboBoxModel<String>) sortingComboBox1.getModel()).removeAllElements();
        ((DefaultComboBoxModel<String>) sortingComboBox2.getModel()).removeAllElements();
        ((DefaultComboBoxModel<String>) sortingComboBox3.getModel()).removeAllElements();
        ((DefaultComboBoxModel<String>) sortingComboBox4.getModel()).removeAllElements();
    }

    void fillInData(List<Record> records) {
        // Reset everything
        resetAll();
        // Fill in columns labels (headers)
        originalColumnLabels.add("row");
        originalColumnLabels.add("column");
        originalColumnLabels.add("plane");
        originalColumnLabels.add("field");
        originalColumnLabels.addAll(records.get(0).getAdditionalColumnsLabels());
        // Fill in data
        for (Record record : records) {
            Vector<String> dataRecord = new Vector<>();
            // Add the four main attributes (row, column, plane and field)
            dataRecord.add(String.valueOf(record.getRow()));
            dataRecord.add(String.valueOf(record.getColumn()));
            dataRecord.add(String.valueOf(record.getPlane()));
            dataRecord.add(String.valueOf(record.getField()));
            // Add additional columns
            for (String additionalColumnLabel : record.getAdditionalColumnsLabels()) {
                dataRecord.add(record.getMetaInfo(additionalColumnLabel));
            }
            // Add the now-complete record to the data
            originalData.add(dataRecord);
        }
        // Add the data to the table
        viewTable.setModel(
                new DefaultTableModel(originalData, originalColumnLabels) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        }
        );
        // Add checkBoxes
        for (int i = 0; i < originalColumnLabels.size(); i++) {
            CheckBoxOfTableColumn checkBoxOfTableColumn = new CheckBoxOfTableColumn(
                    originalColumnLabels.get(i),
                    true,
                    i,
                    originalData.get(i));
            checkBoxOfTableColumn.getCheckBox().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updatedTable();
                }
            });
            addCheckBox(checkBoxOfTableColumn);
        }
    }

    private void fillCriteriaComboboxes() {
        // Fill in sorting combo boxes
        fillInColumnLabels(sortingComboBox1);
        sortingComboBox1.setSelectedIndex(-1);
        fillInColumnLabels(sortingComboBox2);
        sortingComboBox2.setSelectedIndex(-1);
        fillInColumnLabels(sortingComboBox3);
        sortingComboBox3.setSelectedIndex(-1);
        fillInColumnLabels(sortingComboBox4);
        sortingComboBox4.setSelectedIndex(-1);
    }

    /**
     * Creates new form StartFrame
     */
    public StartFrame() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedLookAndFeelException ex) {
            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        setLocationByPlatform(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        initComponents();
        // Set the layout of the panel containing the check-boxes
        boxPanel.setLayout(new BoxLayout(boxPanel, BoxLayout.Y_AXIS));
        // Update other components as needed
        inputFilePathTextField.setEditable(false);
        try {
            // Set frame icon
            //setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("../images/cell.png")));
            BufferedImage icon = ImageIO.read(Toolkit.getDefaultToolkit().getClass().getResourceAsStream("/images/cell.png"));
            setIconImage(icon);
            experimentsExplorerDialog.setIconImage(icon);
        } catch (IOException ex) {
            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        // Put your progress bar in the intermediate mode.
        loadImagesProgressBar.setIndeterminate(true);
        // Set lumen tagging image
        scrollImagePanel.setTaggingImage(lumenTaggingImage);
        // Add mouse listener to the drawing canvas
        scrollImagePanel.getCanvas().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (taggingModeCheckBox.isSelected()) {
                    GuiUtility.tagLumen(
                            evt,
                            scrollImagePanel,
                            experimentsList,
                            lumenCounterLabel);
                }
            }
        });
        // Update lumen count display
        GuiUtility.refreshLumenCountDisplay(experimentsList, lumenCounterLabel);
        // Modify all list models
        experimentsList.setModel(new DefaultListModel<>());
        directoriesList.setModel(new DefaultListModel<>());
        imagesList.setModel(new DefaultListModel<>());
        filterImageList.setModel(new DefaultListModel<>());
        // Add list renderer to lists
        experimentsList.setCellRenderer(new IdBarNameRenderer());
        directoriesList.setCellRenderer(new IdBarNameRenderer());
        imagesList.setCellRenderer(new IdBarNameRenderer());
        filterImageList.setCellRenderer(new IdBarNameRenderer());
        // Add renderer to combobox
        filterDirectoryComboBox.setRenderer(new IdBarNameRenderer());
        // Disable jump to selection button
        jumpToSelectionButton.setEnabled(false);
        // Set plate-map table selection mode
        wellMapTable.getColumnModel().setColumnSelectionAllowed(true);
        wellMapTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        // Set formatted-text-field formats
        wellConcentrationFormattedTextField.setValue(0.0);
        wellCellCountFormattedTextField.setValue(0);
        newPlateInfoRowCountFormattedTextField.setValue(0);
        newPlateInfoColCountFormattedTextField.setValue(0);
        // Add pop-up menu to the plate map table
        wellMapTable.setComponentPopupMenu(wellPopupMenu);
        // Add pop-up menu to the experiments list
        experimentsList.setComponentPopupMenu(experimentPopupMenu);
        // Set the value changed event for plate info row and column count 
        // fields.
        GuiUtility.addTextChangeListeners(
                newPlateInfoExpNameTextField,
                newPlateInfoRowCountFormattedTextField,
                newPlateInfoColCountFormattedTextField,
                newPlateInfoPlateNameTextField,
                newPlateInfoManualSetCheckBox,
                newPlateInfoSaveButton
        );
        // Set experimeny combobox renderer
        experimentsComboBox.setRenderer(new IdBarNameRenderer());
        // Update the experiments combobox
        GuiUtility.updateExperimentsCombobox(
                experimentsComboBox,
                experimentsList,
                directoriesList,
                scrollImagePanel,
                lumenMap);
        if (GuiUtility.isImageCurrentlyDisplayed()) {
            // Update main dialog buttons
            previousImageButton.setEnabled(true);
            nextImageButton.setEnabled(true);
            clearImageButton.setEnabled(true);
        }
        // Add extract potential lumens components
        extractPotentialLumensProgressPanel = new ProgressPanel(
                experimentsExplorerDialog, experimentsList);
        extractPotentialLumensDialog.add(extractPotentialLumensProgressPanel);
    }

    private class IdBarNameRenderer implements ListCellRenderer<String> {

        @Override
        public Component getListCellRendererComponent(
                JList<? extends String> list,
                String value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            if (value != null) {
                JLabel label = new JLabel(value.split("\\|")[1]);
                label.setOpaque(true);
                if (isSelected) {
                    label.setBackground(Color.LIGHT_GRAY);
                } else {
                    label.setBackground(list.getBackground());
                }
                return label;
            } else {
                return new JLabel();
            }
        }
    }

    /**
     * Adds a new check-box (along with its corresponding column index) to both
     * the panel and the internal list
     *
     * @param checkBoxOfTableColumn the check-box/column-index combination to be
     * added
     */
    private void addCheckBox(CheckBoxOfTableColumn checkBoxOfTableColumn) {
        // Add checkbox to the list of checkboxes
        checkBoxs.add(checkBoxOfTableColumn);
        // Add the checkbox to the panel
        boxPanel.add(checkBoxOfTableColumn.getCheckBox());
        // Make sure it shows up
        boxPanel.revalidate();
        boxPanel.repaint();
    }

    /**
     * Removes check-box (along with its corresponding column index) from both
     * the panel and the internal list
     *
     * @param checkBoxOfTableColumn the check-box/column-index combination to be
     * removed
     */
    private void removeCheckBox(CheckBoxOfTableColumn checkBoxOfTableColumn) {
        // Remove the checkbox from both the list and the panel
        boxPanel.remove(checkBoxOfTableColumn.getCheckBox());
        // Make sure it disappears
        boxPanel.revalidate();
        boxPanel.repaint();
    }

    /**
     * Removes all check-boxes (along with their corresponding columns indices)
     * from the panel and the internal list
     */
    private void clearCheckBoxes() {
        while (!checkBoxs.isEmpty()) {
            CheckBoxOfTableColumn checkBoxOfTableColumn = checkBoxs.remove(checkBoxs.size() - 1);
            removeCheckBox(checkBoxOfTableColumn);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        fileChooser = new javax.swing.JFileChooser();
        loadImagesProgressDialog = new javax.swing.JDialog(experimentsExplorerDialog, true);
        loadImagesProgressBar = new javax.swing.JProgressBar();
        loadingImagesProgressLabel = new javax.swing.JLabel();
        loadingImagesProgressButton = new javax.swing.JButton();
        imagesDirChooser = new javax.swing.JFileChooser();
        dbDumpFileChooser = new javax.swing.JFileChooser();
        experimentsExplorerDialog = new javax.swing.JDialog(this, true);
        jPanel1 = new javax.swing.JPanel();
        experimentsLabel = new javax.swing.JLabel();
        directoriesLabel = new javax.swing.JLabel();
        imagesLabel = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        experimentsList = new javax.swing.JList<>();
        jScrollPane5 = new javax.swing.JScrollPane();
        directoriesList = new javax.swing.JList<>();
        jScrollPane6 = new javax.swing.JScrollPane();
        imagesList = new javax.swing.JList<>();
        addExperimentButton = new javax.swing.JButton();
        removeExperimentButton = new javax.swing.JButton();
        addDirectoryButton = new javax.swing.JButton();
        removeDirectoryButton = new javax.swing.JButton();
        removeImageButton = new javax.swing.JButton();
        filterImageButton = new javax.swing.JButton();
        fixPathsButton = new javax.swing.JButton();
        dismissButton = new javax.swing.JButton();
        jumpToSelectionButton = new javax.swing.JButton();
        wellMapDialog = new javax.swing.JDialog(experimentsExplorerDialog, true);
        jScrollPane7 = new javax.swing.JScrollPane();
        wellMapTable = new javax.swing.JTable();
        dismissWellsDialogButton = new javax.swing.JButton();
        wellPopupMenu = new javax.swing.JPopupMenu();
        wellEditInfoMenuItem = new javax.swing.JMenuItem();
        wellCopyInfoMenuItem = new javax.swing.JMenuItem();
        wellPasteInfoMenuItem = new javax.swing.JMenuItem();
        wellDeleteInfoMenuItem = new javax.swing.JMenuItem();
        wellEditDialog = new javax.swing.JDialog(wellMapDialog, true);
        wellExperimentLabel = new javax.swing.JLabel();
        wellRowColumnLabel = new javax.swing.JLabel();
        wellCompoundLabel = new javax.swing.JLabel();
        wellConcentrationLabel = new javax.swing.JLabel();
        wellCellTypeLabel = new javax.swing.JLabel();
        wellCellCountLabel = new javax.swing.JLabel();
        wellExperimentTextField = new javax.swing.JTextField();
        wellRowColumnTextField = new javax.swing.JTextField();
        wellCompoundTextField = new javax.swing.JTextField();
        wellConcentrationFormattedTextField = new javax.swing.JFormattedTextField();
        wellCellTypeTextField = new javax.swing.JTextField();
        wellCellCountFormattedTextField = new javax.swing.JFormattedTextField();
        wellSaveButton = new javax.swing.JButton();
        wellCancelButton = new javax.swing.JButton();
        imageFilterDialog = new javax.swing.JDialog(experimentsExplorerDialog);
        filterExperimentLabel = new javax.swing.JLabel();
        filterDirectoryLabel = new javax.swing.JLabel();
        filterImageFilterMaskLabel = new javax.swing.JLabel();
        filterExperimentTextField = new javax.swing.JTextField();
        filterDirectoryComboBox = new javax.swing.JComboBox<>();
        filterImageFilterMaskTextField = new javax.swing.JTextField();
        filterDismisslButton = new javax.swing.JButton();
        filterDeleteSelectedImagesButton = new javax.swing.JButton();
        filterKeepSelectedButton = new javax.swing.JButton();
        jScrollPane8 = new javax.swing.JScrollPane();
        filterImageList = new javax.swing.JList<>();
        filterApplyFilterButton = new javax.swing.JButton();
        plateInfoDialog = new javax.swing.JDialog(experimentsExplorerDialog);
        newPlateInfoExpNameLabel = new javax.swing.JLabel();
        newPlateInfoRowCountLabel = new javax.swing.JLabel();
        newPlateInfoColCountLabel = new javax.swing.JLabel();
        newPlateInfoPlateNameLabel = new javax.swing.JLabel();
        newPlateInfoExpNameTextField = new javax.swing.JTextField();
        newPlateInfoRowCountFormattedTextField = new javax.swing.JFormattedTextField();
        newPlateInfoColCountFormattedTextField = new javax.swing.JFormattedTextField();
        newPlateInfoPlateNameTextField = new javax.swing.JTextField();
        newPlateInfoManualSetCheckBox = new javax.swing.JCheckBox();
        newPlateInfoDismissButton = new javax.swing.JButton();
        newPlateInfoSaveButton = new javax.swing.JButton();
        experimentPopupMenu = new javax.swing.JPopupMenu();
        openPlateMapMenuItem = new javax.swing.JMenuItem();
        editPlateInfoMenuItem = new javax.swing.JMenuItem();
        trainLumenClassifier = new javax.swing.JMenuItem();
        extractPotentialLumens = new javax.swing.JMenuItem();
        identifyLumens = new javax.swing.JMenuItem();
        performVolumetricAnalysis = new javax.swing.JMenuItem();
        fixPathsDialog = new javax.swing.JDialog(experimentsExplorerDialog, true);
        fixPathsCurrentPrefixLabel = new javax.swing.JLabel();
        fixPathsNewPrefixLabel = new javax.swing.JLabel();
        fixPathsCurrentPrefixTextField = new javax.swing.JTextField();
        fixPathsNewPrefixTextField = new javax.swing.JTextField();
        fixPathsSuffixDotsLabel1 = new javax.swing.JLabel();
        fixPathsSuffixDotsLabel2 = new javax.swing.JLabel();
        fixPathsDismissButton = new javax.swing.JButton();
        fixPathFixButton = new javax.swing.JButton();
        fixPathsExperimentsLabel = new javax.swing.JLabel();
        fixPathsExperimentsTextField = new javax.swing.JTextField();
        fixPathsShortenPrefixButton = new javax.swing.JButton();
        tagRemoveButtonGroup = new javax.swing.ButtonGroup();
        extractPotentialLumensDialog = new javax.swing.JDialog(experimentsExplorerDialog, true);
        backgroundPanel = new javax.swing.JPanel();
        tabbedPane = new javax.swing.JTabbedPane();
        viewPanel = new javax.swing.JPanel();
        resetAllButton = new javax.swing.JButton();
        countingPanelButton = new javax.swing.JButton();
        dataPanel = new javax.swing.JPanel();
        inputFileLabel = new javax.swing.JLabel();
        inputFilePathTextField = new javax.swing.JTextField();
        inputFileBrowseButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        viewTable = new javax.swing.JTable();
        jScrollPane2 = new javax.swing.JScrollPane();
        boxPanel = new javax.swing.JPanel();
        sortingPanel = new javax.swing.JPanel();
        sortButton = new javax.swing.JButton();
        sortingComboBox1 = new javax.swing.JComboBox<>();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        sortingComboBox2 = new javax.swing.JComboBox<>();
        jLabel3 = new javax.swing.JLabel();
        sortingComboBox3 = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        sortingComboBox4 = new javax.swing.JComboBox<>();
        testButton = new javax.swing.JButton();
        countingPanel = new javax.swing.JPanel();
        exclusionsPanel = new javax.swing.JPanel();
        rowsExclusionsLabel = new javax.swing.JLabel();
        columnExclusionsLabel = new javax.swing.JLabel();
        planesExclusionsLabel = new javax.swing.JLabel();
        fieldExclusionsLabel = new javax.swing.JLabel();
        rowsExclusionsTextField = new javax.swing.JTextField();
        columnsExclusionsTextField = new javax.swing.JTextField();
        planesExclusionsTextField = new javax.swing.JTextField();
        fieldsExclusionsTextField = new javax.swing.JTextField();
        countingOptionsPanel = new javax.swing.JPanel();
        countPerPlaneCheckBox = new javax.swing.JCheckBox();
        countPerFieldCheckBox = new javax.swing.JCheckBox();
        countingStepLabel = new javax.swing.JLabel();
        countingStepTextField = new javax.swing.JTextField();
        startCountingButton = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        countingTable = new javax.swing.JTable();
        exportToExcelButton = new javax.swing.JButton();
        lumenExplorerPanel = new javax.swing.JPanel();
        experimentsExplorerButton = new javax.swing.JButton();
        currentExperimentLabel = new javax.swing.JLabel();
        nextImageButton = new javax.swing.JButton();
        previousImageButton = new javax.swing.JButton();
        importFileToDbButton = new javax.swing.JButton();
        clearImageButton = new javax.swing.JButton();
        scrollImagePanel = new com.haithamseada.components.ScrollImagePanel();
        lumenCounterLabel = new javax.swing.JLabel();
        experimentsComboBox = new javax.swing.JComboBox<>();
        exportDbToFileButton = new javax.swing.JButton();
        loadChannelsButton = new javax.swing.JButton();
        exportAllImagesButton = new javax.swing.JButton();
        removeChannelsButton = new javax.swing.JButton();
        taggingModeCheckBox = new javax.swing.JCheckBox();

        fileChooser.setCurrentDirectory(new java.io.File("F:\\data\\19JUN2020_truncated"));

        loadImagesProgressDialog.setTitle("Loading Images");
        loadImagesProgressDialog.setMinimumSize(new java.awt.Dimension(400, 150));

        loadingImagesProgressLabel.setText("Loading images to be processed ...");

        loadingImagesProgressButton.setText("Done");
        loadingImagesProgressButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadingImagesProgressButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout loadImagesProgressDialogLayout = new javax.swing.GroupLayout(loadImagesProgressDialog.getContentPane());
        loadImagesProgressDialog.getContentPane().setLayout(loadImagesProgressDialogLayout);
        loadImagesProgressDialogLayout.setHorizontalGroup(
            loadImagesProgressDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(loadImagesProgressDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(loadImagesProgressDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(loadImagesProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(loadImagesProgressDialogLayout.createSequentialGroup()
                        .addComponent(loadingImagesProgressLabel)
                        .addGap(0, 369, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, loadImagesProgressDialogLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(loadingImagesProgressButton)))
                .addContainerGap())
        );
        loadImagesProgressDialogLayout.setVerticalGroup(
            loadImagesProgressDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, loadImagesProgressDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(loadingImagesProgressLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(loadImagesProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(loadingImagesProgressButton)
                .addContainerGap())
        );

        imagesDirChooser.setCurrentDirectory(new java.io.File("E:\\Dropbox\\Harmony\\3D Harmony counts\\Test Images"));
        imagesDirChooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);

        experimentsExplorerDialog.setTitle("Experiments Manager");
        experimentsExplorerDialog.setMinimumSize(new java.awt.Dimension(1000, 800));

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Add/Remove Experiments, Directories and Images"));

        experimentsLabel.setText("Experiments");

        directoriesLabel.setText("Directories");

        imagesLabel.setText("Images");

        experimentsList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                experimentsListMouseClicked(evt);
            }
        });
        experimentsList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                experimentsListValueChanged(evt);
            }
        });
        jScrollPane4.setViewportView(experimentsList);

        directoriesList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                directoriesListValueChanged(evt);
            }
        });
        jScrollPane5.setViewportView(directoriesList);

        imagesList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                imagesListValueChanged(evt);
            }
        });
        jScrollPane6.setViewportView(imagesList);

        addExperimentButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/add.png"))); // NOI18N
        addExperimentButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addExperimentButtonActionPerformed(evt);
            }
        });

        removeExperimentButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/remove.png"))); // NOI18N
        removeExperimentButton.setEnabled(false);
        removeExperimentButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeExperimentButtonActionPerformed(evt);
            }
        });

        addDirectoryButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/add.png"))); // NOI18N
        addDirectoryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addDirectoryButtonActionPerformed(evt);
            }
        });

        removeDirectoryButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/remove.png"))); // NOI18N
        removeDirectoryButton.setEnabled(false);
        removeDirectoryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeDirectoryButtonActionPerformed(evt);
            }
        });

        removeImageButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/remove.png"))); // NOI18N
        removeImageButton.setEnabled(false);
        removeImageButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeImageButtonActionPerformed(evt);
            }
        });

        filterImageButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/filter.png"))); // NOI18N
        filterImageButton.setEnabled(false);
        filterImageButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterImageButtonActionPerformed(evt);
            }
        });

        fixPathsButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/fix.png"))); // NOI18N
        fixPathsButton.setEnabled(false);
        fixPathsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fixPathsButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(addExperimentButton, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(removeExperimentButton, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(fixPathsButton, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(filterImageButton, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE)
                    .addComponent(experimentsLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(addDirectoryButton, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(removeDirectoryButton, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(directoriesLabel)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 326, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(imagesLabel)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(removeImageButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(experimentsLabel)
                            .addComponent(directoriesLabel)
                            .addComponent(imagesLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 420, Short.MAX_VALUE)
                            .addComponent(jScrollPane5)
                            .addComponent(jScrollPane6))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(removeDirectoryButton, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(addDirectoryButton, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(removeImageButton, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(removeExperimentButton, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(addExperimentButton, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(filterImageButton, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(fixPathsButton, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(15, 15, 15))
        );

        dismissButton.setText("Dismiss");
        dismissButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dismissButtonActionPerformed(evt);
            }
        });

        jumpToSelectionButton.setText("Jump To Selection");
        jumpToSelectionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jumpToSelectionButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout experimentsExplorerDialogLayout = new javax.swing.GroupLayout(experimentsExplorerDialog.getContentPane());
        experimentsExplorerDialog.getContentPane().setLayout(experimentsExplorerDialogLayout);
        experimentsExplorerDialogLayout.setHorizontalGroup(
            experimentsExplorerDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(experimentsExplorerDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(experimentsExplorerDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, experimentsExplorerDialogLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jumpToSelectionButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(dismissButton)))
                .addContainerGap())
        );

        experimentsExplorerDialogLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {dismissButton, jumpToSelectionButton});

        experimentsExplorerDialogLayout.setVerticalGroup(
            experimentsExplorerDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(experimentsExplorerDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(experimentsExplorerDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dismissButton)
                    .addComponent(jumpToSelectionButton))
                .addContainerGap())
        );

        //GuiUtility.loadExperiments(experimentsList, directoriesList);

        wellMapTable.setRowHeight(70);
        wellMapTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        jScrollPane7.setViewportView(wellMapTable);

        dismissWellsDialogButton.setText("Dismiss");
        dismissWellsDialogButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dismissWellsDialogButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout wellMapDialogLayout = new javax.swing.GroupLayout(wellMapDialog.getContentPane());
        wellMapDialog.getContentPane().setLayout(wellMapDialogLayout);
        wellMapDialogLayout.setHorizontalGroup(
            wellMapDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(wellMapDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(wellMapDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 551, Short.MAX_VALUE)
                    .addGroup(wellMapDialogLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(dismissWellsDialogButton)))
                .addContainerGap())
        );
        wellMapDialogLayout.setVerticalGroup(
            wellMapDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(wellMapDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 451, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(dismissWellsDialogButton)
                .addContainerGap())
        );

        wellPopupMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
                wellPopupMenuPopupMenuWillBecomeVisible(evt);
            }
        });

        wellEditInfoMenuItem.setText("Edit Information");
        wellEditInfoMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wellEditInfoMenuItemActionPerformed(evt);
            }
        });
        wellPopupMenu.add(wellEditInfoMenuItem);

        wellCopyInfoMenuItem.setText("Copy Information");
        wellCopyInfoMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wellCopyInfoMenuItemActionPerformed(evt);
            }
        });
        wellPopupMenu.add(wellCopyInfoMenuItem);

        wellPasteInfoMenuItem.setText("Paste Information");
        wellPasteInfoMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wellPasteInfoMenuItemActionPerformed(evt);
            }
        });
        wellPopupMenu.add(wellPasteInfoMenuItem);

        wellDeleteInfoMenuItem.setText("Delete Information");
        wellDeleteInfoMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wellDeleteInfoMenuItemActionPerformed(evt);
            }
        });
        wellPopupMenu.add(wellDeleteInfoMenuItem);

        wellEditDialog.setTitle("Well Information");
        wellEditDialog.setMinimumSize(new java.awt.Dimension(235, 260));
        wellEditDialog.setResizable(false);

        wellExperimentLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        wellExperimentLabel.setText("Experiment");

        wellRowColumnLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        wellRowColumnLabel.setText("Row x Column");

        wellCompoundLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        wellCompoundLabel.setText("Compound");

        wellConcentrationLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        wellConcentrationLabel.setText("Concentration");

        wellCellTypeLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        wellCellTypeLabel.setText("Cell Type");

        wellCellCountLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        wellCellCountLabel.setText("Cell Count");

        wellExperimentTextField.setEditable(false);
        wellExperimentTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        wellRowColumnTextField.setEditable(false);
        wellRowColumnTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        wellCompoundTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        wellConcentrationFormattedTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        wellCellTypeTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        wellCellCountFormattedTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        wellSaveButton.setText("Save");
        wellSaveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wellSaveButtonActionPerformed(evt);
            }
        });

        wellCancelButton.setText("Cancel");
        wellCancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wellCancelButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout wellEditDialogLayout = new javax.swing.GroupLayout(wellEditDialog.getContentPane());
        wellEditDialog.getContentPane().setLayout(wellEditDialogLayout);
        wellEditDialogLayout.setHorizontalGroup(
            wellEditDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(wellEditDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(wellEditDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(wellEditDialogLayout.createSequentialGroup()
                        .addComponent(wellExperimentLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(wellExperimentTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 137, Short.MAX_VALUE))
                    .addGroup(wellEditDialogLayout.createSequentialGroup()
                        .addComponent(wellRowColumnLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(wellRowColumnTextField))
                    .addGroup(wellEditDialogLayout.createSequentialGroup()
                        .addComponent(wellCompoundLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(wellCompoundTextField))
                    .addGroup(wellEditDialogLayout.createSequentialGroup()
                        .addComponent(wellConcentrationLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(wellConcentrationFormattedTextField))
                    .addGroup(wellEditDialogLayout.createSequentialGroup()
                        .addComponent(wellCellCountLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(wellCellCountFormattedTextField))
                    .addGroup(wellEditDialogLayout.createSequentialGroup()
                        .addComponent(wellCellTypeLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(wellCellTypeTextField))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, wellEditDialogLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(wellSaveButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(wellCancelButton)))
                .addContainerGap())
        );

        wellEditDialogLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {wellCellCountLabel, wellCellTypeLabel, wellCompoundLabel, wellConcentrationLabel, wellExperimentLabel, wellRowColumnLabel});

        wellEditDialogLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {wellCancelButton, wellSaveButton});

        wellEditDialogLayout.setVerticalGroup(
            wellEditDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(wellEditDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(wellEditDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(wellExperimentLabel)
                    .addComponent(wellExperimentTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(wellEditDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(wellRowColumnLabel)
                    .addComponent(wellRowColumnTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(wellEditDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(wellCompoundLabel)
                    .addComponent(wellCompoundTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(wellEditDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(wellConcentrationLabel)
                    .addComponent(wellConcentrationFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(wellEditDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(wellCellTypeLabel)
                    .addComponent(wellCellTypeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(wellEditDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(wellCellCountLabel)
                    .addComponent(wellCellCountFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(wellEditDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(wellCancelButton)
                    .addComponent(wellSaveButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        imageFilterDialog.setTitle("Filter Images");
        imageFilterDialog.setMinimumSize(new java.awt.Dimension(755, 590));

        filterExperimentLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        filterExperimentLabel.setText("Experiment ID");

        filterDirectoryLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        filterDirectoryLabel.setText("Directory");

        filterImageFilterMaskLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        filterImageFilterMaskLabel.setText("Filter Mask");

        filterExperimentTextField.setEditable(false);

        filterDirectoryComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterDirectoryComboBoxActionPerformed(evt);
            }
        });

        filterDismisslButton.setText("Dismiss");
        filterDismisslButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterDismisslButtonActionPerformed(evt);
            }
        });

        filterDeleteSelectedImagesButton.setText("Delete Selected Images");
        filterDeleteSelectedImagesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterDeleteSelectedImagesButtonActionPerformed(evt);
            }
        });

        filterKeepSelectedButton.setText("Keep Selected Images Only");
        filterKeepSelectedButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterKeepSelectedButtonActionPerformed(evt);
            }
        });

        jScrollPane8.setViewportView(filterImageList);

        filterApplyFilterButton.setText("Apply Filter");
        filterApplyFilterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterApplyFilterButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout imageFilterDialogLayout = new javax.swing.GroupLayout(imageFilterDialog.getContentPane());
        imageFilterDialog.getContentPane().setLayout(imageFilterDialogLayout);
        imageFilterDialogLayout.setHorizontalGroup(
            imageFilterDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(imageFilterDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(imageFilterDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane8)
                    .addGroup(imageFilterDialogLayout.createSequentialGroup()
                        .addComponent(filterExperimentLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(filterExperimentTextField))
                    .addGroup(imageFilterDialogLayout.createSequentialGroup()
                        .addComponent(filterImageFilterMaskLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(filterImageFilterMaskTextField)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(filterApplyFilterButton))
                    .addGroup(imageFilterDialogLayout.createSequentialGroup()
                        .addComponent(filterDirectoryLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(filterDirectoryComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, imageFilterDialogLayout.createSequentialGroup()
                        .addGap(0, 226, Short.MAX_VALUE)
                        .addComponent(filterDeleteSelectedImagesButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(filterKeepSelectedButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(filterDismisslButton)))
                .addContainerGap())
        );

        imageFilterDialogLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {filterDirectoryLabel, filterExperimentLabel, filterImageFilterMaskLabel});

        imageFilterDialogLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {filterDeleteSelectedImagesButton, filterDismisslButton, filterKeepSelectedButton});

        imageFilterDialogLayout.setVerticalGroup(
            imageFilterDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(imageFilterDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(imageFilterDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(filterExperimentLabel)
                    .addComponent(filterExperimentTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(imageFilterDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(filterDirectoryLabel)
                    .addComponent(filterDirectoryComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(imageFilterDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(filterImageFilterMaskLabel)
                    .addComponent(filterImageFilterMaskTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(filterApplyFilterButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane8, javax.swing.GroupLayout.PREFERRED_SIZE, 399, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(imageFilterDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(filterDismisslButton)
                    .addComponent(filterDeleteSelectedImagesButton)
                    .addComponent(filterKeepSelectedButton))
                .addContainerGap())
        );

        plateInfoDialog.setTitle("New Experiment");
        plateInfoDialog.setMinimumSize(new java.awt.Dimension(350, 230));

        newPlateInfoExpNameLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        newPlateInfoExpNameLabel.setText("Experiment Name");

        newPlateInfoRowCountLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        newPlateInfoRowCountLabel.setText("Number of Rows");

        newPlateInfoColCountLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        newPlateInfoColCountLabel.setText("Number of Columns");

        newPlateInfoPlateNameLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        newPlateInfoPlateNameLabel.setText("Plate Name");

        newPlateInfoExpNameTextField.setEditable(false);

        newPlateInfoPlateNameTextField.setEditable(false);

        newPlateInfoManualSetCheckBox.setText("Set Plate Name Manually");
        newPlateInfoManualSetCheckBox.setMargin(new java.awt.Insets(2, 0, 2, 2));
        newPlateInfoManualSetCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newPlateInfoManualSetCheckBoxActionPerformed(evt);
            }
        });

        newPlateInfoDismissButton.setText("Dismiss");
        newPlateInfoDismissButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newPlateInfoDismissButtonActionPerformed(evt);
            }
        });

        newPlateInfoSaveButton.setText("Save");
        newPlateInfoSaveButton.setEnabled(false);
        newPlateInfoSaveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newPlateInfoSaveButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout plateInfoDialogLayout = new javax.swing.GroupLayout(plateInfoDialog.getContentPane());
        plateInfoDialog.getContentPane().setLayout(plateInfoDialogLayout);
        plateInfoDialogLayout.setHorizontalGroup(
            plateInfoDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(plateInfoDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(plateInfoDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(plateInfoDialogLayout.createSequentialGroup()
                        .addGroup(plateInfoDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, plateInfoDialogLayout.createSequentialGroup()
                                .addComponent(newPlateInfoExpNameLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(newPlateInfoExpNameTextField))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, plateInfoDialogLayout.createSequentialGroup()
                                .addComponent(newPlateInfoColCountLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(newPlateInfoColCountFormattedTextField))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, plateInfoDialogLayout.createSequentialGroup()
                                .addComponent(newPlateInfoRowCountLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(newPlateInfoRowCountFormattedTextField)))
                        .addGap(1, 1, 1))
                    .addGroup(plateInfoDialogLayout.createSequentialGroup()
                        .addComponent(newPlateInfoPlateNameLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(plateInfoDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(newPlateInfoPlateNameTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 190, Short.MAX_VALUE)
                            .addComponent(newPlateInfoManualSetCheckBox)))
                    .addGroup(plateInfoDialogLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(newPlateInfoSaveButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(newPlateInfoDismissButton)))
                .addContainerGap())
        );

        plateInfoDialogLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {newPlateInfoColCountLabel, newPlateInfoExpNameLabel, newPlateInfoPlateNameLabel, newPlateInfoRowCountLabel});

        plateInfoDialogLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {newPlateInfoDismissButton, newPlateInfoSaveButton});

        plateInfoDialogLayout.setVerticalGroup(
            plateInfoDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(plateInfoDialogLayout.createSequentialGroup()
                .addContainerGap(15, Short.MAX_VALUE)
                .addGroup(plateInfoDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(newPlateInfoExpNameLabel)
                    .addComponent(newPlateInfoExpNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(plateInfoDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(newPlateInfoRowCountLabel)
                    .addComponent(newPlateInfoRowCountFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(plateInfoDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(newPlateInfoColCountLabel)
                    .addComponent(newPlateInfoColCountFormattedTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(plateInfoDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(newPlateInfoPlateNameLabel)
                    .addComponent(newPlateInfoPlateNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(newPlateInfoManualSetCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(plateInfoDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(newPlateInfoSaveButton)
                    .addComponent(newPlateInfoDismissButton))
                .addGap(10, 10, 10))
        );

        experimentPopupMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
                experimentPopupMenuPopupMenuWillBecomeVisible(evt);
            }
        });

        openPlateMapMenuItem.setText("Open Plate Map");
        openPlateMapMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openPlateMapMenuItemActionPerformed(evt);
            }
        });
        experimentPopupMenu.add(openPlateMapMenuItem);

        editPlateInfoMenuItem.setText("Edit Plate Info");
        editPlateInfoMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editPlateInfoMenuItemActionPerformed(evt);
            }
        });
        experimentPopupMenu.add(editPlateInfoMenuItem);

        trainLumenClassifier.setText("Train Lumen Classifier");
        trainLumenClassifier.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                trainLumenClassifierActionPerformed(evt);
            }
        });
        experimentPopupMenu.add(trainLumenClassifier);

        extractPotentialLumens.setText("Extract Potential Lumens");
        extractPotentialLumens.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                extractPotentialLumensActionPerformed(evt);
            }
        });
        experimentPopupMenu.add(extractPotentialLumens);

        identifyLumens.setText("Identify Lumens");
        identifyLumens.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                identifyLumensActionPerformed(evt);
            }
        });
        experimentPopupMenu.add(identifyLumens);

        performVolumetricAnalysis.setText("Perform Volumetric Analysis");
        performVolumetricAnalysis.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                performVolumetricAnalysisActionPerformed(evt);
            }
        });
        experimentPopupMenu.add(performVolumetricAnalysis);

        fixPathsDialog.setTitle("Fix Paths");
        fixPathsDialog.setMinimumSize(new java.awt.Dimension(512, 180));

        fixPathsCurrentPrefixLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        fixPathsCurrentPrefixLabel.setText("Current Path Prefix");

        fixPathsNewPrefixLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        fixPathsNewPrefixLabel.setText("New Path Prefix");

        fixPathsCurrentPrefixTextField.setEditable(false);

        fixPathsSuffixDotsLabel1.setText(".....");

        fixPathsSuffixDotsLabel2.setText(".....");

        fixPathsDismissButton.setText("Dismiss");
        fixPathsDismissButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fixPathsDismissButtonActionPerformed(evt);
            }
        });

        fixPathFixButton.setText("Fix");
        fixPathFixButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fixPathFixButtonActionPerformed(evt);
            }
        });

        fixPathsExperimentsLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        fixPathsExperimentsLabel.setText("Experiments");

        fixPathsExperimentsTextField.setEditable(false);

        fixPathsShortenPrefixButton.setText("<<");
        fixPathsShortenPrefixButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fixPathsShortenPrefixButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout fixPathsDialogLayout = new javax.swing.GroupLayout(fixPathsDialog.getContentPane());
        fixPathsDialog.getContentPane().setLayout(fixPathsDialogLayout);
        fixPathsDialogLayout.setHorizontalGroup(
            fixPathsDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fixPathsDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(fixPathsDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fixPathsDialogLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(fixPathFixButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(fixPathsDismissButton))
                    .addGroup(fixPathsDialogLayout.createSequentialGroup()
                        .addComponent(fixPathsExperimentsLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(fixPathsExperimentsTextField))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fixPathsDialogLayout.createSequentialGroup()
                        .addGroup(fixPathsDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(fixPathsDialogLayout.createSequentialGroup()
                                .addComponent(fixPathsNewPrefixLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(fixPathsNewPrefixTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 343, Short.MAX_VALUE))
                            .addGroup(fixPathsDialogLayout.createSequentialGroup()
                                .addComponent(fixPathsCurrentPrefixLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(fixPathsCurrentPrefixTextField)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(fixPathsDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(fixPathsDialogLayout.createSequentialGroup()
                                .addComponent(fixPathsSuffixDotsLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(fixPathsShortenPrefixButton))
                            .addComponent(fixPathsSuffixDotsLabel2))))
                .addContainerGap())
        );

        fixPathsDialogLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {fixPathsCurrentPrefixLabel, fixPathsExperimentsLabel, fixPathsNewPrefixLabel});

        fixPathsDialogLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {fixPathFixButton, fixPathsDismissButton});

        fixPathsDialogLayout.setVerticalGroup(
            fixPathsDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fixPathsDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(fixPathsDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fixPathsExperimentsLabel)
                    .addComponent(fixPathsExperimentsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(fixPathsDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fixPathsCurrentPrefixLabel)
                    .addComponent(fixPathsCurrentPrefixTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fixPathsSuffixDotsLabel1)
                    .addComponent(fixPathsShortenPrefixButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(fixPathsDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fixPathsNewPrefixLabel)
                    .addComponent(fixPathsNewPrefixTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fixPathsSuffixDotsLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(fixPathsDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fixPathsDismissButton)
                    .addComponent(fixPathFixButton))
                .addContainerGap())
        );

        extractPotentialLumensDialog.setLocationByPlatform(true);
        extractPotentialLumensDialog.setMinimumSize(new java.awt.Dimension(200, 300));
        extractPotentialLumensDialog.getContentPane().setLayout(new javax.swing.BoxLayout(extractPotentialLumensDialog.getContentPane(), javax.swing.BoxLayout.LINE_AXIS));

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Harmony Sidekick");

        resetAllButton.setText("Reset All");
        resetAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetAllButtonActionPerformed(evt);
            }
        });

        countingPanelButton.setText("Counting Panel >>");
        countingPanelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                countingPanelButtonActionPerformed(evt);
            }
        });

        dataPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Data"));

        inputFileLabel.setText("Input File");

        inputFileBrowseButton.setText("Browse");
        inputFileBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                inputFileBrowseButtonActionPerformed(evt);
            }
        });

        viewTable.setModel(new javax.swing.table.DefaultTableModel(){public boolean isCellEditable(int row, int column){return false;}});
        jScrollPane1.setViewportView(viewTable);

        boxPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        boxPanel.setLayout(new java.awt.GridLayout(1, 0));
        jScrollPane2.setViewportView(boxPanel);

        javax.swing.GroupLayout dataPanelLayout = new javax.swing.GroupLayout(dataPanel);
        dataPanel.setLayout(dataPanelLayout);
        dataPanelLayout.setHorizontalGroup(
            dataPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dataPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(dataPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(dataPanelLayout.createSequentialGroup()
                        .addComponent(inputFileLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(inputFilePathTextField))
                    .addComponent(jScrollPane1))
                .addGap(10, 10, 10)
                .addGroup(dataPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(inputFileBrowseButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        dataPanelLayout.setVerticalGroup(
            dataPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dataPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(dataPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(inputFilePathTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(inputFileLabel)
                    .addComponent(inputFileBrowseButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(dataPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 438, Short.MAX_VALUE)
                    .addComponent(jScrollPane2))
                .addContainerGap())
        );

        sortingPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Sorting"));

        sortButton.setText("Sort");
        sortButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sortButtonActionPerformed(evt);
            }
        });

        jLabel1.setText("Sort By");

        jLabel2.setText("Then");

        jLabel3.setText("Then");

        jLabel4.setText("Then");

        javax.swing.GroupLayout sortingPanelLayout = new javax.swing.GroupLayout(sortingPanel);
        sortingPanel.setLayout(sortingPanelLayout);
        sortingPanelLayout.setHorizontalGroup(
            sortingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, sortingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sortingComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sortingComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sortingComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sortingComboBox4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 582, Short.MAX_VALUE)
                .addComponent(sortButton, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        sortingPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {sortingComboBox1, sortingComboBox2, sortingComboBox3, sortingComboBox4});

        sortingPanelLayout.setVerticalGroup(
            sortingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sortingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(sortingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sortingComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(sortButton)
                    .addComponent(jLabel2)
                    .addComponent(sortingComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(sortingComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(sortingComboBox4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(18, Short.MAX_VALUE))
        );

        testButton.setText("TEST");
        testButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                testButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout viewPanelLayout = new javax.swing.GroupLayout(viewPanel);
        viewPanel.setLayout(viewPanelLayout);
        viewPanelLayout.setHorizontalGroup(
            viewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(viewPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(viewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sortingPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(viewPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(testButton)
                        .addGap(259, 259, 259)
                        .addComponent(resetAllButton, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(countingPanelButton))
                    .addComponent(dataPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        viewPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {countingPanelButton, resetAllButton});

        viewPanelLayout.setVerticalGroup(
            viewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(viewPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(dataPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(sortingPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(viewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(resetAllButton)
                    .addComponent(countingPanelButton)
                    .addComponent(testButton))
                .addContainerGap())
        );

        tabbedPane.addTab("View", viewPanel);

        exclusionsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Exclusions"));

        rowsExclusionsLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        rowsExclusionsLabel.setText("Rows");

        columnExclusionsLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        columnExclusionsLabel.setText("Columns");

        planesExclusionsLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        planesExclusionsLabel.setText("Planes");

        fieldExclusionsLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        fieldExclusionsLabel.setText("Fields");

        rowsExclusionsTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                rowsExclusionsTextFieldFocusLost(evt);
            }
        });

        columnsExclusionsTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                columnsExclusionsTextFieldFocusLost(evt);
            }
        });

        planesExclusionsTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                planesExclusionsTextFieldFocusLost(evt);
            }
        });

        fieldsExclusionsTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                fieldsExclusionsTextFieldFocusLost(evt);
            }
        });

        javax.swing.GroupLayout exclusionsPanelLayout = new javax.swing.GroupLayout(exclusionsPanel);
        exclusionsPanel.setLayout(exclusionsPanelLayout);
        exclusionsPanelLayout.setHorizontalGroup(
            exclusionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(exclusionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(exclusionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(exclusionsPanelLayout.createSequentialGroup()
                        .addComponent(rowsExclusionsLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(rowsExclusionsTextField))
                    .addGroup(exclusionsPanelLayout.createSequentialGroup()
                        .addComponent(columnExclusionsLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(columnsExclusionsTextField))
                    .addGroup(exclusionsPanelLayout.createSequentialGroup()
                        .addComponent(fieldExclusionsLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fieldsExclusionsTextField))
                    .addGroup(exclusionsPanelLayout.createSequentialGroup()
                        .addComponent(planesExclusionsLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(planesExclusionsTextField)))
                .addContainerGap())
        );

        exclusionsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {columnExclusionsLabel, fieldExclusionsLabel, planesExclusionsLabel, rowsExclusionsLabel});

        exclusionsPanelLayout.setVerticalGroup(
            exclusionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(exclusionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(exclusionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rowsExclusionsLabel)
                    .addComponent(rowsExclusionsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(exclusionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(columnExclusionsLabel)
                    .addComponent(columnsExclusionsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(exclusionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(planesExclusionsLabel)
                    .addComponent(planesExclusionsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(exclusionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fieldExclusionsLabel)
                    .addComponent(fieldsExclusionsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(30, Short.MAX_VALUE))
        );

        countingOptionsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Counting Options"));

        countPerPlaneCheckBox.setSelected(true);
        countPerPlaneCheckBox.setText("Count per plane");

        countPerFieldCheckBox.setSelected(true);
        countPerFieldCheckBox.setText("Count per field");

        countingStepLabel.setText("Counting Step");

        countingStepTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        countingStepTextField.setText("1");
        countingStepTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                countingStepTextFieldFocusLost(evt);
            }
        });

        javax.swing.GroupLayout countingOptionsPanelLayout = new javax.swing.GroupLayout(countingOptionsPanel);
        countingOptionsPanel.setLayout(countingOptionsPanelLayout);
        countingOptionsPanelLayout.setHorizontalGroup(
            countingOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(countingOptionsPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(countingOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(countPerPlaneCheckBox)
                    .addComponent(countPerFieldCheckBox)
                    .addGroup(countingOptionsPanelLayout.createSequentialGroup()
                        .addComponent(countingStepLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(countingStepTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        countingOptionsPanelLayout.setVerticalGroup(
            countingOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(countingOptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(countPerPlaneCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(countPerFieldCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(countingOptionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(countingStepLabel)
                    .addComponent(countingStepTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        startCountingButton.setText("Start Counting");
        startCountingButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startCountingButtonActionPerformed(evt);
            }
        });

        countingTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        jScrollPane3.setViewportView(countingTable);

        exportToExcelButton.setText("Export to Excel");
        exportToExcelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportToExcelButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout countingPanelLayout = new javax.swing.GroupLayout(countingPanel);
        countingPanel.setLayout(countingPanelLayout);
        countingPanelLayout.setHorizontalGroup(
            countingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(countingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(countingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 1254, Short.MAX_VALUE)
                    .addGroup(countingPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(exportToExcelButton))
                    .addGroup(countingPanelLayout.createSequentialGroup()
                        .addComponent(exclusionsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(countingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(countingOptionsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(startCountingButton, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        countingPanelLayout.setVerticalGroup(
            countingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(countingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(countingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(exclusionsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(countingPanelLayout.createSequentialGroup()
                        .addComponent(countingOptionsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(startCountingButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 415, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(exportToExcelButton)
                .addContainerGap())
        );

        tabbedPane.addTab("Counting", countingPanel);

        lumenExplorerPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        experimentsExplorerButton.setText("Experiments Explorer");
        experimentsExplorerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                experimentsExplorerButtonActionPerformed(evt);
            }
        });

        currentExperimentLabel.setText("Current Experiment");

        nextImageButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/right.png"))); // NOI18N
        nextImageButton.setEnabled(false);
        nextImageButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextImageButtonActionPerformed(evt);
            }
        });

        previousImageButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/left.png"))); // NOI18N
        previousImageButton.setEnabled(false);
        previousImageButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                previousImageButtonActionPerformed(evt);
            }
        });

        importFileToDbButton.setText("Import File to DB");
        importFileToDbButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importFileToDbButtonActionPerformed(evt);
            }
        });

        clearImageButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/sweep.png"))); // NOI18N
        clearImageButton.setEnabled(false);
        clearImageButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearImageButtonActionPerformed(evt);
            }
        });

        experimentsComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                experimentsComboBoxActionPerformed(evt);
            }
        });

        exportDbToFileButton.setText("Export All DB to File");
        exportDbToFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportDbToFileButtonActionPerformed(evt);
            }
        });

        loadChannelsButton.setText("Load Channels");
        loadChannelsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadChannelsButtonActionPerformed(evt);
            }
        });

        exportAllImagesButton.setText("Export All Images");
        exportAllImagesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportAllImagesButtonActionPerformed(evt);
            }
        });

        removeChannelsButton.setText("Remove Channels");
        removeChannelsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeChannelsButtonActionPerformed(evt);
            }
        });

        taggingModeCheckBox.setText("Activate Tagging Mode");

        javax.swing.GroupLayout lumenExplorerPanelLayout = new javax.swing.GroupLayout(lumenExplorerPanel);
        lumenExplorerPanel.setLayout(lumenExplorerPanelLayout);
        lumenExplorerPanelLayout.setHorizontalGroup(
            lumenExplorerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, lumenExplorerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(lumenExplorerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(scrollImagePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, lumenExplorerPanelLayout.createSequentialGroup()
                        .addGroup(lumenExplorerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(importFileToDbButton, javax.swing.GroupLayout.PREFERRED_SIZE, 163, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(exportDbToFileButton, javax.swing.GroupLayout.PREFERRED_SIZE, 163, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(lumenExplorerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(loadChannelsButton, javax.swing.GroupLayout.DEFAULT_SIZE, 117, Short.MAX_VALUE)
                            .addComponent(removeChannelsButton, javax.swing.GroupLayout.PREFERRED_SIZE, 163, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(exportAllImagesButton, javax.swing.GroupLayout.PREFERRED_SIZE, 163, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lumenCounterLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(273, 273, 273)
                        .addComponent(taggingModeCheckBox)
                        .addGap(18, 18, 18)
                        .addComponent(clearImageButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(previousImageButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(nextImageButton))
                    .addGroup(lumenExplorerPanelLayout.createSequentialGroup()
                        .addComponent(currentExperimentLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(experimentsComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(experimentsExplorerButton)))
                .addContainerGap())
        );

        lumenExplorerPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {clearImageButton, nextImageButton, previousImageButton});

        lumenExplorerPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {exportDbToFileButton, importFileToDbButton, loadChannelsButton});

        lumenExplorerPanelLayout.setVerticalGroup(
            lumenExplorerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lumenExplorerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(lumenExplorerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(experimentsExplorerButton)
                    .addComponent(currentExperimentLabel)
                    .addComponent(experimentsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(scrollImagePanel, javax.swing.GroupLayout.DEFAULT_SIZE, 508, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(lumenExplorerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(lumenExplorerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(nextImageButton, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE)
                        .addComponent(previousImageButton, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE)
                        .addComponent(clearImageButton, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE)
                        .addComponent(lumenCounterLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, lumenExplorerPanelLayout.createSequentialGroup()
                            .addGroup(lumenExplorerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(loadChannelsButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(importFileToDbButton, javax.swing.GroupLayout.DEFAULT_SIZE, 35, Short.MAX_VALUE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(lumenExplorerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(removeChannelsButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(exportDbToFileButton, javax.swing.GroupLayout.DEFAULT_SIZE, 34, Short.MAX_VALUE)
                                .addComponent(exportAllImagesButton, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addComponent(taggingModeCheckBox))
                .addContainerGap())
        );

        lumenExplorerPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {exportDbToFileButton, importFileToDbButton});

        tabbedPane.addTab("Lumen Explorer", lumenExplorerPanel);

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tabbedPane)
                .addContainerGap())
        );
        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tabbedPane)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void resetAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetAllButtonActionPerformed
        resetAll();
        currentInputFile = null;
        clearCriteriaComboBoxes();
    }//GEN-LAST:event_resetAllButtonActionPerformed

    private File currentInputFile = null;

    private void inputFileBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inputFileBrowseButtonActionPerformed
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                resetAll();
                currentInputFile = fileChooser.getSelectedFile();
                inputFilePathTextField.setText(currentInputFile.getPath());
                clearCriteriaComboBoxes();
                fillInData(IO.readRecords(fileChooser.getSelectedFile()));
                fillCriteriaComboboxes();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        ex.toString(),
                        "Cannot read input file",
                        JOptionPane.ERROR_MESSAGE);
                Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_inputFileBrowseButtonActionPerformed

    private void sortButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sortButtonActionPerformed
        if (fileChooser.getSelectedFile() == null) {
            JOptionPane.showMessageDialog(this, "Use the \"Browse\" button to load input data first.");
            return;
        }
        // Update sorting criteria
        ArrayList<JComboBox<String>> comboBoxes = new ArrayList<JComboBox<String>>();
        comboBoxes.add(sortingComboBox1);
        comboBoxes.add(sortingComboBox2);
        comboBoxes.add(sortingComboBox3);
        comboBoxes.add(sortingComboBox4);
        List<String> orderedCriteria = new ArrayList<>();
        for (JComboBox<String> comboBox : comboBoxes) {
            if (comboBox.getSelectedIndex() != -1) {
                orderedCriteria.add(comboBox.getSelectedItem().toString());
            }
        }
        Record.setSortingCriteriaOrders(orderedCriteria);
        try {
            // Read records from the input file once more
            List<Record> records = IO.readRecords(fileChooser.getSelectedFile());
            // Sort the records according to the criteria just set
            Collections.sort(records);
            // Reset everything and refill with the sorted data
            fillInData(records);
        } catch (IOException ex) {
            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_sortButtonActionPerformed

    private void countingPanelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_countingPanelButtonActionPerformed
        tabbedPane.setSelectedIndex(1);
    }//GEN-LAST:event_countingPanelButtonActionPerformed

    private void rowsExclusionsTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_rowsExclusionsTextFieldFocusLost
        validateExclusionText(rowsExclusionsTextField);
    }//GEN-LAST:event_rowsExclusionsTextFieldFocusLost

    private void columnsExclusionsTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_columnsExclusionsTextFieldFocusLost
        validateExclusionText(columnsExclusionsTextField);
    }//GEN-LAST:event_columnsExclusionsTextFieldFocusLost

    private void planesExclusionsTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_planesExclusionsTextFieldFocusLost
        validateExclusionText(planesExclusionsTextField);
    }//GEN-LAST:event_planesExclusionsTextFieldFocusLost

    private void fieldsExclusionsTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_fieldsExclusionsTextFieldFocusLost
        validateExclusionText(fieldsExclusionsTextField);
    }//GEN-LAST:event_fieldsExclusionsTextFieldFocusLost

    private void startCountingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startCountingButtonActionPerformed
        goCount();
    }//GEN-LAST:event_startCountingButtonActionPerformed

    private void countingStepTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_countingStepTextFieldFocusLost
        if (!INT_PATTERN.matcher(countingStepTextField.getText().trim()).matches()) {
            JOptionPane.showMessageDialog(this, "Counting step must be numeric (integer)", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            countingStepTextField.requestFocusInWindow();
        }
    }//GEN-LAST:event_countingStepTextFieldFocusLost

    private void exportToExcelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportToExcelButtonActionPerformed
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                exportToExcel(countingTable, fileChooser.getSelectedFile());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, ex.toString(), "Export Error", JOptionPane.ERROR_MESSAGE);
                Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_exportToExcelButtonActionPerformed

    private void importFileToDbButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importFileToDbButtonActionPerformed
        GuiUtility.importFileToDbButtonAction(dbDumpFileChooser, this);
    }//GEN-LAST:event_importFileToDbButtonActionPerformed

    private void nextImageButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextImageButtonActionPerformed
        GuiUtility.displayNextImageButtonAction(scrollImagePanel, lumenMap);
    }//GEN-LAST:event_nextImageButtonActionPerformed

    private void experimentsExplorerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_experimentsExplorerButtonActionPerformed
        GuiUtility.experimentsExplorerButtonAction(
                experimentsList,
                directoriesList,
                imagesList,
                experimentsExplorerDialog,
                scrollImagePanel);
    }//GEN-LAST:event_experimentsExplorerButtonActionPerformed

    private void loadingImagesProgressButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadingImagesProgressButtonActionPerformed
        loadImagesProgressDialog.setVisible(false);
    }//GEN-LAST:event_loadingImagesProgressButtonActionPerformed

    private void previousImageButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_previousImageButtonActionPerformed
        GuiUtility.displayPreviousImageButtonAction(scrollImagePanel, lumenMap);
    }//GEN-LAST:event_previousImageButtonActionPerformed

    private void clearImageButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearImageButtonActionPerformed
        if (taggingModeCheckBox.isSelected()) {
            GuiUtility.clearTags(
                    scrollImagePanel,
                    experimentsList,
                    lumenCounterLabel);
        } else {
            GuiUtility.clearIdentifiedLumens(lumenMap, scrollImagePanel);
        }
    }//GEN-LAST:event_clearImageButtonActionPerformed

    private void directoriesListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_directoriesListValueChanged
        GuiUtility.directoriesListValueChangedAction(
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
    }//GEN-LAST:event_directoriesListValueChanged

    private void experimentsListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_experimentsListValueChanged
        GuiUtility.experimentsListValueChangedAction(
                experimentsComboBox,
                experimentsList,
                directoriesList,
                imagesList,
                removeExperimentButton,
                addDirectoryButton,
                removeDirectoryButton,
                //addImageButton,
                removeImageButton,
                filterImageButton,
                fixPathsButton,
                scrollImagePanel,
                lumenMap);
    }//GEN-LAST:event_experimentsListValueChanged

    private void removeExperimentButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeExperimentButtonActionPerformed
        GuiUtility.removeExperimentAction(
                experimentsList,
                directoriesList,
                imagesList,
                removeExperimentButton,
                addDirectoryButton,
                removeDirectoryButton,
                removeImageButton,
                filterImageButton,
                fixPathsButton,
                experimentsExplorerDialog);
    }//GEN-LAST:event_removeExperimentButtonActionPerformed

    private void removeDirectoryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeDirectoryButtonActionPerformed
        GuiUtility.removeDirectoryButtonAction(
                experimentsList,
                directoriesList,
                imagesList,
                removeExperimentButton,
                addDirectoryButton,
                removeDirectoryButton,
                removeImageButton,
                filterImageButton,
                fixPathsButton,
                experimentsExplorerDialog,
                scrollImagePanel);
    }//GEN-LAST:event_removeDirectoryButtonActionPerformed

    private void addDirectoryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addDirectoryButtonActionPerformed
        GuiUtility.addDirectoryButtonAction(
                imagesDirChooser,
                experimentsList,
                directoriesList,
                imagesList,
                removeExperimentButton,
                addDirectoryButton,
                removeDirectoryButton,
                removeImageButton,
                filterImageButton,
                fixPathsButton,
                experimentsExplorerDialog,
                loadImagesProgressDialog,
                loadingImagesProgressLabel,
                loadingImagesProgressButton,
                loadImagesProgressBar,
                nextImageButton,
                previousImageButton,
                clearImageButton,
                scrollImagePanel
        );
    }//GEN-LAST:event_addDirectoryButtonActionPerformed

    private void imagesListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_imagesListValueChanged
        GuiUtility.imagesListValueChangedAction(
                experimentsList,
                directoriesList,
                imagesList,
                removeExperimentButton,
                addDirectoryButton,
                removeDirectoryButton,
                removeImageButton,
                filterImageButton,
                fixPathsButton,
                jumpToSelectionButton);
    }//GEN-LAST:event_imagesListValueChanged

    private void dismissButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dismissButtonActionPerformed
        experimentsExplorerDialog.setVisible(false);
    }//GEN-LAST:event_dismissButtonActionPerformed

    private void jumpToSelectionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jumpToSelectionButtonActionPerformed
        GuiUtility.jumpToSelectionAction(experimentsList,
                imagesList,
                previousImageButton,
                nextImageButton,
                clearImageButton,
                experimentsExplorerDialog,
                lumenCounterLabel,
                scrollImagePanel);
    }//GEN-LAST:event_jumpToSelectionButtonActionPerformed

    private void experimentsListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_experimentsListMouseClicked
        if (evt.getClickCount() == 2) {
            GuiUtility.openWellMapDialog(experimentsList, wellMapTable,
                    wellMapDialog,
                    experimentsExplorerDialog);
        }
    }//GEN-LAST:event_experimentsListMouseClicked

    private void dismissWellsDialogButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dismissWellsDialogButtonActionPerformed
        wellMapDialog.setVisible(false);
    }//GEN-LAST:event_dismissWellsDialogButtonActionPerformed

    private void wellEditInfoMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_wellEditInfoMenuItemActionPerformed
        GuiUtility.wellEditMenuItemAction(experimentsList,
                wellMapTable,
                wellEditDialog,
                wellExperimentTextField,
                wellRowColumnTextField,
                wellCompoundTextField,
                wellConcentrationFormattedTextField,
                wellCellTypeTextField,
                wellCellCountFormattedTextField);
    }//GEN-LAST:event_wellEditInfoMenuItemActionPerformed

    private void wellPopupMenuPopupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_wellPopupMenuPopupMenuWillBecomeVisible
        GuiUtility.updateWellPlateSelectionAndPopupMenuItems(
                wellMapTable, wellPopupMenu);
    }//GEN-LAST:event_wellPopupMenuPopupMenuWillBecomeVisible

    private void wellCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_wellCancelButtonActionPerformed
        wellEditDialog.setVisible(false);
    }//GEN-LAST:event_wellCancelButtonActionPerformed

    private void wellSaveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_wellSaveButtonActionPerformed
        GuiUtility.wellSaveButtonAction(
                experimentsList,
                wellMapTable,
                wellExperimentTextField,
                wellCompoundTextField,
                wellConcentrationFormattedTextField,
                wellCellTypeTextField,
                wellCellCountFormattedTextField,
                wellEditDialog);
    }//GEN-LAST:event_wellSaveButtonActionPerformed

    private void wellCopyInfoMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_wellCopyInfoMenuItemActionPerformed
        GuiUtility.wellCopyMenuItemAction(wellMapTable);
    }//GEN-LAST:event_wellCopyInfoMenuItemActionPerformed

    private void wellPasteInfoMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_wellPasteInfoMenuItemActionPerformed
        GuiUtility.wellPasteMenuItemAction(wellMapTable, experimentsList);
    }//GEN-LAST:event_wellPasteInfoMenuItemActionPerformed

    private void wellDeleteInfoMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_wellDeleteInfoMenuItemActionPerformed
        GuiUtility.wellDeleteMenuItemAction(wellMapTable, experimentsList);
    }//GEN-LAST:event_wellDeleteInfoMenuItemActionPerformed

    private void exportDbToFileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportDbToFileButtonActionPerformed
        GuiUtility.exportDbToFileButtonAction(dbDumpFileChooser, this);
    }//GEN-LAST:event_exportDbToFileButtonActionPerformed

    private void filterImageButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterImageButtonActionPerformed
        GuiUtility.filterImageButtonAction(experimentsList,
                filterExperimentTextField,
                filterDirectoryComboBox,
                filterImageFilterMaskTextField,
                filterImageList,
                imageFilterDialog);
    }//GEN-LAST:event_filterImageButtonActionPerformed

    private void filterApplyFilterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterApplyFilterButtonActionPerformed
        GuiUtility.filterApplyFilterAction(experimentsList,
                filterImageFilterMaskTextField,
                filterImageList);
    }//GEN-LAST:event_filterApplyFilterButtonActionPerformed

    private void filterDirectoryComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterDirectoryComboBoxActionPerformed
        GuiUtility.updateFilterImageList(
                experimentsList,
                filterDirectoryComboBox,
                filterImageList);
    }//GEN-LAST:event_filterDirectoryComboBoxActionPerformed

    private void filterDeleteSelectedImagesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterDeleteSelectedImagesButtonActionPerformed
        GuiUtility.filterDeleteSelectedImagesButtonAction(
                experimentsList,
                filterDirectoryComboBox,
                filterImageList,
                imageFilterDialog,
                scrollImagePanel);
    }//GEN-LAST:event_filterDeleteSelectedImagesButtonActionPerformed

    private void filterKeepSelectedButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterKeepSelectedButtonActionPerformed
        GuiUtility.filterKeepSelectedImagesButtonAction(
                experimentsList,
                filterDirectoryComboBox,
                filterImageList,
                imageFilterDialog,
                scrollImagePanel);
    }//GEN-LAST:event_filterKeepSelectedButtonActionPerformed

    private void filterDismisslButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterDismisslButtonActionPerformed
        imageFilterDialog.setVisible(false);
    }//GEN-LAST:event_filterDismisslButtonActionPerformed

    private void addExperimentButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addExperimentButtonActionPerformed
        GuiUtility.addExperimentButtonAction(
                experimentsList,
                newPlateInfoExpNameTextField,
                newPlateInfoRowCountFormattedTextField,
                newPlateInfoColCountFormattedTextField,
                newPlateInfoPlateNameTextField,
                newPlateInfoManualSetCheckBox,
                plateInfoDialog,
                experimentsExplorerDialog);
    }//GEN-LAST:event_addExperimentButtonActionPerformed

    private void newPlateInfoDismissButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newPlateInfoDismissButtonActionPerformed
        plateInfoDialog.setVisible(false);
    }//GEN-LAST:event_newPlateInfoDismissButtonActionPerformed

    private void newPlateInfoManualSetCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newPlateInfoManualSetCheckBoxActionPerformed
        GuiUtility.newPlateInfoManualSetCheckBoxAction(
                newPlateInfoExpNameTextField,
                newPlateInfoRowCountFormattedTextField,
                newPlateInfoColCountFormattedTextField,
                newPlateInfoPlateNameTextField,
                newPlateInfoManualSetCheckBox);
    }//GEN-LAST:event_newPlateInfoManualSetCheckBoxActionPerformed

    private void newPlateInfoSaveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newPlateInfoSaveButtonActionPerformed
        GuiUtility.newPlateInfoSaveButtonAction(
                experimentsList,
                newPlateInfoRowCountFormattedTextField,
                newPlateInfoColCountFormattedTextField,
                newPlateInfoPlateNameTextField,
                plateInfoDialog
        );
    }//GEN-LAST:event_newPlateInfoSaveButtonActionPerformed

    private void experimentPopupMenuPopupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_experimentPopupMenuPopupMenuWillBecomeVisible
        GuiUtility.updateExperimentListSelectionAndPopupMenuItems(
                experimentsList,
                experimentPopupMenu);
    }//GEN-LAST:event_experimentPopupMenuPopupMenuWillBecomeVisible

    private void openPlateMapMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openPlateMapMenuItemActionPerformed
        GuiUtility.openWellMapDialog(experimentsList, wellMapTable,
                wellMapDialog,
                experimentsExplorerDialog);
    }//GEN-LAST:event_openPlateMapMenuItemActionPerformed

    private void editPlateInfoMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editPlateInfoMenuItemActionPerformed
        GuiUtility.openEditPlateInfoDialog(
                experimentsList,
                newPlateInfoExpNameTextField,
                newPlateInfoRowCountFormattedTextField,
                newPlateInfoColCountFormattedTextField,
                newPlateInfoPlateNameTextField,
                newPlateInfoManualSetCheckBox,
                plateInfoDialog);
    }//GEN-LAST:event_editPlateInfoMenuItemActionPerformed

    private void fixPathsDismissButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fixPathsDismissButtonActionPerformed
        fixPathsDialog.setVisible(false);
    }//GEN-LAST:event_fixPathsDismissButtonActionPerformed

    private void fixPathsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fixPathsButtonActionPerformed
        GuiUtility.fixPathsButtonAction(
                experimentsList,
                fixPathsExperimentsTextField,
                fixPathsCurrentPrefixTextField,
                fixPathsDialog);
    }//GEN-LAST:event_fixPathsButtonActionPerformed

    private void fixPathsShortenPrefixButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fixPathsShortenPrefixButtonActionPerformed
        GuiUtility.fixPathsShortenPrefixButtonAction(
                fixPathsCurrentPrefixTextField);
    }//GEN-LAST:event_fixPathsShortenPrefixButtonActionPerformed

    private void fixPathFixButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fixPathFixButtonActionPerformed
        GuiUtility.fixPathFixButtonAction(
                experimentsList,
                directoriesList,
                imagesList,
                fixPathsCurrentPrefixTextField,
                fixPathsNewPrefixTextField,
                fixPathsDialog);
    }//GEN-LAST:event_fixPathFixButtonActionPerformed

    private void removeImageButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeImageButtonActionPerformed
        GuiUtility.removeImageButtonAction(
                experimentsList,
                directoriesList,
                imagesList,
                removeExperimentButton,
                addDirectoryButton,
                removeDirectoryButton,
                removeImageButton,
                filterImageButton,
                fixPathsButton,
                scrollImagePanel,
                experimentsExplorerDialog);
    }//GEN-LAST:event_removeImageButtonActionPerformed

    private void experimentsComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_experimentsComboBoxActionPerformed
        GuiUtility.experimentsComboBoxAction(
                experimentsComboBox,
                experimentsList);
    }//GEN-LAST:event_experimentsComboBoxActionPerformed

    private void loadChannelsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadChannelsButtonActionPerformed
        GuiUtility.loadChannels(experimentsList, experimentsExplorerDialog);
    }//GEN-LAST:event_loadChannelsButtonActionPerformed

    private void trainLumenClassifierActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_trainLumenClassifierActionPerformed
        List<Integer> selectedIds = GuiUtility.getSelectedIds(experimentsList);
        if (selectedIds.size() != 1) {
            JOptionPane.showMessageDialog(
                    experimentsExplorerDialog,
                    "Please, select one experiment.",
                    "Select One",
                    JOptionPane.ERROR_MESSAGE);
        } else {

            try {
                Experiment experiment
                        = DbUtilities.getExperimentById(selectedIds.get(0));
                new MachineLearning().extractLearningData(
                        experiment,
                        "train",
                        edu.brown.predictivebiology.utilities.Utilities.readProperty("working-dir"),
                        0.5,
                        0.8,
                        10,
                        5,
                        40,
                        0.2,
                        500,
                        0.5,
                        25,
                        false);
            } catch (ClassNotFoundException
                    | SQLException
                    | NoSuchMethodException
                    | InstantiationException
                    | IllegalAccessException
                    | InvocationTargetException
                    | MWException ex) {
                Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println(ex.toString());
            }
        }
    }//GEN-LAST:event_trainLumenClassifierActionPerformed

    private void extractPotentialLumensActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_extractPotentialLumensActionPerformed
        File preClassifyDir = new File(
                edu.brown.predictivebiology.utilities.Utilities.readProperty(
                        "preclassification-dir"));
        File finalOutDir = new File(
                edu.brown.predictivebiology.utilities.Utilities.readProperty(
                        "final-output-dir"));
        try {
            if (preClassifyDir.exists()) {
                // Delete any files in the preclassification-dir
                FileUtils.cleanDirectory(preClassifyDir);
            }
            if (finalOutDir.exists()) {
                // Delete any files in the final-output-dir
                FileUtils.cleanDirectory(finalOutDir);
            }
        } catch (IOException ex) {
            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
        }

        new ExtractPotentialLumensProgressDialog(this, experimentsList, rootPaneCheckingEnabled).setVisible(true);

//        List<Integer> selectedIds = GuiUtility.getSelectedIds(experimentsList);
//        if (selectedIds.size() != 1) {
//            JOptionPane.showMessageDialog(
//                    experimentsExplorerDialog,
//                    "Please, select one experiment.",
//                    "Select One",
//                    JOptionPane.ERROR_MESSAGE);
//        } else {
////            extractPotentialLumensDialog.setVisible(true);
////            extractPotentialLumensProgressPanel.start();
//            try {
//                Experiment experiment
//                        = DbUtilities.getExperimentById(selectedIds.get(0));
//                new MachineLearning().extractLearningData(
//                        experiment,
//                        "preclassify",
//                        "E:\\Dropbox\\Harmony\\3D Harmony counts\\Test Images\\labeled images",
//                        0.5,
//                        0.8,
//                        10,
//                        5,
//                        40,
//                        0.2,
//                        500,
//                        0.5,
//                        25,
//                        false);
//                JOptionPane.showMessageDialog(
//                        this,
//                        "Preclassified sub-images successfully generated.",
//                        "Pre-classification performed successfully.",
//                        JOptionPane.INFORMATION_MESSAGE);
//            } catch (ClassNotFoundException
//                    | SQLException
//                    | NoSuchMethodException
//                    | InstantiationException
//                    | IllegalAccessException
//                    | InvocationTargetException
//                    | MWException ex) {
//                Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
//                System.out.println(ex.toString());
//            }
//        }
    }//GEN-LAST:event_extractPotentialLumensActionPerformed

    private void identifyLumensActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_identifyLumensActionPerformed
        try {
            lumenMap = new MachineLearning().classify(
                    edu.brown.predictivebiology.utilities.Utilities.readProperty(
                            "preclassification-dir"));
//            // Filter those lumens that do not have lumens either above or below
//            // them.
//            lumenMap = GuiUtility.filterOutSingularLumens(lumenMap);
            JOptionPane.showMessageDialog(
                    experimentsExplorerDialog,
                    String.format("%d lumens identified.",
                            lumenMap.getLumenCount()),
                    "Classification Completed Successfully",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (MWException ex) {
            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println(ex.toString());
        } 
//        catch (IOException ex) {
//            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
//            System.out.println(ex.toString());
//        }
    }//GEN-LAST:event_identifyLumensActionPerformed

    private void performVolumetricAnalysisActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_performVolumetricAnalysisActionPerformed
        try {
            List<File> lumenBoundaryFilesList
                    = new MachineLearning().groupRelatedLumenSlices(
                            lumenMap,
                            5,
                            0.65,
                            edu.brown.predictivebiology.utilities.Utilities.readProperty("final-output-dir"));
            double totalVolume = new MachineLearning().calculateVolumesAndAreas(
                    lumenBoundaryFilesList,
                    false,
                    false);
            JOptionPane.showMessageDialog(
                    experimentsExplorerDialog,
                    String.format("Total Volume = %f \u00B5m^3\n(individual volumes and surface areas are stored at: %s)",
                            totalVolume,
                            lumenBoundaryFilesList.get(0).getParentFile().getPath()),
                    "Total Volume",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (MWException | IOException | URISyntaxException ex) {
            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_performVolumetricAnalysisActionPerformed

    private void exportAllImagesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportAllImagesButtonActionPerformed
        GuiUtility.exportAllImages(scrollImagePanel, lumenMap);
    }//GEN-LAST:event_exportAllImagesButtonActionPerformed

    private void removeChannelsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeChannelsButtonActionPerformed
        List<Integer> expIds = GuiUtility.getSelectedIds(experimentsList);
        if (expIds.size() == 1) {
            try {
                List<Image> images
                        = DbUtilities.getImagesOfExperiment(expIds.get(0));
                for (Image image : images) {
                    if (!image.getPath().contains("combined")) {
                        DbUtilities.deleteImage(image);
                    }
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
                    experimentsExplorerDialog,
                    "Only one epxeriment must be selected",
                    "Select One Experiment",
                    JOptionPane.ERROR);
        }
    }//GEN-LAST:event_removeChannelsButtonActionPerformed

    private void testButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_testButtonActionPerformed

    }//GEN-LAST:event_testButtonActionPerformed

    /**
     * Validates exclusion strings
     */
    private void validateExclusionText(JTextField textField) {
        if (!EXCLUSIONS_PATTERN.matcher(textField.getText().trim()).matches()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Make sure you insert either an index (integer) e.g. 3, "
                    + "a range of indices e.g. 3-5 or a comma separated list "
                    + "of both.",
                    "Invalid Input",
                    JOptionPane.ERROR_MESSAGE);
            textField.requestFocusInWindow();
            textField.selectAll();
        }
    }

    /**
     * Returns an updated copy of the table model based on what check-boxes are
     * currently selected
     */
    private void updatedTable() {
        Vector<String> modifiedColumnLabels = new Vector<>();
        // Select only headers corresponding to the selected check-boxes
        for (int i = 0; i < checkBoxs.size(); i++) {
            if (checkBoxs.get(i).getCheckBox().isSelected()) {
                modifiedColumnLabels.add(originalColumnLabels.get(i));
            }
        }
        // Select only data corresponding to the selected check-boxes
        Vector<Vector<String>> modifiedData = new Vector<>();
        for (Vector<String> originalRecord : originalData) {
            Vector<String> recordCopy = new Vector<>();
            for (int i = 0; i < checkBoxs.size(); i++) {
                if (checkBoxs.get(i).getCheckBox().isSelected()) {
                    recordCopy.add(originalRecord.get(i));
                }
            }
            modifiedData.add(recordCopy);
        }
        // Update table
        viewTable.setModel(
                new DefaultTableModel(modifiedData, modifiedColumnLabels));
    }

    /**
     * Fill a sorting criteria dropbox with the labels of the corresponding
     * columns
     *
     * @param sortingComboBox
     */
    private void fillInColumnLabels(JComboBox<String> sortingComboBox) {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<String>();
        for (String label : originalColumnLabels) {
            model.addElement(label);
        }
        sortingComboBox.setModel(model);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(StartFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(StartFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(StartFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(StartFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new StartFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addDirectoryButton;
    private javax.swing.JButton addExperimentButton;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JPanel boxPanel;
    private javax.swing.JButton clearImageButton;
    private javax.swing.JLabel columnExclusionsLabel;
    private javax.swing.JTextField columnsExclusionsTextField;
    private javax.swing.JCheckBox countPerFieldCheckBox;
    private javax.swing.JCheckBox countPerPlaneCheckBox;
    private javax.swing.JPanel countingOptionsPanel;
    private javax.swing.JPanel countingPanel;
    private javax.swing.JButton countingPanelButton;
    private javax.swing.JLabel countingStepLabel;
    private javax.swing.JTextField countingStepTextField;
    private javax.swing.JTable countingTable;
    private javax.swing.JLabel currentExperimentLabel;
    private javax.swing.JPanel dataPanel;
    private javax.swing.JFileChooser dbDumpFileChooser;
    private javax.swing.JLabel directoriesLabel;
    private javax.swing.JList<String> directoriesList;
    private javax.swing.JButton dismissButton;
    private javax.swing.JButton dismissWellsDialogButton;
    private javax.swing.JMenuItem editPlateInfoMenuItem;
    private javax.swing.JPanel exclusionsPanel;
    private javax.swing.JPopupMenu experimentPopupMenu;
    private javax.swing.JComboBox<String> experimentsComboBox;
    private javax.swing.JButton experimentsExplorerButton;
    private javax.swing.JDialog experimentsExplorerDialog;
    private javax.swing.JLabel experimentsLabel;
    private javax.swing.JList<String> experimentsList;
    private javax.swing.JButton exportAllImagesButton;
    private javax.swing.JButton exportDbToFileButton;
    private javax.swing.JButton exportToExcelButton;
    private javax.swing.JMenuItem extractPotentialLumens;
    private javax.swing.JDialog extractPotentialLumensDialog;
    private javax.swing.JLabel fieldExclusionsLabel;
    private javax.swing.JTextField fieldsExclusionsTextField;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JButton filterApplyFilterButton;
    private javax.swing.JButton filterDeleteSelectedImagesButton;
    private javax.swing.JComboBox<String> filterDirectoryComboBox;
    private javax.swing.JLabel filterDirectoryLabel;
    private javax.swing.JButton filterDismisslButton;
    private javax.swing.JLabel filterExperimentLabel;
    private javax.swing.JTextField filterExperimentTextField;
    private javax.swing.JButton filterImageButton;
    private javax.swing.JLabel filterImageFilterMaskLabel;
    private javax.swing.JTextField filterImageFilterMaskTextField;
    private javax.swing.JList<String> filterImageList;
    private javax.swing.JButton filterKeepSelectedButton;
    private javax.swing.JButton fixPathFixButton;
    private javax.swing.JButton fixPathsButton;
    private javax.swing.JLabel fixPathsCurrentPrefixLabel;
    private javax.swing.JTextField fixPathsCurrentPrefixTextField;
    private javax.swing.JDialog fixPathsDialog;
    private javax.swing.JButton fixPathsDismissButton;
    private javax.swing.JLabel fixPathsExperimentsLabel;
    private javax.swing.JTextField fixPathsExperimentsTextField;
    private javax.swing.JLabel fixPathsNewPrefixLabel;
    private javax.swing.JTextField fixPathsNewPrefixTextField;
    private javax.swing.JButton fixPathsShortenPrefixButton;
    private javax.swing.JLabel fixPathsSuffixDotsLabel1;
    private javax.swing.JLabel fixPathsSuffixDotsLabel2;
    private javax.swing.JMenuItem identifyLumens;
    private javax.swing.JDialog imageFilterDialog;
    private javax.swing.JFileChooser imagesDirChooser;
    private javax.swing.JLabel imagesLabel;
    private javax.swing.JList<String> imagesList;
    private javax.swing.JButton importFileToDbButton;
    private javax.swing.JButton inputFileBrowseButton;
    private javax.swing.JLabel inputFileLabel;
    private javax.swing.JTextField inputFilePathTextField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JButton jumpToSelectionButton;
    private javax.swing.JButton loadChannelsButton;
    private javax.swing.JProgressBar loadImagesProgressBar;
    private javax.swing.JDialog loadImagesProgressDialog;
    private javax.swing.JButton loadingImagesProgressButton;
    private javax.swing.JLabel loadingImagesProgressLabel;
    private javax.swing.JLabel lumenCounterLabel;
    private javax.swing.JPanel lumenExplorerPanel;
    private javax.swing.JFormattedTextField newPlateInfoColCountFormattedTextField;
    private javax.swing.JLabel newPlateInfoColCountLabel;
    private javax.swing.JButton newPlateInfoDismissButton;
    private javax.swing.JLabel newPlateInfoExpNameLabel;
    private javax.swing.JTextField newPlateInfoExpNameTextField;
    private javax.swing.JCheckBox newPlateInfoManualSetCheckBox;
    private javax.swing.JLabel newPlateInfoPlateNameLabel;
    private javax.swing.JTextField newPlateInfoPlateNameTextField;
    private javax.swing.JFormattedTextField newPlateInfoRowCountFormattedTextField;
    private javax.swing.JLabel newPlateInfoRowCountLabel;
    private javax.swing.JButton newPlateInfoSaveButton;
    private javax.swing.JButton nextImageButton;
    private javax.swing.JMenuItem openPlateMapMenuItem;
    private javax.swing.JMenuItem performVolumetricAnalysis;
    private javax.swing.JLabel planesExclusionsLabel;
    private javax.swing.JTextField planesExclusionsTextField;
    private javax.swing.JDialog plateInfoDialog;
    private javax.swing.JButton previousImageButton;
    private javax.swing.JButton removeChannelsButton;
    private javax.swing.JButton removeDirectoryButton;
    private javax.swing.JButton removeExperimentButton;
    private javax.swing.JButton removeImageButton;
    private javax.swing.JButton resetAllButton;
    private javax.swing.JLabel rowsExclusionsLabel;
    private javax.swing.JTextField rowsExclusionsTextField;
    private com.haithamseada.components.ScrollImagePanel scrollImagePanel;
    private javax.swing.JButton sortButton;
    private javax.swing.JComboBox<String> sortingComboBox1;
    private javax.swing.JComboBox<String> sortingComboBox2;
    private javax.swing.JComboBox<String> sortingComboBox3;
    private javax.swing.JComboBox<String> sortingComboBox4;
    private javax.swing.JPanel sortingPanel;
    private javax.swing.JButton startCountingButton;
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.ButtonGroup tagRemoveButtonGroup;
    private javax.swing.JCheckBox taggingModeCheckBox;
    private javax.swing.JButton testButton;
    private javax.swing.JMenuItem trainLumenClassifier;
    private javax.swing.JPanel viewPanel;
    private javax.swing.JTable viewTable;
    private javax.swing.JButton wellCancelButton;
    private javax.swing.JFormattedTextField wellCellCountFormattedTextField;
    private javax.swing.JLabel wellCellCountLabel;
    private javax.swing.JLabel wellCellTypeLabel;
    private javax.swing.JTextField wellCellTypeTextField;
    private javax.swing.JLabel wellCompoundLabel;
    private javax.swing.JTextField wellCompoundTextField;
    private javax.swing.JFormattedTextField wellConcentrationFormattedTextField;
    private javax.swing.JLabel wellConcentrationLabel;
    private javax.swing.JMenuItem wellCopyInfoMenuItem;
    private javax.swing.JMenuItem wellDeleteInfoMenuItem;
    private javax.swing.JDialog wellEditDialog;
    private javax.swing.JMenuItem wellEditInfoMenuItem;
    private javax.swing.JLabel wellExperimentLabel;
    private javax.swing.JTextField wellExperimentTextField;
    private javax.swing.JDialog wellMapDialog;
    private javax.swing.JTable wellMapTable;
    private javax.swing.JMenuItem wellPasteInfoMenuItem;
    private javax.swing.JPopupMenu wellPopupMenu;
    private javax.swing.JLabel wellRowColumnLabel;
    private javax.swing.JTextField wellRowColumnTextField;
    private javax.swing.JButton wellSaveButton;
    // End of variables declaration//GEN-END:variables

    /**
     * Reads in all the required parameters from their designated text fields
     * and check boxes, parses them if needed and performs the counting
     * accordingly.
     */
    private void goCount() {
        try {
            if (currentInputFile == null) {
                JOptionPane.showMessageDialog(
                        this,
                        "Please, switch to the view panel and browse "
                        + "for an input file.",
                        "Input File Missing",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Collect exclusions
            List<Integer> excludedRowsIndices = parseIndices(rowsExclusionsTextField);
            List<Integer> excludedColumnsIndices = parseIndices(columnsExclusionsTextField);
            List<Integer> excludedPlanesIndices = parseIndices(planesExclusionsTextField);
            List<Integer> excludedFieldsIndices = parseIndices(fieldsExclusionsTextField);
            // Collect other parameters
            boolean countPerPlane;
            boolean countPerField;
            if (countPerPlaneCheckBox.isSelected()) {
                countPerPlane = true;
            } else {
                countPerPlane = false;
            }
            if (countPerFieldCheckBox.isSelected()) {
                countPerField = true;
            } else {
                countPerField = false;
            }
            int countingStep = Integer.parseInt(countingStepTextField.getText().trim());
            // Start Counting
            HashMap<String, Integer> countingResults = Utilities.count(countPerPlane,
                    countPerField,
                    IO.readRecords(currentInputFile),
                    new HashSet<Integer>(excludedRowsIndices),
                    new HashSet<Integer>(excludedColumnsIndices),
                    new HashSet<Integer>(excludedPlanesIndices),
                    new HashSet<Integer>(excludedFieldsIndices),
                    countingStep);
            // Display in the view table
            // Columns headers
            Vector<String> columnsHeaders = new Vector<>();
            columnsHeaders.add("row");
            columnsHeaders.add("column");
            columnsHeaders.add("plane");
            columnsHeaders.add("field");
            columnsHeaders.add("count");
            // Counting results
            Set<String> sortedKeys = new TreeSet<>(countingResults.keySet());
            Vector<Vector<String>> resultsData = new Vector<>();
            for (String key : sortedKeys) {
                String[] splits = key.split("\\-");
                Vector<String> resultsRecord = new Vector<>();
                for (String split : splits) {
                    if (split.equalsIgnoreCase("n/a")) {
                        resultsRecord.add(split);
                    } else {
                        resultsRecord.add(String.valueOf(Integer.parseInt(split)));
                    }
                }
                resultsRecord.add(String.valueOf(countingResults.get(key)));
                resultsData.add(resultsRecord);
            }
            countingTable.setModel(new DefaultTableModel(resultsData, columnsHeaders));
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.toString());
            Logger.getLogger(StartFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private List<Integer> parseIndices(JTextField exclusionsTextField) {
        List<Integer> excludedIndices = new ArrayList<>();
        if (exclusionsTextField.getText().trim().equals("")) {
            return excludedIndices;
        }
        // If text exists, parse it
        String[] splits = exclusionsTextField.getText().trim().split(",");
        for (String split : splits) {
            if (split.contains("-")) {
                String[] range = split.split("\\-");
                for (int i = Integer.parseInt(range[0].trim()); i <= Integer.parseInt(range[1].trim()); i++) {
                    if (!excludedIndices.contains(i)) {
                        excludedIndices.add(i);
                    }
                }
            } else {
                int index = Integer.parseInt(split.trim());
                if (!excludedIndices.contains(index)) {
                    excludedIndices.add(index);
                }
            }
        }
        return excludedIndices;
    }

    private void exportToExcel(JTable countingTable, File outFile) throws IOException {
        BufferedOutputStream bufferedOut = null;
        try {
            // Create workbook
            HSSFWorkbook wb = new HSSFWorkbook();
            // Create sheet
            HSSFSheet sheet = wb.createSheet("Counting Results");
            // Add column headers to your first excel row
            TableColumnModel tableColumnsModel = countingTable.getColumnModel();
            HSSFRow headersRow = sheet.createRow(0);
            for (int j = 0; j < tableColumnsModel.getColumnCount(); j++) {
                HSSFCell cell = headersRow.createCell(j);
                cell.setCellValue(tableColumnsModel.getColumn(j).getHeaderValue().toString());
            }
            // Add sounting results
            for (int i = 0; i < countingTable.getModel().getRowCount(); i++) {
                HSSFRow dataRow = sheet.createRow((short) (i + 1));
                for (int j = 0; j < countingTable.getModel().getColumnCount(); j++) {
                    HSSFCell cell = dataRow.createCell(j);
                    String valueStr = (String) countingTable.getModel().getValueAt(i, j);
                    try {
                        cell.setCellValue(Integer.parseInt(valueStr));
                    } catch (NumberFormatException ex) {
                        cell.setCellValue(valueStr);
                    }
                }
            }
            // Write the new excel sheet
            bufferedOut = new BufferedOutputStream(new FileOutputStream(outFile.getPath() + ".xls"));
            wb.write(bufferedOut);
            bufferedOut.flush();
            // Display a confirmation message
            JOptionPane.showMessageDialog(
                    this,
                    "Results exported successfully as an Excel file.",
                    "Exported Successfully",
                    JOptionPane.INFORMATION_MESSAGE);
        } finally {
            if (bufferedOut != null) {
                bufferedOut.close();
            }
        }
    }
}
