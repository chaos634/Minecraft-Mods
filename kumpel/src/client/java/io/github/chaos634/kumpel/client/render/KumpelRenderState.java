package io.github.chaos634.kumpel.client.render;

import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.resources.Identifier;

public class KumpelRenderState extends ArmedEntityRenderState {
	public Identifier texture;
	public boolean sitting;
	public boolean pointing;
	public boolean mining;
	public boolean dancing;
	public boolean barbaraDay;
	public boolean canary;
	public boolean forge;
	/** Dust stage, 0 (clean) to 3. */
	public int dust;
}
