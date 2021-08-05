package geniusweb.pompfan.helper;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class CustomTeam {
    public ArrayList<CustomTeamMember> Team = new ArrayList<>();

    public CustomTeam(String partyRef) {
        super();
        this.Team.add(new CustomTeamMember(partyRef));
    }

    public ArrayList<CustomTeamMember> getTeam() {
        return Team;
    }

    public void setTeam(ArrayList<CustomTeamMember> team) {
        Team = team;
    }
}
