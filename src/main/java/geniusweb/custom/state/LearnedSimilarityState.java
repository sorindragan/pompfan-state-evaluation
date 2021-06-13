package geniusweb.custom.state;

import java.util.HashMap;

import geniusweb.actions.Action;
import geniusweb.custom.helper.IVPair;
import geniusweb.custom.strategies.AbstractPolicy;
import geniusweb.issuevalue.Domain;

// Metric
public class LearnedSimilarityState extends AbstractState<HashMap<IVPair,Double>> {

    public LearnedSimilarityState(Domain domain, AbstractPolicy opponent) {
        super(domain, opponent);
        //TODO Auto-generated constructor stub
    }

    @Override
    public String getStringRepresentation() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AbstractState<HashMap<IVPair, Double>> updateState(Action nextAction) throws StateRepresentationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HashMap<IVPair, Double> getCurrentState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Double computeDistance(HashMap<IVPair, Double> otherState) {
        // TODO Auto-generated method stub
        return null;
    }


}
