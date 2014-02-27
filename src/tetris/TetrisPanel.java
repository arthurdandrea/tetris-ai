package tetris;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import tetris.generic.TetrisEngine;
import tetris.ai.TetrisAI;
import tetris.ai.AbstractAI;
import tetris.ProjectConstants.GameState;
import static tetris.ProjectConstants.getResURL;
import static tetris.ProjectConstants.sleep_;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import static tetris.ProjectConstants.addLeadingZeroes;
import tetris.generic.Block;

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
    public AbstractAI controller = null;

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

    /*
     * Public TetrisPanel constructor.
     */
    public TetrisPanel() {
        //Initialize the TetrisEngine object.
        engine = new TetrisEngine(this);
        squaredim = 20;//300 / engine.WIDTH;
        bounds = new Dimension(squaredim * engine.WIDTH, squaredim * engine.HEIGHT);

        //This is the bg-image.
        try {
            bg = ImageIO.read(getResURL("/image/background.png"));
            fg = ImageIO.read(getResURL("/image/backlayer.png"));

            // Actually, the background is the actual background plus
            // the meta image.
            Image meta = ImageIO.read(getResURL("/image/metalayer.png"));
            Graphics g = bg.getGraphics();
            g.drawImage(meta, 0, 0, null);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot load image.");
        }

        //Animation loop. Updates every 40 milliseconds (25 fps).
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        //Logger.getLogger(TetrisPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    repaint();
                }
            }
        }.start();

        //Add all these key functions.
        KeyPressManager kpm = new KeyPressManager();
        kpm.putKey(KeyEvent.VK_LEFT, new Runnable() {
            public void run() {
                TetrisPanel.this.engine.keyleft();
            }
        });
        kpm.putKey(KeyEvent.VK_RIGHT, new Runnable() {
            public void run() {
                TetrisPanel.this.engine.keyright();
            }
        });
        kpm.putKey(KeyEvent.VK_DOWN, new Runnable() {
            public void run() {
                TetrisPanel.this.engine.keydown();
            }
        });
        kpm.putKey(KeyEvent.VK_SPACE, new Runnable() {
            public void run() {
                TetrisPanel.this.engine.keyslam();
            }
        });
        kpm.putKey(KeyEvent.VK_UP, new Runnable() {
            public void run() {
                TetrisPanel.this.engine.keyrotate();
            }
        });
        kpm.putKey(KeyEvent.VK_Z, new Runnable() {
            public void run() {
                TetrisPanel.this.engine.keyrotate();
            }
        });
        kpm.putKey(KeyEvent.VK_SHIFT, new Runnable() {
            @Override
            public void run() {
                if (engine.state != GameState.GAMEOVER && controller != null && !controller.thread.isAlive()) {
                    controller.send_ready(engine.score);
                }
                if (engine.state == GameState.PAUSED) {
                    engine.state = GameState.PLAYING;
                } else {
                    engine.state = GameState.PAUSED;
                    //System.out.println(controller.thread.isAlive());
                }
            }
        });

        addKeyListener(kpm);

        //Focus when clicked.
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                TetrisPanel.this.requestFocusInWindow();
            }
        });

        setFocusable(true);
        engine.state = GameState.PAUSED;

        if (!isHumanControlled) {
            controller = new TetrisAI(this);
        }
    }

    /*
     * Paints this component, called with repaint().
     */
    @Override
    public void paintComponent(Graphics g) {
        //Necessary mostly because this is a JDesktopPane and
        //not a JPanel.
        super.paintComponent(g);

        //Draw: background, then main, then foreground.
        g.drawImage(bg, 0, 0, this);
        //engine.draw(g);

        synchronized (engine) {
            drawGame(g);
        }
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

        g.drawString(addLeadingZeroes(engine.score, 6), 156, 213);//Draw score
        g.drawString(addLeadingZeroes(engine.lines, 3), 156, 250);//Draw lines

        //Loop and draw all the blocks.
        for (int c1 = 0; c1 < engine.blocks.length; c1++) {
            for (int c2 = 0; c2 < engine.blocks[c1].length; c2++) {
                // Just in case block's null, it doesn't draw as black.
                g.setColor(Block.emptycolor);
                g.setColor(engine.blocks[c1][c2].getColor());

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
        if (engine.nextblock != null) {
            nextb = engine.nextblock.array;
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

        if (engine.state == GameState.PAUSED || engine.state == GameState.GAMEOVER) {
            g.setColor(new Color(255, 255, 255, 160));
            g.setFont(new Font(Font.SERIF, Font.BOLD, 16));
            String pausestring = null;

            if (engine.state == GameState.PAUSED) {
                pausestring = "(SHIFT to play).";
            }

            if (engine.state == GameState.GAMEOVER) {
                if (this.isHumanControlled) {
                    pausestring = "Game over (SHIFT to restart).";
                } else {
                    pausestring = Integer.toString(engine.lastlines)
                            + (engine.lastlines == 1 ? " Line" : " Lines");
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
    class KeyPressManager extends KeyAdapter {

        static final int delay = 40;

        class KeyHandlingThread extends Thread {

            volatile boolean flag = true;

            public void run() {
                // The key handling loop.
                // Each iteration, call the functions whose keys are currently
                // being held down.

                while (flag) {
                    sleep_(delay);

                    //if(keys[0]) keymap.get(KeyEvent.VK_LEFT).run();
                    //if(keys[1]) keymap.get(KeyEvent.VK_RIGHT).run();
                    if (keys[2]) {
                        keymap.get(KeyEvent.VK_DOWN).run();
                    }
                }
            }
        }
        KeyHandlingThread keythread;
        // After some testing: I think that it's best to only have the down button
        // have special handling.
        // Lol now I think it's a bit of a waste to have an entire thread running
        // for one button that's barely used.
        // Only keys that require special handling:
        // keys[0]: left
        // keys[1]: right
        // keys[2]: down
        volatile boolean[] keys = {false, false, false};

        KeyPressManager() {
            keythread = new KeyHandlingThread();

            if (TetrisPanel.this.isHumanControlled) {
                keythread.start();
            }
        }

        void putKey(int i, Runnable r) {
            keymap.put(i, r);
        }
        // This hashmap maps from an Int (from KeyEvent.getKeyCode()) to a
        // function, represented by a Runnable.
        Map<Integer, Runnable> keymap = new HashMap<Integer, Runnable>();

        // Called when keypress is detected.
        public void keyPressed(KeyEvent ke) {

            // Make special adjustments for handling of the shift key.
            if (!TetrisPanel.this.isHumanControlled && ke.getKeyCode() != KeyEvent.VK_SHIFT) {
                return;
            }

            int ck = ke.getKeyCode();
            if (keymap.containsKey(ck)) {
                /*
                 * if(ck==KeyEvent.VK_LEFT) keys[0]=true; else
                 * if(ck==KeyEvent.VK_RIGHT) keys[1]=true;
                 */

                if (ck == KeyEvent.VK_DOWN) {
                    keys[2] = true;
                } else {
                    keymap.get(ck).run();
                }
            }
        }

        // Called when key is released. Here we'll want to modify the map.
        public void keyReleased(KeyEvent ke) {

            if (!TetrisPanel.this.isHumanControlled) {
                return;
            }

            int ck = ke.getKeyCode();

            /*
             * if(ck==KeyEvent.VK_LEFT) keys[0]=false; else
             * if(ck==KeyEvent.VK_RIGHT) keys[1]=false;
             */
            if (ck == KeyEvent.VK_DOWN) {
                keys[2] = false;
            }
        }
    }
}
