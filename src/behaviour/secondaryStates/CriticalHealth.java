package behaviour.secondaryStates;

import behaviour.primaryStates.PrimaryState;
import exceptions.SubStatusException;
import utilities.Arithmetic;

import cz.cuni.amis.pogamut.base.agent.navigation.IPathFuture;
import cz.cuni.amis.pogamut.base.agent.navigation.IPathPlanner;
import cz.cuni.amis.pogamut.base.communication.command.IAct;
import cz.cuni.amis.pogamut.base3d.worldview.IVisionWorldView;
import cz.cuni.amis.pogamut.base3d.worldview.object.ILocated;
import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.cuni.amis.pogamut.unreal.agent.navigation.IUnrealPathExecutor;
import cz.cuni.amis.pogamut.unreal.communication.messages.UnrealId;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensomotoric.Raycasting;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensomotoric.Weaponry;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensor.AgentInfo;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensor.Items;
import cz.cuni.amis.pogamut.ut2004.bot.command.AdvancedLocomotion;
import cz.cuni.amis.pogamut.ut2004.bot.command.CompleteBotCommandsWrapper;
import cz.cuni.amis.pogamut.ut2004.communication.messages.ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.SetCrouch;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.AutoTraceRay;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Item;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Player;
import java.util.Map;

import java.util.Set;


/**
 *
 * @author Francisco Aisa García
 */


public class CriticalHealth extends SecondaryState {

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
     */
    public CriticalHealth (final CompleteBotCommandsWrapper body, final IAct act, final IVisionWorldView world,
                         final Items items, final AgentInfo info, final Weaponry weaponry,
                         final IPathPlanner <ILocated> pathPlanner, final IUnrealPathExecutor <ILocated> pathExecutor,
                         final AdvancedLocomotion move, final Raycasting raycasting, final AutoTraceRay orientacionRayArray []) {

        super (body, act, world, items, info, weaponry, pathPlanner, pathExecutor, move, raycasting, orientacionRayArray);
    }

    //__________________________________________________________________________

    /**
     * Uses the data base to look for the closest health pack or health vial. If
     * there are no health vials or health packs available it throws an exception.
     * @param enemy Enemy.
     * @param facingSpot Location where want to make the bot face.
     * @throws SubStatusException Thrown when there are no health packs or health
     * vials available.
     */
    public void executeMovement (final Player enemy, final Location facingSpot) throws SubStatusException {

        if (PrimaryState.crouched) {
            act.act(new SetCrouch ().setCrouch (false));
            PrimaryState.crouched = false;
        }

        // If we haven't looked for health vials or health packs yet
        if (destination == null) {
             Map<UnrealId, Item> healthPackMap = items.getAllItems(ItemType.Group.HEALTH);
             Map<UnrealId, Item> healthVialMap = items.getAllItems(ItemType.Group.MINI_HEALTH);

            if (info.getHealth () < 100) {
                double minimumDistance = Arithmetic.INFINITY;
                double currentDistance = 0;

                // Look for health packs
                for (Item healthPack : healthPackMap.values()) {
                    if (healthPack != null) {
                        Location healthPackLocation = healthPack.getLocation ();

                        currentDistance = info.getDistance (healthPackLocation);
                        if (currentDistance < minimumDistance && items.isPickupSpawned(healthPack)) {
                            minimumDistance = currentDistance;
                            destination = healthPackLocation;
                        }
                    }
                }

                // If there weren't any health packs available, check for vials
                if (destination != null) {
                    for (Item healthVial : healthVialMap.values()) {
                        if (healthVial != null) {
                            Location healthVialLocation = healthVial.getLocation ();

                            currentDistance = info.getDistance (healthVialLocation);
                            if (currentDistance < minimumDistance && items.isPickupSpawned(healthVial)) {
                                minimumDistance = currentDistance;
                                destination = healthVialLocation;
                            }
                        }
                    }
                }
            }
            else {
                double minimumDistance = Arithmetic.INFINITY;
                double currentDistance = 0;

                // Look for health vials
                for (Item healthVial : healthVialMap.values()) {
                    if (healthVial != null) {
                        Location healthVialLocation = healthVial.getLocation ();

                        currentDistance = info.getDistance (healthVialLocation);
                        if (currentDistance < minimumDistance && items.isPickupSpawned(healthVial)) {
                            minimumDistance = currentDistance;
                            destination = healthVialLocation;
                        }
                    }
                }
            }

            if (destination == null) {
                throw new SubStatusException ("There are no available health items");
            }

            IPathFuture <ILocated> pathHandle = pathPlanner.computePath (info.getLocation(), destination);

            if (pathExecutor.isExecuting ()) {
                pathExecutor.stop ();
            }

            pathExecutor.followPath (pathHandle);
        }
    }

    //__________________________________________________________________________

    /**
     * Converts to string the state's name.
     * @return The name of the state.
     */
    public String toString () {
        return "CriticalHealth";
    }
}
