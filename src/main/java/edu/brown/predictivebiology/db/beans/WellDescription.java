/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.brown.predictivebiology.db.beans;

/**
 *
 * @author Haitham
 */
public class WellDescription {

    Integer plateId;
    Integer row;
    Integer col;
    String compound;
    Double concentration;
    String cellType;
    Integer cellCount;

    public WellDescription() {
    }

    public WellDescription(
            Integer plateId,
            Integer row,
            Integer col,
            String compound,
            Double concentration,
            String cellType,
            Integer cellCount) {
        this.plateId = plateId;
        this.row = row;
        this.col = col;
        this.compound = compound;
        this.concentration = concentration;
        this.cellType = cellType;
        this.cellCount = cellCount;
    }

    public Integer getPlateId() {
        return plateId;
    }

    public Integer getRow() {
        return row;
    }

    public Integer getCol() {
        return col;
    }

    public String getCompound() {
        return compound;
    }

    public Double getConcentration() {
        return concentration;
    }

    public String getCellType() {
        return cellType;
    }

    public Integer getCellCount() {
        return cellCount;
    }

    public void setPlateId(Integer plateId) {
        this.plateId = plateId;
    }

    public void setRow(Integer row) {
        this.row = row;
    }

    public void setCol(Integer col) {
        this.col = col;
    }

    public void setCompound(String compound) {
        this.compound = compound;
    }

    public void setConcentration(Double concentration) {
        this.concentration = concentration;
    }

    public void setCellType(String cellType) {
        this.cellType = cellType;
    }

    public void setCellCount(Integer cellCount) {
        this.cellCount = cellCount;
    }

    @Override
    public String toString() {
        return String.format("WellDescription{"
                + "plateId=%d, "
                + "row=%d, "
                + "col=%d, "
                + "compound=%s, "
                + "concentration=%5.3f, "
                + "cellType=%s, "
                + "cellCount=%d}",
                this.plateId,
                this.row,
                this.col,
                this.compound,
                this.concentration,
                this.cellType,
                this.cellCount);
    }

    public String to$Separated() {
        return String.format("%d$%d$%d$%s$%5.3f$%s$%d",
                this.plateId,
                this.row,
                this.col,
                this.compound,
                this.concentration,
                this.cellType,
                this.cellCount);
    }
    
    public static WellDescription string2Object(String $Separated) {
        String[] splits = $Separated.split("\\$");
        return new WellDescription(
                new Integer(splits[0]), 
                new Integer(splits[1]), 
                new Integer(splits[2]), 
                splits[3], 
                new Double(splits[4]), 
                splits[5],
                new Integer(splits[6]));
    }
}
