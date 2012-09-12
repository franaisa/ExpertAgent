package bot;

import behaviour.primaryStates.*;
import behaviour.secondaryStates.*;
import brain.Brain;
import cz.cuni.amis.pogamut.base.agent.navigation.IPathExecutorState;
import cz.cuni.amis.pogamut.base.communication.worldview.listener.annotation.EventListener;
import cz.cuni.amis.pogamut.base.communication.worldview.listener.annotation.ObjectClassEventListener;
import cz.cuni.amis.pogamut.base.communication.worldview.object.event.WorldObjectUpdatedEvent;
import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.cuni.amis.pogamut.base3d.worldview.object.event.WorldObjectAppearedEvent;
import cz.cuni.amis.pogamut.base3d.worldview.object.event.WorldObjectDisappearedEvent;
import cz.cuni.amis.pogamut.unreal.communication.messages.UnrealId;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.stuckdetector.UT2004PositionHistoryStuckDetector;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004Bot;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004BotModuleController;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Combo;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Configuration;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Initialize;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.RemoveRay;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.*;
import cz.cuni.amis.pogamut.ut2004.utils.UT2004BotRunner;
import cz.cuni.amis.pogamut.ut2004.utils.UnrealUtils;
import cz.cuni.amis.utils.exception.PogamutException;
import cz.cuni.amis.utils.flag.FlagListener;
import java.util.logging.Level;
import javax.vecmath.Vector3d;
import knowledge.EnemyInfo;
import utilities.Arithmetic;
import utilities.Initialization;


/**
 *
 * @author Francisco Aisa García
 * @version 1.0.2
 */


public class Bot extends UT2004BotModuleController {

    // *************************************************************************
    //                             INSTANCE FIELDS
    // *************************************************************************


    /** Array containing all the primary states */
    private PrimaryState primaryStateArray [];
    /** Array containing all the secondary states */
    private SecondaryState secondaryStateArray [];
    /** Temporary information (that we know or have guessed) about the enemy */
    private EnemyInfo enemyInfo;
    /** It is the brains of the bot, it decides when to switch from one state to another */
    private Brain brain;
    /** Vector containing all the path nodes from the current map */
    public static NavPoint pathNodes [];
    /** Vector containing all the important areas of the current map */
    public static NavPoint areas [];
    /** Location of a feasible spam or combo */
    private Location bullseye = null;
    /** Enemy's information (it is null when we are not seeing the enemy) */
    private Player enemy = null;
    /** Used to tell the bot where he should be facing (useful when using pathExecutor) */
    private Location facingSpot = null;
    /** It represents the destination where we want to make the bot go (no matter what) */
    private Location destination = null;
    /** True if we have just killed an enemy */
    private boolean enemyKilled = false;
    /** Memorable quotes to be said by the T800 */
    private String memorableQuotes [] = {"Quitate que te avian!", "Piojoso!", "So mugroso!",
                         "Pero esto que es? PERO ESTO QUE ES??", "Ahora vas y lo cascas!",
                         "Ho haveu vist?", "Se va habe un follon que no sabe ni donde sa metio!",
                         "Ole tu ole tu", "Claro que si campeon",
                         "Po no se"};

    // Contiene los rayos a cada una de las direcciones cardinales
    // 0 = Norte, 1 = Nor-este, 2 = Este, 3 = Sur-este, 4 = Sur, 5 = Sur-oeste
    // 6 = Oeste, 7 = Nor-oeste
    /** Vector containing all the rays (@see AutoTraceRay.class). To make it easier,
     *  we will use constants to reference certain rays in the vector.
     *  NORTH RAY = 0
     *  NORTHEAST RAY = 1
     *  EAST RAY = 2
     *  SOUTHEAST RAY = 3
     *  SOUTH RAY = 4
     *  SOUTHWEST RAY = 5
     *  WEST RAY = 6
     *  NORTHWEST RAY = 7
     */
    private AutoTraceRay cardinalRayArray [] = new AutoTraceRay [8];


