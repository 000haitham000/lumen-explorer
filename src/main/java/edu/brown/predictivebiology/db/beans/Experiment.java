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
public class Experiment {

    Integer id;
    String name;
    Integer rowCount;
    Integer colCount;
    String plateName;

    public Experiment() {
    }

    public Experiment(Integer id, String name, Integer rowCount, Integer colCount, String plateName) {
        this.id = id;
        this.name = name;
        this.rowCount = rowCount;
        this.colCount = colCount;
        this.plateName = plateName;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getRowCount() {
        return rowCount;
    }

    public Integer getColCount() {
        return colCount;
    }

    public String getPlateName() {
        return plateName;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRowCount(Integer rowCount) {
        this.rowCount = rowCount;
    }

    public void setColCount(Integer colCount) {
        this.colCount = colCount;
    }

    public void setPlateName(String plateName) {
        this.plateName = plateName;
    }

    @Override
    public String toString() {
        return String.format("Experiment{"
                + "id=%d, "
                + "name=%s, "
                + "rowCount=%d, "
                + "colCount=%d, "
                + "plateName=%s}",
                this.id,
                this.name,
                this.rowCount,
                this.colCount,
                this.plateName);
    }

    public String to$Separated() {
        return String.format("%d$%s$%d$%d$%s",
                this.id,
                this.name,
                this.rowCount,
                this.colCount,
                this.plateName);
    }

    public static Experiment string2Object(String $Separated) {
        String[] splits = $Separated.split("\\$");
        return new Experiment(
                new Integer(splits[0]),
                splits[1],
                new Integer(splits[2]),
                new Integer(splits[3]), splits[4]);
    }
}
