package lphybeast.app;

import beast.app.beastapp.BeastLauncher;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Walter Xie
 */
public class LPhyBEASTLauncher {


    public static void main(String[] args) {

        boolean useStrictVersions = false;
        for (String arg : args) {
            if (arg.equals("-strictversions")) {
                useStrictVersions = true;
            }
        }
        String classpath = null;
        try {
            classpath = BeastLauncher.getPath(useStrictVersions, args.length > 0 ? args[args.length - 1] : null);
        } catch (NoSuchMethodException | ClassNotFoundException | IllegalAccessException |
                InvocationTargetException | IOException e) {
            e.printStackTrace();
        }
        System.out.println("BeastLauncher classpath = " + classpath);

        BeastLauncher.run(classpath, "lphybeast.LPhyBEAST", args);

    }

}
