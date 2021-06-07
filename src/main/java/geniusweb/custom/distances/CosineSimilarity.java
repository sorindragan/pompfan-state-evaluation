package geniusweb.custom.distances;

import org.nd4j.linalg.api.ndarray.INDArray;

public interface CosineSimilarity {
    public Double computeCosineSimiliarity(INDArray arr1, INDArray arr2);
}
