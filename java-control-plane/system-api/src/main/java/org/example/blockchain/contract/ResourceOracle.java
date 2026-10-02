package org.example.blockchain.contract;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
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
import org.web3j.tuples.generated.Tuple4;
import org.web3j.tuples.generated.Tuple5;
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
public class ResourceOracle extends Contract {
    public static final String BINARY = "60a060405234801561001057600080fd5b5060405161132138038061132183398101604081905261002f916100e9565b600082116100845760405162461bcd60e51b815260206004820152601860248201527f4f7261636c653a206261642065706f6368206c656e677468000000000000000060448201526064015b60405180910390fd5b600081116100c95760405162461bcd60e51b81526020600482015260126024820152714f7261636c653a206261642071756f72756d60701b604482015260640161007b565b600080546001600160a01b0319163317905560809190915260035561010d565b600080604083850312156100fc57600080fd5b505080516020909101519092909150565b6080516111f261012f60003960008181610231015261092801526111f26000f3fe608060405234801561001057600080fd5b506004361061010b5760003560e01c80635cb83c2a116100a25780638724d9e7116100715780638724d9e7146102ac578063c1ba4e59146102e0578063ca6d56dc146102f3578063e636d84b14610306578063f851a4401461032957600080fd5b80635cb83c2a146102535780637667180814610266578063790c4d711461026e5780637eaddf7e1461028157600080fd5b8063317ad33c116100de578063317ad33c146101d05780633beb5f8c1461020e5780634772ebdc1461022357806357d775f81461022c57600080fd5b80630ec09ffe1461011057806311aee380146101755780631703a0181461018c57806324fb41d914610195575b600080fd5b61015061011e366004610ff3565b600760209081526000928352604080842090915290825290208054600182015460028301546003909301549192909184565b6040805194855260208501939093529183015260608201526080015b60405180910390f35b61017e60025481565b60405190815260200161016c565b61017e60035481565b6101a86101a3366004610ff3565b610354565b604080519586526020860194909452928401919091526060830152608082015260a00161016c565b6101fe6101de366004610ff3565b600960209081526000928352604080842090915290825290205460ff1681565b604051901515815260200161016c565b61022161021c366004610ff3565b610411565b005b61017e61271081565b61017e7f000000000000000000000000000000000000000000000000000000000000000081565b61017e610261366004610ff3565b610900565b61017e610921565b61022161027c366004611015565b610952565b61017e61028f366004610ff3565b600860209081526000928352604080842090915290825290205481565b6101fe6102ba36600461107d565b600660209081526000938452604080852082529284528284209052825290205460ff1681565b6102216102ee3660046110b2565b610c33565b6102216103013660046110cb565b610cd9565b6101fe6103143660046110cb565b60016020526000908152604090205460ff1681565b60005461033c906001600160a01b031681565b6040516001600160a01b03909116815260200161016c565b6000828152600960209081526040808320848452909152812054819081908190819060ff166103c25760405162461bcd60e51b815260206004820152601560248201527413dc9858db194e881b9bdd08199a5b985b1a5e9959605a1b60448201526064015b60405180910390fd5b5050506000848152600760209081526040808320868452825280832080546001820154600283015460039093015499865260088552838620988652979093529220549096949591949193509150565b600082815260096020908152604080832084845290915290205460ff16156104775760405162461bcd60e51b815260206004820152601960248201527813dc9858db194e88185b1c9958591e48199a5b985b1a5e9959603a1b60448201526064016103b9565b60008281526004602090815260408083208484529091529020546003548110156104e35760405162461bcd60e51b815260206004820152601a60248201527f4f7261636c653a2071756f72756d206e6f74207265616368656400000000000060448201526064016103b9565b60008167ffffffffffffffff8111156104fe576104fe6110e6565b604051908082528060200260200182016040528015610527578160200160208202803683370190505b50905060008267ffffffffffffffff811115610545576105456110e6565b60405190808252806020026020018201604052801561056e578160200160208202803683370190505b50905060008367ffffffffffffffff81111561058c5761058c6110e6565b6040519080825280602002602001820160405280156105b5578160200160208202803683370190505b50905060008467ffffffffffffffff8111156105d3576105d36110e6565b6040519080825280602002602001820160405280156105fc578160200160208202803683370190505b50905060008567ffffffffffffffff81111561061a5761061a6110e6565b604051908082528060200260200182016040528015610643578160200160208202803683370190505b50905060005b8681101561077d5760008981526004602090815260408083208b8452909152812080548390811061067c5761067c6110fc565b9060005260206000209060040201905080600001548783815181106106a3576106a36110fc565b60200260200101818152505080600101548683815181106106c6576106c66110fc565b60200260200101818152505080600201548583815181106106e9576106e96110fc565b602002602001018181525050806003015484838151811061070c5761070c6110fc565b60209081029190910181019190915260008b81526005825260408082208c835290925220805483908110610742576107426110fc565b906000526020600020015483838151811061075f5761075f6110fc565b6020908102919091010152508061077581611128565b915050610649565b506000604051806080016040528061079488610e50565b81526020016107a287610e50565b81526020016107b086610e50565b81526020016107be85610e50565b9052905060006107cd83610e50565b905081600760008c815260200190815260200160002060008b81526020019081526020016000206000820151816000015560208201518160010155604082015181600201556060820151816003015590505080600860008c815260200190815260200160002060008b8152602001908152602001600020819055506001600960008c815260200190815260200160002060008b815260200190815260200160002060006101000a81548160ff021916908315150217905550888a7ff49b55b54ddf39576cfaee28f07dee65a45681cd6693955bd3567464a4a257fe8460000151856020015186604001518760600151876040516108ec959493929190948552602085019390935260408401919091526060830152608082015260a00190565b60405180910390a350505050505050505050565b60008281526004602090815260408083208484529091529020545b92915050565b600061094d7f000000000000000000000000000000000000000000000000000000000000000043611157565b905090565b3360009081526001602052604090205460ff166109a85760405162461bcd60e51b815260206004820152601460248201527327b930b1b6329d103737ba10309036b2b6b132b960611b60448201526064016103b9565b600087815260096020908152604080832089845290915290205460ff1615610a0e5760405162461bcd60e51b815260206004820152601960248201527813dc9858db194e88185b1c9958591e48199a5b985b1a5e9959603a1b60448201526064016103b9565b6000878152600660209081526040808320898452825280832033845290915290205460ff1615610a805760405162461bcd60e51b815260206004820152601960248201527f4f7261636c653a20616c7265616479207375626d69747465640000000000000060448201526064016103b9565b6127108511158015610a9457506127108411155b8015610aa257506127108311155b8015610ab057506127108211155b610afc5760405162461bcd60e51b815260206004820152601c60248201527f4f7261636c653a2072656164696e67206f7574206f662072616e67650000000060448201526064016103b9565b612710811115610b4e5760405162461bcd60e51b815260206004820152601f60248201527f4f7261636c653a2070726564696374696f6e206f7574206f662072616e67650060448201526064016103b9565b6000878152600660209081526040808320898452825280832033808552908352818420805460ff191660019081179091558b855260048085528386208c8752855283862084516080810186528c81528087018c81528187018c8152606083018c815284548088018655948b52898b20935194909502909201928355518285015551600282015590516003909101558b8552600584528285208b8652845282852080549182018155855292842090920184905551909188918a917ff360ebc6df72493e0641630aded9701f53add2b87b173984e5cee28ca121b1c591a450505050505050565b6000546001600160a01b03163314610c815760405162461bcd60e51b815260206004820152601160248201527027b930b1b6329d103737ba1030b236b4b760791b60448201526064016103b9565b600081118015610c9357506002548111155b610cd45760405162461bcd60e51b81526020600482015260126024820152714f7261636c653a206261642071756f72756d60701b60448201526064016103b9565b600355565b6000546001600160a01b03163314610d275760405162461bcd60e51b815260206004820152601160248201527027b930b1b6329d103737ba1030b236b4b760791b60448201526064016103b9565b6001600160a01b038116610d745760405162461bcd60e51b81526020600482015260146024820152734f7261636c653a207a65726f206164647265737360601b60448201526064016103b9565b6001600160a01b03811660009081526001602052604090205460ff1615610ddd5760405162461bcd60e51b815260206004820152601860248201527f4f7261636c653a20616c72656164792061206d656d626572000000000000000060448201526064016103b9565b6001600160a01b03811660009081526001602081905260408220805460ff1916821790556002805491929091610e1490849061116b565b90915550506040516001600160a01b038216907fb251eb052afc73ffd02ffe85ad79990a8b3fed60d76dbc2fa2fdd7123dffd91490600090a250565b805160009060015b81811015610f42576000848281518110610e7457610e746110fc565b6020026020010151905060008290505b600081118015610eb657508186610e9c60018461117e565b81518110610eac57610eac6110fc565b6020026020010151115b15610f0e5785610ec760018361117e565b81518110610ed757610ed76110fc565b6020026020010151868281518110610ef157610ef16110fc565b602090810291909101015280610f0681611191565b915050610e84565b81868281518110610f2157610f216110fc565b60200260200101818152505050508080610f3a90611128565b915050610e58565b50610f4e6002826111a8565b600103610f815782610f61600283611157565b81518110610f7157610f716110fc565b6020026020010151915050919050565b600283610f8e8284611157565b81518110610f9e57610f9e6110fc565b6020026020010151846001600285610fb69190611157565b610fc0919061117e565b81518110610fd057610fd06110fc565b6020026020010151610fe2919061116b565b610fec9190611157565b9392505050565b6000806040838503121561100657600080fd5b50508035926020909101359150565b600080600080600080600060e0888a03121561103057600080fd5b505085359760208701359750604087013596606081013596506080810135955060a0810135945060c0013592509050565b80356001600160a01b038116811461107857600080fd5b919050565b60008060006060848603121561109257600080fd5b83359250602084013591506110a960408501611061565b90509250925092565b6000602082840312156110c457600080fd5b5035919050565b6000602082840312156110dd57600080fd5b610fec82611061565b634e487b7160e01b600052604160045260246000fd5b634e487b7160e01b600052603260045260246000fd5b634e487b7160e01b600052601160045260246000fd5b60006001820161113a5761113a611112565b5060010190565b634e487b7160e01b600052601260045260246000fd5b60008261116657611166611141565b500490565b8082018082111561091b5761091b611112565b8181038181111561091b5761091b611112565b6000816111a0576111a0611112565b506000190190565b6000826111b7576111b7611141565b50069056fea2646970667358221220797a861644ea3f53c91db45c1eae00f95cbd2507bd155753fe56c7ff738c315f64736f6c63430008130033";

