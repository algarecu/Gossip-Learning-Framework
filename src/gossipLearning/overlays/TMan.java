package gossipLearning.overlays;

import gossipLearning.controls.ChurnControl;
import gossipLearning.interfaces.protocols.Churnable;
import gossipLearning.messages.TManMessage;
import gossipLearning.utils.NodeDescriptor;

import java.io.Serializable;
import java.security.InvalidParameterException;

import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Fallible;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;
import peersim.extras.mj.ednewscast.CycleMessage;
import peersim.transport.Transport;

/**
 * Stores the descriptors of the nodes in <b>descendant order</b> based on the 
 * similarity defined in the NodeDescriptor class computeSimilarity 
 * function, so the higher is better. The similarity for a stored descriptor 
 * is set as the similarity between the data of the stored node and the data 
 * of the current node.
 * @author István Hegedűs
 */
public class TMan implements EDProtocol, Linkable, Serializable, Churnable {
  private static final long serialVersionUID = 5481011536830137165L;
  private static final String PAR_CACHE = "cache";
  private static final String PAR_BASEFREQ = "baseFreq";
  private static final String PAR_ISSIM = "isSim";
  
  private final int baseFreq;
  private final boolean isSim;
  private long sessionLength = ChurnControl.INIT_SESSION_LENGTH;
  
  private NodeDescriptor me;
  private NodeDescriptor[] cache;
  private int size;
  
  public TMan(String prefix) {
    final int cachesize = Configuration.getInt(prefix + "." + PAR_CACHE);
    baseFreq = Configuration.getInt(prefix + "." + PAR_BASEFREQ);
    isSim = Configuration.getBoolean(prefix + "." + PAR_ISSIM);

    if (baseFreq <= 0) {
      throw (InvalidParameterException) new InvalidParameterException(
          "parameter 'baseFreq' must be >0");
    }
    cache = new NodeDescriptor[cachesize];
    size = 0;
  }
  
  protected TMan(TMan a) {
    if (a.me == null) {
      me = a.me;
    } else {
      me = a.me.clone();
    }
    baseFreq = a.baseFreq;
    cache = new NodeDescriptor[a.cache.length];
    for (int i = 0; i < a.degree(); i++) {
      cache[i] = a.cache[i].clone();
    }
    size = a.size;
    isSim = a.isSim;
  }
  
  @Override
  public TMan clone() {
    return new TMan(this);
  }

  @Override
  public void onKill() {
    cache = null;
  }

  @Override
  public int degree() {
    return size;
  }

  @Override
  public Node getNeighbor(int i) {
    return cache[i].getNode();
  }

  @Override
  public boolean addNeighbor(Node neighbour) {
    for (int i = 0; i < size; i++) {
      if (cache[i].getNode().getID() == neighbour.getID())
        return false;
    }

    if (size < cache.length) {
      // add new neighbor
      cache[size] = new NodeDescriptor(neighbour, null, isSim);
      cache[size].setSimilarity(Double.NEGATIVE_INFINITY);
      // find its position
      for (int j = size; j > 0 && cache[j].compareTo(cache[j-1]) > 0; j--) {
        NodeDescriptor tmp = cache[j-1];
        cache[j-1] = cache[j];
        cache[j] = tmp;
      }
      size ++;
      return true;
    } else {
      throw new IndexOutOfBoundsException();
    }
  }

  @Override
  public boolean contains(Node neighbor) {
    for (int i = 0; i < size; i++) {
      if (cache[i].getNode().getID() == neighbor.getID())
        return true;
    }
    return false;
  }

  @Override
  public void pack() {
  }

  @Override
  public void processEvent(Node node, int pid, Object event) {
    if (me == null) {
      me = new NodeDescriptor(node, null, isSim);
    }
    
    if (event instanceof TManMessage) {
      final TManMessage msg = (TManMessage) event;
      // send the answer message
      if (!msg.isAnswer) {
        ((Transport) node.getProtocol(FastConfig.getTransport(pid))).send(node, msg.src, new TManMessage(me, cache, true), pid);
      }
      // merge the received descriptors to the local cache
      merge(msg.cache);
      
    }

    if (event instanceof CycleMessage) {
      // get peer from the cache uniformly
      final Node peern = getPeer();

      // send my descriptor and neighbors to the selected node
      if (peern != null) {
        ((Transport) node.getProtocol(FastConfig.getTransport(pid))).send(node, peern, new TManMessage(me, cache, false), pid);
      }
      // add the next event to the queue with base frequency as a delay
      EDSimulator.add(baseFreq, CycleMessage.inst, node, pid);
    }
  }
  
