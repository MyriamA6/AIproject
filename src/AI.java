import java.util.*;
import java.util.Map.Entry;


/**
 * Class used to model the set of belief states already visited and to keep track of their values (in order to avoid visiting multiple times the same states)
 */
class ExploredSet{
	TreeMap<BeliefState, Float> exploredSet;
	
	/**
	 * construct an empty set
	 */
	public ExploredSet() {
		this.exploredSet = new TreeMap<BeliefState, Float>();
	}
	
	/**
	 * Search if a given state belongs to the explored set and returns its values if that is the case
	 * @param state the state for which the search takes place
	 * @return the value of the state if it belongs to the set, and null otherwise
	 */
	public Float get(BeliefState state) {
		Entry<BeliefState, Float> entry = this.exploredSet.ceilingEntry(state);
		if(entry == null || state.compareTo(entry.getKey()) != 0) {
			return null;
		}
		return entry.getValue() * state.probaSum() / entry.getKey().probaSum();
	}
	
	/**
	 * Put a belief state and its corresponding value into the set
	 * @param beliefState the belief state to be added
	 * @param value the
	 */
	public void put(BeliefState beliefState, float value) {
		this.exploredSet.put(beliefState, value);
	}
}

/**
 * Class used to store all possible results of performing an action at a given belief state
 */
class Results implements Iterable<BeliefState>{
	TreeMap<String, BeliefState> results;
	
	public Results(){
		this.results = new TreeMap<String, BeliefState>();
	}
	
	/**
	 * Return the belief state of the result that correspond to a given percept
	 * @param percept String that describe what is visible on the board for player 2
	 * @return belief state corresponding percept, or null if such a percept is not possible
	 */
	public BeliefState get(String percept) {
		return this.results.get(percept);
	}
	
	public void put(String s, BeliefState state) {
		this.results.put(s, state);
	}
	
	public Iterator<BeliefState> iterator(){
		return this.results.values().iterator();
	}
}

/**
 * Class used to represent a belief state i.e., a set of possible states the agent may be in
 */
class BeliefState implements Comparable<BeliefState>, Iterable<GameState>{
	private byte[] isVisible;
	
	private TreeSet<GameState> beliefState;
	
	private int played;
	
	public BeliefState() {
		this.beliefState = new TreeSet<GameState>();
		this.isVisible = new byte[6];
		for(int i = 0; i < 6; i++) {
			this.isVisible[i] = Byte.MIN_VALUE;
		}
		this.played = 0;
	}
	
	public BeliefState(byte[] isVisible, int played) {
		this();
		for(int i = 0; i < 6; i++) {
			this.isVisible[i] = isVisible[i];
		}
		this.played = played;
	}
	
	public void setStates(BeliefState beliefState) {
		this.beliefState = beliefState.beliefState;
		for(int i = 0; i < 6; i++) {
			this.isVisible[i] = beliefState.isVisible[i];
		}
		this.played = beliefState.played;
	}
	
	public boolean contains(GameState state) {
		return this.beliefState.contains(state);
	}

	/**
	 * returns the number of states in the belief state
	 * @return number of state
	 */
	public int size() {
		return this.beliefState.size();
	}
	
	public void add(GameState state) {
		if(!this.beliefState.contains(state)) {
			this.beliefState.add(state);
		}
		else {
			GameState copy = this.beliefState.floor(state);
			copy.addProba(state.proba());
		}
	}
	
	/**
	 * Compute the possible results from a given believe state, after the opponent perform an action. This function souhd be used only when this is the turn of the opponent.
	 * @return an objet of class result containing all possible result of an action performed by the opponent if this is the turn of the opponent, and null otherwise.
	 */
	public Results predict(){
		if(this.turn()) {
			Results tmstates = new Results();
			for(GameState state: this.beliefState) {
				RandomSelector rs = new RandomSelector();
				ArrayList<Integer> listColumn = new ArrayList<Integer>();
				ArrayList<Integer> listGameOver = new ArrayList<Integer>();
				int minGameOver = Integer.MAX_VALUE;
				for(int column = 0; column < 7; column++) {
					if(!state.isFull(column)) {
						GameState copy = state.copy();
						copy.putPiece(column);
						if(copy.isGameOver()) {
							listColumn.clear();
							listColumn.add(column);
							rs = new RandomSelector();
							rs.add(1);
							break;
						}
						int nbrGameOver = 0;
						for(int i = 0; i < 7; i++) {
							if(!copy.isFull(i)) {
								GameState copycopy = copy.copy();
								copycopy.putPiece(i);
								if(copycopy.isGameOver()) {
									nbrGameOver++;
								}
							}
						}
						if(nbrGameOver == 0) {
							rs.add(ProbabilisticOpponentAI.heuristicValue(state, column));
							listColumn.add(column);
						}
						else {
							if(minGameOver > nbrGameOver) {
								minGameOver = nbrGameOver;
								listGameOver.clear();
								listGameOver.add(column);
							}
							else {
								if(minGameOver == nbrGameOver) {
									listGameOver.add(column);
								}
							}
						}
					}
				}
				int index = 0;
				if(listColumn.isEmpty()) {
					for(int column: listGameOver) {
						listColumn.add(column);
						rs.add(1);
					}
				}
				for(int column: listColumn) {
					GameState copy = state.copy();
					if(!copy.isFull(column)) {
						byte[] tab = new byte[6];
						for(int i = 0; i < 6; i++) {
							tab[i] = this.isVisible[i];
						}
						copy.putPiece(column);
						if(copy.isGameOver()) {
							for(int i = 0; i < 6; i++) {
								for(int j = 0; j < 7; j++) {
									BeliefState.setVisible(i, j, true, tab);
								}
							}
						}
						else {
							boolean isVisible = copy.isGameOver() || copy.isFull(column);
							BeliefState.setVisible(5, column, isVisible, tab);
							for(int row = 4; row > -1; row--) {
								isVisible = isVisible || copy.content(row, column) == 2;
								BeliefState.setVisible(row, column, isVisible, tab);
							}
						}
						String s = "";
						char c = 0;
						for(int i = 0; i < 6; i++) {
							int val = tab[i] + 128;
							s += ((char)(val % 128));
							c += (val / 128) << i;
						}
						s += c;
						copy.multProba(rs.probability(index++));
						BeliefState bs = tmstates.get(s);
						if(bs!= null) {
							bs.add(copy);
						}
						else {
							bs = new BeliefState(tab, this.played + 1);
							bs.add(copy);
							tmstates.put(s, bs);
						}
					}
				}
			}
			return tmstates;
		}
		else {
			return null;
		}
	}
	
	/**
	 * Perform the action corresponding for the player to play a given column, and return the result of this action for each state of the belief state as a Results
	 * @param column index of the column played
	 * @return object of type Results representing all states resulting from playing the column if this is the turn of the player, and null otherwise
	 */
	public Results putPiecePlayer(int column){
		if(!this.turn()) {
			Results tmstates = new Results();
			for(GameState state: this.beliefState) {
				GameState copy = state.copy();
				byte[] tab = new byte[6];
				for(int i = 0; i < 6; i++) {
					tab[i] = this.isVisible[i];
				}
				copy.putPiece(column);
				if(copy.isGameOver()) {
					for(int i = 0; i < 6; i++) {
						for(int j = 0; j < 7; j++) {
							BeliefState.setVisible(i, j, true, tab);
						}
					}
				}
				else {
					boolean isVisible = copy.isFull(column);
					BeliefState.setVisible(5, column, isVisible, tab);
					for(int row = 4; row > -1; row--) {
						isVisible = isVisible || copy.content(row, column) == 2;
						BeliefState.setVisible(row, column, isVisible, tab);
					}
				}
				String s = "";
				char c = 0;
				for(int i = 0; i < 6; i++) {
					int val = tab[i] + 128;
					s += ((char)(val % 128));
					c += (val / 128) << i;
				}
				s += c;
				BeliefState bs = tmstates.get(s);
				if(bs!= null) {
					bs.add(copy);
				}
				else {
					bs = new BeliefState(tab, this.played + 1);
					bs.add(copy);
					tmstates.put(s, bs);
				}
			}
			return tmstates;
		}
		else {
			return null;
		}
		
	}
	
