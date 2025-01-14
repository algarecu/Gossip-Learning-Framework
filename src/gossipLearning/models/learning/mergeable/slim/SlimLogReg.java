package gossipLearning.models.learning.mergeable.slim;

import java.util.Random;

import gossipLearning.interfaces.Vector;
import gossipLearning.interfaces.models.Model;
import gossipLearning.interfaces.models.Partializable;
import gossipLearning.interfaces.models.SlimModel;
import gossipLearning.models.learning.mergeable.MergeableLogReg;
import gossipLearning.utils.SparseVector;
import gossipLearning.utils.Utils;
import gossipLearning.utils.VectorEntry;
import peersim.config.Configuration;
import peersim.util.WeightedRandPerm;

public class SlimLogReg extends MergeableLogReg implements SlimModel, Partializable {
  private static final long serialVersionUID = 6140967577949903596L;
  
  private static final String PAR_SIZE = "size";
  private static final String PAR_WEIGHTED = "weighted";
  
  protected final double modelSize;
  protected final boolean weighted;
  
  public SlimLogReg(String prefix){
    super(prefix);
    modelSize = Configuration.getDouble(prefix + "." + PAR_SIZE);
    weighted = 1 == Configuration.getInt(prefix + "." + PAR_WEIGHTED);
  }
  
  protected SlimLogReg(SlimLogReg a){
    super(a);
    modelSize = a.modelSize;
    weighted = a.weighted;
    if (a.weight != null) {
      weight = a.weight.clone();
    }
    biasWeight = a.biasWeight;
  }
  
  public SlimLogReg clone(){
    return new SlimLogReg(this);
  }
  
  @Override
  public Model merge(Model model) {
    SlimLogReg m = (SlimLogReg)model;
    double sum = age + m.age;
    if (sum == 0) {
      return this;
    }
    double modelWeight = m.age / sum;
    age = Math.max(age, m.age);
    for (VectorEntry e : m.w) {
      double value = w.get(e.index);
      w.add(e.index, (e.value - value) * modelWeight);
    }
    bias += (m.bias - bias) * modelWeight;
    return this;
  }
  
  @Override
  public Model getModelPart(Random r) {
    SlimLogReg result = new SlimLogReg(this);
    result.w = new SparseVector(1 + (int)Math.ceil(Math.abs(numberOfFeatures * modelSize)));
    double[] weights = new double[numberOfFeatures];
    for (int i = 0; i < weights.length; i++) {
      weights[i] = modelSize < 0 ? Math.abs(gradient.get(i)) + Utils.EPS : 1.0;
    }
    WeightedRandPerm rp = new WeightedRandPerm(r, weights);
    rp.reset(weights.length);
    int iter = (int)Math.floor(Math.abs(numberOfFeatures * modelSize));
    iter += r.nextDouble() < Math.abs(numberOfFeatures * modelSize) - iter ? 1 : 0;
    while (0 < iter && rp.hasNext()) {
      iter --;
      int idx = rp.next();
      result.w.add(idx, w.get(idx));
    }
    if (!weighted) {
      // TODO: remove this by scaling times of weighted
      result.w.mul(1.0 / Math.abs(modelSize));
    }
    return result;
  }
  
  private double biasWeight = 0.0;
  private SparseVector weight;
  @Override
  public Model weightedAdd(Model model, double times) {
    //times *= 1.0 / (1.0 - Math.pow(1.0 - modelSize, 1.0 / times));
    if (!weighted) {
      super.add(model, times);
      return this;
    }
    if (weight == null) {
      weight = new SparseVector();
    } else {
      // if the w initialization is not 0 do not clean
      w.pointMul(weight);
      weight.mul(biasWeight);
    }
    super.add(model, times);
    SlimLogReg m = (SlimLogReg)model;
    biasWeight += times;
    for (VectorEntry entry : m.w) {
      weight.add(entry.index, times);
    }
    weight.mul(1.0 / biasWeight);
    w.div(weight);
    return this;
  }
  
  @Override
  public void clear() {
    super.clear();
    weight = null;
    biasWeight = 0.0;
  }
  
  @Override
  public Model set(Model model) {
    Vector copy = w.clone();
    super.set(model);
    // TODO: implement for all slim models!!!
    SlimLogReg m = (SlimLogReg)model;
    for (VectorEntry e : m.w) {
      copy.add(e.index, e.value - copy.get(e.index));
    }
    w.set(copy);
    return this;
  }
  
  @Override
  public double getSize() {
    return modelSize;
  }

}
