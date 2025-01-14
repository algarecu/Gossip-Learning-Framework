package gossipLearning.evaluators;

import gossipLearning.interfaces.Evaluator;
import gossipLearning.interfaces.models.LearningModel;
import gossipLearning.utils.AggregationResult;
import gossipLearning.utils.InstanceHolder;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

public class ResultAggregator implements Serializable, Iterable<AggregationResult>, Cloneable {
  private static final long serialVersionUID = 2242497407807240938L;
  protected static final ReentrantLock lock = new ReentrantLock(true);
  
  protected static Map<Integer, Evaluator[][]> aggregations;
  protected static Map<Integer, String[]> pid2ModelNames;
  protected static Map<Integer, StringBuffer[]> pid2ModelAges;
  protected static Map<Integer, String[]> pid2EvalNames;
  protected static InstanceHolder evalSet;
  
  protected final Evaluator[][] evaluators;
  protected final String[] modelNames;
  protected final double[] modelAges;
  protected final String[] evalNames;
  
  public ResultAggregator(String[] modelNames, String[] evalNames) {
    try {
      if (aggregations == null) {
        aggregations = new TreeMap<Integer, Evaluator[][]>();
        pid2ModelNames = new TreeMap<Integer, String[]>();
        pid2ModelAges = new TreeMap<Integer, StringBuffer[]>();
        pid2EvalNames = new TreeMap<Integer, String[]>();
      }
      this.modelNames = modelNames;
      this.evalNames = evalNames;
      this.modelAges = new double[modelNames.length];
      this.evaluators = new Evaluator[modelNames.length][evalNames.length];
      for (int i = 0; i < modelNames.length; i++) {
        modelAges[i] = 0.0;
        for (int j = 0; j < evalNames.length; j++) {
          this.evaluators[i][j] = (Evaluator)Class.forName(evalNames[j]).newInstance();
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Exception was occured in ResultAggregator: ", e);
    }
  }
  
  protected ResultAggregator(ResultAggregator a) {
    modelNames = a.modelNames;
    evalNames = a.evalNames;
    modelAges = new double[a.modelAges.length];
    System.arraycopy(a.modelAges, 0, modelAges, 0, a.modelAges.length);
    evaluators = new Evaluator[a.evaluators.length][];
    for (int i = 0; i < evaluators.length; i++) {
      evaluators[i] = new Evaluator[a.evaluators[i].length];
      for (int j = 0; j < evaluators[i].length; j++) {
        evaluators[i][j] = a.evaluators[i][j].clone();
      }
    }
  }
  
  @Override
  public ResultAggregator clone() {
    return new ResultAggregator(this);
  }
  
  public void push(int pid, int index, LearningModel model) {
    // TODO: voting is not implemented yet
    //LearningModel model = (LearningModel)modelHolder.getModel(modelHolder.size() - 1);
    modelAges[index] = model.getAge();
    for (int i = 0; i < evalSet.size(); i++) {
      double expected = evalSet.getLabel(i);
      double predicted = model.predict(evalSet.getInstance(i));
      for (int j = 0; j < evaluators[index].length; j++) {
        evaluators[index][j].evaluate(expected, predicted);
      }
    }
    push(pid, index);
  }
  
  protected void push(int pid, int modelIndex) {
    lock.lock();
    Evaluator[][] evaluator = aggregations.get(pid);
    if (!pid2ModelNames.containsKey(pid)) {
      pid2ModelNames.put(pid, modelNames);
      pid2ModelAges.put(pid, new StringBuffer[modelAges.length]);
      pid2EvalNames.put(pid, evalNames);
    }
    StringBuffer[] buffs = pid2ModelAges.get(pid);
    if (evaluator == null) {
      evaluator = new Evaluator[modelNames.length][evalNames.length];
      for (int model_i = 0; model_i < modelNames.length; model_i++) {
        if (AggregationResult.isPrintAges) {
          buffs[model_i] = new StringBuffer();
        }
        for (int eval_j = 0; eval_j < evalNames.length; eval_j++) {
          evaluator[model_i][eval_j] = evaluators[model_i][eval_j].clone();
          evaluator[model_i][eval_j].clear();
        }
      }
      aggregations.put(pid, evaluator);
    }
    if (AggregationResult.isPrintAges) {
      buffs[modelIndex].append(' ');
      buffs[modelIndex].append(modelAges[modelIndex]);
    }
    for (int i = 0; i < evalNames.length; i++) {
      evaluator[modelIndex][i].merge(evaluators[modelIndex][i]);
    }
    lock.unlock();
  }

  public void setEvalSet(InstanceHolder evalSet) {
    lock.lock();
    ResultAggregator.evalSet = evalSet;
    lock.unlock();
  }
  
  @Override
  public Iterator<AggregationResult> iterator() {
    lock.lock();
    try {
    List<AggregationResult> results = new LinkedList<AggregationResult>();
    for (Entry<Integer, Evaluator[][]> entry : aggregations.entrySet()) {
      for (int i = 0; i < entry.getValue().length; i++) {
        if (AggregationResult.isPrintAges) {
          System.out.println("##Ages\t" + entry.getKey() + "\t" + pid2ModelNames.get(entry.getKey())[i] + "\t" + pid2ModelAges.get(entry.getKey())[i]);
          pid2ModelAges.get(entry.getKey())[i] = new StringBuffer();
        }
        for (int j = 0; j < entry.getValue()[i].length; j++) {
          results.add(new AggregationResult(entry.getKey(), pid2ModelNames.get(entry.getKey())[i], pid2EvalNames.get(entry.getKey())[j], entry.getValue()[i][j].getNames(), entry.getValue()[i][j].getResults()));
        }
      }
    }
    return results.iterator();
    } finally {
      lock.unlock();
    }
  }
  
  @Override
  public String toString() {
    lock.lock();
    try {
    StringBuffer sb = new StringBuffer();
    for (Entry<Integer, Evaluator[][]> entry : aggregations.entrySet()) {
      for (int i = 0; i < entry.getValue().length; i++) {
        sb.append(pid2ModelNames.get(entry.getKey())[i] + ":\n");
        for (int j = 0; j < entry.getValue()[i].length; j++) {
          sb.append(entry.getValue()[i][j]);
          sb.append('\n');
        }
      }
    }
    return sb.toString();
    } finally {
      lock.unlock();
    }
  }

}
