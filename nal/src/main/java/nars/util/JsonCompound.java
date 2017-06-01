package nars.util;

import com.fasterxml.jackson.databind.JsonNode;
import jcog.Util;
import nars.$;
import nars.term.Term;
import nars.term.atom.Atomic;

/**
 * Created by me on 4/2/17.
 */
public enum JsonCompound  { ;


    public static Term the(JsonNode j) {

        if (j.isArray()) {
            int s = j.size();
            Term[] subterms = new Term[s];
            for (int i = 0; i < s; i++) {
                subterms[i] = the(j.get(i));
            }
            return $.p(subterms);

        } else if (j.isValueNode()) {
            if (j.isTextual()) {
                return $.quote(j.textValue());
            } else if (j.isNumber()) {
                return $.the(j.numberValue());
            } else {
                throw new UnsupportedOperationException();
            }
        } else if (j.isObject()) {
            Term[] s = new Term[j.size()];
            final int[] i = {0};
            j.fields().forEachRemaining(f -> {
                Atomic k = $.quote(f.getKey());
                Term v = the(f.getValue());
                s[i[0]++] =
                        $.inh(v, k);
                        //$.p(k, v);

            });
            return $.sete(s);
        } else {// if (j.isJsonObject()) {
            throw new UnsupportedOperationException("TODO");
        }
    }


    public static Term the(String json) {

        JsonNode x = Util.jsonNode(json);
        return the(x);

    }
}
