import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;


public class ferm_crtl_GUI extends JFrame {

	private JPanel contentPane;
	private JTextField textConIP;
	public static JTextField textSp1;
	private static JTextField textSp2;
	private static JTextField textSpC;
	public static JLabel lblTc1;
	public static JLabel labelTc2;
	public static JLabel labelTcC;
	public static JLabel lblFV1Bath;
	public static JLabel lblFV2Bath;
	public static JLabel lbltcAMB;
	public static JLabel lblFv1chill;
	public static JLabel labelFv2Chill;
	public static JLabel labelChillChill;
	public static JLabel lblNoYes;
	private XYPlot plot;
    private int datasetIndex = 5;
    public final DateAxis axis;
    static XYSeries series[] = new XYSeries[6];
    String[] names={"sp1","sp2","spc","FV1","FV2","Coolant"};
 //   public static final String ipPattern =
    enum FermVessels {FV1,FV2,FVC};
//	static public FermVessel FV1 = new FermVessel(1,75,0.2);
//	static public FermVessel FV2 = new FermVessel(2,75,0.2);
//	static public FermVessel FVC = new FermVessel(0,38,2.0);
//	private Ferm_TCP ferm_TCP=new Ferm_TCP(FV1, FV2, FVC);
	private Ferm_TCP ferm_TCP= new Ferm_TCP();
	public FermVessel FV1 = ferm_TCP.getFV1();
	public FermVessel FV2 =	ferm_TCP.getFV2();	
	public FermVessel FVC = ferm_TCP.getFVC();
	//Confusing, I know. THis was done since tcp modifies the fv's, not this class.
	private enum ConnectionState {CONNECTING, CONNECTED, CLOSED, GETTING, SETTING }

//TODO change star from blue to red depending on heating vs cooling mode
//TODO make a state machine that is threaded to handle the connection, get, set commands

