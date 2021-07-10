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

public class Last2BidsState extends HistoryState {

    public Last2BidsState(UtilitySpace utilitySpace, AbstractPolicy opponent) {
        super(utilitySpace, opponent);
    }

    @Override
    public Double evaluate() {
        return super.evaluateLast2Bids();
        // int length = this.getHistory().size();
        // Bid agentBid = ((Offer) this.getHistory().get(length-1)).getBid();
        // Bid opponenentBid = ((Offer) this.getHistory().get(length)).getBid();
        // BigDecimal utility1 = this.getUtilitySpace().getUtility(agentBid);
        // BigDecimal utility2 = this.getUtilitySpace().getUtility(opponenentBid);
        // BigDecimal mean = utility1.add(utility2).divide(new BigDecimal(2));
        // return mean.doubleValue();
    }

}
