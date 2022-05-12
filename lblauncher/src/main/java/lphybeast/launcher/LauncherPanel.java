package lphybeast.launcher;

import lphybeast.LPhyBeast;
import lphystudio.app.Utils;
import lphystudio.app.graphicalmodelpanel.ErrorPanel;
import lphystudio.core.swing.SpringUtilities;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Launcher Panel
 * @author Walter Xie
 */
public class LauncherPanel extends JPanel {

    final Color LL_GRAY = new Color(230, 230, 230);

    private LPhyBeast lPhyBeast;
    private JProgressBar progressBar;

    public LauncherPanel() {
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new SpringLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JTextField input = new JTextField();
        input.setEditable(false);
        input.setBackground(LL_GRAY);

        JButton buttonInput = new JButton("Select LPhy script : ");
        buttonInput.setToolTipText("Input file containing LPhy model specification, must have .lphy postfix.");
        FileNameExtensionFilter inFilter = new FileNameExtensionFilter("LPhy script", "lphy");
        buttonInput.setMnemonic(KeyEvent.VK_I);
        buttonInput.setActionCommand("input");

        JTextField output = new JTextField();
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
        JTextField rep = new JTextField("1"); // integer
        topPanel.add(label);
        topPanel.add(rep);

        label = new JLabel("MCMC chain length : ", JLabel.TRAILING);
        label.setToolTipText("The total chain length of MCMC, default to 1 million.");
        JTextField chainLen = new JTextField("1000000"); // long
        topPanel.add(label);
        topPanel.add(chainLen);

        label = new JLabel("Pre-burnin : ", JLabel.TRAILING);
        label.setToolTipText("The number of burn in samples taken before entering the main loop of MCMC. " +
                "If empty (or < 0), as default, then estimate it based on all state nodes size.");
        JTextField burnin = new JTextField(""); // integer
        topPanel.add(label);
        topPanel.add(burnin);

        JButton buttonRun = new JButton("Generate XML");
        buttonRun.setFont(buttonRun.getFont().deriveFont(Font.BOLD));

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        topPanel.add(buttonRun);
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

        ErrorPanel errorPanel = new ErrorPanel();
        add(errorPanel, BorderLayout.CENTER);

        buttonRun.addActionListener(e -> {
//            errorPanel.setText("");
            try {
                LPhyBeast lphyBeast = createLPhyBeast(input, output);

                lphyBeast.setRep(getInt(rep));
                lphyBeast.run(getLong(chainLen), getInt(burnin));
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

    }

    private LPhyBeast createLPhyBeast(JTextField input, JTextField output) throws IOException {
        Path infile = Paths.get(input.getText());
        Path outfile = Paths.get(output.getText());
        return new LPhyBeast(infile, outfile, null);
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


}
