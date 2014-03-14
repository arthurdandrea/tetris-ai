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
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;
import tetris.ai.AIExecutor;
import tetris.ai.TetrisAI;
import tetris.generic.Block;
import tetris.generic.Score;
import tetris.generic.TetrisEngine;
import tetris.generic.TetrisEngine.GameState;
import tetris.generic.Tetromino;

/*
 * TetrisPanel is the panel that contains the (main) panels AKA. core. This also
 * holds most of the objects needed to render the game on a JDesktopPane.
 */
public class TetrisPanel extends JPanel {

    public static final ColorTheme colorTheme1 = ColorTheme.create()
            .set(Tetromino.Type.Long, new Color(0, 0, 0, 220))
            .set(Tetromino.Type.Box, new Color(0, 0, 0, 205))
            .set(Tetromino.Type.L, new Color(0, 0, 0, 190))
            .set(Tetromino.Type.J, new Color(0, 0, 0, 165))
            .set(Tetromino.Type.T, new Color(0, 0, 0, 140))
            .set(Tetromino.Type.S, new Color(0, 0, 0, 125))
            .set(Tetromino.Type.Z, new Color(0, 0, 0, 110))
            .setEmpty(new Color(120, 120, 190, 90))
            .finish();
    public static final ColorTheme colorTheme2 = ColorTheme.create()
            .set(Tetromino.Type.Long, new Color(102, 102, 204))
            .set(Tetromino.Type.Box, new Color(204, 102, 204))
            .set(Tetromino.Type.L, new Color(102, 204, 204))
            .set(Tetromino.Type.J, new Color(218, 170, 0))
            .set(Tetromino.Type.T, new Color(204, 204, 102))
            .set(Tetromino.Type.S, new Color(102, 204, 102))
            .set(Tetromino.Type.Z, new Color(204, 102, 102))
            .setEmpty(Color.WHITE)
            .finish();

    /**
     * Returns a resource as a URL object, for certain file parsing. Should
     * accomodate Eclipse and other clients/IDEs as well. Currently it loads
     * resources from Eclipse, the jar file, and from Tortoise.
     */
    @SuppressWarnings("deprecation")
    public static URL getResURL(String path) throws IOException {
        try {
            File f = new File("." + path);
            if (!f.exists()) {
                throw new Exception();
            }
            return f.getCanonicalFile().toURL();
        } catch (Exception e) {
            try {
                return TetrisPanel.class.getResource(path);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        throw new RuntimeException("File: " + path + " not found.");
    }

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
    public boolean isHumanControlled = true;

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

    private final Dimension bounds;
    private int lastLines;
    private final Timer timer;
    private final ColorTheme theme;

    /*
     * Public TetrisPanel constructor.
     */
    public TetrisPanel() throws IOException {
        theme = colorTheme1;
        //Initialize the TetrisEngine object.
        engine = new TetrisEngine();
        squaredim = 20;//300 / engine.WIDTH;
        bounds = new Dimension(squaredim * engine.defs.width, squaredim * engine.defs.height);

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
        Dimension size = this.getSize();
        Score score = this.engine.getScore();
        Tetromino nextblock = engine.getNextblock();

        // The coordinates of the top left corner of the game board.
        int mainx = (size.width - bounds.width) / 2 + 50;
        int mainy = (size.height - bounds.height) / 2;

        // Create a border;
        g.setColor(Color.BLACK);
        g.drawRect(mainx - 1, mainy - 1, bounds.width + 2,
                   bounds.height + 2);

        g.setColor(Color.BLACK);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));

        // Draw score
        g.drawString(String.format("%06d", score.getScore()), 156, 213);
        // Draw lines
        g.drawString(String.format("%03d", score.getLinesRemoved()), 156, 250);

        // Loop and draw all the blocks.
        this.draw(g, engine.getBlocks(), mainx, mainy);

        int nextx = 134;
        int nexty = 336;

        if (nextblock != null) {
            Block[][] nextb = nextblock.array;
            Color color = theme.getColor(nextblock.type);
            //Loop and draw next block.
            for (int i = 0; i < nextb.length; i++) {
                for (int j = 0; j < nextb[i].length; j++) {
                    if (nextb[i][j].getState() != Block.EMPTY) {
                        g.setColor(color);
                        g.fillRect(nextx + i * nextblockdim,
                                   nexty + j * nextblockdim, nextblockdim, nextblockdim);
                    }
                    // Draw square borders.
                    g.setColor(new Color(255, 255, 255, 25));
                    g.drawRect(nextx + i * nextblockdim,
                               nexty + j * nextblockdim, nextblockdim, nextblockdim);
                }
            }
        }

        if (engine.getState() == GameState.PAUSED || engine.getState() == GameState.GAMEOVER) {
            g.setColor(new Color(255, 255, 255, 160));
            g.setFont(new Font(Font.SERIF, Font.BOLD, 16));
            String pausestring;

            if (engine.getState() == GameState.PAUSED) {
                pausestring = "(SHIFT to play).";
            } else { // if (engine.getState() == GameState.GAMEOVER)
                if (this.isHumanControlled) {
                    pausestring = "Game over (SHIFT to restart).";
                } else {
                    pausestring = Integer.toString(this.lastLines)
                            + (this.lastLines == 1 ? " Line" : " Lines");
                }
            }

            g.drawString(pausestring, (this.getWidth() - g.getFontMetrics().stringWidth(pausestring)) / 2 + 50, 300);
        }
    }

    private void draw(Graphics g, Block[][] blocks, int x, int y) {
        for (int i = 0; i < blocks.length; i++) {
            for (int j = 0; j < blocks[i].length; j++) {
                // Just in case block's null, it doesn't draw as black.
                g.setColor(theme.getColor(blocks[i][j].getType()));

                g.fillRect(x + i * squaredim,
                           y + j * squaredim, squaredim, squaredim);

                //Draw square borders.
                g.setColor(new Color(255, 255, 255, 25));
                g.drawRect(x + i * squaredim,
                           y + j * squaredim, squaredim, squaredim);

            }
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
                if (isHumanControlled) {
                    engine.keyleft();
                }
                break;
            case KeyEvent.VK_RIGHT:
                if (isHumanControlled) {
                    engine.keyright();
                }
                break;
            case KeyEvent.VK_DOWN:
                if (isHumanControlled) {
                    engine.keydown();
                }
                break;
            case KeyEvent.VK_SPACE:
                if (isHumanControlled) {
                    engine.keyslam();
                }
                break;
            case KeyEvent.VK_UP:
            case KeyEvent.VK_Z:
                if (isHumanControlled) {
                    engine.keyrotate();
                }
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
