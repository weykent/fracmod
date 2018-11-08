package pictures.cutefox.fracmod

import net.minecraft.init.Blocks
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.FMLInitializationEvent


@Mod(modid = FracMod.MODID, version = FracMod.VERSION, modLanguage = "scala")
object FracMod {
  final val MODID = "fracmod"
  final val VERSION = "0.1"

  @EventHandler
  def init(event: FMLInitializationEvent): Unit = {
    println(s"DIRT BLOCK >> ${Blocks.DIRT.getUnlocalizedName}")
  }
}
