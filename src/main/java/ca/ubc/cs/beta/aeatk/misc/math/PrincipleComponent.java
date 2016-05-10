package ca.ubc.cs.beta.aeatk.misc.math;

/**
 * Created by me on 5/10/16.
 */
public class PrincipleComponent implements Comparable<PrincipleComponent> {
  public double eigenValue;
  public double[] eigenVector;

  public PrincipleComponent(double eigenValue, double[] eigenVector) {
    this.eigenValue = eigenValue;
    this.eigenVector = eigenVector;
  }

  public int compareTo(PrincipleComponent o) {
    int ret = 0;
    if (eigenValue > o.eigenValue) {
      ret = -1;
    } else if (eigenValue < o.eigenValue) {
      ret = 1;
    }
    return ret;
  }

//  public String toString() {
//    return "Principle Component, eigenvalue: " + Debug.num(eigenValue) + ", eigenvector: ["
//            + Debug.num(eigenVector) + "]";
//  }
}
