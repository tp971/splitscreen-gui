package de.tp971.splitscreen;

public class Encoder {
	
	private String name;
	private String title;
	private boolean def;
	
	public Encoder(String name, String title, boolean def) {
		this.name = name;
		this.title = title;
		this.def = def;
	}
	
	public String getName() {
		return name;
	}
	
	public String getTitle() {
		return title;
	}
	
	public boolean isDefault() {
		return def;
	}

	@Override
	public String toString() {
		return name + " - " + title;
	}

}
