package lphybeast.launcher;

import lphyext.manager.DependencyUtils;
import lphystudio.app.LPhyAppConfig;

import javax.swing.*;
import java.awt.*;

/**
 * @author Walter Xie
 */
public class LPhyBeastLauncher extends JFrame {

    private static final String APP_NAME = "LPhy BEAST";
    static {
        LPhyAppConfig.setupEcoSys(APP_NAME);
    }

    private final String VERSION;

    private final int MASK = LPhyAppConfig.MASK;

    public LPhyBeastLauncher() {
        // use MANIFEST.MF to store version in jar, or use system property in development,
        // otherwise VERSION = "DEVELOPMENT"
        VERSION = DependencyUtils.getVersion(LPhyBeastLauncher.class, "lphy.beast.version");

        setTitle(APP_NAME + " version " + VERSION);
        // not close lphy studio frame
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        final int MAX_WIDTH = 800;
        final int MAX_HEIGHT = 900;
        LPhyAppConfig.setFrameLocation(this, MAX_WIDTH, MAX_HEIGHT);

        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            desktop.setAboutHandler(e ->
                    LPhyAppConfig.buildAboutDialog(this, APP_NAME + " v " + VERSION, getHTMLCredits())
            );
        }

        JTabbedPane tabbedPane = new JTabbedPane();
        //The following line enables to use scrolling tabs.
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);


        getContentPane().add(tabbedPane, BorderLayout.CENTER);

        setVisible(true);
    }

    private String getHTMLCredits() {
        return "<html><body width='%1s'><h3>Created by Walter Xie, Alexei Drummond, and Kylie Chen</h3>"+
                "<p>The Centre for Computational Evolution<br>"+
                "University of Auckland<br></p>"+
                "<p>Homepage :<br>"+
                "<a href=\""+LPhyAppConfig.LPHY_WEB+"\">"+LPhyAppConfig.LPHY_WEB+"</a></p>"+
                "<p>Source code distributed under the GNU Lesser General Public License Version 3</p>"+
                "<p>Require Java 17, current Java version " + System.getProperty("java.version") + "</p></html>";
    }

    public static void main(String[] args) {
        LPhyBeastLauncher launcher = new LPhyBeastLauncher();
    }

}