    public static final String FUNC_SCALE_MAX = "SCALE_MAX";

    public static final String FUNC_ADDMEMBER = "addMember";

    public static final String FUNC_ADMIN = "admin";

    public static final String FUNC_CURRENTEPOCH = "currentEpoch";

    public static final String FUNC_EPOCHLENGTH = "epochLength";

    public static final String FUNC_FINALIZESHARD = "finalizeShard";

    public static final String FUNC_FINALIZEDPREDICTION = "finalizedPrediction";

    public static final String FUNC_FINALIZEDREADING = "finalizedReading";

    public static final String FUNC_GETFINALIZEDREADING = "getFinalizedReading";

    public static final String FUNC_HASSUBMITTED = "hasSubmitted";

    public static final String FUNC_ISCOMMITTEEMEMBER = "isCommitteeMember";

    public static final String FUNC_ISFINALIZED = "isFinalized";

    public static final String FUNC_MEMBERCOUNT = "memberCount";

    public static final String FUNC_QUORUM = "quorum";

    public static final String FUNC_SETQUORUM = "setQuorum";

    public static final String FUNC_SUBMISSIONCOUNT = "submissionCount";

    public static final String FUNC_SUBMIT = "submit";

    public static final Event FINALIZED_EVENT = new Event("Finalized", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event MEMBERADDED_EVENT = new Event("MemberAdded", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}));
    ;

    public static final Event SUBMITTED_EVENT = new Event("Submitted", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>(true) {}, new TypeReference<Address>(true) {}));
    ;

    @Deprecated
    protected ResourceOracle(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected ResourceOracle(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected ResourceOracle(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected ResourceOracle(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<FinalizedEventResponse> getFinalizedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(FINALIZED_EVENT, transactionReceipt);
        ArrayList<FinalizedEventResponse> responses = new ArrayList<FinalizedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            FinalizedEventResponse typedResponse = new FinalizedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.epoch = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.shardId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.cpu = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.mem = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.stor = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            typedResponse.net = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
            typedResponse.prediction = (BigInteger) eventValues.getNonIndexedValues().get(4).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static FinalizedEventResponse getFinalizedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(FINALIZED_EVENT, log);
        FinalizedEventResponse typedResponse = new FinalizedEventResponse();
        typedResponse.log = log;
        typedResponse.epoch = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.shardId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.cpu = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.mem = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        typedResponse.stor = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
        typedResponse.net = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
        typedResponse.prediction = (BigInteger) eventValues.getNonIndexedValues().get(4).getValue();
        return typedResponse;
    }

    public Flowable<FinalizedEventResponse> finalizedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getFinalizedEventFromLog(log));
    }

    public Flowable<FinalizedEventResponse> finalizedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(FINALIZED_EVENT));
        return finalizedEventFlowable(filter);
    }

    public static List<MemberAddedEventResponse> getMemberAddedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(MEMBERADDED_EVENT, transactionReceipt);
        ArrayList<MemberAddedEventResponse> responses = new ArrayList<MemberAddedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            MemberAddedEventResponse typedResponse = new MemberAddedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.who = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static MemberAddedEventResponse getMemberAddedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(MEMBERADDED_EVENT, log);
        MemberAddedEventResponse typedResponse = new MemberAddedEventResponse();
        typedResponse.log = log;
        typedResponse.who = (String) eventValues.getIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<MemberAddedEventResponse> memberAddedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getMemberAddedEventFromLog(log));
    }

    public Flowable<MemberAddedEventResponse> memberAddedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(MEMBERADDED_EVENT));
        return memberAddedEventFlowable(filter);
    }

    public static List<SubmittedEventResponse> getSubmittedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(SUBMITTED_EVENT, transactionReceipt);
        ArrayList<SubmittedEventResponse> responses = new ArrayList<SubmittedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            SubmittedEventResponse typedResponse = new SubmittedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.epoch = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.shardId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.who = (String) eventValues.getIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static SubmittedEventResponse getSubmittedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(SUBMITTED_EVENT, log);
        SubmittedEventResponse typedResponse = new SubmittedEventResponse();
        typedResponse.log = log;
        typedResponse.epoch = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.shardId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.who = (String) eventValues.getIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<SubmittedEventResponse> submittedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getSubmittedEventFromLog(log));
    }

    public Flowable<SubmittedEventResponse> submittedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SUBMITTED_EVENT));
        return submittedEventFlowable(filter);
    }

    public RemoteFunctionCall<BigInteger> SCALE_MAX() {
        final Function function = new Function(FUNC_SCALE_MAX, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> addMember(String who) {
        final Function function = new Function(
                FUNC_ADDMEMBER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, who)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> admin() {
        final Function function = new Function(FUNC_ADMIN, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<BigInteger> currentEpoch() {
        final Function function = new Function(FUNC_CURRENTEPOCH, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> epochLength() {
        final Function function = new Function(FUNC_EPOCHLENGTH, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> finalizeShard(BigInteger epoch, BigInteger shardId) {
        final Function function = new Function(
                FUNC_FINALIZESHARD, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(epoch), 
                new org.web3j.abi.datatypes.generated.Uint256(shardId)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> finalizedPrediction(BigInteger param0, BigInteger param1) {
        final Function function = new Function(FUNC_FINALIZEDPREDICTION, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0), 
                new org.web3j.abi.datatypes.generated.Uint256(param1)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>> finalizedReading(BigInteger param0, BigInteger param1) {
        final Function function = new Function(FUNC_FINALIZEDREADING, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0), 
                new org.web3j.abi.datatypes.generated.Uint256(param1)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
        return new RemoteFunctionCall<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>>(function,
                new Callable<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>>() {
                    @Override
                    public Tuple4<BigInteger, BigInteger, BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>(
                                (BigInteger) results.get(0).getValue(), 
                                (BigInteger) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue(), 
                                (BigInteger) results.get(3).getValue());
                    }
                });
    }

    public RemoteFunctionCall<Tuple5<BigInteger, BigInteger, BigInteger, BigInteger, BigInteger>> getFinalizedReading(BigInteger epoch, BigInteger shardId) {
        final Function function = new Function(FUNC_GETFINALIZEDREADING, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(epoch), 
                new org.web3j.abi.datatypes.generated.Uint256(shardId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
        return new RemoteFunctionCall<Tuple5<BigInteger, BigInteger, BigInteger, BigInteger, BigInteger>>(function,
                new Callable<Tuple5<BigInteger, BigInteger, BigInteger, BigInteger, BigInteger>>() {
                    @Override
                    public Tuple5<BigInteger, BigInteger, BigInteger, BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple5<BigInteger, BigInteger, BigInteger, BigInteger, BigInteger>(
                                (BigInteger) results.get(0).getValue(), 
                                (BigInteger) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue(), 
                                (BigInteger) results.get(3).getValue(), 
                                (BigInteger) results.get(4).getValue());
                    }
                });
    }

    public RemoteFunctionCall<Boolean> hasSubmitted(BigInteger param0, BigInteger param1, String param2) {
        final Function function = new Function(FUNC_HASSUBMITTED, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0), 
                new org.web3j.abi.datatypes.generated.Uint256(param1), 
                new org.web3j.abi.datatypes.Address(160, param2)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<Boolean> isCommitteeMember(String param0) {
        final Function function = new Function(FUNC_ISCOMMITTEEMEMBER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<Boolean> isFinalized(BigInteger param0, BigInteger param1) {
        final Function function = new Function(FUNC_ISFINALIZED, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0), 
                new org.web3j.abi.datatypes.generated.Uint256(param1)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<BigInteger> memberCount() {
        final Function function = new Function(FUNC_MEMBERCOUNT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> quorum() {
        final Function function = new Function(FUNC_QUORUM, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> setQuorum(BigInteger q) {
        final Function function = new Function(
                FUNC_SETQUORUM, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(q)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> submissionCount(BigInteger epoch, BigInteger shardId) {
        final Function function = new Function(FUNC_SUBMISSIONCOUNT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(epoch), 
                new org.web3j.abi.datatypes.generated.Uint256(shardId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> submit(BigInteger epoch, BigInteger shardId, BigInteger cpu, BigInteger mem, BigInteger stor, BigInteger net, BigInteger prediction) {
        final Function function = new Function(
                FUNC_SUBMIT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(epoch), 
                new org.web3j.abi.datatypes.generated.Uint256(shardId), 
                new org.web3j.abi.datatypes.generated.Uint256(cpu), 
                new org.web3j.abi.datatypes.generated.Uint256(mem), 
                new org.web3j.abi.datatypes.generated.Uint256(stor), 
                new org.web3j.abi.datatypes.generated.Uint256(net), 
                new org.web3j.abi.datatypes.generated.Uint256(prediction)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static ResourceOracle load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new ResourceOracle(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static ResourceOracle load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new ResourceOracle(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static ResourceOracle load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new ResourceOracle(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static ResourceOracle load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new ResourceOracle(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<ResourceOracle> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, BigInteger _epochLength, BigInteger _quorum) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_epochLength), 
                new org.web3j.abi.datatypes.generated.Uint256(_quorum)));
        return deployRemoteCall(ResourceOracle.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<ResourceOracle> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, BigInteger _epochLength, BigInteger _quorum) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_epochLength), 
                new org.web3j.abi.datatypes.generated.Uint256(_quorum)));
        return deployRemoteCall(ResourceOracle.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<ResourceOracle> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, BigInteger _epochLength, BigInteger _quorum) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_epochLength), 
                new org.web3j.abi.datatypes.generated.Uint256(_quorum)));
        return deployRemoteCall(ResourceOracle.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<ResourceOracle> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, BigInteger _epochLength, BigInteger _quorum) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_epochLength), 
                new org.web3j.abi.datatypes.generated.Uint256(_quorum)));
        return deployRemoteCall(ResourceOracle.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static class FinalizedEventResponse extends BaseEventResponse {
        public BigInteger epoch;

        public BigInteger shardId;

        public BigInteger cpu;

        public BigInteger mem;

        public BigInteger stor;

        public BigInteger net;

        public BigInteger prediction;
    }

    public static class MemberAddedEventResponse extends BaseEventResponse {
        public String who;
    }

    public static class SubmittedEventResponse extends BaseEventResponse {
        public BigInteger epoch;

        public BigInteger shardId;

        public String who;
    }
}
