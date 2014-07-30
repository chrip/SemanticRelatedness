package semanticRelatedness;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class VectorSpaceCentroid {
	QueryParser parser;
	IndexReader reader;
	IndexSearcher searcher;
	String field = "contents";
	DefaultSimilarity similarity;
	
	HashMap<Integer, Double> centroid; // docId and tfidf 
	double maxScore;
	int totalDocs;
	
	public HashMap<Integer, Double> getCentroid() {
		return centroid;
	}
	public double getMaxScore() {
		return maxScore;
	}
	public double numDocs() {
		return totalDocs;
	}

	VectorSpaceCentroid(String luceneIndexPath, String text) throws IOException, ParseException {
		reader = DirectoryReader.open(FSDirectory.open(new File(luceneIndexPath)));
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_4_9);
		parser = new QueryParser(Version.LUCENE_4_9, field, analyzer);
		similarity = new DefaultSimilarity();
		searcher = new IndexSearcher(reader);
		searcher.setSimilarity(similarity);
				
		buildCentroid(text);
	}

	public VectorSpaceCentroid(IndexReader reader2, IndexSearcher searcher2,
			DefaultSimilarity similarity2, QueryParser parser2, String text) throws ParseException, IOException {
		reader = reader2;
		parser = parser2;
		similarity = similarity2;
		searcher = searcher2;
		
		buildCentroid(text);
	}

	public void buildCentroid(String text) throws ParseException, IOException {
		HashMap<String, Integer> parsedTokensCount  = new HashMap<String, Integer>();
		centroid = new HashMap<Integer, Double>();
		maxScore = 0;
		
		
		TokenStream ts = parser.getAnalyzer().tokenStream(field, new StringReader(text));
		CharTermAttribute termAttr = ts.addAttribute(CharTermAttribute.class);
        ts.reset();

        int numTerms = 0;
        while (ts.incrementToken()) {
            String token = termAttr.toString();
            if(parsedTokensCount.containsKey(token)){
				parsedTokensCount.put(token, parsedTokensCount.get(token)+1);
			}
			else {
				parsedTokensCount.put(token, 1);
			}
			numTerms++;
        }
        ts.end();
        ts.close();
		
		totalDocs = reader.numDocs();
		double tfidfSum = 0;
		for (String parsedToken : parsedTokensCount.keySet()) {
			int termFreq = parsedTokensCount.get(parsedToken);
			Term term = new Term(field, parsedToken);
			int docFreq = reader.docFreq(term);
			double idf = similarity.idf(docFreq, totalDocs);
			double tfidf = (similarity.tf(termFreq) * Math.pow(idf, 2));
			TopDocs results =searcher.search(new TermQuery(term, docFreq), totalDocs);
			
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
			double score = entry.getValue()/tfidfSum;
			entry.setValue(score);
			maxScore = Math.max(score, maxScore);
	    }
	}

	private static double sumCommonScores(VectorSpaceCentroid centroid0, VectorSpaceCentroid centroid1){
		double sum = 0;
		double maxScore0 = centroid0.getMaxScore();
		double maxScore1 = centroid1.getMaxScore();

		for (final Map.Entry<Integer, Double> entry : centroid0.getCentroid().entrySet()) {
			int doc0 = entry.getKey();

			if (centroid1.getCentroid().containsKey(doc0)) {
				double score0 = entry.getValue();
				double score1 = centroid1.getCentroid().get(doc0);
				double sim1AND2 = (score0/maxScore0)  * (score1/maxScore1);
				sum += sim1AND2;				 
			}			
		}
		return sum;
	}
	
	public static double normalizedRelevanceDistance(VectorSpaceCentroid centroid0, VectorSpaceCentroid centroid1){
		double log0, log1 , logCommon, maxlog, minlog;

		log0 = Math.log(sumScores(centroid0));
		log1 = Math.log(sumScores(centroid1));
        double commonScore = sumCommonScores(centroid0, centroid1);
		if(commonScore == 0) {
			return 0.5;
		}
		logCommon = Math.log(commonScore);
		maxlog = Math.max(log0, log1);
		minlog = Math.min(log0, log1);
    
		return  Math.exp(-2*  (maxlog - logCommon) / (Math.log(centroid0.numDocs()) - minlog));
	}
	
	private static double sumScores(VectorSpaceCentroid centroid) {
		double sum = 0.0; 
		double maxScore = centroid.getMaxScore();

		for (double score : centroid.getCentroid().values()) {
			sum += (score/maxScore);
		}
		return sum;
	}
}
