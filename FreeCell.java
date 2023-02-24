import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.PriorityQueue;

/**
 * Driver class for the Freecell game.
 *
 * @author Dan DiTursi
 * @version 4 February 2023
 */
public class FreeCell
{
    public FreeCell() {}

    public static void main(String[] args) {
        
        GameState game = new GameState();
        try {
            String fileName = "";
            if (args.length > 0) fileName = args[0]; 
            game = new GameState(fileName);

        } catch (FileNotFoundException e) {
            System.err.println("File Not Found, using random gamestate");
        }
        
        game.display();
        System.out.println("\nSolving...");
        Action.dumpToFile(solve(game), "actions.txt");
    }

    public static ArrayList<Action> solve(GameState start) {
        PriorityQueue<GameState> pQueue = new PriorityQueue<GameState>();
        pQueue.offer(start);
        try {
            while (!pQueue.isEmpty()) {
                GameState gs = pQueue.poll();
                if (gs.isWin()) {
                    ArrayList<Action> actions = gs.returnedActions;
                    System.out.printf("You won in %d moves!%n", actions.size());
                    return actions;
                }
                else {
                    ArrayList<Action> actions = gs.getLegalActions();
                    for (Action a: actions) {
                        pQueue.offer(gs.nextState(a));
                    }
                }
            }
            System.out.println("Solution not found :(\nReturning empty ArrayList");
            return new ArrayList<Action>();
        } catch (OutOfMemoryError e) {
            System.err.println("OutOfMemory :(\nReturning empty ArrayList");
            return new ArrayList<Action>();
        }
    }
}
