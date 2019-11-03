import java.util.ArrayList;
import java.util.List;

// a representation of a Party in the election
// it has a list of candidates who make up the party
public class Party {
	public final String partyName;
	public final Integer uid;
	public final List<Candidate> candidates;
	
	public Party(String partyName, Integer uid, List<Candidate> candidates) {
		this.partyName = partyName;
		this.uid = uid;
		this.candidates = candidates;
	}

	public Party(String partyName, Integer uid) {
		this.partyName = partyName;
		this.uid = uid;
		this.candidates = new ArrayList<Candidate>();
	}
	
	@Override
	public String toString() {
		return this.partyName;
	}
}
