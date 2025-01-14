package gossipLearning.models.learning;

import gossipLearning.utils.InstanceHolder;
import gossipLearning.utils.SparseVector;
import gossipLearning.utils.Utils;
import peersim.config.Configuration;
import peersim.core.CommonState;

public class CompressedLogReg extends LogisticRegression {
  private static final long serialVersionUID = 3906634332190699663L;
  private static final String PAR_NBITS = "nbits";
  
  protected final int nbits;
  
  public CompressedLogReg(String prefix) {
    super(prefix);
    nbits = Configuration.getInt(prefix + "." + PAR_NBITS);
  }
  
  protected CompressedLogReg(CompressedLogReg a) {
    super(a);
    nbits = a.nbits;
  }
  
  @Override
  public CompressedLogReg clone() {
    return new CompressedLogReg(this);
  }
  
  @Override
  protected void gradient(SparseVector instance, double label) {
    double prob = getPositiveProbability(instance);
    double err = label - prob;
    //gradient.set(w).mul(lambda).add(instance, err);
    //biasGradient = lambda * err;
    gradient.set(instance).mul(err).scale(nbits, CommonState.r);
    gradient.add(w, lambda);
    biasGradient = bias * lambda + Utils.scaleValueRange(err, nbits, CommonState.r);
  }
  
  protected SparseVector inst_tmp = new SparseVector();
  @Override
  protected void gradient(InstanceHolder instances) {
    gradient.clear();
    biasGradient = 0.0;
    for (int i = 0; i < instances.size(); i++) {
      SparseVector instance = instances.getInstance(i);
      double label = instances.getLabel(i);
      
      double prob = getPositiveProbability(instance);
      double err = label - prob;
      inst_tmp.set(instance).mul(err).scale(nbits, CommonState.r);
      gradient.add(inst_tmp);
      biasGradient += bias * lambda + Utils.scaleValueRange(err, nbits, CommonState.r);
    }
    gradient.add(w, lambda * instances.size());
  }
  

}