	public static BeliefState filter(Results beliefStates, GameState state) {
		byte tab[] = new byte[6];
		for(int i = 0; i < 6; i++) {
			tab[i] = Byte.MIN_VALUE;
		}
		for(int column = 0; column < 7; column++) {
			boolean isVisible = state.isGameOver() || state.isFull(column);
			BeliefState.setVisible(5, column, isVisible, tab);
			for(int row = 4; row > -1; row--) {
				isVisible = isVisible || (state.content(row, column) == 2);
				BeliefState.setVisible(row, column, isVisible, tab);
			}
		}
		String s = "";
		char c = 0;
		for(int i = 0; i < 6; i++) {
			int val = tab[i] + 128;
			s += ((char)(val % 128));
			c += (val / 128) << i;
		}
		s += c;
		BeliefState beliefState = beliefStates.get(s);
		RandomSelector rs = new RandomSelector();
		for(GameState st: beliefState.beliefState) {
			rs.add(st.proba());
		}
		int i = 0;
		for(GameState st: beliefState.beliefState) {
			st.setProba(rs.probability(i++));
		}
		return beliefState;
	}
	
	/**
	 * Make a copy of the belief state containing the same states
	 * @return copy of the belief state
	 */
	public BeliefState copy() {
		BeliefState bs = new BeliefState();
		for(GameState state: this.beliefState) {
			bs.add(state.copy());
		}
		for(int i = 0; i < 6; i++) {
			bs.isVisible[i] = this.isVisible[i];
		}
		bs.played = this.played;
		return bs;
	}
	
	public Iterator<GameState> iterator(){
		return this.beliefState.iterator();
	}
	
	/**
	 * Return the list of the column where a piece can be played (columns which are not full)
	 * @return
	 */
	public ArrayList<Integer> getMoves(){
		if(!this.isGameOver()) {
			ArrayList<Integer> moves = new ArrayList<Integer>();
			GameState state = this.beliefState.first();
			for(int i = 0; i < 7; i++) {
				if(!state.isFull(i))
					moves.add(i);
			}
			return moves;
		}
		else {
			return new ArrayList<Integer>();
		}
	}
	
	/**
	 * Provide information about the next player to play
	 * @return true if the next to play is the opponent, and false otherwise
	 */
	public boolean turn() {
		return this.beliefState.first().turn();
	}
	
	public boolean isVisible(int row, int column) {
		int pos = row * 7 + column;
		int index = pos / 8;
		pos = pos % 8;
		return ((this.isVisible[index] + 128) >> pos) % 2 == 1;
	}
	
	public void setVisible(int row, int column, boolean val) {
		int pos = row * 7 + column;
		int index = pos / 8;
		pos = pos % 8;
		int delta = ((val? 1: 0) - (this.isVisible(row, column)? 1: 0)) << pos;
		this.isVisible[index] = (byte) (this.isVisible[index] + delta);
	}
	
	public static void setVisible(int row, int column, boolean val, byte[] tab) {
		int pos = row * 7 + column;
		int index = pos / 8;
		pos = pos % 8;
		int posValue = tab[index] + 128;
		int delta = ((val? 1: 0) - ((posValue >> pos) % 2)) << pos;
		tab[index] = (byte) (posValue + delta - 128);
	}
	
