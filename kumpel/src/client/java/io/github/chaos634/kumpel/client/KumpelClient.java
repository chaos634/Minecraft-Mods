package io.github.chaos634.kumpel.client;

import net.minecraft.client.renderer.entity.EntityRenderers;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

import io.github.chaos634.kumpel.client.render.KumpelModel;
import io.github.chaos634.kumpel.client.render.KumpelRenderer;
import io.github.chaos634.kumpel.registry.ModEntities;

public class KumpelClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(KumpelModel.LAYER, KumpelModel::createBodyLayer);
		EntityRenderers.register(ModEntities.KUMPEL, KumpelRenderer::new);
	}
}
