package nars.experiment.wander.brain.actions;

import nars.experiment.wander.Player;
import nars.experiment.wander.brain.Action;

public class TurnLeft extends Action {
	private static final long serialVersionUID = 1L;
	private final Player player;

	public TurnLeft(Player player) {
		this.player = player;
	}

	@Override
	public void execute() {
		player.turn(-Player.TURNING_ANGLE);
	}

}
