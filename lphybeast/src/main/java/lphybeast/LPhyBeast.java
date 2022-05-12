package lphybeast;

import lphy.core.*;
import lphy.graphicalModel.RandomValueLogger;
import lphy.parser.REPL;
import lphy.system.UserDir;
import picocli.CommandLine;

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
public class LPhyBeast {

    private final Path inPath, outPath;
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

    }

    /**
     * Unit test
     */
    public LPhyBeast() {
        inPath = null;
        outPath = null;
    }

    public void setRep(int rep) {
        this.rep = rep;
    }

    public void run(long chainLength, int preBurnin) {
        // add rep after file stem
        if (rep > 1) {
            for (int i = 0; i < rep; i++) {
                final String outPathNoExt = outPath.toString().substring(0, outPath.toString().lastIndexOf("."));
                // well-calibrated validations
                // update outPath to add i
                Path outPathPerRep = Paths.get(outPathNoExt + "_" + i + ".xml");
                // need new reader
                createXML(inPath, outPathPerRep, chainLength, preBurnin);
            }
        } else // normal output
            createXML(inPath, outPath, chainLength, preBurnin);
    }

    // the relative path given in readNexus in a script always refers to user.dir
    // fileNameStem for both outfile and XML loggers
    private void createXML(Path inPath, Path outPath, long chainLength, int preBurnin) {

        // need to call reader each loop
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(inPath.toFile()));

        } catch (FileNotFoundException e) {
            throw new CommandLine.PicocliException("Fail to read LPhy scripts from " +
                    inPath + ", user.dir = " + System.getProperty(UserDir.USER_DIR), e);
        }
        String path = outPath.toString();
        String pathNoExt = path.substring(0, path.lastIndexOf("."));
        String xml = toBEASTXML(Objects.requireNonNull(reader), pathNoExt, chainLength, preBurnin);

        try {
            PrintWriter writer = new PrintWriter(new FileWriter(Objects.requireNonNull(outPath).toFile()));
            writer.println(xml);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new CommandLine.PicocliException("Fail to write XML to " + outPath, e);
        }

        System.out.println("\nInput LPhy : " + inPath.toAbsolutePath());
        System.out.println("Create BEAST 2 XML : " + outPath.toAbsolutePath());
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
    private String toBEASTXML(BufferedReader reader, String filePathNoExt, long chainLength, int preBurnin) {
        //*** Parse LPhy file ***//
        LPhyParser parser = new REPL();
        try {
            parser.source(reader);
        } catch (IOException e) {
            throw new CommandLine.PicocliException("Cannot parse LPhy scripts in " +
                    filePathNoExt + ".lphy", e);
        } catch (ExceptionInInitializerError e) {
            throw new CommandLine.PicocliException("An unexpected exception " +
                    "from a static initializer : ", e);
        }

        // log true values and tree
        List<RandomValueLogger> loggers = new ArrayList<>();
        final String filePathNoExtTrueVaule = filePathNoExt + "_" + "true";
        loggers.add(new VarFileLogger(filePathNoExtTrueVaule, true, true));
        loggers.add(new TreeFileLogger(filePathNoExtTrueVaule));

        GraphicalLPhyParser gparser = new GraphicalLPhyParser(parser);
        Sampler sampler = new Sampler(gparser);
        sampler.sample(1, loggers);

        // register parser
        BEASTContext context = new BEASTContext(parser);

        //*** Write BEAST 2 XML ***//
        // remove any dir in filePathNoExt here
        if (filePathNoExt.contains(File.separator))
            filePathNoExt = filePathNoExt.substring(filePathNoExt.lastIndexOf(File.separator)+1);
        // filePathNoExt here is file stem, which will be used in XML log file names.
        // Cannot handle any directories from other machines.
        return context.toBEASTXML(filePathNoExt, chainLength, preBurnin);
    }

    /**
     * parse LPhy script into BEAST 2 XML.
     * @param lphy           LPhy script with <code>data{}<code/> <code>model{}<code/> blocks,
     *                       and one line one command.
     * @see #toBEASTXML(BufferedReader, String, long, int)
     */
    public String lphyToXML (String lphy, String fileNameStem, long chainLength, int preBurnin) {
        Reader inputString = new StringReader(lphy);
        BufferedReader reader = new BufferedReader(inputString);

        return toBEASTXML(reader, fileNameStem, chainLength, preBurnin);
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

