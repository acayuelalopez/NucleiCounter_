import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.TextField;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.measure.Measurements;
import ij.plugin.ChannelSplitter;
import ij.plugin.PlugIn;
import ij.plugin.RoiRotator;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import ij.text.TextPanel;
import ij.text.TextWindow;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.strel.SquareStrel;

public class NucleiCounter_ implements PlugIn, Measurements {

	ImagePlus imp;
	JFrame frameMain, frameSlider;
	JButton okButton, cancelButton, buttonSave, exportButton;
	JPanel panelOkCancel, panelImages, panelSave;
	TextField textSave, textImages;
	JLabel directLabelImages;
	Preferences prefImages, prefSave;
	String NUCLEICOUNTER_IMAGES_DEFAULT_PATH, NUCLEICOUNTER_SAVE_DEFAULT_PATH, pathImages, pathSave;
	String[] imageTitles;
	UserDialog areaDialog;
	ImageCanvas canvas;
	JTextField pzTF;
	RoiManager rm;
	JTable tableDef;
	DefaultTableModel modelDef;
	JScrollPane jScrollPaneDef;
	ArrayList<String> nucleiCounter;
	List<ArrayList<String>> nucleiCounterDef = new ArrayList<ArrayList<String>>();

	@Override
	public void run(String arg0) {

		NUCLEICOUNTER_IMAGES_DEFAULT_PATH = "images_path";
		NUCLEICOUNTER_SAVE_DEFAULT_PATH = "save_path";
		prefImages = Preferences.userRoot();
		prefSave = Preferences.userRoot();
		imp = WindowManager.getCurrentImage();
		areaDialog = new UserDialog("Action Warning", "Draw AREA selection, for cell-nuclei counting.");

		if (imp == null) {
			try {

				JFrame.setDefaultLookAndFeelDecorated(true);
				JDialog.setDefaultLookAndFeelDecorated(true);
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
			} catch (Exception e) {
				e.printStackTrace();
			}
			createAndShowGUI();
		}
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				pathImages = textImages.getText();
				prefImages.put(NUCLEICOUNTER_IMAGES_DEFAULT_PATH, textImages.getText());
				pathSave = textSave.getText();
				prefSave.put(NUCLEICOUNTER_SAVE_DEFAULT_PATH, textSave.getText());
				frameMain.dispatchEvent(new WindowEvent(frameMain, WindowEvent.WINDOW_CLOSING));

				if (imp == null) {
					File imageFolder = new File(pathImages);
					File[] listOfFiles = imageFolder.listFiles();
					imageTitles = new String[listOfFiles.length];

					for (int i = 0; i < listOfFiles.length; i++)
						imageTitles[i] = listOfFiles[i].getName();

					String[] paths = new String[listOfFiles.length];
					for (int i = 0; i < paths.length; i++)
						paths[i] = pathImages + File.separator + imageTitles[i];
					Thread process0 = new Thread(new Runnable() {

						public void run() {
							rm = new RoiManager();
							sliderAngleGUI();
							for (int i = 0; i < listOfFiles.length; i++) {
								if (rm.getCount() != 0)
									rm.reset();

								imp = new ImagePlus(textImages.getText() + File.separator + imageTitles[i]);
								imp.show();
								imp.getCanvas().addMouseListener(new MouseAdapter() {
									public void mouseClicked(MouseEvent e) {
										if (e.getClickCount() == 2) {

											Roi roi = new Roi(imp.getCanvas().offScreenX(e.getX()),
													imp.getCanvas().offScreenY(e.getY()),
													(int) (50.0 / Double.parseDouble(pzTF.getText())),
													(int) (40.0 / Double.parseDouble(pzTF.getText())));
											imp.setRoi(roi);
											rm.runCommand(imp, "Show All with labels");
											rm.addRoi(roi);
										}

									}

								});
								areaDialog.show();
								Roi[] rois = rm.getRoisAsArray();
								ImagePlus[] imps = imp.crop(rois);
								ImagePlus[] impsT = new ImagePlus[imps.length];
								nucleiCounter = new ArrayList<String>();
								for (int j = 0; j < imps.length; j++) {
									impsT[j] = imps[j].duplicate();
									IJ.run(impsT[j], "8-bit", "");
									IJ.run(impsT[j], "Auto Threshold", "method=Huang2 ignore_white");
									impsT[j] = new ImagePlus(impsT[j].getTitle(),
											Morphology.opening(impsT[j].getProcessor(), SquareStrel.fromDiameter(10)));
									IJ.run(impsT[j], "Create Selection", "");
									Roi mainRoi = impsT[j].getRoi();
									Roi[] roisPS = new ShapeRoi(mainRoi).getRois();
									nucleiCounter.add(String.valueOf(roisPS.length));
									// IJ.log("Area---" + (j + 1) + "-----" + roisPS.length);

								}
								nucleiCounterDef.add(nucleiCounter);
								imp.hide();

							}
							rm.close();
							processTable(imageTitles, nucleiCounterDef);

							frameSlider.dispatchEvent(new WindowEvent(frameSlider, WindowEvent.WINDOW_CLOSING));
						}
					});
					process0.start();

				}

			}
		});

		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frameMain.dispatchEvent(new WindowEvent(frameMain, WindowEvent.WINDOW_CLOSING));
			}
		});

	}

	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void mouseReleased(MouseEvent arg0) {
	}

	public void processTable(String[] imageTitles, List<ArrayList<String>> nucleiCounterDef) {

		exportButton = new JButton("");
		ImageIcon iconExport = createImageIcon("images/export.png");
		Icon iconExportCell = new ImageIcon(iconExport.getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH));
		exportButton.setIcon(iconExportCell);

		tableDef = new JTable();
		modelDef = new DefaultTableModel();
		jScrollPaneDef = new JScrollPane(tableDef);
		jScrollPaneDef.setPreferredSize(new Dimension(650, 300));
		modelDef = new DefaultTableModel(imageTitles.length, 20) {

			@Override
			public Class<?> getColumnClass(int column) {
				if (getRowCount() >= 0) {
					Object value = getValueAt(0, column);
					if (value != null) {
						return getValueAt(0, column).getClass();
					}
				}

				return super.getColumnClass(column);
			}

			public boolean isCellEditable(int row, int col) {
				return false;
			}

		};
		tableDef.setModel(modelDef);
		for (int i = 0; i < modelDef.getRowCount(); i++)
			modelDef.setValueAt(imageTitles[i], i, tableDef.convertColumnIndexToModel(0));
		for (int i = 0; i < nucleiCounterDef.size(); i++)
			for (int j = 0; j < nucleiCounterDef.get(i).size(); j++)
				modelDef.setValueAt(nucleiCounterDef.get(i).get(j), tableDef.convertRowIndexToModel(i),
						tableDef.convertColumnIndexToModel(j + 1));

		tableDef.setModel(modelDef);
		tableDef.setSelectionBackground(new Color(229, 255, 204));
		tableDef.setSelectionForeground(new Color(0, 102, 0));
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(JLabel.CENTER);
		tableDef.setDefaultRenderer(String.class, centerRenderer);
		tableDef.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tableDef.setRowHeight(60);
		tableDef.setAutoCreateRowSorter(true);
		for (int u = 0; u < tableDef.getColumnCount(); u++)
			tableDef.getColumnModel().getColumn(u).setPreferredWidth(170);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		JPanel imagePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		imagePanel.add(jScrollPaneDef);
		JPanel exportPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		exportPanel.add(exportButton);
		mainPanel.add(imagePanel);
		mainPanel.add(exportPanel);
		JFrame frameDef = new JFrame();
		frameDef.setTitle("Results");
		frameDef.setResizable(false);
		frameDef.add(mainPanel);
		frameDef.pack();
		frameDef.setSize(660, 400);
		frameDef.setLocationRelativeTo(null);
		frameDef.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frameDef.setVisible(true);
		exportButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				try {
					csvExport();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			}
		});

	}

	public void csvExport() throws IOException {

		HSSFWorkbook fWorkbook = new HSSFWorkbook();
		HSSFSheet fSheet = fWorkbook.createSheet("new Sheet");
		HSSFFont sheetTitleFont = fWorkbook.createFont();
		HSSFCellStyle cellStyle = fWorkbook.createCellStyle();
		sheetTitleFont.setBold(true);
		// sheetTitleFont.setColor();
		TableModel model = tableDef.getModel();

		// Get Header
		TableColumnModel tcm = tableDef.getColumnModel();
		HSSFRow hRow = fSheet.createRow((short) 0);
		for (int j = 0; j < tcm.getColumnCount(); j++) {
			HSSFCell cell = hRow.createCell((short) j);
			cell.setCellValue(tcm.getColumn(j).getHeaderValue().toString());
			cell.setCellStyle(cellStyle);
		}

		// Get Other details
		for (int i = 0; i < model.getRowCount(); i++) {
			HSSFRow fRow = fSheet.createRow((short) i + 1);
			for (int j = 0; j < model.getColumnCount(); j++) {
				HSSFCell cell = fRow.createCell((short) j);
				cell.setCellValue(model.getValueAt(i, j).toString());
				cell.setCellStyle(cellStyle);
			}
		}
		FileOutputStream fileOutputStream;
		fileOutputStream = new FileOutputStream(pathSave + File.separator + "NucleiCounting_data.xlsx");
		try (BufferedOutputStream bos = new BufferedOutputStream(fileOutputStream)) {
			fWorkbook.write(bos);
		}
		fileOutputStream.close();

		JOptionPane.showMessageDialog(null, "NucleiCounting.xlsx exported to " + pathSave);

	}

	// TODO Auto-generated method stub
	public void createAndShowGUI() {

		okButton = new JButton("");
		okButton.setBounds(50, 100, 95, 30);
		ImageIcon iconOk = createImageIcon("images/ok.png");
		Icon iconOKCell = new ImageIcon(iconOk.getImage().getScaledInstance(17, 15, Image.SCALE_SMOOTH));
		okButton.setIcon(iconOKCell);
		okButton.setToolTipText("Click this button to import your file to table.");
		cancelButton = new JButton("");
		cancelButton.setBounds(50, 100, 95, 30);
		ImageIcon iconCancel = createImageIcon("images/cancel.png");
		Icon iconCancelCell = new ImageIcon(iconCancel.getImage().getScaledInstance(17, 15, Image.SCALE_SMOOTH));
		cancelButton.setIcon(iconCancelCell);
		cancelButton.setToolTipText("Click this button to cancel.");
		panelOkCancel = new JPanel();
		panelOkCancel.setLayout(new FlowLayout());
		panelOkCancel.add(okButton);
		panelOkCancel.add(cancelButton);

		ImageIcon iconBrowse = createImageIcon("images/browse.png");
		Icon iconBrowseCell = new ImageIcon(iconBrowse.getImage().getScaledInstance(15, 15, Image.SCALE_SMOOTH));

		JButton buttonImages = new JButton("");
		buttonImages.setIcon(iconBrowseCell);
		textImages = new TextField(20);
		textImages.setText(prefImages.get(NUCLEICOUNTER_IMAGES_DEFAULT_PATH, ""));
		DirectoryListener listenerImages = new DirectoryListener("Browse for directory to collect images ", textImages,
				JFileChooser.FILES_AND_DIRECTORIES);
		directLabelImages = new JLabel("   Images Directory : ");
		directLabelImages.setFont(new Font("Helvetica", Font.BOLD, 12));
		buttonImages.addActionListener(listenerImages);
		panelImages = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panelImages.add(directLabelImages);
		panelImages.add(textImages);
		panelImages.add(buttonImages);

		JButton buttonSave = new JButton("");
		buttonSave.setIcon(iconBrowseCell);
		panelSave = new JPanel(new FlowLayout(FlowLayout.LEFT));
		textSave = new TextField(20);
		textSave.setText(prefSave.get(NUCLEICOUNTER_SAVE_DEFAULT_PATH, ""));
		DirectoryListener listenerSave = new DirectoryListener("Browse for directory to save files ", textSave,
				JFileChooser.FILES_AND_DIRECTORIES);
		JLabel directLabelSave = new JLabel("   Results Directory : ");
		directLabelSave.setFont(new Font("Helvetica", Font.BOLD, 12));
		buttonSave.addActionListener(listenerSave);
		panelSave.add(directLabelSave);
		panelSave.add(textSave);
		panelSave.add(buttonSave);
		JLabel pixelSizeLabel = new JLabel("Pixel-Size: ");
		pzTF = new JTextField(8);
		JPanel psPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		psPanel.add(pixelSizeLabel);
		psPanel.add(pzTF);
		JPanel generalPanel = new JPanel();
		generalPanel.setLayout(new BoxLayout(generalPanel, BoxLayout.Y_AXIS));
		generalPanel.add(panelImages);
		generalPanel.add(panelSave);
		generalPanel.add(psPanel);
		generalPanel.add(panelOkCancel);

		frameMain = new JFrame("Nuclei Counter");
		frameMain.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frameMain.add(generalPanel);
		frameMain.setSize(730, 430);
		frameMain.pack();
		frameMain.setResizable(false);
		frameMain.setVisible(true);

	}

	public void sliderAngleGUI() {

		JPanel filtersMin = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton replaceButton = new JButton("Replace");
		JPanel rBPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		rBPanel.add(replaceButton);
		JSpinner filterMin = new JSpinner(new SpinnerNumberModel(0, 0, 360, 1));
		filterMin.setPreferredSize(new Dimension(60, 20));
		JSlider sliderMin = new JSlider(0, 360, 0);
		sliderMin.setPreferredSize(new Dimension(150, 15));
		JLabel filterMinLabel = new JLabel(" Angle :  ");
		filtersMin.add(filterMinLabel);
		filtersMin.add(sliderMin);
		filtersMin.add(Box.createHorizontalStrut(2));
		filtersMin.add(filterMin);
		filtersMin.add(Box.createHorizontalStrut(2));
		filtersMin.add(rBPanel);
		frameSlider = new JFrame("Roi Rotator");
		frameSlider.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frameSlider.add(filtersMin);
		frameSlider.setSize(200, 40);
		frameSlider.pack();
		frameSlider.setVisible(true);
		sliderMin.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {

				filterMin.setValue(sliderMin.getValue());
				IJ.run(imp, "Rotate...", String.format("rotate angle=%d", 1));

			}
		});

		filterMin.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				sliderMin.setValue((int) filterMin.getValue());

			}
		});
		replaceButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				rm.setRoi(imp.getRoi(), rm.getSelectedIndex());
				filterMin.setValue(0);
				sliderMin.setValue(0);

			}
		});
	}

	public static ImageIcon createImageIcon(String path) {
		java.net.URL imgURL = NucleiCounter_.class.getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL);
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}

}
