package behaviour.primaryStates;

import behaviour.secondaryStates.SecondaryState;
import knowledge.EnemyInfo;
import exceptions.SubStatusException;
import utilities.Arithmetic;

import cz.cuni.amis.pogamut.unreal.agent.navigation.IUnrealPathExecutor;
import cz.cuni.amis.pogamut.base.agent.navigation.IPathFuture;
import cz.cuni.amis.pogamut.base.agent.navigation.IPathPlanner;
import cz.cuni.amis.pogamut.base.communication.command.IAct;
import cz.cuni.amis.pogamut.base3d.worldview.IVisionWorldView;
import cz.cuni.amis.pogamut.base3d.worldview.object.ILocated;
import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensomotoric.Raycasting;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensomotoric.Weapon;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensomotoric.Weaponry;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensor.AgentInfo;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensor.Game;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensor.Items;
import cz.cuni.amis.pogamut.ut2004.bot.command.AdvancedLocomotion;
import cz.cuni.amis.pogamut.ut2004.bot.command.CompleteBotCommandsWrapper;
import cz.cuni.amis.pogamut.ut2004.bot.command.ImprovedShooting;
import cz.cuni.amis.pogamut.ut2004.communication.messages.ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.AutoTraceRay;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.BotDamaged;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.HearNoise;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.HearPickup;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Item;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Player;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.PlayerDamaged;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 *
 * @author Francisco Aisa García
 */


public abstract class PrimaryState {

    // *************************************************************************
    //                          INSTANCE FIELDS
    // *************************************************************************


    /** Pointer to the body field from T800 */
    protected CompleteBotCommandsWrapper body;
    /** Pointer to the act field from T800 */
    protected IAct act;
    /** Pointer to the world field from T800 */
    protected IVisionWorldView world;
    /** Pointer to the game field from T800 */
    protected Game game;
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
    /** Pointer to the shoot field from T800 */
    protected ImprovedShooting shoot;


    // *************************************************************************
    //                            STATIC FIELDS
    // *************************************************************************


    // FLAGS

    /** Flag that indicates if we have hit with the sniper or the shock */
    protected static boolean sniperOrShockHit;
    /** Flag that indicates if we have to shoot a combo */
    protected static boolean blowCombo;
    /** Flag that indicates if we have to switch to the shock rifle */
    protected static boolean useShockRifle;
    /** Flag that indicates if we are going to throw a spam */
    protected static boolean spam;
    /** Flag that indicates if the bot is crouched */
    public static boolean crouched;

    // FIELDS

    /** Location where we want to make the bot go */
    protected static Location destination;
    /** Location that the state is using to make the bot move */
    protected static Location stateDrivenDestination;
    /** Contains a list of the last places visited recently */
    protected static List <Location> visitedSpots;
    /** Current spot that the bot is facing */
    protected static Location currentFacingSpot;
    /** Game time when the bot made a rotation (as a result of a non cognitive action) */
    protected static double rotationTime;
    /** Collision sensors for each cardinal ray (true if any of them is hitting something) */
    protected static boolean collisionSensorArray [];
    /** True if there's a hit in ANY cardinal ray */
    protected static boolean raycastingHit;
    /** True if the bot is moving (just used for reactive movement) */
    protected static boolean moving;

    // CONSTANTS

    /** Short range */
    public static final double CLOSE = 700;
    /** Medium range */
    public static final double AVERAGE = 1200;
    /** Long range */
    public static final double FAR = 1500;

    /** Identifies the north ray in the cardinalRayArray vector */
    public static final int NORTH = 0;
    /** Identifies the north-east ray in the cardinalRayArray vector */
    public static final int NORTHEAST = 1;
    /** Identifies the east ray in the cardinalRayArray vector */
    public static final int EAST = 2;
    /** Identifies the south-east ray in the cardinalRayArray vector */
    public static final int SOUTHEAST = 3;
    /** Identifies the south ray in the cardinalRayArray vector */
    public static final int SOUTH = 4;
    /** Identifies the south-west ray in the cardinalRayArray vector */
    public static final int SOUTHWEST = 5;
    /** Identifies the west ray in the cardinalRayArray vector */
    public static final int WEST = 6;
    /** Identifies the north-west ray in the cardinalRayArray vector */
    public static final int NORTHWEST = 7;


    // *************************************************************************
    //                                METHODS
    // *************************************************************************


    /** Static constructor */
    static {
        destination = null;
        stateDrivenDestination = null;
        currentFacingSpot = null;
        rotationTime = -1;
        sniperOrShockHit = blowCombo = spam = useShockRifle = crouched = false;
        raycastingHit = moving = false;

        collisionSensorArray = new boolean [8];
        for (int i = 0; i < collisionSensorArray.length; ++i) {
            collisionSensorArray [i] = false;
        }

        visitedSpots = new ArrayList <Location> ();
    }

