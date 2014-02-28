package semanticRelatedness;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
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


public class FullGabToLucene {

	public static void main(String[] args) throws IOException {
		Date start = new Date();

		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_46, analyzer);

		iwc.setSimilarity(new DefaultSimilarity());
		iwc.setOpenMode(OpenMode.CREATE);
		iwc.setRAMBufferSizeMB(2000.0);

		String dumpFile = args[0];
		FileInputStream fin = new FileInputStream(dumpFile);
		BufferedInputStream in = new BufferedInputStream(fin);
		BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(in);

		BufferedReader br = new BufferedReader(new InputStreamReader(bzIn));

		Directory dir = FSDirectory.open(new File(args[1]));
		IndexWriter writer = new IndexWriter(dir, iwc);


		br.readLine();
		br.readLine();
		br.readLine();
		String line = br.readLine();

		int articlesDone = 0;

		line = br.readLine();
		while ( line != null )
		{
			int endPageId = line.indexOf("\" orglength=\"");
			String title;
			if(endPageId != -1) {

				articlesDone++;

				line = br.readLine();
				title = line.substring(7,line.length()-8);


				String text = new String();
				line = br.readLine();

				while ( !br.readLine().equals("<text>") ) {
					continue;
				}
				line = br.readLine();
				while ( !line.equals("</text>") ) {
					text += " ";
					text += line;						
					line = br.readLine();
				}

				articlesDone++;
				Document doc = new Document();

				doc.add(new TextField("contents", title + text, Field.Store.NO ));
				writer.addDocument( doc );
				if ( articlesDone % 1000 == 0 )
				{
					System.out.println(articlesDone);
					writer.commit();
				} 
			}		
			line = br.readLine();
		}

		writer.commit();
		writer.forceMerge(1);
		writer.close();
		br.close();

		Date end = new Date();
		String endStatement = end.getTime() - start.getTime() + " total milliseconds (" + (end.getTime() - start.getTime())/3600000.0 + " hours), " + articlesDone + " Articles.";
		System.out.println(endStatement);
		System.out.println("index_time\tindex_name\t");
		System.out.println(end.getTime() - start.getTime() + "\t" + args[1]);

	}
}
