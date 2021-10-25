package de.tp971.splitscreen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SplitScreenInput {
	
	public Path videoPath;
	public List<Long> splits;

	public Path splitFilePath;
	public boolean dirty;
	
	public SplitScreenInput(Path path) {
		this.videoPath = path;
		this.splits = new ArrayList<>();
		this.splitFilePath = null;
		this.dirty = true;
	}
	
	@Override
	public String toString() {
		return videoPath.getFileName().toString();
	}
	
	public boolean isComplete() {
		return splits.stream().allMatch(t -> t != null);
	}
	
	public void loadFromFile(Path path) throws Exception {
		var splits2 = new ArrayList<Long>();
		for(String line : Files.readAllLines(path)) {
			String[] args = line.split(" ");
			if(args[0].equals("split"))
				splits2.add(parseTime(args[1]));
		}
		this.splits = splits2;
		this.splitFilePath = path;
		this.dirty = false;
	}

	public void saveToFile(Path path) throws Exception {
		var lines = new ArrayList<String>();
		for(Long time : splits) {
			if(time == null)
				throw new RuntimeException("Cannot save incomplete splits");
			lines.add("split " + formatTime(time));
		}
		Files.write(path, lines);
		this.splitFilePath = path;
		this.dirty = false;
	}
	
	public static Long parseTime(String time_str) throws Exception {
		try {
			long h = 0;
			long m = 0;
			double s = 0.0;

			String[] split = time_str.split(":");
			if(split.length == 1)
				s = Double.parseDouble(split[0]);
			else if(split.length == 2) {
				m = Long.parseLong(split[0]);
				s = Double.parseDouble(split[1]);
			} else if(split.length == 3) {
				h = Long.parseLong(split[0]);
				m = Long.parseLong(split[1]);
				s = Double.parseDouble(split[2]);
			} else
				throw new RuntimeException("invalid split file");
			if(h < 0 || h > 60 || m < 0 || m > 60 || s < 0 || s > 60)
				throw new RuntimeException("invalid split file");

			return (h * 60 + m) * 60 * 1000000000 + (long)(s * 1.0e9);
		} catch(Exception ex) {
			throw new RuntimeException("invalid split file", ex);
		}
	}

	public static String formatTime(Long time) {
		if(time == null)
			return "--:--:--.---";
		long ms_total = time / 1000000;
		long ms = ms_total % 1000;
		long s_total = ms_total / 1000;
		long s = s_total % 60;
		long m_total = s_total / 60;
		long m = m_total % 60;
		long h = m_total / 60;
		return String.format("%02d:%02d:%02d.%03d", h, m, s, ms);
	}

}