	public Timer tick = new Timer(5000,new ActionListener(){
		//this method checks values from server every 5 seconds, updates the onscreen labels
		public void actionPerformed(ActionEvent k){
			try{
//				if (ferm_TCP.!=null){
//				System.out.println(ferm_TCP.getState().toString());
//				if (ferm_TCP.getState().toString()=="CLOSED"){
//			ferm_TCP.connect();
//			System.out.println(ferm_TCP.getState().toString());
//				}
//				}
			ferm_TCP.getValues();
//			if(ferm_TCP.getState().toString()=="CLOSED"){
			series[0].add(System.currentTimeMillis(),FV1.getSP());
			series[1].add(System.currentTimeMillis(),FV2.getSP());
			series[2].add(System.currentTimeMillis(),FVC.getSP());
			series[3].add(System.currentTimeMillis(),FV1.getTemp());
			series[4].add(System.currentTimeMillis(),FV2.getTemp());
			series[5].add(System.currentTimeMillis(),FVC.getTemp());
			if (series[0].getMaxX()-series[0].getMinX()>60000){
				//if the series covers more than 5 minutes (1800000 msec), then get rid of the leading entry
				System.out.println("deleting line");
				for(int i =0;i<6;i++){
					series[i].remove(1);
				}
			}
			lblTc1.setText(Integer.toString((int)FV1.getTemp()));
			labelTc2.setText(Integer.toString((int)FV2.getTemp()));
			labelTcC.setText(Integer.toString((int)FVC.getTemp()));
			lblFV1Bath.setText(Integer.toString((int)FV1.getBathTemp()));
			lblFV2Bath.setText(Integer.toString((int)FV2.getBathTemp()));
			lbltcAMB.setText(Integer.toString((int)ferm_TCP.gettcAMB()));
			if (!textSp1.isEditable()){
				textSp1.setText(Double.toString(FV1.getSP()));	
			}
			if (!textSp2.isEditable()){
				textSp2.setText(Double.toString(FV2.getSP()));
			}
			if (!textSpC.isEditable()){
				textSpC.setText(Double.toString(FVC.getSP()));
			}
			if (FV1.isChilling()){
				lblFv1chill.setText("*");
			}else{
				lblFv1chill.setText("");
			}
			if (FV2.isChilling()){
				labelFv2Chill.setText("*");
			}else{
				labelFv2Chill.setText("");
			}
			if (FVC.isChilling()){
				labelChillChill.setText("*");
			}else{
				labelChillChill.setText("");
			}
// 			}
			}catch (Exception e){
				//if nullPointer, conn not established, print message to try again
				e.printStackTrace();
				lblNoYes.setText("NO");
				lblNoYes.setForeground(Color.RED);
				tick.stop();
			}
		}
	});


	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ferm_crtl_GUI frame = new ferm_crtl_GUI();
					frame.setVisible(true);			
				} catch (Exception e) {
					e.printStackTrace();
				}
			}			
		});
		
	}
	

	/**
	 * Create the frame.
	 */
	public ferm_crtl_GUI() {
		for (int i=0;i<6;i++){
			series[i]=new XYSeries("Set:"+names[i]);
		}
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent arg0) {
				try{
					ferm_TCP.clientSocket.close();
				}catch (Exception e){
					e.printStackTrace();
				}System.exit(EXIT_ON_CLOSE);
			}
		});//Ensures connection is closed on application close
		
		setTitle("Fermentation Chiller Sys");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 478, 667);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JLabel lblConnection = new JLabel("Connection:");
		lblConnection.setBounds(0, 11, 191, 45);
		lblConnection.setFont(new Font("Dialog", Font.PLAIN, 31));
		contentPane.add(lblConnection);
		
		final JLabel lblNoYes = new JLabel("NO");
		lblNoYes.setBounds(188, 12, 70, 38);
		lblNoYes.setForeground(Color.RED);
		lblNoYes.setFont(new Font("Dialog", Font.PLAIN, 36));
		contentPane.add(lblNoYes);
		
		textConIP = new JTextField();
		textConIP.setBounds(260, 32, 112, 20);
		textConIP.setText("68.47.140.19");
		textConIP.setBackground(SystemColor.menu);
		textConIP.setFont(new Font("Dialog", Font.PLAIN, 14));
		contentPane.add(textConIP);
		textConIP.setColumns(10);
		
		JLabel lblFV1 = new JLabel("Fermenter #1");
		lblFV1.setBounds(10, 81, 168, 56);
		lblFV1.setFont(new Font("Tahoma", Font.PLAIN, 22));
		contentPane.add(lblFV1);
		
		JLabel lblFv2 = new JLabel("Fermenter #2");
		lblFv2.setBounds(10, 143, 168, 51);
		lblFv2.setFont(new Font("Tahoma", Font.PLAIN, 22));
		contentPane.add(lblFv2);
		
		JLabel lblChiller = new JLabel("Coolant");
		lblChiller.setBounds(200, 214, 88, 33);
		lblChiller.setFont(new Font("Tahoma", Font.PLAIN, 22));
		contentPane.add(lblChiller);
		
		JLabel lblSp1 = new JLabel("#1");
		lblSp1.setBounds(10, 292, 55, 33);
		lblSp1.setFont(new Font("Tahoma", Font.PLAIN, 20));
		contentPane.add(lblSp1);
		
		JLabel lblSp2 = new JLabel("#2");
		lblSp2.setBounds(90, 292, 55, 33);
		lblSp2.setFont(new Font("Tahoma", Font.PLAIN, 20));
		contentPane.add(lblSp2);
		
		JLabel lblSpchiller = new JLabel("Coolant");
		lblSpchiller.setBounds(170, 289, 88, 38);
		lblSpchiller.setFont(new Font("Tahoma", Font.PLAIN, 20));
		contentPane.add(lblSpchiller);
		
		textSp1 = new JTextField();
		textSp1.setBounds(10, 329, 55, 20);
		textSp1.setToolTipText("Double click to enter a new set point.\r\nThe set point is the desired temperature.");
		textSp1.setEditable(false);
		textSp1.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				textSp1.setEditable(true);
			}
		});
		textSp1.setFont(new Font("Tahoma", Font.PLAIN, 18));
		contentPane.add(textSp1);
		textSp1.setColumns(10);
		
		
		textSp2 = new JTextField();
		textSp2.setBounds(90, 329, 55, 20);
		textSp2.setToolTipText("Double click to enter a new set point.");
		textSp2.setEditable(false);
		textSp2.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				textSp2.setEditable(true);
			}
		});
		textSp2.setFont(new Font("Tahoma", Font.PLAIN, 18));
		textSp2.setColumns(10);
		contentPane.add(textSp2);
		
		textSpC = new JTextField();
		textSpC.setBounds(170, 329, 55, 20);
		textSpC.setToolTipText("Double click to enter a new set point.");
		textSpC.setEditable(false);
		textSpC.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				textSpC.setEditable(true);
			}
		});
		textSpC.setFont(new Font("Tahoma", Font.PLAIN, 18));
		textSpC.setColumns(10);
		contentPane.add(textSpC);
		
		lblTc1 = new JLabel("");
		lblTc1.setBounds(190, 92, 88, 41);
		lblTc1.setFont(new Font("Tahoma", Font.PLAIN, 32));
		contentPane.add(lblTc1);
		
		labelTc2 = new JLabel("");
		labelTc2.setBounds(190, 143, 88, 41);
		labelTc2.setFont(new Font("Tahoma", Font.PLAIN, 32));
		contentPane.add(labelTc2);
		
		labelTcC = new JLabel("");
		labelTcC.setBounds(300, 206, 55, 46);
		labelTcC.setFont(new Font("Tahoma", Font.PLAIN, 32));
		contentPane.add(labelTcC);
		
		lblFV1Bath = new JLabel("");
		lblFV1Bath.setBounds(280, 92, 88, 41);
		lblFV1Bath.setFont(new Font("Tahoma", Font.PLAIN, 32));
		contentPane.add(lblFV1Bath);
		
		lblFV2Bath = new JLabel("");
		lblFV2Bath.setBounds(280, 143, 88, 41);
		lblFV2Bath.setFont(new Font("Tahoma", Font.PLAIN, 32));
		contentPane.add(lblFV2Bath);
		
		lbltcAMB = new JLabel("");
		lbltcAMB.setBounds(118, 206, 55, 46);
		lbltcAMB.setFont(new Font("Tahoma", Font.PLAIN, 32));
		contentPane.add(lbltcAMB);
		
		lblFv1chill = new JLabel("");
		lblFv1chill.setBounds(385, 100, 46, 33);
		lblFv1chill.setForeground(Color.BLUE);
		lblFv1chill.setFont(new Font("Tahoma", Font.BOLD, 32));
		contentPane.add(lblFv1chill);
		
		labelFv2Chill = new JLabel("");
		labelFv2Chill.setBounds(380, 151, 46, 33);
		labelFv2Chill.setForeground(Color.BLUE);
		labelFv2Chill.setFont(new Font("Tahoma", Font.BOLD, 32));
		contentPane.add(labelFv2Chill);
		
		labelChillChill = new JLabel("");
		labelChillChill.setBounds(385, 214, 46, 33);
		labelChillChill.setForeground(Color.BLUE);
		labelChillChill.setFont(new Font("Tahoma", Font.BOLD, 32));
		contentPane.add(labelChillChill);
		
		JLabel lblPumpOn = new JLabel("Pump on");
		lblPumpOn.setBounds(375, 68, 77, 14);
		lblPumpOn.setFont(new Font("Tahoma", Font.PLAIN, 12));
		contentPane.add(lblPumpOn);
		
		JSeparator separator = new JSeparator();
		separator.setBounds(10, 267, 414, 2);
		contentPane.add(separator);
		
		JButton btnSet = new JButton("SET");
		btnSet.setBounds(294, 329, 89, 23);
		btnSet.setToolTipText("Click to send the new set points to the controller");
		btnSet.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e){
				textSp1.setEditable(false);
				textSp2.setEditable(false);
				textSpC.setEditable(false);
				//CODE here to check format of set text boxes				
				try{
//					if (ferm_TCP.getState().toString()==CONNECTED){
					ferm_TCP.setValues(Double.parseDouble(textSp1.getText()), Double.parseDouble(textSp2.getText()), Double.parseDouble(textSpC.getText()));
//					}
					}catch (Exception exc){
					System.out.print(exc);
				}
			}
		});
		contentPane.add(btnSet);
		
		JButton btnGo = new JButton("GO!");
		btnGo.setBounds(384, 11, 68, 23);
		btnGo.setToolTipText("Click to connect to the controller");
		btnGo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e){
				try{
					if (textConIP.getText()!=" "){	
						ferm_TCP.setIP(textConIP.getText());
					}else{
						JOptionPane.showMessageDialog(null, "You should really enter a valid ip address", "Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
					ferm_TCP.connect();
					lblNoYes.setText("YES");
					lblNoYes.setForeground(Color.GREEN);
					tick.start();
//					String ipAddr = ferm_TCP.clientSocket.getInetAddress().toString();
//					if (ipAddr.startsWith("/")){
//						ipAddr=ipAddr.substring(1);
//					}
//					textConIP.setText(ipAddr);
				}catch (Exception exc){
					exc.printStackTrace();
					tick.stop();
					lblNoYes.setText("NO");
					lblNoYes.setForeground(Color.RED);
				}
				
			}
		});
		contentPane.add(btnGo);
		
		JButton btnStop = new JButton("Stop");
		btnStop.setBounds(384, 32, 68, 23);
		btnStop.setToolTipText("Click to disconnect from the controller");
		btnStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try{
					tick.stop();
					ferm_TCP.stopThis();
					lblNoYes.setText("NO");
					lblNoYes.setForeground(Color.RED);
				}catch (Exception e){
					e.printStackTrace();
				}
				
			}
		});
		contentPane.add(btnStop);
		
		JSeparator separator_1 = new JSeparator();
		separator_1.setBounds(10, 377, 414, 2);
		contentPane.add(separator_1);
		
		JPanel panel = new JPanel();
		panel.setBounds(10, 379, 414, 198);
		final XYSeriesCollection sp1 = new XYSeriesCollection(series[0]);
        final XYSeriesCollection sp2 = new XYSeriesCollection(series[1]);
        final XYSeriesCollection spC = new XYSeriesCollection(series[2]);
        final XYSeriesCollection tc1 = new XYSeriesCollection(series[3]);
        final XYSeriesCollection tc2 = new XYSeriesCollection(series[4]);
        final XYSeriesCollection tcC = new XYSeriesCollection(series[5]); 
        final JFreeChart chart = ChartFactory.createTimeSeriesChart(
	            null, "Time", "Temp (F)", sp1
	        );
	        chart.setBackgroundPaint(SystemColor.menu);
	        
	       
	       
	        this.plot = chart.getXYPlot();
	        this.plot.setDataset(1, sp2);
	        this.plot.setRenderer(1, new XYLineAndShapeRenderer(true, false));
	        this.plot.setDataset(2, spC);
	        this.plot.setRenderer(2, new XYLineAndShapeRenderer(true, false));
	        this.plot.setDataset(3, tc1);
	        this.plot.setRenderer(3, new XYLineAndShapeRenderer(true, false));
	        this.plot.setDataset(4, tc2);
	        this.plot.setRenderer(4, new XYLineAndShapeRenderer(true, false));
	        this.plot.setDataset(5, tcC);
	        this.plot.setRenderer(5, new XYLineAndShapeRenderer(true, false));
	        this.plot.setBackgroundPaint(Color.lightGray);
	        this.plot.setDomainGridlinePaint(Color.white);
	        this.plot.setRangeGridlinePaint(Color.white);
