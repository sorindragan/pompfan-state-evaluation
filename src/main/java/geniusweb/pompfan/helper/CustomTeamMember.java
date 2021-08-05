package geniusweb.pompfan.helper;

import java.util.HashMap;
import java.util.UUID;

public class CustomTeamMember{
	public String partyRef;
	public HashMap<String, Object> parameters;

    public CustomTeamMember(String partyRef) {
        super();
        this.partyRef = partyRef;
        parameters.put("persistentstate", UUID.fromString(partyRef).toString());
    }
}
