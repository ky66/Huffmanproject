
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

		while (true){
			int val = in.readBits(BITS_PER_WORD);
			if (val == -1) break;
			out.writeBits(BITS_PER_WORD, val);
		}
		out.close();
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

	public void decompress(BitInputStream in, BitOutputStream out) {
		if (in.readBits(BITS_PER_INT)!= HUFF_TREE){	
			throw new HuffException("Illegal Header");
		}

		HuffNode root = readTreeHeader(in);		//uses the helper method to read the trie
		
		HuffNode current = root;   //we will need current for navigating in the tree without messing up the root
		int nextChar = in.readBits(1);		
		while (nextChar !=-1){
			if (nextChar == 1){					
				current = current.myRight;
			}
			else {
				current = current.myLeft;
			}

			if (current.myLeft == null && current.myRight == null){		//If we're at a leaf node
				if (current.myValue == PSEUDO_EOF){ //if we reach the end, we just return here
					return;
				} else {		
					out.writeBits(BITS_PER_WORD,current.myValue); //writes it to the output file
					current = root;									
				}
			}
			nextChar = in.readBits(1);		
		}
		out.close();
	}

		
		
		
		
//		while (true){
//			int val = in.readBits(BITS_PER_WORD);
//			if (val == -1) break;
//			out.writeBits(BITS_PER_WORD, val);
//		}
//		out.close();
//	}


	private HuffNode readTreeHeader(BitInputStream in) {
		
			if (in.readBits(1) == -1) {
				throw new HuffException("bit is equal to -1");
			}   //throws exception when equal to -1

			if (in.readBits(1) == 0){  //internal node so we recurse more
				HuffNode left = readTreeHeader(in);
				HuffNode right = readTreeHeader(in);
				return new HuffNode(-1, 1, left, right);
			} else {
				return new HuffNode(in.readBits(BITS_PER_WORD+1), 0); //we are at the leaf
			}


	}
}