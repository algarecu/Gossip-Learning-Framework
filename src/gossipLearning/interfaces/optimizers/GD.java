package gossipLearning.interfaces.optimizers;

import gossipLearning.interfaces.Vector;

public class GD extends Optimizer {
  private static final long serialVersionUID = 3784331513723993996L;

  public GD() {
    super();
  }
  
  public GD(String prefix) {
    super(prefix);
  }
  
  public GD(GD a) {
    super(a);
  }
  
  @Override
  public GD clone() {
    return new GD(this);
  }
  
  @Override
  public void delta(double lr, Vector gradient, double biasGradient) {
    delta.set(gradient).mul(lr);
    biasDelta = lr * biasGradient;
  }
  
  @Override
  public Optimizer merge(Optimizer o, double weight) {
    return this;
  }
  
  @Override
  public Optimizer add(Optimizer o, double times) {
    return this;
  }

}
