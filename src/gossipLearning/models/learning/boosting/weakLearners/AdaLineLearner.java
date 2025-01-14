package gossipLearning.models.learning.boosting.weakLearners;

import gossipLearning.interfaces.models.WeakLearner;
import gossipLearning.utils.SparseVector;
import gossipLearning.utils.Utils;

import java.util.Random;

public class AdaLineLearner extends WeakLearner {
  private static final long serialVersionUID = -1540156152482197419L;
  
  private SparseVector w;
  private double[] v;
  private Random r;
  private static long c;

  public AdaLineLearner(String prefix, double lambda, long seed) {
    super(prefix, lambda, seed);
    r = new Random(seed | c++);
    w = new SparseVector();
  }
  
  public AdaLineLearner(AdaLineLearner a) {
    super(a);
    r = new Random(seed | c++);
    w = a.w.clone();
    v = new double[numberOfClasses];
    if (a.v != null) {
      for (int i = 0; i < numberOfClasses; i++) {
        v[i] = a.v[i];
      }
    }
  }
  
  @Override
  public void setParameters(int numberOfClasses, int numberOfFeatures) {
    super.setParameters(numberOfClasses, numberOfFeatures);
    v = new double[numberOfClasses];
    for (int i = 0; i < numberOfClasses; i++) {
      v[i] = r.nextBoolean() ? 1.0 : -1.0;
    }
  }
  
  @Override
  public AdaLineLearner clone() {
    return new AdaLineLearner(this);
  }

  @Override
  public void update(SparseVector instance, double label, double[] weight) {
    age ++;
    double nu = 1.0 / (double) (age * lambda); // regularized
    double innerProd = w.mul(instance);
    double[] distribution = distributionForInstance(instance);
    double yl;
    double exp;
    
    // update w
    w.mul(1.0 - 1.0 / age);
    SparseVector grad = new SparseVector(instance.size());
    for (int l = 0; l < numberOfClasses; l++) {
      yl = (label == l) ? 1.0 : -1.0;
      exp = Math.exp(-yl * distribution[l]);
      grad.add(instance, -weight[l] * exp * yl * v[l]);
    }
    w.add(grad, -nu);
    
    // update v
    for (int l = 0; l < numberOfClasses; l++) {
      yl = (label == l) ? 1.0 : -1.0;
      exp = Math.exp(-yl * distribution[l]);
      v[l] += (1.0/age) * weight[l] * exp * yl * innerProd;
    }
  }

  @Override
  public double[] distributionForInstance(SparseVector instance) {
    double[] distribution = new double[numberOfClasses];
    double innerProd = w.mul(instance);
    for (int i = 0; i < numberOfClasses; i++) {
      distribution[i] = v[i] * innerProd;
    }
    return Utils.normalize(distribution);
  }

}
