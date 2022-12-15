/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.brown.predictivebiology.cellcount;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import javax.swing.JCheckBox;

/**
 *
 * @author Haitham
 */
public class CheckBoxOfTableColumn {

    private JCheckBox checkBox;
    private int coresspondingTableColumn;
    private Vector<String> columnData;

    public CheckBoxOfTableColumn(
            String label,
            boolean selected,
            int coresspondingColumnIndex,
            Vector<String> columnData) {
        this.checkBox = new JCheckBox(label, selected);
        this.coresspondingTableColumn = coresspondingColumnIndex;
        this.columnData = columnData;
    }

    /**
     * @return the checkBox
     */
    public JCheckBox getCheckBox() {
        return checkBox;
    }

    /**
     * @param checkBox the checkBox to set
     */
    public void setCheckBox(JCheckBox checkBox) {
        this.checkBox = checkBox;
    }

    /**
     * @return the coresspondingTableColumn
     */
    public int getCoresspondingTableColumn() {
        return coresspondingTableColumn;
    }

    /**
     * @param coresspondingTableColumn the coresspondingTableColumn to set
     */
    public void setCoresspondingTableColumn(int coresspondingTableColumn) {
        this.coresspondingTableColumn = coresspondingTableColumn;
    }

    /**
     * @return the columnData
     */
    public Vector<String> getColumnData() {
        return columnData;
    }

    /**
     * @param columnData the columnData to set
     */
    public void setColumnData(Vector<String> columnData) {
        this.columnData = columnData;
    }
}
