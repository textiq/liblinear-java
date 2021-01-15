package de.bwaldvogel.liblinear;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;


/**
 * <p>Describes the problem</p>
 *
 * For example, if we have the following training data:
 * <pre>
 *  LABEL       ATTR1   ATTR2   ATTR3   ATTR4   ATTR5
 *  -----       -----   -----   -----   -----   -----
 *  1           0       0.1     0.2     0       0
 *  2           0       0.1     0.3    -1.2     0
 *  1           0.4     0       0       0       0
 *  2           0       0.1     0       1.4     0.5
 *  3          -0.1    -0.2     0.1     1.1     0.1
 *
 *  and bias = 1, then the components of problem are:
 *
 *  l = 5
 *  n = 6
 *
 *  y -&gt; 1 2 1 2 3
 *
 *  x -&gt; [ ] -&gt; (2,0.1) (3,0.2) (6,1) (-1,?)
 *       [ ] -&gt; (2,0.1) (3,0.3) (4,-1.2) (6,1) (-1,?)
 *       [ ] -&gt; (1,0.4) (6,1) (-1,?)
 *       [ ] -&gt; (2,0.1) (4,1.4) (5,0.5) (6,1) (-1,?)
 *       [ ] -&gt; (1,-0.1) (2,-0.2) (3,0.1) (4,1.1) (5,0.1) (6,1) (-1,?)
 * </pre>
 */
public class Problem<T extends ArrayList<FeatureVector>> {

    /** the number of training data */
    private int l;

    /** the number of features (including the bias feature if bias &gt;= 0) */
    public int n;

    private int size;

    /** an array containing the target values */
    public double[] y;

    /** array of sparse feature nodes */
    public T x;

    public Class<T> getClazz() {
        return clazz;
    }

    public Class<T> clazz;

    public Problem(Class<T> listclazz, int size) throws IllegalAccessException, InstantiationException {
        x = listclazz.newInstance();
        for (int i =0;i<size;i++){
            x.add(null);
        }
        this.clazz =listclazz;
        this.size = size;
    }

    public void reinitX(int size) throws IllegalAccessException, InstantiationException {
        x = clazz.newInstance();
        for (int i =0;i<size;i++){
            x.add(null);
        }
    }

    public void setL(int l){
        this.l = l;
    }
    public int getL(){
        return l;
    }

    public int getSize(){
        return size;
    }
    public void setX(int index, Feature[] features){
        x.set(index, new FeatureVector(features));
    }

    public Feature[] getX(int index){
        return x.get(index).getFeatures();
    }

    public Iterator<FeatureVector> getXIterator(){
        return x.iterator();
    }
    /**
     * If bias &gt;= 0, we assume that one additional feature is added
     * to the end of each data instance
     */
    public double bias = -1;

    /**
     * @deprecated use {@link Problem#readFromFile(Path, double)} instead
     */
    public static Problem readFromFile(Class<? extends ArrayList<FeatureVector>> clazz,
                                       File file, double bias) throws IOException,
          InvalidInputDataException, IllegalAccessException, InstantiationException {
        return readFromFile(clazz, file.toPath(), bias);
    }

    /**
     * see {@link Train#readProblem(Path, double)}
     */
    public static Problem readFromFile(Class<? extends ArrayList<FeatureVector>> clazz,
                                       Path path, double bias)
          throws IOException, InvalidInputDataException, InstantiationException, IllegalAccessException {
        return Train.readProblem(clazz, path, bias);
    }

    /**
     * @deprecated use {@link Problem#readFromFile(Path, Charset, double)} instead
     */
    public static Problem readFromFile(Class<? extends ArrayList<FeatureVector>> clazz,
                                       File file, Charset charset,
                                       double bias)
          throws IOException, InvalidInputDataException, InstantiationException, IllegalAccessException {
        return readFromFile(clazz, file.toPath(), charset, bias);
    }

    /**
     * see {@link Train#readProblem(Path, Charset, double)}
     */
    public static Problem readFromFile(Class<? extends ArrayList<FeatureVector>> clazz,
                                       Path path, Charset charset,
                                       double bias)
          throws IOException, InvalidInputDataException, IllegalAccessException, InstantiationException {
        return Train.readProblem(clazz, path, charset, bias);
    }

    /**
     * see {@link Train#readProblem(InputStream, double)}
     */
    public static Problem readFromStream(Class<? extends ArrayList<FeatureVector>> clazz,
                                         InputStream inputStream,
                                         double bias)
          throws IOException, InvalidInputDataException, IllegalAccessException, InstantiationException {
        return Train.readProblem(clazz, inputStream, bias);
    }

    /**
     * see {@link Train#readProblem(InputStream, Charset, double)}
     */
    public static Problem readFromStream(Class<? extends ArrayList<FeatureVector>> clazz,
                                         InputStream inputStream,
                                         Charset charset, double bias)
          throws IOException, InvalidInputDataException, InstantiationException, IllegalAccessException {
        return Train.readProblem(clazz, inputStream, charset, bias);
    }
}
