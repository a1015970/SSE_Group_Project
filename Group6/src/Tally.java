import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// a class with static members and methods to perform a tally of the vote
public class Tally {
	// these fields may need to be set before calling Tally.tallyvOTES()
	public static BallotPaper ballotPaper = null;
	public static List<Vote> voteList = null;
	public static List<Integer> excludedCandidates = new ArrayList<Integer>();
	public static int numCandidatesToElect = 0; 
	// and these fields will be set after the tally is complete
	public static List<Integer> electedCandidates = new ArrayList<Integer>();
	public static List<String> tallyLog = new ArrayList<String>();
	
	// set list of excluded candidates
	public static void setExcludedCandidates(List<Integer> newExcludedCandidates) {
		excludedCandidates = newExcludedCandidates;
	}
	
	// do a single tally of the vote as it stands
	public static Map<Integer, Double> doTally() {
		Map<Integer, Double> tally = new HashMap<Integer, Double>();
		for (Vote v : voteList) {
			if (v.isExhausted) {
				continue; // skip exhausted votes
			}
			int currentPref = v.getCurrentPreferredCandidateId();
			double currentTransferValue = v.getCurrentTransferValue();
			if (!tally.containsKey(currentPref)) {
				tally.put(currentPref, 0.0);
			}
			tally.put(currentPref, currentTransferValue+tally.get(currentPref));
		}
		return tally;
	}
	
	// use a single log() routine so that messages can be printed to the console and also saved for later
	static void log(String s) {
		tallyLog.add(s);
		boolean showLog = true;
		if (showLog) {
			System.out.println(s);
		}
	}
	
	// check all votes to see if their current preference has been excluded - if so, go to next pref
	public static void updatePreferences() {
		for (Vote v : voteList) {
			if (v.isCurrentPreferenceExcluded()) {
				v.transferVoteToNextPreference();
			}
		}
	}
		
	// do the entire vote allocation process
	public static void tallyVotes() {
		log("Started tally");
		try {
			// convert preferences to below line
			log("Convert preference to below line");
			for (Vote v : voteList) {
				v.convertToBelowLineVote();
			}
			// deal with excluded candidates
			if (excludedCandidates.size() > 0) {
				ballotPaper.excludedCandidates = excludedCandidates;
				for (Integer id : excludedCandidates) {
					log("Excluding candidate: " + ballotPaper.getCandidateById(id));
				}
				updatePreferences();
			}
			// start tallying votes and electing or excluding candidates
			int numElected = 0;
			int round = 0;
			double quota = 1 + java.lang.Math.floor(((double) voteList.size())/(1.0+numCandidatesToElect));
			log("Quota is " + quota);
			while (numElected < numCandidatesToElect) {
				log("Tally round " + round);
				log("elected " + numElected + " of " + numCandidatesToElect);
				round++;
				// do tally of current vote
				Map<Integer, Double> tally = Tally.doTally();
				printTally(tally);
				//System.out.println("Hit any key to continue");
				//System.in.read();
				// if the number of candidates remaining in the tally is equal
				// to the number we still need to elect, then elect them all and stop!
				if (tally.keySet().size() == (numCandidatesToElect-numElected)) {
					log("Electing all remaining candidates:");
					for (Integer c: tally.keySet()) {
						log("Elected candidate " + ballotPaper.getCandidateById(c) + " with " + tally.get(c) + " votes");
						numElected++;
						electedCandidates.add(c);
					}
					break;
				}
				// work out which candidates got the largest and smallest vote
				double largestVote = 0;
				double smallestVote = Double.MAX_VALUE;
				int mostPopularCandidate = 0;
				int leastPopularCandidate = 0;
				for (Integer c : tally.keySet()) {
					double v = tally.get(c);
					if (v > largestVote) {
						largestVote = v;
						mostPopularCandidate = c;
					}
					if (v < smallestVote) {
						smallestVote = v;
						leastPopularCandidate = c;
					}
				}
				// if the largest vote getter got more than a quota,
				// elect them and transfer the remainder of their votes
				if (largestVote >= quota) {
					log("Elected candidate " + ballotPaper.getCandidateById(mostPopularCandidate) + " with " + largestVote + " votes");
					ballotPaper.excludedCandidates.add(mostPopularCandidate);
					numElected++;
					electedCandidates.add(mostPopularCandidate);
					double transferValue = (largestVote - quota) / largestVote;
					for (Vote v : voteList) {
						if (v.getCurrentPreferredCandidateId() == mostPopularCandidate) {
							v.reduceTransferValue(transferValue);
							v.transferVoteToNextPreference();
						}
					}
					continue;
				}
				// otherwise, exclude the lowest vote getter and transfer their votes
				log("Excluded candidate " + ballotPaper.getCandidateById(leastPopularCandidate) + " with " + smallestVote + " votes");
				ballotPaper.excludedCandidates.add(leastPopularCandidate);
				updatePreferences();
			}
			log("============================================");
			log("Tally complete");
			for (Integer c : electedCandidates) {
				log("Elected candidate " + ballotPaper.getCandidateById(c));

			}
		} catch (CandidateNotFoundException|PartyNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	// utility function for debugging
	public static void printTally(Map<Integer, Double> tally) throws CandidateNotFoundException {
		System.out.println("Votes: Uid (Candidate Name)");
		System.out.println("============================");
		List<Integer> keySet = new ArrayList<Integer>(tally.keySet());
		Collections.sort(keySet);
		for (Integer i : keySet) {
			System.out.println(tally.get(i) + " : " + i + " (" + ballotPaper.getCandidateById(i) + ")");
		}
	}
	
	// a utility function to convert all votes to below the line.
	public static void convertVotesToBelowLine() {
		for (Vote v : voteList) {
			try {
				v.convertToBelowLineVote();
			} catch (PartyNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	// a simple unit test
	public static void main(String[] args) throws CandidateNotFoundException {
		ballotPaper = new BallotPaper("\\Users\\Chris\\git\\SSE_Group_Project\\SenateCandidates2016RandomOrder.csv", "SA");
		System.out.println(ballotPaper);
		for (int i = 0; i < ballotPaper.getNumCandidates(); i++) {
			Candidate c = ballotPaper.getCandidateById(i);
			System.out.println(i + "  " + c.uid + "  " + c.party + "  " + c.candidateName);
		}
		voteList = new ArrayList<Vote>();
		for (int i = 0; i < 1000; i++) {
			voteList.add(Vote.getRandomVote(ballotPaper));
		}
		for (Vote v : voteList) {
			System.out.println(v);
		}
		
		numCandidatesToElect = 6; //6 for half senate, 12 for full senate
		//convertVotesToBelowLine();
		//Map<Integer, Double> tally = Tally.doTally();
		//printTally(tally);
		tallyVotes();
	}

}
