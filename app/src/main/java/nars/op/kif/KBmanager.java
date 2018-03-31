package nars.op.kif;

/**
 * This code is copyright Articulate Software (c) 2003. Some portions copyright
 * Teknowledge (c) 2003 and reused under the terms of the GNU license. This
 * software is released under the GNU Public License
 * <http://www.gnu.org/copyleft/gpl.html>. Users of this code also consent, by
 * use of this code, to credit Articulate Software and Teknowledge in any
 * writings, briefings, publications, presentations, or other representations of
 * any software which incorporates, builds on, or uses this code. Please cite
 * the following article in any publication with references:
 *
 * Pease, A., (2003). The Sigma Ontology Development Environment, in Working
 * Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems, August
 * 9, Acapulco, Mexico.
 */
import java.io.*;
import java.util.*;

/** This is a class that manages a group of knowledge bases.  It should only
 *  have one instance, contained in its own static member variable.
 */
public class KBmanager implements Serializable {

    /** A numeric (bitwise) constant used to signal whether type
     * prefixes (sortals) should be added during formula
     * preprocessing.
     */
    public static final int USE_TYPE_PREFIX  = 1;

    /** A numeric (bitwise) constant used to signal whether holds
     * prefixes should be added during formula preprocessing.
     */
    public static final int USE_HOLDS_PREFIX = 2;

    /** A numeric (bitwise) constant used to signal whether the closure
     * of instance and subclass relastions should be "cached out" for
     * use by the inference engine.
     */
    public static final int USE_CACHE        = 4;

    /*** A numeric (bitwise) constant used to signal whether formulas
     * should be translated to TPTP format during the processing of KB
     * constituent files.
     */
    public static final int USE_TPTP         = 8;
//    private static CCheckManager ccheckManager = new CCheckManager();

    private static KBmanager manager = new KBmanager();
    protected static final String CONFIG_FILE = "config.xml";

    private HashMap<String,String> preferences = new HashMap<String,String>();
    public HashMap<String,KB> kbs = new HashMap<String,KB>();
    public static boolean initialized = false;
    public static boolean initializing = false;
    public static boolean debug = false;
    private int oldInferenceBitValue = -1;
    private String error = "";

    public static final List<String> configKeys =
            Arrays.asList("sumokbname", "testOutputDir", "TPTPDisplay", "semRewrite",
                    "inferenceEngine", "inferenceTestDir", "baseDir", "hostname",
                    "logLevel", "systemsDir", "dbUser", "loadFresh", "userBrowserLimit",
                    "adminBrowserLimit", "https", "graphWidth", "overwrite", "typePrefix",
                    "graphDir", "nlpTools","TPTP","cache","editorCommand","graphVizDir",
                    "kbDir","loadCELT","celtdir","lineNumberCommand","prolog","port",
                    "tptpHomeDir","showcached","leoExecutable","holdsPrefix","logDir",
                    "englishPCFG");

    public static final List<String> fileKeys =
            Arrays.asList("testOutputDir", "inferenceEngine", "inferenceTestDir", "baseDir",
                    "systemsDir","graphVizDir", "kbDir", "celtdir", "tptpHomeDir", "logDir",
                    "englishPCFG");

    /** ***************************************************************
     */
    public KBmanager() {
    }

    /** ***************************************************************
     * Set an error string for file loading.
     */
    public void setError(String er) {
        error = er;
    }

    /** ***************************************************************
     * Get the error string for file loading.
     */
    public String getError() {
        return error;
    }

