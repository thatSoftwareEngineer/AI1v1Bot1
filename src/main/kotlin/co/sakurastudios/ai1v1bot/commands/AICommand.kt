package co.sakurastudios.ai1v1bot.commands

import co.sakurastudios.ai1v1bot.AI1v1Bot
import co.sakurastudios.ai1v1bot.manager.CombatManager
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.npc.NPC
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class AICommand(private val plugin: AI1v1Bot): CommandExecutor {
    private var npcRegistry = CitizensAPI.getNPCRegistry()
    var bot: NPC? = null
    lateinit var opponent: Player
    lateinit var combatManager: CombatManager

    override fun onCommand(p0: CommandSender, p1: Command, p2: String, p3: Array<out String>): Boolean {
        if (p3.size != 1) return false
        opponent = Bukkit.getPlayerExact(p3[0])!!

        // Create and spawn the NPC
        bot = npcRegistry.createNPC(org.bukkit.entity.EntityType.PLAYER, "AI 1v1 Bot")
        bot?.spawn(opponent.location)
        bot?.isProtected = false

        // Set up combat manager to handle the bot's combat logic
        combatManager = CombatManager(bot!!, opponent,plugin)
        combatManager.startCombatTask()

        return true
    }
}