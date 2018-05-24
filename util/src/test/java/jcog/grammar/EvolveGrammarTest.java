package jcog.grammar;


import jcog.grammar.evolve.EvolveGrammar;
import jcog.grammar.evolve.SimpleConfig;
import jcog.grammar.evolve.inputs.DataSet;
import jcog.grammar.evolve.outputs.Results;
import org.junit.jupiter.api.Test;

import static jcog.grammar.DataSetTest.noise;

/**
 * Created by me on 11/29/15.
 */
public class EvolveGrammarTest {

    /** JSON example from wiki: https://github.com/MaLeLabTs/RegexGenerator/wiki/Annotated-Dataset */
    String j = "{\n" +
            "  \"name\": \"Log/MAC\",\n" +
            "  \"description\": \"\",\n" +
            "  \"regexTarget\": \"\",\n" +
            "  \"examples\": [\n" +
            "    {\n" +
            "    \"string\": \"Jan 12 06:26:19: ACCEPT service http from 119.63.193.196 to firewall(pub-nic), prefix: \\\"none\\\" (in: eth0 119.63.193.196(5c:0a:5b:63:4a:82):4399 -> 140.105.63.164(50:06:04:92:53:44):80 TCP flags: ****S* len:60 ttl:32)\",\n" +
            "    \"match\": [\n" +
            "        { “start\": 119, \"end\": 136 },\n" +
            "        { \"start\": 161, \"end\": 178 }\n" +
            "    ],\n" +
            "    \"unmatch\": [\n" +
            "        {\"start\": 0,\"end\": 119},\n" +
            "        {\"start\": 136,\"end\": 161},\n" +
            "        {\"start\": 178,\"end\": 215}\n" +
            "    ]\n" +
            "    } ] }";

    @Test
    public void test1() throws Exception {
        run( DataSetTest.getExampleDataSet());
    }
    @Test
    public void test2() throws Exception {
        run(DataSetTest.getExampleDataSet2(

                () -> "/*" + noise(2 + (int)(Math.random()*3)) + "*/",

                "acs(x111111);", "fn_c(yy3333,ab);", "d123();", "a(x,y,z);",
                "xf(/*ab,c*/z, z1);", "gggg(b /* !;*(fs)s! */);"

                //"a(dd, a123);", "b(x, yz124, v)", "b(a,a,a,a);"
                ));
    }

//    static final Gson resultOutput = new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create();

    private void run(DataSet d) throws Exception {
        Results r = EvolveGrammar.run(new SimpleConfig(
                d, 400, 100)
                .buildConfiguration()
        );


        System.out.println(r.getBestSolution());
        System.out.println(r.getBestExtractionsStats());
//        System.out.println(resultOutput.toJson( r.getBestSolution()) );
//        System.out.println(resultOutput.toJson(r.getBestExtractionsStats()));


        //Configurator.configure(j));
    }

}