package gossipLearning.models.bandits;

import gossipLearning.interfaces.models.LearningModel;
import gossipLearning.interfaces.models.Model;
import gossipLearning.utils.InstanceHolder;
import gossipLearning.utils.SparseVector;
import peersim.config.Configuration;

/**
 * Abstract bandit model that handles the plays and rewards, but the 
 * update function (the logic) should be specified in the descendant classes.
 * @author István Hegedűs
 */
public abstract class BanditModel implements LearningModel {
  private static final long serialVersionUID = -7777210431635913738L;
  private static final String PAR_K = "Bandits.K";
  
  protected double age;
  protected double[] plays;
  protected double[] rewards;
  protected double sumPlays;
  protected double sumRewards;
  protected int K;
  
  /**
   * Constructs a bandit model and loads parameters from configuration file.
   * @param prefix prefix of the class in the configuration file.
   */
  public BanditModel(String prefix) {
    age = 0.0;
    sumPlays = 0.0;
    sumRewards = 0.0;
    K = Configuration.getInt(prefix + "." + PAR_K);
    plays = new double[K];
    rewards = new double[K];
    for (int i = 0; i < plays.length; i++) {
      plays[i] = 0.0;
      rewards[i] = 0.0;
    }
  }
  
  /**
   * Deep copy constructor.
   * @param a to copy
   */
  public BanditModel(BanditModel a) {
    age = a.age;
    if (a.plays != null) {
      plays = new double[a.plays.length];
      rewards = new double[a.rewards.length];
      System.arraycopy(a.plays, 0, plays, 0, a.plays.length);
      System.arraycopy(a.rewards, 0, rewards, 0, a.rewards.length);
    }
    sumPlays = a.sumPlays;
    sumRewards = a.sumRewards;
    K = a.K;
  }
  
  /**
   * Deep copy clone.
   */
  public abstract BanditModel clone();
  
  /**
   * The descendant classes implements here the logic (which arm should be played).
   */
  public abstract void update();
  
  public final void update(SparseVector instance, double label) {
    age ++;
    update();
  }
  
  public final void update(InstanceHolder instances) {
    for (int i = 0; i < instances.size(); i++) {
      update(instances.getInstance(i), instances.getLabel(i));
    }
  }
  
  @Override
  public void update(InstanceHolder instances, int epoch, int batchSize) {
    for (int i = 0; i < epoch; i++) {
      update(instances);
    }
  }
  
  @Override
  public void clear() {
    age = 0.0;
    sumPlays = 0.0;
    sumRewards = 0.0;
    for (int i = 0; i < plays.length; i++) {
      plays[i] = 0.0;
      rewards[i] = 0.0;
    }
  }
  
  public final double predict(SparseVector instance) {
    return 0.0;
  }
  
  public final double getAge() {
    return age;
  }
  
  @Override
  public final void setAge(double age) {
    this.age = age;
  }
  
  @Override
  public final void setParameters(int numberOfClasses, int numberOfFeatures) {
  }
  
  @Override
  public Model set(Model model) {
    BanditModel m = (BanditModel)model;
    age = m.age;
    K = m.K;
    System.arraycopy(m.plays, 0, plays, 0, m.plays.length);
    System.arraycopy(m.rewards, 0, rewards, 0, m.rewards.length);
    sumPlays = m.sumPlays;
    sumRewards = m.sumRewards;
    return this;
  }
  
}
