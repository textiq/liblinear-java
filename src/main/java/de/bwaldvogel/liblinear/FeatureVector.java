package de.bwaldvogel.liblinear;

import java.io.Serializable;

/**
 * Wrapper around feature array.
 */

public class FeatureVector implements Serializable {
    public Feature[] getFeatures() {
        return features;
    }

    private final Feature[] features;

    public FeatureVector(Feature[] features) {
        this.features = features;
    }

}
