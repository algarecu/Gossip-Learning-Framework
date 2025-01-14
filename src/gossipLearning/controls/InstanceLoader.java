package gossipLearning.controls;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedList;

import gossipLearning.protocols.LearningProtocol;
import gossipLearning.utils.AggregationResult;
import gossipLearning.utils.DataBaseReader;
import gossipLearning.utils.InstanceHolder;
import gossipLearning.utils.SparseVector;
import gossipLearning.utils.Utils;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.core.Protocol;

/**
 * This control reads the training and evaluation sets from files and stores them.
 * The format of the files should be the Joachims' file format. <br/>
 * Moreover, this control loads the training instances onto the nodes, and specifies the
 * evaluation set for the error observer. <br/>
 * The number of training instances per node can be parameterized, the default
 * value is 1.
 * <br/><br/>
 * Required configuration parameters:<br/>
 * <ul>
 * <li>extractionProtocol - the extraction protocol</li>
 * <li>learningProtocols - the learning protocols, separated by comma</li>
 * <li>readerClass - the database reader class name</li>
 * <li>trainingFile - the name of the training file</li>
 * <li>evaluationFile - the name of the evaluation file</li>
 * <li>samplesPerNode - the number of loaded samples per nodes</li>
 * <li>printPrecision - the number of floating points of the evaluation metric results</li>
 * <li>isPrintAges - the age of the model is printed or not</li>
 * </ul>
 * @author Róbert Ormándi
 *
 * @navassoc - - - ExtractionProtocol
 * @navassoc - - - LearningProtocol
 */
public class InstanceLoader implements Control {
  private static final String PAR_PROTLS = "learningProtocols";
  private static final String PAR_READERCLASS = "readerClass";
  private static final String PAR_TFILE = "trainingFile";
  private static final String PAR_EFILE = "evaluationFile";
  private static final String PAR_NORMALIZATION = "normalization";
  private static final String PAR_PRINTPRECISION = "printPrecision";
  private static final String PAR_ISPRINTAGES = "isPrintAges";
  private static final String PAR_CLABELS = "cLabels";
  private static final String PAR_TIMES = "times";
  
  /** The array of protocol ID(s) of the learning protocol(s).*/
  protected final int[] pidLS;
  /** @hidden */
  protected final File tFile;
  /** @hidden */
  protected String readerClassName;
  protected DataBaseReader reader;
  /** @hidden */
  protected final File eFile;
  /** Specifies the type of instance normalization.
   * <ul>
   * <li>1: normalization, [0-1]</li>
   * <li>2: standardization, 0 mean, 1 std</li>
   * </ul>
   * */
  protected final int normalization;
  /** Number of different labels per node.*/
  protected final int cLabels;
  protected final double times;
  protected InstanceHolder[] instances;
    
  /**
   * Reads the parameters from the configuration file based on the specified prefix.
   * @param prefix prefix of parameters of this class
   */
  public InstanceLoader(String prefix) throws Exception {
    String[] pidLSS = Configuration.getString(prefix + "." + PAR_PROTLS).split(",");
    pidLS = new int[pidLSS.length];
    for (int i = 0; i < pidLSS.length; i++) {
      pidLS[i] = Configuration.lookupPid(pidLSS[i]);
    }
    tFile = new File(Configuration.getString(prefix + "." + PAR_TFILE));
    eFile = new File(Configuration.getString(prefix + "." + PAR_EFILE));
    readerClassName = Configuration.getString(prefix + "." + PAR_READERCLASS);
    AggregationResult.printPrecision = Configuration.getInt(prefix + "." + PAR_PRINTPRECISION);
    AggregationResult.isPrintAges = Configuration.getBoolean(prefix + "." + PAR_ISPRINTAGES, false);
    normalization = Configuration.getInt(prefix + "." + PAR_NORMALIZATION, 0);
    cLabels = Configuration.getInt(prefix + "." + PAR_CLABELS, 0);
    times = Configuration.getDouble(prefix + "." + PAR_TIMES, 1.0);
  }
  
  public boolean execute(){
    try {
      // read instances
      System.err.println("Reading data set.");
      System.err.println("\ttraining file: " + tFile);
      System.err.println("\tevaluation file: " + eFile);
      reader = DataBaseReader.createDataBaseReader(readerClassName, new FileInputStream(tFile), new FileInputStream(eFile));
      System.err.println("\tsize: " + reader.getTrainingSet().size() + ", " + reader.getEvalSet().size() + " x " + reader.getTrainingSet().getNumberOfFeatures());
      if (normalization == 1) {
        System.err.println("|--WARNING: feature values will be normalized into the [0-1] interval");
        reader.normalize();
      } else if (normalization == 2) {
        System.err.println("|--WARNING: feature values will be standardized (have 0 mean and 1 standard deviation)");
        reader.standardize();
      }
      if (reader.getTrainingSet().size() < Network.size()) {
        System.err.println("|--WARNING: training set size (" + reader.getTrainingSet().size() + ") is less then the network size (" + Network.size() + ")");
      }
      
      // shuffle training set
      int[] indices = new int[reader.getTrainingSet().size()];
      for (int i = 0; i < indices.length; i++) {
        indices[i] = i;
      }
      
      // bias instance distribution
      int k = reader.getTrainingSet().getNumberOfClasses();
      int n = Network.size();
      LinkedList<Integer>[] map = Utils.mapLabelsToNodes(k, n, cLabels);
      if (map != null) {
        Utils.arrayShuffle(CommonState.r, indices);
      }
      /*for (int i = 0; i < k; i++) {
        System.out.println(i + "\t" + map[i]);
      }
      System.exit(0);*/
      
      // init the nodes by adding the instances read before
      instances = new InstanceHolder[Network.size()];
      for (int i = 0; i < reader.getTrainingSet().size() * times; i++) {
        SparseVector instance = reader.getTrainingSet().getInstance(indices[i % reader.getTrainingSet().size()]);
        double label = reader.getTrainingSet().getLabel(indices[i % reader.getTrainingSet().size()]);
        int nodeIdx = i % Network.size();
        if (map != null) {
          nodeIdx = map[(int)label].poll();
          map[(int)label].add(nodeIdx);
        }
        if (instances[nodeIdx] == null) {
          instances[nodeIdx] = new InstanceHolder(reader.getTrainingSet().getNumberOfClasses(), reader.getTrainingSet().getNumberOfFeatures());
        }
        instances[nodeIdx].add(instance, label);
        if (i % reader.getTrainingSet().size() == reader.getTrainingSet().size() - 1) {
          Utils.arrayShuffle(CommonState.r, indices);
        }
      }
      // sets the number of classes for the learning protocols and the evaluation set for the evaluator.
      for (int i = 0; i < Network.size(); i++) {
        Node node = Network.get(i);
        //Protocol protocol = node.getProtocol(pidE);
        for (int j = 0; j < pidLS.length; j++) {
          Protocol protocol = node.getProtocol(pidLS[j]);
          if (protocol instanceof LearningProtocol) {
            LearningProtocol learningProtocol = (LearningProtocol) protocol;
            learningProtocol.setInstanceHolder(instances[i]);
            learningProtocol.getResults().setEvalSet(reader.getEvalSet());
            learningProtocol.setParameters(reader.getTrainingSet().getNumberOfClasses(), reader.getTrainingSet().getNumberOfFeatures());
          } else {
            throw new RuntimeException("The protocol " + pidLS[j] + " has to implement the LearningProtocol interface!");
          }
        }
      }
    } catch (Exception ex) {
      throw new RuntimeException("Exception has occurred in InstanceLoader!", ex);
    }
    
    return false;
  }

}
