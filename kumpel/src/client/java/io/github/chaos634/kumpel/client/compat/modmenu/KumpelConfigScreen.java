package io.github.chaos634.kumpel.client.compat.modmenu;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import io.github.chaos634.kumpel.config.KumpelConfig;
import io.github.chaos634.kumpel.config.KumpelSettings;

/**
 * Builds the settings screen with Cloth Config. Every behaviour option of the config gets an entry, found by reflection,
 * so new options show up without touching this class. Levels, ores and food stay in the config file.
 */
final class KumpelConfigScreen {
	private KumpelConfigScreen() {
	}

	static Screen create(Screen parent) {
		KumpelConfig config = KumpelConfig.load();
		KumpelConfig.Behaviour behaviour = config.behaviour;
		KumpelConfig.Behaviour defaults = new KumpelConfig.Behaviour();

		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.translatable("config.kumpel.title"));
		ConfigEntryBuilder entries = builder.entryBuilder();
		ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.kumpel.behaviour"));
		category.addEntry(entries.startTextDescription(Component.translatable("config.kumpel.file_hint")).build());

		for (Field field : KumpelConfig.Behaviour.class.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers()) || !Modifier.isPublic(field.getModifiers())) {
				continue;
			}

			Component label = Component.translatable(KumpelConfig.optionKey(field.getName()));
			Class<?> type = field.getType();
			if (type == boolean.class) {
				category.addEntry(entries.startBooleanToggle(label, (Boolean) get(field, behaviour))
						.setDefaultValue((Boolean) get(field, defaults))
						.setSaveConsumer(value -> set(field, behaviour, value))
						.build());
			} else if (type == int.class) {
				category.addEntry(entries.startIntField(label, (Integer) get(field, behaviour))
						.setDefaultValue((Integer) get(field, defaults))
						.setSaveConsumer(value -> set(field, behaviour, value))
						.build());
			} else if (type == float.class) {
				category.addEntry(entries.startFloatField(label, (Float) get(field, behaviour))
						.setDefaultValue((Float) get(field, defaults))
						.setSaveConsumer(value -> set(field, behaviour, value))
						.build());
			} else if (type == double.class) {
				category.addEntry(entries.startDoubleField(label, (Double) get(field, behaviour))
						.setDefaultValue((Double) get(field, defaults))
						.setSaveConsumer(value -> set(field, behaviour, value))
						.build());
			} else if (type == String.class) {
				category.addEntry(entries.startStrField(label, (String) get(field, behaviour))
						.setDefaultValue((String) get(field, defaults))
						.setSaveConsumer(value -> set(field, behaviour, value))
						.build());
			} else if (type == List.class) {
				category.addEntry(entries.startStrList(label, stringList(get(field, behaviour)))
						.setDefaultValue(stringList(get(field, defaults)))
						.setSaveConsumer(value -> set(field, behaviour, new ArrayList<>(value)))
						.build());
			}
		}

		builder.setSavingRunnable(() -> {
			config.save();
			KumpelSettings.reload();
		});
		return builder.build();
	}

	private static List<String> stringList(Object value) {
		List<String> list = new ArrayList<>();
		if (value instanceof List<?> values) {
			values.forEach(entry -> list.add(String.valueOf(entry)));
		}
		return list;
	}

	private static Object get(Field field, Object owner) {
		try {
			return field.get(owner);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Cannot read config option " + field.getName(), e);
		}
	}

	private static void set(Field field, Object owner, Object value) {
		try {
			field.set(owner, value);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Cannot write config option " + field.getName(), e);
		}
	}
}
