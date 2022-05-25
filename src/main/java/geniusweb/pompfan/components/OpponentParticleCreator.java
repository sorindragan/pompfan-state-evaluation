package geniusweb.pompfan.components;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import geniusweb.bidspace.AllBidsList;
import geniusweb.issuevalue.Domain;
import geniusweb.pompfan.particles.AbstractPolicy;
import geniusweb.pompfan.particles.AntagonisticOpponentPolicy;
import geniusweb.pompfan.particles.BoulwareOpponentPolicy;
import geniusweb.pompfan.particles.ConcederOpponentPolicy;
import geniusweb.pompfan.particles.HardLinerOpponentPolicy;
import geniusweb.pompfan.particles.LinearOpponentPolicy;
import geniusweb.pompfan.particles.OppUtilityTFTOpponentPolicy;
import geniusweb.pompfan.particles.OwnUtilityTFTOpponentPolicy;
import geniusweb.pompfan.particles.SelfishOpponentPolicy;
import geniusweb.pompfan.particles.TimeDependentOpponentPolicy;
import geniusweb.profile.utilityspace.UtilitySpace;
import geniusweb.progress.Progress;

public abstract class OpponentParticleCreator {

    public static List<AbstractPolicy> generateOpponentParticles(UtilitySpace uSpace, Long numParticlesPerOpponent, Progress progress) {
        Domain domain = uSpace.getDomain();
        AllBidsList bidspace = new AllBidsList(domain);
        List<AbstractPolicy> listOfOpponents = new ArrayList<AbstractPolicy>();
        // TODO: fix arguments
        for (int cnt = 0; cnt < numParticlesPerOpponent; cnt++) {
            listOfOpponents.add(new OwnUtilityTFTOpponentPolicy(domain));
            listOfOpponents.add(new OppUtilityTFTOpponentPolicy(domain, progress));
            listOfOpponents.add(new AntagonisticOpponentPolicy(uSpace));
            // listOfOpponents.add(new SelfishOpponentPolicy(domain));
            // listOfOpponents.add(new HardLinerOpponentPolicy(domain));
            listOfOpponents.add(new BoulwareOpponentPolicy(domain));
            listOfOpponents.add(new LinearOpponentPolicy(domain));
            // listOfOpponents.add(new TimeDependentOpponentPolicy(domain));
            listOfOpponents.add(new ConcederOpponentPolicy(domain));
        }
        listOfOpponents = listOfOpponents.stream().map(opponent -> opponent.setBidspace(bidspace))
                .collect(Collectors.toList());
        return listOfOpponents;
    }
}
