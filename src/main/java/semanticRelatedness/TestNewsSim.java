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
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;


public class TestNewsSim {


	static String field = "contents";
	static String indexPath = "/home/chrisschaefer/Documents/lucene-wikipedia05";
	static IndexReader reader;
	static IndexSearcher searcher;
	static Analyzer analyzer;
	static QueryParser parser;
	public static void main(String[] args) throws IOException, ParseException {

		String line;
		double val;
		reader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
		searcher = new IndexSearcher(reader);
		//ESASimilarity esaSimilarity = new ESASimilarity();
		DefaultSimilarity esaSimilarity = new DefaultSimilarity();
		searcher.setSimilarity(esaSimilarity);
		
//		Analyzer analyzer = new WikipediaAnalyzer();
		analyzer = new StandardAnalyzer(Version.LUCENE_4_9);
		parser = new QueryParser(Version.LUCENE_4_9, field, analyzer);

		
		// read Lee's newspaper articles
		FileReader is = new FileReader("/home/chrisschaefer/Documents/gesa/lee.cor");
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
					VectorSpaceCentroid c0 = new VectorSpaceCentroid(indexPath, leesCorpus[i].replaceAll("[^\\w]", " "));
					VectorSpaceCentroid c1 = new VectorSpaceCentroid(indexPath, leesCorpus[j].replaceAll("[^\\w]", " "));
					val = VectorSpaceCentroid.normalizedRelevanceDistance(c0, c1);
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
}
