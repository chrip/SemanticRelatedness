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
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/** Index all text files under a directory.
 * <p>
 * This is a command-line application demonstrating simple Lucene indexing.
 * Run it with no command-line arguments for usage information.
 */
public class MakeLuceneIndex {
	static Map<String, Integer> _inLinks = new HashMap<String, Integer>();
	private MakeLuceneIndex() {}

	/** Index all text files under a directory. 
	 * @throws UnsupportedEncodingException 
	 * @throws FileNotFoundException */
	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
		String baseDir = "/home/chrisschaefer/";
		//String wikiDumpFile = "Downloads/enwiki-20130604-pages-articles.xml.bz2";
		String wikiDumpFile = "enwiki-20130604-pages-articlese.xml.bz2";
		String luceneIndexName = "enwiki-20130604-lucene2";

		System.currentTimeMillis();
		boolean bIgnoreStubs = false;

		for ( int i = 0; i < args.length; ++i )
		{
			if ( args[i].equals( "-luceneindex" ) )
				luceneIndexName = args[++i];

			if ( args[i].equals( "-basedir" ) )
				baseDir = args[++i];

			if ( args[i].equals( "-dumpfile" ) )
				wikiDumpFile = args[++i];

			if ( args[i].equals( "-includestubs" ) )
				bIgnoreStubs = true;
		}
		String rawTextPath = baseDir + luceneIndexName + "-raw-text.txt";
		String logPath = baseDir + luceneIndexName + ".log";
		PrintWriter artikelTextWriter= new PrintWriter(rawTextPath, "UTF-8");
		PrintWriter logger= new PrintWriter(logPath, "UTF-8");
		logger.println("Indexing to directory '" + baseDir + luceneIndexName + "'");
		System.out.println("Indexing to directory '" + baseDir + luceneIndexName + "'");

		Date start = new Date();

		try {

			Directory dir = FSDirectory.open(new File(baseDir + luceneIndexName));


//			Analyzer analyzer = new WikipediaAnalyzer();
			Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_43);
			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_43, analyzer);


			// Create a new index in the directory, removing any
			// previously indexed documents:
			iwc.setOpenMode(OpenMode.CREATE);
			iwc.setSimilarity(new ESASimilarity());

			// Optional: for better indexing performance, if you
			// are indexing many documents, increase the RAM
			// buffer.  But if you do this, increase the max heap
			// size to the JVM (eg add -Xmxm or -Xmx1g):
			//
			iwc.setRAMBufferSizeMB(2000.0);

			IndexWriter writer = new IndexWriter(dir, iwc);

			Extractor wikidumpExtractor = new Extractor( baseDir + File.separator + wikiDumpFile );
			wikidumpExtractor.setLinkSeparator( "_" );
			wikidumpExtractor.setCategorySeparator( "_" );
			wikidumpExtractor.setTitleSeparator(" ");

			int iStubs = 0;
			int iArticleCount = 0;
			int iSkippedPageCount = 0;
			long iStartTime = java.lang.System.nanoTime();
			long iTime = iStartTime;

			while ( wikidumpExtractor.nextPage() )
			{
				if ( wikidumpExtractor.getPageType() != Extractor.PageType.ARTICLE )
				{
					++iSkippedPageCount;
					continue;
				}

				if ( bIgnoreStubs && wikidumpExtractor.getStub() )
				{
					++iStubs;
					continue;
				}

				// skip pages with less than 5 out links
				if ( wikidumpExtractor.getPageLinkList(true).size() < 5) {
					++iSkippedPageCount;
					continue; 
				}
				if ( wikidumpExtractor.getPageCategories().equals("")) {
					++iSkippedPageCount;
					logger.println("skipped because of stop category: " + wikidumpExtractor.getPageTitle( false ));
					continue; 
				}
				else {
					for(String link: wikidumpExtractor.getPageLinkList(false)) {            		 
						//            		  artikelTextWriter.println(link);
						if(_inLinks.containsKey(link)) {
							int tmp = _inLinks.get(link);
							tmp++;
							_inLinks.put(link, tmp);
						}
						else {
							_inLinks.put(link, 1);
						}
					}
				}
				if(wikidumpExtractor.getPageText().equals("")){
					++iSkippedPageCount;
					continue; 
				}
				artikelTextWriter.println(wikidumpExtractor.getPageTitle( false ) + "\t" + wikidumpExtractor.getPageText(false));

				++iArticleCount;         

				if ( iArticleCount % 1000 == 0 )
				{
					logger.println(new Date().toString() + " phase 1 -- iArticleCount: " + iArticleCount + " iSkippedPageCount: " + iSkippedPageCount);                          
				}
			}
			artikelTextWriter.close();
			iArticleCount = 0;

			PrintWriter artikelInLinkWriter= new PrintWriter(baseDir + luceneIndexName + "-inlinks.txt" , "UTF-8");
			BufferedReader br = new BufferedReader(new FileReader(rawTextPath));
			String line = br.readLine();

			while (line != null) {
				int endOfTitle = line.indexOf("\t");
				String title = line.substring(0, endOfTitle);        	  
				if( _inLinks.containsKey(title)){
					int inlinks = _inLinks.get(title);
					artikelInLinkWriter.println(title + "\t" + inlinks);
					if(inlinks > 4) {
						//System.out.println("inlinks > 0 ");
						Document doc = new Document();
						++iArticleCount;


						//                    wikidumpExtractor.setTitleSeparator( "_" );
						//                    doc.add( new TextField( "url_title", wikidumpExtractor.getPageTitle( false ), Field.Store.YES) );


						// doc.add( new TextField( "title", wikidumpExtractor.getPageTitle( false ), Field.Store.YES) );
						//doc.add(new LongField("wiki_id", wikidumpExtractor.getPageId(), Field.Store.YES));
						doc.add(new TextField("contents", 
								title + " " + 
								title + " " + 
								title + " " + 
								title + " " +
								line.substring(endOfTitle+1), Field.Store.NO ));
//						System.out.println(title + " " + 
//								title + " " + 
//								title + " " + 
//								title + " " +
//								line.substring(endOfTitle+1));

						writer.addDocument( doc );              

						if ( iArticleCount % 1000 == 0 )
						{
							writer.commit();
							logger.println(new Date().toString() + " phase 2 -- iArticleCount: " + iArticleCount + " iSkippedPageCount: " + iSkippedPageCount);                          
						} 
					}
				}
				else {
					artikelInLinkWriter.println(title + "\t0");
				}
				line = br.readLine();
			}
			br.close();
			artikelInLinkWriter.close();

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




