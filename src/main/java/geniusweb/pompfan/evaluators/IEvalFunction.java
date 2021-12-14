package geniusweb.pompfan.evaluators;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({ @Type(value = Last2BidsMeanUtilityEvaluator.class),
        @Type(value = Last2BidsProductUtilityEvaluator.class), @Type(value = RandomEvaluator.class),
        @Type(value = Last2BidsOneMinusDifferenceUtilityEvaluator.class), @Type(value = Last2BidsMixtInverseDifferenceUtilEvaluator.class),
        @Type(value = Last2BidsMixtMeanUtilityEvaluator.class), @Type(value = Last2BidsMixtProdUtilEvaluator.class),
        @Type(value = L2BFreqOppModelProdUtilEvaluator.class), @Type(value = L2BAHPOppModelProdUtilEvaluator.class),
        @Type(value = L2BEntropyOppModelProdUtilEvaluator.class), @Type(value = OppConcessionUtilityEvaluator.class),
    })
public interface IEvalFunction<T> {
    
    public Double evaluate(T state);
}
