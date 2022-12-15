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
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 *
 * @author Haitham
 */
public class ProgressPanel extends javax.swing.JPanel
        implements PropertyChangeListener {

    private Task task;
    private Image currentImage;
    private final Component parentComponent;
    private final JList<String> experimentsList;

    /**
     * Creates new form ProgressPanel
     *
     * @param parentComponent
     * @param experimentsList
     */
    public ProgressPanel(
            Component parentComponent,
            JList<String> experimentsList) {
        initComponents();
        this.parentComponent = parentComponent;
        this.experimentsList = experimentsList;
    }

    public void start() {
        progressPanelOKButton.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        //Instances of javax.swing.SwingWorker are not reusuable, so
        //we create new instances as needed.
        task = new Task();
        task.addPropertyChangeListener(this);
        task.execute();
    }

    /**
     * Invoked when task's progress property changes.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress" == evt.getPropertyName()) {
            int progress = (Integer) evt.getNewValue();
            progressPanelProgressBar.setValue(progress);
            progressPanelTextArea.append(String.format(
                    "%d%%: %s >> potential lumens extracted.\n",
                    task.getProgress(),
                    currentImage.getPath()));
        }
    }

    class Task extends SwingWorker<Void, Void> {

        /*
         * Main task. Executed in background thread.
         */
        @Override
        public Void doInBackground() throws Exception {

            java.awt.EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
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
                                currentImage = images.get(i);
                                System.out.println("Processing: " + images.get(i).getPath());
                                listOfLists.add(new MachineLearning()
                                        .extractLearningData(
                                                "preclassify",
                                                images.get(i),
                                                "E:\\Dropbox\\Harmony\\3D Harmony counts\\Test Images\\labeled images",
                                                0.5,
                                                0.8,
                                                10,
                                                5,
                                                40,
                                                0.2,
                                                500,
                                                0.5,
                                                25,
                                                false));
                                setProgress((int) Math.ceil(
                                        Math.min(1.0 * i / images.size(), 100)));
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
                }
            });
            return null;
        }

        /*
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
            setCursor(null); //turn off the wait cursor
            progressPanelTextArea.append("Done!\n");
            progressPanelOKButton.setEnabled(true);
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

        progressPanelProgressBar = new javax.swing.JProgressBar();
        jScrollPane1 = new javax.swing.JScrollPane();
        progressPanelTextArea = new javax.swing.JTextArea();
        progressPanelOKButton = new javax.swing.JButton();

        progressPanelTextArea.setColumns(20);
        progressPanelTextArea.setRows(5);
        jScrollPane1.setViewportView(progressPanelTextArea);

        progressPanelOKButton.setText("OK");
        progressPanelOKButton.setEnabled(false);
        progressPanelOKButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                progressPanelOKButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(progressPanelProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(progressPanelOKButton, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(progressPanelProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 159, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(progressPanelOKButton)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void progressPanelOKButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_progressPanelOKButtonActionPerformed
        parentComponent.setVisible(false);
    }//GEN-LAST:event_progressPanelOKButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton progressPanelOKButton;
    private javax.swing.JProgressBar progressPanelProgressBar;
    private javax.swing.JTextArea progressPanelTextArea;
    // End of variables declaration//GEN-END:variables
}
