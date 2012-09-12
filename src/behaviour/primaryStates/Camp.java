package behaviour.primaryStates;

import knowledge.EnemyInfo;
import cz.cuni.amis.pogamut.base.agent.navigation.IPathPlanner;
import cz.cuni.amis.pogamut.base.communication.command.IAct;
import cz.cuni.amis.pogamut.base3d.worldview.IVisionWorldView;
import cz.cuni.amis.pogamut.base3d.worldview.object.ILocated;
import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.cuni.amis.pogamut.unreal.agent.navigation.IUnrealPathExecutor;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensomotoric.Raycasting;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensomotoric.Weaponry;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensor.AgentInfo;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensor.Game;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensor.Items;
import cz.cuni.amis.pogamut.ut2004.bot.command.AdvancedLocomotion;
import cz.cuni.amis.pogamut.ut2004.bot.command.CompleteBotCommandsWrapper;
import cz.cuni.amis.pogamut.ut2004.bot.command.ImprovedShooting;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.SetCrouch;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.AutoTraceRay;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Player;


/**
 *
 * @author Francisco Aisa García
 */


public class Camp extends PrimaryState {

    // *************************************************************************
    //                                  METHODS
    // *************************************************************************

    /**
     * Argument based constructor.
     * @param body body field from T800.
     * @param act act field from T800.
     * @param world world field from T800.
     * @param game game field from T800.
     * @param items items field from T800.
     * @param info info field from T800.
     * @param weaponry weaponry field from T800.
     * @param pathPlanner pathPlanner field from T800.
     * @param pathExecutor pathExecutor field from T800.
     * @param move move field from T800.
     * @param raycasting raycasting field from T800.
     * @param cardinalRayArray cardinalRayArray field from T800.
     * @param shoot shoot field from T800.
     */
    public Camp (final CompleteBotCommandsWrapper body, final IAct act, final IVisionWorldView world,
                    final Game game, final Items items, final AgentInfo info, final Weaponry weaponry,
                    final IPathPlanner <ILocated> pathPlanner, final IUnrealPathExecutor <ILocated> pathExecutor,
                    final AdvancedLocomotion move, final Raycasting raycasting, final AutoTraceRay cardinalRayArray [],
                    final ImprovedShooting shoot) {

        super (body, act, world, game, items, info, weaponry, pathPlanner, pathExecutor, move, raycasting, cardinalRayArray, shoot);
    }

    //__________________________________________________________________________

    /**
     * Makes the bot crouch. It crouches to improve his shooting and camp easily.
     * @param enemy Enemy.
     * @param facingSpot Location that the bot has to face.
     * @param enemyInfo Guess/Known information about the enemy.
     */
    public void stateDrivenMovement (final Player enemy, final Location facingSpot, final EnemyInfo enemyInfo) {

        if (!crouched) {
            if (pathExecutor.isExecuting()) {
                pathExecutor.stop ();
            }
            else if (move.isRunning()) {
                move.stopMovement();
            }

            act.act(new SetCrouch ().setCrouch(true));
            crouched = true;
        }

        if (enemy != null) {
            move.turnTo(enemy);
        }
    }

    //__________________________________________________________________________

    /**
     * Converts to string the state's name.
     * @return The name of the state.
     */
    public String toString () {
        return "Camp";
    }
}
