package behaviour.secondaryStates;

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
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.AutoTraceRay;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Player;


/**
 *
 * @author Francisco Aisa García
 */


public abstract class SecondaryState {

    // *************************************************************************
    //                             INSTANCE FIELDS
    // *************************************************************************


    /** Pointer to the body field from T800 */
    protected CompleteBotCommandsWrapper body;
    /** Pointer to the act field from T800 */
    protected IAct act;
    /** Pointer to the world field from T800 */
    protected IVisionWorldView world;
    /** Pointer to the items field from T800 */
    protected Items items;
    /** Pointer to the info field from T800 */
    protected AgentInfo info;
    /** Pointer to the weaponry field from T800 */
    protected Weaponry weaponry;
    /** Pointer to the pathPlanner field from T800 */
    protected IPathPlanner <ILocated> pathPlanner;
    /** Pointer to the pathExecutor field from T800 */
    protected IUnrealPathExecutor <ILocated> pathExecutor;
    /** Pointer to the move field from T800 */
    protected AdvancedLocomotion move;
    /** Pointer to the raycasting field from T800 */
    protected Raycasting raycasting;
    /** Pointer to the cardinalRayArray field from T800 */
    protected AutoTraceRay cardinalRayArray [];


    // *************************************************************************
    //                           STATIC FIELDS
    // *************************************************************************


    /** Location were the bot is headed */
    protected static Location destination;


    // *************************************************************************
    //                               METHODS
    // *************************************************************************


    /** static constructor */
    static {
        destination = null;
    }

    //__________________________________________________________________________

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
    protected SecondaryState (final CompleteBotCommandsWrapper body, final IAct act, final IVisionWorldView world,
                        final Items items, final AgentInfo info, final Weaponry weaponry,
                        final IPathPlanner <ILocated> pathPlanner, final IUnrealPathExecutor <ILocated> pathExecutor,
                        final AdvancedLocomotion move, final Raycasting raycasting, final AutoTraceRay cardinalRayArray []) {

        this.body = body;
        this.act = act;
        this.world = world;
        this.items = items;
        this.info = info;
        this.weaponry = weaponry;
        this.pathPlanner = pathPlanner;
        this.pathExecutor = pathExecutor;
        this.move = move;
        this.raycasting = raycasting;
        this.cardinalRayArray = cardinalRayArray;
    }

    //__________________________________________________________________________

    /**
     * Each sub state tells how the bot should move.
     * @param enemy Enemy.
     * @param facingSpot Location where we want to make the bot face.
     * @throws SubStatusException Thrown when the sub state can't be executed.
     */
    public abstract void executeMovement (final Player enemy, final Location facingSpot) throws SubStatusException;

    //__________________________________________________________________________

    /**
     * We have reached the desired destination.
     */
    public void destinationReached () {
        destination = null;
    }

    //__________________________________________________________________________

    /**
     * Stops execution.
     */
    public void stopExecution () {
        destination = null;
    }

    //__________________________________________________________________________

    /**
     * Converts to string the state's name.
     * @return The name of the state.
     */
    @Override
    public abstract String toString ();
}