    /** ***************************************************************
     *  Check whether sources are newer than serialized version.
     */
    public static boolean serializedExists() {

        String kbDir = System.getenv("SIGMA_HOME") + File.separator + "KBs";
        File serfile = new File(kbDir + File.separator + "kbmanager.ser");
        System.out.println("KBmanager.serializedExists(): " + serfile.exists());
        return serfile.exists();
    }

//    /** ***************************************************************
//     *  Check whether sources are newer than serialized version.
//     */
//    public static boolean serializedOld(SimpleElement configuration) {
//
//        System.out.println("KBmanager.serializedOld(config): ");
//        String kbDir = System.getenv("SIGMA_HOME") + File.separator + "KBs";
//        File configFile = new File(kbDir + File.separator + "config.xml");
//        Date configDate = new Date(configFile.lastModified());
//        File serfile = new File(kbDir + File.separator + "kbmanager.ser");
//        Date saveDate = new Date(serfile.lastModified());
//        System.out.println("KBmanager.serializedOld(config): save date: " + saveDate.toString());
//        if (saveDate.compareTo(configDate) < 0)
//            return true;
//        ArrayList<ArrayList<String>> kbFilenames = kbFilenamesFromXML(configuration);
//        for (ArrayList<String> thekb : kbFilenames) { // iterate through the kbs
//            for (String f : thekb) { // iterate through the constituents
//                File file = new File(f);
//                Date fileDate = new Date(file.lastModified());
//                if (saveDate.compareTo(fileDate) < 0) {
//                    return true;
//                }
//            }
//        }
//        System.out.println("KBmanager.serializedOld(config): returning false (not old)");
//        return false;
//    }
//
//    /** ***************************************************************
//     *  Check whether sources are newer than serialized version.
//     */
//    public static boolean serializedOld() {
//
//        System.out.println("KBmanager.serializedOld(): ");
//        String kbDir = System.getenv("SIGMA_HOME") + File.separator + "KBs";
//        File configFile = new File(kbDir + File.separator + "config.xml");
//        Date configDate = new Date(configFile.lastModified());
//        File serfile = new File(kbDir + File.separator + "kbmanager.ser");
//        Date saveDate = new Date(serfile.lastModified());
//        System.out.println("KBmanager.serializedOld(): save date: " + saveDate.toString());
//        if (saveDate.compareTo(configDate) < 0)
//            return true;
//        for (KB thekb : manager.kbs.values()) {
//            for (String f : thekb.constituents) {
//                File file = new File(f);
//                Date fileDate = new Date(file.lastModified());
//                if (saveDate.compareTo(fileDate) < 0) {
//                    return true;
//                }
//            }
//        }
//        System.out.println("KBmanager.serializedOld(): returning false");
//        return false;
//    }
//
//    /** ***************************************************************
//     *  Load the most recently save serialized version.
//     */
//    public static boolean loadSerialized() {
//
//        manager = null;
//        try {
//            // Reading the object from a file
//            String kbDir = System.getenv("SIGMA_HOME") + File.separator + "KBs";
//            FileInputStream file = new FileInputStream(kbDir + File.separator + "kbmanager.ser");
//            ObjectInputStream in = new ObjectInputStream(file);
//            // Method for deserialization of object
//            KBmanager temp = (KBmanager) in.readObject();
//            //if (serializedOld()) {
//            //    System.out.println("KBmanager.loadSerialized(): serialized file is older than sources, " +
//            //            "reloading from sources.");
//            //    return false;
//            //}
//            manager = temp;
//            in.close();
//            file.close();
//            System.out.println("KBmanager.loadSerialized(): KBmanager has been deserialized ");
//            initialized = true;
//        }
//        catch (Exception ex) {
//            System.out.println("Error in KBmanager.loadSerialized(): IOException is caught");
//            ex.printStackTrace();
//            return false;
//        }
//        return true;
//    }
//
//    /** ***************************************************************
//     *  save serialized version.
//     */
//    public static void serialize() {
//
//        try {
//            // Reading the object from a file
//            String kbDir = System.getenv("SIGMA_HOME") + File.separator + "KBs";
//            FileOutputStream file = new FileOutputStream(kbDir + File.separator + "kbmanager.ser");
//            ObjectOutputStream out = new ObjectOutputStream(file);
//            // Method for deserialization of object
//            out.writeObject(manager);
//            out.close();
//            file.close();
//            System.out.println("KBmanager.serialize(): KBmanager has been serialized ");
//        }
//        catch (IOException ex) {
//            System.out.println("Error in KBmanager.serialize(): IOException is caught");
//            ex.printStackTrace();
//        }
//    }

