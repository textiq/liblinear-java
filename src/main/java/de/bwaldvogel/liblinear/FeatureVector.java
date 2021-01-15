package de.bwaldvogel.liblinear;

/**
 * TODO: add javadoc.
 */

public class FeatureVector {
    public Feature[] getFeatures() {
        return features;
    }

    private final Feature[] features;
    public FeatureVector(Feature[] features){
        this.features = features;
    }

}
