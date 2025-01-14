package gossipLearning.models.learning.multiclass;

import gossipLearning.interfaces.Function;
import gossipLearning.interfaces.functions.Sigmoid;
import gossipLearning.interfaces.functions.SigmoidGradient;
import gossipLearning.interfaces.models.Model;
import gossipLearning.interfaces.models.ProbabilityModel;
import gossipLearning.utils.Matrix;
import gossipLearning.utils.SparseVector;

import java.util.Arrays;
import java.util.Random;

import peersim.config.Configuration;
import peersim.core.CommonState;

/**
 * <b>Update rule: </b>
 * <ul>
 * <li>delta for output layer: d^(o) = a^(o) - Y </li>
 * <li>delta for layer (i): d^i = T^(i) * d^(i+1) .* g'(z^(i)) </li>
 * <li>gradient (i): D^(i) = d^(i+1) * a^(i) </li>
 * <li>update (i): T^(i) = T^(i) - D^(i) </li>
 * </ul>
 * <b>where</b>
 * <ul>
 * <li>T^(i): parameter matrix of layer i </li>
 * <li>a^(i): result of layer i </li>
 * <li>z^(i): result of layer i, without applying activation function </li>
 * <ul>
 * <li> z^(i) = a^(i-1) * T^(i), if i > 0</li>
 * <li> z^(0) = X * T^(0), for the input layer</li>
 * </ul>
 * <li>g'(): gradient function</li>
 * </ul>
 * 
 * @author István Hegedűs
 */
public class ANN extends ProbabilityModel {
  private static final long serialVersionUID = 5187257180709173833L;
  protected static final String PAR_HIDDEN = "hiddenLayers";
  protected static final String PAR_ACTF = "activationFunction";
  protected static final String PAR_GRADF = "gradientFunction";
  protected static final String PAR_SEED = "seed";
  
  protected final Function fAct;
  protected final Function fGrad;
  protected final Function sigmoid;
  protected final Function sigmoidGrad;

  /** parameter matrices of the layers */
  protected Matrix[] thetas;
  protected Matrix[] gradients;
  /** output of the layers without applying activation function */
  protected Matrix[] products;
  protected Matrix[] activations;
  /** size of the layers including the number of features + 1 (first)
   * and the number of classes (last)*/
  protected int[] layersSizes;
  protected final long seed;
  protected int numParams = 0;

  /**
   * This constructor is for initializing the member variables of the Model.
   * 
   * @param prefix The ID of the parameters contained in the Peersim configuration file.
   */
  public ANN(String prefix) {
    super(prefix);
    String layers = Configuration.getString(prefix + "." + PAR_HIDDEN, null);
    try {
      fAct = (Function)Class.forName(Configuration.getString(prefix + "." + PAR_ACTF)).newInstance();
      fGrad = (Function)Class.forName(Configuration.getString(prefix + "." + PAR_GRADF)).newInstance();
    } catch (Exception e) {
      throw new RuntimeException("Exception occured in initialization of " + getClass().getCanonicalName() + ": ", e);
    }
    sigmoid = new Sigmoid();
    sigmoidGrad = new SigmoidGradient();
    seed = Configuration.getLong(prefix + "." + PAR_SEED, 1234567890);
    String[] layersSizes = null;
    int numLayers = layers == null ? 0 : (layersSizes = layers.split(",")).length;
    thetas = new Matrix[numLayers + 1];
    gradients = new Matrix[numLayers + 1];
    products = new Matrix[numLayers + 1];
    activations = new Matrix[numLayers + 1];
    // first is numOfFeatures + 1, last is numOfClasses
    this.layersSizes = new int[numLayers + 2];
    for (int i = 0; i < numLayers; i++) {
      // plus 1 for the bias
      this.layersSizes[i + 1] = Integer.parseInt(layersSizes[i]) + 1;
    }
  }