    /** ***************************************************************
     * Set default attribute values if not in the configuration file.
     */
    public void setDefaultAttributes() {

        try {
            String sep = File.separator;
            String base = System.getenv("SIGMA_HOME");
            String tptpHome = System.getenv("TPTP_HOME");
            String systemsHome = System.getenv("SYSTEMS_HOME");
            if (StringUtil.emptyString(base))
                base = System.getProperty("user.dir");
            if (StringUtil.emptyString(tptpHome))
                tptpHome = System.getProperty("user.dir");
            if (StringUtil.emptyString(systemsHome))
                systemsHome = System.getProperty("user.dir");
            String tomcatRoot = System.getenv("CATALINA_HOME");
            if (StringUtil.emptyString(tomcatRoot))
                tomcatRoot = System.getProperty("user.dir");
            File tomcatRootDir = new File(tomcatRoot);
            File baseDir = new File(base);
            File tptpHomeDir = new File(tptpHome);
            File systemsDir = new File(systemsHome);
            File kbDir = new File(baseDir, "KBs");
            File inferenceTestDir = new File(kbDir, "tests");
            File logDir = new File(baseDir, "logs");
            logDir.mkdirs();

            // The links for the test results files will be broken if
            // they are not put under [Tomcat]/webapps/sigma.
            // Unfortunately, we don't know where [Tomcat] is.
            File testOutputDir = new File(tomcatRootDir,
                    ("webapps" + sep + "sigma" + sep + "tests"));
            preferences.put("baseDir",baseDir.getCanonicalPath());
            preferences.put("tptpHomeDir",tptpHomeDir.getCanonicalPath());
            preferences.put("systemsDir",systemsDir.getCanonicalPath());
            preferences.put("kbDir",kbDir.getCanonicalPath());
            preferences.put("inferenceTestDir",inferenceTestDir.getCanonicalPath());
            preferences.put("testOutputDir",testOutputDir.getCanonicalPath());

            File graphVizDir = new File("/usr/bin");
            preferences.put("graphVizDir", graphVizDir.getCanonicalPath());

            File graphDir = new File(tomcatRootDir, "webapps" + sep + "sigma" + sep + "graph");
            if (!graphDir.exists())
                graphDir.mkdir();
            preferences.put("graphDir", graphDir.getCanonicalPath());

            // There is no foolproof way to determine the actual
            // inferenceEngine path without asking the user.  But we
            // can make an educated guess.
            String _OS = System.getProperty("os.name");
            String ieExec = "e_ltb_runner";
            if (StringUtil.isNonEmptyString(_OS) && _OS.matches("(?i).*win.*"))
                ieExec = "e_ltb_runner.exe";
            File ieDirFile = new File(baseDir, "inference");
            File ieExecFile = (ieDirFile.isDirectory()
                    ? new File(ieDirFile, ieExec)
                    : new File(ieExec));
            String leoExec = "leo";
            File leoExecFile = (ieDirFile.isDirectory()
                    ? new File(ieDirFile, leoExec)
                    : new File(leoExec));
            preferences.put("inferenceEngine",ieExecFile.getCanonicalPath());
            preferences.put("leoExecutable",leoExecFile.getCanonicalPath());
            preferences.put("loadCELT","no");
            preferences.put("showcached","yes");
            preferences.put("typePrefix","no");

            // If no then instantiate variables in predicate position.
            preferences.put("holdsPrefix","no");
            preferences.put("cache","yes");
            preferences.put("TPTP","yes");
            preferences.put("TPTPDisplay","no");
            preferences.put("userBrowserLimit","25");
            preferences.put("adminBrowserLimit","200");
            preferences.put("port","8080");
            preferences.put("hostname","localhost");
            preferences.put("https","false");
            preferences.put("sumokbname","SUMO");

            // Default logging things
            preferences.put("logDir", logDir.getCanonicalPath());
            preferences.put("logLevel", "warning");

        }
        catch (Exception ex) {
            System.out.println("Error in KBmanager.setDefaultAttributes(): " + Arrays.toString(ex.getStackTrace()));
            ex.printStackTrace();
        }
        return;
    }

//    /** ***************************************************************
//     */
//    public static CCheckStatus initiateCCheck(KB kb, String chosenEngine, String systemChosen, String location,
//                                              String language, int timeout) {
//
//        return ccheckManager.performConsistencyCheck(kb, chosenEngine, systemChosen, location, language, timeout);
//    }
//
//    public static String ccheckResults(String kbName) {
//        return ccheckManager.ccheckResults(kbName);
//    }
//
//    public static CCheckStatus ccheckStatus(String kbName) {
//        return ccheckManager.ccheckStatus(kbName);
//        //return HTMLformatter.formatConsistencyCheck(msg, ccheckManager.ccheckResults(kb.name), language, page);
//    }
//
//    /** ***************************************************************
//     */
//    private void preferencesFromXML(SimpleElement configuration) {
//
//        if (!configuration.getTagName().equals("configuration"))
//            System.out.println("Error in KBmanager.fromXML(): Bad tag: " + configuration.getTagName());
//        else {
//            for (int i = 0; i < configuration.getChildElements().size(); i++) {
//                SimpleElement element = (SimpleElement) configuration.getChildElements().get(i);
//                if (element.getTagName().equals("preference")) {
//                    String name = (String) element.getAttribute("name");
//                    String value = (String) element.getAttribute("value");
//                    preferences.put(name,value);
//                }
//            }
//        }
//        if (debug) System.out.println("KBmanager.preferencesFromXML(): number of preferences: " +
//                preferences.keySet().size());
//    }
//
//    /** ***************************************************************
//     * Note that filenames that are not full paths are prefixed with the
//     * value of preference kbDir
//     */
//    private static void kbsFromXML(SimpleElement configuration) {
//
//        boolean SUMOKBexists = false;
//        if (!configuration.getTagName().equals("configuration"))
//            System.out.println("Error in KBmanager.fromXML(): Bad tag: " + configuration.getTagName());
//        else {
//            for (int i = 0; i < configuration.getChildElements().size(); i++) {
//                SimpleElement element = (SimpleElement) configuration.getChildElements().get(i);
//                if (element.getTagName().equals("kb")) {
//                    String kbName = (String) element.getAttribute("name");
//                    if (kbName.equals(getMgr().getPref("sumokbname")))
//                        SUMOKBexists = true;
//                    KBmanager.getMgr().addKB(kbName);
//                    ArrayList<String> constituentsToAdd = new ArrayList<String>();
//                    boolean useCacheFile = KBmanager.getMgr().getPref("cache").equalsIgnoreCase("yes");
//                    for (int j = 0; j < element.getChildElements().size(); j++) {
//                        SimpleElement kbConst = (SimpleElement) element.getChildElements().get(j);
//                        if (!kbConst.getTagName().equals("constituent"))
//                            System.out.println("Error in KBmanager.fromXML(): Bad tag: " + kbConst.getTagName());
//                        String filename = (String) kbConst.getAttribute("filename");
//                        if (!filename.startsWith((File.separator)))
//                            filename = KBmanager.getMgr().getPref("kbDir") + File.separator + filename;
//                        if (!StringUtil.emptyString(filename)) {
//                            if (filename.endsWith(KB._cacheFileSuffix)) {
//                                if (useCacheFile)
//                                    constituentsToAdd.add(filename);
//                            }
//                            else
//                                constituentsToAdd.add(filename);
//                        }
//                    }
//                    KBmanager.getMgr().loadKB(kbName, constituentsToAdd);
//                }
//            }
//        }
//        System.out.println("kbsFromXML(): Completed loading KBs");
//        if (!SUMOKBexists)
//            System.out.println("Error in KBmanager.fromXML(): no SUMO kb.  Some Sigma functions will not work.");
//    }
//
//    /** ***************************************************************
//     * Note that filenames that are not full paths are prefixed with the
//     * value of preference kbDir
//     */
//    private static ArrayList<ArrayList<String>> kbFilenamesFromXML(SimpleElement configuration) {
//
//        ArrayList<ArrayList<String>> result = new ArrayList<>();
//        if (!configuration.getTagName().equals("configuration"))
//            System.out.println("Error in KBmanager.kbsFilenamesFromXML(): Bad tag: " + configuration.getTagName());
//        else {
//            for (int i = 0; i < configuration.getChildElements().size(); i++) {
//                SimpleElement element = (SimpleElement) configuration.getChildElements().get(i);
//                if (element.getTagName().equals("kb")) {
//                    ArrayList<String> kb = new ArrayList<>();
//                    result.add(kb);
//                    boolean useCacheFile = KBmanager.getMgr().getPref("cache").equalsIgnoreCase("yes");
//                    for (int j = 0; j < element.getChildElements().size(); j++) {
//                        SimpleElement kbConst = (SimpleElement) element.getChildElements().get(j);
//                        if (!kbConst.getTagName().equals("constituent"))
//                            System.out.println("Error in KBmanager.fromXML(): Bad tag: " + kbConst.getTagName());
//                        String filename = (String) kbConst.getAttribute("filename");
//                        if (!filename.startsWith((File.separator)))
//                            filename = KBmanager.getMgr().getPref("kbDir") + File.separator + filename;
//                        if (!StringUtil.emptyString(filename)) {
//                            if (filename.endsWith(KB._cacheFileSuffix)) {
//                                if (useCacheFile)
//                                    kb.add(filename);
//                            }
//                            else
//                                kb.add(filename);
//                        }
//                    }
//                }
//                else
//                    System.out.println("Error in KBmanager.fromXML(): Bad tag: " + element.getTagName());
//            }
//        }
//        System.out.println("kbsFilenamesFromXML(): Completed loading KB names");
//        return result;
//    }
//
//    /** ***************************************************************
//     */
//    public boolean loadKB(String kbName, List<String> constituents) {
//
//        KB kb = null;
//        try {
//            if (existsKB(kbName))
//                removeKB(kbName);
//            addKB(kbName);
//            kb = getKB(kbName);
//
//            if (!(constituents.isEmpty())) {
//                Iterator<String> it = constituents.iterator();
//                while (it.hasNext()) {
//                    String filename = it.next();
//                    try {
//                        System.out.println("KBmanager.loadKB(): add constituent " + filename + " to " + kbName);
//                        kb.addConstituent(filename);
//                    }
//                    catch (Exception e1) {
//                        System.out.println("Error in KBmanager.loadKB():  " + e1.getMessage());
//                        e1.printStackTrace();
//                        return false;
//                    }
//                }
//            }
//        }
//        catch (Exception e) {
//            System.out.println("Error in KBmanager.loadKB(): Unable to save configuration: " + e.getMessage());
//            e.printStackTrace();
//            return false;
//        }
//
//        kb.kbCache = new KBcache(kb);
//        kb.kbCache.buildCaches();
//        kb.checkArity();
//
//        if (KBmanager.getMgr().getPref("TPTP").equals("yes"))
//            kb.loadEProver();
//        return true;
//    }
//
//    /** ***************************************************************
//     */
//    private void fromXML(SimpleElement configuration) {
//
//        if (!configuration.getTagName().equals("configuration"))
//            System.out.println("Error in KBmanager.fromXML(): Bad tag: " + configuration.getTagName());
//        else {
//            for (int i = 0; i < configuration.getChildElements().size(); i++) {
//                SimpleElement element = (SimpleElement) configuration.getChildElements().get(i);
//                if (element.getTagName().equals("preference")) {
//                    String name = (String) element.getAttribute("name");
//                    if (!configKeys.contains(name)) {
//                        System.out.println("Error in KBmanager.fromXML(): Bad key: " + name);
//                        // continue; // set it anyway
//                    }
//                    String value = (String) element.getAttribute("value");
//                    preferences.put(name,value);
//                }
//                else {
//                    if (element.getTagName().equals("kb")) {
//                        String kbName = (String) element.getAttribute("name");
//                        addKB(kbName);
//                        ArrayList<String> constituentsToAdd = new ArrayList<String>();
//                        boolean useCacheFile = KBmanager.getMgr().getPref("cache").equalsIgnoreCase("yes");
//                        for (int j = 0; j < element.getChildElements().size(); j++) {
//                            SimpleElement kbConst = (SimpleElement) element.getChildElements().get(j);
//                            if (!kbConst.getTagName().equals("constituent"))
//                                System.out.println("Error in KBmanager.fromXML(): Bad tag: " + kbConst.getTagName());
//                            String filename = (String) kbConst.getAttribute("filename");
//                            if (!StringUtil.emptyString(filename)) {
//                                if (filename.endsWith(KB._cacheFileSuffix)) {
//                                    if (useCacheFile)
//                                        constituentsToAdd.add(filename);
//                                }
//                                else
//                                    constituentsToAdd.add(filename);
//                            }
//                        }
//                        loadKB(kbName, constituentsToAdd);
//                    }
//                    else
//                        System.out.println("Error in KBmanager.fromXML(): Bad tag: " + element.getTagName());
//                }
//            }
//        }
//    }
//
//    /** ***************************************************************
//     * Read an XML-formatted configuration file. The method initializeOnce()
//     * sets the preferences based on the contents of the configuration file.
//     * This routine has the side effect of setting the variable
//     * called "configuration".  It also creates the KBs directory and an empty
//     * configuration file if none exists.
//     */
//    public static void copyFile(File in, File out) {
//
//        FileInputStream fis  = null;
//        FileOutputStream fos = null;
//        try {
//            fis = new FileInputStream(in);
//            fos = new FileOutputStream(out);
//            byte[] buf = new byte[1024];
//            int i = 0;
//            while ((i = fis.read(buf)) != -1) {
//                fos.write(buf, 0, i);
//            }
//            fos.flush();
//        }
//        catch (Exception ex) {
//            ex.printStackTrace();
//        }
//        finally {
//            try {
//                if (fis != null) fis.close();
//                if (fos != null) fos.close();
//            }
//            catch (Exception ioe) {
//                ioe.printStackTrace();
//            }
//        }
//        return;
//    }
//
//    /** ***************************************************************
//     * Reads an XML configuration file from the directory
//     * configDirPath, and tries to find a configuration file elsewhere
//     * if configDirPath is null.  The method initializeOnce() sets the
//     * preferences based on the contents of the configuration file.
//     * This routine has the side effect of setting the variable called
//     * "configuration".  It also creates the KBs directory and an
//     * empty configuration file if none exists.
//     */
//    protected SimpleElement readConfiguration(String configDirPath) {
//
//        System.out.println("KBmanager.readConfiguration()");
//        SimpleElement configuration = null;
//        BufferedReader br = null;
//        try {
//            String kbDirStr = configDirPath;
//            if (StringUtil.emptyString(kbDirStr)) {
//                kbDirStr = (String) preferences.get("kbDir");
//                if (StringUtil.emptyString(kbDirStr))
//                    kbDirStr = System.getProperty("user.dir");
//            }
//            File kbDir = new File(kbDirStr);
//            if (!kbDir.exists()) {
//                kbDir.mkdir();
//                preferences.put("kbDir", kbDir.getCanonicalPath());
//            }
//            String config_file = CONFIG_FILE;
//            File configFile = new File(kbDir, config_file);
//            File global_config = new File(kbDir, CONFIG_FILE);
//            if (!configFile.exists()) {
//                if (global_config.exists()) {
//                    copyFile(global_config, configFile);
//                    configFile = global_config;
//                }
//                else
//                    writeConfiguration();
//            }
//            br = new BufferedReader(new FileReader(configFile));
//            SimpleDOMParser sdp = new SimpleDOMParser();
//            configuration = sdp.parse(br);
//        }
//        catch (Exception ex) {
//            System.out.println("ERROR in KBmanager.readConfiguration(" + configDirPath
//                    + "):\n" + "  Exception parsing configuration file \n" + ex.getMessage());
//            ex.printStackTrace();
//        }
//        finally {
//            try {
//                if (br != null)
//                    br.close();
//            }
//            catch (Exception ex2) {
//                ex2.printStackTrace();
//            }
//        }
//        return configuration;
//    }
//
    /** ***************************************************************
     * Reads in the KBs and other parameters defined in the XML
     * configuration file, or uses the default parameters.
     */
    public void initializeOnce() {

        System.out.println("Info in KBmanager.initializeOnce()");
        //Thread.dumpStack();
        String base = System.getenv("SIGMA_HOME");
        initializeOnce(base + File.separator + "KBs");
        return;
    }

