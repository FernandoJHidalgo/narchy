package org.boon.template;

import org.boon.Str;
import org.boon.template.support.Token;
import org.boon.template.support.TokenTypes;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.boon.Boon.equalsOrDie;
import static org.boon.Boon.puts;
import static org.boon.Exceptions.die;

/**
 * Created by Richard on 9/14/14.
 */
public class BoonCoreParserTest {

    boolean ok;

    @Test
    public void testSimpleNoEnd() {
        TemplateParser parser = new BoonCoreTemplateParser();
        String text = "<c:set var='foo' target='bar'/>love";
        //..............012345678901234567890123456789012345678901234567890
        //..............0         10        20        30        40        50
        parser.parse(text);
        final List<Token> tokenList = parser.getTokenList();
        Token token = tokenList.get(0);
        equalsOrDie("Token starts at 3", 3, token.start());
        equalsOrDie("Token stops at 29", 29, token.stop());
        equalsOrDie("Token is COMMAND", TokenTypes.COMMAND, token.type());
        equalsOrDie("TEXT is ", "set var='foo' target='bar'", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(1);
        equalsOrDie("Token starts at 30", 30, token.start());
        equalsOrDie("Token stops at 30", 30, token.stop());
        equalsOrDie("Token is COMMAND_BODY", TokenTypes.COMMAND_BODY, token.type());
        equalsOrDie("TEXT is ", "", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(2);
        equalsOrDie("Token starts at 31", 31, token.start());
        equalsOrDie("Token stops at 35", 35, token.stop());
        equalsOrDie("Token is TEXT", TokenTypes.TEXT, token.type());
        equalsOrDie("TEXT is ", "love", Str.sliceOf(text, token.start(), token.stop()));
    }

    @Test
    public void testSimple() {
        TemplateParser parser = new BoonCoreTemplateParser();
        String text = "  blah   <c:if test='foo'>  body </c:if> more text";
        //  012345678901234567890123456789012345678901234567890
        //  0         10        20        30        40        50
        parser.parse(text);
        final List<Token> tokenList = parser.getTokenList();
        Token token = tokenList.get(0);
        equalsOrDie("Token starts at 0", 0, token.start());
        equalsOrDie("Token stops at 9", 9, token.stop());
        equalsOrDie("Token is TEXT", TokenTypes.TEXT, token.type());
        equalsOrDie("TEXT is ", "  blah   ", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(1);
        equalsOrDie("Token starts at 12", 12, token.start());
        equalsOrDie("Token stops at 25", 25, token.stop());
        equalsOrDie("Token is COMMAND", TokenTypes.COMMAND, token.type());
        equalsOrDie("TEXT is ", "if test='foo'", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(2);
        equalsOrDie("Token starts at 26", 26, token.start());
        equalsOrDie("Token stops at 33", 33, token.stop());
        equalsOrDie("Token is COMMAND_BODY", TokenTypes.COMMAND_BODY, token.type());
        equalsOrDie("TEXT is ", "  body ", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(3);
        equalsOrDie("Token starts at 26", 26, token.start());
        equalsOrDie("Token stops at 33", 33, token.stop());
        equalsOrDie("Token is TEXT", TokenTypes.TEXT, token.type());
        equalsOrDie("TEXT is ", "  body ", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(4);
        equalsOrDie("Token starts at 40", 40, token.start());
        equalsOrDie("Token stops at 50", 50, token.stop());
        equalsOrDie("Token is TEXT", TokenTypes.TEXT, token.type());
        equalsOrDie("TEXT is ", " more text", Str.sliceOf(text, token.start(), token.stop()));
    }

    @Test
    public void testSimpleNoQuote() {
        TemplateParser parser = new BoonCoreTemplateParser();
        String text = "  blah   <c:if test=$foo >  body </c:if> more text";
        //  012345678901234567890123456789012345678901234567890
        //  0         10        20        30        40        50
        parser.parse(text);
        final List<Token> tokenList = parser.getTokenList();
        Token token = tokenList.get(0);
        equalsOrDie("Token starts at 0", 0, token.start());
        equalsOrDie("Token stops at 9", 9, token.stop());
        equalsOrDie("Token is TEXT", TokenTypes.TEXT, token.type());
        equalsOrDie("TEXT is ", "  blah   ", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(1);
        equalsOrDie("Token starts at 12", 12, token.start());
        equalsOrDie("Token stops at 25", 25, token.stop());
        equalsOrDie("Token is COMMAND", TokenTypes.COMMAND, token.type());
        equalsOrDie("TEXT is ", "if test=$foo ", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(2);
        equalsOrDie("Token starts at 26", 26, token.start());
        equalsOrDie("Token stops at 33", 33, token.stop());
        equalsOrDie("Token is COMMAND_BODY", TokenTypes.COMMAND_BODY, token.type());
        equalsOrDie("TEXT is ", "  body ", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(3);
        equalsOrDie("Token starts at 26", 26, token.start());
        equalsOrDie("Token stops at 33", 33, token.stop());
        equalsOrDie("Token is TEXT", TokenTypes.TEXT, token.type());
        equalsOrDie("TEXT is ", "  body ", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(4);
        equalsOrDie("Token starts at 40", 40, token.start());
        equalsOrDie("Token stops at 50", 50, token.stop());
        equalsOrDie("Token is TEXT", TokenTypes.TEXT, token.type());
        equalsOrDie("TEXT is ", " more text", Str.sliceOf(text, token.start(), token.stop()));
    }

    @Test
    public void testSimple1Char() {
        TemplateParser parser = new BoonCoreTemplateParser();
        String text = "  blah   <c:if test=1    >  body </c:if> more text";
        //  012345678901234567890123456789012345678901234567890
        //  0         10        20        30        40        50
        parser.parse(text);
        final List<Token> tokenList = parser.getTokenList();
        Token token = tokenList.get(0);
        equalsOrDie("Token starts at 0", 0, token.start());
        equalsOrDie("Token stops at 9", 9, token.stop());
        equalsOrDie("Token is TEXT", TokenTypes.TEXT, token.type());
        equalsOrDie("TEXT is ", "  blah   ", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(1);
        equalsOrDie("Token starts at 12", 12, token.start());
        equalsOrDie("Token stops at 25", 25, token.stop());
        equalsOrDie("Token is COMMAND", TokenTypes.COMMAND, token.type());
        equalsOrDie("TEXT is ", "#if test=1    #", "#" + Str.sliceOf(text, token.start(), token.stop()) + "#");
        token = tokenList.get(2);
        equalsOrDie("Token starts at 26", 26, token.start());
        equalsOrDie("Token stops at 33", 33, token.stop());
        equalsOrDie("Token is COMMAND_BODY", TokenTypes.COMMAND_BODY, token.type());
        equalsOrDie("TEXT is ", "  body ", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(3);
        equalsOrDie("Token starts at 26", 26, token.start());
        equalsOrDie("Token stops at 33", 33, token.stop());
        equalsOrDie("Token is TEXT", TokenTypes.TEXT, token.type());
        equalsOrDie("TEXT is ", "  body ", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(4);
        equalsOrDie("Token starts at 40", 40, token.start());
        equalsOrDie("Token stops at 50", 50, token.stop());
        equalsOrDie("Token is TEXT", TokenTypes.TEXT, token.type());
        equalsOrDie("TEXT is ", " more text", Str.sliceOf(text, token.start(), token.stop()));
    }

    @Test
    public void testNested() {
        TemplateParser parser = new BoonCoreTemplateParser();
        String text = "  blah   <c:if test='foo'>  body  <c:if test='bar'> body2 </c:if> body3 </c:if>more text";
        //  01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
        //  0         10        20        30        40        50        60        70        80        90
        parser.parse(text);
        final List<Token> tokenList = parser.getTokenList();
        Token token = tokenList.get(0);
        equalsOrDie("Token starts at 0", 0, token.start());
        equalsOrDie("Token stops at 9", 9, token.stop());
        equalsOrDie("Token is TEXT", TokenTypes.TEXT, token.type());
        equalsOrDie("TEXT is ", "  blah   ", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(1);
        equalsOrDie("Token starts at 12", 12, token.start());
        equalsOrDie("Token stops at 25", 25, token.stop());
        equalsOrDie("Token is COMMAND", TokenTypes.COMMAND, token.type());
        equalsOrDie("TEXT is ", "if test='foo'", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(2);
        equalsOrDie("Token starts at 26", 26, token.start());
        equalsOrDie("Token stops at 71", 72, token.stop());
        equalsOrDie("Token is COMMAND_BODY", TokenTypes.COMMAND_BODY, token.type());
        equalsOrDie("TEXT is ", "  body  <c:if test='bar'> body2 </c:if> body3 ", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(3);
        equalsOrDie("Token starts at 26", 26, token.start());
        equalsOrDie("Token stops at 34", 34, token.stop());
        equalsOrDie("Token is TEXT", TokenTypes.TEXT, token.type());
        equalsOrDie("TEXT is ", "  body  ", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(4);
        puts(tokenList.size());
        equalsOrDie("Token starts at 37", 37, token.start());
        equalsOrDie("Token stops at 50", 50, token.stop());
        equalsOrDie("Token is COMMAND", TokenTypes.COMMAND, token.type());
        equalsOrDie("TEXT is ", "#if test='bar'#", "#" + Str.sliceOf(text, token.start(), token.stop()) + "#");
        token = tokenList.get(5);
        equalsOrDie("Token starts at 51", 51, token.start());
        equalsOrDie("Token stops at 58", 58, token.stop());
        equalsOrDie("Token is COMMAND_BODY", TokenTypes.COMMAND_BODY, token.type());
        equalsOrDie("TEXT is ", "# body2 #", "#" + Str.sliceOf(text, token.start(), token.stop()) + "#");
        token = tokenList.get(6);
        equalsOrDie("Token starts at 51", 51, token.start());
        equalsOrDie("Token stops at 58", 58, token.stop());
        equalsOrDie("Token is TEXT", TokenTypes.TEXT, token.type());
        equalsOrDie("TEXT is ", "# body2 #", "#" + Str.sliceOf(text, token.start(), token.stop()) + "#");
        token = tokenList.get(7);
        equalsOrDie("Token starts at 65", 65, token.start());
        equalsOrDie("Token stops at 72", 72, token.stop());
        equalsOrDie("Token is TEXT", TokenTypes.TEXT, token.type());
        equalsOrDie("TEXT is ", "# body3 #", "#" + Str.sliceOf(text, token.start(), token.stop()) + "#");
        token = tokenList.get(8);
        equalsOrDie("Token starts at 79", 79, token.start());
        equalsOrDie("Token stops at 88", 88, token.stop());
        equalsOrDie("Token is TEXT", TokenTypes.TEXT, token.type());
        equalsOrDie("TEXT is ", "#more text#", "#" + Str.sliceOf(text, token.start(), token.stop()) + "#");
    }

    @Test
    public void testSimpleExpression() {
        TemplateParser parser = new BoonCoreTemplateParser();
        String text = "  blah   <c:if test='foo'>  ${body} </c:if> more text";
        //  012345678901234567890123456789012345678901234567890
        //  0         10        20        30        40        50
        parser.parse(text);
        final List<Token> tokenList = parser.getTokenList();
        Token token = tokenList.get(0);
        equalsOrDie("Token starts at 0", 0, token.start());
        equalsOrDie("Token stops at 9", 9, token.stop());
        equalsOrDie("Token is TEXT", TokenTypes.TEXT, token.type());
        equalsOrDie("TEXT is ", "  blah   ", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(1);
        equalsOrDie("Token starts at 12", 12, token.start());
        equalsOrDie("Token stops at 25", 25, token.stop());
        equalsOrDie("Token is COMMAND", TokenTypes.COMMAND, token.type());
        equalsOrDie("TEXT is ", "if test='foo'", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(2);
        equalsOrDie("Token starts at 26", 26, token.start());
        equalsOrDie("Token stops at 36", 36, token.stop());
        equalsOrDie("Token is COMMAND_BODY", TokenTypes.COMMAND_BODY, token.type());
        equalsOrDie("TEXT is ", "  ${body} ", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(3);
        equalsOrDie("Token starts at 26", 26, token.start());
        equalsOrDie("Token stops at 28", 28, token.stop());
        equalsOrDie("Token is TEXT", TokenTypes.TEXT, token.type());
        equalsOrDie("TEXT is ", "  ", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(4);
        equalsOrDie("Token starts at 30", 30, token.start());
        equalsOrDie("Token stops at 34", 34, token.stop());
        equalsOrDie("Token is EXPRESSION", TokenTypes.EXPRESSION, token.type());
        equalsOrDie("TEXT is ", "body", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(5);
        puts(token, Str.sliceOf(text, token.start(), token.stop()));
        equalsOrDie("Token starts at 35", 35, token.start());
        equalsOrDie("Token stops at 36", 36, token.stop());
        equalsOrDie("Token is TEXT", TokenTypes.TEXT, token.type());
        equalsOrDie("TEXT is ", " ", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(6);
        equalsOrDie("Token starts at 43", 43, token.start());
        equalsOrDie("Token stops at 53", 53, token.stop());
        equalsOrDie("Token is TEXT", TokenTypes.TEXT, token.type());
        equalsOrDie("TEXT is ", " more text", Str.sliceOf(text, token.start(), token.stop()));
    }

    @Test
    public void testTwoExpressions() {
        TemplateParser parser = new BoonCoreTemplateParser();
        String text = "  blah   <c:if test='foo'>  ${body}abc${bacon}zzz</c:if> more text";
        //  01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
        //  0         10        20        30        40        50       60
        parser.parse(text);
        final List<Token> tokenList = parser.getTokenList();
        Token token = tokenList.get(0);
        equalsOrDie("Token starts at 0", 0, token.start());
        equalsOrDie("Token stops at 9", 9, token.stop());
        equalsOrDie("Token is TEXT", TokenTypes.TEXT, token.type());
        equalsOrDie("TEXT is ", "  blah   ", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(1);
        equalsOrDie("Token starts at 12", 12, token.start());
        equalsOrDie("Token stops at 25", 25, token.stop());
        equalsOrDie("Token is COMMAND", TokenTypes.COMMAND, token.type());
        equalsOrDie("TEXT is ", "if test='foo'", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(2);
        equalsOrDie("Token starts at 26", 26, token.start());
        equalsOrDie("Token stops at 49", 49, token.stop());
        equalsOrDie("Token is COMMAND_BODY", TokenTypes.COMMAND_BODY, token.type());
        equalsOrDie("TEXT is ", "  ${body}abc${bacon}zzz", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(3);
        equalsOrDie("Token starts at 26", 26, token.start());
        equalsOrDie("Token stops at 28", 28, token.stop());
        equalsOrDie("Token is TEXT", TokenTypes.TEXT, token.type());
        equalsOrDie("TEXT is ", "  ", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(4);
        equalsOrDie("Token starts at 30", 30, token.start());
        equalsOrDie("Token stops at 34", 34, token.stop());
        equalsOrDie("Token is EXPRESSION", TokenTypes.EXPRESSION, token.type());
        equalsOrDie("TEXT is ", "body", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(5);
        equalsOrDie("Token starts at 35", 35, token.start());
        equalsOrDie("Token stops at 38", 38, token.stop());
        equalsOrDie("Token is TEXT", TokenTypes.TEXT, token.type());
        equalsOrDie("TEXT is ", "abc", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(6);
        equalsOrDie("Token starts at 40", 40, token.start());
        equalsOrDie("Token stops at 45", 45, token.stop());
        equalsOrDie("Token is EXPRESSION", TokenTypes.EXPRESSION, token.type());
        equalsOrDie("TEXT is ", "bacon", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(7);
        equalsOrDie("Token starts at 46", 46, token.start());
        equalsOrDie("Token stops at 49", 49, token.stop());
        equalsOrDie("Token is TEXT", TokenTypes.TEXT, token.type());
        equalsOrDie("TEXT is ", "zzz", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(8);
        equalsOrDie("Token starts at 46", 56, token.start());
        equalsOrDie("Token stops at 49", 66, token.stop());
        equalsOrDie("Token is TEXT", TokenTypes.TEXT, token.type());
        equalsOrDie("TEXT is ", " more text", Str.sliceOf(text, token.start(), token.stop()));
    }

    @Test
    public void testTight() {
        TemplateParser parser = new BoonCoreTemplateParser();
        String text = "<c:if test='foo'>body</c:if>";
        //  01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
        //  0         10        20        30        40        50        60        70        80        90
        parser.parse(text);
        final List<Token> tokenList = parser.getTokenList();
        ok = tokenList != null || die();
        Token token = tokenList.get(0);
        equalsOrDie("Token starts at 3", 3, token.start());
        equalsOrDie("Token stops at 16", 16, token.stop());
        equalsOrDie("Token is COMMAND", TokenTypes.COMMAND, token.type());
        equalsOrDie("TEXT is ", "if test='foo'", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(1);
        puts(token);
        equalsOrDie("Token starts at 17", 17, token.start());
        equalsOrDie("Token stops at 21", 21, token.stop());
        equalsOrDie("Token is COMMAND_BODY", TokenTypes.COMMAND_BODY, token.type());
        equalsOrDie("TEXT is ", "body", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(2);
        puts(token);
        equalsOrDie("Token starts at 17", 17, token.start());
        equalsOrDie("Token stops at 21", 21, token.stop());
        equalsOrDie("Token is TEXT", TokenTypes.TEXT, token.type());
        equalsOrDie("TEXT is ", "body", Str.sliceOf(text, token.start(), token.stop()));
    }

    @Test
    public void testBuggyTight() {
        TemplateParser parser = new BoonCoreTemplateParser();
        String text = "<c:if test='${flag}'>${name}</c:if>";
        //.............01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
        //.............0         10        20        30        40        50        60        70        80        90
        parser.parse(text);
        final List<Token> tokenList = parser.getTokenList();
        ok = tokenList != null || die();
        Token token = tokenList.get(0);
        equalsOrDie("Token starts at 3", 3, token.start());
        equalsOrDie("Token stops at 20", 20, token.stop());
        equalsOrDie("Token is COMMAND", TokenTypes.COMMAND, token.type());
        equalsOrDie("TEXT is ", "if test='${flag}'", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(1);
        puts(token);
        equalsOrDie("Token starts at 21", 21, token.start());
        equalsOrDie("Token stops at 28", 28, token.stop());
        equalsOrDie("Token is COMMAND_BODY", TokenTypes.COMMAND_BODY, token.type());
        equalsOrDie("TEXT is ", "${name}", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(2);
        puts(token);
        equalsOrDie("Token starts at 23", 23, token.start());
        equalsOrDie("Token stops at 27", 27, token.stop());
        equalsOrDie("Token is EXPRESSION", TokenTypes.EXPRESSION, token.type());
        equalsOrDie("TEXT is ", "name", Str.sliceOf(text, token.start(), token.stop()));
    }

    @Test
    public void testNoCurlyBrackets1() {
        TemplateParser parser = new BoonCoreTemplateParser();
        String text = "$name";
        //.............01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
        //.............0         10        20        30        40        50        60        70        80        90
        parser.parse(text);
        final List<Token> tokenList = parser.getTokenList();
        ok = tokenList != null || die();
        Token token = tokenList.get(0);
        equalsOrDie("Token starts at 1", 1, token.start());
        equalsOrDie("Token stops at 5", 5, token.stop());
        equalsOrDie("Token is EXPRESSION", TokenTypes.EXPRESSION, token.type());
        equalsOrDie("TEXT is ", "name", Str.sliceOf(text, token.start(), token.stop()));
    }

    @Test
    public void testNoCurlyBrackets() {
        TemplateParser parser = new BoonCoreTemplateParser();
        String text = "<c:if test='$flag'>$name </c:if>";
        //.............01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
        //.............0         10        20        30        40        50        60        70        80        90
        parser.parse(text);
        final List<Token> tokenList = parser.getTokenList();
        ok = tokenList != null || die();
        Token token = tokenList.get(0);
        equalsOrDie("Token starts at 3", 3, token.start());
        equalsOrDie("Token stops at 18", 18, token.stop());
        equalsOrDie("Token is COMMAND", TokenTypes.COMMAND, token.type());
        equalsOrDie("TEXT is ", "if test='$flag'", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(1);
        puts(token);
        equalsOrDie("Token starts at 19", 19, token.start());
        equalsOrDie("Token stops at 25", 25, token.stop());
        equalsOrDie("Token is COMMAND_BODY", TokenTypes.COMMAND_BODY, token.type());
        equalsOrDie("TEXT is ", "$name ", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(2);
        puts(token);
        equalsOrDie("Token starts at 20", 20, token.start());
        equalsOrDie("Token stops at 24", 24, token.stop());
        equalsOrDie("Token is EXPRESSION", TokenTypes.EXPRESSION, token.type());
        equalsOrDie("TEXT is ", "name", Str.sliceOf(text, token.start(), token.stop()));
    }

    @Test
    public void testNoCurlyBracketsNoSpace() {
        TemplateParser parser = new BoonCoreTemplateParser();
        String text = "<c:if test='$flag'>$name</c:if>";
        //.............01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
        //.............0         10        20        30        40        50        60        70        80        90
        parser.parse(text);
        final List<Token> tokenList = parser.getTokenList();
        ok = tokenList != null || die();
        Token token = tokenList.get(0);
        equalsOrDie("Token starts at 3", 3, token.start());
        equalsOrDie("Token stops at 18", 18, token.stop());
        equalsOrDie("Token is COMMAND", TokenTypes.COMMAND, token.type());
        equalsOrDie("TEXT is ", "if test='$flag'", Str.sliceOf(text, token.start(), token.stop()));
        token = tokenList.get(1);
        puts(token);
        equalsOrDie("Token starts at 19", 19, token.start());
        equalsOrDie("Token stops at 24", 24, token.stop());
        equalsOrDie("Token is COMMAND_BODY", TokenTypes.COMMAND_BODY, token.type());
        equalsOrDie("TEXT is ", "#$name#", "#" + Str.sliceOf(text, token.start(), token.stop()) + "#");
        token = tokenList.get(2);
        puts(token);
        equalsOrDie("Token starts at 20", 20, token.start());
        equalsOrDie("Token stops at 24", 24, token.stop());
        equalsOrDie("Token is EXPRESSION", TokenTypes.EXPRESSION, token.type());
        equalsOrDie("TEXT is ", "name", Str.sliceOf(text, token.start(), token.stop()));
    }
}
