package geniusweb.opponents;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;

import javax.websocket.DeploymentException;

import geniusweb.actions.Accept;
import geniusweb.actions.ActionWithBid;
import geniusweb.actions.Offer;
import geniusweb.bidspace.Interval;
import geniusweb.inform.Agreements;
import geniusweb.inform.Inform;
import geniusweb.inform.Settings;
import geniusweb.issuevalue.Bid;
import tudelft.utilities.immutablelist.ImmutableList;

public class SelfishAgent extends GenericOpponent<Object>{


    private ImmutableList<Bid> possibleBids;

    @Override
    protected void initializeVariables(Settings settings) throws DeploymentException {
        super.initializeVariables(settings);
        this.setPossibleBids(this.getBidsWithUtility().getBids(new Interval(new BigDecimal("0.75"), BigDecimal.ONE)));
    }

    public ImmutableList<Bid> getPossibleBids() {
        return possibleBids;
    }

    public void setPossibleBids(ImmutableList<Bid> possibleBids) {
        this.possibleBids = possibleBids;
    }

    @Override
    protected void processOpponentAction(ActionWithBid action) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void processAgreements(Agreements agreements) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void processNonAgreements(Agreements agreements) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected ActionWithBid myTurn(Object param) {
        ActionWithBid action;
        List<ActionWithBid> oppHistory = this.getOpponentHistory();

        Bid lastOpponentsBid = oppHistory.get(oppHistory.size()).getBid();
        Bid selfishBid = this.getPossibleBids().get(this.getRandom().nextInt(this.getPossibleBids().size().intValue()));

        ActionWithBid result = this.getUtilitySpace().isPreferredOrEqual(lastOpponentsBid, selfishBid)
                ? new Accept(this.getMe(), lastOpponentsBid)
                : new Offer(this.getMe(), selfishBid);
        return result;
    }
    
}
