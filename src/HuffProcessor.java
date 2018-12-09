import java.util.PriorityQueue;

/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 * 
 * @author Owen Astrachan
 */

public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private final int myDebugLevel;
	
	public static final int DEBUG_HIGH = 4;
	public static final int DEBUG_LOW = 1;
	
	public HuffProcessor() {
		this(0);
	}
	
	public HuffProcessor(int debug) {
		myDebugLevel = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */


	public void compress(BitInputStream in, BitOutputStream out){
		
		int [] counts = readforCounts(in);
		
		HuffNode root = makeTreeFtomCounts(counts);
		
		
		String [] codings = makeCodingsFromTree(root);
		
		out.writeBits(BITS_PER_INT, HUFF_TREE);
		
		writeHeader(root,out);
		
		
		in.reset();
		
		writeCompressedBits(codings,in,out);
		out.close();
		
	}
		
		
		
	
		
		

private void writeCompressedBits(String[] codings, BitInputStream in, BitOutputStream out) {
		

	int nextChar = in.readBits(BITS_PER_WORD);	
	while (nextChar != -1){
		String code = encodings[nextChar];				
		out.writeBits(code.length(), Integer.parseInt(code, 2));
		nextChar = in.readBits(BITS_PER_WORD);
	}

	String code = encodings[PSEUDO_EOF];			
	out.writeBits(code.length(), Integer.parseInt(code, 2));

		
	}

public void writeHeader(HuffNode root, BitOutputStream out){
		if (root.myLeft == null && root.myRight==null){
			out.writeBits(1, 1);
			out.writeBits(9,root.myValue);
			return;
		}
		out.writeBits(1,0);

		writeHeader(root.myLeft, out);
		writeHeader(root.myRight, out);
	}


public String[] makeCodingsFromTree(HuffNode root) {
		
	  codingHelper(root,"");

		return null;
	}

String[] encodings = new String[ALPH_SIZE+1];

public void codingHelper(HuffNode root, String path) {
	
	
	if (root.myLeft == null && root.myRight == null){
		
		encodings[root.myValue]=path;
		return;
	}
	
	codingHelper(root.myLeft, path + "0");
	codingHelper(root.myRight, path + "1");
}
	
	
	
	


private HuffNode makeTreeFtomCounts(int[] counts) {
	
	
	PriorityQueue<HuffNode> pq = new PriorityQueue<>();


	for (int i = 0; i < ALPH_SIZE; i++){
	    
		if (counts[i] > 0){
			pq.add(new HuffNode(i, counts[i]));
		}
	}
	
	pq.add(new HuffNode(PSEUDO_EOF, 0));  //adds the PSEUDO_EOF for the last character
	
	

	while (pq.size() > 1) {
	    HuffNode left = pq.remove();
	    HuffNode right = pq.remove();
	    int finalWeight = left.myWeight + right.myWeight;
	    // left.weight+right.weight and left, right subtrees
	    pq.add(new HuffNode(-1, finalWeight, left, right));
	}
	HuffNode root = pq.remove();
	return root;
	
}
	
	


//		while (true){
//			int val = in.readBits(BITS_PER_WORD);
//			if (val == -1) break;
//			out.writeBits(BITS_PER_WORD, val);
//		}
//		out.close();
//		
		
		
		
	private int[] readforCounts(BitInputStream in) {
		
		int[] countOccur = new int[ALPH_SIZE+1];
		
		int nextCharacter = in.readBits(BITS_PER_WORD);
		
		while(nextCharacter != -1){					
			countOccur[nextCharacter]++;
			nextCharacter = in.readBits(BITS_PER_WORD);
		}
		
		
		
		
		return null;
	}







	
	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void decompress(BitInputStream in, BitOutputStream out){

		int bits = in.readBits(BITS_PER_INT);
		
		if (bits != HUFF_TREE) {
			throw new HuffException("illegal header starts with "+bits);
		}
		
		
		HuffNode root = readTreeHeader(in);
		
		readCompressBits(root,in,out);
		
		out.close();

}

	private void readCompressBits(HuffNode root, BitInputStream in, BitOutputStream out) {

			   HuffNode current = root; 
			   while (true) {
				   int bits = in.readBits(1);
			       if (bits == -1) {
			           throw new HuffException("bad input, no PSEUDO_EOF");
			       }
			       else { 
			           if (bits == 0) current = current.myLeft;
			      else current = current.myRight;

			           if (current.myLeft == null && current.myRight == null){	
			               if (current.myValue == PSEUDO_EOF) 
			                   break;   // out of loop
			               else {
			            	   out.writeBits(BITS_PER_WORD,current.myValue);
			                   current = root; // start back after leaf
			               }
			           }
			       }
			   }
		
		
		
		
	}

	private HuffNode readTreeHeader(BitInputStream in) {
		
//			if (in.readBits(1) == -1) {
//				throw new HuffException("bit is equal to -1");
//			}   //throws exception when equal to -1

			if (in.readBits(1) == 0){  //internal node so we recurse more
				HuffNode left = readTreeHeader(in);
				HuffNode right = readTreeHeader(in);
				return new HuffNode(-1, 1, left, right);
			} else {
				return new HuffNode(in.readBits(BITS_PER_WORD+1), 0); //we are at the leaf
			}


	}
	
	
	
}