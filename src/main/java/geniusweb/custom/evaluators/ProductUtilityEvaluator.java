package geniusweb.custom.evaluators;

import java.math.BigDecimal;
import java.math.BigInteger;

import geniusweb.custom.helper.BidVector;
import geniusweb.custom.state.AbstractState;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.profile.utilityspace.UtilitySpace;

/**
 * MeanUtilityEvaluator
 */
public class ProductUtilityEvaluator implements EvaluationFunctionInterface{

    private UtilitySpace utilitySpace;

    public ProductUtilityEvaluator(UtilitySpace utilitySpace) {
        super();
        this.utilitySpace  = utilitySpace;
    }

    @Override
    public Double evaluate(AbstractState<?> state) {
        return null;
    }

    @Override
    public Double evaluate(AbstractState<?> state, Bid bid) {
        return null;
    }

    @Override
    public Double evaluate(AbstractState<?> state, Bid agentBid, Bid opponenentBid) {
        BigDecimal utility1 = this.utilitySpace.getUtility(agentBid);
        BigDecimal utility2 = this.utilitySpace.getUtility(opponenentBid);
        BigDecimal mean = utility1.multiply(utility2);
        return mean.doubleValue();
    }


    
}