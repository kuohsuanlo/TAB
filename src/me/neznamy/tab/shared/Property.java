package me.neznamy.tab.shared;

import java.util.ArrayList;
import java.util.List;

import me.neznamy.tab.shared.placeholders.Constant;
import me.neznamy.tab.shared.placeholders.Placeholder;
import me.neznamy.tab.shared.placeholders.Placeholders;

public class Property {

	private ITabPlayer owner;
	private String rawValue;
	private String temporaryValue;
	public String lastReplacedValue;

	private List<Placeholder> placeholders = new ArrayList<Placeholder>();
	private boolean hasRelationalPlaceholders;
	private long lastUpdate;
	private boolean Static;

	public Property(ITabPlayer owner, String rawValue) {
		if (rawValue == null) rawValue = "";
		this.owner = owner;
		this.rawValue = analyze(rawValue);
	}
	private String analyze(String value) {
		for (Constant c : Placeholders.constants) {
			if (value.contains(c.getIdentifier())) {
				value = value.replace(c.getIdentifier(), c.get());
			}
		}
		placeholders = detectPlaceholders(value, owner != null);
		hasRelationalPlaceholders = value.contains("%rel_");
		if (placeholders.isEmpty() && !hasRelationalPlaceholders) {
			//no placeholders, this is a static string
			//performing final changes before saving it
			for (String removed : Configs.removeStrings) {
				if (value.contains(removed)) value = value.replace(removed, "");
			}
			lastReplacedValue = Placeholders.color(value);
			Static = true;
		} else {
			lastReplacedValue = null;
			Static = false;
		}
		return value;
	}
	public void setTemporaryValue(String temporaryValue) {
		this.temporaryValue = temporaryValue;
		if (temporaryValue != null) {
			temporaryValue = analyze(temporaryValue);
		} else {
			rawValue = analyze(rawValue);
		}
	}
	public void changeRawValue(String newValue) {
		if (rawValue.equals(newValue)) return;
		rawValue = newValue;
		if (temporaryValue == null) {
			rawValue = analyze(rawValue);
		}
	}
	public String get() {
		if (lastReplacedValue == null) isUpdateNeeded();
		return lastReplacedValue;
	}
	public String getCurrentRawValue() {
		return temporaryValue != null ? temporaryValue : rawValue;
	}
	public String getTemporaryValue() {
		return temporaryValue;
	}
	public String getOriginalRawValue() {
		return rawValue;
	}
	public boolean isUpdateNeeded() {
		if (Static) return false;
		String string = getCurrentRawValue();

		//placeholders
		for (Placeholder pl : placeholders) {
			string = pl.set(string, owner);
		}

		//removing strings
		for (String removed : Configs.removeStrings) {
			if (string.contains(removed)) string = string.replace(removed, "");
		}

		//colors
		string = Placeholders.color(string);

		if (lastReplacedValue == null || !string.equals(lastReplacedValue) || (hasRelationalPlaceholders() && System.currentTimeMillis()-lastUpdate > Configs.SECRET_relational_placeholders_refresh *1000)) {
			lastReplacedValue = string;
			lastUpdate = System.currentTimeMillis();
			return true;
		} else {
			return false;
		}
	}
	public boolean hasRelationalPlaceholders() {
		return hasRelationalPlaceholders && PluginHooks.placeholderAPI;
	}
	public boolean isStatic() {
		return Static;
	}
	public static List<Placeholder> detectPlaceholders(String rawValue, boolean playerPlaceholders) {
		if (rawValue == null || (!rawValue.contains("%") && !rawValue.contains("{"))) return new ArrayList<Placeholder>();
		List<Placeholder> placeholdersTotal = new ArrayList<Placeholder>();
		for (Placeholder placeholder : playerPlaceholders ? Placeholders.getAll() : Placeholders.serverPlaceholders) {
			if (rawValue.contains(placeholder.getIdentifier())) {
				placeholdersTotal.add(placeholder);
				for (String child : placeholder.getChilds()) {
					for (Placeholder p : detectPlaceholders(child, playerPlaceholders)) {
						if (!placeholdersTotal.contains(p)) placeholdersTotal.add(p);
					}
				}
			}
		}
		return placeholdersTotal;
	}
	public static List<String> detectPlaceholderAPIPlaceholders(String s){
		List<String> list = new ArrayList<String>();
		if (s == null) return list;
		while (s.contains("%")) {
			s = s.substring(s.indexOf("%")+1, s.length());
			if (s.contains("%")) {
				String placeholder = s.substring(0, s.indexOf("%"));
				s = s.substring(s.indexOf("%")+1, s.length());
				if (!placeholder.startsWith("rel_")) list.add("%" + placeholder + "%");
			}
		}
		return list;
	}
}