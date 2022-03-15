package de.tp971.splitscreen;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;

import org.freedesktop.gstreamer.Bus;
import org.freedesktop.gstreamer.Format;
import org.freedesktop.gstreamer.State;
import org.freedesktop.gstreamer.elements.PlayBin;
import org.freedesktop.gstreamer.event.SeekFlags;
import org.freedesktop.gstreamer.event.SeekType;
import org.freedesktop.gstreamer.event.StepEvent;
import org.freedesktop.gstreamer.message.MessageType;
import org.freedesktop.gstreamer.swing.GstVideoComponent;

import de.tp971.splitscreen.utils.FileDialogFilter;
import de.tp971.splitscreen.utils.FileDialogHelper;
import de.tp971.splitscreen.utils.WaitingDialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;

import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.JSlider;
import java.awt.Font;
import javax.swing.JTextField;

public class MainWindow extends JFrame {
	
	private static final long serialVersionUID = 1L;

	private JSplitPane splitPane;

	private JPanel panel_left;
	private JPanel panel_settings;
	private JLabel lblResolution;
	private JComboBox<String> cmbResolution;
	private JLabel lblFPS;
	private JSpinner spinnerFPS;
	private JLabel lblCompare;
	private JComboBox<Compare> cmbCompare;
	private JLabel lblPause;
	private JTextField txtPause;
	private JLabel lblEncoder;
	private JComboBox<Encoder> cmbEncoder;
	private JPanel panel_inputs;
	private JScrollPane scrollPane;
	private JPanel panel_inputs_buttons;
	private JList<SplitScreenInput> list;
	private JButton btnAddInput;
	private JButton btnRemoveInput;
	private JPanel panel_controls;
	private JButton btnPlay;
	private JButton btnRender;

	private JPanel panel_editor;
	private JPanel panel_editor_title;
	private JLabel lbl_editor_title;
	private JSplitPane splitPane_1;

	private JPanel panel_player;
	private GstVideoComponent video;
	private JPanel panel_player_controls;
	private JButton btnPlayPause;
	private JButton btnPrevFrame;
	private JButton btnNextFrame;
	private JSlider slider;
	private JLabel lblPosition;

	private JPanel panel_splits;
	private JButton btnLoadSplits;
	private JButton btnSaveSplits;
	private JScrollPane scrollPane_1;
	private JList<Long> list_splits;
	private JPanel panel_splits_buttons2;
	private JButton btnRemoveSplit;
	private JButton btnInsertAbove;
	private JButton btnSetSplit;
	private JPanel panel_splits_buttons3;
	private JButton btnClearSplit;
	private JButton btnInsertBelow;
	
	private DefaultListModel<SplitScreenInput> list_inputs_model;
	private DefaultListModel<Long> list_splits_model;
	
	private FileDialogHelper fileDialogs;
	private FileDialogHelper fileDialogsOutput;
	
	private PlayBin playbin;
	private Timer timer_position;
	private double rate = 1.0;
	private boolean starting;

	private Long seekingBackwards;
	private boolean seekingBackwardsHack;
	private JButton btnAbout;

