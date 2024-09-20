import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.Stack;

// Notice: To adhere to good design principles, a grid and vehicles are explicitly used. 
// Since the number of cells is always 36, having a grid does not affect the time complexity for this implementation of the game.
// The game can certainly be done without utalizing a Vehicle class and a grid.

/**
 * Represents the game board.
 * This class provides functionalities for reading the game setup from a file,
 * computing a game plan, and getting the number of shortest paths.
 */
public class GameBoard {
    GameState initialGameState;
    GameState finalGameState;
    boolean canEscape = false;
    private HashMap<HashKey, GameState> map = new HashMap<>();

    public static void main(String[] args) throws Exception {
        GameBoard gameBoard = new GameBoard();
        String fileName = "src\\21.txt";
        gameBoard.readInput(fileName);
        ArrayList<Pair> path = gameBoard.getPlan();

        if (path != null) {
            for (Pair pair : path) {
                System.out.println(pair.getId() + " " + pair.getDirection());
            }
            System.out.println(gameBoard.getNumOfPaths());
        } else {
            System.out.println("No valid path found.");
        }
    }

    /**
     * Initializes an empty game board.
     */
    public GameBoard() {
        initialGameState = null;
        finalGameState = null;
    }

    /**
     * Reads the initial game setup from the given text file and initializes the
     * game board.
     *
     * @param FileName Name of the input file containing the game setup.
     * @throws IOException If there's an error reading the file.
     */
    public void readInput(String FileName) throws IOException {
        // Long timeStart; // to test the time
        // Long timeEnd;
        try (BufferedReader reader = new BufferedReader(new FileReader(FileName))) {
            int numVehicles = Integer.parseInt(reader.readLine().trim());
            ArrayList<Vehicle> vehicles = new ArrayList<>(numVehicles);

            String line;
            // Scanner scanner;
            Vehicle vehicle;
            String[] strEl;
            for (int i = 0; i < numVehicles; i++) {
                line = reader.readLine();
                // timeStart = System.nanoTime(); // to test the time
                strEl = line.trim().split("\\s+");
                // if (i == 0) { // // to test the time
                // timeEnd = System.nanoTime();
                // System.out.println(timeEnd - timeStart);
                // }
                vehicle = new Vehicle();
                vehicle.setID(i);
                for (int j = 0; j < strEl.length; j++) {
                    vehicle.addLocation(Integer.parseInt(strEl[j]));
                }
                if ((vehicle.getLocationAt(0) - vehicle.getLocationAt(1)) % 6 == 0) { // vertical:0 horizontal:1
                    vehicle.setDirection(0);
                } else {
                    vehicle.setDirection(1);
                }
                vehicles.add(vehicle);
            }
            initialGameState = new GameState(vehicles, 0, null, 1);
        }
    }

    /**
     * Computes and returns a plan to achieve the game objective. The plan is a
     * sequence
     * of moves for specific vehicles. Each move is represented as a Pair: the first
     * value
     * represents the vehicle number, and the second is a direction ('e' for east,
     * 'w' for west,
     * 'n' for north, 's' for south).
     * 
     * If no plan is possible, it returns null.
     *
     * @return A list of moves to achieve the game objective, or null if not
     *         possible.
     */
    public ArrayList<Pair> getPlan() {
        Queue<GameState> queue = new LinkedList<GameState>();

        // Long timeStart;
        // Long timeEnd;
        queue.add(initialGameState);
        map.put(initialGameState.getHashKey(), initialGameState);
        GameState visitedGameState;
        while (!queue.isEmpty()) {
            visitedGameState = queue.remove();
            if (visitedGameState.escaped() == true) {
                finalGameState = visitedGameState;
                canEscape = true;
                break;
            }
            // timeStart = System.nanoTime();
            ArrayList<GameState> neighbors = visitedGameState.getNeighbors();
            // timeEnd = System.nanoTime();
            // System.out.println(timeEnd - timeStart);
            for (GameState neighbor : neighbors) {
                HashKey neighborsHashKey = neighbor.getHashKey();
                if (map.containsKey(neighborsHashKey)) {
                    GameState sameGameState = map.get(neighborsHashKey);
                    if (sameGameState.getLayer() == neighbor.getLayer()) {
                        sameGameState.setNumShortestPaths(
                                sameGameState.getNumShortestPaths() + neighbor.getNumShortestPaths());
                    }
                } else {
                    map.put(neighborsHashKey, neighbor);
                    queue.add(neighbor);
                }
            }
        }
        Stack<Pair> escapedPaths = new Stack<Pair>();
        ArrayList<Pair> plans = new ArrayList<Pair>(); // the plans
        // backtrack the map
        if (canEscape) {
            GameState current = finalGameState;
            Pair pair;
            Pair reversedPair;
            while (current.getPathPair() != null) {
                pair = current.getPathPair();
                escapedPaths.push(pair); // adding the moves

                // An edge is a pair/move. So Move backwards
                reversedPair = pair.reverse();
                current = new GameState(current, reversedPair);
                current = map.get(current.getHashKey());
            }

            int size = escapedPaths.size();
            for (int i = 0; i < size; i++) {
                plans.add(escapedPaths.pop());
            }
            return plans;
        }
        return plans;

    }

