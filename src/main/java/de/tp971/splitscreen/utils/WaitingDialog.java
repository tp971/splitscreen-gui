package de.tp971.splitscreen.utils;

import javax.swing.JDialog;
import net.miginfocom.swing.MigLayout;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;

public abstract class WaitingDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private JLabel lblMessage;
	private JProgressBar progressBar;
	private JPanel panel;
	private JButton btnCancel;

	public WaitingDialog(Window parent, String title, String message, boolean cancelable) {
		super(parent);

		initialize();
		setTitle(title);
		setLocationRelativeTo(parent);
		lblMessage.setText(message);
		if(!cancelable)
			btnCancel.setEnabled(false);

		btnCancel.addActionListener(ev -> {
			cancel();
			btnCancel.setEnabled(false);
		});
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent e) {
				new Thread(() -> {
					try {
						run();
					} catch(Exception ex) {
						ex.printStackTrace();
					}
					dispose();
				}).start();
			}
		});
	}
	
	private void initialize() {
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		setResizable(false);
		setModalityType(ModalityType.DOCUMENT_MODAL);
		getContentPane().setLayout(new MigLayout("", "[360px,grow]", "[][][]"));
		
		lblMessage = new JLabel("New label");
		getContentPane().add(lblMessage, "cell 0 0,alignx center");
		
		progressBar = new JProgressBar();
		progressBar.setStringPainted(true);
		progressBar.setIndeterminate(true);
		progressBar.setString("");
		getContentPane().add(progressBar, "cell 0 1,growx");
		
		panel = new JPanel();
		FlowLayout flowLayout = (FlowLayout) panel.getLayout();
		flowLayout.setAlignment(FlowLayout.RIGHT);
		getContentPane().add(panel, "cell 0 2,grow");
		
		btnCancel = new JButton("Cancel");
		panel.add(btnCancel);

		pack();
	}

	protected void setProgressIndeterminate() {
		progressBar.setIndeterminate(true);
	}

	protected void setProgressDeterminate(int min, int max) {
		progressBar.setMinimum(min);
		progressBar.setMaximum(max);
		progressBar.setIndeterminate(false);
	}

	protected void setProgress(int value) {
		progressBar.setValue(value);
	}
	
	protected void setProgressText(String text) {
		progressBar.setString(text == null ? "" : text);
	}
	
	public abstract void run() throws Exception;

	public void cancel() {
	}
	
}
