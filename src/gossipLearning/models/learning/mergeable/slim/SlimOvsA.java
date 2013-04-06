package gossipLearning.models.learning.mergeable.slim;

import gossipLearning.interfaces.ModelHolder;
import gossipLearning.interfaces.models.Model;
import gossipLearning.interfaces.models.Partializable;
import gossipLearning.models.learning.mergeable.MergeableOVsA;
import gossipLearning.utils.BQModelHolder;

import java.util.Arrays;
import java.util.Set;

import peersim.config.Configuration;

public class SlimOvsA extends MergeableOVsA {
  private static final long serialVersionUID = 4459146413742898799L;
  
  /** @hidden */
  private static final String PAR_BNAME = "SlimOVsA";
  
  /**
   * Default constructor (do nothing).
   */
  public SlimOvsA() {
    super();
  }
  
  /**
   * Copy constructor for deep copy
   * @param a to copy
   */
  public SlimOvsA(SlimOvsA a) {
    this.baseLearnerName = a.baseLearnerName;
    this.numberOfClasses = a.numberOfClasses;
    this.prefix = a.prefix;
    if (a.classifiers != null) {
      this.classifiers = (ModelHolder)a.classifiers.clone();
    } else {
      classifiers = null;
    }
    this.distribution = Arrays.copyOf(a.distribution, a.distribution.length);
  }
  
  /**
   * Constructs an object and sets the specified parameters.
   * @param baseLearnerName name of the used learning algorithm
   * @param numberOfClasses number of classes
   * @param prefix
   * @param classifiers
   * @param distribution
   */
  protected SlimOvsA(String baseLearnerName, int numberOfClasses, 
      String prefix, ModelHolder classifiers, double[] distribution) {
    this.baseLearnerName = baseLearnerName;
    this.numberOfClasses = numberOfClasses;
    this.prefix = prefix;
    this.classifiers = classifiers;
    this.distribution = distribution;
  }
  
  @Override
  public Object clone() {
    return new SlimOvsA(this);
  }

  @Override
  public void init(String prefix) {
    this.prefix = prefix;
    baseLearnerName = Configuration.getString(prefix + "." + PAR_BNAME + ".modelName");
  }

  @Override
  public SlimOvsA merge(MergeableOVsA model) {
    super.merge(model);
    return this;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public SlimOvsA getModelPart(Set<Integer> indices) {
    ModelHolder classifiers = new BQModelHolder(this.classifiers.size());
    for (int i = 0; i < numberOfClasses; i++) {
      Model m = ((Partializable)this.classifiers.getModel(i)).getModelPart(indices);
      classifiers.add(m);
    }
    return new SlimOvsA(baseLearnerName, numberOfClasses, prefix, classifiers, Arrays.copyOf(distribution, distribution.length));
  }

}