    //__________________________________________________________________________

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
    protected PrimaryState (final CompleteBotCommandsWrapper body, final IAct act, final IVisionWorldView world,
                     final Game game, final Items items, final AgentInfo info, final Weaponry weaponry,
                     final IPathPlanner <ILocated> pathPlanner, final IUnrealPathExecutor <ILocated> pathExecutor,
                     final AdvancedLocomotion move, final Raycasting raycasting, final AutoTraceRay cardinalRayArray [],
                     final ImprovedShooting shoot) {

        this.body = body;
        this.act = act;
        this.world = world;
        this.game = game;
        this.items = items;
        this.info = info;
        this.weaponry = weaponry;
        this.pathPlanner = pathPlanner;
        this.pathExecutor = pathExecutor;
        this.move = move;
        this.raycasting = raycasting;
        this.cardinalRayArray = cardinalRayArray;
        this.shoot = shoot;
    }


    //__________________________________________________________________________

    /**
     * PrimaryState driven movement. If substate is not null and can be executed, the
     * bot's behaviour will be driven by the substate which will tell the bot where
     * to go. If the substate is null or can't be executed, primary state will
     * drive the bot's behaviour. If destination is not null, the bot will move
     * to that destination regardless of the primary and secondary state.
     * @param subState Secondary state.
     * @param newDestination Destination where we want to make the bot go.
     * @param enemy Enemy.
     * @param facingSpot Location where we want the bot to be facing.
     * @param enemyInfo Guessed/Known information about the enemy.
     */
    public void executeMovement (final SecondaryState subState, final Location newDestination, final Player enemy, final Location facingSpot, final EnemyInfo enemyInfo) {

        boolean subStateError = false;
        if (subState != null && newDestination == null) {
            // Try, if it fails, capture exception and execute primary state movement
            try {
                subState.executeMovement(enemy, facingSpot);
            }
            catch (SubStatusException e) {
                subStateError = true;
            }
        }

        // If there's not a substate or the substate can't be executed, execute
        // primary state movement
        if (subState == null || newDestination != null || subStateError == true) {
            if (newDestination != null && (!newDestination.equals (destination) || (newDestination.equals (destination) && !pathExecutor.isExecuting()))) {
                destination = newDestination;

                IPathFuture <ILocated> pathHandle = pathPlanner.computePath(info.getLocation(), destination);

                if (pathExecutor.isExecuting ()) {
                    pathExecutor.stop ();
                }

                pathExecutor.followPath (pathHandle);
            }
            else if (newDestination == null) {
                stateDrivenMovement (enemy, facingSpot, enemyInfo);
            }
        }

        // If the location of the spot we want to face is different from the one
        // we are facing right now and it is not null, change rotation
        if (facingSpot != null && !facingSpot.equals (currentFacingSpot)) {
            pathExecutor.setFocus (facingSpot);
            currentFacingSpot = facingSpot;
            rotationTime = -1; // Por si el bot estaba en proceso de reaccion
        }
        // If the spot that we want to face is null, check if we have to disable
        // reactive facing (happens when the bot hears a noise or gets hits; he
        // automatically turns to see what happened) and if not, if the focus hasn't
        // been disabled, disable it
        // Reactive facing gets disabled after 2 seconds
        else if (facingSpot == null) {
            if (rotationTime != -1) {
                double elapsedTime = Math.abs (rotationTime - game.getTime());
                if (elapsedTime >= 2) {
                    pathExecutor.setFocus (null);
                    rotationTime = -1;
                }
            }
            else if (currentFacingSpot != null) {
                pathExecutor.setFocus (null);
                currentFacingSpot = null;
                rotationTime = -1;
            }
        }
    }

    //__________________________________________________________________________

    /**
     * PrimaryState driven movement. Each state will define how the bot should move.
     * @param enemy Enemy.
     * @param facingSpot Location where we should be facing.
     * @param enemyInfo Guessed/Known enemy information.
     */
    protected abstract void stateDrivenMovement (final Player enemy, final Location facingSpot, final EnemyInfo enemyInfo);

    //__________________________________________________________________________

    /**
     * We have reached the destination, update inner state.
     * @param subState
     */
    public void destinationReached (final SecondaryState subState) {
        pathExecutor.stop();

        if (subState != null) {
            subState.destinationReached();
        }
        else {
            //destination = null;
            stateDrivenDestination = null;
        }
    }

    //__________________________________________________________________________

    /**
     * Stops the execution of the state.
     * @param subState Sub state being executed.
     */
    public void stopExecution (final SecondaryState subState) {
        pathExecutor.stop();

        if (subState != null) {
            subState.stopExecution ();
        }

        destination = null;
        currentFacingSpot = null;
        rotationTime = -1;
        sniperOrShockHit = blowCombo = spam = useShockRifle = false;
        visitedSpots.clear ();
    }

    //__________________________________________________________________________

    /**
     * Executed when the bot is stuck.
     * @param subState Sub state being executed.
     */
    public void botStuck (final SecondaryState subState) {
        stopExecution (subState);
        pathExecutor.setFocus (null);
    }

    //__________________________________________________________________________

