package lphybeast;

import lphy.core.*;
import lphy.graphicalModel.RandomValueLogger;
import lphy.parser.REPL;
import lphy.system.UserDir;
import lphy.util.LoggerUtils;
import lphy.util.Progress;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Walter Xie
 * @author Alexei Dummond
 */
public class LPhyBeast implements Runnable {

    private final Path inPath, outPath;
    private int preBurnin;
    private long chainLength;
    private int rep = 1;

    /**
     * Handle the input file path, output file path, and user.dir.
     * Either can be a relative or absolute path.
     * If relative, then concatenate user.dir before it.
     * @param infile   lphy script file path.
     * @param outfile  XML file path. If null,
     *                 then use the input file name stem plus .xml,
     *                 and output to the user.dir.
     * @param wd       Use to set user.dir. If null,
     *                 then set user.dir to the parent folder of lphy script.
     */
    public LPhyBeast(Path infile, Path outfile, Path wd) throws IOException {
        //        if (versionInfoRequested) CommandLine.usage(this, System.out);
        if (infile == null || !infile.toFile().exists())
            throw new IOException("Cannot find LPhy script file ! " + (infile != null ? infile.toAbsolutePath() : null));
        String fileName = infile.getFileName().toString();
        if (!fileName.endsWith(".lphy"))
            throw new IllegalArgumentException("Invalid LPhy file: the postfix has to be '.lphy'");

        if (wd != null)
            UserDir.setUserDir(wd.toAbsolutePath().toString());
        // if the relative path, then concatenate user.dir before it
        inPath = UserDir.getUserPath(infile);
        // still need to set user.dir, if no -wd, in case LPhy script uses relative path
        if (wd == null)
            // set user.dir to the folder containing lphy script
            UserDir.setUserDir(inPath.getParent().toString());

        if (outfile != null) {
            outPath = UserDir.getUserPath(outfile);
        } else {
            String infileNoExt = fileName.substring(0, fileName.lastIndexOf("."));
            // add wd before file stem
            outPath = Paths.get(UserDir.getUserDir().toString(), infileNoExt + ".xml");
        }

        LoggerUtils.log.info("\nRead LPhy script from " + inPath.toAbsolutePath());
    }

    /**
     * For unit test, and then call {@link #lphyToXML(String, String, long, int)}.
     */
    public LPhyBeast() {
        inPath = null;
        outPath = null;
    }

