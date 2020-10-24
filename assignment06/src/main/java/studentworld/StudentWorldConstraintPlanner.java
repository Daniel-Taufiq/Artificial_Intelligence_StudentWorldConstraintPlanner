package studentworld;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.chocosolver.solver.ICause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IVariableMonitor;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.RealVar;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.delta.IDelta;
import org.chocosolver.solver.variables.delta.IIntDeltaMonitor;
import org.chocosolver.solver.variables.events.IEventType;
import org.chocosolver.solver.variables.view.IView;
import org.chocosolver.util.iterators.DisposableRangeIterator;
import org.chocosolver.util.iterators.DisposableValueIterator;
import org.chocosolver.util.iterators.EvtScheduler;
import org.chocosolver.util.objects.setDataStructures.iterable.IntIterableSet;

import gridgames.data.action.Action;
import gridgames.data.action.MoveAction;
import gridgames.grid.Cell;
import studentworld.grid.StudentWorldCell;
import studentworld.player.StudentWorldPlayer;

public class StudentWorldConstraintPlanner {
	private StudentWorldPlayer player;
	private Model model;
	private int targetRow;
	private int targetCol;
	private List<IntVar> moves;
	private List<IntVar> playerCells;
	private Solver solver;
	
	public StudentWorldConstraintPlanner(StudentWorldPlayer player, int targetRow, int targetCol) {
		this.player = player;
		this.targetRow = targetRow;
		this.targetCol = targetCol;
	}
	
	public void initializeConstraintPlanner() {
		model = new Model("studentworld planner");
		moves = new ArrayList<IntVar>();
		playerCells = new ArrayList<IntVar>();
		solver = model.getSolver();
	}
	
	public List<Action> getShortestPath() {
		int numMoves = getMinNumMoves(player.getCell(), targetRow, targetCol);
		
		do {
			initializeConstraintPlanner();
			createVariables(numMoves);
			createConstraints(numMoves);
			numMoves++;
		} while(!solver.solve());
		
		return getMoveActions();
	}
	
	private void createVariables(int numMoves) {	
		// get current cell
        StudentWorldCell currentCell = (StudentWorldCell) player.getCell();
        int currentRow = currentCell.getRow();
        int currentCol = currentCell.getCol();

        //************UPDATE MOVES************
        System.out.println(numMoves);
        System.out.println("moves:");
        // moves
        for (int i = 0; i < numMoves; i++) 
        {
        	moves.add(model.intVar(0,3));
        }
        
        System.out.println("Stored moves with domains: [0, 3] " + "x" + numMoves);
        
        
        //************UPDATE PLAYERCELLS************
        
        // store current position (initial cell)
        System.out.println("\nplayerCells: (x" + (numMoves+1) + ")");
        this.playerCells.add(model.intVar(getCellNum(currentRow, currentCol)));
        System.out.println("Stored: " + getCellNum(currentRow, currentCol));
        // store in-between domains
        for(int i = 0; i < numMoves-1; i++)	// -1 since we populate last index with target cell
        {
        	playerCells.add(model.intVar(0, 24));
        	
        }
        System.out.println("Stored positions with domains: [0, 24] " + "x"+ (numMoves - 1));
        // store target position
        this.playerCells.add(model.intVar(getCellNum(targetRow, targetCol)));
        System.out.println("Stored: " + getCellNum(targetRow, targetCol) + "\n\n");
	}
	

