package de.bwaldvogel.liblinear;

import static de.bwaldvogel.liblinear.Linear.*;
import static de.bwaldvogel.liblinear.SolverType.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;


public class Train {

    public static void main(String[] args)
          throws IOException, InvalidInputDataException, IllegalAccessException, InstantiationException {
        new Train().run(args);
    }

    Class clazz = ArrayList.class;

    private double    bias             = 1;
    private boolean   find_parameters  = false;
    private boolean   C_specified      = false;
    private boolean   P_specified      = false;
    private boolean   solver_specified = false;
    private boolean   cross_validation = false;
    private String    inputFilename;
    private String    modelFilename;
    private int       nr_fold;
    private Parameter param            = null;
    private Problem   prob             = null;

    private void do_find_parameters() throws IllegalAccessException, InstantiationException {
        double start_C, start_p;
        if (C_specified)
            start_C = param.C;
        else
            start_C = -1.0;
        if (P_specified)
            start_p = param.p;
        else
            start_p = -1.0;

        System.out.printf("Doing parameter search with %d-fold cross validation.%n", nr_fold);
        ParameterSearchResult result = Linear.findParameters(clazz, prob, param, nr_fold, start_C, start_p);
        if (param.getSolverType() == L2R_LR || param.getSolverType() == L2R_L2LOSS_SVC)
            System.out.printf("Best C = %g  CV accuracy = %g%%\n", result.getBestC(), 100.0 * result.getBestScore());
        else if (param.getSolverType() == L2R_L2LOSS_SVR)
            System.out.printf("Best C = %g Best p = %g  CV MSE = %g\n", result.getBestC(), result.getBestP(), result.getBestScore());
    }

    private void do_cross_validation() throws IllegalAccessException, InstantiationException {

        double total_error = 0;
        double sumv = 0, sumy = 0, sumvv = 0, sumyy = 0, sumvy = 0;
        double[] target = new double[prob.getL()];

        long start, stop;
        start = System.currentTimeMillis();
        Linear.crossValidation(prob, param, nr_fold, target);
        stop = System.currentTimeMillis();
        System.out.println("time: " + (stop - start) + " ms");

        if (param.solverType.isSupportVectorRegression()) {
            for (int i = 0; i < prob.getL(); i++) {
                double y = prob.y[i];
                double v = target[i];
                total_error += (v - y) * (v - y);
                sumv += v;
                sumy += y;
                sumvv += v * v;
                sumyy += y * y;
                sumvy += v * y;
            }
            System.out.printf("Cross Validation Mean squared error = %g%n", total_error / prob.getL());
            System.out.printf("Cross Validation Squared correlation coefficient = %g%n", //
                ((prob.getL() * sumvy - sumv * sumy) * (prob.getL() * sumvy - sumv * sumy)) / ((prob.getL() * sumvv - sumv * sumv) * (prob.getL() * sumyy - sumy * sumy)));
        } else {
            int total_correct = 0;
            for (int i = 0; i < prob.getL(); i++)
                if (target[i] == prob.y[i])
                    ++total_correct;

            System.out.printf("correct: %d%n", total_correct);
            System.out.printf("Cross Validation Accuracy = %g%%%n", 100.0 * total_correct / prob.getL());
        }
    }

