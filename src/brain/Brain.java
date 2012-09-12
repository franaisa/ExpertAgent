package brain;

import bot.Bot;
import behaviour.primaryStates.PrimaryState;
import knowledge.EnemyInfo;
import utilities.Arithmetic;

import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.cuni.amis.pogamut.unreal.communication.messages.UnrealId;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensomotoric.Weaponry;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensor.AgentInfo;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensor.Game;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensor.Items;
import cz.cuni.amis.pogamut.ut2004.bot.command.CompleteBotCommandsWrapper;
import cz.cuni.amis.pogamut.ut2004.communication.messages.ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.IncomingProjectile;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Item;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Player;
import java.util.Map;


/**
 *
 * @author Francisco Aisa García
 */


public class Brain {

    // *************************************************************************
    //                             INSTANCE FIELDS
    // *************************************************************************


    /** Pointer to body from T800 */
    protected CompleteBotCommandsWrapper body;
    /** Location of a combo (null if it doesn't exist) */
    private Location comboLocation;
    /** Location of a spam (null if it doesn't exists) */
    private Location spamLocation;


    // *************************************************************************
    //                                 METHODS
    // *************************************************************************


    /**
     * Argument based constructor.
     * @param body Pointer to body from T800.
     */
    public Brain (final CompleteBotCommandsWrapper body) {
        this.body = body;

        comboLocation = null;
        spamLocation = null;
    }

    //__________________________________________________________________________

    /**
     * Resets all the temporary information hosted in skynet.
     */
    public void resetTempInfo () {
        comboLocation = null;
        spamLocation = null;
    }

    //__________________________________________________________________________

    /**
     * It estimates how bad we need an item based on the items that we already have.
     * @param item Item that we are evaluating.
     * @param weaponry Weaponry that we are carrying.
     * @return The priority of the given item.
     */
    protected int estimateItemPriority (final Item item, final Weaponry weaponry) {
        int priority = -1;
        ItemType type = item.getType ();

        if (type.equals (ItemType.SUPER_SHIELD_PACK)) {
            priority = 100;
        }
        else if (type.equals (ItemType.SHIELD_PACK)) {
            priority = 90;
        }
        else if (type.equals (ItemType.SNIPER_RIFLE) || type.equals (ItemType.LIGHTNING_GUN)) {
            if (!weaponry.hasLoadedWeapon (type)) {
                priority = 90;
            }
        }
        else if (type.equals (ItemType.SHOCK_RIFLE)) {
            if (!weaponry.hasLoadedWeapon (type)) {
                priority = 90;
            }
        }
        else if (type.equals (ItemType.FLAK_CANNON) || type.equals (ItemType.ROCKET_LAUNCHER)) {
            if (!weaponry.hasWeapon(ItemType.FLAK_CANNON) && !weaponry.hasWeapon(ItemType.ROCKET_LAUNCHER)) {
                priority = 85;
            }
        }
        else if (type.equals (ItemType.MINIGUN)) {
            if (!weaponry.hasWeapon (type)) {
                priority = 80;
            }
        }

        return priority;
    }

    //__________________________________________________________________________

