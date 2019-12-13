package org.kotobank.kuarry.block

import java.util.ArrayList
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.properties.PropertyDirection
import net.minecraft.block.properties.PropertyInteger
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.resources.I18n
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.NonNullList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.text.TextFormatting
import net.minecraftforge.fml.common.Optional
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import net.minecraftforge.items.ItemStackHandler
import org.kotobank.kuarry.helper.TranslationHelper
import org.kotobank.kuarry.item.LevelUpgrade
import org.kotobank.kuarry.KuarryMod
import org.kotobank.kuarry.KuarryModGUIHandler
import org.kotobank.kuarry.tile_entity.KuarryTileEntity
import cofh.api.block.IDismantleable

@Optional.Interface(iface = "cofh.api.block.IDismantleable", modid = "cofhcore")
class KuarryBlock(material: Material, registryName: String) : Block(material), IDismantleable {
    companion object {
        val FACING: PropertyDirection =
                PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL)

        val LEVEL: PropertyInteger = PropertyInteger.create("level", 0, 2)

        val tileEntityClass = KuarryTileEntity::class.java
    }

    init {
        setUnlocalizedName(registryName)

        setRegistryName(KuarryMod.MODID, registryName)

        setHardness(2f)
        setResistance(5f)
        setHarvestLevel("pickaxe", 1)
    }

    override fun hasTileEntity(state: IBlockState) = true

    override fun createTileEntity(world: World, state: IBlockState): TileEntity? = KuarryTileEntity()


    override fun onBlockActivated(worldIn: World, pos: BlockPos, state: IBlockState,
                                  playerIn: EntityPlayer, hand: EnumHand, facing: EnumFacing,
                                  hitX: Float, hitY: Float, hitZ: Float): Boolean {
        if (!worldIn.isRemote) {
            val item = playerIn.getHeldItem(hand).item
            // If the item used is an upgrade, DON'T open the GUI, because the block will either be upgraded
            // or the player will be sent a message that the upgrade it not appropriate for the level
            if (item is LevelUpgrade) {
                return false
            }

            playerIn.openGui(KuarryMod, KuarryModGUIHandler.KUARRY, worldIn, pos.x, pos.y, pos.z)
        }

        return true
    }

    override fun createBlockState() = BlockStateContainer(this, FACING, LEVEL)

    override fun getMetaFromState(state: IBlockState) = state.getValue(FACING).index

    override fun getStateFromMeta(meta: Int): IBlockState =
            defaultState.withProperty(FACING, EnumFacing.getHorizontal(meta))


    override fun getStateForPlacement(world: World, pos: BlockPos, facing: EnumFacing,
                                      hitX: Float, hitY: Float, hitZ: Float,
                                      meta: Int, placer: EntityLivingBase, hand: EnumHand): IBlockState =
            super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand)
                    .withProperty(FACING, placer.horizontalFacing.opposite)

    override fun hasComparatorInputOverride(state: IBlockState) = true

    override fun getComparatorInputOverride(blockState: IBlockState, worldIn: World, pos: BlockPos): Int {
        val tileEntity = worldIn.getTileEntity(pos)

        // Give a strong comparator signal when there is no more resources to mine
        return if (tileEntity is KuarryTileEntity && tileEntity.approxResourcesLeft == 0) {
            15
        } else {
            0
        }
    }

    override fun getDrops(drops: NonNullList<ItemStack>, world: IBlockAccess, pos: BlockPos, state: IBlockState, fortune: Int) {
        val rand = if (world is World) world.rand else RANDOM

        val tileEntity = world.getTileEntity(pos)
        if (tileEntity is KuarryTileEntity) {
            val stack = ItemStack(getItemDropped(state, rand, fortune))
            stack.tagCompound = tileEntity.writeModNBT(NBTTagCompound())

            drops.add(stack)
        }
    }

    override fun removedByPlayer(state: IBlockState, world: World, pos: BlockPos, player: EntityPlayer, willHarvest: Boolean): Boolean {
        if (willHarvest)
            return true //If it will harvest, delay deletion of the block until after getDrops﻿﻿
        return super.removedByPlayer(state, world, pos, player, willHarvest)
    }

    override fun harvestBlock(worldIn: World, player: EntityPlayer, pos: BlockPos, state: IBlockState, te: TileEntity?, stack: ItemStack) {
        super.harvestBlock(worldIn, player, pos, state, te, stack)

        // Actually delete the block
        worldIn.setBlockToAir(pos)
    }

    override fun onBlockPlacedBy(worldIn: World, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, stack: ItemStack) {
        val tileEntity = worldIn.getTileEntity(pos)

        // Restore the data stored in the item if there's any
        val compound = stack.tagCompound
        if (tileEntity is KuarryTileEntity && compound != null) {
            tileEntity.readModNBT(compound)
        }
    }

    @SideOnly(Side.CLIENT)
    override fun addInformation(itemStack: ItemStack, player: World?, tooltip: MutableList<String>, advanced: ITooltipFlag) {
        if (GuiScreen.isShiftKeyDown()) {
            tooltip.add(I18n.format("tile.kuarry.tooltips.description"))
        } else {
            tooltip.add(TranslationHelper.shiftForDetailsTooltip)
        }

        // Only react if it's a tooltip for the kuarry block
        val compound = itemStack.tagCompound
        if (compound != null) {
            val energyStored = compound.getInteger("energy")
            val itemCount = run {
                val handler = ItemStackHandler(KuarryTileEntity.inventorySize)
                handler.deserializeNBT(compound.getCompoundTag("inventory"))

                var itemC = 0
                for (i in 0 until KuarryTileEntity.inventorySize)
                    itemC += handler.getStackInSlot(i).count

                itemC
            }

            tooltip.addAll(listOf(
                    "${I18n.format("tile.kuarry.tooltips.energy_stored", TextFormatting.BLUE)}: ${energyStored}RF",
                    "${I18n.format("tile.kuarry.tooltips.items_stored", TextFormatting.BLUE)}: $itemCount"
            ))
        }
    }

    override fun getActualState(state: IBlockState, worldIn: IBlockAccess, pos: BlockPos): IBlockState {
        val te = worldIn.getTileEntity(pos)
        return if (te is KuarryTileEntity) {
            // If there tile entity already exists, return the upgrade level from it
            state.withProperty(LEVEL, te.upgradeLevel)
        } else {
            state
        }
    }

    // region IDismantleable implementation

    @Optional.Method(modid = "cofhcore")
    override fun canDismantle(world: World?, pos: BlockPos?, state: IBlockState?, player: EntityPlayer?) = true

    @Optional.Method(modid = "cofhcore")
    override fun dismantleBlock(world: World, pos: BlockPos, state: IBlockState, player: EntityPlayer, returnDrops: Boolean): ArrayList<ItemStack> {
        val res = ItemStack.EMPTY
        val te = world.getTileEntity(pos)
        if (te != null) {
            harvestBlock(world, player, pos, state, te, res)
        }

        return arrayListOf(res)
    }

    // endregion
}