    /** ***************************************************************
     * Reads in the KBs and other parameters defined in the XML
     * configuration file, or uses the default parameters.  If
     * configFileDir is not null and a configuration file can be read
     * from the directory, reinitialization is forced.
     */
    public void initializeOnce(String configFileDir) {

        boolean loaded = false;
        if (initializing || initialized) {
            System.out.println("Info in KBmanager.initializeOnce(): initialized is " + initialized);
            System.out.println("Info in KBmanager.initializeOnce(): initializing is " + initializing);
            System.out.println("Info in KBmanager.initializeOnce(): returning ");
            return;
        }
        initializing = true;
        KBmanager.getMgr().setPref("kbDir",configFileDir);
        if (debug) System.out.println("KBmanager.initializeOnce(): number of preferences: " +
                preferences.keySet().size());
//        try {
            System.out.println("Info in KBmanager.initializeOnce(): initializing with " + configFileDir);
//            SimpleElement configuration = readConfiguration(configFileDir);
//            if (configuration == null)
//                throw new Exception("Error reading configuration file in KBmanager.initializeOnce()");
//            if (serializedExists() && !serializedOld(configuration)) {
//                if (debug) System.out.println("KBmanager.initializeOnce(): serialized exists and is not old ");
//                loaded = loadSerialized();
//                if (loaded) {
//                    if (debug) System.out.println("KBmanager.initializeOnce(): manager is loaded ");
//                    WordNet.wn.initOnce();
//                    NLGUtils.init(configFileDir);
//                    OMWordnet.readOMWfiles();
//                    initializing = false;
//                    initialized = true;
//                }
//            }
//            if (!loaded) { // if there was an error loading the serialized file, then reload from sources
                System.out.println("Info in KBmanager.initializeOnce(): reading from sources");
                if (debug) System.out.println("KBmanager.initializeOnce(): number of preferences: " +
                        preferences.keySet().size());
                manager = this;
                KBmanager.getMgr().setPref("kbDir",configFileDir); // need to restore config file path
//                if (StringUtil.isNonEmptyString(configFileDir)) {
//                    setDefaultAttributes();
//                    setConfiguration(configuration);
//                }
//                else
                    setDefaultAttributes();
                System.out.println("Info in KBmanager.initializeOnce(): completed initialization");
//                serialize();
                initializing = false;
                initialized = true;
//            }
//        }
//        catch (Exception ex) {
//            System.out.println(ex.getMessage());
//            ex.printStackTrace();
//            return;
//        }
        System.out.println("Info in KBmanager.initializeOnce(): initialized is " + initialized);
        if (debug) System.out.println("KBmanager.initializeOnce(): number of preferences: " +
                preferences.keySet().size());
        return;
    }

//    /** ***************************************************************
//     * Sets instance fields by reading the xml found in the configuration file.
//     * @param configuration
//     */
//    public void setConfiguration(SimpleElement configuration) {
//
//        System.out.println("Info in KBmanager.setConfiguration():");
//        preferencesFromXML(configuration);
//        kbsFromXML(configuration);
//        String kbDir = (String) preferences.get("kbDir");
//        System.out.println("Info in KBmanager.setConfiguration(): Using kbDir: " + kbDir);
//        NLGUtils.init(kbDir);
//        WordNet.wn.initOnce();
//        OMWordnet.readOMWfiles();
//        if (kbs != null && kbs.size() > 0) {
//            Iterator<String> it = kbs.keySet().iterator();
//            while (it.hasNext()) {
//                String kbName = it.next();
//                System.out.println("INFO in KBmanager.setConfiguration(): " + kbName);
//                WordNet.wn.termFormatsToSynsets(KBmanager.getMgr().getKB(kbName));
//            }
//        }
//        else
//            System.out.println("Error in KBmanager.setConfiguration(): No kbs");
//        if (debug) System.out.println("KBmanager.setConfiguration(): number of preferences: " +
//                preferences.keySet().size());
//    }

