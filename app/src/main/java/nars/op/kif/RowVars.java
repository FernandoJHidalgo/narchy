package nars.op.kif;

import java.util.*;

public class RowVars {

    public static boolean DEBUG = false;
    
    /** ***************************************************************
     * @return a HashSet, possibly empty, containing row variable
     * names, each of which will start with the row variable
     * designator '@'.
     */
    private static HashSet<String> findRowVars(Formula f) {

        
        HashSet<String> result = new HashSet<String>();
        if (!StringUtil.emptyString(f.theFormula)
            && f.theFormula.contains(Formula.R_PREF)) {
            Formula fnew = new Formula();
            fnew.read(f.theFormula);
            while (fnew.listP() && !fnew.empty()) {
                String arg = fnew.getArgument(0);
                if (arg.startsWith(Formula.R_PREF))
                    result.add(arg);
                else {
                    Formula argF = new Formula();
                    argF.read(arg);
                    if (argF.listP())
                        result.addAll(findRowVars(argF));
                }
                fnew.read(fnew.cdr());
            }
        }
        return result;
    }

    /** ***************************************************************
     * given in @param ar which is a list for each variable of all the
     * predicates in which it appears as an argument, find the minimum
     * arity allowed by predicate arities, as given by 
     * @seeAlso kb.kbCache.valences
     */
    private static HashMap<String,Integer> getRowVarMaxArities(HashMap<String,HashSet<String>> ar, KB kb) {
        
        HashMap<String,Integer> arities = new HashMap<String,Integer>();
        Iterator<String> it = ar.keySet().iterator();
        while (it.hasNext()) {
            String rowvar = it.next();
            HashSet<String> preds = ar.get(rowvar);
            Iterator<String> it2 = preds.iterator();
            while (it2.hasNext()) {
                String pred = it2.next();
                
                
                if (kb.kbCache.valences.get(pred) != null) {
                    int arity = kb.kbCache.valences.get(pred).intValue();
                    if (arities.containsKey(pred)) {
                        if (arity < arities.get(rowvar).intValue())
                            arities.put(rowvar, arity);
                    }
                    else
                        arities.put(rowvar, arity);
                }
            }
        }
        return arities;
    }

    /** ***************************************************************
     * given in @param ar which is a list for each variable of all the
     * predicates in which it appears as an argument, find the maximum
     * arity allowed by predicate arities, as given by
     * @seeAlso kb.kbCache.valences
     *
     * TODO: currently we only find the maximum arity allowed by predicate arities;
     *       we also need to find the minimum predicate arities;
     */
    public static HashMap<String,Integer> getRowVarMaxAritiesWithOtherArgs(HashMap<String,HashSet<String>> ar, KB kb, Formula f) {

        HashMap<String,Integer> arities = new HashMap<String,Integer>();
        Iterator<String> it = ar.keySet().iterator();
        while (it.hasNext()) {
            String rowvar = it.next();
            HashSet<String> preds = ar.get(rowvar);
            Iterator<String> it2 = preds.iterator();
            while (it2.hasNext()) {
                String pred = it2.next();

                
                
                int nonRowVar = 0;
                int start = f.theFormula.indexOf("("+pred);
                int end = f.theFormula.indexOf(')', start);
                String simpleFS = f.theFormula.substring(start, end+1);
                Formula simpleF = new Formula();
                simpleF.read(simpleFS);
                for (int i = 0; i < simpleF.listLength(); i++) {
                    if(simpleF.getArgument(i).startsWith(Formula.V_PREF))
                        nonRowVar++;
                }

                if (kb.kbCache!= null && kb.kbCache.valences != null &&
                        kb.kbCache.valences.get(pred) != null) {
                    int arity = kb.kbCache.valences.get(pred).intValue();
                    if (arities.containsKey(pred)) {
                        if (arity < arities.get(rowvar).intValue())
                            arities.put(rowvar, arity -nonRowVar);
                    }
                    else if (arity > 0)
                        arities.put(rowvar, arity -nonRowVar);
                }
            }
        }
        return arities;
    }

