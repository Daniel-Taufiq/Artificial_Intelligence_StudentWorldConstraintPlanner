package studentworld.grid;

import java.util.*;

import gridgames.data.item.Item;
import gridgames.data.item.MoveItem;
import gridgames.grid.Board;
import studentworld.data.StudentWorldItem;
import studentworld.data.StudentWorldPercept;

public class StudentWorldBoard extends Board {

    private int numStudents;

    public StudentWorldBoard(int numRows, int numCols, int numStudents) {
    	super(numRows, numCols);
        this.numStudents = numStudents;

        for(int i=0; i<this.numRows; i++) {
            for(int j=0; j<this.numCols; j++) {
                cells[i][j] = new StudentWorldCell(i, j);
            }
        }
    }

    public void initializeBoard() {
    	cells[0][0].add(MoveItem.PLAYER);
    	cells[0][0].setVisited(true);
        placeDoor();
        placeStudents(numStudents);
        addPercepts();
    }

    public boolean didLose() {
        return getPlayerCell().getItems().contains(StudentWorldItem.STUDENT);
    }

    public boolean didWin() {
        return getPlayerCell().getItems().contains(StudentWorldItem.DOOR);
    }

    public Set<StudentWorldPercept> getCurrentPercepts() {
        return ((StudentWorldCell) getPlayerCell()).getPercepts();
    }

    private void placeDoor() {
        Random r = new Random();
        int row;
        int col;
        //repeat until a valid placement is found
        do {
            row = r.nextInt(this.numRows);
            col = r.nextInt(this.numCols);
        } while(row+col <= 1);
        cells[row][col].add(StudentWorldItem.DOOR);
    }

    private void placeStudents(int numStudents) {
        Random r = new Random();
        int row;
        int col;
        for(int i=0; i<numStudents; i++) {
            //repeat until a valid placement is found
            do {
                row = r.nextInt(this.numRows);
                col = r.nextInt(this.numCols);
            } while(!isValidPlacement(row, col));

            cells[row][col].add(StudentWorldItem.STUDENT);

            //if placement causes board to be unsolvable, undo it
            if(!isSolveable((StudentWorldCell) getPlayerCell())) {
                cells[row][col].remove(StudentWorldItem.STUDENT);
                i--;
            }
        }
    }

    private void addPercepts() {
        StudentWorldCell currentCell;
        List<StudentWorldCell> adjacentCells;
        for(int i=0; i<this.numRows; i++) {
            for(int j=0; j<this.numCols; j++) {
                currentCell = (StudentWorldCell) cells[i][j];
                adjacentCells = getAdjacentCells(currentCell);
                for(StudentWorldCell adjacentCell : adjacentCells) {
                    for(Item item : adjacentCell.getItems()) {
                        currentCell.addPerceptForItem(item);
                    }
                }
            }
        }
    }

    private List<StudentWorldCell> getAdjacentCells(StudentWorldCell cell) {
        List<StudentWorldCell> adjacentCells = new ArrayList<StudentWorldCell>();
        int row = cell.getRow();
        int col = cell.getCol();

        if(row>0) {
            adjacentCells.add((StudentWorldCell)cells[row-1][col]);
        }
        if(col<numCols-1) {
            adjacentCells.add((StudentWorldCell)cells[row][col+1]);
        }
        if(row<numRows-1) {
            adjacentCells.add((StudentWorldCell)cells[row+1][col]);
        }
        if(col>0) {
            adjacentCells.add((StudentWorldCell)cells[row][col-1]);
        }
        return adjacentCells;
    }

    private boolean isValidPlacement(int row, int col) {
        //if cell isn't empty
        if(!cells[row][col].getItems().isEmpty()) {
            return false;
        }
        //if cell is adjacent to start cell
        else if(row+col==1) {
            return false;
        }
        return true;
    }

    private boolean isSolveable(StudentWorldCell playerCell) {
        if(playerCell.getItems().contains(StudentWorldItem.DOOR)) {
            return true;
        } else if(playerCell.getItems().contains(StudentWorldItem.STUDENT)) {
            return false;
        } else {
            int row = playerCell.getRow();
            int col = playerCell.getCol();
            return (row < numRows-1 && isSolveable((StudentWorldCell)cells[row+1][col])) || (col < numCols-1 && isSolveable((StudentWorldCell)cells[row][col+1]));
        }
    }

    @Override
    public Object clone() {
        StudentWorldBoard b = new StudentWorldBoard(numRows, numCols, numStudents);
        for(int i=0; i<numRows; i++) {
            for(int j=0; j<numCols; j++) {
                b.cells[i][j].addAll(this.cells[i][j].getItems());
            }
        }
        return b;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<this.numRows; i++) {
            for (int j = 0; j < this.numCols; j++) {
                sb.append(this.cells[i][j].toString());
            }
        }
        return sb.toString();
    }
}