    private void exit_with_help() {
        System.out.printf("Usage: train [options] training_set_file [model_file]%n" //
            + "options:%n"
            + "-s type : set type of solver (default 1)%n"
            + "  for multi-class classification%n"
            + "        0 -- L2-regularized logistic regression (primal)%n"
            + "        1 -- L2-regularized L2-loss support vector classification (dual)%n"
            + "        2 -- L2-regularized L2-loss support vector classification (primal)%n"
            + "        3 -- L2-regularized L1-loss support vector classification (dual)%n"
            + "        4 -- support vector classification by Crammer and Singer%n"
            + "        5 -- L1-regularized L2-loss support vector classification%n"
            + "        6 -- L1-regularized logistic regression%n"
            + "        7 -- L2-regularized logistic regression (dual)%n"
            + "  for regression%n"
            + "       11 -- L2-regularized L2-loss support vector regression (primal)%n"
            + "       12 -- L2-regularized L2-loss support vector regression (dual)%n"
            + "       13 -- L2-regularized L1-loss support vector regression (dual)%n"
            + "  for outlier detection%n"
            + "       21 -- one-class support vector machine (dual)%n"
            + "-c cost : set the parameter C (default 1)%n"
            + "-p epsilon : set the epsilon in loss function of SVR (default 0.1)%n"
            + "-n nu : set the parameter nu of one-class SVM (default 0.5)%n"
            + "-e epsilon : set tolerance of termination criterion%n"
            + "       -s 0 and 2%n"
            + "               |f'(w)|_2 <= eps*min(pos,neg)/l*|f'(w0)|_2,%n"
            + "               where f is the primal function and pos/neg are # of%n"
            + "               positive/negative data (default 0.01)%n"
            + "       -s 11%n"
            + "               |f'(w)|_2 <= eps*|f'(w0)|_2 (default 0.0001)%n"
            + "       -s 1, 3, 4, 7, and 21%n"
            + "               Dual maximal violation <= eps; similar to libsvm (default 0.1 except 0.01 for -s 21)%n"
            + "      -s 5 and 6%n"
            + "               |f'(w)|_1 <= eps*min(pos,neg)/l*|f'(w0)|_1,%n"
            + "               where f is the primal function (default 0.01)%n"
            + "       -s 12 and 13%n"
            + "               |f'(alpha)|_1 <= eps |f'(alpha0)|,%n"
            + "               where f is the dual function (default 0.1)%n"
            + "-B bias : if bias >= 0, instance x becomes [x; bias]; if < 0, no bias term added (default -1)%n"
            + "-R : not regularize the bias; must with -B 1 to have the bias; DON'T use this unless you know what it is%n"
            + "       (for -s 0, 2, 5, 6, 11)%n"
            + "-wi weight: weights adjust the parameter C of different classes (see README for details)%n"
            + "-v n: n-fold cross validation mode%n"
            + "-C : find parameters (C for -s 0, 2 and C, p for -s 11)%n"
            + "-q : quiet mode (no outputs)%n");
        System.exit(1);
    }

    public Problem getProblem() {
        return prob;
    }

    public double getBias() {
        return bias;
    }

    public Parameter getParameter() {
        return param;
    }

    public void parse_command_line(String argv[]) {
        int i;

        // eps: see setting below
        param = new Parameter(L2R_L2LOSS_SVC_DUAL, 1, Double.POSITIVE_INFINITY, 0.1);
        // default values
        bias = -1;
        cross_validation = false;

        // parse options
        for (i = 0; i < argv.length; i++) {
            if (argv[i].charAt(0) != '-')
                break;
            if (++i >= argv.length)
                exit_with_help();
            switch (argv[i - 1].charAt(1)) {
                case 's':
                    param.solverType = SolverType.getById(atoi(argv[i]));
                    solver_specified = true;
                    break;
                case 'c':
                    param.setC(atof(argv[i]));
                    C_specified = true;
                    break;
                case 'p':
                    P_specified = true;
                    param.setP(atof(argv[i]));
                    break;
                case 'n':
                    param.nu = atof(argv[i]);
                    break;
                case 'e':
                    param.setEps(atof(argv[i]));
                    break;
                case 'B':
                    bias = atof(argv[i]);
                    break;
                case 'w':
                    int weightLabel = atoi(argv[i - 1].substring(2));
                    double weight = atof(argv[i]);
                    param.weightLabel = addToArray(param.weightLabel, weightLabel);
                    param.weight = addToArray(param.weight, weight);
                    break;
                case 'v':
                    cross_validation = true;
                    nr_fold = atoi(argv[i]);
                    if (nr_fold < 2) {
                        System.err.println("n-fold cross validation: n must >= 2");
                        exit_with_help();
                    }
                    break;
                case 'q':
                    i--;
                    Linear.disableDebugOutput();
                    break;
                case 'C':
                    find_parameters = true;
                    i--;
                    break;
                case 'R':
                    param.regularize_bias = false;
                    i--;
                    break;
                default:
                    System.err.println("unknown option");
                    exit_with_help();
            }
        }

        // determine filenames

        if (i >= argv.length)
            exit_with_help();

        inputFilename = argv[i];

        if (i < argv.length - 1)
            modelFilename = argv[i + 1];
        else {
            int p = argv[i].lastIndexOf('/');
            ++p; // whew...
            modelFilename = argv[i].substring(p) + ".model";
        }

        // default solver for parameter selection is L2R_L2LOSS_SVC
        if (find_parameters) {
            if (!cross_validation)
                nr_fold = 5;
            if (!solver_specified) {
                System.err.printf("Solver not specified. Using -s 2%n");
                param.setSolverType(L2R_L2LOSS_SVC);
            } else if (param.getSolverType() != L2R_LR && param.getSolverType() != L2R_L2LOSS_SVC && param.getSolverType() != L2R_L2LOSS_SVR) {
                System.err.printf("Warm-start parameter search only available for -s 0, -s 2 and -s 11%n");
                exit_with_help();
            }
        }

        if (param.eps == Double.POSITIVE_INFINITY) {
            switch (param.solverType) {
                case L2R_LR:
                case L2R_L2LOSS_SVC:
                    param.setEps(0.01);
                    break;
                case L2R_L2LOSS_SVR:
                    param.setEps(0.0001);
                    break;
                case L2R_L2LOSS_SVC_DUAL:
                case L2R_L1LOSS_SVC_DUAL:
                case MCSVM_CS:
                case L2R_LR_DUAL:
                    param.setEps(0.1);
                    break;
                case L1R_L2LOSS_SVC:
                case L1R_LR:
                    param.setEps(0.01);
                    break;
                case L2R_L1LOSS_SVR_DUAL:
                case L2R_L2LOSS_SVR_DUAL:
                    param.setEps(0.1);
                    break;
                case ONECLASS_SVM:
                    param.setEps(0.01);
                    break;
                default:
                    throw new IllegalStateException("unknown solver type: " + param.solverType);
            }
        }
    }

