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
        game.display();
        ArrayList<Action> moves = game.getLegalActions();
        for (Action a : moves) {
            System.out.println(a.toDisplayString());
        }
        Action a1 = moves.get(moves.size()-2);
        GameState g2 = game.nextState(a1);
        System.out.println();
        g2.display();
        moves = g2.getLegalActions();
        for (Action a : moves) {
            System.out.println(a.toDisplayString());
        }
        Action a2 = moves.get(moves.size()-3);
        GameState g3 = g2.nextState(a2);
        System.out.println();
        g3.display();
        moves = g3.getLegalActions();
        for (Action a : moves) {
            System.out.println(a.toDisplayString());
        }
        
    }

    public ArrayList<Action> solve(GameState gs) {
        return null;
    }


    private ArrayList<Action> AStarSolve(GameState start) {
        PriorityQueue<GameState> pQueue = new PriorityQueue<GameState>();
        
        pQueue.offer(start);
        while (!pQueue.isEmpty()) {
            GameState gs = pQueue.poll();
            if (gs.isWin()) {
                System.out.printf("You wont in %d moves! %n", gs.getNumSteps());
            }
            else {
                ArrayList<Action> actions = gs.getLegalActions();
                for (Action a: actions) {
                    pQueue.offer(gs.nextState(a));
                }
            }
        }
        
        
        return null;
    }


}