    /**
     * It dictates where the bot should go (if any place). If we are seeing the enemy
     * it will return a destination ONLY if the super shield is spawned. If we aren't
     * seeing the enemy, it will return the best destination where we could be headed.
     * @param info Agent information.
     * @param enemy Enemy.
     * @param weaponry Weaponry we are carrying.
     * @param items Items from the current map.
     * @return Location where the bot must go. If it musn't go to a certain location,
     * it returns null.
     */
    public Location estimateDestination (final AgentInfo info, final Player enemy, final Weaponry weaponry, final Items items) {
        // Set it to null in case there is nowhere new to go
        Location newDestination = null;
        int currentPriority;
        int maximumPriority = 0;

        // Check if there is important items spawned, and if so, set the new
        // destination to the highest priority item to be taken care of.

        // First, let's check if there is any armor item spawned.
        Map<UnrealId, Item> itemList = items.getAllItems(ItemType.Category.ARMOR);
        for(Item item : itemList.values()) {
            if (item != null) {
                if (items.isPickupSpawned (item)) {
                    currentPriority = estimateItemPriority (item, weaponry);

                    if (currentPriority > maximumPriority) {
                        maximumPriority = currentPriority;

                        if (enemy != null) {
                            // If we are seeing the enemy, set the destination
                            // only if the super shield pack has spawned
                            if (maximumPriority == 100) {
                                newDestination = item.getLocation ();
                            }
                        }
                        else {
                            newDestination = item.getLocation ();
                        }
                    }
                }
            }
        }

        // If there aren't any shields spawned and we are not facing the enemy,
        // see if there is any weapon that's worth timing.
        if (enemy == null && newDestination == null) {
            itemList = items.getAllItems(ItemType.Category.WEAPON);

            double currentDistance;
            double targetDistance = Arithmetic.INFINITY;
            for(Item item : itemList.values()) {
                if (item != null) {
                    if (items.isPickupSpawned (item)) {
                        currentPriority = estimateItemPriority (item, weaponry);

                        if (currentPriority > maximumPriority) {
                            maximumPriority = currentPriority;
                            targetDistance = info.getDistance (item.getLocation ());
                            newDestination = item.getLocation ();
                        }
                        // If we have two items with the same priority, we'll
                        // stick with the closest one
                        else if (currentPriority == maximumPriority) {
                            currentDistance = info.getDistance (item.getLocation ());
                            if (currentDistance < targetDistance) {
                                targetDistance = currentDistance;
                                newDestination = item.getLocation ();
                            }
                        }
                    }
                }
            }
        }

        return newDestination;
    }

    //__________________________________________________________________________