    // *************************************************************************
    //                            CONSTANTS
    // *************************************************************************


    // CONSTANTS FOR RAYCASTING

    /** North ray identifier */
    public static final String FRONT   = "frontRay";
    /** North east ray identifier */
    public static final String RIGHT45 = "rightRay45";
    /** East ray identifier */
    public static final String RIGHT = "rightRay";
    /** Sourth east ray identifier */
    public static final String BACKRIGHT45 = "backRightRay45";
    /** South ray identifier */
    public static final String BACK = "backRay";
    /** South west ray identifier */
    public static final String BACKLEFT45 = "backLeftRay45";
    /** West ray identifier */
    public static final String LEFT  = "leftRay";
    /** North west ray identifier */
    public static final String LEFT45  = "leftRay45";

    // CONSTANTS FOR STATES

    /** Identifies the Attack state in the primary states vector */
    public static final int ATTACK = 0;
    /** Identifies the Retreat state in the primary states vector */
    public static final int RETREAT = 1;
    /** Identifies the Hunt state in the primary states vector */
    public static final int HUNT = 2;
    /** Identifies the Greedy state in the primary states vector */
    public static final int GREEDY = 3;
    /** Identifies the Camp state in the primary states vector */
    public static final int CAMP = 4;

    // CONSTANTS FOR SUBSTATES

    /** Identifies the Disabled sub state in the secondary states vector (note that it is null, hence IT CAN'T BE USED) */
    public static final int DISABLED = 0;
    /** Identifies the Defensive Profile sub state in the secondary states vector */
    public static final int DEFENSIVEPROFILE = 1;
    /** Identifies the Ofensive Profile sub state in the secondary states vector */
    public static final int OFENSIVEPROFILE = 2;
    /** Identifies the Pickup Weapon sub state in the secondary states vector */
    public static final int PICKUPWEAPON = 3;
    /** Identifies the Pickup Ammo sub state in the secondary states vector */
    public static final int PICKUPAMMO = 4;
    /** Identifies the Pickup Health sub state in the secondary states vector */
    public static final int PICKUPHEALTH = 5;
    /** Identifies the Critical Health sub state in the secondary states vector */
    public static final int CRITICALHEALTH = 6;
    /** Identifies the Critical Weaponry sub state in the secondary states vector */
    public static final int CRITICALWEAPONRY = 7;


    // *************************************************************************
    //                                METHODS
    // *************************************************************************


    @Override
    public void prepareBot (UT2004Bot bot) {
        // TODO used for initialization, initialize agent modules here

        enemyInfo = new EnemyInfo (body);
        brain = new Brain (body);

        primaryStateArray = new PrimaryState [5];
        primaryStateArray [ATTACK] = new Attack (body, act, world, game, items, info, weaponry, pathPlanner, pathExecutor, move, raycasting, cardinalRayArray, shoot);
        primaryStateArray [RETREAT] = new Retreat (body, act, world, game, items, info, weaponry, pathPlanner, pathExecutor, move, raycasting, cardinalRayArray, shoot);
        primaryStateArray [HUNT] = new Hunt (body, act, world, game, items, info, weaponry, pathPlanner, pathExecutor, move, raycasting, cardinalRayArray, shoot);
        primaryStateArray [GREEDY] = new Greedy (body, act, world, game, items, info, weaponry, pathPlanner, pathExecutor, move, raycasting, cardinalRayArray, shoot);
        primaryStateArray [CAMP] = new Camp (body, act, world, game, items, info, weaponry, pathPlanner, pathExecutor, move, raycasting, cardinalRayArray, shoot);

        secondaryStateArray = new SecondaryState [8];
        secondaryStateArray [DISABLED] = null;
        secondaryStateArray [DEFENSIVEPROFILE] = new DefensiveProfile (body, act, world, items, info, weaponry, pathPlanner, pathExecutor, move, raycasting, cardinalRayArray);
        secondaryStateArray [OFENSIVEPROFILE] = new OfensiveProfile (body, act, world, items, info, weaponry, pathPlanner, pathExecutor, move, raycasting, cardinalRayArray);
        secondaryStateArray [PICKUPWEAPON] = new PickupWeapon (body, act, world, items, info, weaponry, pathPlanner, pathExecutor, move, raycasting, cardinalRayArray);
        secondaryStateArray [PICKUPAMMO] = new PickupAmmo (body, act, world, items, info, weaponry, pathPlanner, pathExecutor, move, raycasting, cardinalRayArray);
        secondaryStateArray [PICKUPHEALTH] = new PickupHealth (body, act, world, items, info, weaponry, pathPlanner, pathExecutor, move, raycasting, cardinalRayArray);
        secondaryStateArray [CRITICALHEALTH] = new CriticalHealth (body, act, world, items, info, weaponry, pathPlanner, pathExecutor, move, raycasting, cardinalRayArray);
        secondaryStateArray [CRITICALWEAPONRY] = new CriticalWeaponry (body, act, world, items, info, weaponry, pathPlanner, pathExecutor, move, raycasting, cardinalRayArray);
    }

