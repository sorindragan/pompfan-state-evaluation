package geniusweb.custom.helper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.issuevalue.Value;

public class BidVector extends Bid {

    private HashMap<IVPair, Double> mapping;
    private INDArray vector;


    public BidVector(Map<String, Value> issuevalues, Domain domain) {
        super(issuevalues);
        this.initMapping(domain);
    }

    public BidVector(String issue, Value value, Domain domain) {
        super(issue, value);
        this.initMapping(domain);
    }

    public BidVector(Bid bid, Domain domain) {
        super(bid.getIssueValues());
        this.initMapping(domain);
    }


    public HashMap<IVPair, Double> getMapping() {
        return mapping;
    }

    public void setMapping(HashMap<IVPair, Double> mapping) {
        this.mapping = mapping;
    }

    public INDArray getVector() {
        return vector;
    }

    public void setVector(INDArray vector) {
        this.vector = vector;
    }


    private void initMapping(Domain domain) {
        this.mapping = IVPair.getVectorContainer(domain);
        this.vector = BidVector.getVector(this, domain);
    }

    public static INDArray getVector(Bid bid, Domain domain) {
        List<IVPair> ivs = bid.getIssueValues().entrySet().stream().map(iv -> new IVPair(iv.getKey(), iv.getValue())).collect(Collectors.toList());
        List<INDArray> onehots = ivs.stream().map(issuevalue -> IVPair.getVector(issuevalue, domain)).collect(Collectors.toList());
        INDArray containerVector = IVPair.getContainerVector(domain);
        INDArray containerMatrix = Nd4j.accumulate(containerVector, onehots);
        return containerMatrix;
    }


}
