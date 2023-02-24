import java.util.ArrayList;
import java.util.Collections;
import java.io.File;
import java.util.Scanner;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileWriter;

/**
 * Contains the state of a FreeCell game
 *
 * @author Dan DiTursi
 * @version 11 February 2023
 */
public class GameState implements Comparable<GameState>
{
    // instance variables - replace the example below with your own
    private ArrayList<Card> cells; // Numbered from 0-3; empty cells will always be last
    private int numCellsFree;
    private ArrayList<ArrayList<Card>> tableau; // In actions, numbered 1-8; we adjust the -1 manually.
    private int[] foundations = {0,0,0,0,0}; // We'll ignore the first one.
    private int score;
    ArrayList<Action> returnedActions;

    /**
     * Creates a random deal
     */
    public GameState()
    {
        returnedActions = new ArrayList<Action>(1);
        cells = new ArrayList<Card>(4);
        numCellsFree = 4;
        tableau = new ArrayList<ArrayList<Card>>(8);
        
        ArrayList<Card> deck = new ArrayList<Card>(52);
        int i,j,k;
        for (i = 1; i <= 13; i++) {
            for (j = 1; j <= 4; j++) {
                deck.add(new Card(i,j));
            }
        }
        Collections.shuffle(deck);
        
        for (i = 0; i < 8; i++) {
            ArrayList<Card> current = new ArrayList<Card>();
            k = i<4 ? 7 : 6;    // first four piles get 7 cards; last four get 6 cards
            for (j = 0; j < k; j++) {
                current.add(deck.remove(0));
            }
            tableau.add(current);
        }

        this.setScore();
    }
    
    public GameState(GameState gs) {
        cells = new ArrayList<Card>(4);
        for (Card c : gs.cells) { cells.add(c); }
        
        numCellsFree = gs.numCellsFree;
        tableau = new ArrayList<ArrayList<Card>>(8);
        
        for (int i = 0; i < 8; i++) {
            ArrayList<Card> current = new ArrayList<Card>();
            ArrayList<Card> pile = gs.tableau.get(i);
            for (Card c : pile) { current.add(c); }
            tableau.add(current);
        }

        for (int i = 0; i < 5; i++) {
            foundations[i] = gs.foundations[i];
        }
        
        returnedActions = new ArrayList<Action>(gs.returnedActions.size() + 1);
        returnedActions.addAll(gs.returnedActions);

        this.setScore();
    }
    
    // Note: input string must be full file path, unless file is in current working directory
    public GameState(String filename) throws FileNotFoundException {
        File f = new File(filename);
        Scanner sc = new Scanner(f);
        
        String s1 = sc.nextLine();
        String[] S = s1.split(" ");
        for (int i = 1; i <=4; i++) {
            foundations[i] = Integer.parseInt(S[i-1]);
        }
        
        cells = new ArrayList<Card>(4);
        String s2 = sc.nextLine();
        S = s2.split(" ");
        numCellsFree = Integer.parseInt(S[0]);
        for (int i = 0; i < (4 - numCellsFree); i++) {
            String s3 = S[i+1];
            cells.add(new Card(s3.charAt(0),s3.charAt(1)));
        }
        
        tableau = new ArrayList<ArrayList<Card>>(8);
        for (int i = 0; i < 8; i++) {
            ArrayList<Card> pile = new ArrayList<Card>();
            String s4 = sc.nextLine();
            S = s4.split(" ");
            if (!S[0].equals("--")) {
                for (int j = 0; j < S.length; j++) {
                    pile.add(new Card(S[j].charAt(0),S[j].charAt(1)));
                }
            }
            tableau.add(pile);
        }
        returnedActions = new ArrayList<Action>(1);
        this.setScore();
        sc.close();
    }
    
    // Note: Modifies internal state; no "undo" available
    private boolean executeAction(Action a) {
        if (!isLegalAction(a)) { return false; }
        Card c;
        if (a.fromCell()) {
            c = cells.remove(a.get_src_pile());
            numCellsFree++;
        }
        else {
            ArrayList<Card> p1 = tableau.get(a.get_src_pile()-1);
            c = p1.remove(p1.size()-1);
        }
        int d = a.get_dest_pile();
        if (d == 0) {
            cells.add(c);
            numCellsFree--;
        }
        else if (d == 9) {
            foundations[c.getSuit()] = foundations[c.getSuit()] + 1;
        }
        else {
            ArrayList<Card> p2 = tableau.get(d-1);
            p2.add(c);
        }
        this.returnedActions.add(a);
        this.setScore();

        return true;
    }
        