    /**
     * Switches to best weapon.
     * @param enemy Enemy.
     * @param enemyInfo Guessed/Known information about the enemy.
     */
    public void switchToBestWeapon (final Player enemy, final EnemyInfo enemyInfo) {
        Map <ItemType, Weapon> arsenal;
        int distanceAdvantage = 0, maximum = 0;
        Weapon selectedWeapon = null;

        // Get all weapons
        arsenal = weaponry.getWeapons ();

        // If we have hit with sniper or shock, switch to shock rifle
        if (sniperOrShockHit && weaponry.hasPrimaryWeaponAmmo(ItemType.SHOCK_RIFLE)) {
            weaponry.changeWeapon (ItemType.SHOCK_RIFLE);
            useShockRifle = true;
        }
        else if (blowCombo && weaponry.hasWeapon(ItemType.SHOCK_RIFLE) && weaponry.getAmmo(ItemType.SHOCK_RIFLE) >= 5) {
            weaponry.changeWeapon (ItemType.SHOCK_RIFLE);
        }
        else {
            for (Weapon currentWeapon : arsenal.values ()) {
                if (currentWeapon.getAmmo() > 0) {
                    // Estimate the advantage this weapon has based on the distance to the enemy
                    distanceAdvantage = estimateWeaponAdvantage (currentWeapon, enemy, enemyInfo);

                    if (distanceAdvantage > maximum) {
                        maximum = distanceAdvantage;
                        selectedWeapon = currentWeapon;
                    }
                }
            }

            if (selectedWeapon != null) {
                Weapon currentWeapon = weaponry.getCurrentWeapon();
                if (!currentWeapon.equals(selectedWeapon)) {
                    weaponry.changeWeapon(selectedWeapon);
                }
            }
        }

        sniperOrShockHit = false;
    }

    //__________________________________________________________________________

