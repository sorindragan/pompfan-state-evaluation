package geniusweb.custom.state;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.custom.components.Opponent;
import geniusweb.custom.opponents.AbstractPolicy;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.profile.utilityspace.UtilitySpace;

public class HistoryState extends AbstractState<ArrayList<Action>> {
    private double DISCOUNT_RATE = 0.95;
    private ArrayList<Action> history;

    public HistoryState(UtilitySpace utilitySpace, AbstractPolicy opponent) {
        super(utilitySpace, opponent);
        this.setHistory(new ArrayList<>());
    }

    public ArrayList<Action> getHistory() {
        return this.getRepresentation();
    }

    public void setHistory(ArrayList<Action> history) {
        this.init(history);
    }

    @Override
    public String getStringRepresentation() {
        return this.getHistory().stream().map(Object::toString).collect(Collectors.joining("->"));
    }

    @Override
    public AbstractState<ArrayList<Action>> updateState(Action nextAction) throws StateRepresentationException {
        ArrayList<Action> representation = new ArrayList<Action>(this.getRepresentation());
        representation.add(nextAction);
        return new HistoryState(this.getUtilitySpace(), this.getOpponent()).init(representation);
    }

    @Override
    public ArrayList<Action> getCurrentState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Double computeDistance(ArrayList<Action> otherState) {
        ArrayList<Action> currState = this.getRepresentation();
        return this.computeExactSame(currState, otherState);
    }

    @Override
    public Double evaluate() {
        ArrayList<Action> currState = this.getRepresentation();
        int numBids = currState.size();
        double discountedUtility = this.evaluateLast2Bids() * Math.pow(DISCOUNT_RATE, numBids);
        return discountedUtility;
    }

    protected Double evaluateLast2Bids() {
        ArrayList<Action> currHistory = this.getHistory();
        int length = currHistory.size();
        Bid lastAgentBid = length > 1 ? ((Offer) currHistory.get(length - 2)).getBid() : null;
        Bid lastOpponentBid = length > 1 ? ((Offer) currHistory.get(length - 1)).getBid() : null;
        BigDecimal ZERO_UTILITY = new BigDecimal(0);
        BigDecimal utility1 = lastAgentBid != null ? this.getUtilitySpace().getUtility(lastAgentBid)
                : ZERO_UTILITY;
        BigDecimal utility2 = lastOpponentBid != null ? this.getUtilitySpace().getUtility(lastOpponentBid)
                : ZERO_UTILITY;
        BigDecimal mean = utility1.add(utility2).divide(new BigDecimal(2));
        return mean.doubleValue();
    }

}
