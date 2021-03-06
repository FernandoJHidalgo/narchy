package nars.experiment.pacman.entities;

import nars.experiment.pacman.maze.Maze;
import nars.experiment.pacman.maze.Maze.Direction;

public class Player extends Entity {

    public final static int MAX_POWER = 500;
    public int mouthAngle = 10;
    int mouthSpeed = 2;
    final static int MOUTH_WIDTH = 40;
    public int power;


    public Player(Maze maze, int x, int y, float speed) {

        super(maze, x, y);

        this.speed = speed;
        this.lead = 0.4;

        power = 0;

    }

    @Override
    public void update() {

        super.update();

        power = Math.max(0, power - 1);

        try {

            if (maze.dots[(int) (x / 2)][(int) (y / 2)]) {

                maze.dots[(int) (x / 2)][(int) (y / 2)] = false;
                maze.dotCount--;

                if (maze.isBigFood(2 * (int) (x / 2) + 1, 2 * (int) (y / 2) + 1))
                    power = MAX_POWER;

            }

        } catch (ArrayIndexOutOfBoundsException e) {

        }

        mouthAngle += mouthSpeed;
        if (mouthAngle >= MOUTH_WIDTH || mouthAngle <= 0)
            mouthSpeed = -mouthSpeed;

    }

    public void turn(Direction d) {

        if (!walled(d)) {

            this.dir = d;

        }

    }

    public boolean die() {


        this.x = maze.playerStart().x;
        this.y = maze.playerStart().y;
        this.mouthAngle = 5;
        this.dir = Direction.right;

        return false;

    }

    public boolean deathAnimation() {

        if (mouthAngle < 180)
            mouthAngle = Math.min(180, mouthAngle + 5);
        else
            return true;

        return false;

    }

}
