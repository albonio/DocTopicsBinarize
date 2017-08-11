import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
 
 
 
 
public class DocTopicsBinarize {
	static String outputDir = "output";    
	static String inputDir  = "input";	

	//static String fileName  = "FTM_20170714_100.doc_topics";
	static String fileName  = "propuestas170728_100.doc_topics";
	static boolean complete_topic_format = true;
	
	static int[] emptyTopic = {};
	
	
	
    
    private static DecimalFormat df4;
    private static float[][] docTopicValues;    
    private static String[] docNames;
    private static int numdocs = 0;
    private static int numtopics = 0;




    public static void main(String[] args) throws ParseException {
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
        simbolos.setDecimalSeparator('.');    
        df4 = new DecimalFormat("#.####", simbolos);
        
        
        // parse CLI options
        Options options = createCLIoptions();
        
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse( options, args);
        
        
        // outputDir, get -o option value
        String outputDircli = cmd.getOptionValue("o");
        if(outputDircli == null) {
        	System.out.println("No output dir specified. Default: " + outputDir);
        } else {
        	System.out.println("Output dir specified: " + outputDircli);
        	outputDir = outputDircli;
        }
        
        // outputDir, get -o option value
        String inputDircli = cmd.getOptionValue("i");
        if(inputDircli == null) {
        	 System.out.println("No output dir specified. Default: " + outputDir);
        } else {
        	System.out.println("Output dir specified: " + inputDircli);
        	inputDir = inputDircli;
        }
        
        // doc_topics, get -d option value
        String docTopicscli = cmd.getOptionValue("d");
        if(docTopicscli == null) {
        	 System.out.println("No DocTopics file specified.");
        	 usage(options);
        	 System.exit(1);
        } else {
        	System.out.println("DocTopics file specified: " + docTopicscli);
        	fileName = docTopicscli;
        }
        
        // doc_topics, get -d option value
        String no_complete_topic_format_cli = cmd.getOptionValue("nc");
        if(no_complete_topic_format_cli != null) {
        	System.out.println("Specified no complete topic format.");
     		complete_topic_format = false;
        } 
        
             
        try {
        	// inspect doc topic file
        	if(complete_topic_format){
        		inspectTopicFile_CompleteFormat(fileName);
        	} else {
        		inspectTopicFile_NonCompleteFormat(fileName);
        	}
            
            
            if(numdocs == 0){
            	System.out.println("Error loading doc-topics: incorrect numdocs...");
            	return;            	
            }
            
            if(numtopics == 0){
            	System.out.println("Error loading doc-topics: incorrect numtopics...");
            	return;            	
            }
            
            docTopicValues = new float[numdocs][numtopics];
            docNames = new String[numdocs];  
            
            System.out.println("\nLoading doc-topics, numdocs: " + numdocs + ", numtopics: " + numtopics);

            // load topics
        	if(complete_topic_format){
                if(loadTopics_CompleteFormat(fileName, numdocs) == 0){
                	System.out.println("Error loading doc-topics...");
                	return;
                }           		
        	} else {
                if(loadTopics_NonCompleteFormat(fileName, numdocs) == 0){
                	System.out.println("Error loading doc-topics...");
                	return;
                }          		
        	}
      
            
            float[][] docTopicValuesCleanned = cleanZeros(docTopicValues);
            saveCleanedDocTopics(docTopicValuesCleanned, docNames);
            
            
            System.out.println("Bin topic file saved");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	private static Options createCLIoptions() {
		//-o output -i input -d cordis-projects_100.doc_topics
		
		// create Options object
		Options options = new Options();

		// add options
		options.addOption("o", true, "output directory");
		options.addOption("i", true, "input directory");
		options.addOption("d", true, "doctopics file");
		options.addOption("nc", false, "non compressed doctopics file format (default include only non null doc topic values).");

		return options;
	}

	private static void usage(Options options) {
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp( "DocTopicsBinarize", options );
	}

	private static void saveCleanedDocTopics(float[][] docTopicValuesCleanned, String[] docNames) {
		try {
	        FileOutputStream outputStream = new FileOutputStream(new File(outputDir + File.separator + fileName + ".bin"));
			
	        for(int i = 0; i < numdocs; i++){
	        	byte[] dataBin = getBinVector(docTopicValuesCleanned[i], docNames[i]);
	        	outputStream.write(dataBin, 0, dataBin.length);
	        }
			outputStream.flush();
			outputStream.close();	
			
		} catch (IOException e) {
            System.err.println("Error writing topic distance file: ");
			e.printStackTrace();
		} 	       
		
	}

	private static byte[] getBinVector(float[] docTopicValues, String docName) {		
		// num non zero topics
		short num_non_zero_topics = 0;
		for(int i = 0; i < numtopics; i++){
			if(docTopicValues[i]>0){
				num_non_zero_topics++;
			}
		}
		
		int cnt_non_zero_topics = 0;
		byte[] bytesDocTopicValues = new byte[num_non_zero_topics*4];
		for(int i = 0; i < numtopics; i++){
					
			if(docTopicValues[i] > 0){
				short mappedval = (short)(docTopicValues[i]*10000);
				short mappedpos = (short)i;
					
				// topic position
				bytesDocTopicValues[cnt_non_zero_topics*4]     = (byte)(mappedpos & 0xff);
				bytesDocTopicValues[cnt_non_zero_topics*4 + 1] = (byte)((mappedpos >> 8) & 0xff);
				// topic value
				bytesDocTopicValues[cnt_non_zero_topics*4 + 2] = (byte)(mappedval & 0xff);
				bytesDocTopicValues[cnt_non_zero_topics*4 + 3] = (byte)((mappedval >> 8) & 0xff);
				cnt_non_zero_topics++;
			}
		}
		// sizeOf(short) = 2, sizeOf(topic number) = sizeOf(short), sizeOf(topic value) = sizeOf(short)
		//  start, number of active topics		
		byte[] docNameBytes = docName.getBytes();
		
		
		// active topics
		short offset = 4;		

		// reg size
		short reg_size = (short) (docNameBytes.length + num_non_zero_topics*4 + offset);	
		byte[] bytes = new byte[reg_size];

		bytes[0] = (byte)(reg_size & 0xff);
		bytes[1] = (byte)((reg_size >> 8) & 0xff);		
		
		// num non zero topics
		bytes[2] = (byte)(num_non_zero_topics & 0xff);
		bytes[3] = (byte)((num_non_zero_topics >> 8) & 0xff);
		
		for(int i=0; i < num_non_zero_topics; i++){			
			// topic position
			bytes[i*4 + offset] = bytesDocTopicValues[i*4];
			bytes[i*4 + offset + 1] = bytesDocTopicValues[i*4 + 1];
			// topic value
			bytes[i*4 + offset + 2] = bytesDocTopicValues[i*4 + 2];
			bytes[i*4 + offset + 3] = bytesDocTopicValues[i*4 + 3];
		}
		
		// doc name/id
		for(int i=0; i < docNameBytes.length; i++){
			bytes[i + num_non_zero_topics*4 + offset] = docNameBytes[i];
		}
		return bytes;
	}

	private static float[][] cleanZeros(float[][] docTopicValues) {
		float[][] docTopicValuesCleanned =  new float[numdocs][numtopics];
		
		for(int i=0; i < numdocs; i++){
			docTopicValuesCleanned[i] = cleanZerosDocTopicVector(docTopicValues[i]);
		}
		return docTopicValuesCleanned;
	}

	private static float[] cleanZerosDocTopicVector(float[] docTopicValues) {
		float[] docTopicVector = new float[numtopics];
		
		// find zero
		float min = 1f;
		for(int i=0; i < numtopics; i++){
			if(docTopicValues[i] < min){
				min = docTopicValues[i];
			}
		}
		
		if(min > 0.01d || min == 0d){
			return docTopicValues;
		}
		
		// clean zero and get rest
		int num_zeros = 0;
		for(int i=0; i < numtopics; i++){
			if(docTopicValues[i] > min && !isEmptyTopic(i)){
				docTopicVector[i] = docTopicValues[i];
			} else {
				docTopicVector[i] = 0f;
				num_zeros++;
			}
		}

		// complete rest
		float rest = 1;
		for(int i=0; i < numtopics; i++){
			rest-=docTopicVector[i];
		}		
		rest = rest/(float)(numtopics - num_zeros);
		for(int i=0; i < numtopics; i++){
			if(docTopicValues[i] > min && !isEmptyTopic(i)){
				docTopicVector[i]+=rest;
			}
		}		
		
		return docTopicVector;
	}

	private static boolean isEmptyTopic(int topicNumber) {
		for(int i=0; i< emptyTopic.length; i++){
			if(emptyTopic[i]==topicNumber){
				return true;
			}
		}
		return false;
	}

	private static int getDocId(String docName) {
		for(int i=0; i < numdocs; i++){
			if(docNames[i].equalsIgnoreCase(docName)){
				return i;
			}
		}	
		return -1;
	}

	private static int loadTopics_NonCompleteFormat(String fileName, int numdocs) throws UnsupportedEncodingException, IOException {
        FileInputStream inputStream = new FileInputStream(new File(inputDir + File.separator + fileName));
    	BufferedReader stdInReader = new BufferedReader(new InputStreamReader(inputStream,"UTF-8"));
 
        int cnt = 0;
         
        try {
            String line;
 
            DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
            simbolos.setDecimalSeparator('.');                         
            
            while ((line = stdInReader.readLine()) != null) {
                if(cnt > 0){
	            	String[] result = line.split("\\t");
                	docNames[cnt-1] = result[1].replace("\"", "").replace("/","-").toUpperCase().trim();
	                
	                for (int i=0; i<result.length; i++){ 
	                    if(i%2 == 1 && i > 1){
	                        int pos = Integer.parseInt(result[i-1]);
	                        docTopicValues[cnt-1][pos] = Float.valueOf(result[i]).floatValue();
	                    }                       
	                }
                }
	            cnt++;
            }
            return cnt;
            
        } catch (Exception e) {
            System.err.println("Error reading topic file: ");
            e.printStackTrace();
        } finally{
        	stdInReader.close();
        	inputStream.close();
        }
        return 0;
    }
     
	private static int loadTopics_CompleteFormat(String fileName, int numdocs) throws UnsupportedEncodingException, IOException {
        FileInputStream inputStream = new FileInputStream(new File(inputDir + File.separator + fileName));
    	BufferedReader stdInReader = new BufferedReader(new InputStreamReader(inputStream,"UTF-8"));
 
        int cnt = 0;
         
        try {
            String line;
 
            DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
            simbolos.setDecimalSeparator('.');                         
            
            while ((line = stdInReader.readLine()) != null) {
            	String[] result = line.split("\\t");
            	docNames[cnt] = result[1].replace("\"", "").replace("/","-").toUpperCase().trim();
                
                for (int i=0; i<result.length-2; i++){ 
                    docTopicValues[cnt][i] = Float.valueOf(result[i+2]).floatValue();                    
	            }
	            cnt++;
            }
            return cnt;
            
        } catch (Exception e) {
            System.err.println("Error reading topic file: ");
            e.printStackTrace();
        } finally{
        	stdInReader.close();
        	inputStream.close();
        }
        return 0;
    }
	
    private static int inspectTopicFile_NonCompleteFormat(String fileName) throws IOException {
        InputStream inputStream = new FileInputStream(new File(inputDir + File.separator + fileName));
    	BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream,"UTF-8"));
    	int lines = 0;
    	String line;
    	
    	int maxtopic = 0;
        while ((line = reader.readLine()) != null) {
        	if(lines%1000==0){
        		System.out.print(".");
        	}
        	if(lines%50000==0){
        		System.out.print("\n");
        	}
        	
        	if(lines > 0){
	        	String[] result = line.split("\\t");
	            
	            for (int i=0; i<result.length; i++){ 
	                if(i%2 == 1 && i > 1){
	                    int pos = Integer.parseInt(result[i-1]);
	                    if(pos > maxtopic){
	                    	maxtopic = pos;
	                    }
	                }                       
	            }
        	}
    		lines++;
        }	    	
    	reader.close();    	
        inputStream.close();

        numdocs = lines-1;
        numtopics = maxtopic+1;
        
    	return numdocs;
	}

    private static int inspectTopicFile_CompleteFormat(String fileName) throws IOException {
        InputStream inputStream = new FileInputStream(new File(inputDir + File.separator + fileName));
    	BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream,"UTF-8"));
    	int lines = 0;
    	String line;
    	
    	int maxtopic = 0;
        while ((line = reader.readLine()) != null) {
        	if(lines%1000==0 && lines > 0){
        		System.out.print(".");
        	}
        	if(lines%50000==0){
        		System.out.print("\n");
        	}
        	
        	if(lines == 1){
	        	String[] result = line.split("\\t");
	            maxtopic = result.length-2;
	            
        	}
    		lines++;
        }	    	
    	reader.close();    	
        inputStream.close();

        numdocs = lines;
        numtopics = maxtopic;
        
    	return numdocs;
	}    
    public static void deleteFolder(File folder, boolean recursive) {
        File[] files = folder.listFiles();
        if(files!=null) { //some JVMs return null for empty dirs
            for(File f: files) {
            	if(recursive){
	                if(f.isDirectory()) {
	                    deleteFolder(f, recursive);
	                } else {
	                    f.delete();
	                }
            	} else {
            		deleteFolder(f,recursive);
            	}
            }
        }
        folder.delete();
    }    
}