    /** ***************************************************************
     * Merge the key,value pairs for a multiple value ArrayList
     */
    private static HashMap<String,HashSet<String>> 
        mergeValueSets(HashMap<String,HashSet<String>> ar1, HashMap<String,HashSet<String>> ar2) {

        HashMap<String, HashSet<String>> result = new HashMap<String, HashSet<String>>(ar1);
        Iterator<String> it = ar2.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            HashSet<String> values = ar2.get(key);
            HashSet<String> arg1values = ar1.get(key);
            if (arg1values == null)
                result.put(key, values);
            else {
                arg1values.addAll(values);
            }
        }
        return result;
    }
    
    /** ***************************************************************
     * Add a key,value pair for a multiple value ArrayList
     */
    private static HashMap<String,HashSet<String>> 
        addToValueSet(HashMap<String,HashSet<String>> ar, String key, String value) {
        
        HashSet<String> val = ar.get(key);
        if (val == null) 
            val = new HashSet<String>();
        val.add(value);
        ar.put(key, val);
        return ar;
    }
    
    /** ***************************************************************
     */
    private static HashMap<String,HashSet<String>> getRowVarRelLogOps(Formula f, String pred) {
    
        
        HashMap<String,HashSet<String>> result = new HashMap<String,HashSet<String>>();
        if (Formula.isQuantifier(pred)) {
            Formula arg2 = new Formula(f.getArgument(2));
            if (arg2 != null)
                return getRowVarRelations(arg2);
        }
        else if (pred.equals(Formula.NOT)) {
            Formula arg1 = new Formula(f.getArgument(1));
            if (arg1 != null)
                return getRowVarRelations(arg1);
            else
                return result;
        }
        else if (pred.equals(Formula.EQUAL) || pred.equals(Formula.IFF)  || pred.equals(Formula.IF)) {
            Formula arg1 = new Formula(f.getArgument(1));
            Formula arg2 = new Formula(f.getArgument(2));
            if (arg1 != null && arg2 != null)
                return mergeValueSets(getRowVarRelations(arg1),getRowVarRelations(arg2));
            else
                return result;
        }
        else {  
            ArrayList<String> args = f.complexArgumentsToArrayList(1);
            for (int i = 1; i < args.size(); i++) {
                Formula f2 = new Formula(args.get(i));
                result = mergeValueSets(result,getRowVarRelations(f2));
            }
            return result;
        }
        return result;
    }
    
    /** ***************************************************************
     * Recurse through the formula looking for row variables.  If found,
     * add it to a map that has row variables as keys and a set of
     * predicate names as values. 
     */
    private static HashMap<String,HashSet<String>> getRowVarRelations(Formula f) {
        
        
        HashMap<String,HashSet<String>> result = new HashMap<String,HashSet<String>>();
        if (!f.theFormula.contains("@") || f.empty() || f.atom())
            return result;
        String pred = f.getArgument(0);
        if (!f.theFormula.substring(1).contains("(")) {  
            
            HashSet<String> rowvars = findRowVars(f);
            Iterator<String> it = rowvars.iterator();
            while (it.hasNext()) {
                String var = it.next();
                
                addToValueSet(result,var,pred);
            }
            return result;
        }
        if (Formula.isLogicalOperator(pred)) {
            return getRowVarRelLogOps(f,pred);
        }
        else {  
            ArrayList<String> args = f.complexArgumentsToArrayList(1);
            for (int i = 0; i < args.size(); i++) {
                Formula f2 = new Formula(args.get(i));
                if (f2.theFormula.startsWith("@")) {
                    
                    addToValueSet(result,f2.theFormula,pred);
                }
                else if (f2.theFormula.contains("@"))
                    result = mergeValueSets(result,getRowVarRelations(f2));
            }
        }
        return result;
    }
    
    /** ***************************************************************
     * Expand row variables, keeping the information about the original
     * source formula.  Each variable is treated like a macro that
     * expands to up to seven regular variables.  For example
     *
     * (=>
     *    (and
     *       (subrelation ?REL1 ?REL2)
     *       (?REL1 @ROW))
     *    (?REL2 @ROW))
     *
     * would become
     *
     * (=>
     *    (and
     *       (subrelation ?REL1 ?REL2)
     *       (?REL1 ?ARG1))
     *    (?REL2 ?ARG1))
     *
     * (=>
     *    (and
     *       (subrelation ?REL1 ?REL2)
     *       (?REL1 ?ARG1 ?ARG2))
     *    (?REL2 ?ARG1 ?ARG2))
     * etc.
     *
     * TODO: Note that this method does not handle the case of row 
     * variables in an argument list with other arguments.  It will
     * just blindly generate all 7 variable expansions even if this
     * means that a predicate will wind up with more than 7 arguments
     * due to the existence of a non-row-variable in the argument
     * list.
     * 
     * @return an ArrayList of Formulas, or an empty ArrayList.
     */
    public static ArrayList<Formula> expandRowVars(KB kb, Formula f) {
        
        Set<String> result = new TreeSet<String>();
        ArrayList<Formula> formresult = new ArrayList<Formula>();
        if (!f.theFormula.contains("@")) {
            
            formresult.add(f);
            return formresult;
        }
        if (DEBUG)
            System.out.println("Info in RowVars.expandRowVars(): f: " +f);
        HashMap<String,HashSet<String>> rels = getRowVarRelations(f);   
        HashMap<String,Integer> rowVarMaxArities = getRowVarMaxAritiesWithOtherArgs(rels, kb, f);
        result.add(f.theFormula);
        HashSet<String> rowvars = findRowVars(f);
        Iterator<String> it = rowvars.iterator();
        while (it.hasNext()) {
            String var = it.next();
            if (DEBUG)
                System.out.println("Info in RowVars.expandRowVars(): var: " + var);
            String replaceVar = var.replace('@', '?');
            Set<String> newresult = new TreeSet<String>();
            StringBuffer replaceString = new StringBuffer();
            int maxArity = 7;  
            if (rowVarMaxArities.containsKey(var) && maxArity > rowVarMaxArities.get(var).intValue())
                maxArity = rowVarMaxArities.get(var).intValue();
            for (int j = 0; j < maxArity; j++) {
                if (j > 0)
                    replaceString.append(" ");
                replaceString.append(replaceVar + (j + 1));
                if (DEBUG)
                    System.out.println("Info in RowVars.expandRowVars(): replace: " + replaceString);
                for (String form : result) {
                    form = form.replaceAll("\\"+var, replaceString.toString());
                    if (DEBUG)
                        System.out.println("Info in RowVars.expandRowVars(): form: " + form);
                    newresult.add(form);
                }
            }
            result = newresult;
        }

        for (String r : result) {
            formresult.add(new Formula(r));
        }
        if (DEBUG)
            System.out.println("Info in RowVars.expandRowVars(): exiting with: " + formresult);
        return formresult;
    }

    /** ***************************************************************
     * */
    public static void main(String[] args) {
        
        
        String fstring = "(=> (and (contraryAttribute @ROW1) (identicalListItems (ListFn @ROW1) (ListFn @ROW2))) (contraryAttribute @ROW2))"; 
        Formula f = new Formula(fstring);
        System.out.println("Info in RowVars.main(): " + findRowVars(f));
        KBmanager.getMgr().initializeOnce();
        System.out.println("Info in RowVars.main(): finished initialization");
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        RowVars.DEBUG = true;
        System.out.println("Info in RowVars.main(): " + getRowVarRelations(f));
    }
}
