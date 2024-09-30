package co.sakurastudios.ai1v1bot

import co.sakurastudios.ai1v1bot.commands.AICommand
import org.bukkit.plugin.java.JavaPlugin

class AI1v1Bot : JavaPlugin() {


    override fun onEnable() {
        getCommand("aibot")?.setExecutor(AICommand(this))
    }
}
