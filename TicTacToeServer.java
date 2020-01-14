import java.io.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Server side class for Tic Tac Toe game.
 * Connects two clients using multithreading and processes their moves.
 * Determines winner by given game rules.
 * 
 * @author Janvi Sharma 
 *
 */
public class TicTacToeServer {
	/**
	 * Main function of TicTacToeServer
	 * Connects 2 clients and assigns them symbols depending on who connected first.
	 * 
	 * @param args
	 * @throws Exception
	 */
    public static void main(String[] args) throws Exception {
    	
        try (var serverSocket = new ServerSocket(55000)) {
            System.out.println("Tic Tac Toe Server is Running...");
            var threadPool = Executors.newFixedThreadPool(200);
            
            while (true) {
                TicTacToeGame newGame = new TicTacToeGame();
                threadPool.execute(newGame.new GamePlayer(serverSocket.accept(), 'X'));
                Thread.sleep(500);
                threadPool.execute(newGame.new GamePlayer(serverSocket.accept(), 'O'));
            }      
        }
    }
    }

/**
 * Class that implements the game using defined rules.
 * 
 * @author Janvi Sharma
 *
 */
class TicTacToeGame {
	public int ctr = 0;
    private GamePlayer[] gameBox = new GamePlayer[9];
    GamePlayer currPlayer;
    
    /**
     * Method to check if the playing grid is full 
     * @return True if the playing grid is full
     */
    public boolean gameBoxFull() {
        return Arrays.stream(gameBox).allMatch(p -> p != null);
    }
    
    /**
     * Synchronized method to ensure that only one client can make a move at any given time
     * 
     * @param loc Location of mouse click
     * @param player Reference to the player who made this move
     */
    public synchronized void move(int loc, GamePlayer player) {
        if (player != currPlayer) {
            throw new IllegalStateException("Wait for your turn.");
        } else if (player.oppPlayer == null) {
            throw new IllegalStateException("You don't have an opponent");
        } else if (gameBox[loc] != null) {
            throw new IllegalStateException("Already occupied");
        }
        gameBox[loc] = currPlayer;
        currPlayer = currPlayer.oppPlayer;
    }
    /**
     * Method that stores winning combinations according to the game rules
     * 
     * @return True if any of the winner combinations is found
     */
    public boolean winnerCombinations() {
        return (gameBox[0] != null && gameBox[0] == gameBox[1] && gameBox[0] == gameBox[2])
            || (gameBox[3] != null && gameBox[3] == gameBox[4] && gameBox[3] == gameBox[5])
            || (gameBox[6] != null && gameBox[6] == gameBox[7] && gameBox[6] == gameBox[8])
            || (gameBox[0] != null && gameBox[0] == gameBox[3] && gameBox[0] == gameBox[6])
            || (gameBox[1] != null && gameBox[1] == gameBox[4] && gameBox[1] == gameBox[7])
            || (gameBox[2] != null && gameBox[2] == gameBox[5] && gameBox[2] == gameBox[8])
            || (gameBox[0] != null && gameBox[0] == gameBox[4] && gameBox[0] == gameBox[8])
            || (gameBox[2] != null && gameBox[2] == gameBox[4] && gameBox[2] == gameBox[6]
        );
    }
    /**
     *A Player is either "X" or "O"
     *The player communicates with the server to keep its board up to date.
     *
     *@author Janvi Sharma
     *
     */
    class GamePlayer implements Runnable {
    	
        char symbol;
        GamePlayer oppPlayer;
        PrintWriter oStream;
        Scanner inStream;
        Socket socket;
        /**
         * Constructor for class assigns socket and symbol(either 'X' or 'O') to the player.
         * 
         * @param socket
         * @param symbol
         */
        public GamePlayer(Socket socket, char symbol) {
            this.socket = socket;
            this.symbol = symbol;
        }
        /**
         * Method that sets up the default game set up and allows back and forth client-server communication
         * 
         */
        public void run() {
            try {
                defaultGameSetUp();
              
                basicMessageProcessing();
            } 
            catch (Exception e) {
                e.printStackTrace();
            } 
            finally {
                if (oppPlayer != null && oppPlayer.oStream != null) {
                    oppPlayer.oStream.println("OppLeft");
                }
                try {socket.close();} 
                catch (IOException e) {}
            }
        }
        /**
         * Method that sends the first message from the server assigning the symbol to a client/player.
         * 
         * @throws IOException
         */
        private void defaultGameSetUp() throws IOException {
            inStream = new Scanner(socket.getInputStream());
            oStream = new PrintWriter(socket.getOutputStream(), true);
            oStream.println("WELCOME " + symbol);
            if (symbol == 'X') {
                currPlayer = this;
               
            } 
            else {
                oppPlayer = currPlayer;
                oppPlayer.oppPlayer = this;   
            }
        }
        /**
         * Method that reads incoming messages from the server
         */
        private void basicMessageProcessing() {
            while (inStream.hasNextLine()) {
                var command = inStream.nextLine();
                if(command.startsWith("ButtonClicked")) {
                	System.out.println("Start button clicked!");
                	ctr++;
                	System.out.println("Times: "+ctr);
                	
                } else if (command.startsWith("QuitGame")) {
                    return;
                } else if(ctr==2){
                	if (command.startsWith("Move")) {
                    moveMessageProcessing(Integer.parseInt(command.substring(5)));
                }
            }
            }
        }
        /**
         * Method to process the message if the message contained a "Move" that the client made.
         * Also checks if a client has won, lost or a game is draw.
         * Sends appropriate messages back to the client.
         * @param location location of the player's move
         */
        private void moveMessageProcessing(int location) {
            try {
                move(location, this);
                oStream.println("ValidMove");
                oppPlayer.oStream.println("OppMoved " + location);
                if (winnerCombinations()) {
                    oStream.println("Winner");
                    oppPlayer.oStream.println("Loss");
                } else if (gameBoxFull()) {
                    oStream.println("Draw");
                    oppPlayer.oStream.println("Draw");
                }
            } catch (IllegalStateException e) {
                oStream.println("Message " + e.getMessage());
            }
        }
    }
}