    /** ***************************************************************
     * Double the backslash in a filename so that it can be saved to a text
     * file and read back properly.
     */
    public static String escapeFilename(String fname) {

        StringBuilder newstring = new StringBuilder("");
        for (int i = 0; i < fname.length(); i++) {
            if (fname.charAt(i) == 92 && fname.charAt(i+1) != 92)
                newstring = newstring.append("\\\\");
            if (fname.charAt(i) == 92 && fname.charAt(i+1) == 92) {
                newstring = newstring.append("\\\\");
                i++;
            }
            if (fname.charAt(i) != 92)
                newstring = newstring.append(fname.charAt(i));
        }
        return newstring.toString();
    }

    /** ***************************************************************
     * Create a new empty KB with a name.
     * @param name - the name of the KB
     */
    public KB addKB(String name) {
        return addKB(name, true);
    }

    public KB addKB(String name, boolean isVisible) {

        KB kb = new KB(name,(String) preferences.get("kbDir"), isVisible);
        kbs.put(name.intern(),kb);
        return kb;
    }

//    /** ***************************************************************
//     * Remove a knowledge base.
//     * @param name - the name of the KB
//     */
//    public String removeKB(String name) {
//
//        KB kb = (KB) kbs.get(name);
//        if (kb == null)
//            return "KB " + name + " does not exist and cannot be removed.";
//        try {
//            if (kb.eprover != null)
//                kb.eprover.terminate();
//        }
//        catch (Exception ioe) {
//            System.out.println("Error in KBmanager.removeKB(): ");
//            System.out.println("  Error terminating inference engine: " + ioe.getMessage());
//        }
//        kbs.remove(name);
//        try {
//            //writeConfiguration();
//        }
//        catch (Exception ioe) {
//            System.out.println("Error in KBmanager.removeKB(): ");
//            System.out.println("  Error writing configuration file: " + ioe.getMessage());
//        }
//        return "KB " + name + " successfully removed.";
//    }
//
//    /** ***************************************************************
//     * Write the current configuration of the system.  Call
//     * writeConfiguration() on each KB object to write its manifest.
//     */
//    public void writeConfiguration() throws IOException {
//
//        System.out.println("INFO in KBmanager.writeConfiguration()");
//        FileWriter fw = null;
//        PrintWriter pw = null;
//        String dir = (String) preferences.get("kbDir");
//        File fDir = new File(dir);
//        String username = (String) preferences.get("userName");
//        String userrole = (String) preferences.get("userRole");
//        String config_file = (((username != null)
//                && userrole.equalsIgnoreCase("administrator")
//                && !username.equalsIgnoreCase("admin"))
//                ? username + "_"
//                : "") + CONFIG_FILE;
//        File file = new File(fDir, config_file);
//        String canonicalPath = file.getCanonicalPath();
//
//        SimpleElement configXML = new SimpleElement("configuration");
//        Iterator<String> it = preferences.keySet().iterator();
//        while (it.hasNext()) {
//            String key = it.next();
//            String value = preferences.get(key);
//            if (fileKeys.contains(key))
//                value = escapeFilename(value);
//            if (!Arrays.asList("userName", "userRole").contains(key)) {
//                SimpleElement preference = new SimpleElement("preference");
//                preference.setAttribute("name",key);
//                preference.setAttribute("value",value);
//                configXML.addChildElement(preference);
//            }
//        }
//        Iterator<String> it2 = kbs.keySet().iterator();
//        while (it2.hasNext()) {
//            String key = it2.next();
//            KB kb = kbs.get(key);
//            SimpleElement kbXML = kb.writeConfiguration();
//            configXML.addChildElement(kbXML);
//        }
//        try {
//            fw = new FileWriter(file);
//            pw = new PrintWriter(fw);
//            pw.println(configXML.toFileString());
//        }
//        catch (java.io.IOException e) {
//            System.out.println("Error writing file " + canonicalPath + ".\n " + e.getMessage());
//            throw new IOException("Error writing file " + canonicalPath + ".\n " + e.getMessage());
//        }
//        finally {
//            if (pw != null)
//                pw.close();
//            if (fw != null)
//                fw.close();
//        }
//        return;
//    }