    /**
     * Shoots an enemy if he is on sight or a target if we specify it.
     * @param enemy Enemy.
     * @param bullseye Target, it could be a combo or a spam location.
     */
    public void engage (final Player enemy, final Location bullseye) {
        Weapon currentWeapon = weaponry.getCurrentWeapon();
        ItemType weaponType = currentWeapon.getType();

        if (bullseye != null) {
            // IF THE TARGET IS A COMBO
            if (blowCombo) {
                //body.getCommunication().sendGlobalTextMessage("engage = intento reventar el combo");

                shoot.shoot(bullseye);
            }
            // IF THE TARGET IS A SPAM
            else {
                //body.getCommunication().sendGlobalTextMessage("engage = DISPARO UN SPAM!!!");

                double bullseyeDistance = info.getDistance(bullseye);
                int bullseyeHeight = Arithmetic.estimateHeight (info.getLocation(), bullseye);

                // ASSAULT RIFLE SPAM

                if(weaponType.equals(ItemType.ASSAULT_RIFLE)) {
                    if (bullseyeDistance < CLOSE) {
                        shoot.shootSecondaryCharged (bullseye, 1);
                    }
                    else if (bullseyeDistance < AVERAGE) {
                        shoot.shootSecondaryCharged (bullseye, 2);
                    }
                    else {
                        shoot.shootSecondaryCharged (bullseye, 3);
                    }
                }

                // BIO RIFLE SPAM

                else if(weaponType.equals(ItemType.BIO_RIFLE)) {
                    if (bullseyeDistance < CLOSE) {
                        shoot.shootSecondaryCharged (bullseye, 1.5);
                    }
                    else {
                        shoot.shootSecondaryCharged (bullseye, 3);
                    }
                }

                // LINK GUN SPAM

                else if(weaponType.equals(ItemType.LINK_GUN)) {
                    shoot.shootPrimary(bullseye);
                }

                // FLAK CANNON SPAM

                else if(weaponType.equals(ItemType.FLAK_CANNON)) {
                    if (bullseyeDistance < CLOSE) {
                        shoot.shootPrimary (bullseye);
                    }
                    else if (bullseyeDistance < AVERAGE) {
                        if (bullseyeHeight == 0) {
                            shoot.shootPrimary (bullseye);
                        }
                        else if (bullseyeHeight > 0 || bullseyeHeight < 0) {
                            shoot.shootSecondary (bullseye);
                        }
                    }
                    else if (bullseyeDistance < FAR) {
                        if (bullseyeHeight == 0 || bullseyeHeight < 0) {
                            shoot.shootPrimary (bullseye);
                        }
                        else if (bullseyeHeight > 0) {
                            shoot.shootSecondary (bullseye);
                        }
                    }
                    else {
                        shoot.shootPrimary (bullseye);
                    }
                }

                // ROCKET LAUNCHER SPAM

                else if(weaponType.equals(ItemType.ROCKET_LAUNCHER)) {
                    shoot.shootPrimary (bullseye);
                }

                // SHOCK RIFLE SPAM

                else if (weaponType.equals(ItemType.SHOCK_RIFLE)) {
                    shoot.shootSecondary (bullseye);
                }

                // FOR ANY OF THE OTHER WEAPONS

                // Stop shooting because they are not suitable for spamming
                else {
                    shoot.stopShooting();
                }
            }
        }
        if(enemy != null) {
            Location enemyLocation = enemy.getLocation();
            double enemyDistance = info.getDistance(enemyLocation);
            int enemyHeight = Arithmetic.estimateHeight (info.getLocation(), enemyLocation);

            // *****************************************************************
            //                     TRADITIONAL SHOOTING
            // *****************************************************************

            // SHIELD GUN

            if (weaponType.equals(ItemType.SHIELD_GUN)) {
                shoot.shootPrimary(enemy);
            }

            // ASSAULT RIFLE

            else if (weaponType.equals(ItemType.ASSAULT_RIFLE)) {
                if (enemyDistance < CLOSE) { // Enemy at a close range distance
                    if (currentWeapon.getSecondaryAmmo() > 0) {
                        shoot.shootPrimary(enemy);
                    }
                    else {
                        shoot.shootSecondaryCharged(enemy, 2);

                    }
                }
                else { // Enemy at an average range distance
                    if (currentWeapon.getSecondaryAmmo() > 0) {
                        shoot.shootPrimary(enemy);
                    }
                    else {
                       shoot.shootSecondaryCharged(enemy, 3);
                    }
                }
            }

            // BIO RIFLE

            else if (weaponType.equals(ItemType.BIO_RIFLE)) {
                if (enemyDistance < CLOSE) { // Enemy at a close range distance
                    shoot.shootPrimary(enemy);
                }
                else {
                    shoot.shootSecondaryCharged(enemy, 3);
                }
            }

            // LINK GUN

            else if (weaponType.equals(ItemType.LINK_GUN)) {
                if (enemyDistance < CLOSE) { // Enemy at a close range distance
                    shoot.shootSecondary(enemy);
                }
                else if (enemyDistance < AVERAGE) { // Enemy at an average distance
                    shoot.shootSecondary(enemy);
                }
                else {
                    shoot.shootPrimary(enemy);
                }
            }

            // MINIGUN

            else if (weaponType.equals(ItemType.MINIGUN)) {
                if (enemyDistance >= FAR) {
                    shoot.shootSecondary(enemy);
                }
                else {
                    shoot.shootPrimary(enemy);
                }
            }

            // FLAK CANNON

            else if (weaponType.equals(ItemType.FLAK_CANNON)) {
                if (enemyDistance < CLOSE) { // Enemy at a close range distance
                    shoot.shootPrimary(enemy);
                }
                else if (enemyDistance < AVERAGE) { // Enemy at an average range distance
                    if (enemyHeight == 0) {
                        if (enemyDistance < AVERAGE/2) {
                            shoot.shootPrimary(enemy);
                        }
                        else {
                            shoot.shootSecondary(enemy);
                        }
                    }
                    else if (enemyHeight < 0) {
                        shoot.shootPrimary(enemy);
                    }
                    else {
                        shoot.shootSecondary(enemy);
                    }
                }
                else if (enemyDistance < FAR) { // Enemy at a far range distance
                    if (enemyHeight == 0 || enemyHeight < 0) { // Enemy at the same height
                        shoot.shootPrimary(enemy);
                    }
                    else { // Enemy on a higher position
                        shoot.shootSecondary(enemy);
                    }
                }
                else if (enemyDistance >= FAR) { // Enemy at a very far range distance
                    shoot.shootPrimary(enemy);
                }
            }

            // ROCKET LAUNCHER

            else if (weaponType.equals(ItemType.ROCKET_LAUNCHER)) {
                shoot.shootPrimary(enemy);
            }

            //SHOCK RIFLE

            else if (weaponType.equals(ItemType.SHOCK_RIFLE)) {
                shoot.shootPrimary(enemy);

                /*
                // If we previously hit the enemy with the shock or the sniper
                // try again with the shock
                if (useShockRifle) {
                    shoot.shootPrimary (enemy);
                }
                else {
                    if (enemyDistance < CLOSE) { // Enemy at a close range distance
                        shoot.shootPrimary(enemy);
                    }
                    else if (enemyDistance < AVERAGE) { // Enemy at an average range distance
                        if (enemyHeight == 0 || enemyHeight < 0) { // Enemy at the same height
                            // 50 % combo and beam
                            Random rand = new Random();
                            int probabilidad = rand.nextInt(100);

                            if (probabilidad < 50) {
                                shoot.shootPrimary(enemy);
                            }
                            else {
                                shoot.shootSecondary(enemy);
                            }
                        }
                        else { // Enemy on a higher position
                            shoot.shootSecondary(enemy);
                        }
                    }
                    else if (enemyDistance < FAR) { // Enemy at a far range distance
                        // 70 % beam, 30 % combo
                        Random rand = new Random();
                        int probabilidad = rand.nextInt(100);

                        if (probabilidad < 70) {
                            shoot.shootPrimary(enemy);
                        }
                        else {
                            shoot.shootSecondary(enemy);
                        }
                    }
                    else if (enemyDistance >= FAR) { // Enemy at a ver far range distance
                        shoot.shootPrimary(enemy);
                    }
                }
                */
            }

            // SNIPER RIFLE

            else if (weaponType.equals(ItemType.SNIPER_RIFLE) || weaponType.equals(ItemType.LIGHTNING_GUN)) {
                shoot.shootPrimary(enemy);
            }
        }
        else {
            shoot.stopShooting();
        }

        blowCombo = spam = useShockRifle = false;
    }

