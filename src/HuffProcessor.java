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


	String[] extractedCodes = new String[ALPH_SIZE+1];
	
	private void extractCodes(HuffNode current, String path){			
		if (current.myLeft == null && current.myRight == null){
			extractedCodes[current.myValue]=path;
			return;
		}
		extractCodes(current.myLeft, path + "0");
		extractCodes(current.myRight, path + "1");
	}

	private void writeHeader(HuffNode current, BitOutputStream out){
		if (current.myLeft == null && current.myRight==null){
			out.writeBits(1, 1);
			out.writeBits(9,current.myValue);
			return;
		}
		out.writeBits(1,0);

		writeHeader(current.myLeft, out);
		writeHeader(current.myRight, out);
	}


	public void compress(BitInputStream in, BitOutputStream out) {	
		int[] countOccur = new int[ALPH_SIZE+1];
		
		int nextCharacter = in.readBits(BITS_PER_WORD);
		while(nextCharacter != -1){					
			countOccur[nextCharacter]++;
			nextCharacter = in.readBits(BITS_PER_WORD);
		}
		countOccur[PSEUDO_EOF] =1;
		
		in.reset();			

		PriorityQueue<HuffNode> queueNode = new PriorityQueue<HuffNode>();	
		for (int i = 0; i < ALPH_SIZE; i++){
			if (countOccur[i] > 0){
				queueNode.add(new HuffNode(i, countOccur[i]));
			}
		}
		queueNode.add(new HuffNode(PSEUDO_EOF, 0));	
		
		while (queueNode.size() > 1){		
			HuffNode left = queueNode.remove();
		    HuffNode right = queueNode.remove();

			int combinedWeight = left.myWeight + right.myWeight;
			queueNode.add(new HuffNode(-1, combinedWeight, left, right)); 
		}

		HuffNode root = queueNode.poll();
		extractCodes(root, "");					
		out.writeBits(BITS_PER_INT, HUFF_NUMBER);
		writeHeader(root, out);	

		int nextChar = in.readBits(BITS_PER_WORD);	
		while (nextChar != -1){
			String code = extractedCodes[nextChar];				
			out.writeBits(code.length(), Integer.parseInt(code, 2));
			nextChar = in.readBits(BITS_PER_WORD);
		}

		String code = extractedCodes[PSEUDO_EOF];			
		out.writeBits(code.length(), Integer.parseInt(code, 2));
		in.reset();										
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