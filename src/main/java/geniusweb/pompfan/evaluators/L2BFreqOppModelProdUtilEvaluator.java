package geniusweb.pompfan.evaluators;

import java.util.ArrayList;

import geniusweb.actions.Action;
import geniusweb.issuevalue.Domain;
import geniusweb.pompfan.opponentModels.BetterFreqOppModel;
import geniusweb.profile.utilityspace.UtilitySpace;

public class L2BFreqOppModelProdUtilEvaluator extends L2BAHPOppModelProdUtilEvaluator {

    public L2BFreqOppModelProdUtilEvaluator(UtilitySpace utilitySpace) {
        super(utilitySpace);
    }

    public L2BFreqOppModelProdUtilEvaluator() {
    }

    @Override
    protected void initOppModel(ArrayList<Action> currState, Domain domain) {
        // this.setOppModel(new BetterFreqOppModel(domain, currState, this.getHolder()));
    }

}