    /**
     * Returns the number of shortest paths to achieve the game objective.
     * 
     * @return Number of shortest paths or 0 if no paths exist.
     */
    public int getNumOfPaths() {
        if (canEscape) {
            return finalGameState.getNumShortestPaths();
        } else {
            return 0;
        }
    }
}

/**
 * Represents the state of the game at a given moment.
 * A GameState captures the layout of vehicles on a game board.
 * A GameState is a vertex in the garph.
 */
class GameState {
    /**
     * List of vehicles currently on the board.
     */
    private ArrayList<Vehicle> vehicles;
    /**
     * Representation of the gameboard as an integer array.
     */
    private int[] grid;
    /**
     * Path taken to reach this state from a previous state.
     */
    private Pair pathPair;
    /**
     * Distance (or layer) from the initial state.
     */
    private int layer;
    /**
     * Count of shortest paths leading up to this state.
     */
    private int numShortestPaths;
    /**
     * The value given to an empty cell in the grid
     */
    private final int EMPTYCELL = -1;

    /**
     * Initializes a new GameState with the provided attributes.
     * Typically used to define the initial state of the game.
     *
     * @param initialVehicles         Initial list of vehicles.
     * @param initialLayer            Initial layer or distance from the root state.
     * @param initialPathPairs        Initial path leading up to this state.
     * @param initialNumShortestPaths Initial count of shortest paths.
     */
    public GameState(ArrayList<Vehicle> initialVehicles, int initialLayer, Pair initialPathPairs,
            int initialNumShortestPaths) {
        vehicles = initialVehicles;
        layer = initialLayer;
        pathPair = initialPathPairs;
        numShortestPaths = initialNumShortestPaths;
        makeGrid();
        setGrid(grid);
        this.addVehiclesToGrid(initialVehicles);
    }

    /**
     * Constructs a new GameState derived from a parent state and a move represented
     * by a Pair.
     *
     * @param parent Parent or preceding GameState.
     * @param pair   Represents a move or transition from the parent state.
     */
    public GameState(GameState parent, Pair pair) {
        ArrayList<Vehicle> parentVehicles = parent.vehicles;
        vehicles = new ArrayList<Vehicle>();
        Vehicle newVehicle;
        for (Vehicle curVehicle : parentVehicles) {
            if (curVehicle.getID() == pair.getId()) {
                newVehicle = curVehicle.changeDirection(pair.getDirection());
            } else {
                newVehicle = new Vehicle(curVehicle);
            }
            this.vehicles.add(newVehicle);
        }
        layer = parent.getLayer() + 1;
        pathPair = pair;
        numShortestPaths = parent.getNumShortestPaths();
        makeGrid();
        setGrid(grid);
        this.addVehiclesToGrid(this.vehicles);
    }

    /**
     * Initializes the grid to a 1D array with 36 cells.
     */
    public void makeGrid() {
        grid = new int[36];
    }

    /**
     * Initializes all cells of the provided grid to be empty.
     *
     * @param grid The game board to be initialized.
     */
    public void setGrid(int[] grid) {
        for (int i = 0; i < 36; i++) {
            grid[i] = EMPTYCELL;
        }
    }

