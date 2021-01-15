package de.bwaldvogel.liblinear;

import java.util.ArrayList;

/**
 * A list-factory which produces in-memory lists.
 */

public class MemoryListFactory extends ListFactory {
    @Override
    public ArrayList<FeatureVector> createList(int size) {
        return new ArrayList<>(size);
    }

    @Override
    public void close() {
        //NOOP
    }

}
