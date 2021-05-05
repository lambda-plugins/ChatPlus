import com.lambda.client.command.CommandManager
import com.lambda.client.module.Category
import com.lambda.client.plugin.api.PluginModule

import com.lambda.client.event.events.PacketEvent
import com.lambda.client.manager.managers.FriendManager
import com.lambda.client.manager.managers.MessageManager
import com.lambda.client.manager.managers.MessageManager.newMessageModifier
import com.lambda.client.mixin.extension.textComponent
import com.lambda.client.util.TickTimer
import com.lambda.client.util.TimeUnit
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
import net.minecraftforge.fml.common.gameevent.TickEvent

internal object ChatPlusAutoReply: PluginModule(
    name = "AutoReply",
    description = "Automatically reply to direct messages",
    category = Category.CHAT,
    pluginMain = ChatPlusPlugin
) {
    private val customMessage = setting("Custom Message", false)
    private val customText = setting("Custom Text", "unchanged", { customMessage.value })

    private val timer = TickTimer(TimeUnit.SECONDS)

    init {
        listener<PacketEvent.Receive> {
            if (it.packet !is SPacketChat || MessageDetection.Direct.RECEIVE detect (it.packet as SPacketChat).chatComponent.unformattedText) return@listener
            if (customMessage.value) {
                sendServerMessage("/r " + customText.value)
            } else {
                sendServerMessage("/r I just automatically replied, thanks to Lambda's AutoReply module!")
            }
        }

        safeListener<TickEvent.ClientTickEvent> {
            if (timer.tick(5L) && customMessage.value && customText.value.equals("unchanged", true)) {
                MessageSendHelper.sendWarningMessage("$chatName Warning: In order to use the custom $name, please change the CustomText setting in ClickGUI")
            }
        }
    }
}