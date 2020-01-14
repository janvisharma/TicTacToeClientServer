
import java.net.Socket;
import java.awt.*;
import javax.swing.*;
import java.util.*;
import java.io.*;
import java.awt.event.*;
import java.util.concurrent.ExecutionException; 

/**
 * 
 * Client side class for the Tic Tac Toe game. 
 * Builds the initial set up for the game on the client side.
 * Connects to the Tic Tac Toe server and communicates with it through messages to facilitate
 * game play.
 * 
 * @author Janvi Sharma
 * 
 */
public class TicTacToeClient {

    private JFrame gameFrame = new JFrame("Tic Tac Toe");
    private JLabel messageLabel = new JLabel("Enter your player name...");
    private JPanel bottomPanel = new JPanel();
    private JButton startButton = new JButton("Submit");
    private JTextField nameOfPlayer = new JTextField(20);
    private int startGame = 0, mouseCtr=0;

    private Socket socket;
    private Scanner scannerInstream;
    private PrintWriter writerOutstream;
    
    private GameBoxPanel[] gameBox = new GameBoxPanel[9];
    private GameBoxPanel currPlayingBox;
    
    /**
     * Constructor for class TicTacToeClient
     * Sets up socket connection to the server and loads
     * GUI components on the client side
     * Also sets mouse listener to the playing grid to send messages to the server.
     */
  
