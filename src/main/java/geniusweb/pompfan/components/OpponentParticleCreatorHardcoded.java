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
import geniusweb.pompfan.opponents.OppUtilityTFTOpponentPolicy;
import geniusweb.pompfan.opponents.OwnUtilityTFTOpponentPolicy;
import geniusweb.pompfan.opponents.RandomOpponentPolicy;
import geniusweb.pompfan.opponents.SelfishOpponentPolicy;
import geniusweb.pompfan.opponents.TimeDependentOpponentPolicy;
import geniusweb.profile.utilityspace.UtilitySpace;
import geniusweb.profileconnection.ProfileConnectionFactory;
import geniusweb.profileconnection.ProfileInterface;
import geniusweb.progress.Progress;
import geniusweb.references.PartyWithProfile;
import tudelft.utilities.logging.ReportToLogger;

public abstract class OpponentParticleCreatorHardcoded {

    public static List<AbstractPolicy> generateOpponentParticles(UtilitySpace uSpace, Long numParticlesPerOpponent, Progress progress) throws URISyntaxException, IOException, DeploymentException {
        Domain domain = uSpace.getDomain();
        AllBidsList bidspace = new AllBidsList(domain);
        List<AbstractPolicy> listOfOpponents = new ArrayList<AbstractPolicy>();

        // hardcoded profile/utility
        ReportToLogger reporter = new ReportToLogger("HardcodedProfile");
        String profileString = "file:src/test/resources/party2.json";
        // String profileString = "file:src/test/resources/abstract2.json";
        // String profileString = "file:src/test/resources/flightbooking2.json";
        URI profile = new URI(profileString);
        ProfileInterface profileint = ProfileConnectionFactory.create(
                profile, reporter);
        UtilitySpace utilitySpace = ((UtilitySpace) profileint.getProfile());
        
        // AbstractPolicy correctOpponent = new HardLinerOpponentPolicy(utilitySpace, "ExactHardliner", 0.0);
        // AbstractPolicy correctOpponent = new BoulwareOpponentPolicy(utilitySpace, "ExactBoulware", 0.2);
        // AbstractPolicy correctOpponent = new LinearOpponentPolicy(utilitySpace, "ExactLinear", 1.0);
        // AbstractPolicy correctOpponent = new ConcederOpponentPolicy(utilitySpace, "ExactConceder", 2.0);
        // AbstractPolicy correctOpponent = new OwnUtilityTFTOpponentPolicy(utilitySpace, "ExactOwnTFT");
        // AbstractPolicy correctOpponent = new OppUtilityTFTOpponentPolicy(utilitySpace, "ExactOppTFT", progress);
        // listOfOpponents.add(correctOpponent);
        // can we find the opponent?
        // for (int cnt = 0; cnt < numParticlesPerOpponent; cnt++) {
        //     listOfOpponents.add(new OwnUtilityTFTOpponentPolicy(domain));
        //     listOfOpponents.add(new OppUtilityTFTOpponentPolicy(domain, progress));
            
        //     listOfOpponents.add(new HardLinerOpponentPolicy(domain));
        //     listOfOpponents.add(new BoulwareOpponentPolicy(domain));
        //     listOfOpponents.add(new LinearOpponentPolicy(domain));
        //     listOfOpponents.add(new ConcederOpponentPolicy(domain));
        // }


        // same profile; get strategy?
        // for (int cnt = 0; cnt < numParticlesPerOpponent; cnt++) {
        //     listOfOpponents.add(new OwnUtilityTFTOpponentPolicy(utilitySpace, "OwnTFT"));
        //     listOfOpponents.add(new OppUtilityTFTOpponentPolicy(utilitySpace, "OppTFT", progress));
            
        //     listOfOpponents.add(new HardLinerOpponentPolicy(utilitySpace, "Hardliner", 0.0));
        //     listOfOpponents.add(new BoulwareOpponentPolicy(utilitySpace, "Boulware", 0.2));
        //     listOfOpponents.add(new LinearOpponentPolicy(utilitySpace, "Linear", 1));
        //     listOfOpponents.add(new ConcederOpponentPolicy(utilitySpace, "Conceder", 2.0));
        //     listOfOpponents.add(new RandomOpponentPolicy(utilitySpace, "Random"));
        // }

        // same strategy; get profile?
        // AbstractPolicy correctOpponent = new OppUtilityTFTOpponentPolicy(utilitySpace, "ExactOppTFT", progress);
        // AbstractPolicy correctOpponent = new OwnUtilityTFTOpponentPolicy(utilitySpace, "ExactOwnTFT");
        // AbstractPolicy correctOpponent = new BoulwareOpponentPolicy(utilitySpace, "ExactBoulware", 0.2);
        // AbstractPolicy correctOpponent = new LinearOpponentPolicy(utilitySpace, "ExactLinear", 1.0);
        // AbstractPolicy correctOpponent = new ConcederOpponentPolicy(utilitySpace, "ExactConceder", 2.0);

        // listOfOpponents.add(correctOpponent);
        // for (int cnt = 0; cnt < numParticlesPerOpponent*5; cnt++) {
        // listOfOpponents.add(new OwnUtilityTFTOpponentPolicy(domain));
        // listOfOpponents.add(new OppUtilityTFTOpponentPolicy(domain, progress));
        // listOfOpponents.add(new BoulwareOpponentPolicy(domain));
        // listOfOpponents.add(new LinearOpponentPolicy(domain));
        // listOfOpponents.add(new ConcederOpponentPolicy(domain));
        // }

        // listOfOpponents.add(correctOpponent);
        // does the opponent matter?
        for (int cnt = 0; cnt < numParticlesPerOpponent; cnt++) {
            // listOfOpponents.add(new HardLinerOpponentPolicy(utilitySpace, "ExactHardliner", 0.0));
            // listOfOpponents.add(new BoulwareOpponentPolicy(utilitySpace, "ExactBoulware", 0.2));
            // listOfOpponents.add(new LinearOpponentPolicy(utilitySpace, "ExactLinear", 1.0));
            // listOfOpponents.add(new ConcederOpponentPolicy(utilitySpace, "ExactConceder", 2.0));
            // listOfOpponents.add(new OwnUtilityTFTOpponentPolicy(utilitySpace, "ExactOwnTFT"));
            // listOfOpponents.add(new OppUtilityTFTOpponentPolicy(utilitySpace, "ExactOppTFT", progress));
            // listOfOpponents.add(new OppUtilityTFTOpponentPolicy(domain, progress));
            listOfOpponents.add(new OwnUtilityTFTOpponentPolicy(domain));
            
            listOfOpponents.add(new HardLinerOpponentPolicy(domain));
            listOfOpponents.add(new BoulwareOpponentPolicy(domain));
            listOfOpponents.add(new LinearOpponentPolicy(domain));
            listOfOpponents.add(new ConcederOpponentPolicy(domain));
            System.out.println("Particle Group Done");
        }


        // general experiment
        // for (int cnt = 0; cnt < numParticlesPerOpponent; cnt++) {
        //     // listOfOpponents.add(new OppUtilityTFTOpponentPolicy(domain, progress));
        //     listOfOpponents.add(new OwnUtilityTFTOpponentPolicy(domain));

        //     listOfOpponents.add(new HardLinerOpponentPolicy(domain));
        //     listOfOpponents.add(new BoulwareOpponentPolicy(domain));
        //     listOfOpponents.add(new LinearOpponentPolicy(domain));
        //     listOfOpponents.add(new ConcederOpponentPolicy(domain));
        // }

        
        
        listOfOpponents = listOfOpponents.stream().map(opponent -> opponent.setBidspace(bidspace))
                .collect(Collectors.toList());
        return listOfOpponents;
    }
}
