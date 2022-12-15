/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.brown.predictivebiology.utilities;

import edu.brown.predictivebiology.gui.RowColumnFieldPlane;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Haitham
 */
public class LumenMap {

    private int minRow = Integer.MAX_VALUE;
    private int maxRow = Integer.MIN_VALUE;
    private int minColumn = Integer.MAX_VALUE;
    private int maxColumn = Integer.MIN_VALUE;
    private int minField = Integer.MAX_VALUE;
    private int maxField = Integer.MIN_VALUE;
    private int minPlane = Integer.MAX_VALUE;
    private int maxPlane = Integer.MIN_VALUE;

    private Map<Integer, Map<Integer, Map<Integer, Map<Integer, List<File>>>>> map
            = new HashMap<>();

    public void add(int row, int column, int field, int plane, File infoFile) {
        if (map.get(row) == null) {
            map.put(row, new HashMap<>());
        }
        if (map.get(row).get(column) == null) {
            map.get(row).put(column, new HashMap<>());
        }
        if (map.get(row).get(column).get(field) == null) {
            map.get(row).get(column).put(field, new HashMap<>());
        }
        if (map.get(row).get(column).get(field).get(plane) == null) {
            map.get(row).get(column).get(field).put(plane, new ArrayList<>());
        }
        map.get(row).get(column).get(field).get(plane).add(infoFile);
        // Update min/max values
        if (row < minRow) {
            minRow = row;
        }
        if (row > maxRow) {
            maxRow = row;
        }
        if (column < minColumn) {
            minColumn = column;
        }
        if (column > maxColumn) {
            maxColumn = column;
        }
        if (field < minField) {
            minField = field;
        }
        if (field > maxField) {
            maxField = field;
        }
        if (plane < minPlane) {
            minPlane = plane;
        }
        if (plane > maxPlane) {
            maxPlane = plane;
        }
    }

    public void removeAll(int row, int column, int field, int plane) {
        RowColumnFieldPlane rcfl = new RowColumnFieldPlane(
                row, 
                column, 
                field, 
                plane);
        if (map.get(rcfl.getRow()) != null
                && map.get(rcfl.getRow())
                        .get(rcfl.getColumn()) != null
                && map.get(rcfl.getRow())
                        .get(rcfl.getColumn())
                        .get(rcfl.getField()) != null
                && map.get(rcfl.getRow())
                        .get(rcfl.getColumn())
                        .get(rcfl.getField())
                        .get(rcfl.getPlane()) != null) {
            map.get(rcfl.getRow())
                    .get(rcfl.getColumn())
                    .get(rcfl.getField())
                    .put(rcfl.getPlane(), null);
        }
    }

    public void remove(File file) {
        RowColumnFieldPlane rcfl = new RowColumnFieldPlane(file);
        if (map.get(rcfl.getRow()) != null
                && map.get(rcfl.getRow())
                        .get(rcfl.getColumn()) != null
                && map.get(rcfl.getRow())
                        .get(rcfl.getColumn())
                        .get(rcfl.getField()) != null
                && map.get(rcfl.getRow())
                        .get(rcfl.getColumn())
                        .get(rcfl.getField())
                        .get(rcfl.getPlane()) != null) {
            map.get(rcfl.getRow())
                    .get(rcfl.getColumn())
                    .get(rcfl.getField())
                    .get(rcfl.getPlane()).remove(file);
        }
    }

    public List<File> getLumenInfoFiles(
            int row,
            int column,
            int field,
            int plane) {
        if (map.containsKey(row)) {
            if (map.get(row).containsKey(column)) {
                if (map.get(row).get(column).containsKey(field)) {
                    return map.get(row).get(column).get(field).get(plane);
                }
            }
        }
        return null;
    }

