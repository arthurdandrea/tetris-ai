package tetris;

import java.awt.Dimension;
import java.io.IOException;
import javax.swing.JFrame;

/*
 * The game window.
 */
public class GameWindow extends JFrame {

    private TetrisPanel t;

    /*
     * Creates a GameWindow, by default.
     */
    public GameWindow() throws IOException {
        super();
        setUndecorated(false);
        setTitle("JTetris");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);


        t = new TetrisPanel();

        t.setPreferredSize(new Dimension(800, 600));
        setContentPane(t);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        t.engine.startengine();
    }
}
