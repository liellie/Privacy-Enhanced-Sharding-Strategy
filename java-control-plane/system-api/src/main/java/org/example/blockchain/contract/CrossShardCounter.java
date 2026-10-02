package org.example.blockchain.contract;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.9.8.
 */
@SuppressWarnings("rawtypes")
public class CrossShardCounter extends Contract {
    public static final String BINARY = "608060405234801561001057600080fd5b5060008054336001600160a01b031991821681178355600180549092161790556105c090819061004090396000f3fe608060405234801561001057600080fd5b506004361061009e5760003560e01c8063d7dba4ee11610066578063d7dba4ee146100fe578063d826f88f14610107578063f33930d91461010f578063f68becb71461013a578063f851a4401461014d57600080fd5b806303bce1b8146100a35780634310adb8146100b8578063504f6cf3146100cb5780636a146024146100e657806376155d80146100f5575b600080fd5b6100b66100b13660046104ae565b610160565b005b6100b66100c63660046104de565b6101d6565b6100d36102be565b6040519081526020015b60405180910390f35b6100d3670de0b6b3a764000081565b6100d360035481565b6100d360025481565b6100b66102f8565b600154610122906001600160a01b031681565b6040516001600160a01b0390911681526020016100dd565b6100b6610148366004610500565b61037c565b600054610122906001600160a01b031681565b6000546001600160a01b031633146101b45760405162461bcd60e51b815260206004820152601260248201527121b7bab73a32b91d103737ba1030b236b4b760711b60448201526064015b60405180910390fd5b600180546001600160a01b0319166001600160a01b0392909216919091179055565b6001546001600160a01b03163314806101f957506000546001600160a01b031633145b61023d5760405162461bcd60e51b815260206004820152601560248201527421b7bab73a32b91d103737ba103932b1b7b93232b960591b60448201526064016101ab565b6001600360008282546102509190610538565b909155505080156102745760016002600082825461026e9190610538565b90915550505b60025460035460408051841515815260208101939093528201527f3e558ee6cbfed86e242c5fa55c59430f60dfbbf0d9d2198e9aec2f7603a2507e9060600160405180910390a150565b60006003546000036102d05750600090565b600354670de0b6b3a76400006002546102e99190610551565b6102f39190610568565b905090565b6000546001600160a01b031633146103475760405162461bcd60e51b815260206004820152601260248201527121b7bab73a32b91d103737ba1030b236b4b760711b60448201526064016101ab565b6000600281905560038190556040517f6e5ed049f7c4af70a87a487198fa15a58fc46fb782b69e991acbd014c4ef3ff29190a1565b6001546001600160a01b031633148061039f57506000546001600160a01b031633145b6103e35760405162461bcd60e51b815260206004820152601560248201527421b7bab73a32b91d103737ba103932b1b7b93232b960591b60448201526064016101ab565b808211156104335760405162461bcd60e51b815260206004820152601c60248201527f436f756e7465723a2063726f7373206578636565647320746f74616c0000000060448201526064016101ab565b81600260008282546104459190610538565b92505081905550806003600082825461045e9190610538565b909155505060025460035460408051851515815260208101939093528201527f3e558ee6cbfed86e242c5fa55c59430f60dfbbf0d9d2198e9aec2f7603a2507e9060600160405180910390a15050565b6000602082840312156104c057600080fd5b81356001600160a01b03811681146104d757600080fd5b9392505050565b6000602082840312156104f057600080fd5b813580151581146104d757600080fd5b6000806040838503121561051357600080fd5b50508035926020909101359150565b634e487b7160e01b600052601160045260246000fd5b8082018082111561054b5761054b610522565b92915050565b808202811582820484141761054b5761054b610522565b60008261058557634e487b7160e01b600052601260045260246000fd5b50049056fea2646970667358221220f8d8117b3fe8a9fea16bc20319f3a97f38ca36623a57fbf40fc11fec8e3afccf64736f6c63430008130033";

    public static final String FUNC_WAD = "WAD";

    public static final String FUNC_ADMIN = "admin";

    public static final String FUNC_CROSSSHARDOPS = "crossShardOps";

    public static final String FUNC_CROSSSHARDRATIOWAD = "crossShardRatioWad";

    public static final String FUNC_RECORD = "record";

