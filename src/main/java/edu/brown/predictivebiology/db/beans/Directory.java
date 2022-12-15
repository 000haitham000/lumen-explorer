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
public class Directory {

    Integer id;
    String path;
    Integer experimentId;

    public Directory() {
    }

    public Directory(Integer id, String path, Integer experimentId) {
        this.id = id;
        this.path = path;
        this.experimentId = experimentId;
    }

    public Integer getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public Integer getExperimentId() {
        return experimentId;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setExperimentId(Integer experimentId) {
        this.experimentId = experimentId;
    }

    @Override
    public String toString() {
        return String.format("Directory{id=%d, path=%s, experimentId=%d}",
                this.id,
                this.path,
                this.experimentId);
    }

    public String to$Separated() {
        return String.format("%d$%s$%d",
                this.id,
                this.path,
                this.experimentId);
    }

    public static Directory string2Object(String $Separated) {
        System.out.println($Separated);
        String[] splits = $Separated.split("\\$");
        return new Directory(
                new Integer(splits[0]),
                splits[1],
                new Integer(splits[2]));
    }
}
