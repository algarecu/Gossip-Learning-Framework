package gossipLearning.utils;

import java.io.Serializable;

import peersim.core.Node;

/**
 * This class represents a node and a corresponding vector.
 * Nodes can be compared by cosine similarity or Euclidean distance 
 * of the corresponding descriptors.
 * @author István Hegedűs
 */
public class NodeDescriptor implements Serializable, Comparable<NodeDescriptor>, Cloneable {
  private static final long serialVersionUID = -8582247148380060765L;
  
  private final Node node;
  private SparseVector descriptor;
  private double similarity;
  private final boolean isSim;
  
  /**
   * Creates an object and stored the specified parameters.
   * @param node node to be described
   * @param descriptor descriptor that describes the node
   * @param isSim if true the comparison is based on cosine similarity and 
   * Euclidean distance otherwise
   */
  public NodeDescriptor(Node node, SparseVector descriptor, boolean isSim) {
    this.node = node;
    this.descriptor = descriptor;
    this.isSim = isSim;
  }
  
  /**
   * Deep copy constructor.
   * @param a to be copied
   */
  public NodeDescriptor(NodeDescriptor a) {
    node = a.node;
    if (a.descriptor != null) {
      descriptor = a.descriptor.clone();
    } else {
      descriptor = null;
    }
    similarity = a.similarity;
    isSim = a.isSim;
  }
  
  @Override
  public NodeDescriptor clone() {
    return new NodeDescriptor(this);
  }
  
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof NodeDescriptor)) {
      return false;
    }
    NodeDescriptor a = (NodeDescriptor)o;
    if (node.getID() != a.node.getID()) {
      return false;
    }
    return true;
  }
  
  /**
   * Returns the described node.
   * @return described node
   */
  public Node getNode() {
    return node;
  }
  
  /**
   * Sets the specified descriptor as the descriptor of the node.
   * @param descriptor to be set
   */
  public void setDecriptor(SparseVector descriptor) {
    this.descriptor = descriptor;
  }
  
  /**
   * Returns the descriptor that describes the node.
   * @return descriptor of the node.
   */
  public SparseVector getDescriptor() {
    return descriptor;
  }
  
  /**
   * Sets the specified similarity for the descriptor
   * @param similarity to be set
   */
  public void setSimilarity(double similarity) {
    this.similarity = similarity;
  }
  
  /**
   * Returns the similarity value of the descriptor.
   * @return similarity
   */
  public double getSimilarity() {
    return similarity;
  }

  @Override
  public int compareTo(NodeDescriptor a) {
    if (similarity < a.similarity) {
      return isSim ? 1 : -1;
    } else if (similarity > a.similarity) {
      return isSim ? -1 : 1;
    }
    return 0;
  }
  
  /**
   * Computes the similarity between the current descriptor and the 
   * specified descriptor. The similarity of a null is Double.NEGATIVE_INFINITY.
   * @param a compute similarity for
   * @return similarity
   */
  public double computeSimilarity(NodeDescriptor a) {
    if (a.descriptor == null) {
      return Double.NEGATIVE_INFINITY;
    }
    if (isSim) {
      return descriptor.cosineSimilarity(a.descriptor);
    } else {
      return -descriptor.euclideanDistance(a.descriptor);
    }
  }
  
  @Override
  public String toString() {
    return node.getID() + ":" + descriptor;
  }
  
}
