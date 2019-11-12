package org.kotobank.kuarry.packet

import io.netty.buffer.ByteBuf
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.network.ByteBufUtils
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import net.minecraftforge.fml.relauncher.Side
import org.kotobank.kuarry.tile_entity.KuarryTileEntity

/** Requests the [KuarryTileEntity] to change circle one of it's settings
 */
class SwitchKuarrySetting(var pos: BlockPos, var setting: Setting) : IMessage {
    constructor() : this(BlockPos(0, 0, 0), Setting.Empty)

    /** The setting to switch in the TE */
    enum class Setting {
        Empty, ActivationMode, RenderBounds
    }

    override fun toBytes(buf: ByteBuf) {
        ByteBufUtils.writeUTF8String(buf, setting.name)
        buf.writeLong(pos.toLong())
    }

    override fun fromBytes(buf: ByteBuf?) {
        setting = Setting.valueOf(ByteBufUtils.readUTF8String(buf))
        pos = BlockPos.fromLong(buf!!.readLong())
    }

    class Handler : IMessageHandler<SwitchKuarrySetting, IMessage> {
        override fun onMessage(message: SwitchKuarrySetting, ctx: MessageContext): IMessage? {
            if (ctx.side == Side.SERVER) {
                val world = ctx.serverHandler.player.world

                val tileEntity = world.getTileEntity(message.pos)
                require(tileEntity is KuarryTileEntity)

                // Depending on the setting in the message that came it, switch the appropriate
                // setting in the TE
                when (message.setting) {
                    Setting.Empty -> {}
                    Setting.ActivationMode -> tileEntity.switchActivationMode()
                    Setting.RenderBounds -> tileEntity.toggleRenderBounds()
                }
            }

            return null
        }
    }
}