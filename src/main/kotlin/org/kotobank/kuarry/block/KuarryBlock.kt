package org.kotobank.kuarry.block

import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.properties.PropertyDirection
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.kotobank.kuarry.KuarryMod
import org.kotobank.kuarry.KuarryModGUIHandler
import org.kotobank.kuarry.tile_entity.KuarryTileEntity

class KuarryBlock(material: Material, registryName: String) : Block(material) {
    companion object {
        val FACING: PropertyDirection =
                PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL)

        val tileEntityClass = KuarryTileEntity::class.java
    }

    init {
        setUnlocalizedName(registryName)

        setRegistryName(KuarryMod.MODID, registryName)

        setHardness(2f)
        setResistance(5f)
    }

    override fun hasTileEntity(state: IBlockState) = true

    override fun createTileEntity(world: World, state: IBlockState): TileEntity? = KuarryTileEntity();


    override fun onBlockActivated(worldIn: World, pos: BlockPos, state: IBlockState,
                                  playerIn: EntityPlayer, hand: EnumHand, facing: EnumFacing,
                                  hitX: Float, hitY: Float, hitZ: Float): Boolean {
        if (!worldIn.isRemote) {
            playerIn.openGui(KuarryMod, KuarryModGUIHandler.KUARRY, worldIn, pos.x, pos.y, pos.z)
        }

        return true;
    }

    override fun createBlockState() = BlockStateContainer(this, FACING)

    override fun getMetaFromState(state: IBlockState) = state.getValue(FACING).index

    override fun getStateFromMeta(meta: Int): IBlockState =
            defaultState.withProperty(FACING, EnumFacing.getHorizontal(meta))


    override fun getStateForPlacement(world: World, pos: BlockPos, facing: EnumFacing,
                                      hitX: Float, hitY: Float, hitZ: Float,
                                      meta: Int, placer: EntityLivingBase, hand: EnumHand): IBlockState =
            super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand)
                    .withProperty(FACING, placer.horizontalFacing.opposite)

}