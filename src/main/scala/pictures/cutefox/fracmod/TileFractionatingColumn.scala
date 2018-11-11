package pictures.cutefox.fracmod

import buildcraft.energy.BCEnergyFluids
import buildcraft.lib.fluid.BCFluid
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ITickable
import net.minecraft.world.World
import net.minecraftforge.fluids.{Fluid, FluidRegistry, FluidStack}
import pictures.cutefox.fracmod.TileFractionatingColumn.FractionatingFluidStack
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

  case class FractionatingFluidStack(fs: FluidStack, var moving: Boolean = false) {
    def beMoving: FractionatingFluidStack = {
      moving = true
      this
    }
    def stopMoving: FractionatingFluidStack = {
      moving = false
      this
    }
  }
}

class TileFractionatingColumn extends TileEntity with ITickable {

  var fluids: List[FractionatingFluidStack] = List()
  val CAPACITY = 10000  // mB

  def setSomeFluid(): Unit = {
    val someFluids = (1 to Random.nextInt(5) + 2)
      .map { _ => TileFractionatingColumn.randomFluid() }
    val chooseFluid = () => someFluids(Random.nextInt(someFluids.size))

    fluids = (1 to Random.nextInt(25) + 5)
      .map { _ => FractionatingFluidStack(new FluidStack(chooseFluid(), Random.nextInt(500) + 150)) }
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
    fluids = tag.loadDecoded[List[FractionatingFluidStack]]("fluids").getOrElse(List())
  }

  override def readFromNBT(compound: NBTTagCompound): Unit = {
    super.readFromNBT(compound)
    fluids = compound.loadDecoded[List[FractionatingFluidStack]]("fluids").getOrElse(List())
  }

  override def setWorldCreate(worldIn: World): Unit =
    world = worldIn

  val TICKS_PER_UPDATE = 20
  var nextTick: Long = 0
  var now: Long = 0
  override def update(): Unit = {
    now = world.getTotalWorldTime
    if (now < nextTick) {
      return
    }
    nextTick = now + TICKS_PER_UPDATE

    var changed = false
    fluids = fluids.iterator
      .zipAll(fluids.iterator.drop(1).map(Some(_)), null, None)
      .flatMap {
        case (curStack, _) if curStack.fs.amount <= 0 => Array[FractionatingFluidStack]()
        case (curStack, Some(nextStack)) => {
          val cur = curStack.fs.getFluid
          val next = nextStack.fs.getFluid
          if (cur.getName == next.getName) {
            changed = true
            nextStack.fs.amount += curStack.fs.amount
            Array[FractionatingFluidStack]()
          } else if (next.getDensity > cur.getDensity) {
            changed = true
            val movingDown = nextStack.fs.amount min 100
            nextStack.fs.amount -= movingDown
            Array(
              FractionatingFluidStack(new FluidStack(next, movingDown), moving = true),
              curStack.stopMoving)
          } else {
            Array(curStack.stopMoving)
          }
        }
        case (curStack, None) => Array(curStack.stopMoving)
      }
      .toList

    if (changed) {
      sendFluids()
      markDirty()
    }
  }

  def getBubbleRatio(partialTicks: Float): Float = {
    val fullTicks = TICKS_PER_UPDATE - (nextTick - now)
    (fullTicks.floatValue + partialTicks) / TICKS_PER_UPDATE
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
                           showBottom: Boolean, ratioBottom: Double,
                           showTop: Boolean, ratioTop: Double)

  def fluidsToRender(partialTicks: Float = 0): Iterator[FluidToRender] = {
    val bubbleRatio = getBubbleRatio(partialTicks)
    val lastIndex = fluids.size - 1
    var currentRatio: Double = 0
    var ratioOverrides = Map[Int, Double]()
    // XXX: Reapply jitter. Disabled only because lazy.
    var currentJitter = 1
    fluids.iterator
      .zipWithIndex
      .flatMap {
        case (fluid, e) if fluid.moving && e < lastIndex => {
          val nextFluid = fluids(e + 1)
          val thisFluidRatio = ratioOverrides.getOrElse(e, fluid.fs.amount.toDouble / CAPACITY.toDouble)
          val nextFluidRatio = nextFluid.fs.amount.toDouble / CAPACITY.toDouble
          val bottomRatio = nextFluidRatio * (1 - bubbleRatio)
          val topRatio = nextFluidRatio * bubbleRatio
          // XXX: There really has to be a better way to write these sums.
          val ret = Array(
            FluidToRender(
              nextFluid.fs, jitter = 0,
              showBottom = e == 0, currentRatio,
              showTop = false, currentRatio + bottomRatio),
            FluidToRender(
              fluid.fs, jitter = 0,
              showBottom = false, currentRatio + bottomRatio,
              showTop = false, currentRatio + bottomRatio + thisFluidRatio),
          )
          currentRatio += bottomRatio + thisFluidRatio
          ratioOverrides += e + 1 -> topRatio
          ret
        }
        case (fluid, e) => {
          val bottomRatio = currentRatio
          currentRatio += ratioOverrides.getOrElse(e, fluid.fs.amount.toDouble / CAPACITY.toDouble)
          Array(FluidToRender(
            fluid.fs, jitter = 0,
            showBottom = e == 0, bottomRatio,
            showTop = e == lastIndex, currentRatio))
        }
      }
  }
}

