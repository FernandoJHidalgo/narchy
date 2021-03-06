package jcog.pri;

public interface Pressurizable {

    /** the current pressure */
    float pressure();

    /** add pressure */
    void pressurize(float f);

    /** depressurize a constant amount */
    void depressurize(float pri);

    /** depressurize a percentage of what exists */
    float depressurizePct(float rate);


}
