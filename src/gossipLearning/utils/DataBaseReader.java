package gossipLearning.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

/**
 * This class reads and stores the training and the evaluation sets for training machine learning algorithms 
 * and is represented as a singleton. The required files are the training and the evaluation file names with paths. <br/>
 * The files should have Joachims' SVMLight format.
 * 
 * @author István Hegedűs
 *
 */
public class DataBaseReader {
  
  /** @hidden */
  private InstanceHolder trainingSet;
  /** @hidden */
  private InstanceHolder evalSet;
  
  private int numberOfClasses;
  private int numberOfFeatures;
  
  /** @hidden */
  private SparseVector means;
  /** @hidden */
  private SparseVector devs;
  private boolean isStandardized;
  
  /** @hidden */
  private SparseVector mins;
  /** @hidden */
  private SparseVector maxs;
  private boolean isNormalized;
  
  protected DataBaseReader(final InputStream tFile, final InputStream eFile) throws IOException{
    means = new SparseVector();
    devs = new SparseVector();
    mins = new SparseVector();
    maxs = new SparseVector();
    isStandardized = false;
    isNormalized = false;
    
    // reading training file
    trainingSet = parseFile(tFile);
    
    // compute means and standard deviations on training set for standardization
    for (int i = 0; i < trainingSet.size(); i++) {
      SparseVector instance = trainingSet.getInstance(i);
      SparseVector clone = new SparseVector(instance);
      clone.pointMul(instance);
      means.add(instance);
      devs.add(clone);
      for (VectorEntry e : instance) {
        mins.put(e.index, Math.min(mins.get(e.index), e.value));
        maxs.put(e.index, Math.max(maxs.get(e.index), e.value));
      }
    }
    means.mul(1.0 / (double)trainingSet.size());
    devs.mul(1.0 / (double)trainingSet.size());
    SparseVector m2 = new SparseVector(means);
    m2.powerTo(2.0);
    devs.add(m2, -1.0);
    //devs.sqrt();
    devs.powerTo(0.5);
    
    // reading evaluation file
    evalSet = parseFile(eFile);
    
    // set the correct number of features and classes for both sets
    numberOfFeatures = Math.max(trainingSet.getNumberOfFeatures(), evalSet.getNumberOfFeatures());
    numberOfClasses = Math.max(trainingSet.getNumberOfClasses(), evalSet.getNumberOfClasses());
    trainingSet = new InstanceHolder(trainingSet.getInstances(), trainingSet.getLabels(), numberOfClasses, numberOfFeatures);
    evalSet = new InstanceHolder(evalSet.getInstances(), evalSet.getLabels(), numberOfClasses, numberOfFeatures);
  }
  
  /**
   * This method parses the given file into collections of instances and corresponding class labels.
   * @param file the file that has to be parsed
   * @throws IOException if file reading error occurs.
   */
  protected InstanceHolder parseFile(final InputStream file) throws IOException{
    Vector<SparseVector> instances = new Vector<SparseVector>();
    Vector<Double> labels = new Vector<Double>();
    BufferedReader br = new BufferedReader(new InputStreamReader(file, "utf8"));
    int numberOfClasses = -1;
    int numberOfFeatures = -1;
    Set<Double> classes = new TreeSet<Double>();
    String line;
    String[] split;
    int c = 0;
    double label;
    int key;
    int prevKey;
    double value;
    int[] indices;
    double[] values;
    int size;
    while ((line = br.readLine()) != null){
      try {
      c++;
      // eliminating empty and comment lines
      if (line.length() == 0 || line.startsWith("#")){
        continue;
      }
      // eliminating comments and white spaces from the endings of the line
      int charIndex = line.indexOf("#");
      line = line.substring(0, charIndex == -1 ? line.length() : charIndex).trim();
      // splitting line at white spaces and at colons
      split = line.split(":|\\s");
      // throwing exception if the line is invalid (= has even number of tokens, since 
      // a valid line has a class label and pairs of indices and corresponding values)
      if (split.length % 2 != 1){
        throw new RuntimeException("The file \"" + file.toString() + "\" has invalid structure at line " + c);
      }
      label = Double.parseDouble(split[0]);
      indices = new int[split.length >>> 1];
      values = new double[split.length >>> 1];
      size = 0;
      prevKey = -1;
      for (int i = 1; i < split.length; i += 2){
        // index from 0
        key = Integer.parseInt(split[i]) - 1;
        if (key < 0){
          throw new RuntimeException("The index of the features must be positive integer, line " + c);
        }
        if (key <= prevKey) {
          throw new RuntimeException("Unexpected key (" + (key+1) + ") at line " + c);
        }
        if (key > numberOfFeatures) {
          numberOfFeatures = key;
        }
        value = Double.parseDouble(split[i + 1]);
        indices[size] = key;
        values[size] = value;
        size ++;
        prevKey = key;
      }
      // storing parsed instance
      instances.add(new SparseVector(indices, values));
      labels.add(label);
      
      // calculating the number of classes if it is not a regression
      if (numberOfClasses != Integer.MAX_VALUE) {
        classes.add(label);
      }
      } catch (Exception e) {
        throw new RuntimeException("Exceprion was caught while parsing file " + file.toString() + " at line " + c, e);
      }
    }
    br.close();
    
    // sets the correct value of number of classes
    if (numberOfClasses != Integer.MAX_VALUE) {
      numberOfClasses = classes.size();
    }
    
    return new InstanceHolder(instances, labels, (numberOfClasses == 1) ? 0 : numberOfClasses, numberOfFeatures + 1); // 1-> indicating clustering
  }
  
