package geniusweb.custom.distances;

import org.nd4j.linalg.api.ndarray.INDArray;

public interface ExactSame<T> {
    public Double computeExactSame(T arr1, T arr2);
}
