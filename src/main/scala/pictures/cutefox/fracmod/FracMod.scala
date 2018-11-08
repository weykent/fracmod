package pictures.cutefox.fracmod

import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.item.ItemBlock
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.registry.ForgeRegistries


@Mod(modid = FracMod.MODID, version = FracMod.VERSION, modLanguage = "scala")
object FracMod {
  final val MODID = "fracmod"
  final val VERSION = "0.1"

  @EventHandler
  def preInit(event: FMLPreInitializationEvent): Unit = {
    val tank = new BlockTank()
      .setUnlocalizedName("tank")
      .setRegistryName("tank")
    ForgeRegistries.BLOCKS.register(tank)

    val tankItem = new ItemBlock(tank)
      .setRegistryName(tank.getRegistryName)
    ForgeRegistries.ITEMS.register(tankItem)

    //GameRegistry.registerTileEntity(classOf[TileTank], tank.getRegistryName)
    ModelLoader.setCustomModelResourceLocation(
      tankItem, 0, new ModelResourceLocation(tankItem.getRegistryName, "inventory"))
  }
}
