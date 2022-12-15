/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.brown.predictivebiology.gui;

import com.mathworks.toolbox.javabuilder.MWException;
import edu.brown.predictivebiology.db.DbUtilities;
import edu.brown.predictivebiology.db.beans.Experiment;
import edu.brown.predictivebiology.db.beans.Image;
import edu.brown.predictivebiology.utilities.MachineLearning;
import edu.brown.predictivebiology.utilities.SubImageInfo;
import edu.brown.predictivebiology.utilities.Utilities;
import java.awt.Component;
import java.awt.Cursor;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

/**
 *
 * @author Haitham
 */
public class ExtractPotentialLumensProgressDialog extends javax.swing.JDialog
        implements PropertyChangeListener {

    private Task task;
    private final Component parentComponent;
    private final JList<String> experimentsList;
    List<Image> completedSoFar;

    /**
     * Creates new form ProgressDialog
     */
    public ExtractPotentialLumensProgressDialog(
            java.awt.Frame parentComponent,
            JList<String> experimentsList,
            boolean modal) {
        super(parentComponent, modal);
        this.completedSoFar = new ArrayList<>();
        this.parentComponent = parentComponent;
        this.experimentsList = experimentsList;
        initComponents();
        setLocationByPlatform(true);
        start();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        progressDialogProgressBar = new javax.swing.JProgressBar();
        jScrollPane1 = new javax.swing.JScrollPane();
        progressDialogTextArea = new javax.swing.JTextArea();
        progressDialogDoneButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        progressDialogTextArea.setColumns(20);
        progressDialogTextArea.setRows(5);
        progressDialogTextArea.setEnabled(false);
        jScrollPane1.setViewportView(progressDialogTextArea);

        progressDialogDoneButton.setText("Done");
        progressDialogDoneButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                progressDialogDoneButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(progressDialogProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 873, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(progressDialogDoneButton, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(progressDialogProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 212, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(progressDialogDoneButton)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void progressDialogDoneButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_progressDialogDoneButtonActionPerformed
        this.dispose();
    }//GEN-LAST:event_progressDialogDoneButtonActionPerformed

    public void start() {
        progressDialogDoneButton.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        //Instances of javax.swing.SwingWorker are not reusuable, so
        //we create new instances as needed.
        task = new Task();
        task.addPropertyChangeListener(this);
        task.execute();
    }

    class Task extends SwingWorker<Void, Void> {

        /*
         * Main task. Executed in background thread.
         */
        @Override
        public Void doInBackground() throws Exception {
            List<Integer> selectedIds = GuiUtility.getSelectedIds(experimentsList);
            if (selectedIds.size() != 1) {
                JOptionPane.showMessageDialog(
                        parentComponent,
                        "Please, select one experiment.",
                        "Select One",
                        JOptionPane.ERROR_MESSAGE);
            } else {
                setProgress(0);
                try {
                    Experiment experiment
                            = DbUtilities.getExperimentById(selectedIds.get(0));
                    List<Image> images
                            = DbUtilities.getImagesOfExperiment(experiment.getId());
                    List<List<SubImageInfo>> listOfLists = new ArrayList<>();
                    for (int i = 0; i < images.size(); i++) {
                        System.out.println("Processing: " + images.get(i).getPath());
                        listOfLists.add(new MachineLearning()
                                .extractLearningData(
                                        "preclassify",
                                        images.get(i),
                                        Utilities.readProperty("working-dir"),
                                        0.5, // Linear fusion factor
                                        0.8, // Tagging scaling factor
                                        6, // Disk size start
                                        5, // Disk size step
                                        40, // Disk size end // Original 40
                                        0.75, // Binarization threshold // Original 0.5
                                        500, // Opening size
                                        0.2, // Area of the disk used to create the margin (as a percentage of the total area of the lumen)
                                        25, // Maximum radious of the raius of the disk used to create the margin
                                        false));
                        completedSoFar.add(images.get(i));
                        int progress = (int) Math.min(
                                Math.ceil(((i + 1.0) / images.size()) * 100),
                                100);
                        if(progress != progressDialogProgressBar.getValue()) {
                            setProgress(progress);
                        }
                    }
//                    return listOfLists;
//                    JOptionPane.showMessageDialog(
//                            parentComponent,
//                            "Preclassified sub-images successfully generated.",
//                            "Pre-classification performed successfully.",
//                            JOptionPane.INFORMATION_MESSAGE);
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
            return null;
        }

        /*
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
            setCursor(null); //turn off the wait cursor
            progressDialogTextArea.append("Done!\n");
            progressDialogDoneButton.setEnabled(true);
        }
    }

    /**
     * Invoked when task's progress property changes.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress" == evt.getPropertyName()) {
            int progress = (Integer) evt.getNewValue();
            progressDialogProgressBar.setValue(progress);
            progressDialogTextArea.append(String.format(
                    "%d%%: %s >> potential lumens extracted.\n",
                    task.getProgress(),
                    "Image Path Sample"));
            progressDialogProgressBar.setString(String.format("%d%%", progress));
            for (Image image : completedSoFar) {
                progressDialogTextArea.append(image.getPath() + ": Extraction Completed.");
            }
            completedSoFar.clear();
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton progressDialogDoneButton;
    private javax.swing.JProgressBar progressDialogProgressBar;
    private javax.swing.JTextArea progressDialogTextArea;
    // End of variables declaration//GEN-END:variables
}
