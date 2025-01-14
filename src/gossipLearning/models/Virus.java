package gossipLearning.models;

import gossipLearning.interfaces.models.Model;
import gossipLearning.utils.SparseVector;

public class Virus implements Model {
  private static final long serialVersionUID = 3929739149066767906L;
  
  protected SparseVector seenPeers;
  protected double age;
  protected boolean isInfected;
  
  public Virus(String prefix) {
    seenPeers = new SparseVector();
    age = 0.0;
    isInfected = false;
  }
  
  public Virus(Virus a) {
    seenPeers = new SparseVector(a.seenPeers);
    age = a.age;
    isInfected = a.isInfected;
  }
  
  @Override
  public Virus clone() {
    return new Virus(this);
  }

  @Override
  public double getAge() {
    return age;
  }
  
  @Override
  public void setAge(double age) {
    this.age = age;
  }
  
  @Override
  public void clear() {
    age = 0.0;
    seenPeers.clear();
    isInfected = false;
  }
  
  @Override
  public Model set(Model model) {
    Virus m = (Virus)model;
    age = m.age;
    isInfected = m.isInfected;
    seenPeers.set(m.seenPeers);
    return this;
  }

  public boolean isInfecter() {
    return isInfected;
  }
  
  public void setInfected() {
    isInfected = true;
  }

  public void update(int rowIndex) {
    seenPeers.add(rowIndex, 1.0);
    age ++;
  }
  
  public SparseVector getVector() {
    return seenPeers;
  }
  
  public void reset() {
    isInfected = false;
    seenPeers.clear();
    age = 0.0;
  }

}
