package geniusweb.pompfan.distances;

import org.nd4j.linalg.api.ndarray.INDArray;

/**
 * L2Distance
 */
public interface L2Distance {
    public Double computeL2(INDArray arr1, INDArray arr2);
}