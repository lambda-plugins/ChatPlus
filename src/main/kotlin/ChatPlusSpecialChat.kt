import com.lambda.client.manager.managers.MessageManager.newMessageModifier
import com.lambda.client.module.Category
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.util.text.MessageSendHelper

internal object ChatPlusSpecialChat : PluginModule(
    name = "SpecialChat",
    description = "Add color and linebreak support to upstream chat packets using formatting",
    category = Category.CHAT,
    modulePriority = 300,
    pluginMain = ChatPlusPlugin
) {
    private val modifier = newMessageModifier {
        it.packet.message
            .replace('&', '')
            .replace("#n", "\n")
    }

    init {
        onEnable {
            if (mc.currentServerData == null) {
                MessageSendHelper.sendWarningMessage("$chatName &6&lWarning: &r&6This does not work in singleplayer")
                disable()
            } else {
                MessageSendHelper.sendWarningMessage("$chatName &6&lWarning: &r&6This will kick you on most servers!")
                modifier.enable()
            }
        }

        onDisable {
            modifier.enable()
        }
    }
}