  public ANN(ANN a) {
    super(a);
    fAct = a.fAct;
    fGrad = a.fGrad;
    sigmoid = a.sigmoid;
    sigmoidGrad = a.sigmoidGrad;
    seed = a.seed;
    if (a.thetas != null) {
      thetas = new Matrix[a.thetas.length];
      gradients = new Matrix[a.thetas.length];
      products = new Matrix[a.products.length];
      activations = new Matrix[a.activations.length];
      for (int i = 0; i < a.thetas.length && a.thetas[i] != null; i++) {
        thetas[i] = a.thetas[i].clone();
        gradients[i] = a.gradients[i].clone();
      }
      for (int i = 0; i < a.products.length && a.products[i] != null; i++) {
        products[i] = a.products[i].clone();
      }
      for (int i = 0; i < a.activations.length && a.activations[i] != null; i++) {
        activations[i] = a.activations[i].clone();
      }
    }
    if (a.layersSizes != null) {
      layersSizes = Arrays.copyOf(a.layersSizes, a.layersSizes.length);
    }
    numParams = a.numParams;
  }

  @Override
  public ANN clone() {
    return new ANN(this);
  }

  @Override
  public double[] distributionForInstance(SparseVector instance) {
    Matrix predicted = evaluate(instance);
    if (predicted != null) {
      for (int i = 0; i < numberOfClasses; i++) {
        distribution[i] = predicted.get(0, i);
      }
    }
    return distribution;
  }

  @Override
  public void update(SparseVector instance, double label) {
    age ++;
    double lr = eta / (isTime == 1 ? age : 1.0);
    
    // evaluate instance
    Matrix predicted = evaluate(instance);
    if (predicted == null) {
      return;
    }
    // delta for computing gradient
    Matrix deriv = products[thetas.length - 1].apply(sigmoidGrad);
    Matrix act = activations[thetas.length - 1];
    Matrix delta = new Matrix(predicted).mulEquals(0.0);
    for (int i = 0; i < delta.getNumberOfColumns(); i++) {
      double a = Math.max(Math.min(act.get(0, i), 1.0 - 1E-7), 1E-7);
      double d = deriv.get(0, i);
      delta.set(0, i, i != label ? d / (1.0 - a) : d/ -a);
    }
    
    // hidden layers
    for (int i = thetas.length - 1; i > 0; i--) {
      Function fg = fGrad;
      if (i == thetas.length - 1) {
        fg = sigmoidGrad;
      }
      // avoiding bias regularization
      gradients[i].setMatrix(thetas[i]).mulEquals(0, layersSizes[i] - 2, 0, layersSizes[i + 1] - 1, lambda);
      
      //gradients[i].mulAdd(products[i - 1].apply(fa).transpose(), delta);
      gradients[i].mulAdd(activations[i - 1].transpose(), delta);
      activations[i - 1].transpose();
      // next delta
      delta = thetas[i].mul(delta.transpose()).transpose().pointMulEquals(products[i - 1].applyEquals(fg));
      // bias scaling (not necessary)
      gradients[i].mulEquals(layersSizes[i] - 1, layersSizes[i] - 1, 0, layersSizes[i + 1] - 1, lambda);
      // update
      thetas[i].addEquals(gradients[i], -lr);
    }

    // input layer
    // avoiding bias regularization
    gradients[0].setMatrix(thetas[0]).mulEquals(0, layersSizes[0] - 2, 0, layersSizes[0 + 1] - 1, lambda);
    // for bias
    instance.add(layersSizes[0] - 1, 1.0);
    gradients[0].addEquals(new Matrix(instance, delta.getRow(0), layersSizes[0]));
    instance.add(layersSizes[0] - 1, -1.0);
    // bias scaling (not necessary)
    gradients[0].mulEquals(layersSizes[0] - 1, layersSizes[0] - 1, 0, layersSizes[0 + 1] - 1, lambda);
    // update
    thetas[0].addEquals(gradients[0], -lr);
  }
  
