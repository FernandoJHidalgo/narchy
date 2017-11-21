package nars.control;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * utility class for working witih Deriver's
 */
public class Derivers {

    /** range is inclusive */
    public static Set<String> standard(int minLevel, int maxLevel, String... otherFiles) {
        Set<String> files = new TreeSet();
        for (int level = minLevel; level <= maxLevel; level++) {
            switch (level) {
                case 8:
                    //files.add("motivation.nal");
                    //files.add("list.nal");  //experimental
                case 7:
                    //TODO move temporal induction to a separate file
                    //fallthru
                case 6:
                    files.add("nal6.nal");
                    files.add("nal6.guess.nal");

                    files.add("induction.nal");  //TODO nal6 only needs general induction, not the temporal parts

                    files.add("misc.nal"); //TODO split this up
                    //fallthru
                case 5:
                case 4:
                case 3:
                case 2:
                    files.add("nal3.nal");
                    files.add("nal3.guess.nal");
                    files.add("nal2.nal");
                    files.add("nal2.guess.nal");
                    //fallthru
                case 1:
                    files.add("nal1.nal");
                    files.add("nal1.guess.nal");
                    break;
            }
        }

        Collections.addAll(files, otherFiles);

        return files;
    }
}