    @Override
    public void run() {
        if (inPath == null || outPath == null || rep < 1 || chainLength * preBurnin < 1)
            throw new IllegalArgumentException("Illegal inputs : inPath = " + inPath + ", outPath = " + outPath +
                    ", rep = " + rep + ", chainLength = " + chainLength + ", preBurnin = " + preBurnin);
        try {
            run(rep, chainLength, preBurnin);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run(int rep, long chainLength, int preBurnin) throws IOException {
        run(rep, chainLength, preBurnin, null);
    }

    public void run(int rep, long chainLength, int preBurnin, Progress progress) throws IOException {
        final int start0 = 5;
        final int endAll = 95;
        BufferedReader reader;
        // e.g. well-calibrated validations
        if (rep > 1) {
            LoggerUtils.log.info("\nStart " + rep + " replicates : \n");

            final int incre = (endAll-start0) / rep;

            for (int i = 0; i < rep; i++) {
                if (progress != null) {
                    progress.setStart(start0 + incre * i);
                    progress.setEnd(start0 + incre * (i + 1));
                }

                // need new reader
                reader = lphyReader(progress);
                // add _i after file stem
                Path outPathPerRep = getOutPath(i);
                createXML(reader, outPathPerRep, chainLength, preBurnin, progress);
            }
        } else { // 1 simulation
            if (progress != null) {
                progress.setStart(start0);
                progress.setEnd(endAll);
            }

            reader = lphyReader(progress);
            createXML(reader, Objects.requireNonNull(outPath), chainLength, preBurnin, progress);
        }
    }


    private Path getOutPath(int i) {
        final String outPathNoExt = getPathNoExtension(outPath);
        // update outPath to add i
        return Paths.get(outPathNoExt + "_" + i + ".xml");
    }

    private String getPathNoExtension(Path path) {
        String str = Objects.requireNonNull(path).toString();
        return str.substring(0, str.lastIndexOf("."));
    }

    // the relative path given in readNexus in a script always refers to user.dir
    // fileNameStem for both outfile and XML loggers
    private void createXML(BufferedReader reader, Path outPath, long chainLength,
                           int preBurnin, Progress progress) throws IOException {
        final String pathNoExt = getPathNoExtension(outPath);
        // create XML string from reader, given file name and MCMC setting
        String xml = toBEASTXML(Objects.requireNonNull(reader), pathNoExt, chainLength, preBurnin, progress);
        if (progress != null)
            progress.setProgressPercentage(0.9);
        writeXML(xml, outPath);
    }

    private BufferedReader lphyReader(Progress progress) throws FileNotFoundException {
        // need to call reader each loop
        FileReader fileReader = new FileReader(Objects.requireNonNull(inPath).toFile());
        if (progress != null)
            progress.setProgressPercentage(0.1);
        return new BufferedReader(fileReader);
    }

    private void writeXML(String xml, Path outPath) throws IOException {
        FileWriter fileWriter = new FileWriter(Objects.requireNonNull(outPath).toFile());
        PrintWriter writer = new PrintWriter(fileWriter);
        writer.println(xml);
        writer.flush();
        writer.close();

        LoggerUtils.log.info("Save BEAST 2 XML to " + outPath.toAbsolutePath() + "\n");
    }


    /**
     * Alternative method to give LPhy script (e.g. from String), not only from a file.
     * @param reader
     * @param filePathNoExt
     * @param chainLength    if <=0, then use default 1,000,000.
     *                       logEvery = chainLength / numOfSamples,
     *                       where numOfSamples = 2000 as default.
     * @param preBurnin      preBurnin for BEAST MCMC, default to 0.
     * @return    BEAST 2 XML
     * @see BEASTContext#toBEASTXML(String, long, int)
     * @throws IOException
     */
    private String toBEASTXML(BufferedReader reader, String filePathNoExt, long chainLength,
                              int preBurnin, Progress progress) throws IOException {
        //*** Parse LPhy file ***//
        LPhyParser parser = new REPL();
        parser.source(reader);

        if (progress != null)
            progress.setProgressPercentage(0.2);

        // log true values and tree
        List<RandomValueLogger> loggers = new ArrayList<>();
        final String filePathNoExtTrueVaule = filePathNoExt + "_" + "true";
        loggers.add(new VarFileLogger(filePathNoExtTrueVaule, true, true));
        loggers.add(new TreeFileLogger(filePathNoExtTrueVaule));

        GraphicalLPhyParser gparser = new GraphicalLPhyParser(parser);
        Sampler sampler = new Sampler(gparser);
        sampler.sample(1, loggers);

        if (progress != null)
            progress.setProgressPercentage(0.3);

        // register parser
        BEASTContext context = new BEASTContext(parser);

        //*** Write BEAST 2 XML ***//
        // remove any dir in filePathNoExt here
        if (filePathNoExt.contains(File.separator))
            filePathNoExt = filePathNoExt.substring(filePathNoExt.lastIndexOf(File.separator)+1);

        if (progress != null)
            progress.setProgressPercentage(0.4);

        // filePathNoExt here is file stem, which will be used in XML log file names.
        // Cannot handle any directories from other machines.
        return context.toBEASTXML(filePathNoExt, chainLength, preBurnin);
    }

    /**TODO  merge it into run(...)
     * parse LPhy script into BEAST 2 XML.
     * @param lphy           LPhy script with <code>data{}<code/> <code>model{}<code/> blocks,
     *                       and one line one command.
     * @see #toBEASTXML(BufferedReader, String, long, int, Progress)
     */
    @Deprecated
    public String lphyToXML(String lphy, String fileNameStem, long chainLength, int preBurnin) throws IOException {
        Reader inputString = new StringReader(lphy);
        BufferedReader reader = new BufferedReader(inputString);

        return toBEASTXML(reader, fileNameStem, chainLength, preBurnin, null);
    }

    public Path getInPath() {
        return inPath;
    }

    public Path getOutPath() {
        return outPath;
    }

    public void setRep(int rep) {
        this.rep = rep;
    }

    public void setPreBurnin(int preBurnin) {
        this.preBurnin = preBurnin;
    }

    public void setChainLength(long chainLength) {
        this.chainLength = chainLength;
    }

    //    private static void source(BufferedReader reader, LPhyParser parser)
//            throws IOException {
//        LPhyParser.Context mode = null;
//
//        String line = reader.readLine();
//        while (line != null) {
//            String s = line.replaceAll("\\s+","");
//            if (s.isEmpty()) {
//                // skip empty lines
//            } else if (s.startsWith("data{"))
//                mode = LPhyParser.Context.data;
//            else if (s.startsWith("model{"))
//                mode = LPhyParser.Context.model;
//            else if (s.startsWith("}"))
//                mode = null; // reset
//            else {
//                if (mode == null)
//                    throw new IllegalArgumentException("Please use data{} to define data and " +
//                            "model{} to define models !\n" + line);
//
//                parser.parse(line, mode);
//            }
//            line = reader.readLine();
//        }
//        reader.close();
//    }


}