    /**
     * Estimate how good our arsenal is when compared to the enemy's.
     * @param weaponry Weaponry that we are carrying.
     * @param enemyArsenal List of weapons we suppose the enemy has.
     * @return A vector divided in 3 slots. Each one indicates close, average and
     * far distance (in that order). Each slot has a value that varies from 1 to 5, meaning:
     * 1 our arsenal is far worst, 2 our arsenal is worst, 3 our arsenals are more or less
     * the same, 4 our arsenal is better, 5 our arsenal is far better.
     */
    public int [] compareArsenals (final Weaponry weaponry, final boolean enemyArsenal []) {
        int arsenalProfit [] = new int [3];
        int enemyTotalProfit [] = new int [3];
        int ownTotalProfit [] = new int [3];
        int ownProfit = 0;
        int enemyProfit = 0;

        // *********************************************************************
        //                       PROFIT FROM A FAR DISTANCE
        // *********************************************************************


        // CHECK OUR ARSENAL


        if ((weaponry.hasLoadedWeapon(ItemType.LIGHTNING_GUN) || weaponry.hasLoadedWeapon(ItemType.SNIPER_RIFLE)) && weaponry.hasLoadedWeapon(ItemType.SHOCK_RIFLE)) {
            ownProfit = 100;
        }
        else if (weaponry.hasLoadedWeapon(ItemType.LIGHTNING_GUN) || weaponry.hasLoadedWeapon(ItemType.SNIPER_RIFLE)) {
            ownProfit = 90;
        }
        else if (weaponry.hasLoadedWeapon(ItemType.SHOCK_RIFLE)) {
            ownProfit = 80;
        }
        else if (weaponry.hasLoadedWeapon(ItemType.MINIGUN)) {
            ownProfit = 40;
        }


        // CHECK ENEMY'S ARSENAL


        if (enemyArsenal [EnemyInfo.LIGHTNING_GUN] && enemyArsenal [EnemyInfo.SHOCK_RIFLE]) {
            enemyProfit = 100;
        }
        else if (enemyArsenal [EnemyInfo.LIGHTNING_GUN]) {
            enemyProfit = 90;
        }
        else if (enemyArsenal [EnemyInfo.SHOCK_RIFLE]) {
            enemyProfit = 80;
        }
        else if (weaponry.hasLoadedWeapon(ItemType.MINIGUN)) {
            enemyProfit = 40;
        }


        // ESTIMATE PROFIT

        ownTotalProfit [2] = ownProfit;
        enemyTotalProfit [2] = enemyProfit;

        ownProfit = enemyProfit = 0;



        // *********************************************************************
        //                   PROFIT FROM AN AVERAGE DISTANCE
        // *********************************************************************


        // CHECK OUR ARSENAL


        if ((weaponry.hasLoadedWeapon(ItemType.FLAK_CANNON) || weaponry.hasLoadedWeapon(ItemType.ROCKET_LAUNCHER))) {
            if (weaponry.hasLoadedWeapon(ItemType.SHOCK_RIFLE) && weaponry.hasLoadedWeapon(ItemType.MINIGUN)) {
                ownProfit = 100;
            }
            else if (weaponry.hasLoadedWeapon(ItemType.SHOCK_RIFLE)) {
                ownProfit = 95;
            }
            else if (weaponry.hasLoadedWeapon(ItemType.MINIGUN)) {
                ownProfit = 90;
            }
            else if (weaponry.hasLoadedWeapon(ItemType.SNIPER_RIFLE) || weaponry.hasLoadedWeapon(ItemType.LIGHTNING_GUN)) {
                ownProfit = 85;
            }
            else {
                ownProfit = 80;
            }
        }
        else {
            if (weaponry.hasLoadedWeapon(ItemType.SHOCK_RIFLE) && (weaponry.hasLoadedWeapon(ItemType.SNIPER_RIFLE) || weaponry.hasLoadedWeapon(ItemType.LIGHTNING_GUN))) {
                if (weaponry.hasLoadedWeapon(ItemType.MINIGUN)) {
                    ownProfit = 80;
                }
                else {
                    ownProfit = 75;
                }
            }
            else if (weaponry.hasLoadedWeapon (ItemType.SHOCK_RIFLE)) {
                if (weaponry.hasLoadedWeapon(ItemType.MINIGUN)) {
                    ownProfit = 60;
                }
                else {
                    ownProfit = 50;
                }
            }
            else if (weaponry.hasLoadedWeapon(ItemType.SNIPER_RIFLE) || weaponry.hasLoadedWeapon(ItemType.LIGHTNING_GUN)) {
                if (weaponry.hasLoadedWeapon(ItemType.MINIGUN)) {
                    ownProfit = 40; // Lower than if he is holding shock and mini
                }
                else {
                    ownProfit = 10;
                }
            }
            else if (weaponry.hasLoadedWeapon (ItemType.MINIGUN)) {
                ownProfit = 30; // Lower than if he is holding shock/sniper and mini
            }
        }


        // CHECK ENEMY'S ARSENAL


        if ((enemyArsenal [EnemyInfo.FLAK_CANNON] || enemyArsenal [EnemyInfo.ROCKET_LAUNCHER])) {
            if (enemyArsenal [EnemyInfo.SHOCK_RIFLE] && enemyArsenal [EnemyInfo.MINIGUN]) {
                enemyProfit = 100;
            }
            else if (enemyArsenal [EnemyInfo.SHOCK_RIFLE]) {
                enemyProfit = 90;
            }
            else if (enemyArsenal [EnemyInfo.MINIGUN]) {
                enemyProfit = 80;
            }
            else if (enemyArsenal [EnemyInfo.SNIPER_RIFLE]) {
                enemyProfit = 70;
            }
            else {
                enemyProfit = 60;
            }
        }
        else {
            if (enemyArsenal [EnemyInfo.SHOCK_RIFLE] && (enemyArsenal [EnemyInfo.SNIPER_RIFLE])) {
                if (enemyArsenal [EnemyInfo.MINIGUN]) {
                    enemyProfit = 90;
                }
                else {
                    enemyProfit = 60;
                }
            }
            else if (enemyArsenal [EnemyInfo.SHOCK_RIFLE]) {
                if (enemyArsenal [EnemyInfo.MINIGUN]) {
                    enemyProfit = 55;
                }
                else {
                    enemyProfit = 50;
                }
            }
            else if (enemyArsenal [EnemyInfo.SNIPER_RIFLE]) {
                if (enemyArsenal [EnemyInfo.MINIGUN]) {
                    enemyProfit = 40; // Lower than if he has shock and mini
                }
                else {
                    enemyProfit = 10;
                }
            }
            else if (enemyArsenal [EnemyInfo.MINIGUN]) {
                enemyProfit = 30; // Lower than if he has shock/sniper and mini
            }
        }


        // ESTIMATE PROFIT


        ownTotalProfit [1] = ownProfit;
        enemyTotalProfit [1] = enemyProfit;

        ownProfit = enemyProfit = 0;


        // *********************************************************************
        //                    PROFIT FROM A CLOSE DISTANCE
        // *********************************************************************


        // CHECK OUR ARSENAL


        if (weaponry.hasLoadedWeapon(ItemType.FLAK_CANNON) || weaponry.hasLoadedWeapon(ItemType.ROCKET_LAUNCHER)) {
            if (weaponry.hasLoadedWeapon(ItemType.LINK_GUN) || weaponry.hasLoadedWeapon(ItemType.MINIGUN)) {
               ownProfit = 100;
            }
            else {
                ownProfit = 90;
            }
        }
        else if (weaponry.hasLoadedWeapon(ItemType.LINK_GUN) || weaponry.hasLoadedWeapon(ItemType.MINIGUN)) {
            ownProfit = 50;
        }


        // CHECK ENEMY'S ARSENAL


        if (enemyArsenal [EnemyInfo.FLAK_CANNON] || enemyArsenal[EnemyInfo.ROCKET_LAUNCHER]) {
            if (enemyArsenal[EnemyInfo.LINK_GUN] || enemyArsenal[EnemyInfo.MINIGUN]) {
               enemyProfit = 100;
            }
            else {
                enemyProfit = 90;
            }
        }
        else if (enemyArsenal[EnemyInfo.LINK_GUN] || enemyArsenal[EnemyInfo.MINIGUN]) {
            enemyProfit = 50;
        }


        // ESTIMATE PROFIT


        ownTotalProfit [0] = ownProfit;
        enemyTotalProfit [0] = enemyProfit;


        // *********************************************************************
        //    ESTIMATE HOW GOOD OUR ARSENAL IS WHEN COMPARED TO THE ENEMY'S
        // *********************************************************************

        // 1 -> Our arsenal is far worst
        // 2 -> Our arsenal is worst
        // 3 -> Our arsenals are mor or less the same
        // 4 -> Our arsenal is better
        // 5 -> Our arsenal is far better


        // --------------------------> RESULTS  <-------------------------------

        for (int i = 0; i < 3; ++i) {
            int dif = ownTotalProfit [i] - enemyTotalProfit [i];
            if (dif >= -10 && dif <= 10 ) {
                arsenalProfit [i] = 3;
            }
            else if(dif >= 0) {
                if (dif > 10 && dif <= 30) {
                    arsenalProfit [i] = 4;
                }
                else
                    arsenalProfit [i] = 5;
            }
            else {
                if (dif < -10 && dif >= -30) {
                    arsenalProfit [i] = 2;
                }
                else
                    arsenalProfit [i] = 1;
            }
        }

        return arsenalProfit;
    }

