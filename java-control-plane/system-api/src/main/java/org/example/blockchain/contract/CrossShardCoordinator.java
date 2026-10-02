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
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple5;
import org.web3j.tuples.generated.Tuple8;
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
public class CrossShardCoordinator extends Contract {
    public static final String BINARY = "60806040523480156200001157600080fd5b5060405162001b7338038062001b7383398101604081905262000034916200012c565b600081118015620000455750600082115b8015620000525750808211155b620000995760405162461bcd60e51b81526020600482015260126024820152715853686172643a206261642071756f72756d60701b60448201526064015b60405180910390fd5b60008311620000eb5760405162461bcd60e51b815260206004820152601360248201527f5853686172643a206261642074696d656f757400000000000000000000000000604482015260640162000090565b60008054336001600160a01b031991821617909155600180549091166001600160a01b03959095169490941790935560029190915560035560045562000179565b600080600080608085870312156200014357600080fd5b84516001600160a01b03811681146200015b57600080fd5b60208601516040870151606090970151919890975090945092505050565b6119ea80620001896000396000f3fe608060405234801561001057600080fd5b50600436106101585760003560e01c8063986ba392116100c3578063ce6b6f961161007c578063ce6b6f9614610360578063d30a0c3f14610394578063e0e074d2146103a7578063f14fcbc8146103d2578063f3819990146103e5578063f851a440146103f857600080fd5b8063986ba39214610288578063a4f7d9ee146102fe578063a7713a7014610311578063a90ad6be1461031a578063c58a34cc1461033a578063ca0bdfd21461034d57600080fd5b806380db53bc1161011557806380db53bc1461022b5780638659bdbc14610234578063893d8f4c1461023d578063903ac41d1461024657806395e272bd1461024f57806397c3d3341461027f57600080fd5b806309d6ce0e1461015d5780632ebe14b01461017257806358d4ef831461018e5780635ffee4691461019757806378925021146101d55780637b10399914610200575b600080fd5b61017061016b3660046115fc565b61040b565b005b61017b60025481565b6040519081526020015b60405180910390f35b61017b600e5481565b6101c56101a5366004611615565b600660209081526000928352604080842090915290825290205460ff1681565b6040519015158152602001610185565b61017b6101e3366004611615565b600760209081526000928352604080842090915290825290205481565b600154610213906001600160a01b031681565b6040516001600160a01b039091168152602001610185565b61017b60105481565b61017b600c5481565b61017b600f5481565b61017b600d5481565b61025761052f565b604080519586526020860194909452928401919091526060830152608082015260a001610185565b61017b60045481565b6102ea6102963660046115fc565b6005602081905260009182526040909120805460018201546002830154600384015460048501549585015460068601546007909601546001600160a01b039095169693959294919360ff9081169391921688565b60405161018598979695949392919061164d565b61017061030c366004611712565b610586565b61017b60035481565b61032d6103283660046115fc565b610aef565b604051610185919061177c565b6101706103483660046115fc565b610b51565b61032d61035b366004611615565b610bea565b6101c561036e3660046117c0565b600860209081526000938452604080852082529284528284209052825290205460ff1681565b6101706103a2366004611805565b610c56565b61017b6103b5366004611615565b600a60209081526000928352604080842090915290825290205481565b6101706103e03660046115fc565b61104e565b61017b6103f3366004611615565b611234565b600054610213906001600160a01b031681565b60008181526005602052604090206001600482015460ff16600381111561043457610434611637565b1461045a5760405162461bcd60e51b815260040161045190611858565b60405180910390fd5b80600201544311806104705750600781015460ff165b80610484575080546001600160a01b031633145b6104d05760405162461bcd60e51b815260206004820152601860248201527f5853686172643a2063616e6e6f742061626f72742079657400000000000000006044820152606401610451565b61052b828260020154431161050657604051806040016040528060098152602001681c995c5d595cdd195960ba1b81525061133b565b604051806040016040528060078152602001661d1a5b595bdd5d60ca1b81525061133b565b5050565b600c54600d54600e54600080831561055657600d54601054610551919061189b565b610559565b60005b9150600c5460001461057a57600c54600f54610575919061189b565b61057d565b60005b90509091929394565b60008581526005602052604090206001600482015460ff1660038111156105af576105af611637565b146105cc5760405162461bcd60e51b815260040161045190611858565b80600201544311156106185760405162461bcd60e51b81526020600482015260156024820152745853686172643a207061737420646561646c696e6560581b6044820152606401610451565b61062286866113ab565b61066e5760405162461bcd60e51b815260206004820152601a60248201527f5853686172643a206e6f7420616e20696e7075742073686172640000000000006044820152606401610451565b6001805490820154604051633ca6fc4f60e11b81526004810191909152602481018790523360448201526001600160a01b039091169063794df89e90606401602060405180830381865afa1580156106ca573d6000803e3d6000fd5b505050506040513d601f19601f820116820180604052508101906106ee91906118bd565b61073a5760405162461bcd60e51b815260206004820152601c60248201527f5853686172643a206e6f7420636f6d6d6974746565206d656d626572000000006044820152606401610451565b6000868152600860209081526040808320888452825280832033845290915290205460ff16156107a45760405162461bcd60e51b81526020600482015260156024820152741614da185c990e88185b1c9958591e481d9bdd1959605a1b6044820152606401610451565b600086815260086020908152604080832088845282528083203384529091528120805460ff19166001908117909155600f8054919290916107e69084906118e1565b90915550849050610895576007808201805460ff19166001179055600087815260209182526040808220888352909252818120549151339288928a927f4f04c584a198a966cc922d5979854640745ebefeb936fa172f3eb496edd3fbab926108579215158252602082015260400190565b60405180910390a461088f866040518060400160405280600e81526020016d736861726420766f746564206e6f60901b81525061133b565b50610ae8565b600086815260076020908152604080832088845290915281208054600192906108bf9084906118e1565b9091555050600086815260076020908152604080832088845282529182902054825160018152918201523391879189917f4f04c584a198a966cc922d5979854640745ebefeb936fa172f3eb496edd3fbab910160405180910390a4600086815260066020908152604080832088845290915290205460ff16156109425750610ae8565b610950816001015486611234565b600087815260076020908152604080832089845290915290205410156109765750610ae8565b60005b82811015610a6e576000848483818110610995576109956118f4565b60008a8152600a6020908152604080832093820295909501358083529290529290922054919250508015806109c957508881145b610a155760405162461bcd60e51b815260206004820152601d60248201527f5853686172643a206f626a65637420616c7265616479206c6f636b65640000006044820152606401610451565b80610a59576000888152600a6020908152604080832085845282528083208c90558b8352600b82528083208b84528252822080546001810182559083529120018290555b50508080610a669061190a565b915050610979565b5060008681526006602081815260408084208985529091528220805460ff19166001908117909155908301805491929091610aaa9084906118e1565b9091555050604051828152859087907fafaad35f4fcb8aa53dabe4c5608b6139fae1a9b2fc808d7d0df847a2d8d966e89060200160405180910390a3505b5050505050565b600081815260096020908152604091829020805483518184028101840190945280845260609392830182828015610b4557602002820191906000526020600020905b815481526020019060010190808311610b31575b50505050509050919050565b6000546001600160a01b03163314610b9f5760405162461bcd60e51b81526020600482015260116024820152702c29b430b9321d103737ba1030b236b4b760791b6044820152606401610451565b60008111610be55760405162461bcd60e51b81526020600482015260136024820152721614da185c990e88189859081d1a5b595bdd5d606a1b6044820152606401610451565b600255565b6000828152600b60209081526040808320848452825291829020805483518184028101840190945280845260609392830182828015610c4857602002820191906000526020600020905b815481526020019060010190808311610c34575b505050505090505b92915050565b60008481526005602052604081206004015460ff166003811115610c7c57610c7c611637565b14610cc95760405162461bcd60e51b815260206004820152601960248201527f5853686172643a207478496420616c72656164792075736564000000000000006044820152606401610451565b80610d165760405162461bcd60e51b815260206004820152601760248201527f5853686172643a206e6f20696e707574207368617264730000000000000000006044820152606401610451565b60015460405163503aba7360e01b8152600481018590526001600160a01b039091169063503aba7390602401602060405180830381865afa158015610d5f573d6000803e3d6000fd5b505050506040513d601f19601f82011682018060405250810190610d8391906118bd565b610dcf5760405162461bcd60e51b815260206004820152601c60248201527f5853686172643a20636f6d6d6974746565206e6f7420666f726d6564000000006044820152606401610451565b60005b81811015610ed8576000610de78260016118e1565b90505b82811015610e8357838382818110610e0457610e046118f4565b90506020020135848484818110610e1d57610e1d6118f4565b9050602002013503610e715760405162461bcd60e51b815260206004820152601760248201527f5853686172643a206475706c69636174652073686172640000000000000000006044820152606401610451565b80610e7b8161190a565b915050610dea565b506000858152600960205260409020838383818110610ea457610ea46118f4565b8354600181018555600094855260209485902091909402929092013591909201555080610ed08161190a565b915050610dd2565b50604051806101000160405280336001600160a01b0316815260200184815260200160025443610f0891906118e1565b81524360208201526040016001815260208082018490526000604080840182905260609384018290528882526005835290819020845181546001600160a01b0319166001600160a01b039091161781559184015160018084019190915590840151600283015591830151600380830191909155608084015160048301805493949193909260ff1990911691908490811115610fa557610fa5611637565b021790555060a0820151600582015560c0820151600682015560e0909101516007909101805460ff1916911515919091179055600c805460019190600090610fee9084906118e1565b9091555050600254339085907f95596388f02ed1945021bcbc568e061ee0eb934ca44728df60a7ae2718e7f5df908690859061102a90436118e1565b6040805193845260208401929092529082015260600160405180910390a350505050565b60008181526005602052604090206001600482015460ff16600381111561107757611077611637565b146110945760405162461bcd60e51b815260040161045190611858565b600781015460ff16156110e95760405162461bcd60e51b815260206004820152601860248201527f5853686172643a20616c72656164792072656a656374656400000000000000006044820152606401610451565b80600201544311156111355760405162461bcd60e51b81526020600482015260156024820152745853686172643a207061737420646561646c696e6560581b6044820152606401610451565b806005015481600601541461118c5760405162461bcd60e51b815260206004820152601f60248201527f5853686172643a206e6f7420616c6c20736861726473207072657061726564006044820152606401610451565b60048101805460ff191660021790556111a48261140f565b60008160030154436111b69190611923565b90506001600d60008282546111cb91906118e1565b9250508190555080601060008282546111e491906118e1565b909155508390507f3956ea6fde4336146d379c41903651a75e366b198e5d51174c6ddf03f455630e8261121683611542565b604080519283526020830191909152015b60405180910390a2505050565b6001546040516327c8e21d60e21b8152600481018490526024810183905260009182916001600160a01b0390911690639f23887490604401602060405180830381865afa158015611289573d6000803e3d6000fd5b505050506040513d601f19601f820116820180604052508101906112ad9190611936565b9050600081116112ff5760405162461bcd60e51b815260206004820152601760248201527f5853686172643a20656d70747920636f6d6d69747465650000000000000000006044820152606401610451565b600454600160045460035484611315919061194f565b61131f91906118e1565b6113299190611923565b611333919061189b565b949350505050565b600082815260056020526040902060048101805460ff191660031790556113618361140f565b6001600e600082825461137491906118e1565b92505081905550827f50e0ab77b38d688a530c89347626bcdbe9c3ef3dedabf3d81223b4902662c61e836040516112279190611966565b6000828152600960205260408120815b815481101561140457838282815481106113d7576113d76118f4565b9060005260206000200154036113f257600192505050610c50565b806113fc8161190a565b9150506113bb565b506000949350505050565b6000818152600960205260408120905b815481101561153d57600082828154811061143c5761143c6118f4565b6000918252602080832090910154868352600b82526040808420828552909252908220909250905b8154811015611505576000838152600a602052604081208354889290859085908110611492576114926118f4565b9060005260206000200154815260200190815260200160002054036114f3576000838152600a60205260408120835482908590859081106114d5576114d56118f4565b90600052602060002001548152602001908152602001600020819055505b806114fd8161190a565b915050611464565b506000858152600b602090815260408083208584529091528120611528916115c2565b505080806115359061190a565b91505061141f565b505050565b6000818152600960205260408120815b81548110156115bb57600760008581526020019081526020016000206000838381548110611582576115826118f4565b9060005260206000200154815260200190815260200160002054836115a791906118e1565b9250806115b38161190a565b915050611552565b5050919050565b50805460008255906000526020600020908101906115e091906115e3565b50565b5b808211156115f857600081556001016115e4565b5090565b60006020828403121561160e57600080fd5b5035919050565b6000806040838503121561162857600080fd5b50508035926020909101359150565b634e487b7160e01b600052602160045260246000fd5b6001600160a01b038916815260208101889052604081018790526060810186905261010081016004861061169157634e487b7160e01b600052602160045260246000fd5b8560808301528460a08301528360c083015282151560e08301529998505050505050505050565b80151581146115e057600080fd5b60008083601f8401126116d857600080fd5b50813567ffffffffffffffff8111156116f057600080fd5b6020830191508360208260051b850101111561170b57600080fd5b9250929050565b60008060008060006080868803121561172a57600080fd5b85359450602086013593506040860135611743816116b8565b9250606086013567ffffffffffffffff81111561175f57600080fd5b61176b888289016116c6565b969995985093965092949392505050565b6020808252825182820181905260009190848201906040850190845b818110156117b457835183529284019291840191600101611798565b50909695505050505050565b6000806000606084860312156117d557600080fd5b833592506020840135915060408401356001600160a01b03811681146117fa57600080fd5b809150509250925092565b6000806000806060858703121561181b57600080fd5b8435935060208501359250604085013567ffffffffffffffff81111561184057600080fd5b61184c878288016116c6565b95989497509550505050565b6020808252601390820152725853686172643a206e6f742070656e64696e6760681b604082015260600190565b634e487b7160e01b600052601160045260246000fd5b6000826118b857634e487b7160e01b600052601260045260246000fd5b500490565b6000602082840312156118cf57600080fd5b81516118da816116b8565b9392505050565b80820180821115610c5057610c50611885565b634e487b7160e01b600052603260045260246000fd5b60006001820161191c5761191c611885565b5060010190565b81810381811115610c5057610c50611885565b60006020828403121561194857600080fd5b5051919050565b8082028115828204841417610c5057610c50611885565b600060208083528351808285015260005b8181101561199357858101830151858201604001528201611977565b506000604082860101526040601f19601f830116850101925050509291505056fea2646970667358221220969157485b93bc933f868125bbef0d4bdcd8cf5a5bff00c86f78fca96795353664736f6c63430008130033";

