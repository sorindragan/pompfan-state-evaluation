package geniusweb.pompfan.opponentModels;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import geniusweb.actions.Action;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.issuevalue.Value;
import geniusweb.opponentmodel.OpponentModel;
import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.profile.utilityspace.ValueSetUtilities;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({ @Type(value = WeightedFrequencyOpponentModel.class)})
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public abstract class AbstractOpponentModel implements LinearAdditive, OpponentModel {
	private static final int DECIMALS = 4; // accuracy of our computations.
	private static int serial = 1; // counter for auto name generation
    private String id = UUID.randomUUID().toString();

	private Domain domain;
	private List<Action> history;
	private Map<String, ValueSetUtilities> issueUtilities = new HashMap<>();
	private Map<String, BigDecimal> issueWeights = new HashMap<>();

	public AbstractOpponentModel(Domain domain, List<Action> history) {
		if (domain == null) {
			throw new IllegalStateException("domain is not initialized");
		}
		this.domain = domain;
		this.history = history;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getName() {
		if (domain == null) {
			throw new IllegalStateException("domain is not initialized");
		}
		return "AbstractOppModel" + (serial++) + "For" + domain;
	}

	@Override
	public Map<String, ValueSetUtilities> getUtilities() {
		return Collections.unmodifiableMap(issueUtilities);
	}

	@Override
	public BigDecimal getWeight(String issue) {
		return issueWeights.get(issue);
	}

	@Override
	public Map<String, BigDecimal> getWeights() {
		return Collections.unmodifiableMap(issueWeights);
	}

	@Override
	public Domain getDomain() {
		return this.domain;
	}

	@Override
	public Bid getReservationBid() {
		return null;
	}


	public void setDomain(Domain domain) {
		this.domain = domain;
	}

	public List<Action> getHistory() {
		return history;
	}

	public void setHistory(List<Action> history) {
		this.history = history;
	}

	public Integer getTotalBids() {
		return this.getHistory().size();
	}

	public void setUtilities(Map<String, ValueSetUtilities> issueUtilities) {
		this.issueUtilities = issueUtilities;
	}

	/**
	 * 
	 * @param issue the issue to get weighted util for
	 * @param value the issue value to use (typically coming from a bid)
	 * @return weighted util of just the issue value:
	 *         issueUtilities[issue].utility(value) * issueWeights[issue)
	 */
	protected BigDecimal util(String issue, Value value) {
		return issueWeights.get(issue).multiply(issueUtilities.get(issue).getUtility(value));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((domain == null) ? 0 : domain.hashCode());
		result = prime * result + ((issueUtilities == null) ? 0 : issueUtilities.hashCode());
		result = prime * result + ((issueWeights == null) ? 0 : issueWeights.hashCode());
		result = prime * result + ((this.getName() == null) ? 0 : this.getName().hashCode());
		result = prime * result + ((this.getReservationBid() == null) ? 0 : this.getReservationBid().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractOpponentModel other = (AbstractOpponentModel) obj;
		if (domain == null) {
			if (other.domain != null)
				return false;
		} else if (!domain.equals(other.domain))
			return false;
		if (issueUtilities == null) {
			if (other.issueUtilities != null)
				return false;
		} else if (!issueUtilities.equals(other.issueUtilities))
			return false;
		if (issueWeights == null) {
			if (other.issueWeights != null)
				return false;
		} else if (!issueWeights.equals(other.issueWeights))
			return false;
		if (this.getName() == null) {
			if (other.getName() != null)
				return false;
		} else if (!this.getName().equals(other.getName()))
			return false;
		if (this.getReservationBid() == null) {
			if (other.getReservationBid() != null)
				return false;
		} else if (!this.getReservationBid().equals(other.getReservationBid()))
			return false;
		return true;
	}

	public Map<String, ValueSetUtilities> getIssueUtilities() {
		return issueUtilities;
	}

	public void setIssueUtilities(Map<String, ValueSetUtilities> issueUtilities) {
		this.issueUtilities = issueUtilities;
	}

	public Map<String, BigDecimal> getIssueWeights() {
		return issueWeights;
	}

	public void setIssueWeights(Map<String, BigDecimal> issueWeights) {
		this.issueWeights = issueWeights;
	}

}
