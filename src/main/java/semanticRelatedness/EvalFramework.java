package semanticRelatedness;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.apache.lucene.queryparser.classic.ParseException;


public class EvalFramework {

	public static void main(String[] args) throws IOException, ParseException {

		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(args[2]),"UTF-8"));
		Statistics stats = new Statistics();
		bw.write(stats.getHeader());
		bw.flush();
	
		for(int i = 100; i <= 100; i*=10)
		
		{
			
		    long start = System.currentTimeMillis();
			
			stats.indexPath = args[0];
			stats.ngramPath = args[1];
			stats.datasetDir = args[3];
			stats.stopWordsPath = "";
			stats.indexPruning = true;
			stats.freqThreshold = 3; // text = text.replaceAll("[^\\w]", " ");
			stats.WINDOW_THRES = 0.005f;
			stats.outlinkThreshold = 0;
			stats.inlinkThreshold = 0;
			stats.numberOfUniqueNonStopwordsThreshold = 0;
			stats.minWordLengthThreshold = 0;
			stats.filterTitle = false;
			stats.filterCategories = false;	
			stats.filterStopWords = true;
			stats.stemmerCalls = 1;
			stats.titleWeight = 1;
			stats.tfidfThreshold = 0;
			stats.anchorText = "NONE";
			stats.limit = i;
						
			//SqliteToLucene.buildIndex(stats);
			SemanticRelatedness.evaluate(stats);
		    stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
			bw.write(stats.getValues());
			bw.flush();
	}
			bw.close();
		}
}