	/**
	 * Check if the game is over in all state of the belief state. Note that when the game is over, the board is revealed and the environment becomes observable.
	 * @return true if the game is over, and false otherwise
	 */
	public boolean isGameOver() {
		for(GameState state: this.beliefState) {
			if(!state.isGameOver()) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Check if all the games in the belief state are full
	 * @return
	 */
	public boolean isFull() {
		return this.beliefState.first().isFull();
	}

	
	public void restart() {
		this.beliefState = new TreeSet<GameState>();
		this.isVisible = new byte[6];
		for(int i = 0; i < 6; i++) {
			this.isVisible[i] = Byte.MIN_VALUE;
		}
		this.played = 0;
	}
	
	public String toString() {
		String s = "BeliefState: size = " + this.beliefState.size() + " played = " + this.played + "\n";
		for(int row = 5; row > -1; row--) {
			for(int column = 0; column < 7; column++) {
				s += this.isVisible(row, column)? "1": "0";
			}
			s += "\n";
		}
		for(GameState state:this.beliefState) {
			s += state.toString() + "\n";
		}
		return s;
	}
	
	public int compareTo(BeliefState bs) {
		if(this.played != bs.played)
			return this.played > bs.played? 1: -1;
		for(int i = 0; i < 6; i++) {
			if(this.isVisible[i] != bs.isVisible[i])
				return this.isVisible[i] > bs.isVisible[i]? 1: -1;
		}
		if(this.beliefState.size() != bs.beliefState.size()) {
			return this.beliefState.size() > bs.beliefState.size()? 1: -1;
		}
		Iterator<GameState> iter = bs.beliefState.iterator();
		for(GameState next: this.beliefState) {
			GameState otherNext = iter.next();
			int comp = next.compareTo(otherNext);
			if(comp != 0)
				return comp;
		}
		iter = bs.beliefState.iterator();
		float sum1 = this.probaSum(), sum2 = bs.probaSum();
		for(GameState next: this.beliefState) {
			GameState otherNext = iter.next();
			if(Math.abs(next.proba() * sum1 - otherNext.proba() * sum2) > 0.001) {
				return next.proba() > otherNext.proba()? 1: -1;
			}
		}
		return 0;
	}
	
	public float probaSum() {
		float sum = 0;
		for(GameState state: this.beliefState) {
			sum += state.proba();
		}
		return sum;
	}
}





class ContingencyPlan{
	Integer action;
	HashMap<BeliefState, ContingencyPlan> plan=new HashMap<>();
	double heuristic_value;

	public ContingencyPlan() {
		this.heuristic_value = Double.NEGATIVE_INFINITY;
	}

	public ContingencyPlan(Integer action, HashMap<BeliefState, ContingencyPlan> plan, double heuristic_value) {
		this.action = action;
		this.plan = plan;
		this.heuristic_value = heuristic_value;
	}


	public ContingencyPlan(Integer action) {
		this.action = action;
		this.plan = new HashMap<BeliefState, ContingencyPlan>();
	}

	public void setHeuristicValue(double heuristic_value) {
		this.heuristic_value = heuristic_value;
	}
	
	public double getHeuristicValue() {
		return this.heuristic_value;
	}

	public void add(BeliefState state, ContingencyPlan subplan) {
		this.plan.put(state, subplan);
	}

	public HashMap<BeliefState, ContingencyPlan> getPlan() {
		return plan;
	}
}


public class AI{

	//exploredSet to keep the value of the BeliefState explored during orSearch and andSearch
	static ExploredSet exploredSet =new ExploredSet();
	final static int DEPTH = 2;
	final static int HEURISTIC[][] = new int[][] {{3,4,5,7,5,4,3},
		   										  {4,6,8,10,8,6,4},
		   										  {5,8,11,13,11,8,5},
		   										  {5,8,11,13,11,8,5},
		   										  {4,6,8,10,8,6,4},
		   										  {3,4,5,7,5,4,3}};

	public AI() {
	}


	//Method to compute the heuristic of a given beliefState
	//Returns the maximum heuristic value of the potential GameState contained in the beliefState
	public static double computeHeuristicForBeliefState(BeliefState currentBeliefState){
		double value_of_belief_state =0;
		for(GameState game : currentBeliefState) {
			value_of_belief_state += heuristic(game);
		}
		return value_of_belief_state;
	}


	//insertion sort tel que l'action la plus prometteuse est placée en première
	public static void sort_moves(ArrayList<Integer> moves, BeliefState state) {
		int n = moves.size();
		for (int i = 0; i < n; i++) {
			Integer key = moves.get(i);
			double key_value = heuristic(state.putPiecePlayer(key));
			int j = i - 1;

			while (j >= 0 && heuristic(state.putPiecePlayer(moves.get(j))) < key_value) {
				moves.set(j+1, moves.get(j));
				j--;
			}
			moves.set(j + 1, key);
		}
	}
	
	//function orSearch applying the algorithm of the course but stopping itself when a plan of 4 actions has been constructed
	public static ContingencyPlan orSearch(BeliefState currentBeliefState, ArrayList<BeliefState> path,int depth_of_prediction) {
		HashMap<BeliefState, ContingencyPlan> plan;
		ContingencyPlan plan_res;
		ContingencyPlan max_plan = new ContingencyPlan();
		
		if (depth_of_prediction > DEPTH) return new ContingencyPlan();
		
		if (currentBeliefState.isGameOver())
			return new ContingencyPlan();
		
		if (path.contains(currentBeliefState))
			return null;
		
		ArrayList<Integer> moves = currentBeliefState.getMoves();
		
		//we will sort the list of moves in increasing order of the value of the beliefstate associated
		sort_moves(moves, currentBeliefState);
		
		for (Integer action : moves) {
			path.add(currentBeliefState);
			plan = andSearch(currentBeliefState.copy().putPiecePlayer(action), path, depth_of_prediction+1);
			if (plan != null) {
				plan_res = new ContingencyPlan(action, plan, heuristic(currentBeliefState.copy().putPiecePlayer(action)));
				//System.out.println(plan_res.getHeuristicValue());
				//System.out.println(max_plan.getHeuristicValue());
				if (plan_res.getHeuristicValue() > max_plan.getHeuristicValue())
					max_plan = plan_res;
			}
			
		}
		
		//System.out.println(max_plan.getHeuristicValue());
		if (max_plan.getHeuristicValue() == Double.NEGATIVE_INFINITY) {
			//System.out.println("salut !");
			return null;
		}
		return max_plan;
	}

	//function andSearch applying the algorithm of the course but stopping itself when a plan of 4 actions has been constructed
	public static HashMap<BeliefState, ContingencyPlan> andSearch(Results currentBeliefStates,ArrayList<BeliefState> path, int depth_of_prediction) {
		
		HashMap<BeliefState, ContingencyPlan> hmap = new HashMap<BeliefState,ContingencyPlan>();
		ContingencyPlan subplan;
		
		if (depth_of_prediction > DEPTH) return new HashMap<BeliefState,ContingencyPlan>();
		
		for (BeliefState state : currentBeliefStates) {
			Results predictions = state.copy().predict();
			if (predictions == null) //erreur contournée ici (je sais pas pourquoi il est nul des fois)
				continue;
			for (BeliefState s_i : predictions) {
				subplan = orSearch(s_i, path, depth_of_prediction+1);
				if (subplan == null)
					return null;
				hmap.put(s_i, subplan);
			}
		}
		return hmap;
	}
		
	public static double heuristic(Results predictions) {
		double res = 0.0f;
		int nbState=0;
		for (BeliefState beliefState : predictions) {
			for (GameState state : beliefState) {
				res += heuristic(state);
				nbState++;
			}
		}
		return res/nbState;
	}
		
	
	public static double heuristic(GameState game) {
		int heuristic_value = 0;						   
		
		//calcul de l'utilité en fonction des pions rouges
		for (int row = 0; row < 6; row++) {
			for (int column = 0; column < 7; column++) {
				if (game.content(row, column) == 2)
					heuristic_value += (6-row)*(HEURISTIC[row][column] - scan(game, row, column, 1)); //on mets du poids sur les pions placés bas
				if (game.content(row, column) == 1)
					heuristic_value -= (6-row)*(HEURISTIC[row][column] - scan(game, row, column, 2));
			}
		}
		return game.proba()*((double) heuristic_value);
	}
	
	
	public static int scan(GameState game, int row, int column, int opponent) {
		int cpt = 0;
		
		switch(row) {
		case 0:
			
			switch(column) {
			case 0:
				if (game.content(row+1, column) == opponent || game.content(row+2, column) == opponent || game.content(row+3, column) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column+2) == opponent || game.content(row, column+3) == opponent)
					cpt++;
				if (game.content(row+1, column+1) == opponent || game.content(row+2, column+2) == opponent || game.content(row+3, column+3) == opponent)
					cpt++;
				break;
				
			case 1:
				if (game.content(row, column-1) == opponent || game.content(row, column+1) == opponent || game.content(row, column+2) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column+2) == opponent || game.content(row, column+3) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row+2, column) == opponent || game.content(row+3, column) == opponent)
					cpt++;
				if (game.content(row+1, column+1) == opponent || game.content(row+2, column+2) == opponent || game.content(row+3, column+3) == opponent)
					cpt++;
				break;
				
			case 2:
				if (game.content(row, column-2) == opponent || game.content(row, column-1) == opponent || game.content(row, column+1) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column+1) == opponent || game.content(row, column+2) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column+2) == opponent || game.content(row, column+3) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row+2, column) == opponent || game.content(row+3, column) == opponent)
					cpt++;
				if (game.content(row+1, column+1) == opponent || game.content(row+2, column+2) == opponent || game.content(row+3, column+3) == opponent)
					cpt++;
				break;
				
			case 3:
				if (game.content(row, column-3) == opponent || game.content(row, column-2) == opponent || game.content(row, column-1) == opponent)
					cpt++;
				if (game.content(row, column-2) == opponent || game.content(row, column-1) == opponent || game.content(row, column+1) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column+1) == opponent || game.content(row, column+2) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column+2) == opponent || game.content(row, column+3) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row+2, column) == opponent || game.content(row+3, column) == opponent)
					cpt++;
				if (game.content(row+1, column-1) == opponent || game.content(row+2, column-2) == opponent || game.content(row+3, column-3) == opponent)
					cpt++;
				if (game.content(row+1, column+1) == opponent || game.content(row+2, column+2) == opponent || game.content(row+3, column+3) == opponent)
					cpt++;
				break;
				
			case 4:
				if (game.content(row, column+2) == opponent || game.content(row, column+1) == opponent || game.content(row, column-1) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column-1) == opponent || game.content(row, column-2) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column-2) == opponent || game.content(row, column-3) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row+2, column) == opponent || game.content(row+3, column) == opponent)
					cpt++;
				if (game.content(row+1, column-1) == opponent || game.content(row+2, column-2) == opponent || game.content(row+3, column-3) == opponent)
					cpt++;
				break;
				
			case 5:
				if (game.content(row, column+1) == opponent || game.content(row, column-1) == opponent || game.content(row, column-2) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column-2) == opponent || game.content(row, column-3) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row+2, column) == opponent || game.content(row+3, column) == opponent)
					cpt++;
				if (game.content(row+1, column-1) == opponent || game.content(row+2, column-2) == opponent || game.content(row+3, column-3) == opponent)
					cpt++;
				break;
				
			case 6:
				if (game.content(row+1, column) == opponent || game.content(row+2, column) == opponent || game.content(row+3, column) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column-2) == opponent || game.content(row, column-3) == opponent)
					cpt++;
				if (game.content(row+1, column+1) == opponent || game.content(row+2, column+2) == opponent || game.content(row+3, column+3) == opponent)
					cpt++;
				break;
			}
			
			break;
			
		case 1:
			
			switch(column) {
			case 0:
				if (game.content(row-1, column) == opponent || game.content(row+1, column) == opponent || game.content(row+2, column) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row+2, column) == opponent || game.content(row+3, column) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column+2) == opponent || game.content(row, column+3) == opponent)
					cpt++;
				if (game.content(row+1, column+1) == opponent || game.content(row+2, column+2) == opponent || game.content(row+3, column+3) == opponent)
					cpt++;
				break;
				
			case 1:
				if (game.content(row-1, column) == opponent || game.content(row+1, column) == opponent || game.content(row+2, column) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row+2, column) == opponent || game.content(row+3, column) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column+1) == opponent || game.content(row, column+2) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column+2) == opponent || game.content(row, column+3) == opponent)
					cpt++;
				if (game.content(row-1, column-1) == opponent || game.content(row+1, column+1) == opponent || game.content(row+2, column+2) == opponent)
					cpt++;
				if (game.content(row+1, column+1) == opponent || game.content(row+2, column+2) == opponent || game.content(row+3, column+3) == opponent)
					cpt++;
				break;
				
			case 2:
				if (game.content(row, column-2) == opponent || game.content(row, column-1) == opponent || game.content(row, column+1) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column+1) == opponent || game.content(row, column+2) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column+2) == opponent || game.content(row, column+3) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row+1, column) == opponent || game.content(row+2, column) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row+2, column) == opponent || game.content(row+3, column) == opponent)
					cpt++;
				if (game.content(row-1, column+1) == opponent || game.content(row+1, column-1) == opponent || game.content(row+2, column-2) == opponent)
					cpt++;
				if (game.content(row-1, column-1) == opponent || game.content(row+1, column+1) == opponent || game.content(row+2, column+2) == opponent)
					cpt++;
				if (game.content(row+1, column+1) == opponent || game.content(row+2, column+2) == opponent || game.content(row+3, column+3) == opponent)
					cpt++;
				break; 
				
			case 3:
				if (game.content(row, column-3) == opponent || game.content(row, column-2) == opponent || game.content(row, column-1) == opponent)
					cpt++;
				if (game.content(row, column-2) == opponent || game.content(row, column-1) == opponent || game.content(row, column+1) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column+1) == opponent || game.content(row, column+2) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column+2) == opponent || game.content(row, column+3) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row+1, column) == opponent || game.content(row+2, column) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row+2, column) == opponent || game.content(row+3, column) == opponent)
					cpt++;
				if (game.content(row-1, column-1) == opponent || game.content(row+1, column+1) == opponent || game.content(row+2, column+2) == opponent)
					cpt++;
				if (game.content(row+1, column+1) == opponent || game.content(row+2, column+2) == opponent || game.content(row+3, column+3) == opponent)
					cpt++;
				if (game.content(row-1, column+1) == opponent || game.content(row+1, column-1) == opponent || game.content(row+2, column-2) == opponent)
					cpt++;
				if (game.content(row+1, column-1) == opponent || game.content(row+2, column-2) == opponent || game.content(row+3, column-3) == opponent)
					cpt++;
				break;
				
			case 4:
				if (game.content(row, column+2) == opponent || game.content(row, column+1) == opponent || game.content(row, column-1) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column-1) == opponent || game.content(row, column-2) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column-2) == opponent || game.content(row, column-3) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row+1, column) == opponent || game.content(row+2, column) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row+2, column) == opponent || game.content(row+3, column) == opponent)
					cpt++;
				if (game.content(row-1, column-1) == opponent || game.content(row+1, column+1) == opponent || game.content(row+2, column+2) == opponent)
					cpt++;
				if (game.content(row-1, column+1) == opponent || game.content(row+1, column-1) == opponent || game.content(row+2, column-2) == opponent)
					cpt++;
				if (game.content(row+1, column-1) == opponent || game.content(row+2, column-2) == opponent || game.content(row+3, column-3) == opponent)
					cpt++;
				break;
				
			case 5:
				if (game.content(row-1, column) == opponent || game.content(row+1, column) == opponent || game.content(row+2, column) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row+2, column) == opponent || game.content(row+3, column) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column-1) == opponent || game.content(row, column-2) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column-2) == opponent || game.content(row, column-3) == opponent)
					cpt++;
				if (game.content(row-1, column+1) == opponent || game.content(row+1, column-1) == opponent || game.content(row+2, column-2) == opponent)
					cpt++;
				if (game.content(row+1, column-1) == opponent || game.content(row+2, column-2) == opponent || game.content(row+3, column-3) == opponent)
					cpt++;
				break;
				
			case 6:
				if (game.content(row-1, column) == opponent || game.content(row+1, column) == opponent || game.content(row+2, column) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row+2, column) == opponent || game.content(row+3, column) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column-2) == opponent || game.content(row, column-3) == opponent)
					cpt++;
				if (game.content(row+1, column-1) == opponent || game.content(row+2, column-2) == opponent || game.content(row+3, column-3) == opponent)
					cpt++;
				break;
			}
			
			break;
			
		case 2:
			
			switch(column) {
			case 0:
				if (game.content(row, column+1) == opponent || game.content(row, column+2) == opponent || game.content(row, column+3) == opponent)
					cpt++;
				if (game.content(row-2, column) == opponent || game.content(row-1, column) == opponent || game.content(row+1, column) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row+1, column) == opponent || game.content(row+2, column) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row+2, column) == opponent || game.content(row+3, column) == opponent)
					cpt++;
				if (game.content(row+1, column+1) == opponent || game.content(row+2, column+2) == opponent || game.content(row+3, column+3) == opponent)
					cpt++;
				break;
				
			case 1:
				if (game.content(row, column-1) == opponent || game.content(row, column+1) == opponent || game.content(row, column+2) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column+2) == opponent || game.content(row, column+3) == opponent)
					cpt++;
				if (game.content(row-2, column) == opponent || game.content(row-1, column) == opponent || game.content(row+1, column) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row+1, column) == opponent || game.content(row+2, column) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row+2, column) == opponent || game.content(row+3, column) == opponent)
					cpt++;
				if (game.content(row+1, column-1) == opponent || game.content(row-1, column+1) == opponent || game.content(row-2, column+2) == opponent)
					cpt++;
				if (game.content(row-1, column-1) == opponent || game.content(row+1, column+1) == opponent || game.content(row+2, column+2) == opponent)
					cpt++;
				if (game.content(row+1, column+1) == opponent || game.content(row+2, column+2) == opponent || game.content(row+3, column+3) == opponent)
					cpt++;
				break;
				
			case 2:
				if (game.content(row, column-2) == opponent || game.content(row, column-1) == opponent || game.content(row, column+1) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column+1) == opponent || game.content(row, column+2) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column+2) == opponent || game.content(row, column+3) == opponent)
					cpt++;
				if (game.content(row-2, column) == opponent || game.content(row-1, column) == opponent || game.content(row+1, column) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row+1, column) == opponent || game.content(row+2, column) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row+2, column) == opponent || game.content(row+3, column) == opponent)
					cpt++;
				if (game.content(row-2, column-2) == opponent || game.content(row-1, column-1) == opponent || game.content(row+1, column+1) == opponent)
					cpt++;
				if (game.content(row-1, column-1) == opponent || game.content(row+1, column+1) == opponent || game.content(row+2, column+2) == opponent)
					cpt++;
				if (game.content(row+1, column+1) == opponent || game.content(row+2, column+2) == opponent || game.content(row+3, column+3) == opponent)
					cpt++;
				if (game.content(row+2, column-2) == opponent || game.content(row+1, column-1) == opponent || game.content(row-1, column+1) == opponent)
					cpt++;
				if (game.content(row+1, column-1) == opponent || game.content(row-1, column+1) == opponent || game.content(row-2, column+2) == opponent)
					cpt++;
				break;
				
			case 3:
				if (game.content(row, column-3) == opponent || game.content(row, column-2) == opponent || game.content(row, column-1) == opponent)
					cpt++;
				if (game.content(row, column-2) == opponent || game.content(row, column-1) == opponent || game.content(row, column+1) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column+1) == opponent || game.content(row, column+2) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column+2) == opponent || game.content(row, column+3) == opponent)
					cpt++;
				if (game.content(row-2, column) == opponent || game.content(row-1, column) == opponent || game.content(row+1, column) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row+1, column) == opponent || game.content(row+2, column) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row+2, column) == opponent || game.content(row+3, column) == opponent)
					cpt++;
				if (game.content(row-2, column-2) == opponent || game.content(row-1, column-1) == opponent || game.content(row+1, column+1) == opponent)
					cpt++;
				if (game.content(row-1, column-1) == opponent || game.content(row+1, column+1) == opponent || game.content(row+2, column+2) == opponent)
					cpt++;
				if (game.content(row+1, column+1) == opponent || game.content(row+2, column+2) == opponent || game.content(row+3, column+3) == opponent)
					cpt++;
				if (game.content(row-2, column+2) == opponent || game.content(row-1, column+1) == opponent || game.content(row+1, column-1) == opponent)
					cpt++;
				if (game.content(row-1, column+1) == opponent || game.content(row+1, column-1) == opponent || game.content(row+2, column-2) == opponent)
					cpt++;
				if (game.content(row+1, column-1) == opponent || game.content(row+2, column-2) == opponent || game.content(row+3, column-3) == opponent)
					cpt++;
				break;
				
			case 4:
				if (game.content(row, column+2) == opponent || game.content(row, column+1) == opponent || game.content(row, column-1) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column-1) == opponent || game.content(row, column-2) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column-2) == opponent || game.content(row, column-3) == opponent)
					cpt++;
				if (game.content(row-2, column) == opponent || game.content(row-1, column) == opponent || game.content(row+1, column) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row+1, column) == opponent || game.content(row+2, column) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row+2, column) == opponent || game.content(row+3, column) == opponent)
					cpt++;
				if (game.content(row-2, column+2) == opponent || game.content(row-1, column+1) == opponent || game.content(row+1, column-1) == opponent)
					cpt++;
				if (game.content(row-1, column+1) == opponent || game.content(row+1, column-1) == opponent || game.content(row+2, column-2) == opponent)
					cpt++;
				if (game.content(row+1, column-1) == opponent || game.content(row+2, column-2) == opponent || game.content(row+3, column-3) == opponent)
					cpt++;
				if (game.content(row+2, column+2) == opponent || game.content(row+1, column+1) == opponent || game.content(row-1, column-1) == opponent)
					cpt++;
				if (game.content(row+1, column+1) == opponent || game.content(row-1, column-1) == opponent || game.content(row-2, column-2) == opponent)
					cpt++;
				break;
				
			case 5:
				if (game.content(row, column+1) == opponent || game.content(row, column-1) == opponent || game.content(row, column-2) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column-2) == opponent || game.content(row, column-3) == opponent)
					cpt++;
				if (game.content(row-2, column) == opponent || game.content(row-1, column) == opponent || game.content(row+1, column) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row+1, column) == opponent || game.content(row+2, column) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row+2, column) == opponent || game.content(row+3, column) == opponent)
					cpt++;
				if (game.content(row+1, column+1) == opponent || game.content(row-1, column-1) == opponent || game.content(row-2, column-2) == opponent)
					cpt++;
				if (game.content(row-1, column+1) == opponent || game.content(row+1, column-1) == opponent || game.content(row+2, column-2) == opponent)
					cpt++;
				if (game.content(row+1, column-1) == opponent || game.content(row+2, column-2) == opponent || game.content(row+3, column-3) == opponent)
					cpt++;
				break;
				
			case 6:
				if (game.content(row, column-1) == opponent || game.content(row, column-2) == opponent || game.content(row, column-3) == opponent)
					cpt++;
				if (game.content(row-2, column) == opponent || game.content(row-1, column) == opponent || game.content(row+1, column) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row+1, column) == opponent || game.content(row+2, column) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row+2, column) == opponent || game.content(row+3, column) == opponent)
					cpt++;
				if (game.content(row+1, column-1) == opponent || game.content(row+2, column-2) == opponent || game.content(row+3, column-3) == opponent)
					cpt++;
				break;
			}
			
			break;
			
		case 3:
			
			switch(column) {
			case 0:
				if (game.content(row, column+1) == opponent || game.content(row, column+2) == opponent || game.content(row, column+3) == opponent)
					cpt++;
				if (game.content(row+2, column) == opponent || game.content(row+1, column) == opponent || game.content(row-1, column) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row-1, column) == opponent || game.content(row-2, column) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row-2, column) == opponent || game.content(row-3, column) == opponent)
					cpt++;
				if (game.content(row-1, column+1) == opponent || game.content(row-2, column+2) == opponent || game.content(row-3, column+3) == opponent)
					cpt++;
				break;
				
			case 1:
				if (game.content(row, column-1) == opponent || game.content(row, column+1) == opponent || game.content(row, column+2) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column+2) == opponent || game.content(row, column+3) == opponent)
					cpt++;
				if (game.content(row+2, column) == opponent || game.content(row+1, column) == opponent || game.content(row-1, column) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row-1, column) == opponent || game.content(row-2, column) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row-2, column) == opponent || game.content(row-3, column) == opponent)
					cpt++;
				if (game.content(row-1, column-1) == opponent || game.content(row+1, column+1) == opponent || game.content(row+2, column+2) == opponent)
					cpt++;
				if (game.content(row+1, column-1) == opponent || game.content(row-1, column+1) == opponent || game.content(row-2, column+2) == opponent)
					cpt++;
				if (game.content(row-1, column+1) == opponent || game.content(row-2, column+2) == opponent || game.content(row-3, column+3) == opponent)
					cpt++;
				break;
				
			case 2:
				if (game.content(row, column-2) == opponent || game.content(row, column-1) == opponent || game.content(row, column+1) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column+1) == opponent || game.content(row, column+2) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column+2) == opponent || game.content(row, column+3) == opponent)
					cpt++;
				if (game.content(row+2, column) == opponent || game.content(row+1, column) == opponent || game.content(row-1, column) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row-1, column) == opponent || game.content(row-2, column) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row-2, column) == opponent || game.content(row-3, column) == opponent)
					cpt++;
				if (game.content(row+2, column-2) == opponent || game.content(row+1, column-1) == opponent || game.content(row-1, column+1) == opponent)
					cpt++;
				if (game.content(row+1, column-1) == opponent || game.content(row-1, column+1) == opponent || game.content(row-2, column+2) == opponent)
					cpt++;
				if (game.content(row-1, column+1) == opponent || game.content(row-2, column+2) == opponent || game.content(row-3, column+3) == opponent)
					cpt++;
				if (game.content(row-2, column-2) == opponent || game.content(row-1, column-1) == opponent || game.content(row+1, column+1) == opponent)
					cpt++;
				if (game.content(row-1, column-1) == opponent || game.content(row+1, column+1) == opponent || game.content(row+2, column+2) == opponent)
					cpt++;
				break;
				
			case 3:
				if (game.content(row, column-3) == opponent || game.content(row, column-2) == opponent || game.content(row, column-1) == opponent)
					cpt++;
				if (game.content(row, column-2) == opponent || game.content(row, column-1) == opponent || game.content(row, column+1) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column+1) == opponent || game.content(row, column+2) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column+2) == opponent || game.content(row, column+3) == opponent)
					cpt++;
				if (game.content(row+2, column) == opponent || game.content(row+1, column) == opponent || game.content(row-1, column) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row-1, column) == opponent || game.content(row-2, column) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row-2, column) == opponent || game.content(row-3, column) == opponent)
					cpt++;
				if (game.content(row+2, column-2) == opponent || game.content(row+1, column-1) == opponent || game.content(row-1, column+1) == opponent)
					cpt++;
				if (game.content(row+1, column-1) == opponent || game.content(row-1, column+1) == opponent || game.content(row-2, column+2) == opponent)
					cpt++;
				if (game.content(row-1, column+1) == opponent || game.content(row-2, column+2) == opponent || game.content(row-3, column+3) == opponent)
					cpt++;
				if (game.content(row+2, column+2) == opponent || game.content(row+1, column+1) == opponent || game.content(row-1, column-1) == opponent)
					cpt++;
				if (game.content(row+1, column+1) == opponent || game.content(row-1, column-1) == opponent || game.content(row-2, column-2) == opponent)
					cpt++;
				if (game.content(row-1, column-1) == opponent || game.content(row-2, column-2) == opponent || game.content(row-3, column-3) == opponent)
					cpt++;
				break;
				
			case 4:
				if (game.content(row, column+2) == opponent || game.content(row, column+1) == opponent || game.content(row, column-1) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column-1) == opponent || game.content(row, column-2) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column-2) == opponent || game.content(row, column-3) == opponent)
					cpt++;
				if (game.content(row+2, column) == opponent || game.content(row+1, column) == opponent || game.content(row-1, column) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row-1, column) == opponent || game.content(row-2, column) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row-2, column) == opponent || game.content(row-3, column) == opponent)
					cpt++;
				if (game.content(row+2, column+2) == opponent || game.content(row+1, column+1) == opponent || game.content(row-1, column-1) == opponent)
					cpt++;
				if (game.content(row+1, column+1) == opponent || game.content(row-1, column-1) == opponent || game.content(row-2, column-2) == opponent)
					cpt++;
				if (game.content(row-1, column-1) == opponent || game.content(row-2, column-2) == opponent || game.content(row-3, column-3) == opponent)
					cpt++;
				if (game.content(row-2, column+2) == opponent || game.content(row-1, column+1) == opponent || game.content(row+1, column-1) == opponent)
					cpt++;
				if (game.content(row-1, column+1) == opponent || game.content(row+1, column-1) == opponent || game.content(row+2, column-2) == opponent)
					cpt++;
				break;
				
			case 5:
				if (game.content(row, column+1) == opponent || game.content(row, column-1) == opponent || game.content(row, column-2) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column-2) == opponent || game.content(row, column-3) == opponent)
					cpt++;
				if (game.content(row+2, column) == opponent || game.content(row+1, column) == opponent || game.content(row-1, column) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row-1, column) == opponent || game.content(row-2, column) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row-2, column) == opponent || game.content(row-3, column) == opponent)
					cpt++;
				if (game.content(row-1, column+1) == opponent || game.content(row+1, column-1) == opponent || game.content(row+2, column-2) == opponent)
					cpt++;
				if (game.content(row+1, column+1) == opponent || game.content(row-1, column-1) == opponent || game.content(row-2, column-2) == opponent)
					cpt++;
				if (game.content(row-1, column-1) == opponent || game.content(row-2, column-2) == opponent || game.content(row-3, column-3) == opponent)
					cpt++;
				break;
				
			case 6:
				if (game.content(row, column-1) == opponent || game.content(row, column-2) == opponent || game.content(row, column-3) == opponent)
					cpt++;
				if (game.content(row+2, column) == opponent || game.content(row+1, column) == opponent || game.content(row-1, column) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row-1, column) == opponent || game.content(row-2, column) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row-2, column) == opponent || game.content(row-3, column) == opponent)
					cpt++;
				if (game.content(row-1, column-1) == opponent || game.content(row-2, column-2) == opponent || game.content(row-3, column-3) == opponent)
					cpt++;
				break;
			}
			
			break;
			
		case 4:
			
			switch(column) {
			case 0:
				if (game.content(row+1, column) == opponent || game.content(row-1, column) == opponent || game.content(row-2, column) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row-2, column) == opponent || game.content(row-3, column) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column+2) == opponent || game.content(row, column+3) == opponent)
					cpt++;
				if (game.content(row-1, column+1) == opponent || game.content(row-2, column+2) == opponent || game.content(row-3, column+3) == opponent)
					cpt++;
				break;
				
			case 1:
				if (game.content(row+1, column) == opponent || game.content(row-1, column) == opponent || game.content(row-2, column) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row-2, column) == opponent || game.content(row-3, column) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column+1) == opponent || game.content(row, column+2) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column+2) == opponent || game.content(row, column+3) == opponent)
					cpt++;
				if (game.content(row+1, column-1) == opponent || game.content(row-1, column+1) == opponent || game.content(row-2, column+2) == opponent)
					cpt++;
				if (game.content(row-1, column+1) == opponent || game.content(row-2, column+2) == opponent || game.content(row-3, column+3) == opponent)
					cpt++;
				break;
				
			case 2:
				if (game.content(row, column-2) == opponent || game.content(row, column-1) == opponent || game.content(row, column+1) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column+1) == opponent || game.content(row, column+2) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column+2) == opponent || game.content(row, column+3) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row-1, column) == opponent || game.content(row-2, column) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row-2, column) == opponent || game.content(row-3, column) == opponent)
					cpt++;
				if (game.content(row+1, column+1) == opponent || game.content(row-1, column-1) == opponent || game.content(row-2, column-2) == opponent)
					cpt++;
				if (game.content(row+1, column-1) == opponent || game.content(row-1, column+1) == opponent || game.content(row-2, column+2) == opponent)
					cpt++;
				if (game.content(row-1, column+1) == opponent || game.content(row-2, column+2) == opponent || game.content(row-3, column+3) == opponent)
					cpt++;
				break;
				
			case 3:
				if (game.content(row, column-3) == opponent || game.content(row, column-2) == opponent || game.content(row, column-1) == opponent)
					cpt++;
				if (game.content(row, column-2) == opponent || game.content(row, column-1) == opponent || game.content(row, column+1) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column+1) == opponent || game.content(row, column+2) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column+2) == opponent || game.content(row, column+3) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row-1, column) == opponent || game.content(row-2, column) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row-2, column) == opponent || game.content(row-3, column) == opponent)
					cpt++;
				if (game.content(row+1, column-1) == opponent || game.content(row-1, column+1) == opponent || game.content(row-2, column+2) == opponent)
					cpt++;
				if (game.content(row-1, column+1) == opponent || game.content(row-2, column+2) == opponent || game.content(row-3, column+3) == opponent)
					cpt++;
				if (game.content(row+1, column+1) == opponent || game.content(row-1, column-1) == opponent || game.content(row-2, column-2) == opponent)
					cpt++;
				if (game.content(row-1, column-1) == opponent || game.content(row-2, column-2) == opponent || game.content(row-3, column-3) == opponent)
					cpt++;
				break;
				
			case 4:
				if (game.content(row, column+2) == opponent || game.content(row, column+1) == opponent || game.content(row, column-1) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column-1) == opponent || game.content(row, column-2) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column-2) == opponent || game.content(row, column-3) == opponent)
					cpt++;
				if (game.content(row+1, column) == opponent || game.content(row-1, column) == opponent || game.content(row-2, column) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row-2, column) == opponent || game.content(row-3, column) == opponent)
					cpt++;
				if (game.content(row+1, column-1) == opponent || game.content(row-1, column+1) == opponent || game.content(row-2, column+2) == opponent)
					cpt++;
				if (game.content(row+1, column+1) == opponent || game.content(row-1, column-1) == opponent || game.content(row-2, column-2) == opponent)
					cpt++;
				if (game.content(row-1, column-1) == opponent || game.content(row-2, column-2) == opponent || game.content(row-3, column-3) == opponent)
					cpt++;
				break;
				
			case 5:
				if (game.content(row+1, column) == opponent || game.content(row-1, column) == opponent || game.content(row-2, column) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row-2, column) == opponent || game.content(row-3, column) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column-1) == opponent || game.content(row, column-2) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column-2) == opponent || game.content(row, column-3) == opponent)
					cpt++;
				if (game.content(row+1, column+1) == opponent || game.content(row-1, column-1) == opponent || game.content(row-2, column-2) == opponent)
					cpt++;
				if (game.content(row-1, column-1) == opponent || game.content(row-2, column-2) == opponent || game.content(row-3, column-3) == opponent)
					cpt++;
				break;
				
			case 6:
				if (game.content(row+1, column) == opponent || game.content(row-1, column) == opponent || game.content(row-2, column) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row-2, column) == opponent || game.content(row-3, column) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column-2) == opponent || game.content(row, column-3) == opponent)
					cpt++;
				if (game.content(row-1, column-1) == opponent || game.content(row-2, column-2) == opponent || game.content(row-3, column-3) == opponent)
					cpt++;
				break;
			}
			
			break;
			
		case 5:
			
			switch(column) {
			case 0:
				if (game.content(row-1, column) == opponent || game.content(row-2, column) == opponent || game.content(row-3, column) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column+2) == opponent || game.content(row, column+3) == opponent)
					cpt++;
				if (game.content(row-1, column-1) == opponent || game.content(row-2, column-2) == opponent || game.content(row-3, column-3) == opponent)
					cpt++;
				break;
				
			case 1:
				if (game.content(row, column-1) == opponent || game.content(row, column+1) == opponent || game.content(row, column+2) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column+2) == opponent || game.content(row, column+3) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row-2, column) == opponent || game.content(row-3, column) == opponent)
					cpt++;
				if (game.content(row-1, column+1) == opponent || game.content(row-2, column+2) == opponent || game.content(row-3, column+3) == opponent)
					cpt++;
				break;
				
			case 2:
				if (game.content(row, column-2) == opponent || game.content(row, column-1) == opponent || game.content(row, column+1) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column+1) == opponent || game.content(row, column+2) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column+2) == opponent || game.content(row, column+3) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row-2, column) == opponent || game.content(row-3, column) == opponent)
					cpt++;
				if (game.content(row-1, column+1) == opponent || game.content(row-2, column+2) == opponent || game.content(row-3, column+3) == opponent)
					cpt++;
				break;
				
			case 3:
				if (game.content(row, column-3) == opponent || game.content(row, column-2) == opponent || game.content(row, column-1) == opponent)
					cpt++;
				if (game.content(row, column-2) == opponent || game.content(row, column-1) == opponent || game.content(row, column+1) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column+1) == opponent || game.content(row, column+2) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column+2) == opponent || game.content(row, column+3) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row-2, column) == opponent || game.content(row-3, column) == opponent)
					cpt++;
				if (game.content(row-1, column-1) == opponent || game.content(row-2, column-2) == opponent || game.content(row-3, column-3) == opponent)
					cpt++;
				if (game.content(row-1, column+1) == opponent || game.content(row-2, column+2) == opponent || game.content(row-3, column+3) == opponent)
					cpt++;
				break;
				
			case 4:
				if (game.content(row, column+2) == opponent || game.content(row, column+1) == opponent || game.content(row, column-1) == opponent)
					cpt++;
				if (game.content(row, column+1) == opponent || game.content(row, column-1) == opponent || game.content(row, column-2) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column-2) == opponent || game.content(row, column-3) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row-2, column) == opponent || game.content(row-3, column) == opponent)
					cpt++;
				if (game.content(row-1, column-1) == opponent || game.content(row-2, column-2) == opponent || game.content(row-3, column-3) == opponent)
					cpt++;
				break;
				
			case 5:
				if (game.content(row, column+1) == opponent || game.content(row, column-1) == opponent || game.content(row, column-2) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column-2) == opponent || game.content(row, column-3) == opponent)
					cpt++;
				if (game.content(row-1, column) == opponent || game.content(row-2, column) == opponent || game.content(row-3, column) == opponent)
					cpt++;
				if (game.content(row-1, column-1) == opponent || game.content(row-2, column-2) == opponent || game.content(row-3, column-3) == opponent)
					cpt++;
				break;
				
			case 6:
				if (game.content(row-1, column) == opponent || game.content(row-2, column) == opponent || game.content(row-3, column) == opponent)
					cpt++;
				if (game.content(row, column-1) == opponent || game.content(row, column-2) == opponent || game.content(row, column-3) == opponent)
					cpt++;
				if (game.content(row-1, column-1) == opponent || game.content(row-2, column-2) == opponent || game.content(row-3, column-3) == opponent)
					cpt++;
				break;
			}
			
			break;
		}
		
		
	
		
		return cpt;
	}
	
	
	
	
	//function returning the best move for a given BeliefState
	public static int findNextMove(BeliefState game) {
		ExploredSet path = new ExploredSet();

		
		ContingencyPlan plan = orSearch(game,new ArrayList<BeliefState>(),1);
		
		/*
		ContingencyPlan bestPlan = null;
		
		
		//Search of the best plan in the plan of action obtained after or-and-search
		float best_C_value=Float.NEGATIVE_INFINITY;
		for (Entry<BeliefState,ContingencyPlan> subplan : plan.getPlan().entrySet()){
			Float current_C_value=exploredSet.get(subplan.getKey());
			if(current_C_value==null) {
				current_C_value=computeHeuristicForBeliefState(subplan.getKey());
			}
			if(current_C_value>best_C_value){
				best_C_value=current_C_value;
				bestPlan=subplan.getValue();
			}
		}
		*/
		//System.out.println(plan.getPlan().size());

		
        return plan.action;
	}













