import java.util.*;
import java.util.Map.Entry;

/* 
 * AI PROJECT - M1 I2D, MIAGE
 * 
 * Group Members:
 * 		- ACHOUR Myriam 
 * 		- FERNANDES MACEDO Gabriella
 * 		- PERIANAYAGASSAMY Oscar
 * 		- SURESH Sabana
 * 
 */



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
	boolean is_leaf;

	public ContingencyPlan() {
		this.heuristic_value = Double.NEGATIVE_INFINITY;
		this.is_leaf = true;
	}

	public ContingencyPlan(Integer action, HashMap<BeliefState, ContingencyPlan> plan, double heuristic_value) {
		this.action = action;
		this.plan = plan;
		this.heuristic_value = heuristic_value;
		this.is_leaf = false;
	}

	public ContingencyPlan(Integer action) {
		this.action = action;
		this.plan = new HashMap<BeliefState, ContingencyPlan>();
		this.is_leaf = false;
	}

	public void setIs_leaf(boolean is_leaf) {
		this.is_leaf = is_leaf;
	}
	
	public void setAction(Integer action) {
		this.action = action;
	}
	
	public void setPlan(HashMap<BeliefState, ContingencyPlan> plan) {
		this.plan = plan;
	}
	
	public void setHeuristicValue(double heuristic_value) {
		this.heuristic_value = heuristic_value;
	}
	
	public boolean getIsLeaf() {
		return this.is_leaf;
	}
	
	public Integer getAction() {
		return action;
	}
	
	public HashMap<BeliefState, ContingencyPlan> getPlan() {
		return plan;
	}
	
	public double getHeuristicValue() {
		return this.heuristic_value;
	}

	public void add(BeliefState state, ContingencyPlan subplan) {
		this.plan.put(state, subplan);
	}
	
	public void isLeaf() {
		this.is_leaf = true;
	}	
	
	public double heuristic() {
		//this value should not be used in computations, it is a warning value
		if (this.is_leaf)
			return Double.NEGATIVE_INFINITY;
		
		double heuristic_value = 0.;
		
		for (BeliefState state : this.plan.keySet()) {
			if (this.plan.get(state).is_leaf)
				heuristic_value += AI.heuristic(state);
				
			else
				heuristic_value += this.plan.get(state).heuristic();
				
		}
		return heuristic_value;
	}
}


public class AI{

	//exploredSet to keep the value of the BeliefState explored during orSearch and andSearch
	static ExploredSet exploredSet =new ExploredSet();
	//maximum depth of search of the algorithm (it is better if it is set on 1)
	final static int DEPTH = 1; 
	/* heuristic table that will be used in the future computations
	 * each entry of the table is the number of lines of 4 that the case is on
	 * for example, for the entry in the upper left corner there is the number 3 because there are three 4-lines starting at 0
	 *   _ _ _ _ _ _ _ 
	 *  |0 1 1 1 * * *|
	 *  |2 3 * * * * *|
	 *  |2 * 3 * * * *|
	 *  |2 * * 3 * * *|
	 *  |* * * * * * *|
	 *  |* * * * * * *|
	 *   ‾ ‾ ‾ ‾ ‾ ‾ ‾
	 *   The computation for the heuristic value will be detailed later
	 */
	final static int HEURISTIC[][] = new int[][] {{3,4,5,7,5,4,3},
		   										  {4,6,8,10,8,6,4},
		   										  {5,8,11,13,11,8,5},
		   										  {5,8,11,13,11,8,5},
		   										  {4,6,8,10,8,6,4},
		   										  {3,4,5,7,5,4,3}};
    
    //weight for columns : We put weight on pawns that are positioned on the sides of the board instead of the center to avoid a concentration of pawns in the three centered columns.
    final static double cweights[] = new double[] {1.1, 1., 0.9, 0.8, 0.9, 1, 1.1}; 
    //weights for rows : We put weight on pawns that are positioned at the bottom of the board because they are more likely to form a 4-disc line in early game.
    final static double rweights[] = new double[] {1.2, 1., 0.7, 0.5, 0.3, 0.1};    
		   										  
	public AI() {
	}


