package geniusweb.custom.distances;

import org.nd4j.linalg.api.ndarray.INDArray;

public interface ExactSame {
    public Double computeExactSame(INDArray arr1, INDArray arr2);
}
