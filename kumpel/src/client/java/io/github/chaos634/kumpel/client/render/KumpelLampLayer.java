package io.github.chaos634.kumpel.client.render;

import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

import io.github.chaos634.kumpel.Kumpel;

/** The lamp on the Kumpel's helmet glows, even in the dark. */
public class KumpelLampLayer extends EyesLayer<KumpelRenderState, KumpelModel> {
	private static final RenderType LAMP = RenderTypes.eyes(Kumpel.id("textures/entity/kumpel/lamp_glow.png"));

	public KumpelLampLayer(RenderLayerParent<KumpelRenderState, KumpelModel> parent) {
		super(parent);
	}

	@Override
	public RenderType renderType() {
		return LAMP;
	}
}
