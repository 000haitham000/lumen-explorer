/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.brown.predictivebiology.cellcount;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 *
 * @author Haitham
 */
public class Utilities {

    /**
     * Count the records either per row-column, row-column-plane or
     * row-column-plane-field combinations, according to the exclusions
     * specified.
     *
     * @param countPlanes if false, the returned count is per row-column
     * combinations, otherwise it can be either for row-column-plane or
     * row-column-plane-field combinations, depending on the value of
     * countFields
     * @param countFields if countPlanes is false, this parameter has not
     * effect, the returned count will be of row-column combinations anyway.
     * However, if countPlanes is true, the value of countFields specifies
     * whether the returned count will be of row-column-plane combinations (if
     * false) or row-column-plane-field combinations (if true)
     * @param records records to count
     * @param rowExclusions indices of rows to exclude
     * @param columnExclusions indices of columns to exclude
     * @param planeExclusions indices of planes to exclude
     * @param fieldExclusions indices of fields to exclude
     * @param countingStep the step of counting e.g. 2 means that you count
     * record 0, then 2, then 4 etc.
     */
    public static HashMap<String, Integer> count(
            boolean countPlanes,
            boolean countFields,
            List<Record> records,
            Set<Integer> rowExclusions,
            Set<Integer> columnExclusions,
            Set<Integer> planeExclusions,
            Set<Integer> fieldExclusions,
            int countingStep) {
        HashMap<String, Integer> countMap = new HashMap<>();
        // Start counting
        for (int i = 0; i < records.size(); i++) {
            if (i % countingStep == 0) {
                if (!isExcluded(records.get(i),
                        rowExclusions,
                        columnExclusions,
                        planeExclusions,
                        fieldExclusions)) {
                    // Form the label identifying the entry of this record in the count map
                    String label = String.format("%03d-%03d", records.get(i).getRow(), records.get(i).getColumn());
                    if (countPlanes) {
                        label += String.format("-%03d", records.get(i).getPlane());
                    } else {
                        label += "-N/A";
                    }
                    if (countFields) {
                        label += String.format("-%03d", records.get(i).getField());
                    } else {
                        label += "-N/A";
                    }
                    // Check if the label already exists. If so increment it by one.
                    // Otherwise, add the new label to the count map.
                    if (countMap.containsKey(label)) {
                        countMap.put(label, countMap.get(label) + 1);
                    } else {
                        countMap.put(label, 1);
                    }
                }
            }
        }
        // Return countMap
        return countMap;
    }

    /**
     * Check if a record should be excluded from counting or not.
     *
     * @param record
     * @param rowExclusions indices of rows to exclude
     * @param columnExclusions indices of columns to exclude
     * @param planeExclusions indices of planes to exclude
     * @param fieldExclusions indices of fields to exclude
     * @return true if the record should be excluded, false otherwise
     */
    private static boolean isExcluded(Record record,
            Set<Integer> rowExclusions,
            Set<Integer> columnExclusions,
            Set<Integer> planeExclusions,
            Set<Integer> fieldExclusions) {
        return rowExclusions != null && rowExclusions.contains(record.getRow())
                || columnExclusions != null && columnExclusions.contains(record.getColumn())
                || planeExclusions != null && planeExclusions.contains(record.getPlane())
                || fieldExclusions != null && fieldExclusions.contains(record.getField());
    }
}
