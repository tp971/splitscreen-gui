package de.tp971.splitscreen.utils;

public class FileDialogFilter {
	String name;
	String[] exts;
	boolean def;

	public FileDialogFilter(String name, String... exts) {
		this.name = name;
		this.exts = exts;
		this.def = false;
	}

	public FileDialogFilter(String name, boolean def, String... exts) {
		this.name = name;
		this.exts = exts;
		this.def = def;
	}
}