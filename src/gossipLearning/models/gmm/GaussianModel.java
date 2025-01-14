package gossipLearning.models.gmm;

import gossipLearning.interfaces.models.Model;
import gossipLearning.interfaces.models.ProbabilityModel;
import gossipLearning.utils.SparseVector;
import peersim.config.Configuration;
import peersim.core.CommonState;

public class GaussianModel extends ProbabilityModel {
  private static final long serialVersionUID = -2123083125764994578L;
  private static final String PAR_K = "K";

  protected GMM[][] models;
  protected final int k;
  protected double[] ages;
  public GaussianModel(String prefix) {
    super(0.0);
    k = Configuration.getInt(prefix + "." + PAR_K);
  }
  
  @Override
  public GaussianModel clone() {
    return null;
  }
  
  @Override
  public void update(SparseVector instance, double label) {
    age ++;
    GMM[] m = models[(int)label];
    ages[(int)label] ++;
    int idx = 0;
    for (int i = 0; i < numberOfFeatures; i++) {
      if (idx < instance.size() && i == instance.indexAt(idx)) {
        m[i].update(instance.valueAt(idx));
        idx ++;
      } else {
        m[i].update(0);
      }
    }
  }
  
  @Override
  public void setParameters(int numberOfClasses, int numberOfFeatures) {
    super.setParameters(numberOfClasses, numberOfFeatures);
    models = new GMM[numberOfClasses][numberOfFeatures];
    ages = new double[numberOfClasses];
    for (int i = 0; i < numberOfClasses; i++) {
      for (int j = 0; j < numberOfFeatures; j++) {
        models[i][j] = new GMM(k, CommonState.r);
      }
    }
  }
  
  @Override
  public double[] distributionForInstance(SparseVector instance) {
    double p;
    for (int i = 0; i < numberOfClasses; i++) {
      p = Math.log(ages[i] / age);
      for (int j = 0; j < numberOfFeatures; j++) {
        double value = models[i][j].prob(instance.get(j));
        p += value <= 1e-5 ? -10 : Math.log(value);
      }
      distribution[i] = p;
    }
    return distribution;
  }
  
  @Override
  public Model set(Model model) {
    super.set(model);
    GaussianModel m = (GaussianModel)model;
    for (int i = 0; i < numberOfClasses; i++) {
      ages[i] = m.ages[i];
      for (int j = 0; j < numberOfFeatures; j++) {
        models[i][j].set(m.models[i][j]);
      }
    }
    return this;
  }

}
