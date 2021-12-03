import java.time.temporal.TemporalAccessor;
import java.util.*;

public class TxHandler {

    private UTXOPool utxoPool;

    public UTXOPool getUtxoPool() {
        return utxoPool;
    }

    public void setUtxoPool(UTXOPool utxoPool) {
        this.utxoPool = utxoPool;
    }

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. 
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        UTXO utxo;
        UTXOPool newPool = new UTXOPool();
        int ipSum=0;
        for (int i=0; i<tx.numInputs(); i++){
            utxo = new UTXO(tx.getInput(i).prevTxHash, tx.getInput(i).outputIndex);
            //utxo in the utxoPool or not.
            if (!utxoPool.contains(utxo)){
                System.out.println("Can't find the UTXO in the UTXOPool");
                return false;
            }

            //verify signature of the tx
            //sig = sign(key, msg)
            //isvalid = verify(key, msg, sig) = verify(key, msg, (sign(key,msg)) )
            boolean isValidTx = Crypto.verifySignature(utxoPool.getTxOutput(utxo).address, tx.getRawDataToSign(i), tx.getInput(i).signature);
            if (!isValidTx){
                System.out.println("Signature is invalid");
                return false;
            }

            //after verify the output existence and the signature we add the utxo to new UTXOPool to
            // avoid repeating the utxo
            if (!newPool.contains(utxo))
                newPool.addUTXO(utxo,utxoPool.getTxOutput(utxo));
            else{
                System.out.println("UTXO is already exists in the UTXOPool");
                return false;
            }
            ipSum += utxoPool.getTxOutput(utxo).value;
        }
        //ip = op
        int opSum = 0;
        for (int i=0; i< tx.numOutputs(); i++){
            if(tx.getOutput(i).value<0)
                return false;
            opSum += tx.getOutput(i).value;
        }
        if (ipSum<opSum)
            return false;
        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        List<Transaction> validTxs = new ArrayList<>();

        HashSet<Transaction> txs = new HashSet<Transaction>(Arrays.asList(possibleTxs));

        int counter = 0;

        while (validTxs.size() != possibleTxs.length){
            HashSet<Transaction> TxsToBeRemoved = new HashSet<>();
            for (Transaction tx : txs){
                if (isValidTx(tx)){
                    validTxs.add(tx);
                    TxsToBeRemoved.add(tx);

                    for (int k=0; k<tx.numInputs(); k++){
                        utxoPool.removeUTXO(new UTXO(tx.getInput(k).prevTxHash, tx.getInput(k).outputIndex));
                    }
                    for (int k=0; k<tx.numOutputs(); k++){
                        utxoPool.addUTXO(new UTXO(tx.getHash(),k),tx.getOutput(k));
                    }
                }
            }
            for (Transaction rtx : TxsToBeRemoved)
                txs.remove(rtx);

            if (counter++ >= possibleTxs.length) break;
        }

        Transaction[] result = new Transaction[validTxs.size()];
        result = validTxs.toArray(result);                      //mesh metzawed fely etsalem
        return result;
    }

}
