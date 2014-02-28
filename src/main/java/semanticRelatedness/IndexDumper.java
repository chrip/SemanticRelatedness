package semanticRelatedness;

import java.io.File;
import java.io.IOException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;


public class IndexDumper {

	static String field = "contents";
	static String indexPath = "/home/chrisschaefer/enwiki-20130604-lucene-no-stubs-custom-analyzer";
	static IndexReader reader;
	static IndexSearcher searcher;
	static Analyzer analyzer;
	static QueryParser parser;
	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParseException 
	 */
	public static void main(String[] args) throws IOException, ParseException {

	    
		reader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
		searcher = new IndexSearcher(reader);
		
		ESASimilarity esaSimilarity = new ESASimilarity();
		searcher.setSimilarity(esaSimilarity);
		
		AtomicReader ar = reader.leaves().get(0).reader();
		Fields fields = ar.fields();
		Terms terms = fields.terms(field);
		TermsEnum termsEnum = terms.iterator(null);
		BytesRef term;
		int totalDocs = reader.numDocs();
		String s = new String();
		int o = 0;
		while ((term = termsEnum.next()) != null) {			

			int docFreq = termsEnum.docFreq();
			if (docFreq < 3){
				continue;
			}
			if(!term.utf8ToString().matches("[a-z]+")) {
				continue;		
			}

			double idf = esaSimilarity.idf(docFreq, totalDocs);
			s = term.utf8ToString();
			 DocsEnum docEnum = termsEnum.docs(ar.getLiveDocs(), null);
			 int docid;
			 while ((docid = docEnum.nextDoc()) != DocsEnum.NO_MORE_DOCS) {
				 double tfidf = esaSimilarity.tf(docEnum.freq()) * idf;

				 if(tfidf > 10) {
					 o++;
					 s += "\t" + docid + "\t" + tfidf;
				 }
			 }
			System.out.println(s);
		}
		System.out.println(o);
		System.exit(0);
	}
}

