package tetris;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;
import tetris.ProjectConstants.GameState;
import static tetris.ProjectConstants.getResURL;
import tetris.ai.AIExecutor;
import tetris.ai.TetrisAI;
import tetris.generic.Block;
import tetris.generic.Score;
import tetris.generic.TetrisEngine;
import tetris.generic.TetrisEngineListener;

/*
 * TetrisPanel is the panel that contains the (main) panels AKA. core. This also
 * holds most of the objects needed to render the game on a JDesktopPane.
 */
public class TetrisPanel extends JPanel {

    //---------------BEGIN PUBLIC VARIABLES---------------//
    /*
     * Public reference to the TetrisEngine object.
     */
    public TetrisEngine engine;
    /*
     * Background image used for the game.
     */
    public Image bg = null;
    /*
     * Foreground image.
     */
    public Image fg = null;

    /*
     * Is it being controlled by a human or ai?
     */
    public boolean isHumanControlled = false;

    /*
     * AI object controlling the game.
     */
    public AIExecutor controller = null;

    /*
     * Dimensions (Width and HEIGHT) of each square. Squares in Tetris must be
     * the same HEIGHT and WIDTH.
     */
    public int squaredim;

    /*
     * Dimensions of the squares of the next block as drawn. See squaredim.
     */
    public int nextblockdim = 18;

    private Dimension bounds;
    private int lastLines;
    private Timer timer;

    /*
     * Public TetrisPanel constructor.
     */
    public TetrisPanel() throws IOException {
        //Initialize the TetrisEngine object.
        engine = new TetrisEngine();
        squaredim = 300 / engine.WIDTH;
        bounds = new Dimension(squaredim * engine.WIDTH, squaredim * engine.HEIGHT);

        //This is the bg-image.
        bg = ImageIO.read(getResURL("/image/background.png"));
        fg = ImageIO.read(getResURL("/image/backlayer.png"));

        // Actually, the background is the actual background plus
        // the meta image.
        Image meta = ImageIO.read(getResURL("/image/metalayer.png"));
        Graphics g = bg.getGraphics();
        g.drawImage(meta, 0, 0, null);

        this.timer = new Timer(100, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                repaint();
            }
        });
        this.timer.setInitialDelay(0);
        this.timer.start();

        this.addKeyListener(new KeyPressManager());
        this.setFocusable(true);
        this.engine.setState(GameState.PAUSED);

        if (!this.isHumanControlled) {
            ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));
            TetrisAI ai = new TetrisAI(executor);
            //ai.MakeItDumb();
            this.controller = new AIExecutor(ai, engine, executor);
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g); //To change body of generated methods, choose Tools | Templates.
        
        //Draw: background, then main, then foreground.
        g.drawImage(bg, 0, 0, this);
        //engine.draw(g);

        drawGame(g);
        g.drawImage(fg, 0, 0, this);
    }

    private void drawGame(Graphics g) {
        //The coordinates of the top left corner of the game board.
        int mainx = (this.getWidth() - bounds.width) / 2 + 50;
        int mainy = (this.getHeight() - bounds.height) / 2;

        //Create a border;
        g.setColor(Color.BLACK);
        g.drawRect(mainx - 1, mainy - 1,
                   bounds.width + 2, bounds.height + 2);

        g.setColor(Color.BLACK);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));

        Score score = engine.getScore();
        g.drawString(String.format("%06d", score.getScore()), 156, 213);//Draw score
        g.drawString(String.format("%03d", score.getLinesRemoved()), 156, 250);//Draw lines

        //Loop and draw all the blocks.
        Block[][] blocks = engine.getBlocks();
        for (int c1 = 0; c1 < blocks.length; c1++) {
            for (int c2 = 0; c2 < blocks[c1].length; c2++) {
                // Just in case block's null, it doesn't draw as black.
                g.setColor(Block.emptycolor);
                g.setColor(blocks[c1][c2].getColor());

                g.fillRect(mainx + c1 * squaredim,
                           mainy + c2 * squaredim, squaredim, squaredim);

                //Draw square borders.
                g.setColor(new Color(255, 255, 255, 25));
                g.drawRect(mainx + c1 * squaredim,
                           mainy + c2 * squaredim, squaredim, squaredim);

            }
        }

        int nextx = 134;
        int nexty = 336;

        //Less typing.
        Block[][] nextb;
        if (engine.getNextblock() != null) {
            nextb = engine.getNextblock().array;
            //Loop and draw next block.
            for (int c1 = 0; c1 < nextb.length; c1++) {
                for (int c2 = 0; c2 < nextb[c1].length; c2++) {
                    Color c = nextb[c2][c1].getColor();

                    if (c != null && !c.equals(Block.emptycolor)) {
                        g.setColor(new Color(0, 0, 0, 128));

                        g.fillRect(nextx + c1 * nextblockdim,
                                   nexty + c2 * nextblockdim, nextblockdim, nextblockdim);
                    }
                }
            }
        }

        if (engine.getState() == GameState.PAUSED || engine.getState() == GameState.GAMEOVER) {
            g.setColor(new Color(255, 255, 255, 160));
            g.setFont(new Font(Font.SERIF, Font.BOLD, 16));
            String pausestring = null;

            if (engine.getState() == GameState.PAUSED) {
                pausestring = "(SHIFT to play).";
            }

            if (engine.getState() == GameState.GAMEOVER) {
                if (this.isHumanControlled) {
                    pausestring = "Game over (SHIFT to restart).";
                } else {
                    pausestring = Integer.toString(this.lastLines)
                            + (this.lastLines == 1 ? " Line" : " Lines");
                }
            }

            g.drawString(pausestring,
                         (this.getWidth() - g.getFontMetrics().stringWidth(pausestring)) / 2 + 50, 300);
        }

    }

    /*
     * This is a class that manages key presses. It's so that each press is sent
     * once, and if you hold a key, it doesn't count as multiple presses.
     *
     * Note that some keys should never be counted more than once.
     */
    private class KeyPressManager extends KeyAdapter {
        // Called when keypress is detected.
        @Override
        public void keyPressed(KeyEvent ke) {
            switch (ke.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    if (isHumanControlled)
                        engine.keyleft();
                    break;
                case KeyEvent.VK_RIGHT:
                    if (isHumanControlled)
                        engine.keyright();
                    break;
                case KeyEvent.VK_DOWN:
                    if (isHumanControlled)
                        engine.keydown();
                    break;
                case KeyEvent.VK_SPACE:
                    if (isHumanControlled)
                        engine.keyslam();
                    break;
                case KeyEvent.VK_UP:
                case KeyEvent.VK_Z:
                    if (isHumanControlled)
                        engine.keyrotate();
                    break;
                case KeyEvent.VK_SHIFT:
                    if (engine.getState() == GameState.PAUSED) {
                        engine.setState(GameState.PLAYING);
                    } else {
                        engine.setState(GameState.PAUSED);
                    }
                    break;
            }
        }
    }
}
