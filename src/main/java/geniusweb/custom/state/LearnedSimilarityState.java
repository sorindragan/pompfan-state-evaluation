package geniusweb.custom.state;

import java.util.HashMap;

import geniusweb.actions.Action;
import geniusweb.custom.helper.IVPair;
import geniusweb.custom.opponents.AbstractPolicy;
import geniusweb.issuevalue.Domain;
import geniusweb.profile.utilityspace.UtilitySpace;

// Metric
public class LearnedSimilarityState extends AbstractState<HashMap<IVPair,Double>> {

    public LearnedSimilarityState(UtilitySpace utilitySpace, AbstractPolicy opponent) {
        super(utilitySpace, opponent);
    }

    @Override
    public String getStringRepresentation() {
        return null;
    }

    @Override
    public AbstractState<HashMap<IVPair, Double>> updateState(Action nextAction, Double time) throws StateRepresentationException {
        return null;
    }

    @Override
    public HashMap<IVPair, Double> getCurrentState() {
        return null;
    }

    @Override
    public Double computeStateDistance(HashMap<IVPair, Double> otherState) {
        return null;
    }

    @Override
    public Double evaluate() {
        return null;
    }


}