    public boolean isLegalAction(Action a) {
        int s = a.get_src_pile();
        Card c;
        ArrayList<Card> pile;
        if (a.fromCell()) {
            c = cells.get(s);
        }
        else {
            pile = tableau.get(s-1);
            c = pile.get(pile.size()-1);
        }
        if (!c.equals(a.getCard())) { return false; }    
        int d = a.get_dest_pile();
        if (d == 0 && numCellsFree > 0) { return true; }
        if (d == 9) {
            return c.getRank() == foundations[c.getSuit()] + 1;
            // is this card the next one for its suit's foundation pile?
        }
        else {
            pile = tableau.get(d-1);
            if (pile.size() == 0) { return true; }
            Card last = pile.get(pile.size()-1);
            return (last.getRank() == c.getRank() + 1) && (!last.sameColor(c));
        }
        //return false;
    }
    
    public ArrayList<Action> getLegalActions() {
        ArrayList<Action> result = new ArrayList<Action>();
        
        // Moves from tableau to cells
        if (numCellsFree > 0) {
            for (int i = 0; i < 8; i++) {
                ArrayList<Card> pile = tableau.get(i);
                if (pile.size() > 0) {
                    Card c = pile.get(pile.size()-1);
                    result.add(new Action(false,i+1,c,0));
                }
            }
        }
        
        // Moves to tableau
        boolean foundEmpty = false;
        for (int d = 0; d < 8; d++) {
            ArrayList<Card> pile = tableau.get(d);
            // non-empty pile - check all movable cards to see if they can go here.
            if (pile.size() > 0) {
                Card top = pile.get(pile.size() - 1);
                for (int s = 0; s<cells.size(); s++) {
                    Card c2 = cells.get(s);
                    if (!top.sameColor(c2) && (top.getRank() == c2.getRank()+1)) {
                        result.add(new Action(true,s,c2,d+1));
                    }
                }
                for (int s = 0; s < 8; s++) {
                    if (s == d) { continue; }
                    ArrayList<Card> p2 = tableau.get(s);
                    if (p2.size() == 0) { continue; }
                    Card c2 = p2.get(p2.size()-1);
                    if (!top.sameColor(c2) && (top.getRank() == c2.getRank()+1)) {
                        result.add(new Action(false,s+1,c2,d+1));
                    }                    
                }
            }
            else {  // empty pile - any card can go here
                if (!foundEmpty) {
                    foundEmpty = true;
                    for (int s = 0; s<cells.size(); s++) {
                        result.add(new Action(true,s,cells.get(s),d+1));
                    }
                    for (int s = 0; s < 8; s++) {
                        if (s == d) { continue; }
                        ArrayList<Card> p2 = tableau.get(s);
                        // No point in moving a single card from one tableau pile to an empty space
                        if (p2.size() >= 2) {
                            result.add(new Action(false,s+1,p2.get(p2.size()-1),d+1));
                        }
                    }
                }
            }
        }

        
        // Moves to foundation
        for (int s = 0; s<cells.size(); s++) {
            Card c2 = cells.get(s);
            if (c2.getRank() == foundations[c2.getSuit()] + 1) {
                result.add(new Action(true,s,c2,9));
            }
        }
        for (int s = 0; s < 8; s++) {
            ArrayList<Card> p2 = tableau.get(s);
            if (p2.size() > 0) {
                Card c2 = p2.get(p2.size()-1);
                if (c2.getRank() == foundations[c2.getSuit()] + 1) {
                    result.add(new Action(false,s+1,c2,9));
                }
            }
        }
        
        return result;
    }

    public GameState nextState(Action a) {
        GameState result = new GameState(this);
        if (!result.executeAction(a)) { return null; }
        return result;
    }
    
    public GameState resultState(ArrayList<Action> Alist) {
        GameState result = new GameState(this);
        for (Action a : Alist) {
            if (!result.executeAction(a)) { return null; }
        }
        return result;
    }
    
    public String toDisplayString() {
        String s1 = "Foundations:";
        String s2 = "";
        for (int i = 1; i <= 4; i++) {
            s2 = s2 + " " + Card.rankString.charAt(foundations[i]) + Card.suitString.charAt(i);
        }
        String s3 = "Free cells:";
        for (Card c : cells) {
            s3 = s3 + " " + c.toString();
        }
        for (int i = 0; i < numCellsFree; i++) {
            s3 = s3 + " --";
        }
        String s4 = "Tableau (piles go left to right, right is top):";
        for (int j = 0; j < 8; j++) {
            ArrayList<Card> pile = tableau.get(j);
            s4 = s4 + System.lineSeparator() + " " + (j+1) + ":";
            if (pile.size() == 0) {
                s4 = s4 + " --";
            }
            else {
                for (Card c : pile) {
                    s4 = s4 + " " + c.toString();
                }
            }
        }
        return s1 + s2 + System.lineSeparator() + s3 + System.lineSeparator() + s4;
    }
    