    //__________________________________________________________________________

    /**
     * Given a weapon, it estimates how profitable it is from 0 to 100.
     * @param weapon Weapon we want estimate how good it is.
     * @param enemy Enemy.
     * @param enemyInfo Guess/Known information about the enemy.
     * @return How good this weapon is from 0 to 100.
     */
    protected int estimateWeaponAdvantage (final Weapon weapon, final Player enemy, final EnemyInfo enemyInfo) {
        ItemType type = weapon.getType();
        int profit = 0;

        if (enemy != null) {
            int height = Arithmetic.estimateHeight (info.getLocation(), enemy.getLocation());
            double enemyDistance = enemy.getLocation ().getDistance (info.getLocation ());

            // SHIELD GUN

            if(type.equals(ItemType.SHIELD_GUN)) {
                if (enemyDistance <= CLOSE) {
                    if (height == 0) {
                        profit = 5;
                    }
                    else if (height < 0) {
                        profit = 1;
                    }
                    else {
                        profit = 1;
                    }
                }
                else if (enemyDistance <= AVERAGE) {
                    if (height == 0) {
                        profit = 0;
                    }
                    else if (height < 0) {
                        profit = 1;
                    }
                    else {
                        profit = 1;
                    }
                }
                else if (enemyDistance <= FAR) {
                    if (height == 0) {
                        profit = 0;
                    }
                    else if (height < 0) {
                        profit = 1;
                    }
                    else {
                        profit = 1;
                    }
                }
                else if (enemyDistance > FAR) {
                    if (height == 0) {
                        profit = 1;
                    }
                    else if (height < 0) {
                        profit = 1;
                    }
                    else {
                        profit = 1;
                    }
                }
            }

            // ASSAULT RIFLE

            else if (type.equals(ItemType.ASSAULT_RIFLE)) {
                if (enemyDistance <= CLOSE) {
                    if (height == 0) {
                        profit = 10;
                    }
                    else if (height < 0) {
                        profit = 10;
                    }
                    else {
                        profit = 10;
                    }
                }
                else if (enemyDistance <= AVERAGE) {
                    if (height == 0) {
                        profit = 10;
                    }
                    else if (height < 0) {
                        profit = 10;
                    }
                    else {
                        profit = 20;
                    }
                }
                else if (enemyDistance <= FAR) {
                    if (height == 0) {
                        profit = 10;
                    }
                    else if (height < 0) {
                        profit = 20;
                    }
                    else {
                        profit = 30;
                    }
                }
                else if (enemyDistance > FAR) {
                    if (height == 0) {
                        profit = 10;
                    }
                    else if (height < 0) {
                        profit = 30;
                    }
                    else {
                        profit = 30;
                    }
                }
            }

            // BIO RIFLE

            else if (type.equals(ItemType.BIO_RIFLE)) {
                if (enemyDistance <= CLOSE) {
                    if (height == 0) {
                        profit = 20;
                    }
                    else if (height < 0) {
                        profit = 50;
                    }
                    else {
                        profit = 50;
                    }
                }
                else if (enemyDistance <= AVERAGE) {
                    if (height == 0) {
                        profit = 30;
                    }
                    else if (height < 0) {
                        profit = 40;
                    }
                    else {
                        profit = 30;
                    }
                }
                else if (enemyDistance <= FAR) {
                    if (height == 0) {
                        profit = 15;
                    }
                    else if (height < 0) {
                        profit = 10;
                    }
                    else {
                        profit = 10;
                    }
                }
                else if (enemyDistance > FAR) {
                    if (height == 0) {
                        profit = 0;
                    }
                    else if (height < 0) {
                        profit = 0;
                    }
                    else {
                        profit = 0;
                    }
                }
            }

            // LINK GUN

            else if (type.equals(ItemType.LINK_GUN)) {
                if (enemyDistance <= CLOSE) {
                    if (height == 0) {
                        profit = 80;

                        /*
                        // If the enemy's health is under 150 and he is trying to run
                        if (enemyInfo.getHealthArmor() < 150) {
                            profit = 100;
                        }
                        // If he is under 70
                        else if (enemyInfo.getHealthArmor() < 70) {
                            profit = 100;
                        }*/
                        if (enemyInfo.getHealthArmor() < 70) {
                            profit = 100;
                        }
                    }
                    else if (height < 0) {
                        profit = 40;
                    }
                    else {
                        profit = 40;
                    }
                }
                else if (enemyDistance <= AVERAGE) {
                    if (height == 0) {
                        profit = 50;

                        /*
                        // If the enemy's health is under 150 and he is trying to run
                        if (enemyInfo.getHealthArmor() < 150) {
                            profit = 90;
                        }
                        // If he is under 70
                        else if (enemyInfo.getHealthArmor() < 70) {
                            profit = 90;
                        }*/
                        if (enemyInfo.getHealthArmor() < 100) {
                            profit = 90;
                        }
                    }
                    else if (height < 0) {
                        profit = 20;
                    }
                    else {
                        profit = 50;
                    }
                }
                else if (enemyDistance <= FAR) {
                    if (height == 0) {
                        profit = 20;
                    }
                    else if (height < 0) {
                        profit = 40;
                    }
                    else {
                        profit = 50;
                    }
                }
                else if (enemyDistance > FAR) {
                    if (height == 0) {
                        profit = 20;
                    }
                    else if (height < 0) {
                        profit = 40;
                    }
                    else {
                        profit = 50;
                    }
                }
            }

            // MINIGUN

            else if (type.equals(ItemType.MINIGUN)) {
                if (enemyDistance <= CLOSE) {
                    if (height == 0) {
                        profit = 40;

                        /*
                        // If the enemy's health is under 150 and he is trying to run
                        if (enemyInfo.getHealthArmor() < 150) {
                            profit = 97;
                        }
                        // If he is under 70
                        else if (enemyInfo.getHealthArmor() < 70) {
                            profit = 97;
                        }*/
                        if (enemyInfo.getHealthArmor() < 100) {
                            profit = 97;
                        }
                    }
                    else if (height < 0) {
                        profit = 60;
                    }
                    else {
                        profit = 20;
                    }
                }
                else if (enemyDistance <= AVERAGE) {
                    if (height == 0) {
                        profit = 70;

                        /*
                        // If the enemy's health is under 150 and he is trying to run
                        if (enemyInfo.getHealthArmor() < 150) {
                            profit = 100;
                        }
                        // If he is under 70
                        else if (enemyInfo.getHealthArmor() < 70) {
                            profit = 100;
                        }*/
                        if (enemyInfo.getHealthArmor() < 70) {
                            profit = 100;
                        }
                    }
                    else if (height < 0) {
                        profit = 60;

                        if (enemyInfo.getHealthArmor() < 70) {
                            profit = 100;
                        }
                    }
                    else {
                        profit = 60;
                    }
                }
                else if (enemyDistance <= FAR) {
                    if (height == 0) {
                        profit = 80;

                        /*
                        // If the enemy's health is under 150 and he is trying to run
                        if (enemyInfo.getHealthArmor() < 150) {
                            profit = 100;
                        }
                        // If he is under 70
                        else if (enemyInfo.getHealthArmor() < 70) {
                            profit = 100;
                        }*/
                        if (enemyInfo.getHealthArmor() < 70) {
                            profit = 100;
                        }
                    }
                    else if (height < 0) {
                        profit = 60;

                        if (enemyInfo.getHealthArmor() < 70) {
                            profit = 100;
                        }
                    }
                    else {
                        profit = 60;

                        if (enemyInfo.getHealthArmor() < 70) {
                            profit = 100;
                        }
                    }
                }
                else if (enemyDistance > FAR) {
                    if (height == 0) {
                        profit = 50;
                    }
                    else if (height < 0) {
                        profit = 70;
                    }
                    else {
                        profit = 70;
                    }
                }
            }

            // FLAK CANNON

            else if (type.equals(ItemType.FLAK_CANNON)) {
                if (enemyDistance <= CLOSE) {
                    if (height == 0) {
                        profit = 95;
                    }
                    else if (height < 0) {
                        profit = 90;
                    }
                    else {
                        profit = 100;
                    }
                }
                else if (enemyDistance <= AVERAGE) {
                    if (height == 0) {
                        profit = 80;
                    }
                    else if (height < 0) {
                        profit = 80;
                    }
                    else {
                        profit = 90;
                    }
                }
                else if (enemyDistance <= FAR) {
                    if (height == 0) {
                        profit = 40;
                    }
                    else if (height < 0) {
                        profit = 50;
                    }
                    else {
                        profit = 70;
                    }
                }
                else if (enemyDistance > FAR) {
                    if (height == 0) {
                        profit = 20;
                    }
                    else if (height < 0) {
                        profit = 60;
                    }
                    else {
                        profit = 60;
                    }
                }
            }

            // ROCKET LAUNCHER

            else if (type.equals(ItemType.ROCKET_LAUNCHER)) {
                if (enemyDistance <= CLOSE) {
                    if (height == 0) {
                        profit = 95;
                    }
                    else if (height < 0) {
                        profit = 100;
                    }
                    else {
                        profit = 60;
                    }
                }
                else if (enemyDistance <= AVERAGE) {
                    if (height == 0) {
                        profit = 90;
                    }
                    else if (height < 0) {
                        profit = 90;
                    }
                    else {
                        profit = 40;
                    }
                }
                else if (enemyDistance <= FAR) {
                    if (height == 0) {
                        profit = 30;
                    }
                    else if (height < 0) {
                        profit = 70;
                    }
                    else {
                        profit = 40;
                    }
                }
                else if (enemyDistance > FAR) {
                    if (height == 0) {
                        profit = 10;
                    }
                    else if (height < 0) {
                        profit = 50;
                    }
                    else {
                        profit = 40;
                    }
                }
            }

            // SHOCK RIFLE

            else if (type.equals(ItemType.SHOCK_RIFLE)) {
                if (enemyDistance <= CLOSE) {
                    if (height == 0) {
                        profit = 30;
                    }
                    else if (height < 0) {
                        profit = 80;
                    }
                    else {
                        profit = 90;
                    }
                }
                else if (enemyDistance <= AVERAGE) {
                    if (height == 0) {
                        profit = 97;
                    }
                    else if (height < 0) {
                        profit = 97;
                    }
                    else {
                        profit = 100;
                    }
                }
                else if (enemyDistance <= FAR) {
                    if (height == 0) {
                        profit = 80;
                    }
                    else if (height < 0) {
                        profit = 90;
                    }
                    else {
                        profit = 90;
                    }
                }
                else if (enemyDistance > FAR) {
                    if (height == 0) {
                        profit = 80;
                    }
                    else if (height < 0) {
                        profit = 80;
                    }
                    else {
                        profit = 80;
                    }
                }
            }

            // SNIPER RIFLE/LIGHTNING GUN

            else if (type.equals(ItemType.SNIPER_RIFLE) || type.equals(ItemType.LIGHTNING_GUN)) {
                if (enemyDistance <= CLOSE) {
                    if (height == 0) {
                        profit = 15;
                    }
                    else if (height < 0) {
                        profit = 70;
                    }
                    else {
                        profit = 70;
                    }
                }
                else if (enemyDistance <= AVERAGE) {
                    if (height == 0) {
                        profit = 40;
                    }
                    else if (height < 0) {
                        profit = 70;
                    }
                    else {
                        profit = 70;
                    }
                }
                else if (enemyDistance <= FAR) {
                    if (height == 0) {
                        profit = 90;
                    }
                    else if (height < 0) {
                        profit = 80;
                    }
                    else {
                        profit = 80;
                    }
                }
                else if (enemyDistance > FAR) {
                    if (height == 0) {
                        profit = 100;
                    }
                    else if (height < 0) {
                        profit = 100;
                    }
                    else {
                        profit = 100;
                    }
                }
            }
        }
        else if (spam) {
            // If we are going to shoot a spam
            if (type.equals(ItemType.SHIELD_GUN)) {
                profit = 1;
            }
            else if (type.equals(ItemType.ASSAULT_RIFLE)) {
                profit = 50;
            }
            else if (type.equals(ItemType.BIO_RIFLE)) {
                profit = 40;
            }
            else if (type.equals(ItemType.LINK_GUN)) {
                profit = 60;
            }
            else if (type.equals(ItemType.MINIGUN)) {
                profit = 5;
            }
            else if (type.equals(ItemType.FLAK_CANNON)) {
                profit = 70;
            }
            else if (type.equals(ItemType.ROCKET_LAUNCHER)) {
                profit = 90;
            }
            else if (type.equals(ItemType.SHOCK_RIFLE)) {
                profit = 80;
            }
            else if (type.equals(ItemType.SHOCK_RIFLE) || type.equals(ItemType.SNIPER_RIFLE)) {
                profit = 10;
            }
        }
        // If the enemy is not on sight
        else {
            if (type.equals(ItemType.SHIELD_GUN)) {
                profit = 1;
            }
            else if (type.equals(ItemType.ASSAULT_RIFLE)) {
                profit = 10;
            }
            else if (type.equals(ItemType.BIO_RIFLE)) {
                profit = 20;
            }
            else if (type.equals(ItemType.LINK_GUN)) {
                profit = 50;
            }
            else if (type.equals(ItemType.MINIGUN)) {
                profit = 40;
            }
            else if (type.equals(ItemType.FLAK_CANNON)) {
                profit = 80;
            }
            else if (type.equals(ItemType.ROCKET_LAUNCHER)) {
                profit = 70;
            }
            else if (type.equals(ItemType.SHOCK_RIFLE)) {
                profit = 90;
            }
            else if (type.equals(ItemType.SHOCK_RIFLE) || type.equals(ItemType.SNIPER_RIFLE)) {
                profit = 100;
            }
        }

        return profit;
    }