//Première idée d'heuristique

public static float heuristic2(GameState game) {
	int heuristic_value = 0;
	
	//On va calculer pour chaque case jouable une valeur qui nous donnera une estimation de l'utilité
	for (int column = 0; column < 7; column++) {
		
		//On cherche la première ligne vide sur la colonne j
		for (int row = 0; row < 6 && game.content(row, column) != 0; row++) {
			
			//si la colonne n'est pas remplie
			if (row < 6)
				heuristic_value += compute_row(game, row, column) + compute_column(game, row, column) + compute_diag(game, row, column);
			
		}
	}
	return game.proba()*((float) heuristic_value);
	}


/* Calcule la valeur d'utilité sur la ligne concernée */
public static int compute_row(GameState game, int row, int column) {
	int res = 0;
	
	switch(column) {
	
	case 0:
		//à droite
		if (game.content(row, column+1) == 2) res+=2;
		else if (game.content(row, column+1) == 0) res+=1;
		break;
		
	case 6:
		//à gauche
		if (game.content(row, column-1) == 2) res+=2;
		else if (game.content(row, column-1) == 0) res+=1;
		break;
		
	default:
		//à droite
		if (game.content(row, column+1) == 2) res+=2;
		else if (game.content(row, column+1) == 0) res+=1;
		//à gauche
		if (game.content(row, column-1) == 2) res+=2;
		else if (game.content(row, column-1) == 0) res+=1;
		break;
	}
	
	return res;
}