	/** Perform the computation of an heuristic value for a given GameState 
	 *  @param game the game state which is currently considered
	 *  @return the heuristic value of the game state 
	 */
	public static double heuristic(GameState game) {
		int heuristic_value = 0;						   
		
		//We consider each row...
		for (int row = 0; row < 6; row++) {
			
			//...and each column
			for (int column = 0; column < 7; column++) {
				
				//If the pawn at position (row, column) is red
				if (game.content(row, column) == 2)
					
					//the computation is : weight x ( #{number of lines of 4 discs that the red pawn is on} - #{number of lines of 4 discs that the red pawn is on AND that a yellow pawn is blocking} )
					//the weights are such that it is risker to play at the top than at the bottom of the board and to counterbalance the fact that the agent will try to play only in the middle
					//and it is risker to play in the centre of the board than on its sides
					heuristic_value += cweights[column] * rweights[row] *(HEURISTIC[row][column] - scan(game, row, column, 1)); 
				
				//If the pawn at position (row, column) is yellow, we penalize the heuristic value following the previous explanation
				if (game.content(row, column) == 1)
					heuristic_value -= cweights[column] * rweights[row] *(HEURISTIC[row][column] - scan(game, row, column, 2));
			}
		}
		return game.proba()*((double) heuristic_value);
	}
	
	
	/** Perform the computation of an heuristic value for a given BeliefState : we sum the heuristic value of each game state composing the belief state 
	 *  @param currentBeliefState the belief state which is currently considered
	 *  @return the heuristic value of the belief state 
	 */
	public static double heuristic(BeliefState currentBeliefState){
		double value_of_belief_state =0;
		for(GameState game : currentBeliefState) {
			value_of_belief_state += heuristic(game);
		}
		return value_of_belief_state;
	}
	
	
	/** Perform the computation of an heuristic value for a given Results object : we sum the heuristic value of each belief state
	 *  @param predictions the Results object that we are dealing with  (result of the function predict())
	 *  @return the heuristic value of the Results object
	 */
	public static double heuristic(Results predictions) {
		double res = 0.0f;
		
		for (BeliefState beliefState : predictions) {
			for (GameState state : beliefState)
				res += heuristic(state);
		}
		return res;
	}
	
	/**
	 * Perform the computation of an heuristic value for a given hash map object : we sum the heuristic value of each contingency plan
	 * @param hmap a hash map that maps belief states to contingency plans
	 * @return a utility value
	 */
	public static double heuristic(HashMap<BeliefState, ContingencyPlan> hmap) {
		double heuristic_value = 0.;
		
		if (hmap.isEmpty())
			return heuristic_value;
		
		for (BeliefState state : hmap.keySet()) {
			
			//If the associated contingency plan is for a belief state that is a leaf... we won't dive deeper in the recursion
			if (hmap.get(state).is_leaf)
				heuristic_value += heuristic(state);
			
			//...else we follow the recursion
			else
				heuristic_value += hmap.get(state).heuristic();
		}
		return heuristic_value;
	}
	
	
	/**
	 * Sorts (Insertion Sort) the ArrayList<Integer> moves in decreasing order such that the first move leads to a state with the highest heuristic value when applied the BeliefState state.
	 * @param moves ArrayList of integers which are the moves that are allowed in the current situation of the belief state
	 * @param state belief state that is currently considered
	 */
	public static void sort_moves(ArrayList<Integer> moves, BeliefState state) {
		int n = moves.size();
		
		for (int i = 0; i < n; i++) {
			
			Integer key = moves.get(i);
			double key_value = heuristic(state.copy().putPiecePlayer(key));
			int j = i - 1;
			
			while (j >= 0 && heuristic(state.copy().putPiecePlayer(moves.get(j))) < key_value) {
				
				moves.set(j+1, moves.get(j));
				j--;
			}
			moves.set(j + 1, key);
		}
	}
	