    /**
     * Returns a list of neighboring game states for the current game state.
     * A neighboring game state is derived by making a single move using one of the
     * vehicles.
     * 
     * @return ArrayList<GameState> List of neighboring game states.
     */
    public ArrayList<GameState> getNeighbors() {
        ArrayList<GameState> neighbors = new ArrayList<>();

        for (Vehicle curVehicle : vehicles) {
            switch (curVehicle.getDirection()) {
                case 0: // north or south
                    addNeighborIfPossible(curVehicle, 'n', -7, neighbors); // Check north direction
                    addNeighborIfPossible(curVehicle, 's', 5, neighbors); // Check south direction
                    break;
                case 1: // east or west
                    addNeighborIfPossible(curVehicle, 'w', -2, neighbors); // Check west direction
                    addNeighborIfPossible(curVehicle, 'e', 0, neighbors); // Check east direction
                    break;
            }
        }

        return neighbors;
    }

    /**
     * Checks the feasibility of moving the given vehicle in the specified direction
     * and if feasible, adds a new GameState to the neighbors list.
     * 
     * @param curVehicle The vehicle in consideration.
     * @param direction  The direction of the move ('n' for north, 's' for south,
     *                   'e' for east, 'w' for west).
     * @param offset     The offset to add to the current or last location of the
     *                   vehicle to check for feasibility.
     * @param neighbors  The list where a new neighbor GameState will be added if
     *                   the move is feasible.
     */
    private void addNeighborIfPossible(Vehicle curVehicle, char direction, int offset, ArrayList<GameState> neighbors) {
        int indexToCheck = (direction == 'n' || direction == 'w') ? curVehicle.getLocationAt(0) + offset
                : curVehicle.getLastLocation() + offset;
        boolean canMove = false;

        switch (direction) {
            case 'n':
            case 's':
                canMove = (indexToCheck >= 0 && indexToCheck < 36) && grid[indexToCheck] == EMPTYCELL;
                break;
            case 'w':
                canMove = (indexToCheck >= 0 && curVehicle.getLocationAt(0) % 6 != 1)
                        && grid[indexToCheck] == EMPTYCELL;
                break;
            case 'e':
                canMove = (curVehicle.getLastLocation() % 6 != 0) && grid[indexToCheck] == EMPTYCELL;
                break;
        }

        if (canMove) {
            Pair newPair = new Pair(curVehicle.getID(), direction);
            GameState newNeighbor = new GameState(this, newPair);
            neighbors.add(newNeighbor);
        }
    }

    /**
     * Updates the grid to reflect the positions of the provided list of vehicles.
     *
     * @param vehicles List of vehicles to be positioned on the grid.
     */
    public void addVehiclesToGrid(ArrayList<Vehicle> vehicles) {
        for (Vehicle curVehicle : vehicles) {
            for (int location : curVehicle.getAllLocations()) {
                this.grid[location - 1] = curVehicle.getID();
            }
        }
    }

    /**
     * Checks if the current state represents a winning condition.
     * A win is determined by the presence of the iceCream truck at the exit cell.
     *
     * @return True if the state is a winning state, otherwise false.
     */
    public boolean escaped() {
        if (grid[17] == 0) {
            return true;
        }
        return false;
    }

    /**
     * Produces a hash key based on the current state's grid. Useful for efficient
     * storage or lookup.
     *
     * @return A HashKey derived from the current grid.
     */
    public HashKey getHashKey() {
        return new HashKey(this.grid);
    }

    /**
     * Displays the game board's grid to the console.
     * This method visualizes the current state of the game board, primarily for
     * testing and debugging purposes.
     */
    public void printCellGrid() {
        final int GRID_SIZE = 36;
        final int ROW_LENGTH = 6;

        StringBuilder output = new StringBuilder();

        for (int i = 0; i < GRID_SIZE; i++) {
            output.append(grid[i] == EMPTYCELL ? ". " : grid[i] + " ");

            if ((i + 1) % ROW_LENGTH == 0) {
                output.append('\n');
            }
        }

        System.out.print(output);
    }

    /**
     * Gets the layer
     * 
     * @return The layer or distance
     */
    public int getLayer() {
        return this.layer;
    }

    /**
     * Sets the layer
     * 
     * @param newLevel
     */
    public void setLayer(int newLevel) {
        this.layer = newLevel;
    }

