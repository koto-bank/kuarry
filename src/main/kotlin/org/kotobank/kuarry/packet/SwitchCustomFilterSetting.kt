package org.kotobank.kuarry.packet

import io.netty.buffer.ByteBuf
import net.minecraftforge.fml.common.network.ByteBufUtils
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import net.minecraftforge.fml.relauncher.Side
import org.kotobank.kuarry.item.KuarryCustomFilter

class SwitchCustomFilterSetting(var setting: Setting) : IMessage {
    constructor() : this(Setting.Empty)

    /** The setting to switch in the TE */
    enum class Setting {
        Empty, Mode, BlacklistMode
    }

    override fun toBytes(buf: ByteBuf) {
        ByteBufUtils.writeUTF8String(buf, setting.name)
    }

    override fun fromBytes(buf: ByteBuf?) {
        setting = Setting.valueOf(ByteBufUtils.readUTF8String(buf))
    }

    class Handler : IMessageHandler<SwitchCustomFilterSetting, IMessage> {
        override fun onMessage(message: SwitchCustomFilterSetting, ctx: MessageContext): IMessage? {
            if (ctx.side == Side.SERVER) {
                val player = ctx.serverHandler.player

                val mainHand = player.heldItemMainhand
                // Check if the item in the main hand is actually a filter
                if (mainHand.item is KuarryCustomFilter) {
                    when (message.setting) {
                        Setting.Empty -> {}
                        Setting.Mode -> KuarryCustomFilter.switchMode(mainHand)
                        Setting.BlacklistMode -> KuarryCustomFilter.switchBlacklistMode(mainHand)
                    }
                }
            }

            return null
        }
    }
}