package pictures.cutefox.fracmod

import buildcraft.energy.BCEnergyFluids
import buildcraft.lib.fluid.BCFluid
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ITickable
import net.minecraft.world.World
import net.minecraftforge.fluids.{Fluid, FluidRegistry, FluidStack}
import pictures.cutefox.fracmod.serialization.NBT._
import pictures.cutefox.fracmod.serialization.{NetPacket, UpdateFluids}

import scala.collection.JavaConverters._
import scala.util.Random


object TileFractionatingColumn {
  def randomFluid(): Fluid = {
    val fluids = FluidRegistry.getRegisteredFluids.asScala
    val fluidIndex = Random.nextInt(fluids.size)
    fluids.values.slice(fluidIndex, fluidIndex + 1).head
  }
}

class TileFractionatingColumn extends TileEntity with ITickable {
  var fluids: List[FluidStack] = List()
  val CAPACITY = 10000  // mB

  def setSomeFluid(): Unit = {
    val someFluids = (1 to Random.nextInt(5) + 2)
      .map { _ => TileFractionatingColumn.randomFluid() }
    val chooseFluid = () => someFluids(Random.nextInt(someFluids.size))

    fluids = (1 to Random.nextInt(25) + 5)
      .map { _ => new FluidStack(chooseFluid(), Random.nextInt(500) + 150) }
      .toList
    sendFluids()
    markDirty()
  }

  override def hasFastRenderer: Boolean = true

  // XXX: Obviously needs some deduplication across these four methods, but I'm
  // still unsure of the relationships between them.
  override def getUpdateTag: NBTTagCompound =
    super.getUpdateTag
      .withEncoded("fluids", fluids)

  override def writeToNBT(compound: NBTTagCompound): NBTTagCompound =
    super.writeToNBT(compound)
      .withEncoded("fluids", fluids)

  override def handleUpdateTag(tag: NBTTagCompound): Unit = {
    super.handleUpdateTag(tag)
    fluids = tag.loadDecoded[List[FluidStack]]("fluids").getOrElse(List())
  }

  override def readFromNBT(compound: NBTTagCompound): Unit = {
    super.readFromNBT(compound)
    fluids = compound.loadDecoded[List[FluidStack]]("fluids").getOrElse(List())
  }

  override def setWorldCreate(worldIn: World): Unit =
    world = worldIn

  var nextTick: Long = 0
  override def update(): Unit = {
    val now = world.getTotalWorldTime
    if (now < nextTick) {
      return
    }
    nextTick = now + 20

    var changed = false
    fluids = fluids.iterator
      .zipAll(fluids.iterator.drop(1).map(Some(_)), null, None)
      .flatMap {
        case (curStack, _) if curStack.amount <= 0 => Array[FluidStack]()
        case (curStack, Some(nextStack)) => {
          val cur = curStack.getFluid
          val next = nextStack.getFluid
          if (cur.getName == next.getName) {
            changed = true
            nextStack.amount += curStack.amount
            Array[FluidStack]()
          } else if (next.getDensity > cur.getDensity) {
            changed = true
            val movingDown = nextStack.amount min 100
            nextStack.amount -= movingDown
            Array(new FluidStack(next, movingDown), curStack)
          } else {
            Array(curStack)
          }
        }
        case (curStack, None) => Array(curStack)
      }
      .toList

    if (changed) {
      sendFluids()
      markDirty()
    }
  }

  private def sendFluids(): Unit = {
    // XXX: Make a typeclass for these conversions too? Vec3i/EmbeddedNBT are
    // pretty trivial so far but maybe the conversions will get more
    // complicated with other types.
    val position = serialization.Vec3i(pos.getX, pos.getY, pos.getZ)
    val req = NetPacket(NetPacket.Kind.UpdateFluids(
      UpdateFluids(
        position = Some(position),
        fluids = Some(serialization.NBT.embed(fluids)))))
    // XXX: Figure out how to send updates to a smaller subset of clients.
    NetMessage.sendToClient(req)
  }

  case class FluidToRender(fluid: FluidStack, jitter: Int,
                           showBottom: Boolean, fractionBottom: Double,
                           showTop: Boolean, fractionTop: Double)

  def fluidsToRender: Iterator[FluidToRender] = {
    val lastIndex = fluids.size - 1
    var currentFraction: Double = 0
    var currentJitter = 1
    fluids.iterator
      .zipWithIndex
      .map { case (fluid, e) =>
        val fractionBottom = currentFraction
        currentFraction += fluid.amount.toDouble / CAPACITY.toDouble
        currentJitter *= -1
        FluidToRender(
          fluid, currentJitter,
          e == 0, fractionBottom,
          e == lastIndex, currentFraction)
      }
  }
}