    /*
     * gets the NumShortestPaths
     * 
     * @return The Number of Shortest Paths
     */
    public int getNumShortestPaths() {
        return this.numShortestPaths;
    }

    /**
     * sets the NumShortestPaths
     * 
     * @param newNumShortestPaths
     */
    public void setNumShortestPaths(int newNumShortestPaths) {
        this.numShortestPaths = newNumShortestPaths;
    }

    /*
     * PathPairs Getter
     * 
     * @return The pathPair
     */
    public Pair getPathPair() {
        return this.pathPair;
    }

    /**
     * sets the path pair
     * 
     * @param newPair
     */
    public void setPathPair(Pair newPair) {
        this.pathPair = newPair;
    }

    /*
     * grid getter
     * 
     * @return the grid
     */
    public int[] getGrid() {
        return this.grid;
    }
}

/**
 * The first attribute of the pair is the vehicle number and the second
 * attribute is a character: ’e’ (for east), ’w’ (for west), ’n’ (for north),
 * ’s’ (for south).
 */
class Pair {
    /**
     * the id of the vehicle
     */
    private int id;
    /**
     * the direction of the vehicle
     * {’e’, ’w’, ’n’, ’s’}
     */
    private char direction;

    /**
     * constructs a new pair
     * 
     * @param id
     * @param direction
     */
    public Pair(int id, char direction) {
        this.id = id;
        this.direction = direction;
    }

    /**
     * gets the direction of the pair
     * 
     * @return
     */
    char getDirection() {
        return direction;
    }

    /**
     * gets the id of the pair
     */
    int getId() {
        return id;
    }

    /**
     * sets the direction of the pair
     * 
     * @param direction
     */
    void setDirection(char direction) {
        this.direction = direction;
    }

    /**
     * sets the id of the pair
     * 
     * @param id
     */
    void setId(int id) {
        this.id = id;
    }

    /**
     * reverses the direction of the pair. It is used for backtracking in the BFS to
     * get the actual plan.
     * 
     * @return the reversed pair.
     */
    public Pair reverse() {
        Pair reversePair = new Pair(this.id, this.direction);
        switch (this.getDirection()) {
            case 'n':
                reversePair.setDirection('s');
                break;
            case 's':
                reversePair.setDirection('n');
                break;
            case 'w':
                reversePair.setDirection('e');
                break;
            case 'e':
                reversePair.setDirection('w');
                break;
        }

        return reversePair;
    }
}

/*
 * Vehicle class that stores information about a vehicle.
 * 
 * Vehicles have the following attributes:
 * id: unique identifier for the vehicle. The Ice Cream Truck is always 0.
 * locations: stores the cells on the gamboard the vehicle sits
 * direction: stores which direction the car can move (Vertical[NS] = 0,
 * horizontal[EW] = 1);
 */
class Vehicle {
    private int id;
    private ArrayList<Integer> locations;
    private int direction;

    /**
     * Default constructor that initializes an empty location list for the vehicle.
     */
    public Vehicle() {
        locations = new ArrayList<Integer>();
    }

    /**
     * Constructor to initialize a vehicle with a given ID, locations, and
     * direction.
     * 
     * @param id        The unique ID for the vehicle.
     * @param locations The list of locations occupied by the vehicle.
     * @param direction The direction in which the vehicle can move.
     */
    public Vehicle(int id, ArrayList<Integer> locations, int direction) {
        this.id = id;
        this.locations = locations;
        this.direction = direction;
    }

    /**
     * Copy constructor to create a new vehicle instance with attributes copied from
     * an existing vehicle.
     * 
     * @param lastVehicle The vehicle instance to copy attributes from.
     */
    public Vehicle(Vehicle lastVehicle) {
        this.id = lastVehicle.id;
        this.locations = lastVehicle.locations;
        this.direction = lastVehicle.direction;
    }

