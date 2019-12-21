package org.kotobank.kuarry

import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.event.RegistryEvent.Register
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.kotobank.kuarry.item.*

@Mod.EventBusSubscriber(modid = KuarryMod.MODID)
object KuarryModItems {

    private val upgrades by lazy {
        listOf(
                Pair("x_boundaries_upgrade", KuarryXBoundariesUpgrade()),
                Pair("z_boundaries_upgrade", KuarryZBoundariesUpgrade()),
                Pair("silk_touch_upgrade", KuarrySilkTouchUpgrade()),
                Pair("custom_filter", KuarryCustomFilter()),
                Pair("speed_upgrade", KuarrySpeedUpgrade()),
                Pair("xp_collection_upgrade", KuarryXPCollectionUpgrade())
        )
    }

    private val levelUpgrades by lazy {
        listOf(
                Pair("level_2_upgrade", HardenedUpgrade()),
                Pair("level_3_upgrade", ReinforcedUpgrade())
        )
    }

    private val items by lazy {
        listOf(
                Pair("denatured_stone", ItemBlock(KuarryModBlocks.denatured_stone)),

                Pair("kuarry_casing", Item())
        )
    }

    /** Items that can easily be registered with the same code */
    internal val commonlyRegisteredItems by lazy { items + upgrades + levelUpgrades }

    // region Items that need special code for registration

    internal val luckUpgrade by lazy { KuarryLuckUpgrade() }

    internal val kuarry by lazy { KuarryItemBlock(KuarryModBlocks.kuarry) }

    // endregion

    @SubscribeEvent
    fun registerItems(event: Register<Item>) {
        // Set each upgrade's max stack size
        upgrades.forEach { (_, item) -> item.setMaxStackSize(item.stackSize) }

        commonlyRegisteredItems.forEach { (name, item) ->
            item.apply {
                setRegistryName(KuarryMod.MODID, name)
                setUnlocalizedName(name)

                event.registry.register(this)
            }
        }

        // The luck upgrade is a special case: it has subitems and the models have to be registered
        // for their names and not for the base name
        with(luckUpgrade) {
            setMaxStackSize(stackSize)
            setRegistryName(KuarryMod.MODID, "luck_upgrade")
            setUnlocalizedName("luck_upgrade")
            event.registry.register(this)
        }

        with(kuarry) {
            setRegistryName(KuarryMod.MODID, "kuarry")
            setUnlocalizedName("kuarry")

            ModelLoader.setCustomMeshDefinition(this) { stack ->
                val level = stack.tagCompound?.getInteger("upgrade_level") ?: 0

                // Return a custom model based on the level from the ItemStack
                ModelResourceLocation(this.registryName!!, "facing=north,level=${level}")
            }

            event.registry.register(this)
        }
    }

    // The fields are named by their IDs
    @ObjectHolder("${KuarryMod.MODID}:kuarry_casing")
    lateinit var kuarry_casing: Item
    @ObjectHolder("${KuarryMod.MODID}:x_boundaries_upgrade")
    lateinit var x_boundaries_upgrade: Item
    @ObjectHolder("${KuarryMod.MODID}:z_boundaries_upgrade")
    lateinit var z_boundaries_upgrade: Item
}

@Mod.EventBusSubscriber(Side.CLIENT, modid = KuarryMod.MODID)
@SideOnly(Side.CLIENT)
/** A separate class that loads item models.
 *
 * This can only run on the client, since [ModelLoader] doesn't exist on the server.
 * The event has to run after [KuarryModItems.registerItems], so its priority is lower.
 */
object ItemModelLoader {
    @SubscribeEvent(priority = EventPriority.LOW)
    @Suppress("UNUSED_PARAMETER")
    fun loadModels(event: Register<Item>) {
        KuarryModItems.commonlyRegisteredItems.forEach { (_, item) ->
            item.apply {
                ModelLoader.setCustomModelResourceLocation(this, 0, ModelResourceLocation(this.registryName!!, "inventory"))
            }
        }

        KuarryModItems.luckUpgrade.registerSubitemModels()
    }
}