package de.tp971.splitscreen;

import java.awt.Window;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import javax.swing.JButton;
import net.miginfocom.swing.MigLayout;
import javax.swing.JLabel;
import java.awt.Font;

public class AboutDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	private JPanel panel;
	private JPanel panel_1;
	private JButton btnClose;
	private JLabel lblNewLabel;
	private JLabel lblNewLabel_1;
	private JLabel lblNewLabel_2;
	private JButton btnVisitGitHub;
	
	public AboutDialog(Window parent) {
		super(parent);
		
		initialize();
		setLocationRelativeTo(parent);
		
		btnVisitGitHub.addActionListener(ev -> {
			try {
				Desktop.getDesktop().browse(new URI("https://github.com/tp971/splitscreen-gui"));
			} catch (IOException | URISyntaxException ex) {
				ex.printStackTrace();
			}
		});
		
		btnClose.addActionListener(ev -> {
			dispose();
		});
	}
	
	private void initialize() {
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setResizable(false);
		setTitle("About SplitScreen");
		setModalityType(ModalityType.DOCUMENT_MODAL);

		panel = new JPanel();
		getContentPane().add(panel, BorderLayout.CENTER);
		panel.setLayout(new MigLayout("", "15[grow]15", "15[][][]15"));
		
		lblNewLabel = new JLabel(Info.NAME);
		lblNewLabel.setFont(new Font("Dialog", Font.BOLD, 24));
		panel.add(lblNewLabel, "cell 0 0,alignx center");
		
		lblNewLabel_1 = new JLabel("Version " + Info.VERSION);
		lblNewLabel_1.setFont(new Font("Dialog", Font.BOLD, 16));
		panel.add(lblNewLabel_1, "cell 0 1,alignx center");
		
		lblNewLabel_2 = new JLabel(Info.COPYRIGHT);
		panel.add(lblNewLabel_2, "cell 0 2,alignx center");
		
		panel_1 = new JPanel();
		FlowLayout flowLayout = (FlowLayout) panel_1.getLayout();
		flowLayout.setAlignment(FlowLayout.RIGHT);
		getContentPane().add(panel_1, BorderLayout.SOUTH);
		
		btnVisitGitHub = new JButton("Visit on GitHub");
		panel_1.add(btnVisitGitHub);
		
		btnClose = new JButton("Close");
		panel_1.add(btnClose);
		
		pack();
	}

}
