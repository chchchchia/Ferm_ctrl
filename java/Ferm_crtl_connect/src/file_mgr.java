package ferm_ctrl;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import org.jfree.data.xy.XYSeries;

public class file_mgr {
	public static XYSeries[] main() throws IOException {
		XYSeries[] seriesloc = new XYSeries[6];
		int numSet=6; //number of settings
		int setPlace=0; //which value is being stored
		String[] names={"sp1","sp2","spc","tc1","tc2","tcC"};
		for (int i=0;i<numSet;i++){
			seriesloc[i]=new XYSeries("Set:"+names[i]);
		}
        Scanner s = null;
        try {
            s = new Scanner(new BufferedReader(new FileReader("C:\\Users\\Chia\\ferm_log.old.txt")));
            s.useDelimiter("\\s:\\s|\r\n");
            long datelong=0;
            while (s.hasNext()) { //WARNING, this will fuck up if there is a date with no data entry
            	String dateStr = s.next();
            	if (dateStr.charAt(0)!='&'){
//            		SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
            		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
            		System.out.println(dateStr);
            		Date ddd = sdf.parse(dateStr);
            		datelong = ddd.getTime();
            		setPlace=0;
            	}else if (dateStr.charAt(0)=='&'){
            		Scanner k = new Scanner(dateStr).useDelimiter("&");
            		while (k.hasNext()){
//            			System.out.println(k.next());
            			String tempK=k.next();
            			if (tempK.contains("*")){
            				tempK=tempK.substring(0,tempK.length()-1);
            				tempK.trim();
            				seriesloc[setPlace].add(datelong, Double.parseDouble(tempK));
            				setPlace++;            				
            			}else if (!tempK.isEmpty()&&!tempK.contains("/")&&tempK!=null){
            				seriesloc[setPlace].add(datelong, Double.parseDouble(tempK));
            				setPlace++;
            				}
            		//	System.out.println(tempK);
            			}
            		k.close();
            	}
            		
            }
        }catch(ParseException pe){
            	System.out.println(pe.getMessage());
            }
        finally {
            if (s != null) {
                s.close();
            }
        }
		return seriesloc;
    }
}