    /**
     * Moves the vehicle in the specified direction and returns a new vehicle
     * instance.
     * 
     * @param direction The direction ('n', 's', 'e', 'w') to move the vehicle in.
     * @return A new vehicle instance that represents the moved vehicle.
     */
    public Vehicle changeDirection(char direction) {
        ArrayList<Integer> newLocs = new ArrayList<Integer>();
        switch (direction) {
            case 'n':
                for (int location : this.getAllLocations()) {
                    newLocs.add(location - 6);
                }
                return new Vehicle(this.getID(), newLocs, this.getDirection());
            case 's':
                for (int location : this.getAllLocations()) {
                    newLocs.add(location + 6);
                }
                return new Vehicle(this.getID(), newLocs, this.getDirection());

            case 'e':
                for (int location : this.getAllLocations()) {
                    newLocs.add(location + 1);
                }
                return new Vehicle(this.getID(), newLocs, this.getDirection());

            case 'w':
                for (int location : this.getAllLocations()) {
                    newLocs.add(location - 1);
                }
                return new Vehicle(this.getID(), newLocs, this.getDirection());
        }
        return null;
    }

    /**
     * @return The direction of the vehicle.
     */
    public int getDirection() {
        return this.direction;
    }

    /**
     * Updates the direction of the vehicle.
     * 
     * @param newDir The new direction for the vehicle.
     */
    public void setDirection(int newDir) {
        this.direction = newDir;
    }

    /**
     * @return The list of all locations occupied by the vehicle.
     */
    public ArrayList<Integer> getAllLocations() {
        return locations;
    }

    /**
     * @return The location occupied by the vehicle.
     */
    public int getLocationAt(int index) {
        return locations.get(index);
    }

    /**
     * @return The last location.
     */
    public int getLastLocation() {
        return locations.get(locations.size() - 1);
    }

    /**
     * Sets the new locations.
     * 
     * @param A list of new locations
     */
    public void setAllLocations(ArrayList<Integer> newLocations) {
        this.locations = newLocations;
    }

    /**
     * Adds a location to the end of the vehicles location list
     * 
     * @param the new location
     */
    public void addLocation(int loc) {
        locations.add(loc);
    }

    /*
     * Prints out the list of locations in the vehicle
     * For test purposes only
     */
    public void printLocations() {
        System.out.print("[");
        for (int i = 0; i < locations.size(); i++) {
            System.out.print(locations.get(i));
            if (i != locations.size() - 1) {
                System.out.print(" ");
            }
        }
        System.out.print("]");
    }

    /*
     * Id setter
     * 
     * @param the new vehicle id
     */
    public void setID(int newID) {
        this.id = newID;
    }

    /*
     * Id getter
     * 
     * @return the vehicle id
     */
    public int getID() {
        return this.id;
    }

    /**
     * Determines if two vehicle instances are equal based on their attributes.
     * 
     * @param o The object to compare with the current instance.
     * @return True if the given object represents a vehicle with the same
     *         attributes, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Vehicle)) {
            return false;
        }
        Vehicle v2 = (Vehicle) o;

        for (int i = 0; i < locations.size(); i++) {
            if (locations.get(i) != v2.locations.get(i)) {
                return false;
            }
        }
        return (this.id == v2.id) && (this.direction == v2.direction) && (this.locations.size() == v2.locations.size());
    }
}

/**
 * Represents the hash key for the game board hash map.
 */
class HashKey {
    /**
     * the array of board state
     */
    int[] c;

    /**
     * Constructor to initialize a HashKey instance with the provided array.
     * 
     * @param inputc The array used for the hash key.
     */
    public HashKey(int[] inputc) {
        c = new int[inputc.length];
        c = inputc;
    }

    /**
     * Determines if two HashKey instances are equal based on their internal arrays.
     * 
     * @param o The object to compare with the current instance.
     * @return True if the given object represents a HashKey with the same array,
     *         false otherwise.
     */
    public boolean equals(Object o) {
        boolean flag = true;
        if (this == o)
            return true; // same object
        if ((o instanceof HashKey)) {
            HashKey h = (HashKey) o;
            int[] locs1 = h.c;
            int[] locs = c;
            if (locs1.length == locs.length) {
                for (int i = 0; i < locs1.length; i++) {
                    // mismatch
                    if (locs1[i] != locs[i]) {
                        flag = false;
                        break;
                    }
                }
            } else
                // different size
                flag = false;
        } else // not an instance of HashKey
            flag = false;
        return flag;
    }

    /**
     * Computes the hash code for this HashKey instance based on its internal array.
     * 
     * @return The computed hash code.
     */
    public int hashCode() {
        return Arrays.hashCode(c); // using default hashing of arrays
    }
}