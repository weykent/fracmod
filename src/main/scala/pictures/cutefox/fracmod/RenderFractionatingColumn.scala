package pictures.cutefox.fracmod

import buildcraft.lib.client.render.fluid.{FluidRenderer, FluidSpriteType}
import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.Vec3d
import net.minecraftforge.client.model.animation.FastTESR


class RenderFractionatingColumn extends FastTESR[TileFractionatingColumn] {
  private val EPSILON = 1e-4
  private val MIN = new Vec3d(0.13, EPSILON, 0.13)
  private val MAX = new Vec3d(0.86, 1 - EPSILON, 0.86)
  private val Y_RANGE = MAX.y - MIN.y

  override def renderTileEntityFast(tile: TileFractionatingColumn, x: Double, y: Double, z: Double,
                                    partialTicks: Float, destroyStage: Int, partial: Float,
                                    buffer: BufferBuilder): Unit = {
    if (tile.fluids.isEmpty) {
      return
    }
    val lightValue = tile.fluids
      .map { f => f.fs.getFluid.getLuminosity(f.fs) }
      .max
    val lightCombined = tile.getWorld.getCombinedLight(tile.getPos, lightValue)
    val sideRender = Array(true, true, true, true, true, true)

    buffer.setTranslation(x, y, z)
    FluidRenderer.vertex.lighti(lightCombined)
    for (f <- tile.fluidsToRender(partialTicks)) {
      sideRender(EnumFacing.DOWN.ordinal) = f.showBottom
      sideRender(EnumFacing.UP.ordinal) = f.showTop
      val xzJitter = EPSILON * f.jitter
      FluidRenderer.renderFluid(
        FluidSpriteType.STILL, f.fluid, 1, 1,
        new Vec3d(MIN.x - xzJitter, MIN.y + Y_RANGE * f.ratioBottom - (if (f.showBottom) 0 else EPSILON), MIN.z - xzJitter),
        new Vec3d(MAX.x + xzJitter, MIN.y + Y_RANGE * f.ratioTop + (if (f.showTop) 0 else EPSILON), MAX.z + xzJitter),
        buffer, sideRender)
    }
    // XXX: Is this still necessary?
    buffer.setTranslation(0, 0, 0)
  }
}

