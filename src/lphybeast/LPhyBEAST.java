package lphybeast;

import lphy.app.GraphicalLPhyParser;
import lphy.core.LPhyParser;
import lphy.core.Sampler;
import lphy.core.TreeFileLogger;
import lphy.core.VarFileLogger;
import lphy.graphicalModel.RandomValueLogger;
import lphy.parser.REPL;
import lphy.utils.LoggerUtils;
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

    public static final String VERSION = "0.0.2 alpha";

    @Parameters(paramLabel = "LPhy_scripts", description = "File of the LPhy model specification")
    Path infile;

    @Option(names = {"-V", "--version"}, versionHelp = true, description = "display version info")
    boolean versionInfoRequested;
    @Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
    boolean usageHelpRequested;

    @Option(names = {"-o", "--out"},     description = "BEAST 2 XML")  Path outfile;
    // not change the current directory
    @Option(names = {"-wd", "--workdir"}, description = "It will concatenate to the front of " +
            "the input and output path, which can be used for batch processing.") Path wd;

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
//        if (wd != null)
//            System.setProperty("user.dir", wd.toAbsolutePath().toString());

        String fileName = infile.getFileName().toString();
        if (fileName == null || !fileName.endsWith(".lphy"))
            throw new CommandLine.InitializationException("Invalid LPhy file: the postfix has to be '.lphy'");

        String fileNameStem = fileName.substring(0, fileName.lastIndexOf("."));

        // if outfile is given, it will prefer to extract the fileNameStem from outfile
        Path outfileParent = null;
        if (outfile != null) {
            fileName = outfile.getFileName().toString();
            fileNameStem = fileName.substring(0, fileName.lastIndexOf("."));
            outfileParent = outfile.getParent();
        }

        // assign user.dir
//        wd = Paths.get(System.getProperty("user.dir"));
        if (rep > 1) {
            // well-calibrated validations
            for (int i = 0; i < rep; i++) {
                // update fileNameStem and outfile
                final String repFileNameStem = fileNameStem + "_" + i;
                // need new reader
                createXML(infile, outfileParent, repFileNameStem, chainLength, preBurnin);
            }
        } else
            createXML(infile, outfileParent, fileNameStem, chainLength, preBurnin);

        return 0;
    }

    // inPath and outPath will base on System.getProperty("user.dir")
    // fileNameStem for both outfile and XML loggers
    private void createXML(Path infile, Path outfileParent, String fileNameStem, long chainLength, int preBurnin) throws CommandLine.PicocliException {
        // need to call reader each loop
        // if wd == null, assume infile can be absolute path
        Path inPath = wd == null ? infile: Paths.get(wd.toString(), infile.toString());

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(inPath.toFile()));
        } catch (FileNotFoundException e) {
            throw new CommandLine.PicocliException("Fail to read LPhy scripts from " +
                    inPath.toString() + (wd == null ? "" : ", working path is " + wd.toString()), e);
        }
        String xml = toBEASTXML(Objects.requireNonNull(reader), fileNameStem, chainLength, preBurnin);

        // use fileNameStem to recover outfile
        String outFile = fileNameStem + ".xml";
        // add parent if outfile Path has
        if (outfileParent != null)
            outFile = Paths.get(outfileParent.toString(), outFile).toString();

        // if wd == null, assume infile can be absolute path
        Path outPath = wd == null ? Paths.get(outFile) : Paths.get(wd.toString(), outFile);
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(outPath.toFile()));
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
     * @param fileNameStem
     * @param chainLength    if <=0, then use default 1,000,000.
     *                       logEvery = chainLength / numOfSamples,
     *                       where numOfSamples = 2000 as default.
     * @param preBurnin      preBurnin for BEAST MCMC, default to 0.
     * @return    BEAST 2 XML
     * @see BEASTContext#toBEASTXML(String, long, int)
     * @throws IOException
     */
    public String toBEASTXML(BufferedReader reader, String fileNameStem, long chainLength, int preBurnin) throws CommandLine.PicocliException {
        //*** Parse LPhy file ***//
        LPhyParser parser = new REPL();
        try {
            parser.source(reader);
        } catch (IOException e) {
            throw new CommandLine.PicocliException("Cannot parse LPhy scripts in " +
                    fileNameStem + ".lphy", e);
        }

        // log true values and tree
        List<RandomValueLogger> loggers = new ArrayList<>();
        final String fileNameStemTrueVaule = fileNameStem + "_" + "true";
        loggers.add(new VarFileLogger(fileNameStemTrueVaule, true, true));
        loggers.add(new TreeFileLogger(fileNameStemTrueVaule));

        GraphicalLPhyParser gparser = new GraphicalLPhyParser(parser);
        Sampler sampler = new Sampler(gparser);
        sampler.sample(1, loggers);

        // register parser
        BEASTContext context = new BEASTContext(parser);

        //*** Write BEAST 2 XML ***//
        // avoid to add dir into fileNameStem passed into XML logger
        return context.toBEASTXML(fileNameStem, chainLength, preBurnin);
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
