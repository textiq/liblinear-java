package de.bwaldvogel.liblinear;

import java.util.ArrayList;

/**
 * Abstract class for wrapping around an arraylist.
 */

public abstract class ListFactory implements AutoCloseable {

    public abstract ArrayList<FeatureVector> createList(int size);
}
