package pictures.cutefox.fracmod

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import net.minecraftforge.fluids.{Fluid, FluidRegistry, FluidStack}
import pictures.cutefox.fracmod.serialization.NBT._
import pictures.cutefox.fracmod.serialization.{NetPacket, TankContents, UpdateTank}

import scala.collection.JavaConverters._
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
    println(s"-!- generated random fluid")
    fluids = List(new FluidStack(TileTank.randomFluid(), 1000))
    sendFluids()
    markDirty()
  }

  override def hasFastRenderer: Boolean = true

  // XXX: Obviously needs some deduplication across these four methods, but I'm
  // still unsure of the relationships between them.
  override def getUpdateTag: NBTTagCompound = {
    val ret = super.getUpdateTag
      .withEncoded("fluids", fluids)
    println(s"-!- sending update tag NBT $ret")
    ret
  }

  override def writeToNBT(compound: NBTTagCompound): NBTTagCompound = {
    val ret = super.writeToNBT(compound)
      .withEncoded("fluids", fluids)
    ret.merge(getUpdateTag)
    println(s"-!- writing NBT $ret")
    ret
  }

  override def handleUpdateTag(tag: NBTTagCompound): Unit = {
    println(s"-!- got update tag NBT $tag")
    super.handleUpdateTag(tag)
    fluids = tag.loadDecoded[List[FluidStack]]("fluids").getOrElse(List())
  }

  override def readFromNBT(compound: NBTTagCompound): Unit = {
    println(s"-!- reading NBT $compound")
    super.readFromNBT(compound)
    fluids = compound.loadDecoded[List[FluidStack]]("fluids").getOrElse(List())
  }

  override def setWorldCreate(worldIn: World): Unit =
    world = worldIn

  private def sendFluids(): Unit = {
    // XXX: Make a typeclass for these conversions too? Vec3i/EmbeddedNBT are
    // pretty trivial so far but maybe the conversions will get more
    // complicated with other types.
    val position = serialization.Vec3i(pos.getX, pos.getY, pos.getZ)
    val req = NetPacket(NetPacket.Kind.UpdateTank(
      UpdateTank(
        position = Some(position),
        newContents = Some(TankContents(Some(serialization.NBT.embed(fluids)))))))
    // XXX: Figure out how to send updates to a smaller subset of clients.
    NetMessage.sendToClient(req)

    val displayFluids = fluids.map(fs => fs.getLocalizedName -> fs.amount).toMap
    println(s"-!- today, my fluids are $displayFluids")
  }
}