/* calcule la valeur d'utilité sur la colonne concernée */
public static int compute_column(GameState game, int row, int column) {
	int res = 0;
	
	switch(row) {
	
	//il n'y a rien en-dessous
	case 0:
		break;
		
	default:
		//en-dessous
		if (game.content(row-1, column) == 2) res+=2;
		else if (game.content(row-1, column) == 0) res+=1;
		break;
	}
	
	return res;
}

/* calcule la valeur d'utilité sur les diagonales concernées */
public static int compute_diag(GameState game, int row, int column) {
	int res = 0;
	
	switch(column) {
	
	case 0:
		
		switch(row) {
			
		case 0:
			//en haut à droite
			if (game.content(row+1, column+1) == 2) res+=2;
			else if (game.content(row+1, column+1) == 0) res+=1;
			break;
			
		case 5:
			//en bas à droite
			if (game.content(row-1, column+1) == 2) res+=2;
			else if (game.content(row-1, column+1) == 0) res+=1;
			break;
			
		default:
			//en haut à droite
			if (game.content(row+1, column+1) == 2) res+=2;
			else if (game.content(row+1, column+1) == 0) res+=1;
			//en bas à droite
			if (game.content(row-1, column+1) == 2) res+=2;
			else if (game.content(row-1, column+1) == 0) res+=1;
			break;
		}
		break;
		
	
	case 6:
		
		switch(row) {
		
		case 0:
			//en haut à gauche
			if (game.content(row+1, column-1) == 2) res+=2;
			else if (game.content(row+1, column-1) == 0) res+=1;
			break;
			
		case 5:
			//en bas à gauche
			if (game.content(row-1, column-1) == 2) res+=2;
			else if (game.content(row-1, column-1) == 0) res+=1;
			break;
			
		default:
			//en haut à gauche
			if (game.content(row+1, column-1) == 2) res+=2;
			else if (game.content(row+1, column-1) == 0) res+=1;
			//en bas à gauche
			if (game.content(row-1, column-1) == 2) res+=2;
			else if (game.content(row-1, column-1) == 0) res+=1;
			break;
		}
		break;
		
		
	default:
		
		switch(row) {
		
		case 0:
			//en haut à droite
			if (game.content(row+1, column+1) == 2) res+=2;
			else if (game.content(row+1, column+1) == 0) res+=1;
			//en haut à gauche
			if (game.content(row+1, column-1) == 2) res+=2;
			else if (game.content(row+1, column-1) == 0) res+=1;
			break;
			
		case 5:
			//en bas à gauche
			if (game.content(row-1, column-1) == 2) res+=2;
			else if (game.content(row-1, column-1) == 0) res+=1;
			//en bas à droite
			if (game.content(row-1, column+1) == 2) res+=2;
			else if (game.content(row-1, column+1) == 0) res+=1;
			break;
			
		default:
			//en bas à gauche
			if (game.content(row-1, column-1) == 2) res+=2;
			else if (game.content(row-1, column-1) == 0) res+=1;
			//en bas à droite
			if (game.content(row-1, column+1) == 2) res+=2;
			else if (game.content(row-1, column+1) == 0) res+=1;
			//en haut à droite
			if (game.content(row+1, column+1) == 2) res+=2;
			else if (game.content(row+1, column+1) == 0) res+=1;
			//en haut à gauche
			if (game.content(row+1, column-1) == 2) res+=2;
			else if (game.content(row+1, column-1) == 0) res+=1;
			break;
		
		}
	
	}
	return res;
}
















