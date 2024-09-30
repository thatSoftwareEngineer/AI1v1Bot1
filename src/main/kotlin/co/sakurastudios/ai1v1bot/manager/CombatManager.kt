package co.sakurastudios.ai1v1bot.manager

import co.sakurastudios.ai1v1bot.AI1v1Bot
import net.citizensnpcs.api.npc.NPC
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector

class CombatManager(
    private val npc: NPC,
    private val opponent: Player,
    private val plugin: AI1v1Bot
) {
    private val npcEntity = npc.entity as Player
    private var isUsingBow = false
    private var isFleeing = false
    private var isReturning = false
    private val fleeHealthThreshold = 10.0  // Health at which NPC will flee
    private val returnHealthThreshold = 15.0  // Health at which NPC will return to fight
    private val normalSpeed = 1.0  // Default speed
    private val fleeSpeed = 2.0  // Speed while fleeing

    fun startCombatTask() {
        object : BukkitRunnable() {
            override fun run() {
                if (!npc.isSpawned || opponent.isDead || npcEntity.isDead) {
                    cancel()
                    return
                }

                armTheDude()

                val distance = npcEntity.location.distance(opponent.location)
                val health = npcEntity.health

                // Flee if health is low
                if (health < fleeHealthThreshold && !isFleeing) {
                    val npcHealth = getNpcHealth()
                    println("NPC's current health: $npcHealth")

                    println("NPC health is low ($health), fleeing...")
                    isFleeing = true
                    isReturning = false
                    fleeFromPlayer()
                    return
                }

                // Return to combat if health is high enough
                if (isFleeing && health >= returnHealthThreshold) {
                    println("NPC health has recovered ($health), returning to combat...")
                    isFleeing = false
                    isReturning = true
                    returnToCombat()
                    return
                }

                // Engage in combat if not fleeing or returning
                if (!isFleeing && !isReturning) {
                    println("NPC is engaging in combat, distance to player: $distance")
                    combatLogic(distance)
                }
            }
        }.runTaskTimer(plugin, 0L, 20L)
    }

    // Arms the NPC with sword and armor
    private fun armTheDude() {
        val sword = ItemStack(Material.DIAMOND_SWORD)
        val head = ItemStack(Material.DIAMOND_HELMET)
        val chest = ItemStack(Material.DIAMOND_CHESTPLATE)
        val leg = ItemStack(Material.DIAMOND_LEGGINGS)
        val boot = ItemStack(Material.DIAMOND_BOOTS)

        npcEntity.inventory.helmet = head
        npcEntity.inventory.chestplate = chest
        npcEntity.inventory.leggings = leg
        npcEntity.inventory.boots = boot
        npcEntity.inventory.setItemInMainHand(sword)
    }

    // Main combat logic for melee, ranged, and avoiding danger
    private fun combatLogic(distance: Double) {
        if (isDangerNearby()) {
            println("NPC detected danger, avoiding...")
            avoidDanger()
            return
        }

        if (distance > 10) {
            switchToBow()
            fireBow()
        } else {
            switchToSword()
            if (distance < 4) {
                performMeleeAttack()
            }
        }

        // Follow the player dynamically using pathfinding
        if (distance > 3) {
            npc.navigator.localParameters.speedModifier(normalSpeed.toFloat())
            npc.navigator.setTarget(opponent, false)
        }
    }

    // Switch to sword if the bot is close enough to the player
    private fun switchToSword() {
        if (isUsingBow) {
            isUsingBow = false
            val sword = ItemStack(Material.DIAMOND_SWORD)
            npcEntity.inventory.setItemInMainHand(sword)
            println("NPC switched to sword")
        }
    }

    // Switch to bow if the bot is far enough from the player
    private fun switchToBow() {
        if (!isUsingBow) {
            isUsingBow = true
            val bow = ItemStack(Material.BOW)
            npcEntity.inventory.setItemInMainHand(bow)
            println("NPC switched to bow")
        }
    }

    // Perform melee attack with a chance of critical hit
    private fun performMeleeAttack() {
        val isFalling = npcEntity.velocity.y < 0

        if (isFalling) {
            println("NPC performed critical hit")
            opponent.damage(6.0, npcEntity)  // Critical hit (higher damage)
        } else {
            println("NPC performed normal attack")
            opponent.damage(2.0, npcEntity)  // Normal sword attack
        }
    }

    // Basic bow shooting logic
    private fun fireBow() {
        val arrow = npcEntity.launchProjectile(org.bukkit.entity.Arrow::class.java)
        arrow.velocity = opponent.location.subtract(npcEntity.location).toVector().normalize().multiply(1.5)
        println("NPC fired an arrow")
    }

    // Logic to make the NPC flee from the player, avoiding dangerous blocks
    private fun fleeFromPlayer() {
        npc.navigator.localParameters.speedModifier(fleeSpeed.toFloat())

        // Get the vector in the opposite direction of the player
        val direction = npcEntity.location.toVector().subtract(opponent.location.toVector()).normalize()
        val fleeDistance = 25.0  // Set a fixed flee distance
        var fleeLocation = npcEntity.location.clone().add(direction.multiply(fleeDistance))

        // Make sure the target location is safe (avoid lava or dangerous blocks)
        if (isLavaNearbyAtLocation(fleeLocation)) {
            println("Detected lava at flee location, recalculating a safe path")
            // If there's lava nearby, adjust the flee direction
            fleeLocation = findSafeLocation(npcEntity.location)
        }

        npc.navigator.setTarget(fleeLocation)
        println("NPC is fleeing to: $fleeLocation")
    }

    // Function to check if a specific location has lava nearby
    private fun isLavaNearbyAtLocation(location: Location): Boolean {
        val blockBelow = location.block.getRelative(BlockFace.DOWN)
        return blockBelow.type == Material.LAVA || blockBelow.type == Material.MAGMA_BLOCK
    }

    // Function to find a safe location (not near lava or dangerous blocks)
    private fun findSafeLocation(startLocation: Location): Location {
        val directions = listOf(
            Vector(1, 0, 0),  // North
            Vector(-1, 0, 0), // South
            Vector(0, 0, 1),  // East
            Vector(0, 0, -1)  // West
        )

        for (direction in directions) {
            val possibleLocation = startLocation.clone().add(direction.multiply(10))
            if (!isLavaNearbyAtLocation(possibleLocation)) {
                println("Safe flee location found: $possibleLocation")
                return possibleLocation
            }
        }

        // If no safe location is found, return the original location
        println("No safe location found, returning to original flee location")
        return startLocation
    }


    // Return to combat once health is high enough
    private fun returnToCombat() {
        npc.navigator.localParameters.speedModifier(normalSpeed.toFloat())
        npc.navigator.setTarget(opponent.location)
        println("NPC is returning to combat")

        object : BukkitRunnable() {
            override fun run() {
                val distance = npcEntity.location.distance(opponent.location)

                // If the NPC is close enough, stop returning and engage
                if (distance < 5) {
                    isReturning = false
                    println("NPC has returned to combat and is now engaging")
                    cancel()
                }
            }
        }.runTaskTimer(plugin, 0L, 20L)
    }

    // Check for nearby danger such as lava, cliffs, or hostile mobs
    private fun isDangerNearby(): Boolean {
        val dangerousBlocksNearby = isLavaNearby() || isCliffNearby()
        return dangerousBlocksNearby
    }

    // Helper function to check for lava near the NPC
    private fun isLavaNearby(): Boolean {
        val blockBelow = npcEntity.location.block.getRelative(BlockFace.DOWN)
        return blockBelow.type == Material.LAVA || blockBelow.type == Material.MAGMA_BLOCK
    }

    // Helper function to check for cliffs (i.e., no blocks below, suggesting a fall)
    private fun isCliffNearby(): Boolean {
        val blockBelow = npcEntity.location.block.getRelative(BlockFace.DOWN)
        return blockBelow.type == Material.AIR
    }

    // Avoid danger by moving away from the threat
    private fun avoidDanger() {
        val awayFromDanger = Vector(-1, 0, -1)  // Move in a safer direction (just an example)
        val newLocation = npcEntity.location.add(awayFromDanger.multiply(10))
        npc.navigator.localParameters.speedModifier(fleeSpeed.toFloat())
        npc.navigator.setTarget(newLocation)
        println("Avoiding danger, moving to safer location: $newLocation")
    }

    // Method to get the NPC's current health
    fun getNpcHealth(): Double {
        return npcEntity.health  // Returns the NPC's health as a double
    }


}
