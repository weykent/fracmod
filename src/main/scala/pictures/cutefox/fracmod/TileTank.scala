package pictures.cutefox.fracmod

import net.minecraft.entity.Entity
import net.minecraft.nbt.{NBTTagCompound, NBTTagList}
import pictures.cutefox.fracmod.serialization.NBT._

import collection.JavaConverters._
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.fluids.{Fluid, FluidRegistry, FluidStack}
import pictures.cutefox.fracmod.serialization.{NetworkRequest, TankContents, UpdateTank}

import scala.util.Random


object TileTank {
  def randomFluid(): Fluid = {
    val fluids = FluidRegistry.getRegisteredFluids.asScala
    val fluidIndex = Random.nextInt(fluids.size)
    fluids.values.slice(fluidIndex, fluidIndex + 1).head
  }
}

class TileTank extends TileEntity {
  var fluids: List[FluidStack] = List()

  def setSomeFluid(): Unit = {
    fluids = List(new FluidStack(TileTank.randomFluid(), 1000))
    sendFluids()
  }

  override def hasFastRenderer: Boolean = true

  override def writeToNBT(compound: NBTTagCompound): NBTTagCompound =
    super.writeToNBT(compound)
      .withEncoded("fluids", fluids)

  override def readFromNBT(compound: NBTTagCompound): Unit = {
    super.readFromNBT(compound)
    fluids = compound.loadDecoded[List[FluidStack]]("fluids").getOrElse(List())
    sendFluids()
  }

  private def sendFluids(): Unit = {
    val req = NetworkRequest(NetworkRequest.Kind.UpdateTank(
      UpdateTank(Some(
        TankContents(Some(serialization.NBT.embed(fluids)))))))
    PbMessage.sendToClient(req)

    val displayFluids = fluids.map(fs => fs.getLocalizedName -> fs.amount).toMap
    println(s"-!- today, my fluids are $displayFluids")
  }
}

