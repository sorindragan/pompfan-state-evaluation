package geniusweb.pompfan.evaluators;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({ @Type(value = Last2BidsMeanUtilityEvaluator.class),
        @Type(value = Last2BidsProductUtilityEvaluator.class), @Type(value = RandomEvaluator.class),
        @Type(value = L2BWithOppModelProductUtilityEvaluator.class) })
public interface IEvalFunction<T> {
    public Double evaluate(T state);
}
