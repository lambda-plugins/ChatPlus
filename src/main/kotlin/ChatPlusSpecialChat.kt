import com.lambda.client.command.CommandManager
import com.lambda.client.module.Category
import com.lambda.client.plugin.api.PluginModule

import com.lambda.client.event.events.PacketEvent
import com.lambda.client.manager.managers.FriendManager
import com.lambda.client.manager.managers.MessageManager
import com.lambda.client.manager.managers.MessageManager.newMessageModifier
import com.lambda.client.mixin.extension.textComponent
import com.lambda.client.util.text.MessageDetection
import com.lambda.client.util.text.MessageSendHelper
import com.lambda.client.util.text.MessageSendHelper.sendServerMessage
import com.lambda.client.util.text.format
import com.lambda.client.util.text.formatValue
import com.lambda.client.util.threads.defaultScope
import com.lambda.client.util.threads.safeListener
import com.lambda.commons.utils.SystemUtils
import com.lambda.event.listener.listener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.network.play.server.SPacketChat
import net.minecraft.util.text.TextFormatting

internal object ChatPlusSpecialChat: PluginModule(
    name = "SpecialChat",
    description = "Add color and linebreak support to upstream chat packets using formatting",
    category = Category.CHAT,
    modulePriority = 300,
    pluginMain = ChatPlusPlugin
) {
    private val modifier = newMessageModifier {
        it.packet.message
            .replace('&', '§')
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