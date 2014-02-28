package semanticRelatedness;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;


public class SqliteToLucene {
	
	public static void buildIndex(Statistics stats) throws IOException {
		Date start = new Date();
		
//		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_43);
		Analyzer analyzer = new WikipediaAnalyzer(stats);
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_43, analyzer);

//		iwc.setSimilarity(new ESASimilarity());
		iwc.setSimilarity(new DefaultSimilarity());
		iwc.setOpenMode(OpenMode.CREATE);
		iwc.setRAMBufferSizeMB(2000.0);

		Directory dir = FSDirectory.open(new File(stats.indexPath));
		IndexWriter writer = new IndexWriter(dir, iwc);

		String dumpFile = "/home/chrisschaefer/robustness_evaluation/estrella/wiki2005/corpus.db";
		
		Date end = new Date();
		int iArticleCount = 0;
		int totalArticles = stats.limit;

		Connection c = null;
	    Statement stmt = null;
	    try {
	      Class.forName("org.sqlite.JDBC");
	      c = DriverManager.getConnection("jdbc:sqlite:" + dumpFile);
	      c.setAutoCommit(false);
	      System.out.println("Opened database successfully");

	      stmt = c.createStatement();
	      ResultSet rs = stmt.executeQuery( "select content, length from subcorpus limit "+ stats.limit +";" );
	      while ( rs.next() ) {
	         String  content = rs.getString("content");
	         Document doc = new Document();
	         doc.add(new TextField("contents", content, Field.Store.NO ));
				writer.addDocument( doc );
			    iArticleCount++;
				if ( iArticleCount % 1000 == 0 )
				{
					System.out.println(iArticleCount + "\t" + (iArticleCount/(double)totalArticles)*100);
					writer.commit();
				} 
	      
	      }
	      rs.close();
	      stmt.close();
	      c.close();
	    } catch ( Exception e ) {
	      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
	      System.exit(0);
	    }
	    
		writer.commit();
		writer.forceMerge(1);
		writer.close();
	
		String endStatement = end.getTime() - start.getTime() + " total milliseconds (" + (end.getTime() - start.getTime())/3600000.0 + " hours), " + iArticleCount + " Articles.";
		System.out.println(endStatement);
		System.out.println("index_time\tindex_name\t");
		System.out.println(end.getTime() - start.getTime() + "\t" + stats.indexPath);
		
	}
}
