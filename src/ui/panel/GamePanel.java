package ui.panel;

import DAO.TubeDAO;
import model.Block;
import model.Data;
import model.Step;
import model.Tube;
import utils.Constant;
import utils.Sounds;
import utils.Utils;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;

import static utils.Constant.FONT_PATH;

public class GamePanel extends JPanel implements MouseListener, MouseMotionListener {

    private List<Tube> tubeList;
    private int size;
    private boolean isDrag = false;
    private Block top;
    private double ax, ay, width, height;
    private int moveCount;
    public int levelQuantity;
    private int tubePop;
    private LinkedList<Step> undo;
    private static final int undoSize = 5;
    private Data data;
    private Boolean sound;
    private int level = 1; // Starting at level 1
    private static final int INITIAL_TIME = 10; // Starting time for level 1 in seconds
    private static final int TIME_INCREMENT = 5; // Time increment for each level

    // Timer and display fields
    private int timeLeft = INITIAL_TIME;
    private Timer timer;
    private JLabel timerLabel; // To display the time remaining
    private JLabel levelLabel; // To display the current level

    public GamePanel(Data data, Boolean sound){
        this.sound = sound;
        this.data = data;
        this.levelQuantity = 5;
        this.level = this.data.getLevel() + 1;
        timeLeft = INITIAL_TIME + (this.level - 1) * TIME_INCREMENT; // Set time based on level
        init(this.level);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Timer label setup
        timerLabel = new JLabel("Time: " + timeLeft + "s");
        timerLabel.setFont(new Font("Arial", Font.BOLD, 40));
        timerLabel.setForeground(Color.YELLOW); // Customize to match your UI
        timerLabel.setAlignmentX(CENTER_ALIGNMENT); // Center align timer label

        this.add(Box.createVerticalStrut(10)); // Adds fixed vertical space between labels
        this.add(timerLabel);
        this.add(Box.createVerticalGlue()); // Adds flexible space below

        // Timer functionality
        timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timeLeft--;
                timerLabel.setText("Time: " + timeLeft + "s");

                if (timeLeft <= 0) {
                    timer.stop();
                    handleTimeOut();
                }
            }
        });

        timer.start(); // Start the timer based on the loaded level
    }

    public void startNewLevel() {
        // Only update the level and time if you're actually starting a new level
        timeLeft = INITIAL_TIME + (level - 1) * TIME_INCREMENT; // Reset and increase time for the next level
        timerLabel.setText("Time: " + timeLeft + "s");

        // Restart the timer for the new level
        if (!timer.isRunning()) {
            timer.restart();
        }
    }

    private void handleTimeOut() {
        JOptionPane.showMessageDialog(this, "Time's up! Restarting level " + level);
        restartLevel(); // Restart the level with a unified reset method
    }

    public int getLevel(){
        return this.level;
    }

    private void init(int level){
        if (level > 5) {
            level = 5; // Cap the level at 5
        }
        TubeDAO tubeDAO = new TubeDAO(level);
        this.tubeList = tubeDAO.getTubeList();
        this.undo = new LinkedList<>();
        this.size = tubeList.size();
        this.top = null;
        this.ax = 0.0;
        this.ay = 0.0;
        this.width = 0.0;
        this.height = 0.0;
        this.moveCount = 0;
    }

    public int currentTube(Point p){
        Rectangle2D.Double r= new Rectangle2D.Double();
        int x;
        int y;
        if (this.size < 7) {
            x = getXStart(this.size);
            y = (getHeight() - Constant.TUBE_HEIGHT) / 2 - 50;

            for (int i = 0; i < this.size; i++){
                r.setFrame(x, y, Constant.TUBE_WIDTH, Constant.TUBE_HEIGHT);
                x += this.getWidth()/this.size;
                if (r.contains(p)){
                    return i;
                }
            }
        } else {
            int row1Size = (this.size + 1) / 2;
            int row2Size = this.size - row1Size;
            // Get Position of row 1
            x = getXStart(row1Size);
            y = 50;

            for (int i = 0; i < row1Size; i++){
                r.setFrame(x, y, Constant.TUBE_WIDTH, Constant.TUBE_HEIGHT);
                x += this.getWidth()/row1Size;
                if (r.contains(p)){
                    return i;
                }
            }
            // get Position of row 2
            x = getXStart(row2Size);
            y = y + Constant.TUBE_HEIGHT + Constant.ROW_TUBE_DISTANCE;

            for (int i = row1Size; i < this.size; i++){
                r.setFrame(x, y, Constant.TUBE_WIDTH, Constant.TUBE_HEIGHT);
                x += this.getWidth()/row2Size;
                if (r.contains(p)){
                    return i;
                }
            }
        }
        return -1;
    }

    public boolean isCompleteGame() {
        for (Tube tube : this.tubeList) {
            if (!tube.isHomogenous()) {
                return false;
            }
        }

        // Stop the timer if the game is completed
        if (timer.isRunning()) {
            timer.stop();
        }

        return true;
    }

    public void reset() {
        restartLevel(); // Reuse the restart logic for manual reset as well
    }

    private void restartLevel() {
        // Stop any existing timer instance before resetting
        if (timer.isRunning()) {
            timer.stop();
        }
        init(this.level);  // Re-initialize game state for the current level
        timeLeft = INITIAL_TIME + (this.level - 1) * TIME_INCREMENT; // Reset time for the level
        timerLabel.setText("Time: " + timeLeft + "s"); // Update timer label

        // Restart the timer for a fresh start on the current level
        timer.restart();
        repaint();
    }

    public void nextLevel() {
        if (isCompleteGame()) { // Ensure game completion triggers the level increment
            if (this.level < levelQuantity) { // Level quantity is limited to 5
                this.level++;
                init(this.level); // Initialize for the new level
                repaint();
                timer.stop();  // Stop the timer before moving to the next level
                startNewLevel();  // Reset and start the timer for the new level
            } else {
                endGame(); // End the game if it's the last level
            }
        }
    }


    private void endGame() {
        int choice = JOptionPane.showOptionDialog(this, "Congratulations! You've completed all levels!",
                "Game Over", JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null,
                new Object[]{"Restart", "Exit"}, "Restart");
        if (choice == 0) {
            level = 1;
            startNewLevel(); // Restart the game
        } else {
            timer.stop(); // Stop the timer when the game exits
            System.exit(0); // Exit the game
        }
    }

    public void preLevel() {
        if (this.level > 1){
            this.level--;
            init(this.level);
            repaint();
        }
    }

    public boolean isUndo() {
        if (this.undo.size() == 0){
            return false;
        }
        return true;
    }

    public void undo() {
        if (isUndo()){
            Step step = this.undo.getLast();
            this.tubeList.get(step.getTubeTaken()).push(step.getBlockPop());
            this.tubeList.get(step.getTubeGiven()).pop();
            this.undo.removeLast();
            this.moveCount--;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g1=(Graphics2D)g;
        int x;
        int y;

        if (this.size < 7) {
//          Reduced from x = (getWidth() - (Constant.TUBE_WIDTH * this.size + (getWidth() / this.size - Constant.TUBE_WIDTH) * (this.size - 1)))/2
            x = getXStart(this.size);
            y = (getHeight() - Constant.TUBE_HEIGHT) / 2 - 50;

            drawAllTube(g1, 0, this.size, this.size, x, y);
        } else {
            int row1Size = (this.size + 1) / 2;
            int row2Size = this.size - row1Size;
            // Get Position of row 1
            x = getXStart(row1Size);
            y = 50;
            // Draw tube in row 1
            drawAllTube(g1,0, row1Size, row1Size, x, y);

            // get Position of row 2
            x = getXStart(row2Size);
            y = y + Constant.TUBE_HEIGHT + Constant.ROW_TUBE_DISTANCE;
            // Draw tube in row 2
            drawAllTube(g1, row1Size, this.size, row2Size, x, y);
        }

        if(isDrag == true && top != null) {
            g1.setColor(top.getColor());
            g1.fill(top);
        }
    }

    private void drawAllTube(Graphics2D g, int startTube, int endTube, int rowSize, int x, int y){
        for (int i = startTube; i < endTube; i++){
            tubeList.get(i).setX(x);
            tubeList.get(i).setY(y);
            tubeList.get(i).drawTube(g);
            x += this.getWidth()/rowSize;
        }
    }

    private int getXStart(int rowSize) {
        return (getWidth()/rowSize - Constant.TUBE_WIDTH) / 2;
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        Point p = e.getPoint();
        int n = currentTube(p);
//        System.out.println(n);
        if (n != -1){
            if(!this.tubeList.get(n).isEmpty()){
                this.top = this.tubeList.get(n).top();
                if (top.contains(p)){
                    Sounds.dragSound(sound);
                    tubeList.get(n).pop();
                    this.ax=top.getX();
                    this.ay=top.getY();
                    this.width = p.getX() - ax;
                    this.isDrag = true;
                    this.tubePop = n;
                }
                else {
                    this.top = null;
                    this.isDrag = false;
                    this.tubePop = -1;
                }
            }
        }

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if(this.top != null && isDrag) {
            int tubeNum = currentTube(e.getPoint());
            double x, y;
            if (tubeNum == -1) {
                tubeNum = currentTube(new Point((int) ax, (int) ay));
                if (!this.tubeList.get(tubeNum).isEmpty()) {
                    x = this.tubeList.get(tubeNum).getX() + 5;
                    y = this.tubeList.get(tubeNum).top().getY() - 42;
                }else {
                    x = this.tubeList.get(tubeNum).getX() + 5;
                    y = this.tubeList.get(tubeNum).getY() + 148;
                }
            }else {
                if (!this.tubeList.get(tubeNum).isEmpty()) {
                    if (!this.tubeList.get(tubeNum).isFull() && top.equals(tubeList.get(tubeNum).top())) {
                        x = this.tubeList.get(tubeNum).getX() + 5;
                        y = this.tubeList.get(tubeNum).top().getY() - 42;
                        if (this.tubePop != tubeNum) {
                            this.moveCount++;
                        }
                    } else {
                        Sounds.failSound(sound);
                        tubeNum = currentTube(new Point((int) this.ax, (int) this.ay));
                        if (!this.tubeList.get(tubeNum).isEmpty()) {
                            x = this.tubeList.get(tubeNum).getX() + 5;
                            y = this.tubeList.get(tubeNum).top().getY() - 42;
                        }else {
                            x = this.tubeList.get(tubeNum).getX() + 5;
                            y = this.tubeList.get(tubeNum).getY() + 148;
                        }

                    }
                } else {
                    x = this.tubeList.get(tubeNum).getX() + 5;
                    y = this.tubeList.get(tubeNum).getY() - 190;
                    if (this.tubePop != tubeNum) {
                        this.moveCount++;
                    }
                }
            }
            this.top.setFrame(x, y, this.top.getWidth(), this.top.getHeight());
            this.tubeList.get(tubeNum).push(this.top);

            if (this.undo.size() >= undoSize){
                this.undo.removeFirst();
            }
            if (this.tubePop != tubeNum){
                this.undo.addLast(new Step(tubePop, tubeNum, this.top));
            }

            this.top = null;
            this.isDrag = false;
            repaint();
        }

//        System.out.println("move:" + moveCount);
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        int cx = e.getX();
        int cy = e.getY();
        if(top!=null && isDrag == true){
            top.setFrame(cx-width,cy-height,top.getWidth(),top.getHeight());
            repaint();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }

    public void setSound(Boolean sound) {
        this.sound = sound;
    }
}
