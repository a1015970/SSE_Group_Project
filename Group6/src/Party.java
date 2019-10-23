import java.util.ArrayList;
import java.util.List;

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
	
}
