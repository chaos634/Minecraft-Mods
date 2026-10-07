package io.github.chaos634.taubenschlag.client.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

import io.github.chaos634.taubenschlag.entity.BrieftaubeVariant;

public class BrieftaubeRenderState extends LivingEntityRenderState {
	public BrieftaubeVariant variant = BrieftaubeVariant.BLAU;
	public boolean flying;
	public boolean sitting;
	/** How far the wings are raised, from 0 (folded) to about 2. */
	public float flapAngle;
}