    public static final String FUNC_ABORT = "abort";

    public static final String FUNC_ABORTEDCOUNT = "abortedCount";

    public static final String FUNC_ADMIN = "admin";

    public static final String FUNC_APPROVALS = "approvals";

    public static final String FUNC_COMMIT = "commit";

    public static final String FUNC_COMMITTEDCOUNT = "committedCount";

    public static final String FUNC_HASVOTED = "hasVoted";

    public static final String FUNC_INITIATE = "initiate";

    public static final String FUNC_INITIATEDCOUNT = "initiatedCount";

    public static final String FUNC_INPUTSHARDSOF = "inputShardsOf";

    public static final String FUNC_LOCKEDBY = "lockedBy";

    public static final String FUNC_LOCKEDOBJECTSOF = "lockedObjectsOf";

    public static final String FUNC_QUORUMDENOMINATOR = "quorumDenominator";

    public static final String FUNC_QUORUMNUMERATOR = "quorumNumerator";

    public static final String FUNC_QUORUMOF = "quorumOf";

    public static final String FUNC_REGISTRY = "registry";

    public static final String FUNC_SETTIMEOUT = "setTimeout";

    public static final String FUNC_SHARDAPPROVED = "shardApproved";

    public static final String FUNC_STATISTICS = "statistics";

