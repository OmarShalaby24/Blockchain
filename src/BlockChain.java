// The BlockChain class should maintain only limited block nodes to satisfy the functionality.
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.security.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;

    public class Node{
        Block currentBlock;
        Node parentNode;
        ArrayList<Node> childrenNodes;
        int height;

        UTXOPool utxoPool;

        public Node(Block block, Node parent, UTXOPool utxoPool){
            this.currentBlock = block;
            this.parentNode = parent;
            childrenNodes = new ArrayList<>();
            this.utxoPool = utxoPool;

            if(parent==null)
                this.height = 1;                 //genesis block
            else {
                this.height = parent.height+1;
                parentNode.childrenNodes.add(this);
            }
        }
    }


    /**
     * create an empty blockchain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    HashMap<ByteArrayWrapper, Node> blockchain;
    Node lastNode;
    TransactionPool txPool;
    public BlockChain(Block genesisBlock) {
        // IMPLEMENT THIS
        blockchain = new HashMap<>();
        txPool = new TransactionPool();

        UTXOPool utxoPool = new UTXOPool();
        for (int i=0; i<genesisBlock.getCoinbase().numOutputs(); i++){
            UTXO utxo = new UTXO(genesisBlock.getCoinbase().getHash(), i);
            utxoPool.addUTXO(utxo,genesisBlock.getCoinbase().getOutput(i));
        }
        Node genesisNode = new Node(genesisBlock, null, utxoPool);
        lastNode = genesisNode;
        blockchain.put(new ByteArrayWrapper(genesisBlock.getHash()),genesisNode);

    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        // IMPLEMENT THIS
        return lastNode.currentBlock;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        // IMPLEMENT THIS
        return lastNode.utxoPool;
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        // IMPLEMENT THIS
        return txPool;
    }

    /**
     * Add {@code block} to the blockchain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}, where maxHeight is 
     * the current height of the blockchain.
	 * <p>
	 * Assume the Genesis block is at height 1.
     * For example, you can try creating a new block over the genesis block (i.e. create a block at 
	 * height 2) if the current blockchain height is less than or equal to CUT_OFF_AGE + 1. As soon as
	 * the current blockchain height exceeds CUT_OFF_AGE + 1, you cannot create a new block at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        // IMPLEMENT THIS
        //law el prev hash bta3 el block ely ablo b null aw el parent Node b null
        Node parentNode = blockchain.get(new ByteArrayWrapper(block.getPrevBlockHash()));
        if(block.getPrevBlockHash() == null || parentNode==null)
            return false;

        TxHandler txHandler = new TxHandler(parentNode.utxoPool);
        UTXOPool utxoPool = txHandler.getUtxoPool();
        Node newNode = new Node(block,parentNode,utxoPool);

        //if the block is too old
        if(newNode.height <= lastNode.height - CUT_OFF_AGE)
            return false;
        Transaction[] txs = block.getTransactions().toArray(new Transaction[0]);
        Transaction[] validTxs = txHandler.handleTxs(txs);
        if (validTxs.length!=block.getTransactions().size())
            return false;

        for (int i=0; i<block.getCoinbase().numOutputs(); i++){
            UTXO utxo = new UTXO(block.getCoinbase().getHash(), i);
            utxoPool.addUTXO(utxo,block.getCoinbase().getOutput(i));
        }

        //remove txs in block from txPool
        for (int i=0; i<block.getTransactions().size(); i++)
            txPool.removeTransaction(block.getTransaction(i).getHash());

        newNode.utxoPool = utxoPool;
        blockchain.put(new ByteArrayWrapper(block.getHash()),newNode);
        if(newNode.height > lastNode.height)
            lastNode = newNode;

        if (lastNode.height>CUT_OFF_AGE){
            ArrayList<ByteArrayWrapper> NodesToBeRemoved = new ArrayList<>();
            for (Map.Entry<ByteArrayWrapper,Node> entry : blockchain.entrySet()){
                if(entry.getValue().height< lastNode.height - CUT_OFF_AGE){
                    NodesToBeRemoved.add(entry.getKey());
                }
            }
            for (ByteArrayWrapper hash : NodesToBeRemoved){
                for (Node child : blockchain.get(hash).childrenNodes){
                    child.parentNode = null;
                }
                blockchain.remove(hash);
            }
        }

        return true;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        // IMPLEMENT THIS
        txPool.addTransaction(tx);
    }



}