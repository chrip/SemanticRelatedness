package semanticRelatedness;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
//import org.apache.lucene.analysis.standard.StandardAnalyzer;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.similarities.DefaultSimilarity;
//import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;


public class SemanticRelatedness {

	static String field = "contents";

	static IndexReader reader;
	static IndexSearcher searcher;
	static Analyzer analyzer;
	static QueryParser parser;
	static HashMap<String, HashMap<String, Double>> ngrams;
 //	static ESASimilarity esaSimilarity;
	static DefaultSimilarity esaSimilarity;
	static Statistics _stats;

	public static void evaluate(Statistics stats) throws IOException, ParseException {
		_stats = stats;
		ngrams = new HashMap<String, HashMap<String, Double>>();
		reader = DirectoryReader.open(FSDirectory.open(new File(_stats.indexPath)));
		searcher = new IndexSearcher(reader);
		esaSimilarity = new DefaultSimilarity();
//		esaSimilarity = new ESASimilarity();
		searcher.setSimilarity(esaSimilarity);


//		Analyzer analyzer = new WikipediaAnalyzer(_stats);
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_4_9);
		parser = new QueryParser(Version.LUCENE_4_9, field, analyzer);

		
		for(String datasetName: _stats.datasets) {
			for(String algotithm : _stats.algorithms) {
				String line;
				FileReader is = new FileReader(_stats.getInputPath(datasetName));
				BufferedReader br = new BufferedReader(is);

				
				LineNumberReader  lnr = new LineNumberReader(new FileReader(_stats.getInputPath(datasetName)));
				lnr.skip(Long.MAX_VALUE);
				int n = lnr.getLineNumber();
				lnr.close();
				double[] xArray = new double[n];
				double[] yArray = new double[n];
				
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(_stats.getOutputPath(algotithm, datasetName)),"UTF-8"));
				int i = 0;
				while((line = br.readLine()) != null){
					final String [] parts = line.split(",");
					if(parts.length != 3) {
						break;
					}
					if(datasetName.equals("mc") || datasetName.equals("rg") || datasetName.equals("wordsim")) {
						xArray[i] = Double.valueOf(parts[2]);
					}
					if(algotithm.equals("NWD")) {
						yArray[i] = normalizedWikipediaDistance(parts[0], parts[1]);						
					}
					else if(algotithm.equals("ESA")) {
						yArray[i] = explicitSemanticAnalysis(parts[0], parts[1]);
					}
					else if(algotithm.equals("NRD")) {
						yArray[i] = normalizedRelevanceDistance(parts[0], parts[1]);
					}
					else if(algotithm.equals("2gram")) {
						yArray[i] = twogram(parts[0], parts[1]);
					}
					else if(algotithm.equals("3gram")) {
						yArray[i] = threegram(parts[0], parts[1]);
					}
					else if(algotithm.equals("5gram")) {
						yArray[i] = fivegram(parts[0], parts[1]);
					}
					else if(algotithm.equals("PMI")) {
						yArray[i] = pointwiseMutualInformation(parts[0], parts[1]);
					}
					if(yArray[i] < 0 || yArray[i] > 1) {
						//System.out.println("correlation: " + algotithm + " " + yArray[i] + " " + datasetName + " " + parts[0] + " " + parts[1]);
					}
					bw.write(line + ";" + _stats.myFormatter.format(yArray[i]) + "\n");
					i++;
				}
				br.close();
				bw.close();
				
				if(datasetName.equals("mc") || datasetName.equals("rg") || datasetName.equals("wordsim")) {
					SpearmansCorrelation sc = new SpearmansCorrelation();
					double co1 = sc.correlation(xArray, yArray);					
					_stats.setSpearmansCorrelation(co1, datasetName, algotithm);
					
					PearsonsCorrelation pc = new PearsonsCorrelation();
					double co2 = pc.correlation(xArray, yArray);
					_stats.setPearsonsCorrelation(co2, datasetName, algotithm);
				}
				