    //__________________________________________________________________________

    /**
     * Reset temporary information.
     */
    public static void resetTempInfo () {
        crouched = false;
    }

    //__________________________________________________________________________

    /**
     * Notify that there is a feasible combo.
     */
    public static void feasibleCombo () {
        blowCombo = true;
    }

    //__________________________________________________________________________

    /**
     * Notify that there is a feasible spam.
     */
    public static void feasibleSpam () {
        spam = true;
    }

    //__________________________________________________________________________

    /**
     * Actualiza información del estado cuando el desparece un jugador de nuestro
     * campo de visión.
     * Update information (Whenever a player dissappears from our field of vision).
     */
    public void playerDisappeared () {
        sniperOrShockHit = false;
    }

    //__________________________________________________________________________

    /**

     * Update information (Whenever we hit an enemy).
     */
    public void playerDamaged (final PlayerDamaged event) {
        if (event.getDamageType ().equals ("XWeapons.DamTypeShockBeam") || event.getDamageType ().equals ("XWeapons.DamTypeSniperShot")) {
            sniperOrShockHit = true;
        }
    }

    //__________________________________________________________________________

    /**
     * Reactive action of the bot when he hears a noise.
     * @param noise Noise the bot just heared.
     * @param clockTime Game time when the bot heared the noise.
     * @param enemy Enemy.
     */
    public void hearNoise (final HearNoise noise, double clockTime, final Player enemy) {
        if (enemy == null) {
            Location focusSpot = Arithmetic.rotationToLocation (info, noise.getRotation());

            if (pathExecutor.isExecuting()) {
                pathExecutor.setFocus (focusSpot);
            }
            else {
                move.stopMovement();
                move.turnTo (focusSpot);
            }

            rotationTime = clockTime;
        }
    }

