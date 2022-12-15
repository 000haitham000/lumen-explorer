/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.brown.predictivebiology.gui;

import edu.brown.predictivebiology.db.beans.WellDescription;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

public class WellRenderer extends JPanel implements TableCellRenderer {

    private int experimentId;

    JLabel compoundLabel = new JLabel("Compound: ");
    JLabel concentrationLabel = new JLabel("Concentration: ");
    JLabel cellTypeLabel = new JLabel("Cell Type: ");
    JLabel cellCountLabel = new JLabel("Cell Count: ");
    JLabel notesLabel = new JLabel("Notes: ");

    public WellRenderer(int experimentId) {
        super.setOpaque(true);
        this.experimentId = experimentId;
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus, int row, int column) {
        JPanel cellPanel = new JPanel();
        cellPanel.setOpaque(true);
        cellPanel.setBackground(Color.WHITE);
        WellDescription well = (WellDescription) table.getModel().getValueAt(row, column);
        if (well == null) {
            cellPanel.setLayout(new BorderLayout());
            JLabel emptyLabel = new JLabel("EMPTY");
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            cellPanel.add(emptyLabel, BorderLayout.CENTER);
        } else {
            cellPanel.setLayout(new BoxLayout(cellPanel, BoxLayout.PAGE_AXIS));
            cellPanel.setBorder(BorderFactory.createEmptyBorder(7, 10, 10, 10));
            cellPanel.add(new JLabel(String.format("Compound: %s", well.getCompound())));
            cellPanel.add(new JLabel(String.format("Concentraion: %7.5f", well.getConcentration())));
            cellPanel.add(new JLabel(String.format("Cell Type: %s", well.getCellType())));
            cellPanel.add(new JLabel(String.format("Cell Type: %d", well.getCellCount())));
        }
        if (isSelected) {
            cellPanel.setBackground(UIManager.getColor("Table.selectionBackground"));
            for (Component component : cellPanel.getComponents()) {
                component.setForeground(Color.WHITE);
            }
        }
        return cellPanel;
    }
}