				_stats.numberOfDocs = reader.numDocs();
	
//				System.out.println("correlation: " + co + " " + tfidfThreshold + " " + freqThreshold + " " + WINDOW_THRES);
			}

		}
		
	}

	public static double normalizedWikipediaDistance(String term0, String term1) throws ParseException, IOException {

		Query query0 = parser.parse(term0);
		Query query1 = parser.parse(term1);

		BooleanQuery combiQuery0 = new BooleanQuery();
		combiQuery0.add(query0, BooleanClause.Occur.MUST);
		TopDocs results0 = searcher.search(combiQuery0, 1);

		BooleanQuery combiQuery1 = new BooleanQuery();
		combiQuery1.add(query1, BooleanClause.Occur.MUST);
		TopDocs results1 = searcher.search(combiQuery1, 1);

		BooleanQuery query0AND1 = new BooleanQuery();
		query0AND1.add(combiQuery0, BooleanClause.Occur.MUST);
		query0AND1.add(combiQuery1, BooleanClause.Occur.MUST);

		TopDocs results0AND1 = searcher.search(query0AND1, 1);
		
		if(results0.totalHits < 1 || results1.totalHits < 1|| results0AND1.totalHits < 1) {
			return 0;
		}

		double log0, log1 , logCommon, maxlog, minlog;
		log0 = Math.log(results0.totalHits);
		log1 = Math.log(results1.totalHits);
		logCommon = Math.log(results0AND1.totalHits);
		maxlog = Math.max(log0, log1);
		minlog = Math.min(log0, log1);

		return Math.exp(-2* (maxlog - logCommon) / (Math.log(reader.numDocs()) - minlog)); 

	}

	private static double sumScores(TopDocs results) {
		double sum = 0.0; 
		double maxScore = results.getMaxScore();

		for (ScoreDoc sd : results.scoreDocs) {
			sum += (sd.score/maxScore);
		}
		return sum;
	}

	public static double cosineDistance(String term0, String term1) throws ParseException, IOException {

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
		double scalar = 0.0d, r0Norm=0.0d, r1Norm=0.0d;

		int i = 0, j = 0;

		double maxScore0 = results0.getMaxScore();
		double maxScore1 = results1.getMaxScore();
		while (i < results0.scoreDocs.length && j < results1.scoreDocs.length) {
			double score0 = results0.scoreDocs[i].score/maxScore0;
			double score1 = results1.scoreDocs[j].score/maxScore1;

			if(score0 < _stats.tfidfThreshold) {
				score0 = 0.0;	        	
			}
			if(score1 < _stats.tfidfThreshold) {
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
			if(score0 < _stats.tfidfThreshold) {
				score0 = 0.0;	        	
			}
			r0Norm += Math.pow(score0, 2);
			i++;

		}
		while (j < results1.scoreDocs.length) {
			double score1 = results1.scoreDocs[j].score/maxScore1;
			if(score1 < _stats.tfidfThreshold) {
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


	public static double normalizedRelevanceDistance(String term0, String term1) throws ParseException, IOException {
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

		if(tfc0.getTotalHits() < 1 || tfc1.getTotalHits() < 1) {
			return 0.5;
		}

		double log0, log1 , logCommon, maxlog, minlog;

		log0 = Math.log(sumScores(results0));
		log1 = Math.log(sumScores(results1));
        double commonScore = sumCommonScores(results0, results1, term0, term1);
		if(commonScore == 0) {
			return 0.5;
		}
		logCommon = Math.log(commonScore);
		maxlog = Math.max(log0, log1);
		minlog = Math.min(log0, log1);
    
		return  Math.exp(-2*  (maxlog - logCommon) / (Math.log(reader.numDocs()) - minlog));
	}


	private static double sumCommonScores(TopDocs results0, TopDocs results1, String term0, String term1){
		double sum = 0;
		double maxScore0 = results0.getMaxScore();
		double maxScore1 = results1.getMaxScore();
		int i = 0, j = 0;

		while (i < results0.scoreDocs.length && j < results1.scoreDocs.length) {
			if (results0.scoreDocs[i].doc < results1.scoreDocs[j].doc) {
				i++;
			}
			else if (results0.scoreDocs[i].doc == results1.scoreDocs[j].doc) {
				if(results0.scoreDocs[i].score/maxScore0 > _stats.tfidfThreshold && results1.scoreDocs[j].score/maxScore1 > _stats.tfidfThreshold) {
					double sim1AND2 = (results0.scoreDocs[i].score/maxScore0)  * (results1.scoreDocs[j].score/maxScore1);
					sum += sim1AND2;
				}
				i++;
				j++;
			}
			else {
				j++;
			}
		}
		return sum;
	}

	public static double explicitSemanticAnalysis(String term0, String term1) throws ParseException, IOException {

		Query query0 = parser.parse(term0);
		Query query1 = parser.parse(term1);		

		AtomicReader ar = reader.leaves().get(0).reader();	

		int totalDocs = reader.numDocs();

		String term0Parsed = query0.toString(field);
		String term1Parsed = query1.toString(field);
		if (term0Parsed.equals("") || term1Parsed.equals("")) {
			return 0;
		}

		Term t0 = new Term(field, term0Parsed);
		Term t1 = new Term(field, term1Parsed);
		int docFreq0 = reader.docFreq(t0);
		int docFreq1 = reader.docFreq(t1);
		
		if (docFreq0 < _stats.freqThreshold || docFreq1 < _stats.freqThreshold) {
			return 0;
		}
		
		double idf0 = esaSimilarity.idf(docFreq0, totalDocs);
		double idf1 = esaSimilarity.idf(docFreq1, totalDocs);

		DocsEnum docEnum0 = ar.termDocsEnum(t0);
		DocsEnum docEnum1 = ar.termDocsEnum(t1);

		double scalar = 0.0, r0Norm=0.0, r1Norm=0.0;
		List<IdScorePair> prunedVector0 = getIndexPruningThreshold(docEnum0, idf0);
		List<IdScorePair> prunedVector1 = getIndexPruningThreshold(docEnum1, idf1);
		
		int i = 0;
		int j = 0;
		int docid0 = 0;
		int docid1 = 0;
		while (i < prunedVector0.size() && j < prunedVector1.size()) {

			docid0 = prunedVector0.get(i).id;
			docid1 = prunedVector1.get(j).id;
			double tfidf0 = prunedVector0.get(i).score;
			double tfidf1 = prunedVector1.get(j).score;

			if (tfidf0 < _stats.tfidfThreshold){
				tfidf0 = 0;
			}
			if (tfidf1 < _stats.tfidfThreshold){
				tfidf1 = 0;
			}
			
			
			if (docid0 < docid1) {
				r0Norm += Math.pow(tfidf0, 2);
				docid0 = prunedVector0.get(i).id;
				i++;
			}
			else if (docid0 == docid1) {
				scalar += tfidf0 * tfidf1;
				r0Norm += Math.pow(tfidf0, 2);
				r1Norm += Math.pow(tfidf1, 2);
				docid0 = prunedVector0.get(i).id;
				docid1 = prunedVector1.get(j).id;
				i++;
				j++;
			}
			else {
				r1Norm += Math.pow(tfidf1, 2);
				docid1 = prunedVector1.get(j).id;
				j++;
			}

		}
		while (i < prunedVector0.size()) {
			double tfidf0 = prunedVector0.get(i).score;

			if (tfidf0 < _stats.tfidfThreshold){
				tfidf0 = 0;
			}
			r0Norm += Math.pow(tfidf0, 2);
			i++;
		}
		while (j < prunedVector1.size()) {
			double tfidf1 = prunedVector1.get(j).score;

			if (tfidf1 < _stats.tfidfThreshold){
				tfidf1 = 0;
			}
			r1Norm += Math.pow(tfidf1, 2);
			j++;
		}

		r0Norm=Math.sqrt(r0Norm);
		r1Norm=Math.sqrt(r1Norm);
		if(r0Norm == 0 || r1Norm == 0){
			return 0;
		}
		return scalar / (r0Norm * r1Norm); 
	}
	
	private static List<IdScorePair> getIndexPruningThreshold(DocsEnum docEnum, double idf) throws IOException {
		List<IdScorePair> termVector = new ArrayList<IdScorePair>();
		if (docEnum == null) {
			return termVector;
		}
		int docid = docEnum.nextDoc();
		double sum = 0;
		while (docid != DocsEnum.NO_MORE_DOCS) {
			int termFreq = docEnum.freq();
			double tfidf = esaSimilarity.tf(termFreq) * idf;
			sum+= tfidf * tfidf;
			termVector.add(new IdScorePair(docid, tfidf));
			docid = docEnum.nextDoc();
		}
		sum = Math.sqrt(sum);
		
		// normalize vector
		for(IdScorePair p:termVector) {
			p.score = p.score/sum;
		}
		
		if(_stats.indexPruning) {
			Collections.sort(termVector, new ScoreComparator());
			
			int mark = 0;
			int windowMark = 0;
			double score = 0;
			double highest = 0;
			double first = 0;
			double last = 0;
			
			double[] window = new double[_stats.WINDOW_SIZE];
	
			for (int j = 0; j < termVector.size(); j++) {
				score = termVector.get(j).score;
	
				// sliding window
	
				window[windowMark] = score;
	
				if (mark == 0) {
					highest = score;
					first = score;
				}
	
				if (mark < _stats.WINDOW_SIZE) {
					// fill window
				} else if (highest * _stats.WINDOW_THRES < (first - last)) {
					// ok
	
					if (windowMark < _stats.WINDOW_SIZE - 1) {
						first = window[windowMark + 1];
					} else {
						first = window[0];
					}
				} else {
					// truncate
					termVector = termVector.subList(0, j);
					break;
				}
	
				last = score;
	
				mark++;
				windowMark++;
	
				windowMark = windowMark % _stats.WINDOW_SIZE;
	
			}
		}
		Collections.sort(termVector, new IdComparator());
		
		return termVector;
	}

	public static double scoredNWD(String term0, String term1) throws ParseException, IOException {

		Query query0 = parser.parse(term0);
		Query query1 = parser.parse(term1);		

		AtomicReader ar = reader.leaves().get(0).reader();	

		int totalDocs = reader.numDocs();

		String term0Parsed = query0.toString();
		String term1Parsed = query1.toString();
		if (term0Parsed.equals("") || term1Parsed.equals("")) {
			return 0;
		}

		Term t0 = new Term(field, term0Parsed.substring(field.length()+1));
		Term t1 = new Term(field, term1Parsed.substring(field.length()+1));
		int docFreq0 = reader.docFreq(t0);
		int docFreq1 = reader.docFreq(t1);

		if (docFreq0 < _stats.freqThreshold || docFreq1 < _stats.freqThreshold) {
			return 0;
		}
		
		double idf0 = esaSimilarity.idf(docFreq0, totalDocs);
		double idf1 = esaSimilarity.idf(docFreq1, totalDocs);

		DocsEnum docEnum0 = ar.termDocsEnum(t0);
		DocsEnum docEnum1 = ar.termDocsEnum(t1);

		double scalar = 0.0, r0Norm=0.0, r1Norm=0.0, maxScore0=0.0, maxScore1=0.0;
		List<IdScorePair> prunedVector0 = getIndexPruningThreshold(docEnum0, idf0);
		List<IdScorePair> prunedVector1 = getIndexPruningThreshold(docEnum1, idf1);
		
		maxScore0 = getMaxScore(prunedVector0);		
		maxScore1 = getMaxScore(prunedVector1);
		
		int i = 0;
		int j = 0;
		int docid0 = 0;
		int docid1 = 0;
		while (i < prunedVector0.size() && j < prunedVector1.size()) {

			docid0 = prunedVector0.get(i).id;
			docid1 = prunedVector1.get(j).id;
			double tfidf0 = prunedVector0.get(i).score/maxScore0;
			double tfidf1 = prunedVector1.get(j).score/maxScore1;

			if (tfidf0 < _stats.tfidfThreshold){
				tfidf0 = 0;
			}
			if (tfidf1 < _stats.tfidfThreshold){
				tfidf1 = 0;
			}
			
			
			if (docid0 < docid1) {
				r0Norm += tfidf0;
				i++;
			}
			else if (docid0 == docid1) {
				scalar += tfidf0 * tfidf1;
				r0Norm += tfidf0;
				r1Norm += tfidf1;
				i++;
				j++;
			}
			else {
				r1Norm +=tfidf1;
				j++;
			}

		}
		while (i < prunedVector0.size()) {
			double tfidf0 = prunedVector0.get(i).score/maxScore0;

			if (tfidf0 < _stats.tfidfThreshold){
				tfidf0 = 0;
			}
			r0Norm += tfidf0;
			i++;
		}
		while (j < prunedVector1.size()) {
			double tfidf1 = prunedVector1.get(j).score/maxScore1;

			if (tfidf1 < _stats.tfidfThreshold){
				tfidf1 = 0;
			}
			r1Norm += tfidf1;
			j++;
		}
		if(scalar == 0) {
			return 0;
		}
		double log0, log1 , logCommon, maxlog, minlog;

		log0 = Math.log(r0Norm);
		log1 = Math.log(r1Norm);
		logCommon = Math.log(scalar);
		maxlog = Math.max(log0, log1);
		minlog = Math.min(log0, log1);
		
		return 1 - (maxlog - logCommon) / (Math.log(totalDocs) - minlog); 
	}

	public static double getMaxScore(List<IdScorePair> prunedVector0) {
		double maxScore = 0.0;
		if(prunedVector0.size() != 0) {
			Collections.sort(prunedVector0, new ScoreComparator());
			maxScore = prunedVector0.get(0).score;
			Collections.sort(prunedVector0, new IdComparator());
		}
		return maxScore;
	}
	
	public static double twogram(String term0, String term1) throws IOException {
		String line;
		String[] pos = {"Before","After"};
		String[] term = new String[2];
		term[0] = term0;
		term[1] = term1;
		ngrams.clear();
		for (String t: term) {
			for (String p: pos){
				if(!ngrams.containsKey(t+p)) {
					HashMap<String, Double> termSet = new HashMap<String, Double>();

					FileReader is = new FileReader(_stats.ngramPath + "/" + p +"/" + t + p + "Result");
					BufferedReader br = new BufferedReader(is);	

					while((line = br.readLine()) != null){
						final String [] parts = line.split("\t");
						if(parts.length != 3) {
							break;
						}
						termSet.put(parts[0], Double.valueOf(parts[2]));
					}
					br.close();
					ngrams.put(t + p, termSet);
				}
			}
		}
		double scalar = 0.0d;
		for (String p: pos){
			scalar = cosine(term0, term1, p);
		}
		return scalar;
	}
	public static double threegram(String term0, String term1) throws IOException {
		String line;
		String[] pos = {"trigrams"};
		String[] term = new String[2];
		term[0] = term0;
		term[1] = term1;
		ngrams.clear();
		for (String t: term) {
			for (String p: pos){
				if(!ngrams.containsKey(t+p)) {
					HashMap<String, Double> termSet = new HashMap<String, Double>();
					
					FileReader is = new FileReader(_stats.ngramPath + "/" + p +"/" + t);
					BufferedReader br = new BufferedReader(is);	

					while((line = br.readLine()) != null){
						final String [] parts = line.split("\t");
						final String [] words = parts[0].split(" ");
						if(words.length != 3) {
							break;
						}
						termSet.put(words[0] + " " + words[2], Double.valueOf(parts[1]));
					}
					br.close();
					ngrams.put(t + p, termSet);
				}
			}
		}
		double scalar = 0;
		for (String p: pos){ // TODO clean up
			scalar = cosine(term0, term1, p);
			//scalar = snwd(term0, term1, p);

		}
		return scalar;
	}
	
	public static double fivegram(String term0, String term1) throws IOException {
		String line;

		String[] term = new String[2];
		term[0] = term0;
		term[1] = term1;
		ngrams.clear();
		for (String t: term) {
			for (int p = 1; p <6; p++){
				if(!ngrams.containsKey(t+p)) {
					HashMap<String, Double> termSet = new HashMap<String, Double>();
					
					FileReader is = new FileReader(_stats.ngramPath + "/5grams/" + p +"/" + p + t + ".txt");
					BufferedReader br = new BufferedReader(is);	

					while((line = br.readLine()) != null){
						final String [] parts = line.split("\t");
						final String [] words = parts[0].split(" ");
						if(words.length != 5) {
							break;
						}
						String k = "";
						for (int pMinus1 = 0; pMinus1 < 5; pMinus1++){
							k+=(p-1 == pMinus1)?"":words[pMinus1];
							k+= " ";
						}
						termSet.put(k, Double.valueOf(parts[1]));
					}
					br.close();
					ngrams.put(t+p, termSet);
				}
			}
		}
		double scalar = 0;
		for (int p = 1; p < 6; p++){ // TODO clean up
			scalar = cosine(term0, term1, ""+p);
			//scalar = snwd(term0, term1, ""+p);

		}
		return scalar;
	}
	
	public static double pointwiseMutualInformation(String term0, String term1) throws ParseException, IOException {
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

		if(tfc0.getTotalHits() < 1 || tfc1.getTotalHits() < 1) {
			return 0;
		}

        double commonScore = sumCommonScores(results0, results1, term0, term1);
		if(commonScore == 0) {
			return 0;
		}

		return Math.log((commonScore/(reader.numDocs()*reader.numDocs())) / (sumScores(results0) * sumScores(results1))/reader.numDocs()) / Math.log(2); 
	}

	public static double cosine(String term0, String term1, String p) {
		double scalar = 0;
		boolean set0IsLarger = ngrams.get(term0 + p).size() > ngrams.get(term1 + p).size();
		HashMap<String, Double> biggerSet = new HashMap<String, Double>(set0IsLarger ? ngrams.get(term0 + p) : ngrams.get(term1 + p));
		HashMap<String, Double> smallerSet = new HashMap<String, Double>(!set0IsLarger ? ngrams.get(term0 + p) : ngrams.get(term1 + p));
		
		double biggerNorm=0.0d, smallerNorm=0.0d;

		
		for(String key:smallerSet.keySet()){
			smallerNorm += Math.pow(smallerSet.get(key), 2);
		}
		for(String key:biggerSet.keySet()){
			biggerNorm += Math.pow(biggerSet.get(key), 2);
			if(smallerSet.containsKey(key)){
				scalar+= biggerSet.get(key) * smallerSet.get(key);
			}
		}
		biggerNorm=Math.sqrt(biggerNorm);
		smallerNorm=Math.sqrt(smallerNorm);
		
		if(biggerNorm == 0 || smallerNorm == 0){
			scalar = 0;
		}
		else {
		
		scalar /= (biggerNorm * smallerNorm);
		}
		return scalar;
	}
}

class IdScorePair {
    public int id;
    public double score;

    public IdScorePair(int id, double score) {
        this.id = id;
        this.score = score;
    }
}
class IdComparator implements Comparator<IdScorePair> {
    public int compare(IdScorePair idScorePair1, IdScorePair idScorePair2) {
        return idScorePair1.id - idScorePair2.id;
    }
}
class ScoreComparator implements Comparator<IdScorePair> {
    public int compare(IdScorePair idScorePair1, IdScorePair idScorePair2) {
        if(idScorePair1.score < idScorePair2.score) {
        	return 1;
        }
        else if (idScorePair1.score > idScorePair2.score) {
        	return -1;
        }
        else {
        	return 0;
        }
    }
}