	private void createConstraints(int numMoves) {
		// get current cell
        StudentWorldCell currentCell = (StudentWorldCell) player.getCell();
        int currentRow = currentCell.getRow();
        int currentCol = currentCell.getCol();
        int index = 0;

        // create array with flatten list of visited cells
        int[] visitedArr = new int[player.getVisitedCells().size()];
        
        
        // get a list of all the pre-defined neighbors that was initiated to true before the game
        ArrayList<IntVar> visitedCells = new ArrayList<IntVar>();
        IntVar[] preDefinedVisitedIntVars;
        preDefinedVisitedIntVars = new IntVar[visitedArr.length];;
		for (StudentWorldCell visitedCell : player.getVisitedCells()) 
		{
			visitedArr[index] = getCellNum(visitedCell.getRow(), visitedCell.getCol());
			preDefinedVisitedIntVars[index] = model.intVar(getCellNum(visitedCell.getRow(), visitedCell.getCol()));
			visitedCells.add(model.intVar(getCellNum(visitedCell.getRow(), visitedCell.getCol())));
			index++;
		}
	
		
		// set constraints on first and last cell
		model.arithm(playerCells.get(0), "=", getCellNum(currentCell.getRow(), currentCell.getCol())).post();
		model.arithm(playerCells.get(playerCells.size()-1), "=", getCellNum(targetRow, targetCol)).post();

		
		for(int k = 0; k < numMoves; k++)
		{
			validPlayerCellConstrains(k);
		}
	
		
		// we want to update every number of moves in playerCells and moves
		for (int i = 0; i < numMoves; i++) {
			// we want to set constraints on all the cells in the board
			for(int cellPos = 0; cellPos < 25; cellPos++) {
				// we need to identify the valid neighbors for the given player cell
				model.ifThen(model.arithm(playerCells.get(i), "=", cellPos), model.or(model.arithm(playerCells.get(i+1), "=", cellPos+5),
						model.arithm(playerCells.get(i+1), "=", cellPos-5), model.arithm(playerCells.get(i+1), "=", cellPos+1),
						model.arithm(playerCells.get(i+1), "=", cellPos-1)));
				
				
				// we need to set valid neighbors
				if(cellPos == 0)
				{
					validPlayerCellConstrains(i);
				}
				
				// check if the cell is a valid neighbor and cannot be assigned to non-visited
				if(!cellMatchVisit(visitedArr, cellPos))
				{
					// set fixed constraint on matching a non-visited cell position
					model.arithm(playerCells.get(i), "!=", cellPos).post();
				}
				
			}
			
			// identify the next possible moves
			model.ifThen(model.arithm(playerCells.get(i),"-",playerCells.get(i+1),"=",-5),model.arithm(moves.get(i),"=", model.intVar(2)));	// down
			model.ifThen(model.arithm(playerCells.get(i),"-",playerCells.get(i+1),"=",5),model.arithm(moves.get(i),"=",model.intVar(0)));	// up
			model.ifThen(model.arithm(playerCells.get(i),"-",playerCells.get(i+1),"=",1),model.arithm(moves.get(i),"=",model.intVar(3)));	// left
			model.ifThen(model.arithm(playerCells.get(i),"-",playerCells.get(i+1),"=",-1),model.arithm(moves.get(i),"=",model.intVar(1)));	// right
		}
	}

