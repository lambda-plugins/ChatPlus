import com.google.gson.JsonObject
import com.lambda.client.LambdaMod
import com.lambda.client.command.CommandManager
import com.lambda.client.event.events.ConnectionEvent
import com.lambda.client.event.events.PacketEvent
import com.lambda.client.module.Category
import com.lambda.client.module.modules.chat.ChatTimestamp
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.util.TickTimer
import com.lambda.client.util.TimeUnit
import com.lambda.client.util.text.MessageDetection
import com.lambda.client.util.text.MessageSendHelper
import com.lambda.client.util.text.formatValue
import com.lambda.client.util.threads.defaultScope
import com.lambda.client.util.threads.safeListener
import com.lambda.client.commons.utils.ConnectionUtils
import com.lambda.client.event.listener.listener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.network.play.server.SPacketChat
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.apache.commons.io.IOUtils

internal object ChatPlusDiscordNotifs : PluginModule(
    name = "DiscordNotifs",
    category = Category.CHAT,
    description = "Sends your chat to a set Discord channel",
    pluginMain = ChatPlusPlugin
) {
    private val timeout by setting("Timeout", true)
    private val timeoutTime by setting("Seconds", 10, 0..120, 5, { timeout })
    private val time by setting("Timestamp", true)
    private val importantPings by setting("Important Pings", false)
    private val connectionChange by setting("Connection Change", true, description = "When you get disconnected or reconnected to the server")
    private val all by setting("All Messages", false)
    private val direct by setting("DMs", true, { !all })
    private val queue by setting("Queue Position", true, { !all })
    private val restart by setting("Restart", true, { !all }, description = "Server restart notifications")

    val url = setting("URL", "unchanged")
    val pingID = setting("Ping ID", "unchanged")
    val avatar = setting("Avatar", "${LambdaMod.GITHUB_LINK}/assets/raw/assets/assets/icons/kamiGithub.png")

    private const val username = "${LambdaMod.NAME} ${LambdaMod.VERSION}"
    private val server: String get() = mc.currentServerData?.serverIP ?: "the server"
    private val timer = TickTimer(TimeUnit.SECONDS)

    /* Listeners to send the messages */
    init {
        safeListener<PacketEvent.Receive> {
            if (it.packet !is SPacketChat) return@safeListener
            val message = (it.packet as SPacketChat).chatComponent.unformattedText

            if (timeout(message) && shouldSend(message)) {
                sendMessage(getPingID(message) + getMessageType(message, server) + getTime() + message)
            }
        }

        listener<ConnectionEvent.Connect> {
            if (!connectionChange) return@listener
            sendMessage(getPingID("LambdaMessageType1") + getTime() + getMessageType("LambdaMessageType1", server))
        }

        listener<ConnectionEvent.Disconnect> {
            if (!connectionChange) return@listener
            sendMessage(getPingID("LambdaMessageType2") + getTime() + getMessageType("LambdaMessageType2", server))
        }

        /* Always on status code */
        safeListener<TickEvent.ClientTickEvent> {
            if (url.value == "unchanged") {
                MessageSendHelper.sendErrorMessage(
                    chatName + " You must first set a webhook url with the " +
                            formatValue("${CommandManager.prefix}discordnotifs") +
                            " command"
                )
                disable()
            } else if (pingID.value == "unchanged" && importantPings) {
                MessageSendHelper.sendErrorMessage(
                    chatName + " For Pings to work, you must set a Discord ID with the " +
                            formatValue("${CommandManager.prefix}discordnotifs") +
                            " command"
                )
                disable()
            }
        }
    }

    private fun shouldSend(message: String): Boolean {
        return all
                || direct && MessageDetection.Direct.ANY detect message
                || restart && MessageDetection.Server.RESTART detect message
                || queue && MessageDetection.Server.QUEUE detect message
    }

    private fun getMessageType(message: String, server: String): String {
        if (direct && MessageDetection.Direct.RECEIVE detect message) return "You got a direct message!\n"
        if (direct && MessageDetection.Direct.SENT detect message) return "You sent a direct message!\n"
        if (message == "LambdaMessageType1") return "Connected to $server"
        return if (message == "LambdaMessageType2") "Disconnected from $server" else ""
    }

    private fun timeout(message: String) = !timeout
            || restart && MessageDetection.Server.RESTART detect message
            || direct && MessageDetection.Direct.ANY detect message
            || timer.tick(timeoutTime.toLong())

    /* Text formatting and misc methods */
    private fun getPingID(message: String) = if (message == "LambdaMessageType1"
        || message == "LambdaMessageType2"
        || direct && MessageDetection.Direct.ANY detect message
        || restart && MessageDetection.Server.RESTART detect message
        || importantPings && MessageDetection.Server.QUEUE_IMPORTANT detect message
    ) formatPingID()
    else ""

    private fun formatPingID(): String {
        return if (!importantPings) "" else "<@!${pingID.value}>: "
    }

    private fun getTime() =
        if (!time) ""
        else ChatTimestamp.time

    private fun sendMessage(content: String) {
        defaultScope.launch(Dispatchers.IO) {
            ConnectionUtils.runConnection(
                url.value,
                { connection ->
                    val bytes = JsonObject().run {
                        addProperty("username", username)
                        addProperty("content", content)
                        addProperty("avatar_url", avatar.value)
                        toString().toByteArray(Charsets.UTF_8)
                    }

                    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    connection.setRequestProperty("Accept", "application/json")
                    connection.setRequestProperty("User-Agent", "")

                    connection.requestMethod = "POST"
                    connection.outputStream.use {
                        it.write(bytes)
                    }

                    val response = connection.inputStream.use {
                        IOUtils.toString(it, Charsets.UTF_8)
                    }

                    if (response.isNotEmpty()) {
                        LambdaMod.LOG.info("Unexpected response from DiscordNotifs http request: $response")
                    }
                },
                {
                    LambdaMod.LOG.warn("Error while sending webhook", it)
                },
            )
        }
    }
}