//	        this.plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 4, 4, 4, 4));
	        axis = (DateAxis)this.plot.getDomainAxis();	
	        axis.setAutoRange(true);
	        axis.setFixedAutoRange(60000);
	        axis.setDateFormatOverride(new SimpleDateFormat("MM-dd HH:mm:ss"));

	       axis.setTickUnit(new DateTickUnit(DateTickUnitType.HOUR,1));
	       //this needs to be readjusted as the user zooms in
	       	axis.setAutoTickUnitSelection(true);
//	        final DateAxis rangeAxis2 = new DateAxis("Time");
//	        rangeAxis2.setAutoRange(false);
	       	
		contentPane.add(panel);
		
		final ChartPanel chartPanel = new ChartPanel(chart);
		panel.add(chartPanel);
        chartPanel.setPreferredSize(new java.awt.Dimension(404, 181));
        
		JButton btnLogGraph = new JButton("Log Graph");
		btnLogGraph.setBounds(321, 588, 103, 23);
		btnLogGraph.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				db_graph.main();
			}
		});
		contentPane.add(btnLogGraph);
		
		JLabel lblNewLabel = new JLabel("Set Points");
		lblNewLabel.setBounds(10, 267, 106, 33);
		lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 16));
		contentPane.add(lblNewLabel);
		
		JLabel lblBathTemp = new JLabel("Bath Temp");
		lblBathTemp.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblBathTemp.setBounds(270, 68, 70, 15);
		contentPane.add(lblBathTemp);
		
		JLabel lblAmbient = new JLabel("Ambient");
		lblAmbient.setFont(new Font("Dialog", Font.PLAIN, 22));
		lblAmbient.setBounds(10, 214, 96, 33);
		contentPane.add(lblAmbient);
		
		JSeparator separator_2 = new JSeparator();
		separator_2.setBounds(10, 196, 414, 2);
		contentPane.add(separator_2);
		
		JLabel lblBeerTemp = new JLabel("Beer Temp");
		lblBeerTemp.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblBeerTemp.setBounds(180, 68, 77, 15);
		contentPane.add(lblBeerTemp);
		
//		JPanel panel = testBed.getPanel();
//		panel.setBounds(10, 360, 429, 258);
//		contentPane.add(panel);
	}
	
	public JLabel getlblFV1Bath(){
		return lblFV1Bath;
	}
	public JLabel getLblTc1() {
		return lblTc1;
	}
	public JLabel getLabelTc2() {
		return labelTc2;
	}
	public JLabel getLabelTcC() {
		return labelTcC;
	}
	public JLabel getLblFv1chill() {
		return lblFv1chill;
	}
	public JLabel getLabelFv2Chill() {
		return labelFv2Chill;
	}
	public JLabel getLabelChillChill() {
		return labelChillChill;
	}
	public FermVessel getFV(FermVessels i){
		if (i==FermVessels.FV1){
			return FV1;
		}else if(i==FermVessels.FV2){
			return FV2;
		}else{
			return FVC;
		}
	}
}
