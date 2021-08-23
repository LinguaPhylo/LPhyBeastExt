package lphybeast;

import lphy.core.*;
import lphy.graphicalModel.RandomValueLogger;
import lphy.parser.REPL;
import lphy.util.IOUtils;
import lphy.util.LoggerUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

@Command(name = "lphybeast", footer = "Copyright(c) 2020",
        description = "LPhyBEAST takes an LPhy model specification, and some data and produces a BEAST 2 XML file.",
        version = { "LPhyBEAST " + LPhyBEAST.VERSION,
        "JVM: ${java.version} (${java.vendor} ${java.vm.name} ${java.vm.version})",
        "OS: ${os.name} ${os.version} ${os.arch}"})
public class LPhyBEAST implements Callable<Integer> {

    public static final String VERSION = "0.1.0";

    @Parameters(paramLabel = "LPhy_scripts", description = "File of the LPhy model specification")
    Path infile;

    @Option(names = {"-V", "--version"}, versionHelp = true, description = "display version info")
    boolean versionInfoRequested;
    @Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
    boolean usageHelpRequested;

    @Option(names = {"-o", "--out"},     description = "BEAST 2 XML")  Path outfile;
    // not change the current directory
    @Option(names = {"-wd", "--workdir"}, description = "set 'user.dir' and concatenate to the front of " +
            "the input and output path (if the relative path is provided), which can be used for batch processing. " +
            "If either LPhy script path or output is a absolute path, then this option will be ignored.") Path wd;

    //MCMC
    @Option(names = {"-l", "--chainLength"}, defaultValue = "-1", description = "define the total chain length of MCMC, default to 1 million.")
    int chainLength;
    @Option(names = {"-b", "--preBurnin"}, defaultValue = "0", description = "define the number of burn in samples taken before entering the main loop of MCMC")
    int preBurnin;

    //well calibrated study
    @Option(names = {"-r", "--replicates"}, defaultValue = "1", description = "the number of replicates (XML) given one LPhy script, " +
            "usually to create simulations for well-calibrated study.") int rep;

//    @Option(names = {"-n", "--nex"},    description = "BEAST 2 partitions defined in Nexus file")
//    Path nexfile;
//    Map<String, String> partitionMap; // d1=primates.nex:noncoding|d2=primates.nex:coding   //
//    @Option(names = {"-m", "--mapping"}, description = "mapping file") Path mapfile;
//    @Option(names = {"-p", "--partition"}, split = "\\|", splitSynopsisLabel = "|",
//            description = "LPhy var <=> Nexus keyword")


    public static void main(String[] args) throws IOException {

        int exitCode = new CommandLine(new LPhyBEAST()).execute(args);

        if (exitCode != 0)
            LoggerUtils.log.severe("LPhyBEAST does not exit normally !");
        System.exit(exitCode);

    }


    @Override
    public Integer call() throws CommandLine.PicocliException { // business logic goes here...

        String fileName = infile.getFileName().toString();
        if (fileName == null || !fileName.endsWith(".lphy"))
            throw new CommandLine.InitializationException("Invalid LPhy file: the postfix has to be '.lphy'");

        if (wd != null)
            IOUtils.setUserDir(wd.toAbsolutePath().toString());
        // if the relative path, then concatenate user.dir before it
        final Path inPath = IOUtils.getUserPath(infile);
        // if no -wd, set user.dir to the folder containing infile
        if (wd == null)
            IOUtils.setUserDir(inPath.getParent().toString());

        Path outPath = null;
        if (outfile != null) {
            outPath = IOUtils.getUserPath(outfile);
        } else {
            if (wd != null) {
                String infileNoExt = fileName.substring(0, fileName.lastIndexOf("."));
                // add wd before file stem
                outPath = Paths.get(wd.toAbsolutePath().toString(), infileNoExt + ".xml");
            } else {
                // use input file stem with file path
                String infilePathNoExt = inPath.toString().substring(0, inPath.toString().lastIndexOf("."));
                outPath = Paths.get(infilePathNoExt + ".xml");
            }
        }

        // add rep after file stem
        if (rep > 1) {
            final String outPathNoExt = outPath.toString().substring(0, outPath.toString().lastIndexOf("."));
            // well-calibrated validations
            for (int i = 0; i < rep; i++) {
                // update outPath to add i
                outPath = Paths.get(outPathNoExt + "_" + i + ".xml");
                // need new reader
                createXML(inPath, outPath, chainLength, preBurnin);
            }
        } else // normal output
            createXML(inPath, outPath, chainLength, preBurnin);

        return 0;
    }

    // the relative path given in readNexus in a script always refers to user.dir
    // fileNameStem for both outfile and XML loggers
    private void createXML(Path inPath, Path outPath, long chainLength, int preBurnin) throws CommandLine.PicocliException {

        // need to call reader each loop
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(inPath.toFile()));

        } catch (FileNotFoundException e) {
            throw new CommandLine.PicocliException("Fail to read LPhy scripts from " +
                    inPath.toString() + ", user.dir = " + System.getProperty(IOUtils.USER_DIR), e);
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
            throw new CommandLine.PicocliException("Fail to write XML to " +
                    outfile.toString(), e);
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
    public String toBEASTXML(BufferedReader reader, String filePathNoExt, long chainLength, int preBurnin) throws CommandLine.PicocliException {
        //*** Parse LPhy file ***//
        LPhyParser parser = new REPL();
        try {
            parser.source(reader);
        } catch (IOException e) {
            throw new CommandLine.PicocliException("Cannot parse LPhy scripts in " +
                    filePathNoExt + ".lphy", e);
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
     * @throws IOException
     */
    public String lphyToXML (String lphy, String fileNameStem, long chainLength, int preBurnin) throws CommandLine.PicocliException {
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
