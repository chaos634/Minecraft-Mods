package io.github.chaos634.taubenschlag.client;

import net.minecraft.client.renderer.entity.EntityRenderers;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

import io.github.chaos634.taubenschlag.client.render.BrieftaubeModel;
import io.github.chaos634.taubenschlag.client.render.BrieftaubeRenderer;
import io.github.chaos634.taubenschlag.registry.TaubenschlagEntities;

public class TaubenschlagClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(BrieftaubeModel.LAYER, BrieftaubeModel::createBodyLayer);
		EntityRenderers.register(TaubenschlagEntities.BRIEFTAUBE, BrieftaubeRenderer::new);
	}
}