    //__________________________________________________________________________

    /**
     * Estimate the best distance to stay put from an enemy (if he is on sight)
     * based on both the enemy's arsenal and our arsenal.
     * @param enemyDistance Distance to enemy (from us).
     * @param maximumProfit Maximum profit we have gotten comparing our arsenal
     * to the enemy's.
     * @param risk Level of risk (between 1 and 5) that we are willing to take.
     * @param sweetSpot Indicates the range in which we want to stay (close, average, far).
     * @return The profile we need (Defensive or Ofensive) to stay in the best range.
     * If the risk is too high or we are already in the range, it returns "disabled".
     */
    public int estimateProfile (double enemyDistance, int maximumProfit, int risk, int sweetSpot) {
        // If our arsenal is more or less the same or better than the enemy's
        if (maximumProfit >= risk) {
            // Check which is the best range

            // SHORT DISTANCE
            if (sweetSpot == 0) {
               if (enemyDistance > PrimaryState.CLOSE/2) {
                   return Bot.OFENSIVEPROFILE;
               }
            }
            // AVERAGE DISTANCE
            else if (sweetSpot == 1) {
                if (enemyDistance > PrimaryState.FAR) {
                   return Bot.OFENSIVEPROFILE;
                }
                else if (enemyDistance < PrimaryState.AVERAGE/3) {
                    return Bot.DEFENSIVEPROFILE;
                }
            }
            // FAR DISTANCE
            else if (sweetSpot == 2) {
                if (enemyDistance < PrimaryState.FAR/2) {
                   return Bot.DEFENSIVEPROFILE;
                }
            }
        }

        return Bot.DISABLED;
    }

