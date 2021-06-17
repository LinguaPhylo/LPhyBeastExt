package lphybeast.registry;

import beast.evolution.datatype.DataType;
import beast.util.BEASTClassLoader;
import jebl.evolution.sequences.SequenceType;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Implement this interface to register classes converting LPhy into BEAST,
 * which should include {@link lphybeast.ValueToBEAST}, {@link lphybeast.GeneratorToBEAST},
 * and {@link DataType}.
 *
 * @author Walter Xie
 */
public interface ClassesRegistry {

    Class<?>[] getValuesToBEASTs();

    Class<?>[] getGeneratorToBEASTs();

    Map<SequenceType, DataType> getDataTypeMap();

    static String[] getAllClassPathEntries() {
        return System.getProperty("java.class.path").split(
                System.getProperty("path.separator")
        );
    }

    /**
     * Derived from //https://stackoverflow.com/questions/28678026/how-can-i-get-all-class-files-in-a-specific-package-in-java
     *
     * @return all {@link ClassesRegistry} child classes from "java.class.path",
     * containing registration information for LPhyBEAST.
     */
    static List<ClassesRegistry> getRegistryClasses() {

        List<ClassesRegistry> registryList = new ArrayList<>();
        String[] classPathEntries = getAllClassPathEntries();

        String name;
        for (String classpathEntry : classPathEntries) {
            if (classpathEntry.toLowerCase().endsWith(".jar")) {
                // this is for released jars
                File jar = new File(classpathEntry);
                try {
                    JarInputStream is = new JarInputStream(new FileInputStream(jar));
                    JarEntry entry;
                    while ((entry = is.getNextJarEntry()) != null) {
                        name = entry.getName();

                        addRegistryClass(name, registryList);
                    }
                } catch (Exception ex) {
                    // Silence is gold
                }
            } else {
                // this is for development scenarios
                try {
                    final String prefix = classpathEntry + File.separatorChar;
                    final Path path = Paths.get(prefix);

                    // find all classes under the "prefix" folder, maxDepth to 10
                    File[] files = path.toFile().listFiles();
                    if (files != null) {
                        for (File file : files) {
                            addRegistryClass(prefix, file, registryList);
                            //TODO assuming one RegistryClass each package? then if (added) break;
                        }
                    }
                } catch (Exception ex) {
                    // Silence is gold
                }
            }
        }// end for
        return registryList;
    }

    // recursively
    private static void addRegistryClass(String prefix, File file, List<ClassesRegistry> registryList) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    addRegistryClass(prefix, child, registryList);
                }
            }
        } else  {
            String filePath = file.getAbsolutePath();
            if (filePath.toLowerCase().endsWith(".class")) {
                // rm the classpath entry from filePath, so only java package left in filePath
                filePath = filePath.replaceFirst(prefix, "");
                addRegistryClass(filePath, registryList);
            }
        }
    }

    // add ClassesRegistry child classes to registryList
    private static void addRegistryClass(String name, List<ClassesRegistry> registryList) {
        String classPath;
        // rm .class from name
        classPath = name.substring(0, name.length() - 6);
        classPath = classPath.replaceAll("[\\|/]", ".");

        try {
            // if use Class.forName, it will have many Error thrown
            Class<?> cls = BEASTClassLoader.forName(classPath);
            if ( ClassesRegistry.class.isAssignableFrom(cls) && !cls.equals(ClassesRegistry.class)) {
                // https://docs.oracle.com/javase/9/docs/api/java/lang/Class.html#newInstance--
                Object obj = cls.getDeclaredConstructor().newInstance();
                registryList.add((ClassesRegistry) obj);
            }
        } catch (ClassNotFoundException | InvocationTargetException |
                InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            // do nothing
        }
//        catch (Throwable e) { return false; }
    }


//        Package[] packages = Package.getPackages();
//        List<Package> packageList = new ArrayList<>();
//        for (Package pkg : packages) {
//            String name = pkg.getName().toLowerCase();//.replaceAll("\\.", "-");
//            if (name.contains("registry"))
//                packageList.add(pkg);
//        }


}