    public static final String FUNC_TIMEOUTBLOCKS = "timeoutBlocks";

    public static final String FUNC_TOTALLATENCYBLOCKS = "totalLatencyBlocks";

    public static final String FUNC_TOTALVOTECOUNT = "totalVoteCount";

    public static final String FUNC_TXS = "txs";

    public static final String FUNC_VOTE = "vote";

    public static final Event ABORTED_EVENT = new Event("Aborted", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Utf8String>() {}));
    ;

    public static final Event COMMITTED_EVENT = new Event("Committed", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event INITIATED_EVENT = new Event("Initiated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event SHARDPREPARED_EVENT = new Event("ShardPrepared", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event VOTED_EVENT = new Event("Voted", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Uint256>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Bool>() {}, new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected CrossShardCoordinator(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected CrossShardCoordinator(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected CrossShardCoordinator(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected CrossShardCoordinator(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<AbortedEventResponse> getAbortedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ABORTED_EVENT, transactionReceipt);
        ArrayList<AbortedEventResponse> responses = new ArrayList<AbortedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AbortedEventResponse typedResponse = new AbortedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.txId = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.reason = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static AbortedEventResponse getAbortedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ABORTED_EVENT, log);
        AbortedEventResponse typedResponse = new AbortedEventResponse();
        typedResponse.log = log;
        typedResponse.txId = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.reason = (String) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<AbortedEventResponse> abortedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getAbortedEventFromLog(log));
    }

    public Flowable<AbortedEventResponse> abortedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ABORTED_EVENT));
        return abortedEventFlowable(filter);
    }

    public static List<CommittedEventResponse> getCommittedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(COMMITTED_EVENT, transactionReceipt);
        ArrayList<CommittedEventResponse> responses = new ArrayList<CommittedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            CommittedEventResponse typedResponse = new CommittedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.txId = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.latencyBlocks = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.votes = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static CommittedEventResponse getCommittedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(COMMITTED_EVENT, log);
        CommittedEventResponse typedResponse = new CommittedEventResponse();
        typedResponse.log = log;
        typedResponse.txId = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.latencyBlocks = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.votes = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<CommittedEventResponse> committedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getCommittedEventFromLog(log));
    }

    public Flowable<CommittedEventResponse> committedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(COMMITTED_EVENT));
        return committedEventFlowable(filter);
    }

    public static List<InitiatedEventResponse> getInitiatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(INITIATED_EVENT, transactionReceipt);
        ArrayList<InitiatedEventResponse> responses = new ArrayList<InitiatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            InitiatedEventResponse typedResponse = new InitiatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.txId = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.initiator = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.epoch = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.inputCount = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.deadlineBlock = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static InitiatedEventResponse getInitiatedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(INITIATED_EVENT, log);
        InitiatedEventResponse typedResponse = new InitiatedEventResponse();
        typedResponse.log = log;
        typedResponse.txId = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.initiator = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.epoch = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.inputCount = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        typedResponse.deadlineBlock = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<InitiatedEventResponse> initiatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getInitiatedEventFromLog(log));
    }

    public Flowable<InitiatedEventResponse> initiatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(INITIATED_EVENT));
        return initiatedEventFlowable(filter);
    }

    public static List<ShardPreparedEventResponse> getShardPreparedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(SHARDPREPARED_EVENT, transactionReceipt);
        ArrayList<ShardPreparedEventResponse> responses = new ArrayList<ShardPreparedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ShardPreparedEventResponse typedResponse = new ShardPreparedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.txId = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.shardId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.lockedObjects = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ShardPreparedEventResponse getShardPreparedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(SHARDPREPARED_EVENT, log);
        ShardPreparedEventResponse typedResponse = new ShardPreparedEventResponse();
        typedResponse.log = log;
        typedResponse.txId = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.shardId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.lockedObjects = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<ShardPreparedEventResponse> shardPreparedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getShardPreparedEventFromLog(log));
    }

    public Flowable<ShardPreparedEventResponse> shardPreparedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SHARDPREPARED_EVENT));
        return shardPreparedEventFlowable(filter);
    }

    public static List<VotedEventResponse> getVotedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(VOTED_EVENT, transactionReceipt);
        ArrayList<VotedEventResponse> responses = new ArrayList<VotedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            VotedEventResponse typedResponse = new VotedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.txId = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.shardId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.voter = (String) eventValues.getIndexedValues().get(2).getValue();
            typedResponse.approve = (Boolean) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.tally = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static VotedEventResponse getVotedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(VOTED_EVENT, log);
        VotedEventResponse typedResponse = new VotedEventResponse();
        typedResponse.log = log;
        typedResponse.txId = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.shardId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.voter = (String) eventValues.getIndexedValues().get(2).getValue();
        typedResponse.approve = (Boolean) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.tally = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<VotedEventResponse> votedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getVotedEventFromLog(log));
    }

    public Flowable<VotedEventResponse> votedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(VOTED_EVENT));
        return votedEventFlowable(filter);
    }

    public RemoteFunctionCall<TransactionReceipt> abort(byte[] txId) {
        final Function function = new Function(
                FUNC_ABORT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(txId)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> abortedCount() {
        final Function function = new Function(FUNC_ABORTEDCOUNT, 
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

    public RemoteFunctionCall<BigInteger> approvals(byte[] param0, BigInteger param1) {
        final Function function = new Function(FUNC_APPROVALS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(param0), 
                new org.web3j.abi.datatypes.generated.Uint256(param1)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> commit(byte[] txId) {
        final Function function = new Function(
                FUNC_COMMIT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(txId)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> committedCount() {
        final Function function = new Function(FUNC_COMMITTEDCOUNT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<Boolean> hasVoted(byte[] param0, BigInteger param1, String param2) {
        final Function function = new Function(FUNC_HASVOTED, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(param0), 
                new org.web3j.abi.datatypes.generated.Uint256(param1), 
                new org.web3j.abi.datatypes.Address(160, param2)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<TransactionReceipt> initiate(byte[] txId, BigInteger epoch, List<BigInteger> inputShards) {
        final Function function = new Function(
                FUNC_INITIATE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(txId), 
                new org.web3j.abi.datatypes.generated.Uint256(epoch), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint256>(
                        org.web3j.abi.datatypes.generated.Uint256.class,
                        org.web3j.abi.Utils.typeMap(inputShards, org.web3j.abi.datatypes.generated.Uint256.class))), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> initiatedCount() {
        final Function function = new Function(FUNC_INITIATEDCOUNT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<List> inputShardsOf(byte[] txId) {
        final Function function = new Function(FUNC_INPUTSHARDSOF, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(txId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Uint256>>() {}));
        return new RemoteFunctionCall<List>(function,
                new Callable<List>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public List call() throws Exception {
                        List<Type> result = (List<Type>) executeCallSingleValueReturn(function, List.class);
                        return convertToNative(result);
                    }
                });
    }

    public RemoteFunctionCall<byte[]> lockedBy(BigInteger param0, byte[] param1) {
        final Function function = new Function(FUNC_LOCKEDBY, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0), 
                new org.web3j.abi.datatypes.generated.Bytes32(param1)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<List> lockedObjectsOf(byte[] txId, BigInteger shardId) {
        final Function function = new Function(FUNC_LOCKEDOBJECTSOF, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(txId), 
                new org.web3j.abi.datatypes.generated.Uint256(shardId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Bytes32>>() {}));
        return new RemoteFunctionCall<List>(function,
                new Callable<List>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public List call() throws Exception {
                        List<Type> result = (List<Type>) executeCallSingleValueReturn(function, List.class);
                        return convertToNative(result);
                    }
                });
    }

    public RemoteFunctionCall<BigInteger> quorumDenominator() {
        final Function function = new Function(FUNC_QUORUMDENOMINATOR, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> quorumNumerator() {
        final Function function = new Function(FUNC_QUORUMNUMERATOR, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> quorumOf(BigInteger epoch, BigInteger shardId) {
        final Function function = new Function(FUNC_QUORUMOF, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(epoch), 
                new org.web3j.abi.datatypes.generated.Uint256(shardId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<String> registry() {
        final Function function = new Function(FUNC_REGISTRY, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> setTimeout(BigInteger blocks_) {
        final Function function = new Function(
                FUNC_SETTIMEOUT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(blocks_)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> shardApproved(byte[] param0, BigInteger param1) {
        final Function function = new Function(FUNC_SHARDAPPROVED, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(param0), 
                new org.web3j.abi.datatypes.generated.Uint256(param1)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<Tuple5<BigInteger, BigInteger, BigInteger, BigInteger, BigInteger>> statistics() {
        final Function function = new Function(FUNC_STATISTICS, 
                Arrays.<Type>asList(), 
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

    public RemoteFunctionCall<BigInteger> timeoutBlocks() {
        final Function function = new Function(FUNC_TIMEOUTBLOCKS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> totalLatencyBlocks() {
        final Function function = new Function(FUNC_TOTALLATENCYBLOCKS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> totalVoteCount() {
        final Function function = new Function(FUNC_TOTALVOTECOUNT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<Tuple8<String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, Boolean>> txs(byte[] param0) {
        final Function function = new Function(FUNC_TXS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint8>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Bool>() {}));
        return new RemoteFunctionCall<Tuple8<String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, Boolean>>(function,
                new Callable<Tuple8<String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, Boolean>>() {
                    @Override
                    public Tuple8<String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, Boolean> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple8<String, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, BigInteger, Boolean>(
                                (String) results.get(0).getValue(), 
                                (BigInteger) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue(), 
                                (BigInteger) results.get(3).getValue(), 
                                (BigInteger) results.get(4).getValue(), 
                                (BigInteger) results.get(5).getValue(), 
                                (BigInteger) results.get(6).getValue(), 
                                (Boolean) results.get(7).getValue());
                    }
                });
    }

    public RemoteFunctionCall<TransactionReceipt> vote(byte[] txId, BigInteger shardId, Boolean approve, List<byte[]> objects) {
        final Function function = new Function(
                FUNC_VOTE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(txId), 
                new org.web3j.abi.datatypes.generated.Uint256(shardId), 
                new org.web3j.abi.datatypes.Bool(approve), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Bytes32>(
                        org.web3j.abi.datatypes.generated.Bytes32.class,
                        org.web3j.abi.Utils.typeMap(objects, org.web3j.abi.datatypes.generated.Bytes32.class))), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static CrossShardCoordinator load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new CrossShardCoordinator(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static CrossShardCoordinator load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new CrossShardCoordinator(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static CrossShardCoordinator load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new CrossShardCoordinator(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static CrossShardCoordinator load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new CrossShardCoordinator(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<CrossShardCoordinator> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, String _registry, BigInteger _timeoutBlocks, BigInteger _num, BigInteger _den) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _registry), 
                new org.web3j.abi.datatypes.generated.Uint256(_timeoutBlocks), 
                new org.web3j.abi.datatypes.generated.Uint256(_num), 
                new org.web3j.abi.datatypes.generated.Uint256(_den)));
        return deployRemoteCall(CrossShardCoordinator.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<CrossShardCoordinator> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, String _registry, BigInteger _timeoutBlocks, BigInteger _num, BigInteger _den) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _registry), 
                new org.web3j.abi.datatypes.generated.Uint256(_timeoutBlocks), 
                new org.web3j.abi.datatypes.generated.Uint256(_num), 
                new org.web3j.abi.datatypes.generated.Uint256(_den)));
        return deployRemoteCall(CrossShardCoordinator.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<CrossShardCoordinator> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String _registry, BigInteger _timeoutBlocks, BigInteger _num, BigInteger _den) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _registry), 
                new org.web3j.abi.datatypes.generated.Uint256(_timeoutBlocks), 
                new org.web3j.abi.datatypes.generated.Uint256(_num), 
                new org.web3j.abi.datatypes.generated.Uint256(_den)));
        return deployRemoteCall(CrossShardCoordinator.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<CrossShardCoordinator> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String _registry, BigInteger _timeoutBlocks, BigInteger _num, BigInteger _den) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _registry), 
                new org.web3j.abi.datatypes.generated.Uint256(_timeoutBlocks), 
                new org.web3j.abi.datatypes.generated.Uint256(_num), 
                new org.web3j.abi.datatypes.generated.Uint256(_den)));
        return deployRemoteCall(CrossShardCoordinator.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static class AbortedEventResponse extends BaseEventResponse {
        public byte[] txId;

        public String reason;
    }

    public static class CommittedEventResponse extends BaseEventResponse {
        public byte[] txId;

        public BigInteger latencyBlocks;

        public BigInteger votes;
    }

    public static class InitiatedEventResponse extends BaseEventResponse {
        public byte[] txId;

        public String initiator;

        public BigInteger epoch;

        public BigInteger inputCount;

        public BigInteger deadlineBlock;
    }

    public static class ShardPreparedEventResponse extends BaseEventResponse {
        public byte[] txId;

        public BigInteger shardId;

        public BigInteger lockedObjects;
    }

    public static class VotedEventResponse extends BaseEventResponse {
        public byte[] txId;

        public BigInteger shardId;

        public String voter;

        public Boolean approve;

        public BigInteger tally;
    }
}