    //__________________________________________________________________________

    @Override
    public Initialize getInitializeCommand () {
        // TODO init bot's params there

        // Initialize the bot's name
        return new Initialize ().setName ("Paquito-V001");
    }

    //__________________________________________________________________________

    /**
    * The bot is initialized in the environment - a physical representation of the
    * bot is present in the game.
    *
    * @param config information about configuration
    * @param init information about configuration
    */
    @SuppressWarnings ("unchecked")
    @Override
    public void botInitialized (GameInfo gameInfo, ConfigChange config, InitedMessage init) {
    	// Initialize listeners for pathExecutor
        initializePathListeners ();
        // Initialize raycasting
        initializeRayCasting ();
        // Get all the path nodes in the current map
        pathNodes = Initialization.initializePathNodes (world);
        // Get all the important areas in the current map
        areas = Initialization.initializeAreas (items);
    }

    //__________________________________________________________________________

    @Override
    public void botSpawned (GameInfo gameInfo, ConfigChange config, InitedMessage init, Self self) {
        // bot is spawned for the first time in the environment
        // examine 'self' to examine current bot's location and other stuff
        // receive logs from the path executor so you can get a grasp on how it is working
        pathExecutor.getLog ().setLevel (Level.ALL);
    }

    //__________________________________________________________________________

    private int previousPrimaryState = HUNT;
    private int previousSecondaryState = DISABLED;

    /** Bot's primary state */
    private int primaryState = HUNT;
    /** Bot's secondary state */
    private int secondaryState = DISABLED;


    // *************************************************************************
    //                   THREAD THAT EXECUTES BOT'S LOGIC
    // *************************************************************************



    @Override
    public void logic () throws PogamutException {
        // Which are the next states?
        primaryState = brain.estimatePrimaryState(info, weaponry, enemy, enemyInfo, game);
        secondaryState = brain.estimateSecondaryState(primaryState, info, weaponry, enemy, enemyInfo);

        /*if (primaryState != previousPrimaryState || secondaryState != previousSecondaryState) {
            previousPrimaryState = primaryState;
            previousSecondaryState = secondaryState;

            body.getCommunication().sendGlobalTextMessage("primary = " + primaryStateArray [primaryState].toString ());

            if (secondaryState != DISABLED) {
                body.getCommunication().sendGlobalTextMessage("secondary = " + secondaryStateArray [secondaryState].toString ());
            }
            else {
                body.getCommunication().sendGlobalTextMessage("secondary = DISABLED");
            }
        }*/

        // Must we go somwhere?
        destination = brain.estimateDestination (info, enemy, weaponry, items);

        // Should we be facing anything?
        facingSpot = enemy != null? enemy.getLocation () : null;

        // Should we blow a combo or shoot a spam?
        bullseye = brain.estimateTarget ();

        // Switch to best weapon, move and shoot (if necessary)
        primaryStateArray [primaryState].switchToBestWeapon (enemy, enemyInfo);
        primaryStateArray [primaryState].executeMovement (secondaryStateArray [secondaryState], destination, enemy, facingSpot, enemyInfo);
        primaryStateArray [primaryState].engage (enemy, bullseye);

        // Reset temporary information
        brain.resetTempInfo ();
    }


