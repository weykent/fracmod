package pictures.cutefox.fracmod

import buildcraft.lib.client.render.fluid.{FluidRenderer, FluidSpriteType}
import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.util.math.Vec3d
import net.minecraftforge.client.model.animation.FastTESR


class RenderFractionatingColumn extends FastTESR[TileFractionatingColumn] {
  override def renderTileEntityFast(tile: TileFractionatingColumn, x: Double, y: Double, z: Double,
                                    partialTicks: Float, destroyStage: Int, partial: Float,
                                    buffer: BufferBuilder): Unit = {
    if (tile.fluids.isEmpty) {
      return
    }
    val fluid = tile.fluids.head
    val lightValue = fluid.getFluid.getLuminosity(fluid)
    val lightCombined = tile.getWorld.getCombinedLight(tile.getPos, lightValue)

    buffer.setTranslation(x, y, z)
    FluidRenderer.vertex.lighti(lightCombined)
    FluidRenderer.renderFluid(
      FluidSpriteType.STILL, fluid, 1.0, 2.0,
      new Vec3d(0.13, 0.01, 0.13),
      new Vec3d(0.86, 0.99, 0.86),
      buffer, null)
    // XXX: Is this still necessary?
    buffer.setTranslation(0, 0, 0)
  }
}