    public TicTacToeClient() throws Exception {
    	socket = new Socket("127.0.0.1", 55000);
        scannerInstream = new Scanner(socket.getInputStream());
        writerOutstream = new PrintWriter(socket.getOutputStream(), true);
        
        messageLabel.setBackground(Color.lightGray);
        gameFrame.getContentPane().add(messageLabel, BorderLayout.NORTH);
        
        bottomPanel.add(nameOfPlayer);
        bottomPanel.add(startButton);
        startButton.addActionListener(new startButtonListener());
       
        bottomPanel.setLayout(new FlowLayout(FlowLayout.LEFT,10,10));
        
        gameFrame.add(bottomPanel, BorderLayout.SOUTH);

        JPanel gameSetup = new JPanel();
        gameSetup.setBackground(Color.black);
        gameSetup.setLayout(new GridLayout(3, 3, 2, 2));
        JMenuBar menuBar = new JMenuBar();
    	JMenu controlMenu = new JMenu("Control");
    	JMenu helpMenu = new JMenu("Help");
    	
    	JMenuItem exitControl = new JMenuItem("Exit");
    	exitControl.addActionListener(new ControlActionListener());
    	
    	JMenuItem instructionsHelp = new JMenuItem("Instructions");
    	instructionsHelp.addActionListener(new HelpActionListener());
    	
    	controlMenu.add(exitControl);
    	helpMenu.add(instructionsHelp);
    	
    	menuBar.add(controlMenu);
    	menuBar.add(helpMenu);
    	gameFrame.setJMenuBar(menuBar);
    	
        for (int i = 0; i < gameBox.length; i++) {
            final int j = i;
            gameBox[i] = new GameBoxPanel();
            
            gameBox[i].addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    currPlayingBox = gameBox[j];
                    if(startGame==1) {
                    mouseCtr++;
                    System.out.println("Mouse count: "+mouseCtr);
                    writerOutstream.println("Move " + j); }
                } 
            }); 
            gameSetup.add(gameBox[i]);
        }
        gameFrame.getContentPane().add(gameSetup, BorderLayout.CENTER);
        gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            }

    /**
     * Method playGame() facilitates communication with the server through messages.
     * The server responds with messages telling if the move is valid, if the opponent moved,
     * if the client won or lost or if the other player leaves the game.
     * There are appropriate ways to handle each message according to the game rules.
     * 
     */
    public void playGame() throws Exception {
    	try {
    	
            var serverMessage = scannerInstream.nextLine();
            var clientSymbol = serverMessage.charAt(8);
            System.out.println(serverMessage);
            var opponentSymbol = clientSymbol == 'X' ? 'O' : 'X';
            while (scannerInstream.hasNextLine()) {
            	
                serverMessage = scannerInstream.nextLine();
                if (serverMessage.startsWith("ValidMove")) {
                    messageLabel.setText("Valid move, wait for your opponent.");
                    currPlayingBox.setText(clientSymbol);
                    currPlayingBox.repaint();
                } else 
	                	if (serverMessage.startsWith("OppMoved")) {
	                    var loc = Integer.parseInt(serverMessage.substring(9));
	                    gameBox[loc].setText(opponentSymbol);
	                    gameBox[loc].repaint();
	                    messageLabel.setText("Your opponent has moved, now is your turn.");
	                } else 
		                	if (serverMessage.startsWith("Message")) {
		                    messageLabel.setText(serverMessage.substring(8));
		                } else 
			                	if (serverMessage.startsWith("Winner")) {
			                    JOptionPane.showMessageDialog(gameFrame, "Congratulations. You Win.");
			                    break;
			                } else 
				                	if (serverMessage.startsWith("Loss")) {
				                    JOptionPane.showMessageDialog(gameFrame, "You lose.");
				                    break;
				                } else 
					                	if (serverMessage.startsWith("Draw")) {
					                    JOptionPane.showMessageDialog(gameFrame, "Draw.");
					                    break;
					                } else 
						                	if (serverMessage.startsWith("OppLeft")) {
						                    JOptionPane.showMessageDialog(gameFrame, "Game Ends. One of the players left.");
						                    break;
						                }
            }
            writerOutstream.println("QuitGame");
        } catch (Exception e) {
            e.printStackTrace();
        }

        finally {
            socket.close();
            gameFrame.dispose();
        }

    }
    /**
     * A game box panel for displaying the playing grid for the client.
     * Overrides setText() method so that the image can change once
     * user clicks on it given that the move is "allowed" by the game rules.
     * 
     * @author Janvi Sharma
     * 
     */
    static class GameBoxPanel extends JPanel {
        JLabel newGameLabel = new JLabel();
        public GameBoxPanel() {
            setBackground(Color.white);
            setLayout(new GridBagLayout());
            newGameLabel.setFont(new Font("Courier", Font.BOLD, 40));
            add(newGameLabel);
        }
        public void setText(char text) {
        	if(text=='X') {
        		newGameLabel.setForeground(Color.green);
        		newGameLabel.setText(text+"");
        	}
        	else
        		newGameLabel.setForeground(Color.red);
        		newGameLabel.setText(text+"");
        }
    }
    /**
     * Main function of client
     * Creates an instance of TicTacToeClient class 
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        TicTacToeClient client = new TicTacToeClient();
        client.gameFrame.setSize(400, 400);
        client.gameFrame.setVisible(true);
      
    }
    
    /**
     * startButtonListener to see if "submit" button is clicked
     * by user and name is input.
     * Changes the GUI according to the name of user.
     * Enables method to communicate with the server to faciliate the game process.
     * 
     * @author Janvi Sharma
     *
     */
    public class startButtonListener implements ActionListener {
    /**
	* Function that changes the GUI according to the name	
	* entered and starts a new thread for the method playGame()	
	*/
    public void actionPerformed(ActionEvent event){
		String name = nameOfPlayer.getText();
		nameOfPlayer.setText("");
		messageLabel.setText("WELCOME "+name);
		startButton.setEnabled(false);
		startGame = 1;
		gameFrame.setTitle("Tic Tac Toe- Player " + name);	
		writerOutstream.println("ButtonClicked");
		new Thread() {
			public void run() {
		try {
			playGame();
		} catch (Exception ex) {ex.printStackTrace();}
			}
		}.start();
		
    }
    }
    /**
     * ControlActionListener is the Listener for JMenuItem "Exit"
     * Terminates program at client side if "exit" is clicked.
     * 
     * @author Janvi Sharma
     *
     */
    public class ControlActionListener implements ActionListener {
    	/**
    	 * Function that enables the user to exit the game
    	 */
    	public void actionPerformed(ActionEvent event) {
    		System.exit(0);
    	}
    }
    /**
     * HelpActionListener is the Listener for JMenuItem "Instructions"
     * Displays the game rules and instructions for playing the game when clicked.
     * 
     * @author Janvi Sharma
     *
     */
    public class HelpActionListener implements ActionListener {
    	/**
    	 * Function that displays the rules of the game
    	 */
    	public void actionPerformed(ActionEvent event) {
    		String message = "Some information about the game.\nCriteria for a valid move:\n- The move is not occupied by any mark.\n- The move is made in the player's turn.\n- The move is made within the 3 x 3 board.\nThe game would continue and switch among the opposite player until it reaches either one of the following conditions:\n - Player 1 wins.\n - Player 2 wins.\n - Draw.";
    		JOptionPane.showMessageDialog(null, message);
    	}
    	
    }
    
    
    
    
}
    
    

