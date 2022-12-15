/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.brown.predictivebiology.gui;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Haitham Seada
 */
public class RowColumnFieldPlane {

    private int row;
    private int column;
    private int field;
    private int plane;

    public RowColumnFieldPlane(File file) {
        Pattern rowPattern = Pattern.compile("r\\d+");
        Matcher rowMatcher = rowPattern.matcher(file.getName());
        // Match column index
        Pattern colPattern = Pattern.compile("c\\d+");
        Matcher colMatcher = colPattern.matcher(file.getName());
        // Match field index
        Pattern fieldPattern = Pattern.compile("f\\d+");
        Matcher fieldMatcher = fieldPattern.matcher(file.getName());
        // Match plane index
        Pattern planePattern = Pattern.compile("p\\d+");
        Matcher planeMatcher = planePattern.matcher(file.getName());
        // Set the fields
        if (rowMatcher.find()
                && colMatcher.find()
                && fieldMatcher.find()
                && planeMatcher.find()) {
            this.row = Integer.parseInt(rowMatcher.group().substring(1));
            this.column = Integer.parseInt(colMatcher.group().substring(1));
            this.field = Integer.parseInt(fieldMatcher.group().substring(1));
            this.plane = Integer.parseInt(planeMatcher.group().substring(1));
        } else {
            throw new IllegalArgumentException("File name does not follow "
                    + "the convention *rXXcXXfXXpXX* where X is any digit "
                    + "from 0 to 9 and * represents any string.");
        }
    }

    public RowColumnFieldPlane(int row, int column, int field, int plane) {
        this.row = row;
        this.column = column;
        this.field = field;
        this.plane = plane;
    }

    /**
     * @return the row
     */
    public int getRow() {
        return row;
    }

    /**
     * @param row the row to set
     */
    public void setRow(int row) {
        this.row = row;
    }

    /**
     * @return the column
     */
    public int getColumn() {
        return column;
    }

    /**
     * @param column the column to set
     */
    public void setColumn(int column) {
        this.column = column;
    }

    /**
     * @return the field
     */
    public int getField() {
        return field;
    }

    /**
     * @param field the field to set
     */
    public void setField(int field) {
        this.field = field;
    }

    /**
     * @return the plane
     */
    public int getPlane() {
        return plane;
    }

    /**
     * @param plane the plane to set
     */
    public void setPlane(int plane) {
        this.plane = plane;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RowColumnFieldPlane other = (RowColumnFieldPlane) obj;
        if (this.row != other.row) {
            return false;
        }
        if (this.column != other.column) {
            return false;
        }
        if (this.field != other.field) {
            return false;
        }
        if (this.plane != other.plane) {
            return false;
        }
        return true;
    }
}