	/**
	 * Performs the AndOrSearch algorithm at a Or-node level
	 * @param currentBeliefState The current belief state of the game
	 * @param path Set of belief states that have already been visited
	 * @param depth_of_prediction Depth at which we should stop the search
	 * @return a ContingencyPlan object which is composed of an action and a hash table which maps belief states to contingency plans
	 */
	public static ContingencyPlan orSearch(BeliefState currentBeliefState, ArrayList<BeliefState> path,int depth_of_prediction) {
		HashMap<BeliefState, ContingencyPlan> subplan;
		//the plan resulting from the and-search
		ContingencyPlan plan_res;
		//the plan that will be returned and which has maximum heuristic value
		ContingencyPlan max_plan = new ContingencyPlan();
		
		//To stop the search when the maximum depth is reached or if the game is over, we return an empty plan 
		if (depth_of_prediction > DEPTH || currentBeliefState.isGameOver()) 
			return new ContingencyPlan();
		
		//If the belief state has already been visited we return null value
		if (path.contains(currentBeliefState))
			return null;
		
		path.add(currentBeliefState);
		
		ArrayList<Integer> moves = currentBeliefState.getMoves();
		
		if (moves.size() == 1)
			return new ContingencyPlan(moves.get(0));
		
		sort_moves(moves, currentBeliefState);
		
		//We consider each possible action...
		for (Integer action : moves) {
			
			//we perform the and-or search algorithm for the and-node which results of the action of putting the piece action on the board
			subplan = andSearch(currentBeliefState.copy().putPiecePlayer(action), path, depth_of_prediction+1);
			
			if (subplan != null) {
				
				if (subplan.isEmpty()) //We reached the maximum depth...
					plan_res = new ContingencyPlan(action, subplan, heuristic(currentBeliefState.copy().putPiecePlayer(action)));
				else
					plan_res = new ContingencyPlan(action, subplan, heuristic(subplan));
				
				//if the resulting plan has higher heuristic value than the current maximum contingency plan... we update the maximum plan
				if (plan_res.getHeuristicValue() > max_plan.getHeuristicValue())
					max_plan = plan_res;
			}
			
		}
		
		//if we do not find a better plan than the empty plan... we return null value
		if (max_plan.getHeuristicValue() == Double.NEGATIVE_INFINITY) {
			return null;
		}
		return max_plan;
	}

	
	/**
	 * performs the AndOrSearch algorithm at a And-level node
	 * @param currentBeliefStates Results object that is a set of belief states that results from a particular action
	 * @param path Set of belief states that have already been visited
	 * @param depth_of_prediction Depth at which we should stop the search
	 * @return A hash table that maps to each belief state that may exist after performing a particular action a contingency plan
	 */
	public static HashMap<BeliefState, ContingencyPlan> andSearch(Results currentBeliefStates,ArrayList<BeliefState> path, int depth_of_prediction) {
		
		HashMap<BeliefState, ContingencyPlan> hmap = new HashMap<BeliefState,ContingencyPlan>();
		ContingencyPlan subplan;
		
		//If the Results object is empty or if the maximum depth is reached, we return an empty hash map
		if (currentBeliefStates == null || depth_of_prediction > DEPTH) 
			return new HashMap<BeliefState,ContingencyPlan>();
		
		//for each belief state in the Results object...
		for (BeliefState state : currentBeliefStates) {
			
			//We predict the move of the other player
			Results predictions = state.copy().predict();
			
			//If there are no predictions left... we skip this iteration
			if (predictions == null) 
				continue;
			
			//for each belief state that may exist after the predictions...
			for (BeliefState substate : predictions) {
				
				//we retrieve the subplan associated with this substate
				subplan = orSearch(substate, path, depth_of_prediction+1);
				if (subplan == null)
					return null;
				
				//we add the plan if it is not null
				hmap.put(substate, subplan);
			}
		}
		return hmap;
	}
	
	
	
	/**
	 * Compute a penalty for our AI (or for the opponent depending on the value of the value of the parameter opponent) that compute the number of valid 4-disc lines that is blocked by an opponent pawn.
	 * We enumerate each position using two nested switch-case structures. Each if-condition verifies if at least one element of the 4-disc line considered is occupied by an opponent piece. If it is the case, we increment the value of cpt.
	 * @param game the game state we consider
	 * @param row the row considered
	 * @param column the column considered
	 * @param opponent if 1, the computation is made for our AI. Otherwise it is the reverse
	 * @return the number of valid 4-disc lines that is blocked by an opponent pawn
	 */
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
				if (game.content(row+1, column-1) == opponent || game.content(row+2, column-2) == opponent || game.content(row+3, column-3) == opponent)
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
				if (game.content(row-1, column+1) == opponent || game.content(row-2, column+2) == opponent || game.content(row-3, column+3) == opponent)
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
	
	
	/**
	 * Return the best action to take in the current situation
	 * @param game the current game state
	 * @return an integer which represents the column to play
	 */
	public static int findNextMove(BeliefState game) {
		ContingencyPlan plan = orSearch(game,new ArrayList<BeliefState>(),1);
        return plan.action;
	}
}