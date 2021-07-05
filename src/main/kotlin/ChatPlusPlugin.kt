import com.lambda.client.plugin.api.Plugin

internal object ChatPlusPlugin : Plugin() {

    override fun onLoad() {
        modules.add(ChatPlusEncryption)
        modules.add(ChatPlusDiscordNotifs)
        modules.add(ChatPlusRemoteCommand)
        modules.add(ChatPlusSpecialChat)
        modules.add(ChatPlusAutoReply)
    }

    override fun onUnload() {}
}