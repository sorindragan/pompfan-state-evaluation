package geniusweb.custom.junk;

import geniusweb.actions.Action;
import geniusweb.actions.PartyId;
import geniusweb.custom.components.Node;
import geniusweb.custom.explorers.AbstractOwnExplorationPolicy;
import geniusweb.custom.state.AbstractState;
import geniusweb.custom.state.StateRepresentationException;
import geniusweb.issuevalue.Bid;
import geniusweb.profile.utilityspace.UtilitySpace;
import geniusweb.progress.Progress;

public class ProgressiveWideningOwnExplorationStrategy extends AbstractOwnExplorationPolicy {

    public ProgressiveWideningOwnExplorationStrategy(UtilitySpace utilitySpace, PartyId id) {
        super(utilitySpace, id);
    }

    // @Override
    public Action chooseAction(Bid lastReceivedBid, Bid lastOwnBid, Node node) {
        // Action a;
        // if (node.getChildren().size() <= (k_a * Math.pow(node.getVisits(), a_a))) {
        //     a = this.chooseAction();
        // } else {
        //     // a = 
        // }

        // return super.chooseAction(lastReceivedBid, lastOwnBid, node);
    }

    @Override
    protected void init() {
        // TODO Auto-generated method stub

    }

}