  public long getSessionLength() {
    return sessionLength;
  }

  public void setSessionLength(long sessionLength) {
    this.sessionLength = sessionLength;
  }

  public void initSession(Node node, int protocol) {
    size = 0;
    while (degree() < cache.length) {
      int onlineNeighbor = CommonState.r.nextInt(Network.size());
      if ( Network.get(onlineNeighbor).getFailState() != Fallible.DOWN
          && Network.get(onlineNeighbor).getFailState() != Fallible.DEAD
          && Network.get(onlineNeighbor).getID() != node.getID()) {
        //System.out.println(currentNode.getID() + " addNeighbor who is UP=" + onlineNeighbor + ", state=" + ((Network.get(onlineNeighbor).getFailState() == Fallible.OK) ? "UP" : "DOWN"));
        addNeighbor(Network.get(onlineNeighbor));
      }
    }
    EDSimulator.add(0, CycleMessage.inst, node, protocol);
  }
  
  /**
   * Returns the descriptor that is at the specified position.
   * @param i position of the descriptor to be returned
   * @return the ith descriptor
   */
  public NodeDescriptor getDescriptor(int i) {
    return cache[i];
  }
  
  /**
   * Returns the descriptor of the current node.
   * @return the descriptor of the current node
   */
  public NodeDescriptor getDescriptor() {
    return me;
  }
  
  /**
   * Sets the specified descriptor for the local descriptor of the current node.
   * @param descriptor descriptor to be set.
   */
  public void setDescriptor(NodeDescriptor descriptor) {
    me = descriptor;
  }
  
  /**
   * Merges the specified array of descriptors to the local cache.
   * @param cache to be merged
   */
  private void merge(NodeDescriptor[] cache) {
    for (int i = 0; i < cache.length && cache[i] != null; i++) {
      NodeDescriptor desc = cache[i];
      desc.setSimilarity(me.computeSimilarity(desc));
      insert(desc);
    }
  }
  
  /**
   * Inserts the specified descriptor to the cache if it belongs to a 
   * different node than the current, and the cache has free space or 
   * the specified descriptor is higher than the last one in the cache.
   * And returns true if the insertion is succeeded.
   * @param desc to be stored
   * @return true if the insertion is succeeded
   */
  private boolean insert(NodeDescriptor desc) {
    if (desc.getNode().getID() == me.getNode().getID()) {
      return false;
    }
    boolean repair = false;
    int index = containsNode(desc.getNode());
    if (index >= 0) {
      // if contains the descriptor, than actualize and repair
      cache[index] = desc;
      // repair forward (goes to the end/tail of the list)
      if (index < size -1 && cache[index].compareTo(cache[index +1]) < 0) {
        for (int i = index; i < size -1 && cache[index].compareTo(cache[index +1]) < 0; i++) {
          NodeDescriptor tmp = cache[index];
          cache[index] = cache[index +1];
          cache[index +1] = tmp;
        }
        return true;
      } else {
        // repair backward (goes to the head of the list)
        repair = true;
      }
    } else if (size < cache.length) {
      // if does not contain and not full, insert to the end and repair (backward)
      cache[size] = desc;
      index = size;
      size ++;
      repair = true;
    } else if (cache[size -1].compareTo(desc) < 0) {
      // if does not contain and better than the last, insert to the end and repair (backward)
      cache[size -1] = desc;
      index = size -1;
      repair = true;
    }
    if (repair) {
      // repair (backward) starts from the index of the insertion and goes to the head of the list
      for (int i = index; i > 0 && cache[i].compareTo(cache[i -1]) > 0; i--) {
        NodeDescriptor tmp = cache[i];
        cache[i] = cache[i -1];
        cache[i -1] = tmp;
      }
    }
    return repair;
  }
  
  /**
   * Checks that the specified node is in the cache and returns its position 
   * if it is in and -1 otherwise.
   * @param neighbor to be checked
   * @return position of the specified node or -1
   */
  private int containsNode(Node neighbor) {
    for (int i = 0; i < cache.length; i++) {
      if (cache[i].getNode().getID() == neighbor.getID()) {
        return i;
      }
    }
    return -1;
  }
  
  /**
   * Returns a node, selected uniform randomly from the cache.
   * @return uniform randomly selected node
   */
  private Node getPeer() {
    final int d = degree();
    if (d == 0) {
      return null;
    } else {
      return cache[CommonState.r.nextInt(d)].getNode();
    }
  }

}
