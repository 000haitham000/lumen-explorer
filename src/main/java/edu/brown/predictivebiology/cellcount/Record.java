/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.brown.predictivebiology.cellcount;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Haitham
 */
public class Record implements Comparable<Record> {

    private int row;
    private int column;
    private int plane;
    private int field;
    private final Map<String, String> metaInfo;

    private static final List<String> sortingCriteriaOrder;

    static {
        sortingCriteriaOrder = new ArrayList<>();
        sortingCriteriaOrder.add("row");
        sortingCriteriaOrder.add("column");
        sortingCriteriaOrder.add("plane");
        sortingCriteriaOrder.add("field");
    }

    /**
     * Set the order of criteria upon which sorting will be performed.
     *
     * @param sortingCriteriaOrder A list of criteria in order
     */
    public static void setSortingCriteriaOrders(List<String> sortingCriteriaOrder) {
        if (sortingCriteriaOrder != null && sortingCriteriaOrder.size() > 0) {
            Record.sortingCriteriaOrder.clear();
            Record.sortingCriteriaOrder.addAll(sortingCriteriaOrder);
        }
    }

    public Record(int row, int col, int plane, int field) {
        this.row = row;
        this.column = col;
        this.plane = plane;
        this.field = field;
        this.metaInfo = new LinkedHashMap<>();
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
     * @param col the column to set
     */
    public void setColumn(int col) {
        this.column = col;
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
     * Return meta info value using the corresponding key
     *
     * @param key
     * @return
     */
    public String getMetaInfo(String key) {
        return metaInfo.get(key.toLowerCase());
    }

    /**
     * Set meta info key-value pair
     *
     * @param key
     * @param value
     * @return
     */
    public String setMetaInfo(String key, String value) {
        return metaInfo.put(key.toLowerCase(), value);
    }

    /**
     * Return additional columns labels
     *
     * @return
     */
    public ArrayList<String> getAdditionalColumnsLabels() {
        return new ArrayList<>(metaInfo.keySet());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(String.format("Row: %-2d, Col: %-2d, Plane: %-3d, Field: %-2d", row, column, plane, field));
        for (String key : metaInfo.keySet()) {
            sb.append(String.format(", %s: %-10s", key, metaInfo.get(key)));
        }
        return sb.toString();
    }

    public String getValueOf(String columnLabel) {
        if (columnLabel.equalsIgnoreCase("row")) {
            return String.valueOf(row);
        } else if (columnLabel.equalsIgnoreCase("column")) {
            return String.valueOf(column);
        } else if (columnLabel.equalsIgnoreCase("plane")) {
            return String.valueOf(plane);
        } else if (columnLabel.equalsIgnoreCase("field")) {
            return String.valueOf(field);
        } else {
            return metaInfo.get(columnLabel.toLowerCase());
        }
    }

    @Override
    public int compareTo(Record otherRecord) {
        for (int i = 0; i < sortingCriteriaOrder.size(); i++) {
            String sortingCriterion = sortingCriteriaOrder.get(i);
            String thisStr = this.getValueOf(sortingCriterion);
            String otherStr = otherRecord.getValueOf(sortingCriterion);
            if (sortingCriterion.equalsIgnoreCase("row")
                    || sortingCriterion.equalsIgnoreCase("column")
                    || sortingCriterion.equalsIgnoreCase("plane")
                    || sortingCriterion.equalsIgnoreCase("field")
                    || sortingCriterion.equalsIgnoreCase("timepoint")
                    || sortingCriterion.equalsIgnoreCase("object no")
                    || sortingCriterion.equalsIgnoreCase("x")
                    || sortingCriterion.equalsIgnoreCase("y")
                    || sortingCriterion.equalsIgnoreCase("cell count")
                    || sortingCriterion.equalsIgnoreCase("nuclei - roi no")) {
                int thisNum = Integer.parseInt(thisStr);
                int otherNum = Integer.parseInt(otherStr);
                if (thisNum < otherNum) {
                    return -1;
                } else if (thisNum > otherNum) {
                    return 1;
                }
            } else if (sortingCriterion.equalsIgnoreCase("position x [µm]")
                    || sortingCriterion.equalsIgnoreCase("position y [µm]")
                    || sortingCriterion.equalsIgnoreCase("concentration")) {
                double thisNum = Double.parseDouble(thisStr);
                double otherNum = Double.parseDouble(otherStr);
                if (thisNum < otherNum) {
                    return -1;
                } else if (thisNum > otherNum) {
                    return 1;
                }

            } else {
                int comparison = thisStr.compareTo(otherStr);
                if (comparison != 0) {
                    return comparison;
                }
            }
        }
        return 0;
//        if (this.row < otherRecord.row) {
//            return -1;
//        } else if (this.row > otherRecord.row) {
//            return 1;
//        } else if (this.column < otherRecord.column) {
//            return -1;
//        } else if (this.column > otherRecord.column) {
//            return 1;
//        } else if (this.plane < otherRecord.plane) {
//            return -1;
//        } else if (this.plane > otherRecord.plane) {
//            return 1;
//        } else if (this.field < otherRecord.field) {
//            return -1;
//        } else if (this.field > otherRecord.field) {
//            return 1;
//        } else {
//            return 0;
//        }
    }
}
