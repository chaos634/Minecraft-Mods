package io.github.chaos634.kumpel.client.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

import io.github.chaos634.kumpel.entity.KumpelTier;

public class KumpelRenderState extends LivingEntityRenderState {
	public KumpelTier tier = KumpelTier.COPPER;
	public boolean sitting;
	public boolean pointing;
}