    /**
     * reads a problem from LibSVM format
     * @param file the SVM file
     * @throws IOException obviously in case of any I/O exception ;)
     * @throws InvalidInputDataException if the input file is not correctly formatted
     * @deprecated use {@link Train#readProblem(Path, double)} instead
     */
    public static Problem readProblem(Class<? extends ArrayList<FeatureVector>> clazz,
                                      File file, double bias)
          throws IOException, InvalidInputDataException, InstantiationException, IllegalAccessException {
        return readProblem(clazz, file.toPath(), bias);
    }

    /**
     * reads a problem from LibSVM format
     * @throws IOException obviously in case of any I/O exception ;)
     * @throws InvalidInputDataException if the input file is not correctly formatted
     */
    public static Problem<? extends ArrayList<FeatureVector>> readProblem(Class<? extends ArrayList<FeatureVector>> clazz,
                                      Path path, double bias)
          throws IOException, InvalidInputDataException, IllegalAccessException, InstantiationException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return readProblem(clazz,inputStream, bias);
        }
    }

    /**
     * @deprecated use {@link Train#readProblem(Path, Charset, double)} instead
     */
    public static Problem<? extends ArrayList<FeatureVector>> readProblem(Class<? extends ArrayList<FeatureVector>> clazz,
                                      File file, Charset charset,
                                      double bias)
          throws IOException, InvalidInputDataException, IllegalAccessException, InstantiationException {
        return readProblem(clazz, file.toPath(), charset, bias);
    }

    public static Problem<? extends ArrayList<FeatureVector>> readProblem(Class<? extends ArrayList<FeatureVector>> clazz,
                                      Path path, Charset charset,
                                      double bias)
          throws IOException, InvalidInputDataException, InstantiationException, IllegalAccessException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return readProblem(clazz, inputStream, charset, bias);
        }
    }

    public static Problem<? extends ArrayList<FeatureVector>> readProblem(Class<? extends ArrayList<FeatureVector>> clazz,
                                      InputStream inputStream,
                                      double bias)
          throws IOException, InvalidInputDataException, InstantiationException, IllegalAccessException {
        return readProblem(clazz, inputStream, Charset.defaultCharset(), bias);
    }

    public static Problem<? extends ArrayList<FeatureVector>> readProblem(Class<? extends ArrayList<FeatureVector>> clazz,
                                      InputStream inputStream,
                                      Charset charset, double bias)
          throws IOException, InvalidInputDataException, IllegalAccessException, InstantiationException {
        BufferedReader fp = new BufferedReader(new InputStreamReader(inputStream, charset));
        List<Double> vy = new ArrayList<>();
        List<Feature[]> vx = new ArrayList<>();
        int max_index = 0;

        int lineNr = 0;

        while (true) {
            String line = fp.readLine();
            if (line == null)
                break;
            lineNr++;

            StringTokenizer st = new StringTokenizer(line, " \t\n\r\f:");
            String token;
            try {
                token = st.nextToken();
            } catch (NoSuchElementException e) {
                throw new InvalidInputDataException("empty line", lineNr, e);
            }

            try {
                vy.add(atof(token));
            } catch (NumberFormatException e) {
                throw new InvalidInputDataException("invalid label: " + token, lineNr, e);
            }

            int m = st.countTokens() / 2;
            Feature[] x;
            if (bias >= 0) {
                x = new Feature[m + 1];
            } else {
                x = new Feature[m];
            }
            int indexBefore = 0;
            for (int j = 0; j < m; j++) {

                token = st.nextToken();
                int index;
                try {
                    index = atoi(token);
                } catch (NumberFormatException e) {
                    throw new InvalidInputDataException("invalid index: " + token, lineNr, e);
                }

                // assert that indices are valid and sorted
                if (index <= 0)
                    throw new InvalidInputDataException("invalid index: " + index, lineNr);
                if (index <= indexBefore)
                    throw new InvalidInputDataException("indices must be sorted in ascending order", lineNr);
                indexBefore = index;

                token = st.nextToken();
                try {
                    double value = atof(token);
                    x[j] = new FeatureNode(index, value);
                } catch (NumberFormatException e) {
                    throw new InvalidInputDataException("invalid value: " + token, lineNr);
                }
            }
            if (m > 0) {
                max_index = Math.max(max_index, x[m - 1].getIndex());
            }

            vx.add(x);
        }

        return constructProblem(clazz, vy, vx, max_index, bias);
    }

    public void readProblem(Class<? extends ArrayList<FeatureVector>> clazz, Path path) throws IOException,
          InvalidInputDataException, InstantiationException, IllegalAccessException {
        prob = Train.readProblem(clazz, path, bias);
    }

    public void readProblem(Class<? extends ArrayList<FeatureVector>> clazz, String filename) throws IOException,
          InvalidInputDataException, IllegalAccessException, InstantiationException {
        readProblem(clazz, filename, bias);
    }

    public void readProblem(Class<? extends ArrayList<FeatureVector>> clazz, String filename, double bias)
          throws IOException, InvalidInputDataException, InstantiationException, IllegalAccessException {
        prob = Train.readProblem(clazz, Paths.get(filename), bias);
    }

    private static int[] addToArray(int[] array, int newElement) {
        int length = array != null ? array.length : 0;
        int[] newArray = new int[length + 1];
        if (array != null && length > 0) {
            System.arraycopy(array, 0, newArray, 0, length);
        }
        newArray[length] = newElement;
        return newArray;
    }

    private static double[] addToArray(double[] array, double newElement) {
        int length = array != null ? array.length : 0;
        double[] newArray = new double[length + 1];
        if (array != null && length > 0) {
            System.arraycopy(array, 0, newArray, 0, length);
        }
        newArray[length] = newElement;
        return newArray;
    }

    private static Problem<? extends ArrayList<FeatureVector>> constructProblem(Class<? extends ArrayList<FeatureVector>> clazz,
                                            List<Double> vy,
                                            List<Feature[]> vx, int max_index, double bias)
          throws InstantiationException, IllegalAccessException {
        Problem<? extends ArrayList<FeatureVector>> prob = new Problem<>(clazz, vy.size());
        prob.bias = bias;
        prob.n = max_index;
        prob.setL(vy.size());
        if (bias >= 0) {
            prob.n++;
        }
        for (int i = 0; i < prob.getL(); i++) {
            prob.setX(i,vx.get(i));

            if (bias >= 0) {
                assert prob.getX(i)[prob.getX(i).length - 1] == null;
                prob.getX(i)[prob.getX(i).length - 1] = new FeatureNode(max_index + 1, bias);
            }
        }

        prob.y = new double[prob.getL()];
        for (int i = 0; i < prob.getL(); i++)
            prob.y[i] = vy.get(i).doubleValue();

        return prob;
    }

    private void run(String[] args)
          throws IOException, InvalidInputDataException, InstantiationException, IllegalAccessException {
        parse_command_line(args);
        Class clazz = ArrayList.class;
        readProblem(clazz, inputFilename);
        if (find_parameters) {
            do_find_parameters();
        } else if (cross_validation)
            do_cross_validation();
        else {
            Model model = Linear.train(prob, param);
            Linear.saveModel(Paths.get(modelFilename), model);
        }
    }

    boolean isFindParameters() {
        return find_parameters;
    }

    int getNumFolds() {
        return nr_fold;
    }
}
