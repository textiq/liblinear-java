package de.bwaldvogel.liblinear;

import java.io.Serializable;

/**
 * @since 1.9
 */
public interface Feature extends Serializable {

    int getIndex();

    double getValue();

    void setValue(double value);
}
