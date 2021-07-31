package geniusweb.custom.opponents;

import geniusweb.issuevalue.Domain;
import geniusweb.opponentmodel.FrequencyOpponentModel;

public class ImitationPolicy extends AbstractPolicy {
    private FrequencyOpponentModel utilityModel = new FrequencyOpponentModel();
    public ImitationPolicy(Domain domain, String name) {
        super(domain, name);
        this.utilityModel = this.utilityModel.with(domain, null);
        // this.utilityModel.getUtility(bid);
    }
    
}
