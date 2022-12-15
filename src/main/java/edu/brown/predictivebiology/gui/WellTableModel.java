/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.brown.predictivebiology.gui;

import edu.brown.predictivebiology.db.beans.WellDescription;
import javax.swing.table.AbstractTableModel;

public class WellTableModel extends AbstractTableModel {

    private final WellDescription[][] wells;
    private final String[] columnNames;
    private final Class[] columnClasses;
    private WellDescription copiedWell;

    public WellTableModel(WellDescription[][] wells) {
        this.wells = wells;
        this.columnNames = new String[wells[0].length];
        this.columnClasses = new Class[wells[0].length];
        for (int i = 0; i < columnNames.length; i++) {
            columnNames[i] = String.format("Column(%d)", i);
            columnClasses[i] = WellDescription.class;
        }
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnClasses[columnIndex];
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public int getRowCount() {
        return wells.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return wells[rowIndex][columnIndex];
    }

    @Override
    public void setValueAt(Object obj, int rowIndex, int columnIndex) {
        super.setValueAt(obj, rowIndex, columnIndex);
        wells[rowIndex][columnIndex] = (WellDescription)obj;
    }

    /**
     * @return the copiedWell
     */
    public WellDescription getCopiedWell() {
        return copiedWell;
    }

    /**
     * @param copiedWell the copiedWell to set
     */
    public void setCopiedWell(WellDescription copiedWell) {
        this.copiedWell = copiedWell;
    }
}