    // *************************************************************************
    //                                LISTENERS
    // *************************************************************************


    @ObjectClassEventListener (eventClass = WorldObjectAppearedEvent.class, objectClass = Item.class)
    protected void objectAppeared (WorldObjectAppearedEvent <Item> event) {
        /* Nothing to do yet, although some of the primary states and secondary
           states check the visible items through a pogamut module (which is
           less efficient). In the future this listener could be used to trigger
           actions.
        */

        //Item item = event.getObject ();
    }

    //__________________________________________________________________________

    /**
     * It gets triggered whenever we gain adrenaline (by killing or picking it up)
     * @param event AdrenalineGained event.
     */
    @EventListener (eventClass = AdrenalineGained.class)
    protected void adrenalineGained (AdrenalineGained event) {
        if (info.isAdrenalineFull()) {
            if (info.getHealth() < 125) {
                act.act (new Combo ().setType("xGame.ComboDefensive"));
            }
            else {
                act.act (new Combo ().setType("xGame.ComboBerserk"));
            }
        }
    }

    /**
     * Whenever an object of a certain type gets in or out of our line of vision
     * it gets triggered. In this particular case, it gets triggered when the object
     * is of type Player.
     * @param event Player appeared..
     */
    @ObjectClassEventListener (eventClass = WorldObjectAppearedEvent.class, objectClass = Player.class)
    protected void playerAppeared (WorldObjectAppearedEvent<Player> event) {
        // Update enemy information
        enemy = event.getObject ();
        // Update the last known location of the enemy
        enemyInfo.updateEnemyLocation(enemy.getLocation (), game.getTime ());
    }

    //__________________________________________________________________________

    /**
     * Whenever an object is updated, it gets triggered. In this particular case,
     * it gets triggered when the object is of type Player.
     * @param event Player updated..
     */
    @ObjectClassEventListener (eventClass = WorldObjectUpdatedEvent.class, objectClass = Player.class)
    protected void playerUpdated (WorldObjectUpdatedEvent<Player> event) {
        // Update enemy information
        // Note that it is null during the handshake, hence, if we are going to use
        // getLocation we should check if it is null first.
        enemy = event.getObject ();
        Location enemyLocation = enemy.getLocation ();

        if (enemyLocation == null) {
            enemyInfo.updateEnemyLocation (null, -1);
        }
        else {
            enemyInfo.updateEnemyLocation (enemyLocation, game.getTime ());
        }

        enemyInfo.updateWeapon (enemy.getWeapon ());
        if (enemyKilled){
            enemyInfo.reset();
            enemyKilled = false;
        }
    }


    //__________________________________________________________________________

    /**
     * Whenever an object gets out of sight, it gets triggered. In this particular
     * case, it gets triggered when the enemy gets out of sight.
     * @param event Player dissappeared.
     */
    @ObjectClassEventListener (eventClass = WorldObjectDisappearedEvent.class, objectClass = Player.class)
    protected void playerDisappeared (WorldObjectDisappearedEvent<Player> event) {
        enemy = null;
        // Update relevant information in the states
        primaryStateArray [primaryState].playerDisappeared ();
    }

    //__________________________________________________________________________

