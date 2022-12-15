/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.brown.predictivebiology.utilities;

import edu.brown.predictivebiology.db.beans.Image;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A FileTraversal object simplifies going back and forth over a set of files.
 *
 * @author Haitham
 */
public class FileTraversal {

    // All the files in the traversal
    private List<Image> images;
    // Starting position of the index (before the first item)
    private int currentIndex = -1;

    /**
     * Creates an empty file traversal.
     */
    public FileTraversal() {
        images = new ArrayList<>();
    }
    
    /**
     * Create a new object and add all the files from the parameter list to your
     * internal list to avoid any disturbance that add and deleting from the
     * original (outer) list might cause.
     *
     * @param images images to be added to the traversal
     */
    public FileTraversal(List<Image> images) {
        this.images = new ArrayList<>();
        for (Image img : images) {
            this.images.add(img);
        }
    }

    /**
     * Are there any files before the current one.
     *
     * @return true if there is, false otherwise
     */
    public boolean hasPrevious() {
        return currentIndex > 0;
    }

    /**
     * Are there any files after the current one.
     *
     * @return true if there is, false otherwise
     */
    public boolean hasNext() {
        return currentIndex < images.size() - 1;
    }

    /**
     * Gets the next file or throws an InvalidStateTraversalException if the
     * traversal reached its upper end.
     *
     * @return next file
     */
    public Image next() {
        if (hasNext()) {
            return images.get(++currentIndex);
        } else {
            throw new InvalidStateTraversalException();
        }
    }

    /**
     * Gets the previous file or throws an InvalidStateTraversalException if the
     * traversal reached its lover end.
     *
     * @return previous file
     */
    public Image previous() {
        if (hasPrevious()) {
            return images.get(--currentIndex);
        } else {
            throw new InvalidStateTraversalException();
        }
    }

    /**
     * Gets the current file. Unlike next() and previous(), this method does not
     * update the value of currentIndex.
     *
     * @return current file
     */
    public Image getCurrent() {
        return images.get(currentIndex);
    }

    /**
     * Resets the current index of the traversal so that it can be used again.
     */
    public void resetIndex() {
        currentIndex = -1;
    }

    /**
     * Gets the number of files in the traversal.
     *
     * @return number of files in the traversal
     */
    public int size() {
        return images.size();
    }

    /**
     * Clear all the images of the traversal and reset its index.
     */
    public void clearTraversal() {
        images.clear();
        resetIndex();
    }

    /**
     * Appends more images to the traversal.
     *
     * @param moreImages a list of images
     */
    public void append(List<Image> moreImages) {
        images.addAll(moreImages);
    }

    /**
     * An object from this class represent the exception that happens when
     * trying to get the previous or next file from a traversal that has reached
     * either its lower or upper ends respectively.
     */
    public static class InvalidStateTraversalException
            extends RuntimeException {

        @Override
        public String getMessage() {
            return "Traversal limit reached";
        }
    }
}
