package ferm_ctrl;

	import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

	import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

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
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

	public class db_graph extends JFrame implements ActionListener {

		    private XYPlot plot;
		   
		    /** The index of the last dataset added. */
		    private int datasetIndex = 5;
		    public final DateAxis axis;
		    String[] names={"sp1","sp2","spc","FV1","FV2","Coolant"};
		    String[] dbnames={"sp1","sp2","spc","tc1","tc2","tcC"};
		    /**
		     * Constructs a new demonstration application.
		     *
		     * @param title  the frame title.
		     * @throws ParseException 
		     */
		    public db_graph(final String title) {

		        super(title);
		        
		        //next part parses the log file, collects data, puts them into collections
		        XYSeries series[] = new XYSeries[6];
//		        try{
		        	series=dbGetValues();
//		        }
//		        catch(IOException ioe){
//		     	   System.out.println(ioe.getMessage());
//		        }
		        final XYSeriesCollection sp1 = new XYSeriesCollection(series[0]);		        
		        final XYSeriesCollection sp2 = new XYSeriesCollection(series[1]);
		        final XYSeriesCollection spC = new XYSeriesCollection(series[2]);
		        final XYSeriesCollection tc1 = new XYSeriesCollection(series[3]);
		        final XYSeriesCollection tc2 = new XYSeriesCollection(series[4]);
		        final XYSeriesCollection tcC = new XYSeriesCollection(series[5]); 

		        
		        final JFreeChart chart = ChartFactory.createTimeSeriesChart(
		            "Recorded Values", "Time", "Value", sp1
		        );
		        chart.setBackgroundPaint(Color.white);		  
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
		        axis = (DateAxis)this.plot.getDomainAxis();
		        axis.setAutoRange(true);
		        axis.setDateFormatOverride(new SimpleDateFormat("MM-dd HH:mm:ss"));
		        axis.setTickUnit(new DateTickUnit(DateTickUnitType.HOUR,1));
		       	axis.setAutoTickUnitSelection(true);
		        
		        final JPanel content = new JPanel(new BorderLayout());
		        final ChartPanel chartPanel = new ChartPanel(chart);
		        content.add(chartPanel);
		        chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
		        setContentPane(content);
	               chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
		               setContentPane(content);
		    }
	    
		    public void actionPerformed(final ActionEvent e) {
		    }
		    
		    private XYSeries[] dbGetValues() {
		    	//returns an 6 valued array of series gathered from the db file
		    	XYSeries[] locSeries = new XYSeries[6];
		    	//now initialize the locSeries XYSeries entries
		    	for (int i =0;i<6;i++){
		    	locSeries[i]=new XYSeries(names[i]);
		    	}
		    	Connection con = null;
//		    	if (!new File(dbPa.toString()).isFile()){//Check to see if db file exists
//		    	if it doesn't exist, create it
		    	//TODO also code to check size of db file, create new one if too big
//		    	}
		    	try{
		    		Class.forName("org.sqlite.JDBC");
		    		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
			    	Path dbPa = Paths.get(System.getProperty("user.home"),"ferm_ctrl.db");
		    		con = DriverManager.getConnection("jdbc:sqlite:"+dbPa.toString());
		    	    Statement statement = con.createStatement();
		    	    statement.setQueryTimeout(30);  // set timeout to 30 sec.
		    	    ResultSet rs = statement.executeQuery("select * from log");
		    	      while(rs.next())
		    	      {
		    	        // read the result set
		    	    	Date d = sdf.parse(rs.getString("time"));
		    	        long rsTime = d.getTime();
		    	        for (int j=0;j<6;j++){
		    	        	locSeries[j].add(rsTime, rs.getFloat(dbnames[j]));	
		    	        }
		    	      }
		    		
		    	}catch (SQLException sqle){
		    		System.err.println(sqle.getMessage());
		    	}catch (ClassNotFoundException cnfe){
		    		JOptionPane.showMessageDialog(null, "SQLite not found", "Error", JOptionPane.ERROR_MESSAGE);
		    	} catch (ParseException pe) {
					pe.printStackTrace();
				}
		    	finally{
		    		try{
		    			if(con != null)
		    		          con.close();
		    		      }
		    		      catch(SQLException e)
		    		      {
		    		        // connection close failed.
		    		        System.err.println(e);
		    		      }
		    		}
		    	return locSeries;
		    	}
		    	
		    public static void main() {
		        final db_graph graph = new db_graph("Long Term Graph");
		        graph.pack();
		        RefineryUtilities.centerFrameOnScreen(graph);
		        graph.setVisible(true);
		        graph.setDefaultCloseOperation(DISPOSE_ON_CLOSE);		        
		    }

}


