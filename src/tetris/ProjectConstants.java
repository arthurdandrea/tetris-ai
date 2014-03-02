package tetris;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/*
 * This class contains a group of project constants. Use import static tetris.code.ProjectConstants.
 */
public class ProjectConstants {

    /*
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
            //eclipse workaround.
            try {
                return ProjectConstants.class.getResource(path);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        throw new RuntimeException("File: " + path + " not found.");
    }


    /*
     * Enum representation of the current game's state
     */
    //Moving this here lol.
    public enum GameState { PLAYING, PAUSED, GAMEOVER }
}
