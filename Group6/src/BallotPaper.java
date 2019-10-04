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
	
	
	public Candidate getCandidateById(Integer id) throws CandidateNotFoundException {
		// first search ungrouped candidates
		for (Candidate c : ungroupedCandidates) {
			if (c.uid == id) {
				return c;
			}
		}
		// then search parties
		for (Party p : partyList) {
			for (Candidate c : p.candidates) {
				if (c.uid == id) {
					return c;
				}
			}
		}
		throw new CandidateNotFoundException("Candidate not found - id = " + id);
	}
	
	
	public Party getPartyById(Integer id) throws PartyNotFoundException {
		for (Party p : partyList) {
			if (p.uid == id) {
				return p;
			}
		}
		throw new PartyNotFoundException("Party not found - id = " + id);
	}
}