    /** ***************************************************************
     * Get the KB that has the given name.
     */
    public KB getKB(String name) {

        if (!kbs.containsKey(name))
            System.out.println("KBmanager.getKB(): KB " + name + " not found.");
        return (KB) kbs.get(name.intern());
    }

    /** ***************************************************************
     * Returns true if a KB with the given name exists.
     */
    public boolean existsKB(String name) {

        return kbs.containsKey(name);
    }

    /** ***************************************************************
     * Remove the KB that has the given name.
     */
    public void remove(String name) {

        kbs.remove(name);
    }

    /** ***************************************************************
     * Get the one instance of KBmanager from its class variable.
     */
    public static KBmanager getMgr() {

        //if (manager == null)
        //    manager = new KBmanager();
        return manager;
    }

    /** ***************************************************************
     * Get the Set of KB names in this manager.
     */
    public HashSet<String> getKBnames() {

        HashSet<String> names = new HashSet<String>();
        Iterator<String> it = kbs.keySet().iterator();
        while (it.hasNext()) {
            String kbName = (String) it.next();
            KB kb = (KB) getKB(kbName);
            if (kb.isVisible())
                names.add(kbName);
        }
        return names;
    }

    /** ***************************************************************
     * Get the the complete list of languages available in all KBs
     */
    public ArrayList<String> allAvailableLanguages() {

        ArrayList<String> result = new ArrayList<String>();
        Iterator<String> it = kbs.keySet().iterator();
        while (it.hasNext()) {
            String kbName = (String) it.next();
            KB kb = (KB) getKB(kbName);
            result.addAll(kb.availableLanguages());
        }
        return result;
    }