//		{ 
//			
//		    long start = System.currentTimeMillis();
//			
//			stats.indexPath = "/home/chrisschaefer/robustness_evaluation/wikipedia-gab-036";
//			stats.indexPruning = false;
//			stats.freqThreshold = 0; // text = text.replaceAll("[^\\w]", " ");
//			stats.WINDOW_THRES = 0;//0.005f;
//			stats.outlinkThreshold = 0;
//			stats.inlinkThreshold = 0;
//			stats.numberOfUniqueNonStopwordsThreshold = 0;
//			stats.minWordLengthThreshold = 0;
//			stats.filterTitle = false;
//			stats.filterCategories = false;	
//			stats.filterStopWords = false;
//			stats.stemmerCalls = 0;
//			stats.titleWeight = 1;
//			stats.tfidfThreshold = 0;
//			stats.anchorText = "NONE";
//						
//			//GabToLucene.buildIndex(stats);
//			SemanticRelatedness.evaluate(stats);
//		    stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
//			bw.write(stats.getValues());
//			bw.flush();
//		}
//		{ 
//			
//		    long start = System.currentTimeMillis();
//			
//			stats.indexPath = "/home/chrisschaefer/robustness_evaluation/wikipedia-gab-037";
//			stats.indexPruning = false;
//			stats.freqThreshold = 3; // text = text.replaceAll("[^\\w]", " ");
//			stats.WINDOW_THRES = 0;//0.005f;
//			stats.outlinkThreshold = 0;
//			stats.inlinkThreshold = 0;
//			stats.numberOfUniqueNonStopwordsThreshold = 0;
//			stats.minWordLengthThreshold = 0;
//			stats.filterTitle = false;
//			stats.filterCategories = false;	
//			stats.filterStopWords = false;
//			stats.stemmerCalls = 0;
//			stats.titleWeight = 1;
//			stats.tfidfThreshold = 0;
//			stats.anchorText = "NONE";
//						
//			//GabToLucene.buildIndex(stats);
//			SemanticRelatedness.evaluate(stats);
//		    stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
//			bw.write(stats.getValues());
//			bw.flush();
//		}
//{ 
//			
//		    long start = System.currentTimeMillis();
//			
//			stats.indexPath = "/home/chrisschaefer/robustness_evaluation/wikipedia-gab-038";
//			stats.indexPruning = false;
//			stats.freqThreshold = 3; // text = text.replaceAll("[^\\w]", " ");
//			stats.WINDOW_THRES = 0;//0.005f;
//			stats.outlinkThreshold = 0;
//			stats.inlinkThreshold = 5;
//			stats.numberOfUniqueNonStopwordsThreshold = 0;
//			stats.minWordLengthThreshold = 0;
//			stats.filterTitle = false;
//			stats.filterCategories = false;	
//			stats.filterStopWords = false;
//			stats.stemmerCalls = 0;
//			stats.titleWeight = 1;
//			stats.tfidfThreshold = 0;
//			stats.anchorText = "NONE";
//						
//			GabToLucene.buildIndex(stats);
//			SemanticRelatedness.evaluate(stats);
//		    stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
//			bw.write(stats.getValues());
//			bw.flush();
//		}
//{ 
//	
//    long start = System.currentTimeMillis();
//	
//	stats.indexPath = "/home/chrisschaefer/robustness_evaluation/wikipedia-gab-039";
//	stats.indexPruning = false;
//	stats.freqThreshold = 3; // text = text.replaceAll("[^\\w]", " ");
//	stats.WINDOW_THRES = 0;//0.005f;
//	stats.outlinkThreshold = 5;
//	stats.inlinkThreshold = 5;
//	stats.numberOfUniqueNonStopwordsThreshold = 0;
//	stats.minWordLengthThreshold = 0;
//	stats.filterTitle = false;
//	stats.filterCategories = false;	
//	stats.filterStopWords = false;
//	stats.stemmerCalls = 0;
//	stats.titleWeight = 1;
//	stats.tfidfThreshold = 0;
//	stats.anchorText = "NONE";
//				
//	GabToLucene.buildIndex(stats);
//	SemanticRelatedness.evaluate(stats);
//    stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
//	bw.write(stats.getValues());
//	bw.flush();
//}
//{ 
//	
//    long start = System.currentTimeMillis();
//	
//	stats.indexPath = "/home/chrisschaefer/robustness_evaluation/wikipedia-gab-040";
//	stats.indexPruning = false;
//	stats.freqThreshold = 3; // text = text.replaceAll("[^\\w]", " ");
//	stats.WINDOW_THRES = 0;//0.005f;
//	stats.outlinkThreshold = 5;
//	stats.inlinkThreshold = 5;
//	stats.numberOfUniqueNonStopwordsThreshold = 100;
//	stats.minWordLengthThreshold = 0;
//	stats.filterTitle = false;
//	stats.filterCategories = false;	
//	stats.filterStopWords = false;
//	stats.stemmerCalls = 0;
//	stats.titleWeight = 1;
//	stats.tfidfThreshold = 0;
//	stats.anchorText = "NONE";
//				
//	GabToLucene.buildIndex(stats);
//	SemanticRelatedness.evaluate(stats);
//    stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
//	bw.write(stats.getValues());
//	bw.flush();
//}
//{ 
//	
//    long start = System.currentTimeMillis();
//	
//	stats.indexPath = "/home/chrisschaefer/robustness_evaluation/wikipedia-gab-041";
//	stats.indexPruning = false;
//	stats.freqThreshold = 3; // text = text.replaceAll("[^\\w]", " ");
//	stats.WINDOW_THRES = 0;//0.005f;
//	stats.outlinkThreshold = 5;
//	stats.inlinkThreshold = 5;
//	stats.numberOfUniqueNonStopwordsThreshold = 100;
//	stats.minWordLengthThreshold = 0;
//	stats.filterTitle = false;
//	stats.filterCategories = false;	
//	stats.filterStopWords = false;
//	stats.stemmerCalls = 3;
//	stats.titleWeight = 1;
//	stats.tfidfThreshold = 0;
//	stats.anchorText = "NONE";
//				
//	GabToLucene.buildIndex(stats);
//	SemanticRelatedness.evaluate(stats);
//    stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
//	bw.write(stats.getValues());
//	bw.flush();
//}
//{ 
//	
//    long start = System.currentTimeMillis();
//	
//	stats.indexPath = "/home/chrisschaefer/robustness_evaluation/wikipedia-gab-042";
//	stats.indexPruning = true;
//	stats.freqThreshold = 3; // text = text.replaceAll("[^\\w]", " ");
//	stats.WINDOW_THRES = 0.005f;
//	stats.outlinkThreshold = 5;
//	stats.inlinkThreshold = 5;
//	stats.numberOfUniqueNonStopwordsThreshold = 100;
//	stats.minWordLengthThreshold = 0;
//	stats.filterTitle = false;
//	stats.filterCategories = false;	
//	stats.filterStopWords = false;
//	stats.stemmerCalls = 3;
//	stats.titleWeight = 1;
//	stats.tfidfThreshold = 0;
//	stats.anchorText = "NONE";
//				
//	GabToLucene.buildIndex(stats);
//	SemanticRelatedness.evaluate(stats);
//    stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
//	bw.write(stats.getValues());
//	bw.flush();
//}
//{ 
//	
//    long start = System.currentTimeMillis();
//	
//	stats.indexPath = "/home/chrisschaefer/robustness_evaluation/wikipedia-gab-043";
//	stats.indexPruning = true;
//	stats.freqThreshold = 3; // text = text.replaceAll("[^\\w]", " ");
//	stats.WINDOW_THRES = 0.005f;
//	stats.outlinkThreshold = 5;
//	stats.inlinkThreshold = 5;
//	stats.numberOfUniqueNonStopwordsThreshold = 100;
//	stats.minWordLengthThreshold = 0;
//	stats.filterTitle = false;
//	stats.filterCategories = false;	
//	stats.filterStopWords = true;
//	stats.stemmerCalls = 3;
//	stats.titleWeight = 1;
//	stats.tfidfThreshold = 0;
//	stats.anchorText = "NONE";
//				
//	GabToLucene.buildIndex(stats);
//	SemanticRelatedness.evaluate(stats);
//    stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
//	bw.write(stats.getValues());
//	bw.flush();
//}
//{ 
//	
//    long start = System.currentTimeMillis();
//	
//	stats.indexPath = "/home/chrisschaefer/robustness_evaluation/wikipedia-gab-044";
//	stats.indexPruning = true;
//	stats.freqThreshold = 3; // text = text.replaceAll("[^\\w]", " ");
//	stats.WINDOW_THRES = 0.005f;
//	stats.outlinkThreshold = 5;
//	stats.inlinkThreshold = 5;
//	stats.numberOfUniqueNonStopwordsThreshold = 100;
//	stats.minWordLengthThreshold = 0;
//	stats.filterTitle = false;
//	stats.filterCategories = false;	
//	stats.filterStopWords = true;
//	stats.stemmerCalls = 3;
//	stats.titleWeight = 1;
//	stats.tfidfThreshold = 0;
//	stats.anchorText = "ALL";
//				
//	GabToLucene.buildIndex(stats);
//	SemanticRelatedness.evaluate(stats);
//    stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
//	bw.write(stats.getValues());
//	bw.flush();
//}
//{ 
//	
//    long start = System.currentTimeMillis();
//	
//	stats.indexPath = "/home/chrisschaefer/robustness_evaluation/wikipedia-gab-045";
//	stats.indexPruning = true;
//	stats.freqThreshold = 3; // text = text.replaceAll("[^\\w]", " ");
//	stats.WINDOW_THRES = 0.005f;
//	stats.outlinkThreshold = 5;
//	stats.inlinkThreshold = 5;
//	stats.numberOfUniqueNonStopwordsThreshold = 100;
//	stats.minWordLengthThreshold = 0;
//	stats.filterTitle = false;
//	stats.filterCategories = false;	
//	stats.filterStopWords = true;
//	stats.stemmerCalls = 3;
//	stats.titleWeight = 4;
//	stats.tfidfThreshold = 0;
//	stats.anchorText = "ALL";
//				
//	GabToLucene.buildIndex(stats);
//	SemanticRelatedness.evaluate(stats);
//    stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
//	bw.write(stats.getValues());
//	bw.flush();
//}
//{ 
//	
//    long start = System.currentTimeMillis();
//	
//	stats.indexPath = "/home/chrisschaefer/robustness_evaluation/wikipedia-gab-045";
//	stats.indexPruning = true;
//	stats.freqThreshold = 3; // text = text.replaceAll("[^\\w]", " ");
//	stats.WINDOW_THRES = 0.005f;
//	stats.outlinkThreshold = 5;
//	stats.inlinkThreshold = 5;
//	stats.numberOfUniqueNonStopwordsThreshold = 100;
//	stats.minWordLengthThreshold = 0;
//	stats.filterTitle = false;
//	stats.filterCategories = false;	
//	stats.filterStopWords = true;
//	stats.stemmerCalls = 0;
//	stats.titleWeight = 4;
//	stats.tfidfThreshold = 0;
//	stats.anchorText = "ALL";
//				
//	GabToLucene.buildIndex(stats);
//	SemanticRelatedness.evaluate(stats);
//    stats.runtimeInHours = (System.currentTimeMillis() - start)/3600000.0;
//	bw.write(stats.getValues());
//	bw.flush();
//}
