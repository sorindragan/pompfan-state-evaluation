package geniusweb.custom.opponents;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import geniusweb.bidspace.AllBidsList;
import geniusweb.issuevalue.Domain;
import geniusweb.profile.utilityspace.UtilitySpace;

public abstract class OpponentParticleCreator {

    public static List<AbstractPolicy> generateOpponentParticles(UtilitySpace uSpace, Long numParticlesPerOpponent) {
        Domain domain = uSpace.getDomain();
        AllBidsList bidspace = new AllBidsList(domain);
        List<AbstractPolicy> listOfOpponents = new ArrayList<AbstractPolicy>();
        for (int cnt = 0; cnt < numParticlesPerOpponent; cnt++) {
            listOfOpponents.add(new AntagonisticOpponentPolicy(uSpace));
            listOfOpponents.add(new SelfishOpponentPolicy(domain));
            listOfOpponents.add(new TimeDependentOpponentPolicy(domain));
            listOfOpponents.add(new HardLinerOpponentPolicy(domain));
            listOfOpponents.add(new ConcederOpponentPolicy(domain));
            listOfOpponents.add(new BoulwareOpponentPolicy(domain));
        }
        listOfOpponents = listOfOpponents.stream().map(opponent -> opponent.setBidspace(bidspace))
                .collect(Collectors.toList());
        return listOfOpponents;
    }
}
