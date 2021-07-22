package geniusweb.custom.junk;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.custom.components.Opponent;
import geniusweb.custom.evaluators.EvaluationFunctionInterface;
import geniusweb.custom.opponents.AbstractPolicy;
import geniusweb.custom.state.HistoryState;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.profile.utilityspace.UtilitySpace;

public class Last2BidsState extends HistoryState {

    public Last2BidsState(UtilitySpace utilitySpace, AbstractPolicy opponent, EvaluationFunctionInterface<Last2BidsState> evaluator) {
        super(utilitySpace, opponent, evaluator);
    }



}
