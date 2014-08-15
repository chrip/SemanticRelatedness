package semanticRelatedness;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
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
		//ESASimilarity similarity = new ESASimilarity();
		DefaultSimilarity similarity = new DefaultSimilarity();
		searcher.setSimilarity(similarity);
		
//		Analyzer analyzer = new WikipediaAnalyzer();
		analyzer = new StandardAnalyzer(Version.LUCENE_4_9);
		parser = new QueryParser(Version.LUCENE_4_9, field, analyzer);

		FileReader is = new FileReader("/home/chrisschaefer/Documents/gesa/LeeSimOriginal.txt");
		BufferedReader br = new BufferedReader(is);
		double[] expectedResults = new double[1275];
		int i = 0, index = 0;
		for(i=0; i < 50; i++) {
			line = br.readLine();
			String[] tokens = line.split("\t");
			for(int j = 0; j< 50; j++) {
				if(i <= j) {
					expectedResults[index] = Double.parseDouble(tokens[j]);
					index++;
				}
			}
		}
		br.close();
		
		// read Lee's newspaper articles
		is = new FileReader("/home/chrisschaefer/Documents/gesa/lee.cor");
		br = new BufferedReader(is);
		VectorSpaceCentroid[] leesCorpus = new VectorSpaceCentroid[50];
		i = 0;
		index = 0;
		while((line = br.readLine()) != null){
			leesCorpus[i] = new VectorSpaceCentroid(reader, searcher, similarity, parser, line);
			System.out.print(i);
			i++;
		}
		br.close();
		System.out.println();
		double[] result = new double[1275];
		for(i=0; i < 50; i++) {
			for(int j = 0; j< 50; j++) {
				if(i > j) {
					val = 0.0;
				}
				else if (i == j) {
					val = 1.0;
					result[index] = val;
					index++;
				}
				else {
					val = VectorSpaceCentroid.normalizedRelevanceDistance(leesCorpus[i], leesCorpus[j]);
					result[index] = val;
					index++;
				}
				System.out.print(val + "\t");
			}
			System.out.print("\n");
		}
		PearsonsCorrelation pc = new PearsonsCorrelation();
		System.out.println(pc.correlation(expectedResults, result));
	}
}