  /**
   * Returns a collection of the instances and corresponding labels of the parsed training set.
   * @return training set.
   */
  public InstanceHolder getTrainingSet() {
    return trainingSet;
  }
  
  /**
   * Returns a collection of the instances and the corresponding labels of the parsed evaluation set.
   * @return evaluation set.
   */
  public InstanceHolder getEvalSet() {
    return evalSet;
  }
  
  /**
   * Standardizes the training and test data sets based on the training data.
   */
  public void standardize() {
    if (isStandardized || isNormalized) {
      return;
    }
    isStandardized = true;
    for (int i = 0; i < trainingSet.size(); i++) {
      trainingSet.getInstance(i).add(means, -1.0).div(devs);
    }
    for (int i = 0; i < evalSet.size(); i++) {
      evalSet.getInstance(i).add(means, -1.0).div(devs);
    }
  }
  
  /**
   * Normalizes the training and test data sets based on the training data.
   */
  public void normalize() {
    if (isNormalized || isStandardized) {
      return;
    }
    isNormalized = true;
    SparseVector maxmin = new SparseVector(maxs);
    maxmin.add(mins, -1.0);
    for (int i = 0; i < trainingSet.size(); i++) {
      // [-1,1] (2x - min - max) /(max - min)
      //trainingSet.getInstance(i).mul(2.0).add(mins, -1.0).add(maxs, -1.0).div(maxmin);
      // [0,1] (x - min) /(max - min)
      trainingSet.getInstance(i).add(mins, -1.0).div(maxmin);
    }
    for (int i = 0; i < evalSet.size(); i++) {
      // [-1,1]
      //evalSet.getInstance(i).mul(2.0).add(mins, -1.0).add(maxs, -1.0).div(maxmin);
      // [0,1]
      evalSet.getInstance(i).add(mins, -1.0).div(maxmin);
    }
  }
  
  public void perturbe() {
    
  }
  
  /**
   * Transform the training and evaluation databases applying an at most n-degree polynomial radial
   * basis function I.e. when n=2 and the original database contains two dimensions (x,y), the mapping 
   * produce a new database containing five dimensions (x,y,x^2,xy,y^2). 
   */
  public void polynomize(int n) {
    polynomize(n, true);
  }
  
  private void polynomize(int n, boolean generateAll) {
    Vector<Vector<Integer>> mapping = Utils.polyGen(numberOfFeatures, n, generateAll);
    trainingSet = convert(trainingSet, mapping);
    evalSet = convert(evalSet, mapping);    
  }
  
  private InstanceHolder convert(InstanceHolder origSet, Vector<Vector<Integer>> mapping) {
    // create the new instance set
    InstanceHolder newSet = new InstanceHolder(origSet.getNumberOfClasses(), mapping.size());
    
    for (int i = 0; i < origSet.size(); i++) {
      // get original instance and create mapped one
      SparseVector origInstance = origSet.getInstance(i);
      SparseVector newInstance = new SparseVector(mapping.size());
      
      // for each new dimension
      for (int j = 0; j < mapping.size(); j++) {
        // perform mapping based on the original values
        double newValue = 1.0;
        for (int k = 0; k < mapping.get(j).size(); k ++) {
          newValue *= origInstance.get(mapping.get(j).get(k));
        }
        // store new value of dimension j
        newInstance.put(j, newValue);
      }
      
      // store mapped instance
      newSet.add(newInstance, origSet.getLabel(i));
    }
    
    // return new instance set
    return newSet;
  }
  
  /**
   * Writes the stored data sets to the specified files.
   * @param trainFile writes the training set to this file
   * @param testFile writes the evaluation set to this file
   * @throws IOException if file write error occurs.
   */
  public void writeToFile(File trainFile, File testFile) throws IOException{
    trainingSet.writeToFile(trainFile);
    evalSet.writeToFile(testFile);
  }
  
  /**
   * Returns the String representation of the class.
   */
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("#training_set: " + DataBaseReader.tFile + "\n");
    sb.append(trainingSet.toString());
    sb.append("#evaluation_set: " + DataBaseReader.eFile + "\n");
    sb.append(evalSet.toString());
    return sb.toString();
  }
  
  private static DataBaseReader instance = null;
  private static InputStream tFile = null;
  private static InputStream eFile = null;
  
  /**
   * Creates and returns a DataBaseReader object that contains the training and the evaluation sets. 
   * Based on the parameter files that should have Jochaims's SVMLight format.
   * 
   * @param className the canonical class name of the reader class
   * @param tFile the training file
   * @param eFile the evaluation file
   * @return An instance of this class
   * @throws IOException if file reading error occurs.
   */
  @SuppressWarnings("unchecked")
  public static DataBaseReader createDataBaseReader(String className, final InputStream tFile, final InputStream eFile) throws Exception {
    if (instance == null || !instance.getClass().getCanonicalName().equals(className) || !tFile.equals(DataBaseReader.tFile) || !eFile.equals(DataBaseReader.eFile)) {
      DataBaseReader.tFile = tFile;
      DataBaseReader.eFile = eFile;
      Class<? extends DataBaseReader> dataBaseReaderClass = (Class<? extends DataBaseReader>) Class.forName(className);
      Constructor<? extends DataBaseReader> dbrConst = dataBaseReaderClass.getDeclaredConstructor(InputStream.class, InputStream.class);
      DataBaseReader.instance = dbrConst.newInstance(tFile, eFile);
    }
    return instance;
  }
  
}
