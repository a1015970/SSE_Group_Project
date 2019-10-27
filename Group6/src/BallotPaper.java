import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BallotPaper {
	public final List<Party> partyList;
	public final List<Candidate> ungroupedCandidates;
	public  List<Integer> excludedCandidates;

	// interim constructor
	// final version -read a file (CSV or JSON?) and create candidates and parties
	public BallotPaper(List<Party> partyList, List<Candidate> ungroupedCandidates) {
		this.partyList = partyList;
		this.ungroupedCandidates = ungroupedCandidates;
		this.excludedCandidates = new ArrayList<Integer>();
	}
	
	
	// construct ballot paper by reading CSV
	public BallotPaper(String filename, String state) {
		BufferedReader reader;
		List<String> partyNames = new ArrayList<String>();
		this.partyList = new ArrayList<Party>();
		this.ungroupedCandidates = new ArrayList<Candidate>();
		this.excludedCandidates = new ArrayList<Integer>();
		int partyUID = 0;
		int candidateUID = 0;
		try {
			reader = new BufferedReader(new FileReader(filename));
			String line = reader.readLine();
			while (line != null) {
				String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
				if (parts[0].equals(state)) {
					System.out.println(line);
					String partyStr = parts[1];
					if (partyStr.startsWith("\"")) {
						partyStr = partyStr.substring(1, partyStr.length()-1);
					}
					String candidateStr = parts[2] + ", " + parts[3];
					if (partyStr.equals("Independent")) {
						ungroupedCandidates.add(new Candidate(candidateStr, "Independent", candidateUID));
						candidateUID++;
					} else {
						if (!partyNames.contains(partyStr)) {
							partyNames.add(partyStr);
							partyList.add(new Party(partyStr, partyUID));
							partyUID++;
						}
						Party thisParty = partyList.get(0);
						for (Party p : partyList) {
							if (p.partyName.equals(partyStr)) {
								thisParty = p;
								break;
							}
						}
						thisParty.candidates.add(new Candidate(candidateStr, partyStr, candidateUID));
						candidateUID++;
					}
				}
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
	
	public int getNumCandidates() {
		int num = ungroupedCandidates.size();
		for (Party p : partyList) {
			num += p.candidates.size();
		}
		return num;
	}
	
	
	public boolean isCandidateExcluded(int uid) {
		return this.excludedCandidates.contains(uid);
	}
	
	
	public Party getPartyById(Integer id) throws PartyNotFoundException {
		for (Party p : partyList) {
			if (p.uid == id) {
				return p;
			}
		}
		throw new PartyNotFoundException("Party not found - id = " + id);
	}
	
	@Override
	public String toString() {
		String str = "";
		for (Party p : partyList) {
			str += p.uid + " Party: " + p.partyName + "\n";
			for (Candidate c : p.candidates) {
				str += "  " + c.uid + " " + c.candidateName + "\n";
			}
		}
		str += "Independent" + "\n";
		for (Candidate c : ungroupedCandidates) {
			str += "  " + c.uid + " " + c.candidateName + "\n";
		}
		return str;
	}
	
	public static void main(String[] args) throws CandidateNotFoundException {
		BallotPaper test = new BallotPaper("\\Users\\Chris\\git\\SSE_Group_Project\\SenateCandidates2016RandomOrder.csv", "SA");
		System.out.println(test);
		for (int i = 0; i < test.getNumCandidates(); i++) {
			Candidate c = test.getCandidateById(i);
			System.out.println(i + "  " + c.uid + "  " + c.party + "  " + c.candidateName);
		}
	}
}