    // The string format for a GameState is as follows:
    //   First line: four integers representing the foundations
    //   Second line: one integer for number of free cells, followed by list of cards in cells (if any)
    //   Lines 3-10: List of cards in each tableau pile. Empty piles are represented by "--"
    public String toString() {
        String result = "";
        for (int i = 1; i <= 4; i++) {
            result = result + foundations[i] + " ";
        }
        result = result + "\n";
        
        result = result + numCellsFree + " ";
        for (int i = 0; i < cells.size(); i++) {
            result = result + cells.get(i).toString() + " ";
        }
        result = result + "\n";
                
        for (int i = 0; i < 8; i++) {
            ArrayList<Card> pile = tableau.get(i);
            if (pile.size() == 0) {
                result = result + "--";
            }
            else {
                for (Card c : pile) {
                    result = result + c.toString() + " ";
                }
            }
            result = result + "\n";
        }
        return result;
    }
    
    public void dumpToFile(String filename) {
        try {
            FileWriter fw = new FileWriter(filename);
            fw.write(toString());
            fw.close();
        }
        catch (IOException e) {
            System.out.println("file dump failed, IOException");
        }
    }
    
    public void display() {
        System.out.println(toDisplayString());
    }
    
    public boolean isWin() {
        for (int i = 1; i <=4; i++) {
            if (foundations[i] < 13) { return false; }
        }
        return true;
    }
    
    public boolean gameover() {
        return (getLegalActions().size()) == 0;
    }
    
    public boolean isLoss() {
        return !isWin() && gameover();
    }

    public void setScore() {
        this.score = this.returnedActions.size() + h();
    }
    
    public int getScore() {
        return this.score;
    }

    @Override
    public int compareTo(GameState other) {
        return this.score - other.getScore();
    }

    public int h() {
        
        int heuristicScore = 52;
        for (int i = 1; i < foundations.length; i++) {
            heuristicScore -= foundations[i];
        }

        ArrayList<Card> blockers = new ArrayList<Card>();
        singlePileBlockers(blockers);
        doublePileBlockers(blockers);

        return heuristicScore + blockers.size();
    }

    public void singlePileBlockers(ArrayList<Card> blockers) {
        for (ArrayList<Card> cascade: tableau) {
            for (int topPos = 0; topPos < cascade.size(); topPos++) {
                Card topCard = cascade.get(topPos);

                for (int bottomPos = topPos + 1; bottomPos < cascade.size(); bottomPos++) {
                    Card bottomCard = cascade.get(bottomPos);

                    if (topCard.getSuit() == bottomCard.getSuit() && topCard.getRank() < bottomCard.getRank()) {
                        if (!blockers.contains(bottomCard)) blockers.add(bottomCard);

                    }
                }
            }
        }
    }

    public void doublePileBlockers(ArrayList<Card> blockers) {

        for (int pileOneIndex = 0; pileOneIndex < tableau.size(); pileOneIndex++) {
            ArrayList<Card> pileOne = tableau.get(pileOneIndex);
            if (pileOne.size() < 2) continue;

            for (int pileTwoIndex = 0; pileTwoIndex < tableau.size(); pileTwoIndex++) {
                ArrayList<Card> pileTwo = tableau.get(pileTwoIndex);
                if (pileTwoIndex == pileOneIndex || pileTwo.size() < 2) continue;

                for (int c1Index = 1; c1Index < pileOne.size(); c1Index++) { 
                    Card c1 = pileOne.get(c1Index);

                    for (int c2Index = pileTwo.size() - 2; c2Index >= 0; c2Index--) {
                        Card c2 = pileTwo.get(c2Index);
                        if (c2.getSuit() != c1.getSuit() || c2.getRank() > c1.getRank()) continue;

                        for (int c3Index = c2Index + 1; c3Index < pileTwo.size(); c3Index++) {
                            Card c3 = pileTwo.get(c3Index);

                            for (int c4Index = c1Index - 1; c4Index >= 0; c4Index--) {
                                Card c4 = pileOne.get(c4Index);

                                if (c4.getSuit() != c3.getSuit() || c4.getRank() > c3.getRank() || 
                                    blockers.contains(c1)) continue;
                                blockers.add(c1);

                            }
                        }
                    }
                }
            }
        }
    }
}