	public MainWindow(List<Encoder> encoders) {
		initialize();
		
		fileDialogs = new FileDialogHelper();
		fileDialogsOutput = new FileDialogHelper();

		cmbResolution.addItem("1920x1080");
		
		cmbCompare.addItem(null);
		cmbCompare.addItem(Compare.TimeLoss);
		cmbCompare.addItem(Compare.TimeSave);
		
		cmbCompare.setRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;
			
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				String text;
				if(value == null)
					text = "None";
				else
					text = switch((Compare)value) {
						case TimeLoss -> "Show time loss";
						case TimeSave -> "Show time save";
					};
				return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
			}
		});
		
		cmbCompare.setSelectedItem(Compare.TimeLoss);

		for(var encoder: encoders) {
			cmbEncoder.addItem(encoder);
			if(encoder.isDefault())
				cmbEncoder.setSelectedIndex(cmbEncoder.getItemCount() - 1);
		}

		list_inputs_model = new DefaultListModel<>();
		list.setModel(list_inputs_model);
		
		list_splits_model = new DefaultListModel<>();
		list_splits.setModel(list_splits_model);
		
		list.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent ev) {
				if(!ev.getValueIsAdjusting()) {
					display(list.getSelectedValue());
				}
			}
		});

		btnAddInput.addActionListener(ev -> {
			Path path = fileDialogs.showOpenDialog(this,
				new FileDialogFilter("Video File", true, "mp4", "mkv")
			);
			if(path != null)
				addInput(path);
		});
		
		btnRemoveInput.addActionListener(ev -> {
			if(list.getSelectedIndex() < 0)
				return;
			
			list_inputs_model.remove(list.getSelectedIndex());
		});
		
		btnPlay.addActionListener(ev -> {
			play();
		});

		btnRender.addActionListener(ev -> {
			render();
		});

		video.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				video.requestFocus();
			}
		});
		
		video.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(seekingBackwards == null) {
					switch(e.getKeyCode()) {
					case KeyEvent.VK_SPACE:
						playOrPause();
						break;
					case KeyEvent.VK_COMMA:
						gotoPrevFrame();
						break;
					case KeyEvent.VK_PERIOD:
						gotoNextFrame();
						break;
					case KeyEvent.VK_LEFT:
						gotoRelative(-5000);
						break;
					case KeyEvent.VK_RIGHT:
						gotoRelative(5000);
						break;
					case KeyEvent.VK_UP:
						if(e.isControlDown()) {
							if(list_splits.getSelectedIndex() > 0)
								selectSplit(list_splits.getSelectedIndex() - 1);
						} else {
							//TODO volume
						}
						break;
					case KeyEvent.VK_DOWN:
						if(e.isControlDown()) {
							if(list_splits.getSelectedIndex() + 1 < list_splits_model.getSize())
								selectSplit(list_splits.getSelectedIndex() + 1);
						} else {
							//TODO volume
						}
						break;
					case KeyEvent.VK_PAGE_UP:
						if(e.isControlDown()) {
							if(list.getSelectedIndex() > 0)
								select(list.getSelectedIndex() - 1);
						} else {
							gotoRelative(-60000);
						}
						break;
					case KeyEvent.VK_PAGE_DOWN:
						if(e.isControlDown()) {
							if(list.getSelectedIndex() + 1 < list_inputs_model.getSize())
								select(list.getSelectedIndex() + 1);
						} else {
							gotoRelative(60000);
						}
						break;
					case KeyEvent.VK_INSERT:
						if(e.isShiftDown())
							insertSplitAbove();
						else
							insertSplitBelow();
						break;
					case KeyEvent.VK_DELETE:
						deleteSplit();
						break;
					case KeyEvent.VK_ENTER:
						if(e.isShiftDown())
							enterSplit();
						else if(list_splits.getSelectedIndex() >= 0 && list_splits.getSelectedValue() == null) {
							enterSplit();
							if(list_splits.getSelectedIndex() + 1 < list_splits_model.getSize())
								if(list_splits_model.get(list_splits.getSelectedIndex() + 1) == null)
									selectSplit(list_splits.getSelectedIndex() + 1);
						} else {
							insertSplitBelow();
							enterSplit();
						}
						break;
					case KeyEvent.VK_BACK_SPACE:
						if(e.isShiftDown())
							resetSplit();
						else {
							if(list_splits.getSelectedIndex() > 0 && list_splits.getSelectedValue() == null)
								selectSplit(list_splits.getSelectedIndex() - 1);
							resetSplit();
						}
						break;
					default:
						break;
					}
				}
			}
		});
		
		playbin = new PlayBin("playbin");
		playbin.setVideoSink(video.getElement());
		
		playbin.getBus().connect((Bus.STATE_CHANGED) (obj, old, state, pending) -> {
			if(pending == State.VOID_PENDING) {
				if(starting) {
					starting = false;
					playbin.pause();
				} else if(seekingBackwards == null) {
					switch(state) {
					case PLAYING:
						displayPlaying();
						timer_position.start();
						break;
					case PAUSED:
					case READY:
						displayPaused();
						timer_position.stop();
						break;
					case NULL:
						displayStopped();
						timer_position.stop();
						break;
					default:
						break;
					}
					updatePosition();
				}
			}
		});

		playbin.getBus().connect((Bus.EOS) obj -> {
			playbin.pause();
		});
		
		playbin.getBus().connect((Bus.MESSAGE) (bus, msg) -> {
			if(msg.getType() == MessageType.STEP_DONE) {
				Long time = playbin.queryPosition(Format.TIME);
				if(seekingBackwards != null && time != null) {
					boolean eos = msg.getStructure().getBoolean("eos");
					if(!eos && Math.abs(time - seekingBackwards) < 10) {
						if(!seekingBackwardsHack) {
							playbin.sendEvent(new StepEvent(Format.BUFFERS, 1, 1.0, true, false));
							seekingBackwardsHack = true;
						}
					} else {
						if(rate > 0.0)
							playbin.seek(rate, Format.TIME,
								EnumSet.of(SeekFlags.FLUSH, SeekFlags.ACCURATE),
								SeekType.SET, time, SeekType.END, 0);
						else
							playbin.seek(rate, Format.TIME,
								EnumSet.of(SeekFlags.FLUSH, SeekFlags.ACCURATE),
								SeekType.SET, 0, SeekType.SET, time);
						seekingBackwards = null;
						seekingBackwardsHack = false;
					}
				}
			}
		});

		timer_position = new Timer(100, ev -> {
			updatePosition();
		});
		
		btnPlayPause.addActionListener(ev -> {
			video.requestFocus();
			playOrPause();
		});
		
		btnPrevFrame.addActionListener(ev -> {
			video.requestFocus();
			gotoPrevFrame();
		});
		
		btnNextFrame.addActionListener(ev -> {
			video.requestFocus();
			gotoNextFrame();
		});
		
		slider.addChangeListener(ev -> {
			if(slider.getValueIsAdjusting()) {
				long pos = (long)slider.getValue() * 1000000;
				playbin.seekSimple(Format.TIME, EnumSet.of(SeekFlags.FLUSH), pos);
				video.requestFocus();
			}
		});

		list_splits.setCellRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;
			
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				String text = formatTime((Long)value);
				return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
			}
		});
		
		list_splits.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent ev) {
				if(!ev.getValueIsAdjusting()) {
					if(list_splits.getSelectedIndex() >= 0) {
						Long time = list_splits.getSelectedValue();
						if(time != null)
							playbin.seekSimple(Format.TIME, EnumSet.of(SeekFlags.FLUSH), time);
						video.requestFocus();
					}
				}
			}
		});
		
		btnLoadSplits.addActionListener(ev -> {
			loadSplits();
		});

		btnSaveSplits.addActionListener(ev -> {
			saveSplits();
		});
		
		btnAbout.addActionListener(ev -> {
			new AboutDialog(this).setVisible(true);
		});
		
		btnInsertAbove.addActionListener(ev -> {
			insertSplitAbove();
		});
		
		btnInsertBelow.addActionListener(ev -> {
			insertSplitBelow();
		});

		btnRemoveSplit.addActionListener(ev -> {
			deleteSplit();
		});
		
		btnSetSplit.addActionListener(ev -> {
			enterSplit();
		});
		
		btnClearSplit.addActionListener(ev -> {
			resetSplit();
		});

		display(null);
	}

	private void play() {
		try {
			var cmd = getBaseCommand();
			cmd.addAll(getCommandSplits());

			System.out.println(cmd);
			
			new WaitingDialog(this, getTitle(), "Playing", true) {
				private static final long serialVersionUID = 1L;
				
				private Process p;
				private boolean aborted = false;

				@Override
				public void run() throws Exception {
					try {
						p = new ProcessBuilder(cmd)
							.redirectInput(Redirect.PIPE)
							.redirectOutput(Redirect.INHERIT)
							.redirectError(Redirect.INHERIT)
							.start();
						p.getInputStream().close();
						int res = p.waitFor();
						if(!aborted && res != 0)
							JOptionPane.showMessageDialog(this,
								"an error occured", getTitle(), JOptionPane.ERROR_MESSAGE);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}

				@Override
				public void cancel() {
					aborted = true;
					p.destroy();
				}
				
			}.setVisible(true);
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this,
				ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
		}
	}

	private void render() {
		try {
			var cmd = getBaseCommand();

			Path output = fileDialogsOutput.showSaveDialog(this, Path.of("splitscreen.mp4"),
				new FileDialogFilter("MP4 video", true, "mp4")
			);
			if(output == null)
				return;

			cmd.add("--encoder");
			cmd.add(((Encoder)cmbEncoder.getSelectedItem()).getName());
			cmd.add("--out");
			cmd.add(output.toString());
			cmd.add("--report");
			cmd.addAll(getCommandSplits());
			
			new WaitingDialog(this, getTitle(), "Rendering", true) {
				private static final long serialVersionUID = 1L;
				
				private Process p;
				private boolean aborted = false;

				@Override
				public void run() throws Exception {
					try {
						p = new ProcessBuilder(cmd)
							.redirectInput(Redirect.PIPE)
							.redirectOutput(Redirect.INHERIT)
							.redirectError(Redirect.PIPE)
							.start();
						p.getInputStream().close();
						BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
						String line;
						String prefix = "[splitscreen] progress: ";
						while((line = reader.readLine()) != null) {
							if(line.startsWith(prefix)) {
								String[] split = line.substring(prefix.length()).strip().split("/");
								int frame = Integer.parseInt(split[0]);
								int length = Integer.parseInt(split[1]);
								setProgressDeterminate(0, length);
								setProgress(frame);
								setProgressText(frame + " / " + length + " (" + (frame * 100 / length) + "%)");
							}
						}
						int res = p.waitFor();
						if(!aborted && res != 0)
							JOptionPane.showMessageDialog(this,
								"an error occured", getTitle(), JOptionPane.ERROR_MESSAGE);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}

				@Override
				public void cancel() {
					aborted = true;
					p.destroy();
				}
				
			}.setVisible(true);
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this,
				ex.getMessage(), getTitle(), JOptionPane.ERROR_MESSAGE);
		}
	}

	private List<String> getBaseCommand() throws Exception {
		if(list_inputs_model.getSize() == 0)
			throw new RuntimeException("No videos.");
		int n_splits = list_inputs_model.get(0).splits.size();
		for(int i = 0; i < list_inputs_model.getSize(); i++) {
			SplitScreenInput input = list_inputs_model.get(i);
			if(input.splits.size() != n_splits)
				throw new RuntimeException("All videos must have the same amount of splits.");
			if(!input.isComplete())
				throw new RuntimeException("All videos must have complete splits.");
		}
		if(n_splits == 0)
			throw new RuntimeException("No splits.");

		String[] split = cmbResolution.getSelectedItem().toString().split("x");
		if(split.length != 2)
			throw new RuntimeException("Invalid resolution");
		int width = Integer.parseInt(split[0]);
		int height = Integer.parseInt(split[1]);
		if(width <= 0 || height <= 0)
			throw new RuntimeException("Invalid resolution");
		double pause = Double.parseDouble(txtPause.getText());
		
		var cmd = new ArrayList<String>();
		cmd.add(Main.findExecutable("splitscreen-cli").toString());
		cmd.add("render");
		cmd.add("--res");
		cmd.add(width + "x" + height);
		cmd.add("--fps");
		cmd.add(spinnerFPS.getValue().toString());
		if(cmbCompare.getSelectedItem() != null)
			switch((Compare)cmbCompare.getSelectedItem()) {
			case TimeLoss:
				cmd.add("--cmp-loss");
				break;
			case TimeSave:
				cmd.add("--cmp-save");
				break;
			}
		cmd.add("--pause");
		cmd.add("" + pause);
		return cmd;
	}
	
	private List<String> getCommandSplits() {
		var cmd = new ArrayList<String>();
		cmd.add("--input-args");
		cmd.add("--");
		for(int i = 0; i < list_inputs_model.getSize(); i++) {
			SplitScreenInput input = list_inputs_model.get(i);
			cmd.add("--");
			cmd.add(input.videoPath.toString());
			for(Long time : input.splits)
				cmd.add("split " + SplitScreenInput.formatTime(time));
		}
		return cmd;
	}
	
	private void addInput(Path path) {
		SplitScreenInput input = new SplitScreenInput(path);
		for(int i = 0; i < list_splits_model.getSize(); i++)
			input.splits.add(null);

		int idx = list_inputs_model.getSize();
		list_inputs_model.addElement(input);
		select(idx);
		selectSplit(0);
	}

	private void select(int idx) {
		list.setSelectedIndex(idx);
	}
	
	private void playOrPause() {
		if(playbin.isPlaying()) {
			playbin.pause();
		} else {
			playbin.play();
		}
	}
	
	private void gotoPrevFrame() {
		if(seekingBackwards != null)
			return;
		seekingBackwards = playbin.queryPosition(Format.TIME);
		playbin.seek(-1.0, Format.TIME, EnumSet.of(SeekFlags.FLUSH, SeekFlags.ACCURATE),
			SeekType.SET, 0, SeekType.SET, seekingBackwards);
		playbin.sendEvent(new StepEvent(Format.BUFFERS, 0, 1.0, true, false));
		displaySeeking();
	}

	private void gotoNextFrame() {
		playbin.sendEvent(new StepEvent(Format.BUFFERS, 1, 1.0, true, false));
	}
	
	private void gotoRelative(int ms) {
		long pos = playbin.queryPosition(Format.TIME);
		if(pos < 0)
			return;
		long pos2 = Math.max(0, pos + (long)ms * 1000000);
		playbin.seekSimple(Format.TIME, EnumSet.of(SeekFlags.FLUSH), pos2);
	}
	
	private void display(SplitScreenInput input) {
		playbin.stop();
		list_splits_model.clear();
		seekingBackwards = null;
		seekingBackwardsHack = false;

		if(list.getSelectedIndex() < 0) {
			displayStopped();
		} else {
			SplitScreenInput selected = list.getSelectedValue();
			playbin.setURI(selected.videoPath.toUri());
			playbin.play();
			starting = true;
			video.requestFocus();
			list_splits_model.addAll(selected.splits);
			selectSplit(0);
		}

		refreshInfo();
	}
	
	private void displayStopped() {
		btnPlayPause.setText("Pause");
		btnPlayPause.setEnabled(false);
		btnPrevFrame.setEnabled(false);
		btnNextFrame.setEnabled(false);
		slider.setEnabled(false);

		slider.setMinimum(0);
		slider.setMaximum(1);
		slider.setValue(0);
		lblPosition.setText("- / -");
	}

	private void displaySeeking() {
		video.requestFocus();
		btnPlayPause.setEnabled(false);
		btnPrevFrame.setEnabled(false);
		btnNextFrame.setEnabled(false);
		slider.setEnabled(false);
	}

	private void displayPlaying() {
		btnPlayPause.setText("Pause");
		btnPlayPause.setEnabled(true);
		btnPrevFrame.setEnabled(true);
		btnNextFrame.setEnabled(true);
		slider.setEnabled(true);
	}

	private void displayPaused() {
		btnPlayPause.setText("Play");
		btnPlayPause.setEnabled(true);
		btnPrevFrame.setEnabled(true);
		btnNextFrame.setEnabled(true);
		slider.setEnabled(true);
	}

	private void refreshInfo() {
		SplitScreenInput input = list.getSelectedValue();
		if(input == null) {
			lbl_editor_title.setText("-");
		} else {
			String text = input.videoPath.getFileName() + " / ";
			if(input.splitFilePath == null)
				text += "-";
			else
				text += input.splitFilePath.getFileName();
			text += " (" + input.splits.size() + " splits)";
			if(input.dirty)
				text += " (*)";
			lbl_editor_title.setText(text);
		}
	}
	
	private void updatePosition() {
		long dur_ns = playbin.queryDuration(Format.TIME);
		if(dur_ns >= 0) {
			int dur_ms = (int)(dur_ns / 1000000);
			slider.setMinimum(0);
			slider.setMaximum(dur_ms);

			long pos_ns = playbin.queryPosition(Format.TIME);
			if(pos_ns >= 0) {
				int pos_ms = (int)(pos_ns / 1000000);
				slider.setValue(pos_ms);
				lblPosition.setText(formatTime(pos_ns) + " / " + formatTime(dur_ns));
			} else
				lblPosition.setText(formatTime(null) + " / " + formatTime(dur_ns));
		}
	}

	private void loadSplits() {
		SplitScreenInput input = list.getSelectedValue();
		if(input == null)
			return;
		
		try {
			Path path = fileDialogs.showOpenDialog(this,
				new FileDialogFilter("Split File", true, "splits")
			);
			if(path == null)
				return;
			
			input.loadFromFile(path);
			list_splits_model.clear();
			list_splits_model.addAll(input.splits);
			if(list_splits_model.getSize() > 0)
				selectSplit(0);
			refreshInfo();
		} catch(Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this,
				"Error: " + ex.getMessage(),
				getTitle(), JOptionPane.ERROR_MESSAGE);
		}
	}

	private void saveSplits() {
		SplitScreenInput input = list.getSelectedValue();
		if(input == null)
			return;

		try {
			if(!input.isComplete())
				throw new RuntimeException("Cannot save incomplete splits");

			Path splitFilePath = input.splitFilePath;
			if(splitFilePath == null) {
				String filename = input.videoPath.getFileName().toString();
				int idx = filename.lastIndexOf('.');
				if(idx < 0)
					splitFilePath = input.videoPath.resolveSibling(filename + ".splits");
				else
					splitFilePath = input.videoPath.resolveSibling(filename.substring(0, idx) + ".splits");
			}
			Path path = fileDialogs.showSaveDialog(this, splitFilePath,
				new FileDialogFilter("Split File", true, "splits")
			);
			if(path == null)
				return;

			input.saveToFile(path);
			refreshInfo();
		} catch(Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this,
				"Error: " + ex.getMessage(),
				getTitle(), JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void selectSplit(int idx) {
		list_splits.setSelectedIndex(idx);
	}
	
	private void insertSplitAbove() {
		int idx = Math.max(0, list_splits.getSelectedIndex());
		insertSplit(idx);
		selectSplit(idx);
	}

	private void insertSplitBelow() {
		int idx = list_splits.getSelectedIndex() + 1;
		insertSplit(idx);
		selectSplit(idx);
	}

	private void insertSplit(int idx) {
		if(list.getSelectedIndex() == -1)
			return;

		SplitScreenInput input = list.getSelectedValue();
		input.splits.add(idx, null);
		input.dirty = true;
		refreshInfo();

		list_splits_model.add(idx, null);
		if(list_splits.getSelectedIndex() < 0)
			list_splits.setSelectedIndex(0);
	}
	
	private void deleteSplit() {
		int idx = list_splits.getSelectedIndex();
		if(idx == -1)
			return;

		SplitScreenInput input = list.getSelectedValue();
		input.splits.remove(idx);
		input.dirty = true;
		refreshInfo();

		list_splits_model.remove(idx);
		list_splits.setSelectedIndex(Math.min(idx, list_splits_model.getSize() - 1));
	}

	private void enterSplit() {
		long time = playbin.queryPosition(Format.TIME);
		if(time < 0)
			return;
		setSplit(time);
	}

	private void resetSplit() {
		setSplit(null);
	}

	private void setSplit(Long time) {
		if(list_splits.getSelectedIndex() == -1)
			return;

		SplitScreenInput input = list.getSelectedValue();
		int idx = list_splits.getSelectedIndex();

		if(time != null) {
			for(int i = idx - 1; i >= 0; i--) {
				Long next = list_splits_model.get(i);
				if(next != null) {
					if(next > time) {
						JOptionPane.showMessageDialog(this,
							"Splits must be in chronological order",
							getTitle(), JOptionPane.ERROR_MESSAGE);
						return;
					}
					break;
				}
			}

			for(int i = idx + 1; i < list_splits_model.getSize(); i++) {
				Long next = list_splits_model.get(i);
				if(next != null) {
					if(next < time) {
						JOptionPane.showMessageDialog(this,
							"Splits must be in chronological order",
							getTitle(), JOptionPane.ERROR_MESSAGE);
						return;
					}
					break;
				}
			}
		}

		input.splits.set(idx, time);
		input.dirty = true;
		refreshInfo();

		list_splits_model.set(idx, time);
	}
	
	private void initialize() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1280, 720);
		setMinimumSize(new Dimension(1024, 576));
		setLocationRelativeTo(null);
		setTitle("SplitScreen");
		
		splitPane = new JSplitPane();
		getContentPane().add(splitPane, BorderLayout.CENTER);
		
		panel_left = new JPanel();
		splitPane.setLeftComponent(panel_left);
		panel_left.setLayout(new MigLayout("", "[grow]", "[][grow][]"));
		
		panel_settings = new JPanel();
		panel_settings.setBorder(new TitledBorder(null, "Settings", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panel_left.add(panel_settings, "cell 0 0,grow");
		panel_settings.setLayout(new MigLayout("", "[][grow]", "[][][][][]"));
		
		lblResolution = new JLabel("Resolution:");
		panel_settings.add(lblResolution, "cell 0 0,alignx trailing");
		
		cmbResolution = new JComboBox<>();
		cmbResolution.setEditable(true);
		panel_settings.add(cmbResolution, "cell 1 0,growx");
		
		lblFPS = new JLabel("FPS:");
		panel_settings.add(lblFPS, "cell 0 1,alignx trailing");
		
		spinnerFPS = new JSpinner();
		spinnerFPS.setModel(new SpinnerNumberModel(60, 1, null, 1));
		panel_settings.add(spinnerFPS, "cell 1 1,growx");
		
		lblCompare = new JLabel("Compare:");
		panel_settings.add(lblCompare, "cell 0 2,alignx trailing");
		
		cmbCompare = new JComboBox<>();
		panel_settings.add(cmbCompare, "cell 1 2,growx");
		
		lblPause = new JLabel("Pause after split:");
		panel_settings.add(lblPause, "flowy,cell 0 3,alignx trailing");
		
		txtPause = new JTextField();
		txtPause.setText("2.0");
		panel_settings.add(txtPause, "cell 1 3,growx");
		txtPause.setColumns(10);

		lblEncoder = new JLabel("Encoder:");
		panel_settings.add(lblEncoder, "cell 0 4,alignx trailing");

		cmbEncoder = new JComboBox<>();
		panel_settings.add(cmbEncoder, "cell 1 4,growx");
		
		panel_inputs = new JPanel();
		panel_inputs.setBorder(new TitledBorder(null, "Videos", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panel_left.add(panel_inputs, "cell 0 1,grow");
		panel_inputs.setLayout(new MigLayout("", "[grow]", "[grow][]"));
		
		scrollPane = new JScrollPane();
		panel_inputs.add(scrollPane, "cell 0 0,grow");
		
		list = new JList<>();
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		scrollPane.setViewportView(list);
		
		panel_inputs_buttons = new JPanel();
		FlowLayout fl_panel_inputs_buttons = (FlowLayout) panel_inputs_buttons.getLayout();
		fl_panel_inputs_buttons.setAlignment(FlowLayout.LEFT);
		panel_inputs.add(panel_inputs_buttons, "cell 0 1,grow");
		
		btnAddInput = new JButton("Add");
		panel_inputs_buttons.add(btnAddInput);
		
		btnRemoveInput = new JButton("Remove");
		panel_inputs_buttons.add(btnRemoveInput);
		
		panel_controls = new JPanel();
		FlowLayout fl_panel_controls = (FlowLayout) panel_controls.getLayout();
		fl_panel_controls.setAlignment(FlowLayout.RIGHT);
		panel_left.add(panel_controls, "cell 0 2,grow");
		
		btnPlay = new JButton("Play");
		panel_controls.add(btnPlay);
		
		btnRender = new JButton("Render");
		panel_controls.add(btnRender);
		
		panel_editor = new JPanel();
		splitPane.setRightComponent(panel_editor);
		panel_editor.setLayout(new BorderLayout(0, 0));
		
		panel_editor_title = new JPanel();
		panel_editor.add(panel_editor_title, BorderLayout.NORTH);
		panel_editor_title.setLayout(new MigLayout("", "[grow][][][]", "[]"));
		
		lbl_editor_title = new JLabel("-");
		lbl_editor_title.setFont(new Font("Dialog", Font.BOLD, 12));
		panel_editor_title.add(lbl_editor_title, "cell 0 0");
		
		btnLoadSplits = new JButton("Load");
		panel_editor_title.add(btnLoadSplits, "cell 1 0");
		
		btnSaveSplits = new JButton("Save");
		panel_editor_title.add(btnSaveSplits, "cell 2 0");
		
		btnAbout = new JButton("About");
		panel_editor_title.add(btnAbout, "cell 3 0");

		splitPane_1 = new JSplitPane();
		splitPane_1.setResizeWeight(1.0);
		panel_editor.add(splitPane_1, BorderLayout.CENTER);
		
		panel_player = new JPanel();
		splitPane_1.setLeftComponent(panel_player);
		
		video = new GstVideoComponent();
		video.setFocusable(true);
		panel_player.setLayout(new BorderLayout(0, 0));
		panel_player.add(video, BorderLayout.CENTER);
		
		panel_player_controls = new JPanel();
		panel_player.add(panel_player_controls, BorderLayout.SOUTH);
		panel_player_controls.setLayout(new MigLayout("", "[][][][grow][]", "[]"));
		
		btnPlayPause = new JButton("Play");
		btnPlayPause.setFocusable(false);
		panel_player_controls.add(btnPlayPause, "cell 0 0");
		
		btnPrevFrame = new JButton("<< Frame");
		btnPrevFrame.setFocusable(false);
		panel_player_controls.add(btnPrevFrame, "cell 1 0");
		
		btnNextFrame = new JButton("Frame >>");
		btnNextFrame.setFocusable(false);
		panel_player_controls.add(btnNextFrame, "cell 2 0");
		
		slider = new JSlider();
		slider.setFocusable(false);
		panel_player_controls.add(slider, "cell 3 0,growx,aligny center");
		
		lblPosition = new JLabel("- / -");
		lblPosition.setFont(new Font("Monospaced", Font.BOLD, 12));
		panel_player_controls.add(lblPosition, "cell 4 0");
		
		panel_splits = new JPanel();
		splitPane_1.setRightComponent(panel_splits);
		panel_splits.setLayout(new MigLayout("", "[grow]", "[grow][]0[]"));
		
		scrollPane_1 = new JScrollPane();
		panel_splits.add(scrollPane_1, "cell 0 0,grow");
		
		list_splits = new JList<>();
		list_splits.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list_splits.setFont(new Font("Monospaced", Font.PLAIN, 12));
		scrollPane_1.setViewportView(list_splits);

		panel_splits_buttons2 = new JPanel();
		FlowLayout fl_panel_splits_buttons1 = (FlowLayout) panel_splits_buttons2.getLayout();
		fl_panel_splits_buttons1.setAlignment(FlowLayout.RIGHT);
		panel_splits.add(panel_splits_buttons2, "cell 0 1,grow");
		
		btnInsertAbove = new JButton("Insert Above");
		panel_splits_buttons2.add(btnInsertAbove);
		
		btnInsertBelow = new JButton("Insert Below");
		panel_splits_buttons2.add(btnInsertBelow);

		panel_splits_buttons3 = new JPanel();
		FlowLayout flowLayout = (FlowLayout) panel_splits_buttons3.getLayout();
		flowLayout.setAlignment(FlowLayout.RIGHT);
		panel_splits.add(panel_splits_buttons3, "cell 0 2,grow");
		
		btnSetSplit = new JButton("Set");
		panel_splits_buttons3.add(btnSetSplit);
		
		btnClearSplit = new JButton("Clear");
		panel_splits_buttons3.add(btnClearSplit);
		
		btnRemoveSplit = new JButton("Remove");
		panel_splits_buttons3.add(btnRemoveSplit);
	}
	
	private static String formatTime(Long time_ns) {
		if(time_ns == null)
			return "--:--:--.---";
		long ms_total = time_ns / 1000000;
		long ms = ms_total % 1000;
		long s_total = ms_total / 1000;
		long s = s_total % 60;
		long m_total = s_total / 60;
		long m = m_total % 60;
		long h = m_total / 60;
		return String.format("%02d:%02d:%02d.%03d", h, m, s, ms);
	}

}
