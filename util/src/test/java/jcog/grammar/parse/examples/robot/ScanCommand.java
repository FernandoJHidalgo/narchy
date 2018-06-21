package jcog.grammar.parse.examples.robot;

/**
 * Just for demonstration.
 */
class ScanCommand extends RobotCommand {
	/**
	 * Return a textual description of this object.
	 * 
	 * @return a textual description of this object
	 */
	public String toString() {
		return "scan " + location;
	}
}
