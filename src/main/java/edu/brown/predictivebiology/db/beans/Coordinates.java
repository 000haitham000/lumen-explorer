/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.brown.predictivebiology.db.beans;

/**
 * An object of this class represents one row in the coordinates table in the
 * database.
 *
 * @author Haitham
 */
public class Coordinates {

    private Integer id;
    private Integer x;
    private Integer y;
    private Integer imageId;

    public Coordinates() {
    }

    public Coordinates(Integer id, Integer x, Integer y, Integer imageID) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.imageId = imageID;
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
     * @return the x
     */
    public Integer getX() {
        return x;
    }

    /**
     * @param x the x to set
     */
    public void setX(Integer x) {
        this.x = x;
    }

    /**
     * @return the y
     */
    public Integer getY() {
        return y;
    }

    /**
     * @param y the y to set
     */
    public void setY(Integer y) {
        this.y = y;
    }

    /**
     * @return the imageId
     */
    public Integer getImageId() {
        return imageId;
    }

    /**
     * @param imageId the imageId to set
     */
    public void setImageId(Integer imageId) {
        this.imageId = imageId;
    }

    @Override
    public String toString() {
        return String.format("CoordinatesRecord{id=%d, x=%d, y=%d, imageId=%d}",
                this.id,
                this.x,
                this.y,
                this.imageId);
    }

    public String to$Separated() {
        return String.format("%d$%d$%d$%d",
                this.id,
                this.x,
                this.y,
                this.imageId);
    }

    public static Coordinates string2Object(String $Separated) {
        String[] splits = $Separated.split("\\$");
        return new Coordinates(
                new Integer(splits[0]),
                new Integer(splits[1]),
                new Integer(splits[2]),
                new Integer(splits[3]));
    }
}
