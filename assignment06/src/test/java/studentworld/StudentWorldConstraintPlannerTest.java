package studentworld;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.chocosolver.solver.variables.IntVar;
import org.junit.Before;
import org.junit.Test;

import gridgames.data.action.Action;
import gridgames.data.action.MoveAction;
import gridgames.display.ConsoleDisplay;
import gridgames.display.Display;
import gridgames.grid.Cell;
import studentworld.grid.StudentWorldBoard;
import studentworld.grid.StudentWorldCell;
import studentworld.player.StudentWorldPlayer;

public class StudentWorldConstraintPlannerTest {
	
	private StudentWorldConstraintPlanner planner;
	private StudentWorldPlayer player;
	private StudentWorld game;
	private StudentWorldBoard board;
	private Field moves;
	private Field playerCells;

	@Before
	public void setUp() throws Exception {
		List<Action> actions = MoveAction.getAllActions();
		Display display = new ConsoleDisplay();
		StudentWorld sw = new StudentWorld(display, 5, 5, 5);
		Cell initialCell = sw.getInitialCell();
		player = new StudentWorldPlayer(actions, display, initialCell);
		game = new StudentWorld(display, 5, 5, 5);
		board = (StudentWorldBoard)game.getBoard();
		
		moves = StudentWorldConstraintPlanner.class.getDeclaredField("moves");
		moves.setAccessible(true);
		playerCells = StudentWorldConstraintPlanner.class.getDeclaredField("playerCells");
		playerCells.setAccessible(true);
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testValidPlayerCells() {
		List<IntVar> playerCells;
		Set<Integer> possibleCellNums;
		int cell1Num;
		int cell2Num;
		int row1;
		int col1;
		
    	try {
    		player.addVisitedCell((StudentWorldCell)board.getCell(0,0));
    		player.addVisitedCell((StudentWorldCell)board.getCell(0,1));
    		player.addVisitedCell((StudentWorldCell)board.getCell(0,2));
    		player.addVisitedCell((StudentWorldCell)board.getCell(0,3));
    		player.addVisitedCell((StudentWorldCell)board.getCell(1,2));
        	planner = new StudentWorldConstraintPlanner(player, 1,3);
        	planner.getShortestPath();
			playerCells = (List<IntVar>)this.playerCells.get(planner);
						
			for(int i=1; i<playerCells.size(); i++) {
				possibleCellNums = new HashSet<Integer>();
				cell1Num = playerCells.get(i-1).getValue();
				cell2Num = playerCells.get(i).getValue();
				row1 = cell1Num/5;
				col1 = cell1Num%5;
				
				if(row1 > 0) {
					possibleCellNums.add(cell1Num-5);
				}
				if(col1 < 4) {
					possibleCellNums.add(cell1Num+1);
				}
				if(row1 < 4) {
					possibleCellNums.add(cell1Num+5);
				}
				if(col1 > 0) {
					possibleCellNums.add(cell1Num-5);
				}
				assertTrue("playerCells contains consecutive cells that are not adjacent", possibleCellNums.contains(cell2Num));
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail("check the console for the exception stack trace");
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			fail("check the console for the exception stack trace");
		} catch (Exception e) {
			e.printStackTrace();
			fail("check the console for the exception stack trace");
		} 	
   	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testValidMoves() {
		List<IntVar> playerCells;
		List<IntVar> moves;
		int cell1Num;
		int cell2Num;
		
    	try {
    		player.addVisitedCell((StudentWorldCell)board.getCell(0,0));
    		player.addVisitedCell((StudentWorldCell)board.getCell(0,1));
    		player.addVisitedCell((StudentWorldCell)board.getCell(0,2));
    		player.addVisitedCell((StudentWorldCell)board.getCell(0,3));
    		player.addVisitedCell((StudentWorldCell)board.getCell(1,2));
        	planner = new StudentWorldConstraintPlanner(player, 1,3);
        	planner.getShortestPath();
			playerCells = (List<IntVar>)this.playerCells.get(planner);
			moves = (List<IntVar>)this.moves.get(planner);
			
			assertEquals("moves should contain one fewer element than playerCells", 1, playerCells.size()-moves.size());
			
			for(int i=1; i<playerCells.size(); i++) {
				cell1Num = playerCells.get(i-1).getValue();
				cell2Num = playerCells.get(i).getValue();
				
				if(cell2Num == cell1Num-5) {
					assertEquals("moves do not correspond to difference in consecutive cells in playerCells", 0, moves.get(i-1).getValue());
				} else if(cell2Num == cell1Num+1) {
					assertEquals("moves do not correspond to difference in consecutive cells in playerCells", 1, moves.get(i-1).getValue());
				} else if(cell2Num == cell1Num+5) {
					assertEquals("moves do not correspond to difference in consecutive cells in playerCells", 2, moves.get(i-1).getValue());
				} else if(cell2Num == cell1Num-1) {
					assertEquals("moves do not correspond to difference in consecutive cells in playerCells", 3, moves.get(i-1).getValue());
				} else {
					fail("moves do not correspond to difference in consecutive cells in playerCells");
				}
				
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail("check the console for the exception stack trace");
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			fail("check the console for the exception stack trace");
		} catch (Exception e) {
			e.printStackTrace();
			fail("check the console for the exception stack trace");
		} 
	}

	@Test
	public void testOneMove() {
		List<Action> moveActions;

		player.addVisitedCell((StudentWorldCell)board.getCell(0,0));
    	planner = new StudentWorldConstraintPlanner(player, 1,0);
		moveActions = planner.getShortestPath();
		assertEquals("planner not returning the correct number of actions", 1, moveActions.size());
		assertEquals("planner not returning the correct action", MoveAction.DOWN, moveActions.get(0));
		
		player.addVisitedCell((StudentWorldCell)board.getCell(0,0));
    	planner = new StudentWorldConstraintPlanner(player, 0,1);
		moveActions = planner.getShortestPath();		
		assertEquals("planner not returning the correct number of actions", 1, moveActions.size());
		assertEquals("planner not returning the correct action", MoveAction.RIGHT, moveActions.get(0));
	}
	
	@Test
	public void testMultipleMovesWithExtraVisited() {
		List<Action> moveActions;

		player.addVisitedCell((StudentWorldCell)board.getCell(0,0));
		player.addVisitedCell((StudentWorldCell)board.getCell(1,0));
		player.addVisitedCell((StudentWorldCell)board.getCell(1,1));
		player.addVisitedCell((StudentWorldCell)board.getCell(1,2));
		player.addVisitedCell((StudentWorldCell)board.getCell(0,2));
		player.addVisitedCell((StudentWorldCell)board.getCell(2,2));
    	planner = new StudentWorldConstraintPlanner(player, 3,2);
		moveActions = planner.getShortestPath();	
		
		assertEquals("planner not returning the correct number of actions", 5, moveActions.size());
		assertEquals("planner not returning the correct actions (first is incorrect)", MoveAction.DOWN, moveActions.get(0));
		assertEquals("planner not returning the correct actions (second is incorrect)", MoveAction.RIGHT, moveActions.get(1));
		assertEquals("planner not returning the correct actions (third is incorrect)", MoveAction.RIGHT, moveActions.get(2));
		assertEquals("planner not returning the correct actions (fourth is incorrect)", MoveAction.DOWN, moveActions.get(3));
		assertEquals("planner not returning the correct actions (fifth is incorrect)", MoveAction.DOWN, moveActions.get(4));
	}
	
	@Test
	public void testMoreThanMinimumMoves() {
		List<Action> moveActions;
		
		player.addVisitedCell((StudentWorldCell)board.getCell(0,0));
    	player.addVisitedCell((StudentWorldCell)board.getCell(0,1));
    	player.addVisitedCell((StudentWorldCell)board.getCell(0,2));
    	player.addVisitedCell((StudentWorldCell)board.getCell(0,3));
    	player.addVisitedCell((StudentWorldCell)board.getCell(1,3));
    	player.addVisitedCell((StudentWorldCell)board.getCell(2,3));
    	player.addVisitedCell((StudentWorldCell)board.getCell(3,3));
    	player.addVisitedCell((StudentWorldCell)board.getCell(3,2));
    	player.addVisitedCell((StudentWorldCell)board.getCell(3,1));
    	planner = new StudentWorldConstraintPlanner(player, 2,1);
		moveActions = planner.getShortestPath();
		
		assertEquals("planner not returning the correct number of actions", 9, moveActions.size());
		assertEquals("planner not returning the correct actions (first is incorrect)", MoveAction.RIGHT, moveActions.get(0));
		assertEquals("planner not returning the correct actions (second is incorrect)", MoveAction.RIGHT, moveActions.get(1));
		assertEquals("planner not returning the correct actions (third is incorrect)", MoveAction.RIGHT, moveActions.get(2));
		assertEquals("planner not returning the correct actions (fourth is incorrect)", MoveAction.DOWN, moveActions.get(3));
		assertEquals("planner not returning the correct actions (fifth is incorrect)", MoveAction.DOWN, moveActions.get(4));
		assertEquals("planner not returning the correct actions (sixth is incorrect)", MoveAction.DOWN, moveActions.get(5));
		assertEquals("planner not returning the correct actions (seventh is incorrect)", MoveAction.LEFT, moveActions.get(6));
		assertEquals("planner not returning the correct actions (eighth is incorrect)", MoveAction.LEFT, moveActions.get(7));
		assertEquals("planner not returning the correct actions (ninth is incorrect)", MoveAction.UP, moveActions.get(8));
	}
}
