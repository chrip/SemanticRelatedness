package semanticRelatedness;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class VectorSpaceCentroid {
	static QueryParser parser;
	static IndexReader reader;
	static IndexSearcher searcher;
	static String field = "contents";
	static DefaultSimilarity similarity;
	HashMap<String, Integer> parsedTokensCount;
	HashMap<String, Query> parsedQueries;
	HashMap<Integer, Double> centroid;
	
	
	VectorSpaceCentroid(String luceneIndexPath, String text) throws IOException, ParseException {
		reader = DirectoryReader.open(FSDirectory.open(new File(luceneIndexPath)));
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_4_9);
		parser = new QueryParser(Version.LUCENE_4_9, field, analyzer);
		similarity = new DefaultSimilarity();
		searcher = new IndexSearcher(reader);
		searcher.setSimilarity(similarity);
		parsedTokensCount  = new HashMap<String, Integer>();
		parsedQueries = new HashMap<String, Query>();
		centroid = new HashMap<Integer, Double>();
		
		String[] tokens = text.split("\\s+");
		
		for(String token: tokens) {
			Query query = parser.parse(token);
			String termParsed = query.toString(field);
			if (termParsed.equals("")) {
				continue;
			}
			if(parsedTokensCount.containsKey(termParsed)){
				parsedTokensCount.put(termParsed, parsedTokensCount.get(termParsed)+1);
			}
			else {
				parsedTokensCount.put(termParsed, 1);
				parsedQueries.put(termParsed, query);
			}
		}
		
		int totalDocs = reader.numDocs();
		double tfidfSum = 0;
		for (String parsedToken : parsedTokensCount.keySet()) {
			int termFreq = parsedTokensCount.get(parsedToken);
			Term term = new Term(field, parsedToken);
			int docFreq = reader.docFreq(term);
			double idf = similarity.idf(docFreq, totalDocs);
			double tfidf = similarity.tf(termFreq) * idf;
			
			TopDocs results =searcher.search(parsedQueries.get(parsedToken), totalDocs);
			
			if(results.totalHits < 1) {
				continue;
			}
			for(ScoreDoc doc : results.scoreDocs){
				if(centroid.containsKey(doc.doc)){
					centroid.put(doc.doc, centroid.get(doc.doc)+ (tfidf * doc.score));
				}
				else {
					centroid.put(doc.doc, tfidf * doc.score);
				}
			}
			tfidfSum += tfidf;
		}
		for (Map.Entry<Integer, Double> entry : centroid.entrySet()) {
			entry.setValue(entry.getValue()/tfidfSum);
	    }
	}
}