    //__________________________________________________________________________

    /**
     * Estimate a primary state for the FSM.
     * @param info Information about the agent.
     * @param weaponry Weaponry information.
     * @param enemy Enemy information.
     * @param enemyInfo Guessed information about the enemy.
     * @param game Game information.
     * @return An integer that represents a primary state.
     */
    public int estimatePrimaryState (AgentInfo info, Weaponry weaponry, Player enemy, EnemyInfo enemyInfo, Game game) {
        int arsenalScore;
        int arsenalStatus [] = compareArsenals (weaponry, enemyInfo.getArsenal ());

        // ESTIMATE HOW GOOD OUR ARSENAL IS WHEN COMPARED TO THE ENEMY'S

        arsenalScore = arsenalStatus [0];
        for (int i = 1; i < arsenalStatus.length; ++i) {
            if (arsenalStatus [i] >= arsenalScore) {
                arsenalScore = arsenalStatus [i];
            }
        }

        int health = info.getHealth ();
        int enemyHealth = enemyInfo.getHealth ();

        double elapsedTime = Math.abs (enemyInfo.getLastTimeMet () - game.getTime ());

        if (health < 50) {
            if (enemyHealth < 100) {
                if (arsenalScore >= 3) {
                    if (enemy != null) {
                        return Bot.ATTACK;
                    }
                    else {
                        return Bot.HUNT;
                    }
                }
                else {
                    return Bot.RETREAT;
                }
            }
            else {
                if (enemy != null) {
                    return Bot.RETREAT;
                }
                else {
                    if (elapsedTime < 7) {
                        return Bot.RETREAT;
                    }
                    else {
                        return Bot.GREEDY;
                    }
                }
            }
        }
        else {
            if (enemyHealth < 120) {
                if (arsenalScore >= 2) {
                    if (enemy != null) {
                        return Bot.ATTACK;
                    }
                    else {
                        if (elapsedTime < 7) {
                            return Bot.HUNT;
                        }
                        else {
                            return Bot.GREEDY;
                        }
                    }
                }
                else {
                    if (enemy != null) {
                        return Bot.RETREAT;
                    }
                    else {
                        if (elapsedTime < 7) {
                            return Bot.RETREAT;
                        }
                        else {
                            return Bot.GREEDY;
                        }
                    }
                }
            }
            else {
                if (arsenalScore >= 3) {
                    if (enemy != null) {
                        return Bot.ATTACK;
                    }
                    else {
                        if (elapsedTime < 7) {
                            return Bot.RETREAT;
                        }
                        else {
                            return Bot.GREEDY;
                        }
                    }
                }
                else {
                    if (enemy != null) {
                        return Bot.RETREAT;
                    }
                    else {
                        if (elapsedTime < 7) {
                            return Bot.RETREAT;
                        }
                        else {
                            return Bot.GREEDY;
                        }
                    }
                }
            }
        }
    }

    //__________________________________________________________________________

