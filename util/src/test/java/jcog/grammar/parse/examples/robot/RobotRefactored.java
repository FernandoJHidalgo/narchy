package jcog.grammar.parse.examples.robot;

import jcog.grammar.parse.Alternation;
import jcog.grammar.parse.Parser;
import jcog.grammar.parse.Sequence;
import jcog.grammar.parse.tokens.CaselessLiteral;
import jcog.grammar.parse.tokens.Word;

/**
 * Provide an example of a class that affords a parser for 
 * the "robot" command language. This class is a refactored 
 * version of the <code>RobotMonolithic</code> class, with
 * one method for each subparser in the robot language.
 *
 * @author Steven J. Metsker
 *
 * @version 1.0 
 */
public class RobotRefactored {
	public Parser command() {
		Alternation a = new Alternation();
		a.get(pickCommand());
		a.get(placeCommand());
		a.get(scanCommand());
		return a;
	}

	protected Parser location() {
		return new Word();
	}

	protected Parser pickCommand() {
		Sequence s = new Sequence();
		s.get(new CaselessLiteral("pick"));
		s.get(new CaselessLiteral("carrier"));
		s.get(new CaselessLiteral("from"));
		s.get(location());
		return s;
	}

	protected Parser placeCommand() {
		Sequence s = new Sequence();
		s.get(new CaselessLiteral("place"));
		s.get(new CaselessLiteral("carrier"));
		s.get(new CaselessLiteral("at"));
		s.get(location());
		return s;
	}

	protected Parser scanCommand() {
		Sequence s = new Sequence();
		s.get(new CaselessLiteral("scan"));
		s.get(location());
		return s;
	}
}
