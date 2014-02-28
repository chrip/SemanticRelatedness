package semanticRelatedness;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.search.similarities.DefaultSimilarity;

public class ESASimilarity extends DefaultSimilarity {

	public float idf(int docFreq, int numDocs) {
		return (float) Math.log(numDocs / (double) docFreq);
	}
	
	@Override
	public float tf(float freq) {
		return (float) (1.0 + Math.log(freq));
	}
	
//	  @Override
//	  public float lengthNorm(FieldInvertState state) {
////	    final int numTerms;
////	    if (discountOverlaps)
////	      numTerms = state.getLength() - state.getNumOverlap();
////	    else
////	      numTerms = state.getLength();
////	   return state.getBoost() * ((float) (1.0 / Math.sqrt(numTerms)));
//		  return (float)1.0;
//	  }
		  

}
