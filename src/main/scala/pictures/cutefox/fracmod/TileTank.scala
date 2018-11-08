package pictures.cutefox.fracmod

import net.minecraft.nbt.NBTTagCompound

import collection.JavaConverters._
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.fluids.{Fluid, FluidRegistry, FluidStack}

import scala.util.Random


object TileTank {
  def randomFluid: Fluid= {
    val fluids = FluidRegistry.getRegisteredFluids.asScala
    val fluidIndex = Random.nextInt(fluids.size)
    fluids.values.slice(fluidIndex, fluidIndex + 1).head
  }
}

class TileTank extends TileEntity {
  var myFluidStack: FluidStack = new FluidStack(TileTank.randomFluid, 1000)
  println(s"-!- today my fluid is ${myFluidStack.getLocalizedName}")

  override def hasFastRenderer: Boolean = true

  override def writeToNBT(compound: NBTTagCompound): NBTTagCompound = {
    val ret = super.writeToNBT(compound)
    ret.setString("fluid", myFluidStack.getUnlocalizedName)
    ret
  }

  override def readFromNBT(compound: NBTTagCompound): Unit = {
    super.readFromNBT(compound)
    val fluid = Option(compound.getString("fluid"))
      .flatMap((name: String) => Option(FluidRegistry.getFluid(name)))
      .getOrElse(TileTank.randomFluid)
    myFluidStack = new FluidStack(fluid, 1000)
    println(s"-!- today, because nbt ($compound), my fluid is ${myFluidStack.getLocalizedName}")
  }
}

