import java.util.List;

public class BallotPaper {
	public final List<Party> partyList;
	public final List<Candidate> ungroupedCandidates;
	
	// interim constructor
	// final version -read a file (CSV or JSON?) and create candidates and parties
	public BallotPaper(List<Party> partyList, List<Candidate> ungroupedCandidates) {
		this.partyList = partyList;
		this.ungroupedCandidates = ungroupedCandidates;
	}
}