    public static final String FUNC_RECORDBATCH = "recordBatch";

    public static final String FUNC_RECORDER = "recorder";

    public static final String FUNC_RESET = "reset";

    public static final String FUNC_SETRECORDER = "setRecorder";

    public static final String FUNC_TOTALOPS = "totalOps";

    public static final Event COUNTERSRESET_EVENT = new Event("CountersReset", 
            Arrays.<TypeReference<?>>asList());
    ;

    public static final Event OPERATIONRECORDED_EVENT = new Event("OperationRecorded", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected CrossShardCounter(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected CrossShardCounter(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected CrossShardCounter(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected CrossShardCounter(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<CountersResetEventResponse> getCountersResetEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(COUNTERSRESET_EVENT, transactionReceipt);
        ArrayList<CountersResetEventResponse> responses = new ArrayList<CountersResetEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            CountersResetEventResponse typedResponse = new CountersResetEventResponse();
            typedResponse.log = eventValues.getLog();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static CountersResetEventResponse getCountersResetEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(COUNTERSRESET_EVENT, log);
        CountersResetEventResponse typedResponse = new CountersResetEventResponse();
        typedResponse.log = log;
        return typedResponse;
    }

    public Flowable<CountersResetEventResponse> countersResetEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getCountersResetEventFromLog(log));
    }

    public Flowable<CountersResetEventResponse> countersResetEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(COUNTERSRESET_EVENT));
        return countersResetEventFlowable(filter);
    }

    public static List<OperationRecordedEventResponse> getOperationRecordedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(OPERATIONRECORDED_EVENT, transactionReceipt);
        ArrayList<OperationRecordedEventResponse> responses = new ArrayList<OperationRecordedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OperationRecordedEventResponse typedResponse = new OperationRecordedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.isCrossShard = (Boolean) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.crossShardOps = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.totalOps = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static OperationRecordedEventResponse getOperationRecordedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(OPERATIONRECORDED_EVENT, log);
        OperationRecordedEventResponse typedResponse = new OperationRecordedEventResponse();
        typedResponse.log = log;
        typedResponse.isCrossShard = (Boolean) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.crossShardOps = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        typedResponse.totalOps = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<OperationRecordedEventResponse> operationRecordedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getOperationRecordedEventFromLog(log));
    }

    public Flowable<OperationRecordedEventResponse> operationRecordedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(OPERATIONRECORDED_EVENT));
        return operationRecordedEventFlowable(filter);
    }

    public RemoteFunctionCall<BigInteger> WAD() {
        final Function function = new Function(FUNC_WAD, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<String> admin() {
        final Function function = new Function(FUNC_ADMIN, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<BigInteger> crossShardOps() {
        final Function function = new Function(FUNC_CROSSSHARDOPS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> crossShardRatioWad() {
        final Function function = new Function(FUNC_CROSSSHARDRATIOWAD, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> record(Boolean isCrossShard) {
        final Function function = new Function(
                FUNC_RECORD, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Bool(isCrossShard)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> recordBatch(BigInteger crossCount, BigInteger total) {
        final Function function = new Function(
                FUNC_RECORDBATCH, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(crossCount), 
                new org.web3j.abi.datatypes.generated.Uint256(total)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> recorder() {
        final Function function = new Function(FUNC_RECORDER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> reset() {
        final Function function = new Function(
                FUNC_RESET, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setRecorder(String who) {
        final Function function = new Function(
                FUNC_SETRECORDER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, who)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> totalOps() {
        final Function function = new Function(FUNC_TOTALOPS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    @Deprecated
    public static CrossShardCounter load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new CrossShardCounter(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static CrossShardCounter load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new CrossShardCounter(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static CrossShardCounter load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new CrossShardCounter(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static CrossShardCounter load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new CrossShardCounter(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<CrossShardCounter> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(CrossShardCounter.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    public static RemoteCall<CrossShardCounter> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(CrossShardCounter.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<CrossShardCounter> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(CrossShardCounter.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<CrossShardCounter> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(CrossShardCounter.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static class CountersResetEventResponse extends BaseEventResponse {
    }

    public static class OperationRecordedEventResponse extends BaseEventResponse {
        public Boolean isCrossShard;

        public BigInteger crossShardOps;

        public BigInteger totalOps;
    }
}