  private Matrix evaluate(SparseVector instance) {
    if (thetas[0] == null || layersSizes[0] == 0) {
      return null;
    }
    
    // input layer
    // for bias
    instance.add(layersSizes[0] - 1, 1.0);
    products[0].setMatrix(thetas[0].mulLeft(instance));
    instance.add(layersSizes[0] - 1, -1.0);
    // apply activation function
    activations[0].setMatrix(products[0]).applyEquals(fAct);
    
    // hidden layers
    for (int i = 1; i < thetas.length; i++) {
      // last value is for adding bias
      activations[i - 1].set(0, layersSizes[i] - 1, 1.0);
      
      products[i].mulSet(activations[i - 1], thetas[i]);
      if (i == thetas.length - 1) {
        activations[i].setMatrix(products[i]).applyEquals(sigmoid);
      } else {
        activations[i].setMatrix(products[i]).applyEquals(fAct);
      }
    }
    return activations[activations.length - 1];
  }
  
  @Override
  public void setParameters(int numberOfClasses, int numberOfFeatures) {
    super.setParameters(numberOfClasses, numberOfFeatures);
    // the instance is a row vector (1Xd) plus 1 for the bias
    layersSizes[0] = numberOfFeatures + 1;
    layersSizes[layersSizes.length - 1] = numberOfClasses;
    
    // thetas are initialized uniform randomly from [-scale : scale] (ML-Class)
    Random r = new Random(seed);
    for (int i = 0; i < thetas.length; i++) {
      //Random r = new Random(seed);
      double scale = Math.sqrt(6)/Math.sqrt(layersSizes[i] + layersSizes[i+1]);
      thetas[i] = new Matrix(layersSizes[i], layersSizes[i + 1], r, false).mulEquals(2.0 * scale).addEquals(-scale);
      gradients[i] = new Matrix(layersSizes[i], layersSizes[i + 1]);
      products[i] = new Matrix(1, thetas[i].getColumnDimension());
      activations[i] = new Matrix(1, thetas[i].getColumnDimension());
      numParams += layersSizes[i] * layersSizes[i + 1];
    }
  }
  
  @Override
  public void clear() {
    super.clear();
    // TODO: fix it
    for (int i = 0; i < thetas.length; i++) {
      thetas[i].fill(0.0);
    }
  }
  
  @Override
  public Model set(Model model) {
    // TODO Auto-generated method stub
    return null;
  }

  public double computeCostFunction(SparseVector x, double label) {
    return computeCostFunction(x, label, lambda);
  }

  public double computeCostFunction(SparseVector x, double label, double lv) {
    // create y
    Matrix y = new Matrix(numberOfClasses, 1);
    y.set((int) label, 0, 1.0);

    // predict y
    Matrix h = new Matrix(distributionForInstance(x), false);

    // compute cost
    double cost = 0.0;
    for (int i = 0; i < numberOfClasses; i ++) {
      cost += - y.get(i, 0) * ((h.get(i, 0) > 1.0E-6) ? Math.log(h.get(i, 0)) : -1E10) - (1.0 - y.get(i, 0)) * ((1.0 - h.get(i, 0) > 1.0E-6) ?  Math.log(1.0 - h.get(i, 0)) : -1E10);
    }

    // adding regularization term
    double reg = 0.0;
    for (int l = 0; l < thetas.length; l ++) {
      for (int i = 0; i < thetas[l].getNumberOfRows(); i ++) {
        for (int j = 1; j < thetas[l].getNumberOfColumns(); j ++) {
          reg += thetas[l].get(i, j) * thetas[l].get(i, j);
        }
      }
    }
    reg *= (lv / 2.0);
    cost += reg;

    // return cost
    return cost;
  }
  
  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < thetas.length; i++) {
      sb.append(thetas[i] + "\n");
    }
    return super.toString() + "\n" + sb.toString();
  }
  
  public Model getModelPart() {
    return getModelPart(CommonState.r);
  }
  public Model getModelPart(Random r) {
    return new ANN(this);
  }
  

}