    /**
     * Whenever a projectile is in the bot's field of vision, it gets triggered.
     * Note that projectiles can also be the ones we shoot.
     * @param event Projectile coming.
     */
    @ObjectClassEventListener (eventClass = WorldObjectUpdatedEvent.class, objectClass = IncomingProjectile.class)
    protected void incomingProjectile (WorldObjectUpdatedEvent<IncomingProjectile> event) {
        // Get the object of type IncomingProjectile
        IncomingProjectile projectile = event.getObject ();
        // Classifie the projectile
        brain.incomingProjectile (projectile, enemy);

        /*if (projectile.getType().equals("XWeapons.RocketProj")) {
            body.getCommunication().sendGlobalTextMessage("Viene un pepino");

            if(info.getDistance(projectile.getLocation()) < 800) {
                // Estimate vector 3d

                body.getCommunication().sendGlobalTextMessage("Intento esquivarlo");
                move.dodgeBack (info.getLocation(), projectile.getLocation());
            }
        }*/
    }

    //__________________________________________________________________________

    /**
     * Whenever we get hit, it gets triggered.
     * @param event BotDamaged event.
     */
    @EventListener (eventClass = BotDamaged.class)
    protected void botDamaged (BotDamaged event) {
        // Trigger the non cognitive behaviour of the bot in response to the hit
        primaryStateArray [primaryState].botDamaged (event, game.getTime (), enemy);
    }

    //__________________________________________________________________________

    /**
     * Whenever the bot hears a noise, it gets triggered (usually when he hears another
     * bot/user).
     * @param event HearNoise event.
     */
    @EventListener (eventClass = HearNoise.class)
    protected void hearNoise (HearNoise event) {
        // Trigger the non cognitive behaviour of the bot in response to the noise
        primaryStateArray [primaryState].hearNoise (event, game.getTime (), enemy);
    }

    //__________________________________________________________________________

    /**
     * Whenever an item is picked up, it gets triggered.
     * Note that it also gets triggered when the bot picks up things (he hears it).
     * @param event HearPickup event.
     */
    @EventListener (eventClass = HearPickup.class)
    protected void hearPickup (HearPickup event) {
        // Update enemy's information
        enemyInfo.hearPickup (event, info, game, items);
        // Trigger the non cognitive behaviour of the bot in response to the noise
        primaryStateArray [primaryState].hearPickup (event, game.getTime (), enemy);
    }

    //__________________________________________________________________________

    /**
     * Whenever a player dies, it gets triggered.
     * @param event PlayerKilled event.
     */
    @EventListener (eventClass = PlayerKilled.class)
    protected void playerKilled (PlayerKilled event) {
        // If we have killed him, let's say a charming comment :P
        UnrealId killerId = event.getKiller ();
        if (killerId != null && info.getId ().equals (killerId)) {
            if (Math.random () < 0.5) {
                int randomQuote = Arithmetic.doRandomNumber (0, memorableQuotes.length - 1);
                body.getCommunication ().sendGlobalTextMessage (memorableQuotes [randomQuote]);
            }
        }

        // Reset enemy's information
        enemyInfo.reset ();
        enemyKilled = true;
    }

    //__________________________________________________________________________

    /**
     * Whenever a user/bot joins the match it gets triggered.
     * @param event PlayerJoinsGame event.
     */
    @EventListener (eventClass = PlayerJoinsGame.class)
    protected void playerJoinedGame (PlayerJoinsGame event) {
        enemyInfo.setName (event.getName ());
    }

    //__________________________________________________________________________

    /**
     * Whenever a user/bot leaves the match it gets triggered.
     * @param event PlayerLeft event.
     */
    @EventListener (eventClass = PlayerLeft.class)
    protected void playerLeft (PlayerLeft event) {
        enemyInfo.eraseName ();
        enemyInfo.reset ();
    }

    //__________________________________________________________________________

    /**
     * When the match ends it gets triggered.
     * @param event MapFinished event.
     */
    @EventListener (eventClass = MapFinished.class)
    protected void mapFinished (MapFinished event) {
        if (pathExecutor.isExecuting ()) {
            pathExecutor.stop ();
        }
    }

