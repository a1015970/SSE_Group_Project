
public class Candidate {
	public final String candidateName;
	public final String party;
	public Integer uid;
	
	public Candidate(String candidateName, String party, Integer uid) {
		this.candidateName = candidateName;
		this.party = party;
		this.uid = uid;
	}

	@Override
	public String toString() {
		return this.candidateName;
	}
}
