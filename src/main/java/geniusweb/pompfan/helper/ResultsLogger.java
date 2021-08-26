package geniusweb.pompfan.helper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.websocket.DeploymentException;

import geniusweb.actions.PartyId;
import geniusweb.inform.Agreements;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Value;
import geniusweb.profile.utilityspace.UtilitySpace;
import geniusweb.profileconnection.ProfileConnectionFactory;
import geniusweb.profileconnection.ProfileInterface;
import geniusweb.protocol.NegoState;
import geniusweb.protocol.session.SessionResult;
import geniusweb.protocol.session.SessionState;
import geniusweb.protocol.tournament.allpermutations.AllPermutationsState;
import geniusweb.references.Parameters;
import geniusweb.references.PartyWithProfile;
import geniusweb.references.ProfileRef;
import tudelft.utilities.logging.ReportToLogger;

public class ResultsLogger {
    private String startTime;
    private String endTime;
    private List<Result> results = new ArrayList<Result>();
    private NegoState state = null;
    private Integer sessionNum = 0;

    public ResultsLogger(NegoState state, String temporalState) {
        super();
        this.state = state;
        this.setStartTime(temporalState);
        this.generateResults(this.state);
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    private void generateResults(NegoState state) {
        List<SessionResult> allSessions = new ArrayList<SessionResult>();
        if (state instanceof SessionState) {
            SessionState sessionState = (SessionState) state;
            allSessions.add(sessionState.getResult());
        }

        if (state instanceof AllPermutationsState) {
            AllPermutationsState tournamentStates = (AllPermutationsState) state;
            allSessions.addAll(tournamentStates.getResults());
        }

        for (SessionResult sess : allSessions) {
            this.sessionNum++;
            Agreements agreements = sess.getAgreements();
            Map<PartyId, PartyWithProfile> allParties = sess.getParticipants();
            Map<PartyId, Bid> allAgreements = agreements.getMap();
            List<Result> collectedResults = allParties.keySet().stream()
                    .map(k -> new Result(this.sessionNum, k, allParties.get(k), allAgreements.get(k))
                            .setSessionStart(this.startTime).computeUtility())
                    .collect(Collectors.toList());
            results.addAll(collectedResults);
        }

    }

    public NegoState getState() {
        return state;
    }

    public void setState(NegoState state) {
        this.state = state;
    }

    public List<Result> getResults() {
        return results;
    }

    public void setResults(List<Result> results) {
        this.results = results;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String temporalState) {
        this.startTime = temporalState;
    }

    public class Result {

        private String party;
        private Double utility;
        private PartyWithProfile pwp;
        private Bid aggreeBid;
        private Map<String, Value> bid;
        private Integer session;

        private String version;
        private Parameters params;
        private ReportToLogger reporter;
        private ProfileRef profile;
        private String sessionStart;

        public Result(Integer sessionNum, PartyId partyId, PartyWithProfile pwp, Bid aggreeBid) {
            this.setAggreeBid(aggreeBid);
            this.setSession(sessionNum);
            this.setVersion(partyId.getName());
            this.setPwp(pwp);
            String[] rawPartyString = pwp.getParty().getPartyRef().getURI().toString().split("\\.");
            this.setParty(rawPartyString[rawPartyString.length - 1]);
            HashMap<String, Value> emptyTemp = new HashMap<String, Value>();
            this.setBid(this.getAggreeBid() != null ? this.getAggreeBid().getIssueValues() : emptyTemp);
            this.setParams(pwp.getParty().getParameters());
            this.reporter = new ReportToLogger(this.getPwp().toString());
        }

        public Bid getAggreeBid() {
            return aggreeBid;
        }

        public void setAggreeBid(Bid aggreeBid) {
            this.aggreeBid = aggreeBid;
        }

        public PartyWithProfile getPwp() {
            return pwp;
        }

        public void setPwp(PartyWithProfile pwp) {
            this.pwp = pwp;
        }

        public String getSessionStart() {
            return sessionStart;
        }

        public Result setSessionStart(String sessionStart) {
            this.sessionStart = sessionStart;
            return this;
        }

        private Result computeUtility() {
            try {
                this.profile = this.getPwp().getProfile();
                ProfileInterface profileint = ProfileConnectionFactory.create(this.profile.getURI(), this.reporter);
                UtilitySpace utilitySpace = ((UtilitySpace) profileint.getProfile());
                this.utility = this.getAggreeBid() != null ? utilitySpace.getUtility(this.getAggreeBid()).doubleValue()
                        : 0.0;
            } catch (IOException e) {
                this.utility = -1.0;
                e.printStackTrace();
                return this;
            } catch (DeploymentException e) {
                this.utility = -1.0;
                e.printStackTrace();
                return this;
            } catch (IllegalArgumentException e) {
                this.reporter.log(Level.WARNING, "Couldn't create profile from: " + this.profile.getURI());
                this.utility = -2.0;
                e.printStackTrace();
                return this;
            }
            return this;
        }

        public Integer getSession() {
            return session;
        }

        public void setSession(Integer session) {
            this.session = session;
        }

        public Parameters getParams() {
            return params;
        }

        public void setParams(Parameters params) {
            this.params = params;
        }

        public String getParty() {
            return party;
        }

        public void setParty(String party) {
            this.party = party;
        }

        public Double getUtility() {
            return utility;
        }

        public void setUtility(Double util) {
            this.utility = util;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public Map<String, Value> getBid() {
            return bid;
        }

        public void setBid(Map<String, Value> map) {
            this.bid = map;
        }

    }
}
