package lphybeast.registry;

import beast.evolution.datatype.DataType;
import jebl.evolution.sequences.SequenceType;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Implement this interface to register classes converting LPhy into BEAST,
 * which include {@link lphybeast.ValueToBEAST}, {@link lphybeast.GeneratorToBEAST}, {@link DataType}.
 *
 * @author Walter Xie
 */
public interface ClassesRegistry {

    Class[] getValuesToBEASTs();

    Class[] getGeneratorToBEASTs();

    Map<SequenceType, DataType> getDataTypeMap();

    static List<ClassesRegistry> getRegistryClasses(String packageName) throws InstantiationException, IllegalAccessException {
        List<Class<?>> classesList = ClassesRegistry.getClassesInPackage(packageName);
        List<ClassesRegistry> registryList = new ArrayList<>();
        for (Class<?> cls : classesList) {
            if ( ClassesRegistry.class.isAssignableFrom(cls) && !cls.equals(ClassesRegistry.class)) {
                Object obj = cls.newInstance();
                registryList.add((ClassesRegistry) obj);
            }
        }
        return registryList;
    }

    static String[] getAllClassPathEntries(){
        return System.getProperty("java.class.path").split(
                System.getProperty("path.separator")
        );
    }

    //https://stackoverflow.com/questions/28678026/how-can-i-get-all-class-files-in-a-specific-package-in-java
    /**
     * @param packageName  a specific package.
     * @return   all {@link Class} files from "java.class.path".
     */
    static List<Class<?>> getClassesInPackage(String packageName) {
        String path = packageName.replaceAll("\\.", File.separator);
        List<Class<?>> classes = new ArrayList<>();
        String[] classPathEntries = System.getProperty("java.class.path").split(
                System.getProperty("path.separator")
        );

        String name;
        for (String classpathEntry : classPathEntries) {
            if (classpathEntry.endsWith(".jar")) {
                File jar = new File(classpathEntry);
                try {
                    JarInputStream is = new JarInputStream(new FileInputStream(jar));
                    JarEntry entry;
                    while((entry = is.getNextJarEntry()) != null) {
                        name = entry.getName();
                        if (name.endsWith(".class")) {
                            if (name.contains(path) && name.endsWith(".class")) {
                                String classPath = name.substring(0, entry.getName().length() - 6);
                                classPath = classPath.replaceAll("[\\|/]", ".");
                                classes.add(Class.forName(classPath));
                            }
                        }
                    }
                } catch (Exception ex) {
                    // Silence is gold
                }
            } else {
                try {
                    // find all classes under the given "path" folder
                    File base = new File(classpathEntry + File.separatorChar + path);
                    for (File file : base.listFiles()) {
                        name = file.getName();
                        if (name.endsWith(".class")) {
                            name = name.substring(0, name.length() - 6);
                            classes.add(Class.forName(packageName + "." + name));
                        }
                    }
                } catch (Exception ex) {
                    // Silence is gold
                }
            }
        }

        return classes;
    }

}
