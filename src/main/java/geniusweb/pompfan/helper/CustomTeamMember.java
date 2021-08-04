package geniusweb.pompfan.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import geniusweb.protocol.session.SessionSettings;
import geniusweb.protocol.session.TeamInfo;
import geniusweb.protocol.tournament.ProfileList;
import geniusweb.protocol.tournament.Team;
import geniusweb.protocol.tournament.allpermutations.AllPermutationsSettings;
import tudelft.utilities.immutablelist.ImmutableList;

public class CustomTeamMember{
	public String partyRef;
	public HashMap<String, Object> parameters;

    public CustomTeamMember(String partyRef) {
        super();
        this.partyRef = partyRef;
        parameters.put("persistentstate", UUID.fromString(partyRef).toString());
    }
}
