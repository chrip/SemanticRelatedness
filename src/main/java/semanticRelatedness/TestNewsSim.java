package semanticRelatedness;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;


public class TestNewsSim {


	static String field = "contents";
	static String indexPath = "/home/chrisschaefer/Downloads/wikipedia-051105-preprocessed";
	static IndexReader reader;
	static IndexSearcher searcher;
	static Analyzer analyzer;
	static QueryParser parser;
	public static void main(String[] args) throws IOException, ParseException {

		String line;
		double val;
		reader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
		searcher = new IndexSearcher(reader);
		ESASimilarity esaSimilarity = new ESASimilarity();
		searcher.setSimilarity(esaSimilarity);
		
//		Analyzer analyzer = new WikipediaAnalyzer();
		analyzer = new StandardAnalyzer(Version.LUCENE_43);
		parser = new QueryParser(Version.LUCENE_43, field, analyzer);

		
		// read Lee's newspaper articles
		FileReader is = new FileReader("/home/chrisschaefer/Dokumente/gesa/lee.cor");
		BufferedReader br = new BufferedReader(is);
		String[] leesCorpus = new String[50];
		int i = 0;
		while((line = br.readLine()) != null){
			leesCorpus[i] = line;
			//System.out.println(leesCorpus[i]);
			i++;
		}
		br.close();
		double[][] result = new double[50][50];
		for(i=0; i < 50; i++) {
			for(int j = 0; j< 50; j++) {
				if(i > j) {
					result[i][j] = 0.0;
				}
				else if (i == j) {
					result[i][j] = 1.0;
				}
				else {
					val = 1 - getRelatedness(leesCorpus[i].replace("/", "\\/"), leesCorpus[j].replace("/", "\\/"));
					if (val == -1) {
						val = 0;
					}
					result[i][j] = val;
				}
				System.out.print(result[i][j] + "\t");
			}
			System.out.print("\n");
		}		
	}
	public static double getRelatedness(String term0, String term1) throws ParseException, IOException {

		Query query0 = parser.parse(term0);
		Query query1 = parser.parse(term1);
		
		Sort sort0 = new Sort(SortField.FIELD_DOC); 
		TopFieldCollector tfc0 = TopFieldCollector.create(sort0, reader.numDocs(), true, true, true, true); 
		Sort sort1 = new Sort(SortField.FIELD_DOC); 
		TopFieldCollector tfc1 = TopFieldCollector.create(sort1, reader.numDocs(), true, true, true, true); 
		
		searcher.search(query0, tfc0);
		TopDocs results0 = tfc0.topDocs();
		searcher.search(query1, tfc1);
		TopDocs results1 = tfc1.topDocs();

		return cosine(results0, results1);
	}
	
	private static double cosine(TopDocs results0, TopDocs results1){
		double scalar = 0.0d, r0Norm=0.0d, r1Norm=0.0d, tfidfThreshold=0;

		int i = 0, j = 0;

		double maxScore0 = results0.getMaxScore();
		double maxScore1 = results1.getMaxScore();
		while (i < results0.scoreDocs.length && j < results1.scoreDocs.length) {
			double score0 = results0.scoreDocs[i].score/maxScore0;
			double score1 = results1.scoreDocs[j].score/maxScore1;

			if(score0 < tfidfThreshold) {
				score0 = 0.0;	        	
			}
			if(score1 < tfidfThreshold) {
				score1 = 0.0;
			}
			if (results0.scoreDocs[i].doc < results1.scoreDocs[j].doc) {
				r0Norm += Math.pow(score0, 2);
				i++;
			}
			else if (results0.scoreDocs[i].doc == results1.scoreDocs[j].doc) {
				scalar += results0.scoreDocs[i].score * results1.scoreDocs[j].score;
				r0Norm += Math.pow(score0, 2);
				r1Norm += Math.pow(score1, 2);
				i++;
				j++;
			}
			else {
				r1Norm += Math.pow(score1, 2);
				j++;
			}
		}
		while (i < results0.scoreDocs.length) {
			double score0 = results0.scoreDocs[i].score/maxScore0;
			if(score0 < tfidfThreshold) {
				score0 = 0.0;	        	
			}
			r0Norm += Math.pow(score0, 2);
			i++;

		}
		while (j < results1.scoreDocs.length) {
			double score1 = results1.scoreDocs[j].score/maxScore1;
			if(score1 < tfidfThreshold) {
				score1 = 0.0;
			}
			r1Norm += Math.pow(score1, 2);
			j++;
		}
		r0Norm=Math.sqrt(r0Norm);
		r1Norm=Math.sqrt(r1Norm);
		if(r0Norm == 0 || r1Norm == 0){
			return 0;
		}
		return scalar / (r0Norm * r1Norm);
	}

}
