package gossipLearning.models.learning.mergeable.slim;

import gossipLearning.interfaces.models.Model;
import gossipLearning.interfaces.models.Partializable;
import gossipLearning.interfaces.models.SlimModel;
import gossipLearning.models.learning.mergeable.MergeableANN;
import gossipLearning.utils.Matrix;
import gossipLearning.utils.Utils;

import java.util.Random;

import peersim.config.Configuration;
import peersim.util.WeightedRandPerm;

public class SlimANN extends MergeableANN implements SlimModel, Partializable {
  private static final long serialVersionUID = -1489213271550290187L;
  private static final String PAR_SIZE = "size";
  
  protected final int modelSize;
  
  public SlimANN(String prefix) {
    super(prefix);
    modelSize = Configuration.getInt(prefix + "." + PAR_SIZE);
  }
  
  protected SlimANN(SlimANN a) {
    super(a);
    modelSize = a.modelSize;
  }
  
  public SlimANN clone() {
    return new SlimANN(this);
  }
  
  @Override
  public Model merge(Model m) {
    SlimANN model = (SlimANN)m;
    double sum = age + model.age;
    if (sum == 0) {
      return this;
    }
    double modelWeight = model.age / sum;
    age = Math.max(age, model.age);
    for (int i = 0; i < thetas.length; i++) {
      for (int j = 0; j < thetas[i].getNumberOfRows(); j++) {
        for (int k = 0; k < thetas[i].getNumberOfColumns(); k++) {
          double value = model.thetas[i].get(j, k);
          if (value == 0.0) {
            continue;
          }
          value *= modelWeight;
          value += thetas[i].get(j, k) * (age / sum);
          thetas[i].set(j, k, value);
        }
      }
      //thetas[i].mulEquals(age / sum).addEquals(model.thetas[i], modelWeight);
    }
    return this;
  }
  
  @Override
  public SlimANN getModelPart(Random r) {
    double[] weights = new double[numParams];
    int size = 0;
    for (int i = 0; i < thetas.length; i++) {
      for (int j = 0; j < thetas[i].getNumberOfRows(); j++) {
        for (int k = 0; k < thetas[i].getNumberOfColumns(); k++) {
          int idx = size + j*thetas[i].getNumberOfColumns() + k;
          weights[idx] = modelSize < 0 ? 1.0 : Math.abs(gradients[i].get(j, k));
          if (weights[idx] == 0.0) {
            weights[idx] = Utils.EPS;
          }
        }
      }
      size += thetas[i].size();
    }
    WeightedRandPerm rp = new WeightedRandPerm(r, weights);
    rp.reset(numParams);
    int iter = (int)(numParams * (Math.abs(modelSize) / (double)numberOfFeatures));
    SlimANN result = new SlimANN(this);
    for (int i = 0; i < thetas.length; i++) {
      result.thetas[i].fill(0.0);
    }
    while (0 < iter && rp.hasNext()) {
      iter --;
      int idx = rp.next();
      int i = 0;
      int s = thetas[i].size();
      while (s <= idx) {
        i++;
        s+=thetas[i].size();
      }
      s-=thetas[i].size();
      int j = (idx - s)/thetas[i].getNumberOfColumns();
      int k = (idx - s)%thetas[i].getNumberOfColumns();
      result.thetas[i].set(j, k, thetas[i].get(j, k));
    }
    return result;
  }
  
  private double biasWeight = 0.0;
  private Matrix[] weight;
  @Override
  public Model weightedAdd(Model model, double times) {
    if (weight == null) {
      weight = new Matrix[thetas.length];
      for (int i = 0; i < thetas.length; i++) {
        weight[i] = new Matrix(thetas[i].getNumberOfRows(), thetas[i].getNumberOfColumns());
      }
    } else {
      // if the w initialization is not 0 do not clean
      for (int i = 0; i < thetas.length; i++) {
        thetas[i].pointMulEquals(weight[i]);
        weight[i].mulEquals(biasWeight);
      }
    }
    super.add(model, times);
    SlimANN m = (SlimANN)model;
    biasWeight += times;
    for (int i = 0; i < thetas.length; i++) {
      for (int j = 0; j < thetas[i].getNumberOfRows(); j++) {
        for (int k = 0; k < thetas[i].getNumberOfColumns(); k++) {
          if (m.thetas[i].get(j, k) != 0.0) {
            weight[i].set(j, k, weight[i].get(j, k) + times);
          }
        }
      }
      weight[i].mulEquals(1.0 / biasWeight);
      thetas[i].pointDivEquals(weight[i]);
    }
    return this;
  }
  
  @Override
  public void clear() {
    super.clear();
    weight = null;
    biasWeight = 0.0;
  }
  
  @Override
  public double getSize() {
    return modelSize;
  }
  
  @Override
  public Model scale(double value) {
    // TODO Auto-generated method stub
    return null;
  }

}