public static int compute_row_bonus(GameState game, int row, int column) {
	int res = 0, temp_res = 0, temp;
	int lb = Integer.max(0, column-3);
	int ub = Integer.min(6, column+3); 
	
	boolean run = true;
	
	while(run) {
		for (int i = lb; i <= Integer.min(lb+4, ub); i++) {
			temp = game.content(row, i);
			if (temp == 2)
				temp_res += 2;
			else if (temp == 0)
				temp_res += 1;
			
			if (ub - lb + 1 < 4)
				run = false;

			lb++;
		}
		res += temp_res;
		temp_res = 0;
	}
	return res;
}

public static int compute_column_bonus(GameState game, int row, int column) {
	int res = 0, temp_res = 0, temp;
	int lb = Integer.max(0, row-3);
	int ub = Integer.min(5, row+3); 
	
	boolean run = true;
	
	while(run) {
		for (int i = lb; i <= Integer.min(lb+4, ub); i++) {
			temp = game.content(i, column);
			if (temp == 2)
				temp_res += 2;
			else if (temp == 0)
				temp_res += 1;
			
			if (ub - lb + 1 < 4)
				run = false;

			lb++;
		}
		res += temp_res;
		temp_res = 0;
	}
	return res;
}

public static int compute_bonus_columns(GameState state){
    int col_count =0;
    for(int i=0; i<7; i++){
        for (int j=0; j<3; j++){
            if(state.content(i,j)==2){
                if(state.content(i,j+1)==2){
                    if(state.content(i,j+2)==2){
                        if(state.content(i,j+3)==2){
                            col_count +=3;
                        }
                    }
                    else{
                        col_count +=2;
                    }
                }
                else if(state.content(i,j+1)==0){
                    col_count +=1;
                }
            }
        }
    }
    return col_count;
}

public static int compute_bonus_rows(GameState state){
    int row_count=0;
    for(int i=0; i<4; i++){
        for (int j=0; j<6; j++){
            if(state.content(i,j)==2){
                if(state.content(i+1,j)==2){
                    if(state.content(i+2,j)==2){
                        if(state.content(i+3,j)==2){
                            row_count+=3;
                        }
                    }
                    else{
                        row_count+=2;
                    }
                }
                else if(state.content(i,j)==0){
                    row_count+=1;
                }
            }
        }
    }
    return row_count;
}

}