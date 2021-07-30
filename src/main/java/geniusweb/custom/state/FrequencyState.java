package geniusweb.custom.state;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;

import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.custom.distances.CosineSimilarity;
import geniusweb.custom.distances.L2Distance;
import geniusweb.custom.helper.IVPair;
import geniusweb.custom.opponents.AbstractPolicy;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.profile.utilityspace.UtilitySpace;

public class FrequencyState extends AbstractState<HashMap<IVPair, Double>> {

    private List<IVPair> allIssueValues;

    public FrequencyState() {
        super();
    }

    public FrequencyState(UtilitySpace utilitySpace, AbstractPolicy opponent) {
        super(utilitySpace, opponent);
        this.init(IVPair.getVectorContainer(utilitySpace.getDomain()));
    }

    public HashMap<IVPair, Double> getFrequency() {
        return this.getRepresentation();
    }

    public void setFrequency(HashMap<IVPair, Double> frequency) {
        this.init(frequency);
    }

    @Override
    public String getStringRepresentation() {
        return this.getRepresentation().toString();
    }

    @Override
    public AbstractState<HashMap<IVPair, Double>> updateState(Action nextAction, Double time) throws StateRepresentationException {
        HashMap<IVPair, Double> representation = new HashMap<IVPair, Double>(this.getRepresentation());
        if (nextAction instanceof Offer) {
            Bid bid = ((Offer) nextAction).getBid();
            List<IVPair> bidIssueValues = bid.getIssueValues().entrySet().stream()
                    .map(entry -> new IVPair(entry.getKey(), entry.getValue())).collect(Collectors.toList());
            for (IVPair issueValuePair : bidIssueValues) {
                representation.computeIfPresent(issueValuePair, (key, val) -> val + 1);
            }
            return new FrequencyState(this.getUtilitySpace(), this.getOpponent()).init(representation).setRound(time);
        }
        throw new StateRepresentationException();
    }

    @Override
    public HashMap<IVPair, Double> getCurrentState() {
        return this.getRepresentation();
    }

    @Override
    public Double computeStateDistance(HashMap<IVPair, Double> otherState) {
        HashMap<IVPair, Double> currState = this.getRepresentation();
        double[] currVals = currState.values().stream().mapToDouble(val -> (double) val).toArray();
        double[] otherVals = otherState.values().stream().mapToDouble(val -> (double) val).toArray();

        INDArray arr1 = Nd4j.createFromArray(currVals);
        INDArray arr2 = Nd4j.createFromArray(otherVals);

        return this.computeL2(arr1, arr2);
    }

    @Override
    public Double evaluate() {
        // TODO Auto-generated method stub
        return null;
    }

}