    //__________________________________________________________________________

    /**
     * Reactive action of the bot when he hears someone picking up something.
     * @param noise Noise that the bot just heared.
     * @param clockTime Game time when the bot heared the noise.
     * @param enemy Enemy.
     */
    public void hearPickup (final HearPickup noise, double clockTime, final Player enemy) {
        Item item = items.getItem (noise.getSource ());

        if (enemy == null && item != null && item.getLocation () != null && info.getDistance (item.getLocation()) > 200) {
            Location focusSpot = Arithmetic.rotationToLocation (info, noise.getRotation());

            if (pathExecutor.isExecuting()) {
                pathExecutor.setFocus (focusSpot);
            }
            else {
                move.stopMovement();
                move.turnTo (focusSpot);
            }

            rotationTime = clockTime;
        }
    }

    //__________________________________________________________________________

    /**
     * Reactive action of the bot when he gets shot. As a reaction, if he is not
     * seeing the enemy, he turns 180 degrees to try to see what happened.
     * @param botDamaged Bot who has been damaged.
     * @param clockTime Game time when the bot was hit.
     * @param enemy Enemy.
     */
    public void botDamaged (final BotDamaged botDamaged, double clockTime, final Player enemy) {
        if (botDamaged.isDirectDamage () && enemy == null) {
            if (pathExecutor.isExecuting()) {
                pathExecutor.stop ();
            }
            else {
                move.stopMovement ();
            }

            move.turnHorizontal(180);
        }
    }