	public boolean cellMatchVisit(int[] visitedArr, int cellPos) 
	{
		for (int i = 0; i < visitedArr.length; i++) 
		{
			// if they match, then we know we can visit them
			if (visitedArr[i] == cellPos) 
			{
				return true;
			}
		}
		// if we haven't found a match, that means we have to set 
		// playerCells value to false for that cell position
		return false;
	}

	
	public void visitConstraints(ArrayList<IntVar> visitedCells, int numMoves)
	{
		
		for (int i = 0; i < numMoves; i++) {
			for (int j = 0; j < visitedCells.size(); j++) {
		model.ifThen(model.and(
				model.or(model.arithm(visitedCells.get(j), "=", 4), model.arithm(visitedCells.get(j), "=", 9),
						model.arithm(visitedCells.get(j), "=", 14), model.arithm(visitedCells.get(j), "=", 19),
						model.arithm(visitedCells.get(j), "=", 24)),
				model.or(model.arithm(playerCells.get(i), "=", 5), model.arithm(playerCells.get(i), "=", 10),
						model.arithm(playerCells.get(i), "=", 15), model.arithm(playerCells.get(i), "=", 20))),
				model.arithm(playerCells.get(i + 1), "!=", visitedCells.get(j)));
			}
		}
	}
	
	
	public void validPlayerCellConstrains(int i)
	{
		model.ifThen(model.arithm(playerCells.get(i), "=", 0),
				model.or(model.arithm(playerCells.get(i + 1), "=", 1), model.arithm(playerCells.get(i + 1), "=", 5)));
		model.ifThen(model.arithm(playerCells.get(i), "=", 1), model.or(model.arithm(playerCells.get(i + 1), "=", 0),
				model.arithm(playerCells.get(i + 1), "=", 6), model.arithm(playerCells.get(i + 1), "=", 2)));
		model.ifThen(model.arithm(playerCells.get(i), "=", 2), model.or(model.arithm(playerCells.get(i + 1), "=", 1),
				model.arithm(playerCells.get(i + 1), "=", 7), model.arithm(playerCells.get(i + 1), "=", 3)));
		model.ifThen(model.arithm(playerCells.get(i), "=", 3), model.or(model.arithm(playerCells.get(i + 1), "=", 2),
				model.arithm(playerCells.get(i + 1), "=", 8), model.arithm(playerCells.get(i + 1), "=", 4)));
		model.ifThen(model.arithm(playerCells.get(i), "=", 4),
				model.or(model.arithm(playerCells.get(i + 1), "=", 3), model.arithm(playerCells.get(i + 1), "=", 9)));
		model.ifThen(model.arithm(playerCells.get(i), "=", 5), model.or(model.arithm(playerCells.get(i + 1), "=", 10),
				model.arithm(playerCells.get(i + 1), "=", 6), model.arithm(playerCells.get(i + 1), "=", 0)));
		model.ifThen(model.arithm(playerCells.get(i), "=", 6),
				model.or(model.arithm(playerCells.get(i + 1), "=", 1), model.arithm(playerCells.get(i + 1), "=", 5),
						model.arithm(playerCells.get(i + 1), "=", 11), model.arithm(playerCells.get(i + 1), "=", 7)));
		model.ifThen(model.arithm(playerCells.get(i), "=", 7),
				model.or(model.arithm(playerCells.get(i + 1), "=", 2), model.arithm(playerCells.get(i + 1), "=", 6),
						model.arithm(playerCells.get(i + 1), "=", 12), model.arithm(playerCells.get(i + 1), "=", 8)));
		model.ifThen(model.arithm(playerCells.get(i), "=", 8),
				model.or(model.arithm(playerCells.get(i + 1), "=", 3), model.arithm(playerCells.get(i + 1), "=", 7),
						model.arithm(playerCells.get(i + 1), "=", 9), model.arithm(playerCells.get(i + 1), "=", 13)));
		model.ifThen(model.arithm(playerCells.get(i), "=", 9), model.or(model.arithm(playerCells.get(i + 1), "=", 4),
				model.arithm(playerCells.get(i + 1), "=", 8), model.arithm(playerCells.get(i + 1), "=", 14)));
		model.ifThen(model.arithm(playerCells.get(i), "=", 10), model.or(model.arithm(playerCells.get(i + 1), "=", 5),
				model.arithm(playerCells.get(i + 1), "=", 11), model.arithm(playerCells.get(i + 1), "=", 15)));
		model.ifThen(model.arithm(playerCells.get(i), "=", 11),
				model.or(model.arithm(playerCells.get(i + 1), "=", 6), model.arithm(playerCells.get(i + 1), "=", 10),
						model.arithm(playerCells.get(i + 1), "=", 16), model.arithm(playerCells.get(i + 1), "=", 12)));
		model.ifThen(model.arithm(playerCells.get(i), "=", 12),
				model.or(model.arithm(playerCells.get(i + 1), "=", 7), model.arithm(playerCells.get(i + 1), "=", 13),
						model.arithm(playerCells.get(i + 1), "=", 17), model.arithm(playerCells.get(i + 1), "=", 11)));
		model.ifThen(model.arithm(playerCells.get(i), "=", 13),
				model.or(model.arithm(playerCells.get(i + 1), "=", 14), model.arithm(playerCells.get(i + 1), "=", 8),
						model.arithm(playerCells.get(i + 1), "=", 12), model.arithm(playerCells.get(i + 1), "=", 18)));
		model.ifThen(model.arithm(playerCells.get(i), "=", 14), model.or(model.arithm(playerCells.get(i + 1), "=", 9),
				model.arithm(playerCells.get(i + 1), "=", 19), model.arithm(playerCells.get(i + 1), "=", 13)));
		model.ifThen(model.arithm(playerCells.get(i), "=", 15), model.or(model.arithm(playerCells.get(i + 1), "=", 10),
				model.arithm(playerCells.get(i + 1), "=", 16), model.arithm(playerCells.get(i + 1), "=", 20)));
		model.ifThen(model.arithm(playerCells.get(i), "=", 16),
				model.or(model.arithm(playerCells.get(i + 1), "=", 17), model.arithm(playerCells.get(i + 1), "=", 11),
						model.arithm(playerCells.get(i + 1), "=", 15), model.arithm(playerCells.get(i + 1), "=", 21)));
		model.ifThen(model.arithm(playerCells.get(i), "=", 17),
				model.or(model.arithm(playerCells.get(i + 1), "=", 12), model.arithm(playerCells.get(i + 1), "=", 18),
						model.arithm(playerCells.get(i + 1), "=", 22), model.arithm(playerCells.get(i + 1), "=", 16)));
		model.ifThen(model.arithm(playerCells.get(i), "=", 18),
				model.or(model.arithm(playerCells.get(i + 1), "=", 13), model.arithm(playerCells.get(i + 1), "=", 19),
						model.arithm(playerCells.get(i + 1), "=", 23), model.arithm(playerCells.get(i + 1), "=", 17)));
		model.ifThen(model.arithm(playerCells.get(i), "=", 19), model.or(model.arithm(playerCells.get(i + 1), "=", 14),
				model.arithm(playerCells.get(i + 1), "=", 24), model.arithm(playerCells.get(i + 1), "=", 18)));
		model.ifThen(model.arithm(playerCells.get(i), "=", 20),
				model.or(model.arithm(playerCells.get(i + 1), "=", 21), model.arithm(playerCells.get(i + 1), "=", 15)));
		model.ifThen(model.arithm(playerCells.get(i), "=", 21), model.or(model.arithm(playerCells.get(i + 1), "=", 22),
				model.arithm(playerCells.get(i + 1), "=", 16), model.arithm(playerCells.get(i + 1), "=", 20)));
		model.ifThen(model.arithm(playerCells.get(i), "=", 22), model.or(model.arithm(playerCells.get(i + 1), "=", 23),
				model.arithm(playerCells.get(i + 1), "=", 17), model.arithm(playerCells.get(i + 1), "=", 21)));
		model.ifThen(model.arithm(playerCells.get(i), "=", 23), model.or(model.arithm(playerCells.get(i + 1), "=", 24),
				model.arithm(playerCells.get(i + 1), "=", 18), model.arithm(playerCells.get(i + 1), "=", 22)));
		model.ifThen(model.arithm(playerCells.get(i), "=", 24),
				model.or(model.arithm(playerCells.get(i + 1), "=", 19), model.arithm(playerCells.get(i + 1), "=", 23)));
	}

		
		
	
	private List<Action> getMoveActions() {
		List<Action> moveActions = new ArrayList<Action>();
		int plannerMove;
		for(IntVar move: moves) {
			plannerMove = move.getValue();
			if(plannerMove == 0) {
				moveActions.add(MoveAction.UP);
			} else if(plannerMove == 1) {
				moveActions.add(MoveAction.RIGHT);
			} else if(plannerMove == 2) {
				moveActions.add(MoveAction.DOWN);
			} else if(plannerMove == 3) {
				moveActions.add(MoveAction.LEFT);
			}
		}
		return moveActions;
	}
	
	private int getMinNumMoves(Cell startCell, int targetRow, int targetCol) {
		int startRow = startCell.getRow();
		int startCol = startCell.getCol();
		return Math.abs(targetRow-startRow) + Math.abs(targetCol-startCol);
	}
	
	private int getCellNum(int row, int col) {
		return row*5+col;
	}
}
