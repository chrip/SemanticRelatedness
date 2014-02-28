package semanticRelatedness;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Date;

/** Index all text files under a directory.
 * <p>
 * This is a command-line application demonstrating simple Lucene indexing.
 * Run it with no command-line arguments for usage information.
 */
public class MakeLuceneIndexPreprocessed {

	private MakeLuceneIndexPreprocessed() {}

	/** Index all text files under a directory. 
	 * @throws UnsupportedEncodingException 
	 * @throws FileNotFoundException */
	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
		String baseDir = "/home/chrisschaefer/";

		String inputLuceneIndexName = "2013-06-18-lucene-gab";
		String luceneIndexName = "2013-06-18-lucene-gab-standard";

		System.currentTimeMillis();

		for ( int i = 0; i < args.length; ++i )
		{
			if ( args[i].equals( "-inputluceneindex" ) )
				inputLuceneIndexName = args[++i];
			
			if ( args[i].equals( "-outputluceneindex" ) )
				luceneIndexName = args[++i];

			if ( args[i].equals( "-basedir" ) )
				baseDir = args[++i];

		}
		String rawTextPath = baseDir + inputLuceneIndexName + "-raw-text.txt";
		String artikelInLinksPath = baseDir + inputLuceneIndexName + "-inlinks.txt";
		String logPath = baseDir + inputLuceneIndexName + ".log";
		

		PrintWriter logger= new PrintWriter(logPath, "UTF-8");
		logger.println("Indexing to directory '" + baseDir + luceneIndexName + "'");
		System.out.println("Indexing to directory '" + baseDir + luceneIndexName + "'");

		Date start = new Date();
		logger.println(start.toString() + " iArticleCount: 0 iSkippedPageCount: 0");

		try {

			Directory dir = FSDirectory.open(new File(baseDir + luceneIndexName));

//			Analyzer analyzer = new WikipediaAnalyzer();
			Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_43);
			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_43, analyzer);


			// Create a new index in the directory, removing any
			// previously indexed documents:
			iwc.setOpenMode(OpenMode.CREATE);


			// Optional: for better indexing performance, if you
			// are indexing many documents, increase the RAM
			// buffer.  But if you do this, increase the max heap
			// size to the JVM (eg add -Xmxm or -Xmx1g):
			//
			iwc.setRAMBufferSizeMB(2000.0);
//			iwc.setSimilarity(new ESASimilarity());
			
			IndexWriter writer = new IndexWriter(dir, iwc);
			

			int iArticleCount = 0;
			int iSkippedPageCount = 0;

			BufferedReader rawTextReader = new BufferedReader(new FileReader(rawTextPath));
			BufferedReader artikelInLinksReader  = new BufferedReader(new FileReader(artikelInLinksPath));
			String lineText = rawTextReader.readLine();
			String lineLinks = artikelInLinksReader.readLine();
			
			while (lineText != null) {
//				String title = lineText.substring(0, lineText.indexOf("\t")); 
//				while(!title.equals(lineLinks.substring(0, lineLinks.indexOf("\t")))){
//					lineLinks = artikelInLinksReader.readLine();
//				}
				int endOfTitle = lineText.indexOf("\t");
				String title = lineText.substring(0, endOfTitle);      
				
				if(Integer.valueOf(lineLinks.substring(lineLinks.indexOf("\t")+1)) > 0){
					++iArticleCount;
					Document doc = new Document();
					doc.add(new TextField("contents", 
							title + " " + 
							title + " " + 
							title + " " + 
							title + " " +
							lineText.substring(endOfTitle+1), Field.Store.NO ));
//					System.out.println(title + " " + 
//					title + " " + 
//					title + " " + 
//					title + " " +
//					lineText.substring(endOfTitle+1));
					writer.addDocument( doc );              

					if ( iArticleCount % 1000 == 0 )
					{
						writer.commit();
						logger.println(new Date().toString() + "phase 2 -- iArticleCount: " + iArticleCount + " iSkippedPageCount: " + iSkippedPageCount);
						logger.flush();
					}
				}
				lineText = rawTextReader.readLine();
				lineLinks = artikelInLinksReader.readLine();
			}
			rawTextReader.close();
			artikelInLinksReader.close();

			// NOTE: if you want to maximize search performance,
			// you can optionally call forceMerge here.  This can be
			// a terribly costly operation, so generally it's only
			// worth it when your index is relatively static (ie
			// you're done adding documents to it):
			//
			writer.commit();
			writer.forceMerge(1);
			writer.close();

			Date end = new Date();
			String endStatement = end.getTime() - start.getTime() + " total milliseconds (" + (end.getTime() - start.getTime())/3600000.0 + " hours), " + iArticleCount + " Articles.";
			logger.println(endStatement);
			System.out.println(endStatement);
			logger.close();
		} catch (Exception e) {
			System.out.println(" caught a " + e.getClass() +
					"\n with message: " + e.getMessage());
		}
	}
}




