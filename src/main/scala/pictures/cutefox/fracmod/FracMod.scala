package pictures.cutefox.fracmod

import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.item.ItemBlock
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.{FMLInitializationEvent, FMLPreInitializationEvent}
import net.minecraftforge.fml.common.registry.{ForgeRegistries, GameRegistry}


@Mod(modid = FracMod.MODID, version = FracMod.VERSION, modLanguage = "scala")
object FracMod {
  final val MODID = "fracmod"
  final val VERSION = "0.1"

  @EventHandler
  def preInit(event: FMLPreInitializationEvent): Unit = {
    // XXX: Should probably do all of this with the other event subscriber
    // model but it's not clear how to split it all up. Where do the tile
    // entity and model resource registration go?
    val fc = new BlockFractionatingColumn()
      .setUnlocalizedName(BlockFractionatingColumn.NAME)
      .setRegistryName(BlockFractionatingColumn.NAME)
    ForgeRegistries.BLOCKS.register(fc)

    val fcItem = new ItemBlock(fc)
      .setRegistryName(fc.getRegistryName)
    ForgeRegistries.ITEMS.register(fcItem)

    GameRegistry.registerTileEntity(classOf[TileFractionatingColumn], fc.getRegistryName)
    ModelLoader.setCustomModelResourceLocation(
      fcItem, 0, new ModelResourceLocation(fcItem.getRegistryName, "inventory"))
  }

  @EventHandler
  def init(event: FMLInitializationEvent): Unit = {
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[TileFractionatingColumn], new RenderFractionatingColumn)
  }
}
