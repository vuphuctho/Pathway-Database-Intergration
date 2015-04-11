import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.commons.math3.distribution.HypergeometricDistribution;


public class PathInt {
	
	public static TreeMap <String, Integer> allGenes;
	public static Integer geneCount, genePairCount;
	public static TreeMap <String, Vector<Integer>> glist1,glist2;
	public static TreeMap <String, Vector<Pair<Integer,Integer>>> gpair1,gpair2;
	public static Vector  <String> plist1,plist2;
	public static Double scoreArr[][][];
	
	public static void main(String[] args) {
		// Usage
		if (args.length != 6) { 
			System.out.println("Usage : <pathway list 1> <pathway gene list 1> <gene pair list 1>" 
							 		+ " <pathway list 2> <pathway gene list 2> <gene pair list 2>");
		} else try {
			allGenes 		= new TreeMap <String, Integer>();
			geneCount 		= 0;
			genePairCount 	= 0;
			new PathInt(args[0],args[1],args[2],args[3],args[4],args[5]);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static Integer getGeneIndex(String geneCode) {
		Integer index = allGenes.get(geneCode);
		if (index != null) {
			return index;
		} else {
			allGenes.put(geneCode,geneCount++);
			return geneCount - 1;
		}
	}
	
	public static Vector <String> getPathwayList(String inputFile) 
	throws FileNotFoundException,IOException {
		BufferedReader fin = new BufferedReader(new FileReader(inputFile));
		String line;
		Vector <String> plist = new Vector <String>();
		while ((line = fin.readLine()) != null) {
			plist.add(line);
		}
		fin.close();
		return plist;
	}
	
	public static TreeMap <String,Vector<Pair<Integer,Integer>>> getPairList(String inputFile) 
	throws FileNotFoundException,IOException {
		BufferedReader fin = new BufferedReader(new FileReader(inputFile));
		TreeMap <String,Vector<Pair<Integer,Integer>>> gpair = new TreeMap <String,Vector<Pair<Integer,Integer>>> ();
		
		// Initialize last pathways read 
		String lastpw = null, line, words[];
		Vector <Pair<Integer,Integer>> pairs = new Vector <Pair<Integer,Integer>> ();
		
		while ((line = fin.readLine())!= null) {
			words = line.split(" ");
			Integer index1 = getGeneIndex(words[1]),
					index2 = getGeneIndex(words[2]);
			if (lastpw == null) {
				// first time
				lastpw = words[0];
				pairs.add(new Pair<Integer,Integer>(index1,index2));
			} else if (lastpw.equals(words[0])) {
				// same pathway
				pairs.add(new Pair<Integer,Integer>(index1,index2));
			} else {
				// new pathway
				gpair.put(lastpw, pairs);
				lastpw = words[0];
				pairs.clear(); pairs.add(new Pair<Integer,Integer>(index1,index2));
			}
		}
		
		// Put last pathway
		gpair.put(lastpw,pairs);
		fin.close();
		
		return gpair;
	}
	
	public static TreeMap < String, Vector<Integer> > getGeneList(String inputFile)
	throws FileNotFoundException,IOException {
		BufferedReader fin = new BufferedReader(new FileReader(inputFile));
		TreeMap < String, Vector <Integer> > glist = new TreeMap < String, Vector <Integer> >();
		
		// Initialize last pathways read 
		String lastpw = null, line, words[];
		Vector <Integer> genes = new Vector <Integer> ();
		
		while ((line = fin.readLine())!= null) {
			words = line.split(" ");
			Integer index = getGeneIndex(words[1]);
			if (lastpw == null) {
				// first time
				lastpw = words[0];
				genes.add(index);
			} else if (lastpw.equals(words[0])) {
				// same pathway
				genes.add(index);
			} else {
				// new pathway
				glist.put(lastpw, genes);
				lastpw = words[0];
				genes.clear(); genes.add(index);
			}
		}
		
		// Put last pathway
		glist.put(lastpw,genes);
		fin.close();
		
		return glist;
	}
	
	public static Double geneAgreementScore(String pathway1, String pathway2) {
		// Acquire 2 gene lists
		Vector <Integer> genes1 = glist1.get(pathway1),
						 genes2 = glist2.get(pathway2);
		// Intersection size
		int n_intersect = 0;
		
		// Get intersection size of 2 arrays (O(max(n1,n2)))
		HashSet<Integer> hash 	= new HashSet<Integer>();
		for (Integer i : genes1) hash.add(i);
		for (Integer j : genes2) if (hash.contains(j)) n_intersect++; 
		
		HypergeometricDistribution hyp = new HypergeometricDistribution(geneCount,(int)genes1.size(),(int)genes2.size());
		Double pvalue = hyp.cumulativeProbability(n_intersect);
		
		return pvalue;
	}
	
	public static Double genePairAgreementScore(String pathway1, String pathway2) {
		Vector < Pair <Integer,Integer> > pairs1 = gpair1.get(pathway1),
										  pairs2 = gpair2.get(pathway2);
		
		int n_intersect = 0;
		
		HashSet< Pair <Integer,Integer> > hash = new HashSet < Pair <Integer,Integer> >();
		
		for (Pair <Integer,Integer> i : pairs1) hash.add(i);
		for (Pair <Integer,Integer> j : pairs2) if (hash.contains(j)) n_intersect++;
		
		
		genePairCount = geneCount * (geneCount-1) / 2;
		
		HypergeometricDistribution hyp = new HypergeometricDistribution(genePairCount,(int)pairs1.size(),(int)pairs2.size());
		Double pvalue = hyp.cumulativeProbability(n_intersect);
		
		return pvalue;
	}
	
	public PathInt(String pw1, String ge1, String gp1, String gp2, String pw2, String ge2) 
	throws FileNotFoundException, IOException {
		plist1 = getPathwayList(pw1); 
		plist2 = getPathwayList(pw2);
		glist1 = getGeneList(ge1);
		glist2 = getGeneList(ge2);
		gpair1 = getPairList(gp1);
		gpair2 = getPairList(gp2);
		
		scoreArr = new Double[(int)plist1.size()][(int)plist2.size()][2];
		for (int i = 0; i < plist1.size(); i++) {
			for (int j = 0; j < plist2.size(); j++) {
				scoreArr[i][j][0] = geneAgreementScore(plist1.get(i),plist2.get(j));
				scoreArr[i][j][1] = genePairAgreementScore(plist1.get(i),plist2.get(j));
			}
		}
	}
}

class Pair <T1,T2> 
{
	public T1 first;
	public T2 second;
	
	public Pair(T1 first, T2 second) {
		this.first 	= first;
		this.second = second;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Pair))
			return false;
		
		Pair<?,?> other = (Pair <?,?>) o; 
		return (Objects.equals(first,other.first) && Objects.equals(second,other.second));
	}
}	