    /** ***************************************************************
     * Print all peferences to stdout
     */
    public void printPrefs() {

        System.out.println("KBmanager.printPrefs()");
        if (preferences == null || preferences.size() == 0)
            System.out.println("KBmanager.printPrefs(): preference list is empty");
        for (String key : preferences.keySet()) {
            String value = preferences.get(key);
            System.out.println(key + " : " + value);
        }
    }

    /** ***************************************************************
     * Get the preference corresponding to the given key
     */
    public String getPref(String key) {

        if (!configKeys.contains(key)) {
            System.out.println("Error in KBmanager.getPref(): bad key: " + key);
            return "";
        }
        String ans = (String) preferences.get(key);
        if (ans == null)
            ans = "";
        return ans;
    }

    /** ***************************************************************
     * Set the preference to the given value.
     */
    public void setPref(String key, String value) {

        if (!configKeys.contains(key)) {
            System.out.println("Error in KBmanager.setPref(): bad key: " + key);
            return;
        }
        preferences.put(key,value);
    }

//    /** ***************************************************************
//     * Create an server-based interface for Python to call the KB object.
//     * https://pypi.python.org
//     *
//     * from py4j.java_gateway import JavaGateway
//     * gateway = JavaGateway()             # connect to the JVM
//     * sigma_app = gateway.entry_point     # get the KB instance
//     * print(sigma_app.getTerms())         # call a method
//     */
//    public static void pythonServer() {
//
//        System.out.println("KBmanager.pythonServer(): begin initialization");
//        try {
//            KBmanager.getMgr().initializeOnce();
//        }
//        catch (Exception e ) {
//            System.out.println(e.getMessage());
//        }
//        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
//        GatewayServer server = new GatewayServer(kb);
//        server.start();
//        System.out.println("KBmanager.pythonServer(): completed initialization, server running");
//    }

    /** ***************************************************************
     * A test method.
     */
    public static void printHelp() {

        System.out.println("Sigma Knowledge Engineering Environment");
        System.out.println("  options:");
        System.out.println("  -h - show this help screen");
        System.out.println("  -p - demo Python interface");
        System.out.println("  with no arguments show this help screen an execute a test");
    }

//    /** ***************************************************************
//     * A test method.
//     */
//    public static void main(String[] args) {
//
//        if (args == null) {
//            printHelp();
//            try {
//                KBmanager.getMgr().initializeOnce();
//            }
//            catch (Exception e) {
//                System.out.println(e.getMessage());
//            }
//            KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
//            Formula f = new Formula();
//            f.read("(=> (and (wears ?A ?C) (part ?P ?C)) (wears ?A ?P))");
//            FormulaPreprocessor fp = new FormulaPreprocessor();
//            System.out.println(fp.preProcess(f, false, kb));
//        }
//        else {
//            if (args != null && args.length > 0 && args[0].equals("-p")) {
//                pythonServer();
//            }
//            if (args != null && args.length > 0 && args[0].equals("-h")) {
//                printHelp();
//            }
//        }
//    }
}
