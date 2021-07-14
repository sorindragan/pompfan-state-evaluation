package geniusweb.custom.state;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.custom.components.Opponent;
import geniusweb.custom.opponents.AbstractPolicy;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.profile.utilityspace.UtilitySpace;

public class HistoryState extends AbstractState<ArrayList<Action>> {
    private double DISCOUNT_RATE = 0.95;

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
    public AbstractState<ArrayList<Action>> updateState(Action nextAction, Double time)
            throws StateRepresentationException {
        ArrayList<Action> representation = new ArrayList<Action>(this.getRepresentation());
        representation.add(nextAction);
        return new HistoryState(this.getUtilitySpace(), this.getOpponent()).init(representation).setRound(time);
    }

    @Override
    public ArrayList<Action> getCurrentState() {
        return null;
    }

    @Override
    public Double computeStateDistance(ArrayList<Action> otherState) {
        ArrayList<Action> currState = this.getRepresentation();
        return this.computeExactSame(currState, otherState);
    }

    @Override
    public Double evaluate() {
        // ArrayList<Action> currState = this.getRepresentation();
        // int numBids = currState.size();
        // double discountedUtility = this.evaluateLast2Bids() * Math.pow(DISCOUNT_RATE, numBids);
        double discountedUtility = this.evaluateLast2Bids();
        return discountedUtility;
    }

    protected Double evaluateLast2Bids() {
        ArrayList<Action> currHistory = this.getHistory();
        int length = currHistory.size();
        Action lastOpponentAction = currHistory.get(length - 1);
        if (lastOpponentAction instanceof Accept) {
            // In case last opponent action was an acceptance
            Accept acceptanceBid = (Accept) lastOpponentAction;
            return this.getUtilitySpace().getUtility(acceptanceBid.getBid()).doubleValue();

        }

        // General Offer Case
        Bid lastOpponentBid = length > 1 ? ((Offer) lastOpponentAction).getBid() : null;
        Bid lastAgentBid = length > 1 ? ((Offer) currHistory.get(length - 2)).getBid() : null;
        BigDecimal ZERO_UTILITY = new BigDecimal(0);
        BigDecimal utility1 = lastAgentBid != null ? this.getUtilitySpace().getUtility(lastAgentBid) : ZERO_UTILITY;
        BigDecimal utility2 = lastOpponentBid != null ? this.getUtilitySpace().getUtility(lastOpponentBid)
                : ZERO_UTILITY;
        BigDecimal mean = utility1.add(utility2).divide(new BigDecimal(2));
        return mean.doubleValue();
    }

    // protected Double evaluateLast2Bids() {
    //     ArrayList<Action> currHistory = this.getHistory();
    //     int length = currHistory.size();
    //     Action lastOpponentAction = currHistory.get(length - 1);
    //     if (lastOpponentAction instanceof Accept) {
    //         // In case last opponent action was an acceptance
    //         Accept acceptanceBid = (Accept) lastOpponentAction;
    //         return this.getUtilitySpace().getUtility(acceptanceBid.getBid()).doubleValue();

    //     }

    //     // General Offer Case
    //     Bid lastOpponentBid = length > 1 ? ((Offer) lastOpponentAction).getBid() : null;
    //     Bid lastAgentBid = length > 1 ? ((Offer) currHistory.get(length - 2)).getBid() : null;
    //     BigDecimal ZERO_UTILITY = new BigDecimal(0);
    //     BigDecimal utility1 = lastAgentBid != null ? this.getUtilitySpace().getUtility(lastAgentBid) : ZERO_UTILITY;
    //     BigDecimal utility2 = lastOpponentBid != null ? this.getUtilitySpace().getUtility(lastOpponentBid)
    //             : ZERO_UTILITY;
    //     BigDecimal mean = utility1.multiply(utility2);
    //     return mean.doubleValue();
    // }

    @Override
    public boolean equals(Object obj) {
        HistoryState secondState = (HistoryState) obj;
        return this.getHistory().equals(secondState.getHistory());
    }

}
