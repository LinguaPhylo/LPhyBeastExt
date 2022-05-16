package lphybeast.launcher;

import beast.app.beastapp.BeastLauncher;
import beast.util.BEASTClassLoader;
import beast.util.PackageManager;
import lphy.util.LoggerUtils;
import lphy.util.Progress;
import lphystudio.app.Utils;
import lphystudio.app.graphicalmodelpanel.ErrorPanel;
import lphystudio.core.swing.SpringUtilities;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Launcher Panel
 * @author Walter Xie
 */
public class LauncherPanel extends JPanel implements ActionListener, PropertyChangeListener {

    final Color LL_GRAY = new Color(230, 230, 230);

    private final ErrorPanel errorPanel;
    private JProgressBar progressBar = new JProgressBar(0, 100);
    private JButton runButton;
    private Task task;

    private JTextField input;
    private JTextField output;
    private JTextField rep;
    private JTextField chainLen;
    private JTextField burnin;

    public LauncherPanel() {
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new SpringLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        input = new JTextField();
        input.setEditable(false);
        input.setBackground(LL_GRAY);

        JButton buttonInput = new JButton("Select LPhy script : ");
        buttonInput.setToolTipText("Input file containing LPhy model specification, must have .lphy postfix.");
        FileNameExtensionFilter inFilter = new FileNameExtensionFilter("LPhy script", "lphy");
        buttonInput.setMnemonic(KeyEvent.VK_I);
        buttonInput.setActionCommand("input");

        output = new JTextField();
        output.setEditable(false);
        output.setBackground(LL_GRAY);

        JButton buttonOutput = new JButton("Output BEAST XML : ");
        buttonOutput.setToolTipText("Output XML file for BEAST 2. " +
                "If replicates > 1, the index will append to the output XML file name.");
        FileNameExtensionFilter outFilter = new FileNameExtensionFilter("XML", "xml");
        buttonInput.setMnemonic(KeyEvent.VK_O);
        buttonInput.setActionCommand("output");
        buttonOutput.addActionListener(e -> {
            File selectedFile = Utils.getFileFromFileChooser(this,
                    outFilter, JFileChooser.FILES_ONLY, false);
            if (selectedFile != null)
                output.setText(selectedFile.getAbsolutePath());
        });

        buttonInput.addActionListener(e -> {
            File selectedFile = Utils.getFileFromFileChooser(this,
                    inFilter, JFileChooser.FILES_ONLY, true);
            if (selectedFile != null) {
                input.setText(selectedFile.getAbsolutePath());
                File outFile = replaceFileExt(selectedFile, "xml");
                if (outFile != null) // use the same file name stem
                    output.setText(outFile.getAbsolutePath());
            }
        });

        topPanel.add(buttonInput);
        topPanel.add(input);
        topPanel.add(buttonOutput);
        topPanel.add(output);

        JLabel label = new JLabel("Replicates : ", JLabel.TRAILING);
        label.setToolTipText("The number of XML (simulations) for given LPhy script." +
                "If more than 1, the index will append to the output XML file name.");
        rep = new JTextField("1"); // integer
        topPanel.add(label);
        topPanel.add(rep);

        label = new JLabel("MCMC chain length : ", JLabel.TRAILING);
        label.setToolTipText("The total chain length of MCMC, default to 1 million.");
        chainLen = new JTextField("1000000"); // long
        topPanel.add(label);
        topPanel.add(chainLen);

        label = new JLabel("Pre-burnin : ", JLabel.TRAILING);
        label.setToolTipText("The number of burn in samples taken before entering the main loop of MCMC. " +
                "If empty (or < 0), as default, then estimate it based on all state nodes size.");
        burnin = new JTextField(""); // integer
        topPanel.add(label);
        topPanel.add(burnin);

        runButton = new JButton("Generate XML");
        runButton.setFont(runButton.getFont().deriveFont(Font.BOLD));
        runButton.addActionListener(this);

        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        topPanel.add(runButton);
        topPanel.add(progressBar);

        //Lay out the panel.
        SpringUtilities.makeCompactGrid(topPanel,
                6, 2, //rows, cols
                6, 6,    //initX, initY
                6, 6);     //xPad, yPad

        add(topPanel, BorderLayout.NORTH);

//        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, jPanel);
//        splitPane.setDividerLocation(200);
//        splitPane.setResizeWeight(0.5);

        errorPanel = new ErrorPanel();
        errorPanel.setNoLvlName(true);
        add(errorPanel, BorderLayout.CENTER);

    }


