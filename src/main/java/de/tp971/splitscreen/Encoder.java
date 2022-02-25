package de.tp971.splitscreen;

public class Encoder {
	
	private String name;
	private String title;
	
	public Encoder(String name, String title) {
		this.name = name;
		this.title = title;
	}
	
	public String getName() {
		return name;
	}
	
	public String getTitle() {
		return title;
	}
	
	@Override
	public String toString() {
		return title;
	}

}
