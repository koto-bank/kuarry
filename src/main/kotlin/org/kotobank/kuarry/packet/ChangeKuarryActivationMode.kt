package org.kotobank.kuarry.packet

import io.netty.buffer.ByteBuf
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import net.minecraftforge.fml.relauncher.Side
import org.kotobank.kuarry.tile_entity.KuarryTileEntity

/** Requests the [KuarryTileEntity] to change its [KuarryTileEntity.activationMode]
 */
class ChangeKuarryActivationMode(var pos: BlockPos) : IMessage {
    constructor() : this(BlockPos(0, 0, 0))

    override fun toBytes(buf: ByteBuf?) {
        buf!!.writeLong(pos.toLong())
    }

    override fun fromBytes(buf: ByteBuf?) {
        pos = BlockPos.fromLong(buf!!.readLong())
    }

    class Handler : IMessageHandler<ChangeKuarryActivationMode, IMessage> {
        override fun onMessage(message: ChangeKuarryActivationMode?, ctx: MessageContext?): IMessage? {
            if (ctx!!.side == Side.SERVER) {
                val world = ctx.serverHandler.player.world

                val tileEntity = world.getTileEntity(message!!.pos)
                if (tileEntity is KuarryTileEntity) {
                    tileEntity.switchActivationMode()
                }
            }

            return null
        }
    }
}