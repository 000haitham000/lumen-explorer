/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.brown.predictivebiology.db.beans;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An object of this class represents one row in the images table in the
 * database.
 *
 * @author Haitham
 */
public class Image implements Comparable<Image> {

    private Integer id;
    private String path;
    private Integer parentDirId;

    public Image() {
    }

    public Image(Integer id, String path, Integer parentDirId) {
        this.id = id;
        this.path = path;
        this.parentDirId = parentDirId;
    }

    /**
     * @return the id
     */
    public Integer getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path the path to set
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return the parentDirId
     */
    public Integer getParentDirId() {
        return parentDirId;
    }

    /**
     * @param parentDirId the parentDirId to set
     */
    public void setParentDirId(Integer parentDirId) {
        this.parentDirId = parentDirId;
    }

    @Override
    public String toString() {
        return String.format("Image{id=%d, path=%s, parentDirId=%d}",
                this.id,
                this.path,
                this.parentDirId);
    }

    public String to$Separated() {
        return String.format("%d$%s$%d",
                this.id,
                this.path,
                this.parentDirId);
    }

    @Override
    public int compareTo(Image image) {
        String thisImageName = new File(this.getPath()).getName();
        String thatImageName = new File(image.getPath()).getName();
        // Match row index
        Pattern rowPattern = Pattern.compile("r\\d+");
        Matcher thisRowMatcher = rowPattern.matcher(thisImageName);
        Matcher thatRowMatcher = rowPattern.matcher(thatImageName);
        // Match column index
        Pattern colPattern = Pattern.compile("c\\d+");
        Matcher thisColMatcher = colPattern.matcher(thisImageName);
        Matcher thatColMatcher = colPattern.matcher(thatImageName);
        // Match field index
        Pattern fieldPattern = Pattern.compile("f\\d+");
        Matcher thisFieldMatcher = fieldPattern.matcher(thisImageName);
        Matcher thatFieldMatcher = fieldPattern.matcher(thatImageName);
        // Match plane index
        Pattern planePattern = Pattern.compile("p\\d+");
        Matcher thisPlaneMatcher = planePattern.matcher(thisImageName);
        Matcher thatPlaneMatcher = planePattern.matcher(thatImageName);
        // Make sure both images follow the correct format
        boolean thisNameIsValid = false;
        if (thisRowMatcher.find()
                && thisColMatcher.find()
                && thisFieldMatcher.find()
                && thisPlaneMatcher.find()) {
            thisNameIsValid = true;
        }
        boolean thatNameIsValid = false;
        if (thatRowMatcher.find()
                && thatColMatcher.find()
                && thatFieldMatcher.find()
                && thatPlaneMatcher.find()) {
            thatNameIsValid = true;
        }
        // If the two images are valid compare them according to row, then
        // column, then field, then plane
        if (thisNameIsValid && thatNameIsValid) {
            int thisRow = Integer.parseInt(thisRowMatcher.group().substring(1));
            int thatRow = Integer.parseInt(thatRowMatcher.group().substring(1));
            if (thisRow == thatRow) {
                // Rows are equal, chech columns
                int thisCol = Integer.parseInt(thisColMatcher.group().substring(1));
                int thatCol = Integer.parseInt(thatColMatcher.group().substring(1));
                if (thisCol == thatCol) {
                    // Columns are also equal, check fields
                    int thisField = Integer.parseInt(thisFieldMatcher.group().substring(1));
                    int thatField = Integer.parseInt(thatFieldMatcher.group().substring(1));
                    if (thisField == thatField) {
                        // Fields are also equal, check planes
                        int thisPlane = Integer.parseInt(thisPlaneMatcher.group().substring(1));
                        int thatPlane = Integer.parseInt(thatPlaneMatcher.group().substring(1));
                        if (thisPlane == thatPlane) {
                            // The two images are equal in row, column, filed
                            // and plane
                            return 0;
                        } else {
                            // Compare based on plabe index
                            return thisPlane < thatPlane ? -1 : 1;
                        }
                    } else {
                        // Compare based on filed index
                        return thisField < thatField ? -1 : 1;
                    }
                } else {
                    // Compare based on column index
                    return thisCol < thatCol ? -1 : 1;
                }
            } else {
                // Comprare based on row index
                return thisRow < thatRow ? -1 : 1;
            }
        } else if (thisNameIsValid && !thatNameIsValid) {
            // The name of this file follows the convention, but the one it is
            // being compared to is not
            return -1;
        } else if (!thisNameIsValid && thatNameIsValid) {
            // The name of this file does not follow the convention, but the one
            // it is being compared to does
            return 1;
        } else {
            // Both the two files do not follow the proper naming converntion.
            // In this case sort them alphabetically
            return thisImageName.compareToIgnoreCase(thatImageName);
        }
    }

    public static Image string2Object(String $Separated) {
        String[] splits = $Separated.split("\\$");
        return new Image(
                new Integer(splits[0]),
                splits[1],
                new Integer(splits[2]));
    }
}
