package org.boon.bugs;

import org.boon.core.Dates;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.boon.Boon.puts;
import static org.junit.jupiter.api.Assertions.assertEquals;

//import static junit.framework.Assertions.assertEquals;

/**
 * Created by Richard on 9/5/14.
 */
public class Bug209 {

    @Test
    public void testIsoJacksonLongDateOffsetVariation() {
        // Colon in the offset: +0200 vs. +02:00
        String test = "2014-05-29T08:54:09.764+02:00";
        Date date = Dates.fromISO8601Jackson(test);
        puts(date);
        Date date2 = Dates.fromISO8601Jackson_(test);
        puts(date2);
        assertEquals(date2.toString(), "" + date);
    }
}
