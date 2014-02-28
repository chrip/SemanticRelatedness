package semanticRelatedness;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.LengthFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;


public class WikipediaAnalyzer extends Analyzer {
		// stopword set for filtering tokens
		final CharArraySet stopWordSet;
		Statistics _stats;
		
		public WikipediaAnalyzer(Statistics stats) throws IOException {

			_stats = stats;
			// read stop words
			if (_stats.stopWordsPath != null) {
				InputStream is = new FileInputStream(_stats.stopWordsPath);
				BufferedReader br = new BufferedReader(new InputStreamReader(is));
				ArrayList<String> stopWords = new ArrayList<String>(500);


				String line;

				while ((line = br.readLine()) != null) {
					line = line.trim();
					if (!line.equals("")) {
						stopWords.add(line.trim());
					}
				}

				br.close();

				final CharArraySet stopSet = new CharArraySet(Version.LUCENE_43, stopWords.size(), false);
				stopSet.addAll(stopWords);
				stopWordSet = CharArraySet.unmodifiableSet(stopSet);
			} else {
				stopWordSet = null;
			}
		}
	  @Override
	  protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
		final StandardTokenizer src = new StandardTokenizer(Version.LUCENE_43, reader);
        TokenStream tok = new StandardFilter(Version.LUCENE_43, src);
//        tok = new LengthFilter(false, tok, 3, 100);
        tok = new LowerCaseFilter(Version.LUCENE_43, tok);	  
		if ( _stats.filterStopWords) {
			tok = new StopFilter(Version.LUCENE_43, tok, (CharArraySet) stopWordSet);
		}
        
//	    tok = new StopFilter(Version.LUCENE_43, tok, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
		for (int i = 0; i < _stats.stemmerCalls; i++) {
		    tok = new PorterStemFilter(tok);
		}
	    return new TokenStreamComponents(src, tok);
		  
	  }
}