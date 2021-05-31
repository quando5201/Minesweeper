

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import java.sql.Date;

import javafx.util.Pair;
import javax.swing.border.TitledBorder;
import Project.Score.Time;



// This is the main controller class
public class Game implements MouseListener, ActionListener, WindowListener
{
    public static String dbPath;
    // "playing" indicates whether a game is running (true) or not (false).
    private boolean playing;

    private Board board;

    private UI gui;

    private Score score;

    //------------------------------------------------------------------//

    public Game() throws IOException, ClassNotFoundException {
        // set db path
        String p = "";

        try
        {
            p = new File(Game.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath() + "\\db.accdb";
        }
        catch (URISyntaxException ex)
        {
            System.out.println("Error loading database file.");
        }

        Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");

        try {
            dbPath = "jdbc:ucanaccess://data/db.accdb" + p;
        } catch (Exception e){
            System.out.println(e.toString());
        }

        score = new Score();
        score.populate();

        UI.setLook("Nimbus");

        createBoard();

        this.gui = new UI(board.getRows(), board.getCols(), board.getNumberOfMines());
        this.gui.setButtonListeners(this);

        this.playing = false;

        gui.setVisible(true);

        gui.setIcons();
        gui.hideAll();

        resumeGame();
    }

    //-----------------Load Save Game (if any)--------------------------//

    public void resumeGame()
    {
        if(board.checkSave())
        {
            ImageIcon question = new ImageIcon(getClass().getResource("/resources/question.png"));

            int option = JOptionPane.showOptionDialog(null, "Continue your saved game?",
                    "Saved Game Found", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, question,null,null);

            switch(option)
            {
                case JOptionPane.YES_OPTION:

                    //load board's state
                    Pair p = board.loadSaveGame();

                    //set button's images
                    setButtonImages();

                    //load timer's value
                    gui.setTimePassed((int)p.getKey());

                    //load mines value
                    gui.setMines((int)p.getValue());

                    gui.startTimer();

                    playing = true;
                    break;

                case JOptionPane.NO_OPTION:
                    board.deleteSavedGame();
                    break;

                case JOptionPane.CLOSED_OPTION:
                    board.deleteSavedGame();
                    break;
            }
        }
    }


    //-------------------------------------------------//
    public void setButtonImages()
    {
        Cell cells[][] = board.getCells();
        JButton buttons[][] = gui.getButtons();

        for( int y=0 ; y<board.getRows() ; y++ )
        {
            for( int x=0 ; x<board.getCols() ; x++ )
            {
                buttons[x][y].setIcon(null);

                if (cells[x][y].getContent().equals(""))
                {
                    buttons[x][y].setIcon(gui.getIconTile());
                }
                else if (cells[x][y].getContent().equals("F"))
                {
                    buttons[x][y].setIcon(gui.getIconFlag());
                    buttons[x][y].setBackground(Color.blue);
                }
                else if (cells[x][y].getContent().equals("0"))
                {
                    buttons[x][y].setBackground(Color.lightGray);
                }
                else
                {
                    buttons[x][y].setBackground(Color.lightGray);
                    buttons[x][y].setText(cells[x][y].getContent());
                    gui.setTextColor(buttons[x][y]);
                }
            }
        }
    }


    //------------------------------------------------------------//

    public void createBoard()
    {
        // Create a new board
        int mines = 10;

        int r = 9;
        int c = 9;

        this.board = new Board(mines, r, c);
    }


    //---------------------------------------------------------------//
    public void newGame()
    {
        this.playing = false;

        createBoard();

        gui.interruptTimer();
        gui.resetTimer();
        gui.initGame();
        gui.setMines(board.getNumberOfMines());
    }
    //------------------------------------------------------------------------------//

    public void restartGame()
    {
        this.playing = false;

        board.resetBoard();

        gui.interruptTimer();
        gui.resetTimer();
        gui.initGame();
        gui.setMines(board.getNumberOfMines());
    }

    //------------------------------------------------------------------------------//
    private void endGame()
    {
        playing = false;
        showAll();

        score.save();
    }


    //-------------------------GAME WON AND GAME LOST ---------------------------------//

    public void gameWon()
    {
        score.incCurrentStreak();
        score.incCurrentWinningStreak();
        score.incGamesWon();
        score.incGamesPlayed();

        gui.interruptTimer();
        endGame();
        //----------------------------------------------------------------//


        JDialog dialog = new JDialog(gui, Dialog.ModalityType.DOCUMENT_MODAL);

        //------MESSAGE-----------//
        JLabel message = new JLabel("CONGRATULATIONS, YOU WON!", SwingConstants.CENTER);

        //-----STATISTICS-----------//
        JPanel statistics = new JPanel();
        statistics.setLayout(new GridLayout(6,1,0,10));

        ArrayList<Score.Time> bTimes = score.getBestTimes();

        if (bTimes.isEmpty() || (bTimes.get(0).getTimeValue() > gui.getTimePassed()))
        {
            statistics.add(new JLabel("    CONGRATULATIONS! THIS IS THE FASTEST TIME YOU GOT!    "));
        }

        score.addTime(gui.getTimePassed(), new Date(System.currentTimeMillis()));

        JLabel time = new JLabel("  TIME:  " + Integer.toString(gui.getTimePassed()) + " seconds            DATE:  " + new Date(System.currentTimeMillis()));

        JLabel bestTime = new JLabel();


        if (bTimes.isEmpty())
        {
            bestTime.setText("  BEST TIME:  ---                  DATE:  ---");
        }
        else
        {
            bestTime.setText("  BEST TIME:  " + bTimes.get(0).getTimeValue() + " second(s)            DATE:  " + bTimes.get(0).getDateValue());
        }

        JLabel gPlayed = new JLabel("  PLAYED:  " + score.getGamesPlayed() + " time(s)");
        JLabel gWon = new JLabel("  WON:  " + score.getGamesWon() + " time(s)");
        JLabel gPercentage = new JLabel("  WIN PERCENTAGE:  " + score.getWinPercentage() + "%");

        statistics.add(time);
        statistics.add(bestTime);
        statistics.add(gPlayed);
        statistics.add(gWon);
        statistics.add(gPercentage);

        Border loweredetched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        statistics.setBorder(loweredetched);


        //--------BUTTONS----------//
        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(1,2,10,0));

        JButton exit = new JButton("QUIT");
        JButton playAgain = new JButton("TRY AGAIN");


        exit.addActionListener((ActionEvent e) -> {
            dialog.dispose();
            windowClosing(null);
        });
        playAgain.addActionListener((ActionEvent e) -> {
            dialog.dispose();
            newGame();
        });


        buttons.add(exit);
        buttons.add(playAgain);

        //--------DIALOG-------------//

        JPanel c = new JPanel();
        c.setLayout(new BorderLayout(20,20));
        c.add(message, BorderLayout.NORTH);
        c.add(statistics, BorderLayout.CENTER);
        c.add(buttons, BorderLayout.SOUTH);

        c.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        dialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                                     @Override
                                     public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                                         dialog.dispose();
                                         newGame();
                                     }
                                 }
        );

        dialog.setTitle("WON");
        dialog.add(c);
        dialog.pack();
        dialog.setLocationRelativeTo(gui);
        dialog.setVisible(true);
    }

    public void gameLost()
    {
        score.decCurrentStreak();
        score.incCurrentLosingStreak();
        score.incGamesPlayed();

        gui.interruptTimer();

        endGame();

        //----------------------------------------------------------------//

        JDialog dialog = new JDialog(gui, Dialog.ModalityType.DOCUMENT_MODAL);

        //------MESSAGE-----------//
        JLabel message = new JLabel("BOOM! YOU LOST!", SwingConstants.CENTER);

        //-----STATISTICS-----------//
        JPanel statistics = new JPanel();
        statistics.setLayout(new GridLayout(5,1,0,10));

        JLabel time = new JLabel("  TIME:  " + Integer.toString(gui.getTimePassed()) + " second(s)");

        JLabel bestTime = new JLabel();

        ArrayList<Score.Time> bTimes = score.getBestTimes();

        if (bTimes.isEmpty())
        {
            bestTime.setText("                        ");
        }
        else
        {
            bestTime.setText("  BEST TIME:  " + bTimes.get(0).getTimeValue() + " second(s)            DATE:  " + bTimes.get(0).getDateValue());
        }

        JLabel gPlayed = new JLabel("  PLAYED:  " + score.getGamesPlayed());
        JLabel gWon = new JLabel("  WON:  " + score.getGamesWon());
        JLabel gPercentage = new JLabel("  WIN PERCENTAGE:  " + score.getWinPercentage() + "%");

        statistics.add(time);
        statistics.add(bestTime);
        statistics.add(gPlayed);
        statistics.add(gWon);
        statistics.add(gPercentage);

        Border loweredetched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        statistics.setBorder(loweredetched);


        //--------BUTTONS----------//
        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(1,3,2,0));

        JButton exit = new JButton("QUIT");
        JButton restart = new JButton("RESUME");
        JButton playAgain = new JButton("TRY AGAIN");


        exit.addActionListener((ActionEvent e) -> {
            dialog.dispose();
            windowClosing(null);
        });
        restart.addActionListener((ActionEvent e) -> {
            dialog.dispose();
            restartGame();
        });
        playAgain.addActionListener((ActionEvent e) -> {
            dialog.dispose();
            newGame();
        });


        buttons.add(exit);
        buttons.add(restart);
        buttons.add(playAgain);

        //--------DIALOG-------------//

        JPanel c = new JPanel();
        c.setLayout(new BorderLayout(20,20));
        c.add(message, BorderLayout.NORTH);
        c.add(statistics, BorderLayout.CENTER);
        c.add(buttons, BorderLayout.SOUTH);

        c.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        dialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                                     @Override
                                     public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                                         dialog.dispose();
                                         newGame();
                                     }
                                 }
        );

        dialog.setTitle("LOST");
        dialog.add(c);
        dialog.pack();
        dialog.setLocationRelativeTo(gui);
        dialog.setVisible(true);
    }


    //--------------------------------SCORE BOARD--------------------------------------//
    public void showScore()
    {
        //----------------------------------------------------------------//

        JDialog dialog = new JDialog(gui, Dialog.ModalityType.DOCUMENT_MODAL);

        //-----BEST TIMES--------//

        JPanel bestTimes = new JPanel();
        bestTimes.setLayout(new GridLayout(5,1));

        ArrayList<Score.Time> bTimes = score.getBestTimes();

        for (int i = 0; i < bTimes.size(); i++)
        {
            JLabel t = new JLabel("  " + bTimes.get(i).getTimeValue() + "           " + bTimes.get(i).getDateValue());
            bestTimes.add(t);
        }

        if (bTimes.isEmpty())
        {
            JLabel t = new JLabel("                               ");
            bestTimes.add(t);
        }

        TitledBorder b = BorderFactory.createTitledBorder("Best Times");
        b.setTitleJustification(TitledBorder.LEFT);

        bestTimes.setBorder(b);

        //-----STATISTICS-----------//
        JPanel statistics = new JPanel();

        statistics.setLayout(new GridLayout(6,1,0,10));

        JLabel gPlayed = new JLabel("  Played:  " + score.getGamesPlayed());
        JLabel gWon = new JLabel("  Won:  " + score.getGamesWon());
        JLabel gPercentage = new JLabel("  Win Percentage:  " + score.getWinPercentage() + "%");
        JLabel lWin = new JLabel("  Winning Streak:  " + score.getLongestWinningStreak());
        JLabel lLose = new JLabel("  Losing Streak:  " + score.getLongestLosingStreak());
        JLabel currentStreak = new JLabel("  Current Streak:  " + score.getCurrentStreak());


        statistics.add(gPlayed);
        statistics.add(gWon);
        statistics.add(gPercentage);
        statistics.add(lWin);
        statistics.add(lLose);
        statistics.add(currentStreak);

        Border loweredetched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        statistics.setBorder(loweredetched);


        //--------BUTTONS----------//
        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(1,2,10,0));

        JButton close = new JButton("EXIT");
        JButton reset = new JButton("RESET");


        close.addActionListener((ActionEvent e) -> {
            dialog.dispose();
        });
        reset.addActionListener((ActionEvent e) -> {
            ImageIcon question = new ImageIcon(getClass().getResource("/resources/question1.png"));

            int option = JOptionPane.showOptionDialog(null, "If you want to clear all of your profile?",
                    "Reset Profile", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, question,null,null);

            switch(option)
            {
                case JOptionPane.YES_OPTION:

                    score.resetScore();
                    score.save();
                    dialog.dispose();
                    showScore();
                    break;

                case JOptionPane.NO_OPTION:
                    break;
            }
        });

        buttons.add(close);
        buttons.add(reset);

        if (score.getGamesPlayed() == 0)
            reset.setEnabled(false);

        //--------DIALOG-------------//

        JPanel c = new JPanel();
        c.setLayout(new BorderLayout(20,20));
        c.add(bestTimes, BorderLayout.WEST);
        c.add(statistics, BorderLayout.CENTER);
        c.add(buttons, BorderLayout.SOUTH);

        c.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        dialog.setTitle("Player Profile");
        dialog.add(c);
        dialog.pack();
        dialog.setLocationRelativeTo(gui);
        dialog.setVisible(true);
    }

    //------------------------------------------------------------------------------//


    // Shows the "solution" of the game.
    private void showAll()
    {
        String cellSolution;

        Cell cells[][] = board.getCells();
        JButton buttons[][] = gui.getButtons();

        for (int x=0; x<board.getCols(); x++ )
        {
            for (int y=0; y<board.getRows(); y++ )
            {
                cellSolution = cells[x][y].getContent();

                // Is the cell still unrevealed
                if( cellSolution.equals("") )
                {
                    buttons[x][y].setIcon(null);

                    // Get Neighbours
                    cellSolution = Integer.toString(cells[x][y].getSurroundingMines());

                    // Is it a mine?
                    if(cells[x][y].getMine())
                    {
                        cellSolution = "M";

                        //mine
                        buttons[x][y].setIcon(gui.getIconMine());
                        buttons[x][y].setBackground(Color.lightGray);
                    }
                    else
                    {
                        if(cellSolution.equals("0"))
                        {
                            buttons[x][y].setText("");
                            buttons[x][y].setBackground(Color.lightGray);
                        }
                        else
                        {
                            buttons[x][y].setBackground(Color.lightGray);
                            buttons[x][y].setText(cellSolution);
                            gui.setTextColor(buttons[x][y]);
                        }
                    }
                }

                // This cell is already flagged!
                else if( cellSolution.equals("F") )
                {
                    // Is it correctly flagged?
                    if(!cells[x][y].getMine())
                    {
                        buttons[x][y].setBackground(Color.orange);
                    }
                    else
                        buttons[x][y].setBackground(Color.green);
                }

            }
        }
    }


    //-------------------------------------------------------------------------//

    //-------------------------------------------------------------------------//


    //-------------------------------------------------------------------------//


    //--------------------------------------------------------------------------//

    public boolean isFinished()
    {
        boolean isFinished = true;
        String cellSolution;

        Cell cells[][] = board.getCells();

        for( int x = 0 ; x < board.getCols() ; x++ )
        {
            for( int y = 0 ; y < board.getRows() ; y++ )
            {
                // If a game is solved, the content of each Cell should match the value of its surrounding mines
                cellSolution = Integer.toString(cells[x][y].getSurroundingMines());

                if(cells[x][y].getMine())
                    cellSolution = "F";

                // Compare the player's "answer" to the solution.
                if(!cells[x][y].getContent().equals(cellSolution))
                {
                    //This cell is not solved yet
                    isFinished = false;
                    break;
                }
            }
        }

        return isFinished;
    }


    //Check the game to see if its finished or not
    private void checkGame()
    {
        if(isFinished())
        {
            gameWon();
        }
    }

    //----------------------------------------------------------------------/


    /*
     * If a player clicks on a zero, all surrounding cells ("neighbours") must revealed.
     * This method is recursive: if a neighbour is also a zero, his neighbours must also be revealed.
     */
    public void findZeroes(int xCo, int yCo)
    {
        int neighbours;

        Cell cells[][] = board.getCells();
        JButton buttons[][] = gui.getButtons();

        // Columns
        for(int x = board.makeValidCoordinateX(xCo - 1) ; x <= board.makeValidCoordinateX(xCo + 1) ; x++)
        {
            // Rows
            for(int y = board.makeValidCoordinateY(yCo - 1) ; y <= board.makeValidCoordinateY(yCo + 1) ; y++)
            {
                // Only unrevealed cells need to be revealed.
                if(cells[x][y].getContent().equals(""))
                {
                    // Get the neighbours of the current (neighbouring) cell.
                    neighbours = cells[x][y].getSurroundingMines();

                    // Reveal the neighbours of the current (neighbouring) cell
                    cells[x][y].setContent(Integer.toString(neighbours));

                    if (!cells[x][y].getMine())
                        buttons[x][y].setIcon(null);

                    // Is this (neighbouring) cell a "zero" cell itself?
                    if(neighbours == 0)
                    {
                        // Yes, give it a special color and recurse!
                        buttons[x][y].setBackground(Color.lightGray);
                        buttons[x][y].setText("");
                        findZeroes(x, y);
                    }
                    else
                    {
                        // No, give it a boring gray color.
                        buttons[x][y].setBackground(Color.lightGray);
                        buttons[x][y].setText(Integer.toString(neighbours));
                        gui.setTextColor(buttons[x][y]);
                    }
                }
            }
        }
    }
    //-----------------------------------------------------------------------------//
    //This function is called when clicked on closed button or exit
    @Override
    public void windowClosing(WindowEvent e)
    {
        if (playing)
        {
            ImageIcon question = new ImageIcon(getClass().getResource("/resources/question1.png"));

            Object[] options = {"Save","Don't Save","Cancel"};

            int quit = JOptionPane.showOptionDialog(null, "What do you want to do with the game in progress?",
                    "New Game", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, question, options, options[2]);

            switch(quit)
            {
                //save
                case JOptionPane.YES_OPTION:

                    gui.interruptTimer();
                    score.save();

                    JDialog dialog = new JDialog(gui, Dialog.ModalityType.DOCUMENT_MODAL);
                    JPanel panel = new JPanel();
                    panel.setLayout(new BorderLayout());
                    panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
                    panel.add(new JLabel("Saving.... Please Wait", SwingConstants.CENTER));
                    dialog.add(panel);
                    dialog.setTitle("Saving Game...");
                    dialog.pack();
                    dialog.setLocationRelativeTo(gui);
                    dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

                    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>(){
                        @Override
                        protected Void doInBackground() throws Exception
                        {
                            board.saveGame(gui.getTimePassed(), gui.getMines());
                            return null;
                        }

                        @Override
                        protected void done(){
                            dialog.dispose();
                        }
                    };

                    worker.execute();
                    dialog.setVisible(true);

                    System.exit(0);
                    break;

                //dont save
                case JOptionPane.NO_OPTION:
                    score.incGamesPlayed();
                    score.save();
                    System.exit(0);
                    break;

                case JOptionPane.CANCEL_OPTION: break;
            }
        }
        else
            System.exit(0);
    }

    //-----------------------------------------------------------------------//

    @Override
    public void actionPerformed(ActionEvent e) {
        JMenuItem menuItem = (JMenuItem) e.getSource();

        if (menuItem.getName().equals("New Game"))
        {
            if (playing)
            {
                ImageIcon question = new ImageIcon(getClass().getResource("/resources/question1.png"));

                Object[] options = {"Quit and Start a New Game","Restart","Keep Playing"};

                int startNew = JOptionPane.showOptionDialog(null, "What do you want to do with the game in progress?",
                        "New Game", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, question, options, options[2]);

                switch(startNew)
                {
                    case JOptionPane.YES_OPTION:

                        // Initialize the new game.
                        newGame();
                        score.incGamesPlayed();
                        score.save();
                        break;

                    case JOptionPane.NO_OPTION:
                        score.incGamesPlayed();
                        score.save();
                        restartGame();
                        break;

                    case JOptionPane.CANCEL_OPTION: break;
                }
            }
        }

        else if (menuItem.getName().equals("Exit"))
        {
            windowClosing(null);
        }

        //Statistics
        else
        {
            showScore();
            score.resetScore();
            score.save();
            showScore();
        }
    }


    //--------------------------------------------------------------------------//

    //Mouse Click Listener
    @Override
    public void mouseClicked(MouseEvent e)
    {
        // start timer on first click
        if(!playing)
        {
            gui.startTimer();
            playing = true;
        }

        if (playing)
        {
            //Get the button's name
            JButton button = (JButton)e.getSource();

            // Get coordinates (button.getName().equals("x,y")).
            String[] co = button.getName().split(",");

            int x = Integer.parseInt(co[0]);
            int y = Integer.parseInt(co[1]);

            // Get cell information.
            boolean isMine = board.getCells()[x][y].getMine();
            int neighbours = board.getCells()[x][y].getSurroundingMines();

            // Left Click
            if (SwingUtilities.isLeftMouseButton(e))
            {
                if (!board.getCells()[x][y].getContent().equals("F"))
                {
                    button.setIcon(null);

                    //Mine is clicked.
                    if(isMine)
                    {
                        //red mine
                        button.setIcon(gui.getIconRedMine());
                        button.setBackground(Color.red);
                        board.getCells()[x][y].setContent("M");

                        gameLost();
                    }
                    else
                    {
                        // The player has clicked on a number.
                        board.getCells()[x][y].setContent(Integer.toString(neighbours));
                        button.setText(Integer.toString(neighbours));
                        gui.setTextColor(button);

                        if( neighbours == 0 )
                        {
                            // Show all surrounding cells.
                            button.setBackground(Color.lightGray);
                            button.setText("");
                            findZeroes(x, y);
                        }
                        else
                        {
                            button.setBackground(Color.lightGray);
                        }
                    }
                }
            }
            // Right Click
            else if (SwingUtilities.isRightMouseButton(e))
            {
                if(board.getCells()[x][y].getContent().equals("F"))
                {
                    board.getCells()[x][y].setContent("");
                    button.setText("");
                    button.setBackground(new Color(135, 0, 140, 255));

                    //simple blue

                    button.setIcon(gui.getIconTile());
                    gui.incMines();
                }
                else if (board.getCells()[x][y].getContent().equals(""))
                {
                    board.getCells()[x][y].setContent("F");
                    button.setBackground(Color.blue);

                    button.setIcon(gui.getIconFlag());
                    gui.decMines();
                }
            }

            checkGame();
        }
    }

    //-------------------------RELATED TO SCORES----------------------//



    //---------------------EMPTY FUNCTIONS-------------------------------//
    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }
}


