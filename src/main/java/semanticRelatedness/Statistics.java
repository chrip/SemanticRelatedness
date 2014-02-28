package semanticRelatedness;

import java.io.File;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;


public class Statistics {
	int inlinkThreshold = 0;
	int outlinkThreshold = 0;
	int numberOfUniqueNonStopwordsThreshold = 0;
	int minWordLengthThreshold = 0;
	int titleWeight = 4;
	int limit = 0;
	boolean filterTitle = false;
	boolean filterCategories = false;
	boolean filterStopWords = false;
	String indexPath = "/home/";
	String ngramPath = "/home/";
	String stopWordsPath = "/home/";
	String datasetDir = "/data/glm/sim/";
	String anchorText = "NONE"; //, UNIQUE, ALL };
	double runtimeInHours = 0;
	int numberOfDocs = 0;

	String similarity = "ESA"; //, DEFAULT_LUCENE }
	int stemmerCalls = 0;

	boolean indexPruning = false;
	double tfidfThreshold = 0;
	int freqThreshold = 0;
	int WINDOW_SIZE = 100;
	double WINDOW_THRES = 0.005f;
	
	String[] algorithms = {"ESA", "NWD", "NRD" /*, "2gram", "3gram", "5gram"*/ };
	String[] datasets = { /*"mc", "rg",*/ "wordsim" /*, "bless", "sn"*/};
	HashMap<String, Double> correlation;
	
	DecimalFormat myFormatter;
	
	Statistics () {
		correlation = new HashMap<String, Double>();
		myFormatter = new DecimalFormat("###.#####");
		DecimalFormatSymbols df = new DecimalFormatSymbols();
		df.setDecimalSeparator('.');
		myFormatter.setDecimalFormatSymbols(df);
	}
	
	public String getOutputPath(String algorithmName, String datasetName) {
		File theDir = new File(indexPath + "/" + algorithmName);
		if (!theDir.exists()) {
		   theDir.mkdir();
		}
		return indexPath + "/" + algorithmName + "/" + datasetName + "-frame.csv";
	}

	public String getInputPath(String datasetName) {
		return datasetDir + datasetName + ".csv";
	}
	
	public void setSpearmansCorrelation(double co, String datasetName, String algotithm) {
		correlation.put(algotithm+datasetName+"Spearmans", co);
	}

	public void setPearsonsCorrelation(double co, String datasetName, String algotithm) {
		correlation.put(algotithm+datasetName+"Pearsons", co);
	}
	
	public String getHeader() {
		StringBuilder result = new StringBuilder();

		//determine fields declared in this class only (no fields of superclass)
		Field[] fields = this.getClass().getDeclaredFields();

		boolean isFirstLine = true;
		//print field names

		for ( Field field : fields  ) {
			result.append(isFirstLine?"":"\t");
			isFirstLine = false;
			if(field.getName().equals("algorithms") 
					|| field.getName().equals("datasets") 
					|| field.getName().equals("correlation")
					|| field.getName().equals("myFormatter")) {
				if (field.getName().equals("algorithms")) {
					for (String a : algorithms) {
						for(String ds: datasets) {
							if(ds.equals("mc") || ds.equals("rg") || ds.equals("wordsim")) {
								result.append( ds+"_"+a+"_Spearmans" + "\t");
//								result.append( ds+"_"+a+"_Pearsons" + "\t");
							}
						}
					}
				}
				else {
					continue;
				}
			}
			else {
				result.append( field.getName() );
			}
			
		}
		return result.toString() + "\n";
	}

	public String getValues() {
		StringBuilder result = new StringBuilder();

		//determine fields declared in this class only (no fields of superclass)
		Field[] fields = this.getClass().getDeclaredFields();

		boolean isFirstLine = true;
		//print field values
		for ( Field field : fields  ) {
			try {
				result.append(isFirstLine?"":"\t");
				isFirstLine = false;
				//requires access to private field:
				if(field.getName().equals("algorithms") 
						|| field.getName().equals("datasets") 
						|| field.getName().equals("correlation")
						|| field.getName().equals("myFormatter")) {
					if (field.getName().equals("algorithms")) {
						for (String a : algorithms) {
							for(String ds: datasets) {
								if(ds.equals("mc") || ds.equals("rg") || ds.equals("wordsim")) {
									try {
										result.append( myFormatter.format(correlation.get(a+ds+"Spearmans")) + "\t");
										//result.append( myFormatter.format(correlation.get(a+ds+"Pearsons")) + "\t");
										}
										catch (Exception e) {
											System.out.println(a+ " "+ds +" "+ correlation.get(a+ds+"Spearmans"));
											//System.out.println(a+ " "+ds +" "+ correlation.get(a+ds+"Pearsons"));
											System.out.println(e);								
										}
								}
							}
						}
					}
					else {
						continue;
					}
				}
				else {
					result.append( field.get(this) );
				}
				
				
			} catch ( IllegalAccessException ex ) {
				System.out.println(ex);
			}
		}

		return result.toString() + "\n";
	}

	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		Statistics s = new Statistics();
		System.out.println(s.getHeader());
		System.out.println(s.getValues());
		
		System.out.println(System.currentTimeMillis() - start);
	}

}