    //__________________________________________________________________________

    /**
     * Makes the bot run continuosly forward.
     */
    protected void goForward () {
        move.moveContinuos();
        moving = true;
    }

    //__________________________________________________________________________

    /**
     * Reactive movement. The bot just avoids hitting walls, nothing else.
     */
    public void explorarReactivo () {
        if (!raycasting.getAllRaysInitialized().getFlag()) return;

    	// once the rays are up and running, move according to them

        collisionSensorArray [NORTH] = cardinalRayArray [NORTH].isResult();
        collisionSensorArray [NORTHEAST] = cardinalRayArray [NORTHEAST].isResult();
        collisionSensorArray [EAST] = cardinalRayArray [EAST].isResult();
        collisionSensorArray [SOUTHEAST] = cardinalRayArray [SOUTHEAST].isResult();
        collisionSensorArray [SOUTH] = cardinalRayArray [SOUTH].isResult();
        collisionSensorArray [SOUTHWEST] = cardinalRayArray [SOUTHWEST].isResult();
        collisionSensorArray [WEST] = cardinalRayArray [WEST].isResult();
        collisionSensorArray [NORTHWEST] = cardinalRayArray [NORTHWEST].isResult();

        // is any of the sensor signalig?
        for (int i = 0; i < collisionSensorArray.length && !raycastingHit; ++i) {
            raycastingHit = collisionSensorArray [i];
        }

        if (!collisionSensorArray[NORTH] && !collisionSensorArray[NORTHEAST] && !collisionSensorArray[NORTHWEST]) {
        	// no sensor are signalizes - just proceed with forward movement
        	goForward ();
        	return;
        }

        // some sensor/s is/are signaling

        // if we're moving
        if (moving) {
            // stop it, we have to turn probably
            move.stopMovement();
            moving = false;
        }

        // according to the signals, take action...
        // 8 cases that might happen follows
        if (collisionSensorArray [NORTH]) {
            if (collisionSensorArray [NORTHWEST]) {
                if (collisionSensorArray [NORTHEAST]) {
                    // LEFT45, RIGHT45, FRONT are signaling
                    move.turnHorizontal(90);
                } else {
                    // LEFT45, FRONT45 are signaling
                    move.turnHorizontal(30);
                }
            } else {
                if (collisionSensorArray [NORTHEAST]) {
                    // RIGHT45, FRONT are signaling
                    move.turnHorizontal(-30);
                } else {
                    // FRONT is signaling
                    move.turnHorizontal(30);
                }
            }
        } else {
            if (collisionSensorArray [NORTHWEST]) {
                if (collisionSensorArray [NORTHEAST]) {
                    // LEFT45, RIGHT45 are signaling
                    goForward ();
                } else {
                    // LEFT45 is signaling
                    move.turnHorizontal(30);
                }
            } else {
                if (collisionSensorArray [NORTHEAST]) {
                    // RIGHT45 is signaling
                    move.turnHorizontal(-30);
                } else {
                    // no sensor is signaling
                    goForward ();
                }
            }
        }
    }

    //__________________________________________________________________________

    /**
     * Converts to string the state's name.
     * @return The name of the state.
     */
    @Override
    public abstract String toString ();
}