    /**
     * Estimate the secondary state, based on the bot needs.
     * @param primaryState Primary state that has been chosen.
     * @param info Information about the agent.
     * @param weaponry Weaponry information.
     * @param enemy Enemy information.
     * @param enemyInfo Guessed information about the enemy.
     * @return An integer that represents a secondary state to be chosen.
     */
    public int estimateSecondaryState (int primaryState, AgentInfo info, Weaponry weaponry, Player enemy, EnemyInfo enemyInfo) {
        int arsenalScore, bestDefensiveRange, bestOfensiveRange;
        int arsenalStatus [] = compareArsenals (weaponry, enemyInfo.getArsenal ());

        // ESTIMATE HOW GOOD OUR ARSENAL IS WHEN COMPARED TO THE ENEMY'S

        // arsenalScore is going to hold the best score our arsenal gets in any range
        // bestDefensiveRange is going to contain the furthest range where our arsenal is the best
        // bestOfensiveRange is going to contain the closest range where our arsenal is the best
        // Note that bestDefensiveRange != bestOfensiveRange only when the best score
        // is repeated.

        arsenalScore = arsenalStatus [0];
        bestDefensiveRange = bestOfensiveRange = 0;
        for (int i = 1; i < arsenalStatus.length; ++i) {
            if (arsenalStatus [i] >= arsenalScore) {
                if (arsenalStatus [i] == arsenalScore) {
                    bestDefensiveRange = i;
                }
                else {
                    bestOfensiveRange = i;
                    bestDefensiveRange = i;
                }

                arsenalScore = arsenalStatus [i];
            }
        }

        int health = info.getHealth ();

        double enemyDistance = 0;
        if (enemy != null) {
            enemyDistance = info.getDistance (enemy.getLocation ());
        }

        if (health < 30) {
            return Bot.CRITICALHEALTH;
        }
        else {
           if (primaryState == Bot.ATTACK) {
               if (health < 60) {
                   return Bot.PICKUPHEALTH;
               }
               else {
                   if (enemy != null) {
                       return estimateProfile (enemyDistance, arsenalScore, 0, bestOfensiveRange);
                   }
                   else {
                       return Bot.PICKUPHEALTH;
                   }
               }
           }
           else if (primaryState == Bot.RETREAT) {
               if (health < 60) {
                   return Bot.PICKUPHEALTH;
               }
               else {
                   if (arsenalScore < 3) {
                       return Bot.PICKUPWEAPON;
                   }
                   else {
                       return Bot.PICKUPHEALTH;
                   }
               }
           }
           else if (primaryState == Bot.HUNT) {
               if (health < 60) {
                   return Bot.PICKUPHEALTH;
               }
               else if (health < 120) {
                   if (arsenalScore < 3) {
                       return Bot.PICKUPWEAPON;
                   }
                   else {
                       return Bot.PICKUPHEALTH;
                   }
               }
               else {
                   if (arsenalScore < 3) {
                       return Bot.PICKUPWEAPON;
                   }
                   else {
                       return Bot.PICKUPAMMO;
                   }
               }
           }
        }

        return Bot.DISABLED;
    }



    //__________________________________________________________________________

    /**
     * Depending on if there is a feasible spam or combo, it returns a target. If
     * there's not a feasible combo or spam it returns null.
     * @return A Location if there is a feasible combo or spam, or null in any other
     * case.
     */
    public Location estimateTarget () {
        if (comboLocation != null) {
            PrimaryState.feasibleCombo ();

            return comboLocation;
        }

        // Check if we should spam. If we should spam, then we should return the spam
        // Location.

        return null;
    }

    //__________________________________________________________________________

    /**
     * Depending on the kind of projectile, it checks whether it is a plausible combo
     * or not (to blow it with the shock).
     * @param projectile Projectile that we are seeing.
     * @param enemy Enemy
     */
    public void incomingProjectile (final IncomingProjectile projectile, final Player enemy) {
        // If the projectile is a feasible combo, we update the combo Location.
        // spamLocation indicates if the spot where we want to blow the combo is a spam.
        if (projectile.getType().equals ("XWeapons.ShockProjectile")) {
            if (enemy != null || spamLocation != null) {
                Location targetPosition = null;
                if (enemy != null) {
                    targetPosition = enemy.getLocation();
                }
                else if (spamLocation != null) {
                    targetPosition = spamLocation;
                }

                Location projectilePosition = projectile.getLocation();
                if (Location.getDistance (targetPosition, projectilePosition) < 600) {
                    comboLocation = projectile.getLocation();
                }
            }
        }
    }
}
