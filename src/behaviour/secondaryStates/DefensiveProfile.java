package behaviour.secondaryStates;

import behaviour.primaryStates.PrimaryState;
import exceptions.SubStatusException;

import cz.cuni.amis.pogamut.base.agent.navigation.IPathPlanner;
import cz.cuni.amis.pogamut.base.communication.command.IAct;
import cz.cuni.amis.pogamut.base3d.worldview.IVisionWorldView;
import cz.cuni.amis.pogamut.base3d.worldview.object.ILocated;
import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.cuni.amis.pogamut.unreal.agent.navigation.IUnrealPathExecutor;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensomotoric.Raycasting;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensomotoric.Weaponry;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensor.AgentInfo;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensor.Items;
import cz.cuni.amis.pogamut.ut2004.bot.command.AdvancedLocomotion;
import cz.cuni.amis.pogamut.ut2004.bot.command.CompleteBotCommandsWrapper;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.SetCrouch;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.AutoTraceRay;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Player;


/**
 *
 * @author Francisco Aisa García
 */


public class DefensiveProfile extends SecondaryState {

    /**
     * Argument based constructor.
     * @param body body field from T800.
     * @param act act field from T800.
     * @param world world field from T800.
     * @param items items field from T800.
     * @param info info field from T800.
     * @param weaponry weaponry field from T800.
     * @param pathPlanner pathPlanner field from T800.
     * @param pathExecutor pathExecutor field from T800.
     * @param move move field from T800.
     * @param raycasting raycasting field from T800.
     * @param cardinalRayArray cardinalRayArray field from T800.
     */
    public DefensiveProfile (final CompleteBotCommandsWrapper body, final IAct act, final IVisionWorldView world,
                            final Items items, final AgentInfo info, final Weaponry weaponry,
                            final IPathPlanner <ILocated> pathPlanner, final IUnrealPathExecutor <ILocated> pathExecutor,
                            final AdvancedLocomotion move, final Raycasting raycasting, final AutoTraceRay cardinalRayArray []) {

        super (body, act, world, items, info, weaponry, pathPlanner, pathExecutor, move, raycasting, cardinalRayArray);
    }

    //__________________________________________________________________________

    /**
     * Retreat if the enemy is visible, throw an exception otherwise.
     * @param enemy Enemy.
     * @param facingSpot Location where want to make the bot face.
     * @throws SubStatusException Thrown when the enemy is not visible.
     */
    public void executeMovement (final Player enemy, final Location facingSpot) throws SubStatusException {

        if (PrimaryState.crouched) {
            act.act(new SetCrouch ().setCrouch (false));
            PrimaryState.crouched = false;
        }

        if (enemy == null) {
            throw new SubStatusException ("There are no visible enemies");
        }
        else {
            if (pathExecutor.isExecuting()) {
                pathExecutor.stop ();
            }

            Location enemyLocation = enemy.getLocation();

            // Check if there is enough space to run backwards
            if (info.getDistance(cardinalRayArray [PrimaryState.SOUTH].getHitLocation()) >= 400) {
                move.strafeTo(cardinalRayArray [PrimaryState.SOUTH].getHitLocation(), enemyLocation);
            }
            else {
                double southWestDistance = info.getDistance(cardinalRayArray [PrimaryState.SOUTHWEST].getHitLocation());
                double southEastDistance = info.getDistance(cardinalRayArray [PrimaryState.SOUTHEAST].getHitLocation());
                double westDistance = info.getDistance(cardinalRayArray [PrimaryState.WEST].getHitLocation());
                double eastDistance = info.getDistance(cardinalRayArray [PrimaryState.EAST].getHitLocation());

                if (southWestDistance > 400 || southEastDistance > 400) {
                    if (southWestDistance > southEastDistance) {
                        move.strafeTo(cardinalRayArray [PrimaryState.SOUTHWEST].getHitLocation(), enemyLocation);
                    }
                    else {
                        move.strafeTo(cardinalRayArray [PrimaryState.SOUTHEAST].getHitLocation(), enemyLocation);
                    }
                }
                else if (westDistance > 400 || eastDistance > 400) {
                    if (westDistance > eastDistance) {
                        move.strafeTo(cardinalRayArray [PrimaryState.WEST].getHitLocation(), enemyLocation);
                    }
                    else {
                        move.strafeTo(cardinalRayArray [PrimaryState.EAST].getHitLocation(), enemyLocation);
                    }
                }
                else {
                    move.moveContinuos ();
                    move.dodge (new Location (1, 0, 0), false);
                }
            }
        }
    }

    //__________________________________________________________________________

    /**
     * Converts to string the state's name.
     * @return The name of the state.
     */
    public String toString () {
        return "DefensiveProfile";
    }
}