    //__________________________________________________________________________

    /**
     * Whenever a bot gets hit it gets triggered.
     * @param event PlayerDamaged event.
     */
    @EventListener (eventClass = PlayerDamaged.class)
    protected void playerDamaged (PlayerDamaged event) {
        // Depending on what hit the player, we may have to consider certain strategies
        primaryStateArray [primaryState].playerDamaged (event);
        // Update the amount of damage we assume the enemy just lost
        enemyInfo.hit (event.getDamage ());
    }

    //__________________________________________________________________________

    /**
     * Whenever the bot spawns it gets triggered.
     * @param event Spawn event.
     */
    @EventListener (eventClass = Spawn.class)
    public void newSpawn (Spawn event) {
        // Empty
    }

    //__________________________________________________________________________

    /**
     * Whenever the bot dies it gets trigered.
     * @param event BotKilled event.
     */
    @Override
    public void botKilled (BotKilled event) {
        primaryStateArray [primaryState].stopExecution (secondaryStateArray [secondaryState]);
        // Reset temporary information
        PrimaryState.resetTempInfo ();
    }


    // *************************************************************************
    //                             OTHER METHODS
    // *************************************************************************


    /**
     * Initialize pathListener. Depending on what happens during execution of a plan
     * the pathListener will raise certain flags. For example, if we reach our destination
     * a flag will be raised, if we get stuck another flag will be raised on so on.
     */
    protected void initializePathListeners () {
    	// add stuck detector that watch over the path-following, if it (heuristicly) finds out that the bot has stuck somewhere,
    	// it reports an appropriate path event and the path executor will stop following the path which in turn allows
    	// us to issue another follow-path command in the right time

        //pathExecutor.addStuckDetector (new UT2004TimeStuckDetector(bot, 3.0));       // if the bot does not move for 3 seconds, considered that it is stuck

        pathExecutor.addStuckDetector (new UT2004PositionHistoryStuckDetector (bot)); // watch over the position history of the bot, if the bot does not move sufficiently enough, consider that it is stuck

        // IMPORTANT
        // adds a listener to the path executor for its state changes, it will allow you to
        // react on stuff like "PATH TARGET REACHED" or "BOT STUCK"
        pathExecutor.getState ().addStrongListener (new FlagListener<IPathExecutorState> () {
            @Override
            public void flagChanged (IPathExecutorState changedValue) {
                switch (changedValue.getState ()) {
                    // If the computation fails
                    case PATH_COMPUTATION_FAILED:
                        body.getCommunication().sendGlobalTextMessage("PATH_COMPUTATION_FAILED!!!!!");

                        //break;
                    // If we reach the destination
                    case TARGET_REACHED:
                        //body.getCommunication ().sendGlobalTextMessage ("TARGET_REACHED");

                        // If the spot where we are is the one that skynet wants us to go
                        // we set it to null
                        if (destination != null && info.getDistance (destination) < 50) {
                            destination = null;
                        }

                        // Notify the primary state that we have reached the destination
                        primaryStateArray [primaryState].destinationReached (secondaryStateArray [secondaryState]);

                        break;
                    // If we are stuck
                    case STUCK:
                        //body.getCommunication().sendGlobalTextMessage ("STUCK");

                        // Set destination to null
                        destination = null;

                        // Notify the primary state that we are stucked
                        primaryStateArray [primaryState].botStuck (secondaryStateArray[secondaryState]);

                        break;
                }
            }
        });
    }

    //__________________________________________________________________________

