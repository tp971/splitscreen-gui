package de.tp971.splitscreen.utils;

import java.awt.Component;
import java.io.File;
import java.nio.file.Path;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

public class FileDialogHelper {
	
	private Path lastPath = null;
	
	public Path showOpenDialog(Component parent, FileDialogFilter... filters) {
		var dialog = createDialog(parent, filters);
		dialog.setDialogTitle("Open File");
		if(dialog.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION)
			return null;
		Path res = dialog.getSelectedFile().toPath();
		lastPath = res.getParent();
		return res;
	}
	
	public Path showSaveDialog(Component parent, FileDialogFilter... filters) {
		return showSaveDialog(parent, null, filters);
	}

	public Path showSaveDialog(Component parent, Path path, FileDialogFilter... filters) {
		var dialog = createDialog(parent, filters);
		dialog.setDialogTitle("Save File");
		if(path != null)
			dialog.setSelectedFile(path.toFile());
		if(dialog.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION)
			return null;
		Path res = dialog.getSelectedFile().toPath();
		lastPath = res.getParent();
		return res;
	}
	
	private JFileChooser createDialog(Component parent, FileDialogFilter... filters) {
		var dialog = new BetterFileChooser();
		if(lastPath != null)
			dialog.setCurrentDirectory(lastPath.toFile());

		dialog.resetChoosableFileFilters();
		for(FileDialogFilter f : filters) {
			var df = new FileNameExtensionFilter(f.name, f.exts);
			if(f.def)
				dialog.setFileFilter(df);
			else
				dialog.addChoosableFileFilter(df);
		}
		return dialog;
	}
	
	private static class BetterFileChooser extends JFileChooser {
		
		private static final long serialVersionUID = 1L;

	    @Override
	    public void approveSelection() {
	        File f = getSelectedFile();

	        if(getDialogType() == JFileChooser.OPEN_DIALOG && !f.exists()) {
	        	JOptionPane.showMessageDialog(this, "File not found.",
	        		getDialogTitle(), JOptionPane.ERROR_MESSAGE);
	            return;
	        }

	        if(getDialogType() == JFileChooser.SAVE_DIALOG && f.exists()) {
	            int answer = JOptionPane.showConfirmDialog(this,
	            	"File already exists. Do you want to overwrite it?",
	                getDialogTitle(), JOptionPane.YES_NO_OPTION);
	            if(answer != JOptionPane.YES_OPTION)
	                return;
	        }

	        super.approveSelection();
	    }

	}

}
