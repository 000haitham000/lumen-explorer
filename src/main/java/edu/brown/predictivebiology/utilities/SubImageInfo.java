/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.brown.predictivebiology.utilities;

/**
 *
 * @author Haitham
 */
public class SubImageInfo {

    private final String subImagePath;
    private final double[][] body;
    private final double[][] boundary;
    private final double[][] corners;

    public SubImageInfo(
            String subImagePath, 
            double[][] body, 
            double[][] boundary, 
            double[][] corners) {
        this.subImagePath = subImagePath;
        this.body = body;
        this.boundary = boundary;
        this.corners = corners;
    }

    public String getSubImagePath() {
        return subImagePath;
    }

    public double[][] getBody() {
        return body;
    }

    public double[][] getBoundary() {
        return boundary;
    }

    public double[][] getCorners() {
        return corners;
    }
}