    /**
     * Initialize raycasting.
     */
    protected void initializeRayCasting () {
        // initialize rays for raycasting
        final int rayLength = (int) (UnrealUtils.CHARACTER_COLLISION_RADIUS * 10);
        final int backRayLength = 100000;

        // settings for the rays
        // fastTrace = true, nos proporciona informacion solo sobre colision
        // aunque es la version mas rapida
        boolean fastTrace = false;        // perform only fast trace == we just need true/false information
        boolean floorCorrection = true;   // provide floor-angle correction for the ray (when the bot is running on the skewed floor, the ray gets rotated to match the skew)
        boolean traceActor = false;       // whether the ray should collid with other actors == bots/players as well

        // 1. remove all previous rays, each bot starts by default with three
        // rays, for educational purposes we will set them manually
        getAct ().act (new RemoveRay ("All"));

        // 2. create new rays
        raycasting.createRay (FRONT,   new Vector3d (1, 0, 0),  rayLength, fastTrace, floorCorrection, traceActor);
        raycasting.createRay (RIGHT45, new Vector3d (1, 1, 0),  rayLength, fastTrace, floorCorrection, traceActor);
        raycasting.createRay (RIGHT, new Vector3d (0, 1, 0),  backRayLength, fastTrace, floorCorrection, traceActor);
        raycasting.createRay (BACKRIGHT45, new Vector3d (-1, 1, 0), backRayLength, fastTrace, floorCorrection, traceActor);
        raycasting.createRay (BACK, new Vector3d (-1, 0, 0), backRayLength, fastTrace, floorCorrection, traceActor);
        raycasting.createRay (BACKLEFT45, new Vector3d (-1, -1, 0), backRayLength, fastTrace, floorCorrection, traceActor);
        raycasting.createRay (LEFT,  new Vector3d (0, -1, 0), backRayLength, fastTrace, floorCorrection, traceActor);
        raycasting.createRay (LEFT45,  new Vector3d (1, -1, 0), rayLength, fastTrace, floorCorrection, traceActor);

        // register listener called when all rays are set up in the UT engine
        raycasting.getAllRaysInitialized ().addListener (new FlagListener <Boolean> () {

            public void flagChanged (Boolean changedValue) {
                // once all rays were initialized store the AutoTraceRay objects
                // that will come in response in local variables, it is just
                // for convenience
                cardinalRayArray [0] = raycasting.getRay (FRONT);
                cardinalRayArray [1] = raycasting.getRay (RIGHT45);
                cardinalRayArray [2] = raycasting.getRay (RIGHT);
                cardinalRayArray [3] = raycasting.getRay (BACKRIGHT45);
                cardinalRayArray [4] = raycasting.getRay (BACK);
                cardinalRayArray [5] = raycasting.getRay (BACKLEFT45);
                cardinalRayArray [6] = raycasting.getRay (LEFT);
                cardinalRayArray [7] = raycasting.getRay (LEFT45);
            }
        });
        // have you noticed the FlagListener interface? The Pogamut is often using {@link Flag} objects that
        // wraps some iteresting values that user might respond to, i.e., whenever the flag value is changed,
        // all its listeners are informed

        // 3. declare that we are not going to setup any other rays, so the 'raycasting' object may know what "all" is
        raycasting.endRayInitSequence ();

        // change bot's default speed
        //config.setSpeedMultiplier(0.2f);

        // IMPORTANT:
        // The most important thing is this line that ENABLES AUTO TRACE functionality,
        // without ".setAutoTrace(true)" the AddRay command would be useless as the bot won't get
        // trace-lines feature activated
        getAct ().act (new Configuration ().setDrawTraceLines (false).setAutoTrace (true));

        // FINAL NOTE: the ray initialization must be done inside botInitialized method or later on inside
        //             botSpawned method or anytime during doLogic method
    }


    // *************************************************************************
    //                                 MAIN
    // *************************************************************************


    /**
     * This method is called when the bot is started either from IDE or from command line.
     * It connects the bot to the game server.
     * @param args
     */
    public static void main (String args[]) throws PogamutException, Exception {
        // Launch a local bot
        new UT2004BotRunner (Bot.class, "T800").setMain (true).startAgent ();
        // Launch a remote bot
        //new UT2004BotRunner (T800.class, "T800", "<server ip>", 3000).setMain(true).startAgent();
    }
}
