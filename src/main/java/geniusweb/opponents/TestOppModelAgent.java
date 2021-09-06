package geniusweb.opponents;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import geniusweb.actions.Action;
import geniusweb.actions.ActionWithBid;
import geniusweb.actions.Offer;
import geniusweb.bidspace.AllBidsList;
import geniusweb.bidspace.BidsWithUtility;
import geniusweb.bidspace.Interval;
import geniusweb.inform.Agreements;
import geniusweb.issuevalue.Bid;
import geniusweb.pompfan.opponentModels.AHPFreqWeightedOpponentModel;
import geniusweb.pompfan.opponentModels.AbstractOpponentModel;
import tudelft.utilities.immutablelist.ImmutableList;

public class TestOppModelAgent extends AbstractOpponent {

    private AbstractOpponentModel opponentModel;
    private BidsWithUtility oppBidsWithUtilities;
    private AllBidsList allPossibleBids;
    private boolean DEBUG_OPP_MODEL = true;
    private ActionWithBid lastOppAction;

    public TestOppModelAgent() {
        super();
    }

    @Override
    protected void processOpponentAction(ActionWithBid action) {
        this.lastOppAction = action;
        List<ActionWithBid> history = this.getHistory();

        if (this.getOpponentHistory().size() > 5) {
            this.updateOpponentModel(history);
            if (DEBUG_OPP_MODEL) {
                System.out.println("==================");
                System.out.println(this.opponentModel.toString());
            }

        }

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
        ImmutableList<Bid> bids = this.getBidsWithUtility()
                .getBids(new Interval(new BigDecimal("0.7"), BigDecimal.ONE));
        int selected = this.getRandom().nextInt(bids.size().intValue());
        action = new Offer(this.getMe(), bids.get(selected));
        return action;
    }

    private void updateOpponentModel(List<ActionWithBid> historyActionsWithBid) {
        try {
            this.getReporter().log(Level.INFO, this.getMe().toString() + ": Update OppModel!!!");
            List<Action> history = historyActionsWithBid.stream().map(a -> (Action) a).collect(Collectors.toList());
            this.opponentModel = new AHPFreqWeightedOpponentModel(this.getUtilitySpace().getDomain(), history,
                    this.getMe());
            this.oppBidsWithUtilities = new BidsWithUtility(this.opponentModel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