    @Override
    public void actionPerformed(ActionEvent e) {
        runButton.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // javax.swing.SwingWorker instance
        task = new Task();
        task.addPropertyChangeListener(this);
        task.execute();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress".equals(evt.getPropertyName())) {
            int progress = (Integer) evt.getNewValue();
            progressBar.setValue(progress);
        }
    }


    private int getInt(JTextField textField) {
        String intStr = textField.getText().trim();
        if (intStr.isEmpty()) return -1;
        return Integer.parseInt(intStr);
    }

    private long getLong(JTextField textField) {
        String longStr = textField.getText().trim();
        if (longStr.isEmpty()) return -1;
        return Long.parseLong(longStr);
    }

    private File replaceFileExt(File file, String newExt) {
        if (file.exists()) {
            String fn = file.getAbsolutePath();
            int i = fn.lastIndexOf('.');
            if (i > 0)
                return new File(fn.substring(0, i) + "." + newExt);
        }
        return null;
    }

    class Task extends SwingWorker<Void, Void> implements Progress {
        public static final String LPHY_BEAST_CLS = "lphybeast.LPhyBeast";

        private int start = 5;
        private int end = 95;
        private double progPer;
        /*
         * Main task. Executed in background thread.
         */
        @Override
        public Void doInBackground() {
            setProgress(0);
            errorPanel.clear();

            if (input.getText().isEmpty()) {
                LoggerUtils.log.severe("Please select a LPhy script file !");
                return null;
            }

            try {
                String classPath = BeastLauncher.getPath(false, null);

                PackageManager.loadExternalJars();
                for (String jarFile : classPath.split(File.pathSeparator)) {
                    if (jarFile.toLowerCase().endsWith("jar")) {
                        BEASTClassLoader.classLoader.addJar(jarFile);
                    }
                }
            } catch (Exception e) {
                LoggerUtils.logStackTrace(e);
                return null;
            }

            Path infile = Paths.get(input.getText());
            Path outfile = Paths.get(output.getText());
            int r = getInt(rep);
            long ch = getLong(chainLen);
            int b = getInt(burnin);

            setProgress(3);

            try {
                Class<?> mainClass = BEASTClassLoader.forName(LPHY_BEAST_CLS);
                // Path infile, Path outfile, Path wd, long chainLength, int preBurnin
                Object o = mainClass.getConstructor(Path.class, Path.class, Path.class, long.class, int.class)
                        .newInstance(infile, outfile, null, ch, b);

                // start run
                setProgress(getStart());

                // lphyBeast.run(r, this);
                Method runMethod = mainClass.getMethod("run", int.class, Progress.class);
                runMethod.invoke(o, r, this);
            } catch (Exception e) {
                LoggerUtils.logStackTrace(e);
                return null;
            }
            setProgress(100);
            return null;
        }

        /*
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
            runButton.setEnabled(true);
            setCursor(null); //turn off the wait cursor
        }

        /**
         * Handle both single process and multi-processes by interval(s).
         * The percentage based on each interval.
         * @param percentage  based on the interval (= end - start), not the whole process.
         *                    So set progress = getStartValue() + percentage * getInterval().
         */
        @Override
        public void setProgressPercentage(double percentage) {
            if (percentage > 1)
                throw new IllegalArgumentException("Illegal progress percentage value : " + percentage + " !");
            this.progPer = percentage;
            int p = getStart() + (int) (percentage * getInterval());
            setProgress(Math.min(p, getEnd()));
        }

        @Override
        public double getCurrentPercentage() {
            return progPer;
        }

        @Override
        public int getStart() {
            return start;
        }

        @Override
        public int getEnd() {
            return end;
        }

        @Override
        public void setStart(int start) {
            this.start = start;
        }

        @Override
        public void setEnd(int end) {
            this.end = end;
        }
    }

}
