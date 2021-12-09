package geniusweb.pompfan.components;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.websocket.DeploymentException;

import geniusweb.bidspace.AllBidsList;
import geniusweb.issuevalue.Domain;
import geniusweb.pompfan.opponents.AbstractPolicy;
import geniusweb.pompfan.opponents.AntagonisticOpponentPolicy;
import geniusweb.pompfan.opponents.BoulwareOpponentPolicy;
import geniusweb.pompfan.opponents.ConcederOpponentPolicy;
import geniusweb.pompfan.opponents.HardLinerOpponentPolicy;
import geniusweb.pompfan.opponents.LinearOpponentPolicy;
import geniusweb.pompfan.opponents.NiceTFTOpponentPolicy;
import geniusweb.pompfan.opponents.OwnUtilityTFTOpponentPolicy;
import geniusweb.pompfan.opponents.SelfishOpponentPolicy;
import geniusweb.pompfan.opponents.TimeDependentOpponentPolicy;
import geniusweb.profile.utilityspace.UtilitySpace;
import geniusweb.profileconnection.ProfileConnectionFactory;
import geniusweb.profileconnection.ProfileInterface;
import geniusweb.references.PartyWithProfile;
import tudelft.utilities.logging.ReportToLogger;

public abstract class OpponentParticleCreatorHardcoded {

    public static List<AbstractPolicy> generateOpponentParticles(UtilitySpace uSpace, Long numParticlesPerOpponent) throws URISyntaxException, IOException, DeploymentException {
        Domain domain = uSpace.getDomain();
        AllBidsList bidspace = new AllBidsList(domain);
        List<AbstractPolicy> listOfOpponents = new ArrayList<AbstractPolicy>();

        // hardcoded profile/utility
        ReportToLogger reporter = new ReportToLogger("HardcodedProfile");
        String profileString = "file:src/test/resources/party2.json";
        URI profile = new URI(profileString);
        ProfileInterface profileint = ProfileConnectionFactory.create(
                profile, reporter);
        UtilitySpace utilitySpace = ((UtilitySpace) profileint.getProfile());
        
        AbstractPolicy correctOpponent = new BoulwareOpponentPolicy(utilitySpace, "ExactOpponentBoulware", 0.2);
        
        listOfOpponents.add(correctOpponent);

        for (int cnt = 0; cnt < numParticlesPerOpponent; cnt++) {
            listOfOpponents.add(new OwnUtilityTFTOpponentPolicy(domain));
            listOfOpponents.add(new NiceTFTOpponentPolicy(domain));
            // listOfOpponents.add(new AntagonisticOpponentPolicy(uSpace));
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
