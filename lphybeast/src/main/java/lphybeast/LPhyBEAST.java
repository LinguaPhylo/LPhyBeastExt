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
        description = "LPhyBEAST takes an LPhy model specification and some data, " +
                "and produces a BEAST 2 XML file. The installation and usage is available at " +
                "https://linguaphylo.github.io/setup/",
        version = { "LPhyBEAST " + LPhyBEAST.VERSION,
                "Local JVM: ${java.version} (${java.vendor} ${java.vm.name} ${java.vm.version})",
                "OS: ${os.name} ${os.version} ${os.arch}"})
public class LPhyBEAST implements Callable<Integer> {

    public static final String VERSION = "0.2.1";

    @Parameters(paramLabel = "LPhy_scripts", description = "File of the LPhy model specification. " +
            "If it is a relative path, then concatenate 'user.dir' to the front of the path. " +
            "But if `-wd` is NOT given, the 'user.dir' will set to the path where the LPhy script is.")
    Path infile;

    @Option(names = {"-V", "--version"}, versionHelp = true, description = "display version info")
    boolean versionInfoRequested;
    @Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
    boolean usageHelpRequested;

    @Option(names = {"-o", "--out"},     description = "BEAST 2 XML. " +
            "If it contains relative path, then concatenate 'user.dir' to the front of the path.")
    Path outfile;
    // 'user.dir' is default to the current directory
    @Option(names = {"-wd", "--workdir"}, description = "Set 'user.dir' " +
            "and concatenate it to the front of the input and output path " +
            "(if the relative path is provided), which can be used for batch processing.")
    Path wd;

    //MCMC
    @Option(names = {"-l", "--chainLength"}, defaultValue = "-1", description = "define the total chain length of MCMC, default to 1 million.")
    int chainLength;
    @Option(names = {"-b", "--preBurnin"}, defaultValue = "0", description = "define the number of burn in samples taken before entering the main loop of MCMC")
    int preBurnin;

    //well calibrated study
    @Option(names = {"-r", "--replicates"}, defaultValue = "1", description = "the number of replicates (XML) given one LPhy script, " +
            "usually to create simulations for well-calibrated study.") int rep;


    public static void main(String[] args) {

        int exitCode = new CommandLine(new LPhyBEAST()).execute(args);

        if (exitCode != 0)
            LoggerUtils.log.severe("LPhyBEAST does not exit normally !");
        System.exit(exitCode);

    }


    /**
     * 1. If the input/output is a relative path, then concatenate 'user.dir'
     * to the front of the path.
     * 2. Use '-wd' to set 'user.dir'. But if `-wd` is NOT given,
     * the 'user.dir' will be set to the path where the LPhy script is.
     * @throws CommandLine.PicocliException
     */
    @Override
    public Integer call() throws CommandLine.PicocliException { // business logic goes here...

//        if (versionInfoRequested) CommandLine.usage(this, System.out);

        String fileName = infile.getFileName().toString();
        if (fileName == null || !fileName.endsWith(".lphy"))
            throw new CommandLine.InitializationException("Invalid LPhy file: the postfix has to be '.lphy'");

        if (wd != null)
            IOUtils.setUserDir(wd.toAbsolutePath().toString());
        // if the relative path, then concatenate user.dir before it
        final Path inPath = IOUtils.getUserPath(infile);
        // still need to set user.dir, if no -wd, in case LPhy script uses relative path
        if (wd == null)
            // set user.dir to the folder containing lphy script
            IOUtils.setUserDir(inPath.getParent().toString());

        Path outPath;
        if (outfile != null) {
            outPath = IOUtils.getUserPath(outfile);
        } else {
            String infileNoExt = fileName.substring(0, fileName.lastIndexOf("."));
            // add wd before file stem
            outPath = Paths.get(IOUtils.getUserDir().toString(), infileNoExt + ".xml");
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
    private String toBEASTXML(BufferedReader reader, String filePathNoExt, long chainLength, int preBurnin) throws CommandLine.PicocliException {
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
     * @throws IOException
     */
    public String lphyToXML (String lphy, String fileNameStem, long chainLength, int preBurnin) throws CommandLine.PicocliException {
        Reader inputString = new StringReader(lphy);
        BufferedReader reader = new BufferedReader(inputString);

        return toBEASTXML(reader, fileNameStem, chainLength, preBurnin);
    }

    /**TODO not working
     * This function is modified from picocli demo {@code VersionProviderDemo2}.
     * {@link CommandLine.IVersionProvider} implementation that returns version information
     * from the lphybeast-x.x.x.jar file's {@code /META-INF/MANIFEST.MF} file.
    static class ManifestVersionProvider implements CommandLine.IVersionProvider {
        public String[] getVersion() throws Exception {
            Enumeration<URL> resources = LPhyBEAST.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try {
                    Manifest manifest = new Manifest(url.openStream());
                    if (isApplicableManifest(manifest)) {
                        Attributes attr = manifest.getMainAttributes();
                        return new String[] { get(attr, "Implementation-Title") + " version \"" +
                                get(attr, "Implementation-Version") + "\"" };
                    }
                } catch (IOException ex) {
                    return new String[] { "Unable to read from " + url + ": " + ex };
                }
            }
            return new String[0];
        }

        private boolean isApplicableManifest(Manifest manifest) {
            return true;
//            Attributes attributes = manifest.getMainAttributes();
//            return "LPhyBEAST".equalsIgnoreCase(get(attributes, "Implementation-Title").toString());
        }

        // no null, so .toString is safe
        private static Object get(Attributes attributes, String key) {
            Object o = attributes.get(new Attributes.Name(key));
            if (o != null) return o;
            return "";
        }
    }
     */


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