    public List<File> getLumenInfoFileAllFields(
            int row,
            int column,
            int plane) {
        List<File> lumenInfoFilesList = new ArrayList<>();
        if (map.containsKey(row)) {
            if (map.get(row).containsKey(column)) {
                for (Map.Entry<Integer, Map<Integer, List<File>>> allPlanesInAField
                        : map.get(row).get(column).entrySet()) {
                    for (Map.Entry<Integer, List<File>> allLumensInPlane
                            : map.get(row).get(column).get(allPlanesInAField.getKey()).entrySet()) {
                        lumenInfoFilesList.addAll(allLumensInPlane.getValue());
                    }
                }
            }
        }
        return lumenInfoFilesList;
    }

    public List<List<File>> getLumenInfoFileAllPlanesLowerToHigher(
            int row,
            int column,
            int field) {
        List<List<File>> lumenInfoFilesList = new ArrayList<>();
        if (map.containsKey(row)) {
            if (map.get(row).containsKey(column)) {
                if (map.get(row).get(column).containsKey(field)) {
                    for (int plane = getMinPlane(); plane <= getMaxPlane(); plane++) {
                        lumenInfoFilesList.add(map.get(row).get(column).get(field).get(plane));
                    }
                }
            }
        }
        return lumenInfoFilesList;
    }

    public int getLumenCount() {
        int count = 0;
        for (Map.Entry<Integer, Map<Integer, Map<Integer, Map<Integer, List<File>>>>> rowEntry : map.entrySet()) {
            for (Map.Entry<Integer, Map<Integer, Map<Integer, List<File>>>> columnEntry : rowEntry.getValue().entrySet()) {
                for (Map.Entry<Integer, Map<Integer, List<File>>> fieldEntry : columnEntry.getValue().entrySet()) {
                    for (Map.Entry<Integer, List<File>> planeEntry : fieldEntry.getValue().entrySet()) {
                        count += planeEntry.getValue().size();
                    }
                }
            }
        }
        return count;
    }

    /**
     * @return the minRow
     */
    public int getMinRow() {
        return minRow;
    }

    /**
     * @param minRow the minRow to add
     */
    public void setMinRow(int minRow) {
        this.minRow = minRow;
    }

    /**
     * @return the maxRow
     */
    public int getMaxRow() {
        return maxRow;
    }

    /**
     * @param maxRow the maxRow to add
     */
    public void setMaxRow(int maxRow) {
        this.maxRow = maxRow;
    }

    /**
     * @return the minColumn
     */
    public int getMinColumn() {
        return minColumn;
    }

    /**
     * @param minColumn the minColumn to add
     */
    public void setMinColumn(int minColumn) {
        this.minColumn = minColumn;
    }

    /**
     * @return the maxColumn
     */
    public int getMaxColumn() {
        return maxColumn;
    }

    /**
     * @param maxColumn the maxColumn to add
     */
    public void setMaxColumn(int maxColumn) {
        this.maxColumn = maxColumn;
    }

    /**
     * @return the minField
     */
    public int getMinField() {
        return minField;
    }

    /**
     * @param minField the minField to add
     */
    public void setMinField(int minField) {
        this.minField = minField;
    }

    /**
     * @return the maxField
     */
    public int getMaxField() {
        return maxField;
    }

    /**
     * @param maxField the maxField to add
     */
    public void setMaxField(int maxField) {
        this.maxField = maxField;
    }

    /**
     * @return the minPlane
     */
    public int getMinPlane() {
        return minPlane;
    }

    /**
     * @param minPlane the minPlane to add
     */
    public void setMinPlane(int minPlane) {
        this.minPlane = minPlane;
    }

    /**
     * @return the maxPlane
     */
    public int getMaxPlane() {
        return maxPlane;
    }

    /**
     * @param maxPlane the maxPlane to add
     */
    public void setMaxPlane(int maxPlane) {
        this.maxPlane = maxPlane;
    }

//    /**
//     * @return the map
//     */
//    public Map<Integer, Map<Integer, Map<Integer, Map<Integer, File>>>> getMap() {
//        return map;
//    }
//
//    /**
//     * @param map the map to add
//     */
//    public void setMap(Map<Integer, Map<Integer, Map<Integer, Map<Integer, File>>>> map) {
//        this.map = map;
//    }
}
