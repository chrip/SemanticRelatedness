package semanticRelatedness;

import java.io.BufferedWriter;
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
	
	double[] centroid;
	int totalDocs;
	double scoreSumLog;
	
	public double[] getCentroid() {
		return centroid;
	}
	public double numDocs() {
		return totalDocs;
	}
	public double getScoreSumLog() {
		return scoreSumLog;
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
	
	VectorSpaceCentroid(double[] centroid){
		totalDocs = centroid.length;
		this.centroid = centroid;
	}

	public void buildCentroid(String text) throws ParseException, IOException {
		HashMap<String, Integer> parsedTokensCount  = new HashMap<String, Integer>();
		
		double maxScore = 0;
		
		
		TokenStream ts = parser.getAnalyzer().tokenStream(field, new StringReader(text));
		CharTermAttribute termAttr = ts.addAttribute(CharTermAttribute.class);
        ts.reset();

        while (ts.incrementToken()) {
            String token = termAttr.toString();
            if(parsedTokensCount.containsKey(token)){
				parsedTokensCount.put(token, parsedTokensCount.get(token)+1);
			}
			else {
				parsedTokensCount.put(token, 1);
			}
        }
        ts.end();
        ts.close();
		
		totalDocs = reader.numDocs();
		HashMap<Integer, Double> centroidMap = new HashMap<Integer, Double>();
		for (String parsedToken : parsedTokensCount.keySet()) {
			int termFreq = parsedTokensCount.get(parsedToken);
			Term term = new Term(field, parsedToken);
			int docFreq = reader.docFreq(term);
                        if(docFreq==0){
                            continue;
                        }
			double idf = similarity.idf(docFreq, totalDocs);
			double tfidf = (similarity.tf(termFreq) * Math.pow(idf, 2));
			TopDocs results = searcher.search(new TermQuery(term, docFreq), docFreq);
			
			if(results.totalHits < 1) {
				continue;
			}
			double score = 0;
			for(ScoreDoc doc : results.scoreDocs){
				if(centroidMap.containsKey(doc.doc)){
					score = centroidMap.get(doc.doc)+ (tfidf * doc.score);
					centroidMap.put(doc.doc, score);
				}
				else {
					score = tfidf * doc.score;
					centroidMap.put(doc.doc, score);
				}
			}
			maxScore = Math.max(score, maxScore);
		}
		double sum = 0.0; 
		for (Map.Entry<Integer, Double> entry : centroidMap.entrySet()) {
			double score = entry.getValue()/maxScore;
			sum += score;
			entry.setValue(score);				
	    }
		scoreSumLog = Math.log(sum);
		
		centroid = new double[totalDocs];
		for (int i = 0; i < totalDocs; i++) {
		   if(centroidMap.containsKey(i)){
			   centroid[i] = centroidMap.get(i);
		   }
		   else {
			   centroid[i] = 0;
		   }
		}
	}

	private static double sumCommonScores(VectorSpaceCentroid centroid0, VectorSpaceCentroid centroid1){
		double sum = 0;
		double[] c0 = centroid0.getCentroid();
		double[] c1 = centroid1.getCentroid();
		for (int i = 0; i < c0.length && i < c1.length; i++) {
			sum += c1[i] * c0[i];
		}
		return sum;
	}
	
	
	public static double normalizedRelevanceDistance(VectorSpaceCentroid centroid0, VectorSpaceCentroid centroid1){
		double log0, log1 , logCommon, maxlog, minlog;

		log0 = centroid0.getScoreSumLog();
		log1 = centroid1.getScoreSumLog();

        double commonScore = sumCommonScores(centroid0, centroid1);
        
		if(commonScore == 0) {
			return 0.0;
		}
		logCommon = Math.log(commonScore);
		maxlog = Math.max(log0, log1);
		minlog = Math.min(log0, log1);
    
		return  Math.exp(-2*  (maxlog - logCommon) / (Math.log(centroid0.numDocs()) - minlog));
	}
	
	public int json(BufferedWriter bw) throws IOException{
		bw.write("{\"totalDocs\":" + totalDocs);
		bw.write(",\"scores\":[");

		for(int j = 0; j < totalDocs; j++){
			if(j != 0){
				bw.write(",");
			}
			if(centroid[j] == 0.0){
				bw.write("0");
			}
			else {
				bw.write(String.format("%.6f",centroid[j]));
			}

		}
		bw.write("]}");
		return totalDocs;
	}
	
}
