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
import org.web3j.abi.datatypes.generated.Bytes32;
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
import org.web3j.tuples.generated.Tuple2;
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
public class AffinityManager extends Contract {
    public static final String BINARY = "608060405234801561001057600080fd5b5060405161159c38038061159c83398101604081905261002f916100b1565b6000811180156100475750670de0b6b3a76400008111155b6100975760405162461bcd60e51b815260206004820152601c60248201527f416666696e6974793a207468657461206f7574206f662072616e676500000000604482015260640160405180910390fd5b600080546001600160a01b031916331790556002556100ca565b6000602082840312156100c357600080fd5b5051919050565b6114c3806100d96000396000f3fe608060405234801561001057600080fd5b50600436106101735760003560e01c80636e09999e116100de578063adcab1a911610097578063cd27ca8211610071578063cd27ca821461039a578063d686b0cb146103ad578063f33930d9146103c0578063f851a440146103d357600080fd5b8063adcab1a91461034a578063bf2292cd14610374578063c6fd1dfa1461038757600080fd5b80636e09999e146102975780637f1bfcb9146102ca5780638eaeb5db146102f55780638f23b32614610318578063a24aaf7414610321578063a5d15e6b1461032a57600080fd5b80632e7618a3116101305780632e7618a314610226578063384eae1e14610246578063445f7679146102595780635f1d99b11461026c5780636634010e146102755780636a1460241461028857600080fd5b806303bce1b814610178578063053f89f31461018d5780630597e64a146101c057806308ddc2bd146101e05780630a552fb0146101f35780632985bbc114610206575b600080fd5b61018b610186366004611296565b6103e6565b005b6101ad61019b3660046112b1565b60036020526000908152604090205481565b6040519081526020015b60405180910390f35b6101ad6101ce3660046112b1565b600a6020526000908152604090205481565b61018b6101ee3660046112b1565b61043b565b61018b6102013660046112ca565b610504565b6101ad6102143660046112b1565b600b6020526000908152604090205481565b6101ad610234366004611296565b60046020526000908152604090205481565b61018b6102543660046112ca565b6105af565b61018b6102673660046112ec565b610681565b6101ad60025481565b6101ad610283366004611296565b61091b565b6101ad670de0b6b3a764000081565b6102ba6102a5366004611296565b60066020526000908152604090205460ff1681565b60405190151581526020016101b7565b6102dd6102d8366004611296565b61095e565b6040516001600160a01b0390911681526020016101b7565b6102ba6103033660046112b1565b600c6020526000908152604090205460ff1681565b6101ad600d5481565b6101ad60085481565b6101ad6103383660046112b1565b60076020526000908152604090205481565b61035d610358366004611296565b6109d0565b6040805192151583526020830191909152016101b7565b61018b61038236600461131f565b610a27565b6101ad6103953660046112ec565b610d15565b6101ad6103a83660046112ec565b610ddb565b6102ba6103bb3660046112ec565b610e81565b6001546102dd906001600160a01b031681565b6000546102dd906001600160a01b031681565b6000546001600160a01b031633146104195760405162461bcd60e51b8152600401610410906113a2565b60405180910390fd5b600180546001600160a01b0319166001600160a01b0392909216919091179055565b6000546001600160a01b031633146104655760405162461bcd60e51b8152600401610410906113a2565b60008111801561047d5750670de0b6b3a76400008111155b6104c95760405162461bcd60e51b815260206004820152601c60248201527f416666696e6974793a207468657461206f7574206f662072616e6765000000006044820152606401610410565b60028190556040518181527f78f1e9884eecb9a7e0508afc5f6627eb56ceec9826204b70ca33acfce67d72c89060200160405180910390a150565b6000546001600160a01b0316331461052e5760405162461bcd60e51b8152600401610410906113a2565b811580159061053f5750600d548211155b61055b5760405162461bcd60e51b8152600401610410906113cf565b6000828152600b60209081526040808320849055600c909152808220805460ff1916600117905551829184917fd0291423b07a78fb841f465307c52f0619f2208c3477b9634c1cfe7b90f73def9190a35050565b6001546001600160a01b03163314806105d257506000546001600160a01b031633145b6106175760405162461bcd60e51b815260206004820152601660248201527520b33334b734ba3c9d103737ba103932b1b7b93232b960511b6044820152606401610410565b81158015906106285750600d548211155b6106445760405162461bcd60e51b8152600401610410906113cf565b600082815260076020526040902054600290610661908390611412565b61066b9190611425565b6000928352600760205260409092209190915550565b6001546001600160a01b03163314806106a457506000546001600160a01b031633145b6106e95760405162461bcd60e51b815260206004820152601660248201527520b33334b734ba3c9d103737ba103932b1b7b93232b960511b6044820152606401610410565b6001600160a01b0382161580159061070957506001600160a01b03811615155b61074e5760405162461bcd60e51b8152602060048201526016602482015275416666696e6974793a207a65726f206164647265737360501b6044820152606401610410565b806001600160a01b0316826001600160a01b0316036107af5760405162461bcd60e51b815260206004820152601a60248201527f416666696e6974793a2073656c6620696e746572616374696f6e0000000000006044820152606401610410565b6107b882610ead565b6107c181610ead565b60006107cd8383610ddb565b905060016003600083815260200190815260200160002060008282546107f39190611412565b90915550506001600160a01b0383166000908152600460205260408120805460019290610821908490611412565b90915550506001600160a01b038216600090815260046020526040812080546001929061084f908490611412565b90915550506000818152600360209081526040918290205491519182526001600160a01b0384811692908616917f5884fcc059776a497a153a91a44929e262a4c6e71e2d1e68143aeee39415d5f5910160405180910390a360006108b38484610d15565b9050600254811061091557826001600160a01b0316846001600160a01b03167f3604a8cf8cc8688a11d587e358482036af433ea843eac76ac6148922c37d7bec8360405161090391815260200190565b60405180910390a36109158484610f0f565b50505050565b6000806109278361095e565b90506001600160a01b0381166109405750600092915050565b6001600160a01b031660009081526009602052604090205492915050565b6001600160a01b03811660009081526006602052604081205460ff1661098657506000919050565b815b6001600160a01b03808216600081815260056020526040902054909116146109ca576001600160a01b0390811660009081526005602052604090205416610988565b92915050565b60008060006109de8461091b565b90508015806109fc57506000818152600c602052604090205460ff16155b15610a0d5750600093849350915050565b6000908152600b6020526040902054600194909350915050565b6000546001600160a01b03163314610a515760405162461bcd60e51b8152600401610410906113a2565b60008311610a975760405162461bcd60e51b8152602060048201526013602482015272416666696e6974793a206e6f2073686172647360681b6044820152606401610410565b6008548414610b0257610aac600e6000611248565b60005b83811015610afb57600e805460018101825560009182527fbb7b4a454dc3493923482f07822329ed19e8244eff582cc204f8554c3620c3fd015580610af381611447565b915050610aaf565b5060088490555b600e548314610b535760405162461bcd60e51b815260206004820152601d60248201527f416666696e6974793a20736861726420636f756e74206368616e6765640000006044820152606401610410565b60001960005b82811015610d0d576000848483818110610b7557610b75611460565b90506020020135905080600014158015610b915750600d548111155b610bad5760405162461bcd60e51b8152600401610410906113cf565b60008181526007602052604090205483811115610c0c5760405162461bcd60e51b815260206004820152601960248201527f416666696e6974793a206e6f7420736f727465642064657363000000000000006044820152606401610410565b925082600060015b88811015610c7457600e8281548110610c2f57610c2f611460565b9060005260206000200154600e8281548110610c4d57610c4d611460565b90600052602060002001541015610c62578091505b80610c6c81611447565b915050610c14565b5081600e8281548110610c8957610c89611460565b906000526020600020016000828254610ca29190611412565b90915550506000838152600b60209081526040808320849055600c909152808220805460ff1916600117905551829185917fd0291423b07a78fb841f465307c52f0619f2208c3477b9634c1cfe7b90f73def9190a35050508080610d0590611447565b915050610b59565b505050505050565b60008060036000610d268686610ddb565b815260200190815260200160002054905080600003610d495760009150506109ca565b6001600160a01b03808516600090815260046020526040808220549286168252902054811580610d77575080155b15610d8857600093505050506109ca565b6000610d9c610d978385611476565b61109f565b905080600003610db35760009450505050506109ca565b80610dc6670de0b6b3a764000086611476565b610dd09190611425565b979650505050505050565b6000816001600160a01b0316836001600160a01b031610610e3a576040516bffffffffffffffffffffffff19606084811b8216602084015285901b16603482015260480160405160208183030381529060405280519060200120610e7a565b6040516bffffffffffffffffffffffff19606085811b8216602084015284901b166034820152604801604051602081830303815290604052805190602001205b9392505050565b600080610e8d8461091b565b90508015801590610ea55750610ea28361091b565b81145b949350505050565b6001600160a01b03811660009081526006602052604090205460ff16610f0c576001600160a01b0381166000818152600660209081526040808320805460ff191660011790556005909152902080546001600160a01b03191690911790555b50565b6000610f1a83611109565b90506000610f2783611109565b9050806001600160a01b0316826001600160a01b031603610f5257610f4b826111af565b5050505050565b6000816001600160a01b0316836001600160a01b031610610f735781610f75565b825b90506000826001600160a01b0316846001600160a01b031610610f985783610f9a565b825b90506000610fa7836111af565b6001600160a01b03838116600090815260096020908152604080832054600590925290912080546001600160a01b031916928716929092179091559091508015611070576000818152600a602052604080822054848352908220805491929091611012908490611412565b90915550506000818152600a602090815260408083208390556001600160a01b0386168352600990915280822082905551829184917f5180b9f9aca15c121b3eef9a22377616d0835e9751df551fa4b13fc464d754589190a3611095565b6000828152600a6020526040812080546001929061108f908490611412565b90915550505b5050505050505050565b6000816000036110b157506000919050565b600060026110c0846001611412565b6110ca9190611425565b90508291505b81811015611103579050806002816110e88186611425565b6110f29190611412565b6110fc9190611425565b90506110d0565b50919050565b6000815b6001600160a01b038082166000818152600560205260409020549091161461114f576001600160a01b039081166000908152600560205260409020541661110d565b825b6001600160a01b038181166000908152600560205260409020548116908316146111a8576001600160a01b03908116600090815260056020526040902080548383166001600160a01b031982161790915516611151565b5092915050565b6001600160a01b0381166000908152600960205260408120548082036109ca576001600d60008282546111e29190611412565b9091555050600d546001600160a01b0384166000818152600960209081526040808320859055848352600a9091528082206001905551929350909183917f6958b51b54d963fa8eeeea5b61b83a9ad28f9ee9d5a2f1b8495e6866cb534bb191a392915050565b5080546000825590600052602060002090810190610f0c91905b808211156112765760008155600101611262565b5090565b80356001600160a01b038116811461129157600080fd5b919050565b6000602082840312156112a857600080fd5b610e7a8261127a565b6000602082840312156112c357600080fd5b5035919050565b600080604083850312156112dd57600080fd5b50508035926020909101359150565b600080604083850312156112ff57600080fd5b6113088361127a565b91506113166020840161127a565b90509250929050565b6000806000806060858703121561133557600080fd5b8435935060208501359250604085013567ffffffffffffffff8082111561135b57600080fd5b818701915087601f83011261136f57600080fd5b81358181111561137e57600080fd5b8860208260051b850101111561139357600080fd5b95989497505060200194505050565b60208082526013908201527220b33334b734ba3c9d103737ba1030b236b4b760691b604082015260600190565b6020808252601390820152720416666696e6974793a206261642067726f757606c1b604082015260600190565b634e487b7160e01b600052601160045260246000fd5b808201808211156109ca576109ca6113fc565b60008261144257634e487b7160e01b600052601260045260246000fd5b500490565b600060018201611459576114596113fc565b5060010190565b634e487b7160e01b600052603260045260246000fd5b80820281158282048414176109ca576109ca6113fc56fea2646970667358221220b4b3a6330f177c67b4356f5a2046579e7d33873eb3aa355ce120d7b06751247764736f6c63430008130033";

    public static final String FUNC_WAD = "WAD";

    public static final String FUNC_ADMIN = "admin";

    public static final String FUNC_AFFINITY = "affinity";

    public static final String FUNC_FINDVIEW = "findView";

    public static final String FUNC_GROUPCOUNT = "groupCount";

    public static final String FUNC_GROUPHASSHARD = "groupHasShard";

    public static final String FUNC_GROUPMEMBERCOUNT = "groupMemberCount";

    public static final String FUNC_GROUPOF = "groupOf";

    public static final String FUNC_GROUPPRIMARYSHARD = "groupPrimaryShard";

    public static final String FUNC_GROUPVOLUME = "groupVolume";

    public static final String FUNC_INSAMEGROUP = "inSameGroup";

    public static final String FUNC_ISKNOWN = "isKnown";

    public static final String FUNC_LASTREBALANCEEPOCH = "lastRebalanceEpoch";

    public static final String FUNC_PAIRINTERACTIONS = "pairInteractions";

    public static final String FUNC_PAIRKEY = "pairKey";

    public static final String FUNC_PRIMARYSHARDOF = "primaryShardOf";

    public static final String FUNC_REBALANCEGROUPS = "rebalanceGroups";

    public static final String FUNC_RECORDGROUPVOLUME = "recordGroupVolume";

    public static final String FUNC_RECORDINTERACTION = "recordInteraction";

    public static final String FUNC_RECORDER = "recorder";

    public static final String FUNC_SETGROUPPRIMARYSHARD = "setGroupPrimaryShard";

    public static final String FUNC_SETRECORDER = "setRecorder";

    public static final String FUNC_SETTHETAA = "setThetaA";

    public static final String FUNC_THETAA = "thetaA";

    public static final String FUNC_TOTALINTERACTIONS = "totalInteractions";

    public static final Event EDGEFORMED_EVENT = new Event("EdgeFormed", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event GROUPCREATED_EVENT = new Event("GroupCreated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event GROUPSHARDASSIGNED_EVENT = new Event("GroupShardAssigned", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>(true) {}));
    ;

    public static final Event GROUPSMERGED_EVENT = new Event("GroupsMerged", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>(true) {}));
    ;

    public static final Event INTERACTIONRECORDED_EVENT = new Event("InteractionRecorded", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event THETAUPDATED_EVENT = new Event("ThetaUpdated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected AffinityManager(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected AffinityManager(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected AffinityManager(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected AffinityManager(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<EdgeFormedEventResponse> getEdgeFormedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(EDGEFORMED_EVENT, transactionReceipt);
        ArrayList<EdgeFormedEventResponse> responses = new ArrayList<EdgeFormedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            EdgeFormedEventResponse typedResponse = new EdgeFormedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.a = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.b = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.affinityWad = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static EdgeFormedEventResponse getEdgeFormedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(EDGEFORMED_EVENT, log);
        EdgeFormedEventResponse typedResponse = new EdgeFormedEventResponse();
        typedResponse.log = log;
        typedResponse.a = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.b = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.affinityWad = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<EdgeFormedEventResponse> edgeFormedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getEdgeFormedEventFromLog(log));
    }

    public Flowable<EdgeFormedEventResponse> edgeFormedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(EDGEFORMED_EVENT));
        return edgeFormedEventFlowable(filter);
    }

    public static List<GroupCreatedEventResponse> getGroupCreatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(GROUPCREATED_EVENT, transactionReceipt);
        ArrayList<GroupCreatedEventResponse> responses = new ArrayList<GroupCreatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            GroupCreatedEventResponse typedResponse = new GroupCreatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.groupId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.root = (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static GroupCreatedEventResponse getGroupCreatedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(GROUPCREATED_EVENT, log);
        GroupCreatedEventResponse typedResponse = new GroupCreatedEventResponse();
        typedResponse.log = log;
        typedResponse.groupId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.root = (String) eventValues.getIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<GroupCreatedEventResponse> groupCreatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getGroupCreatedEventFromLog(log));
    }

    public Flowable<GroupCreatedEventResponse> groupCreatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(GROUPCREATED_EVENT));
        return groupCreatedEventFlowable(filter);
    }

    public static List<GroupShardAssignedEventResponse> getGroupShardAssignedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(GROUPSHARDASSIGNED_EVENT, transactionReceipt);
        ArrayList<GroupShardAssignedEventResponse> responses = new ArrayList<GroupShardAssignedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            GroupShardAssignedEventResponse typedResponse = new GroupShardAssignedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.groupId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.shardId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static GroupShardAssignedEventResponse getGroupShardAssignedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(GROUPSHARDASSIGNED_EVENT, log);
        GroupShardAssignedEventResponse typedResponse = new GroupShardAssignedEventResponse();
        typedResponse.log = log;
        typedResponse.groupId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.shardId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<GroupShardAssignedEventResponse> groupShardAssignedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getGroupShardAssignedEventFromLog(log));
    }

    public Flowable<GroupShardAssignedEventResponse> groupShardAssignedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(GROUPSHARDASSIGNED_EVENT));
        return groupShardAssignedEventFlowable(filter);
    }

    public static List<GroupsMergedEventResponse> getGroupsMergedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(GROUPSMERGED_EVENT, transactionReceipt);
        ArrayList<GroupsMergedEventResponse> responses = new ArrayList<GroupsMergedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            GroupsMergedEventResponse typedResponse = new GroupsMergedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.survivingGroup = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.absorbedGroup = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static GroupsMergedEventResponse getGroupsMergedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(GROUPSMERGED_EVENT, log);
        GroupsMergedEventResponse typedResponse = new GroupsMergedEventResponse();
        typedResponse.log = log;
        typedResponse.survivingGroup = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.absorbedGroup = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<GroupsMergedEventResponse> groupsMergedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getGroupsMergedEventFromLog(log));
    }

    public Flowable<GroupsMergedEventResponse> groupsMergedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(GROUPSMERGED_EVENT));
        return groupsMergedEventFlowable(filter);
    }

    public static List<InteractionRecordedEventResponse> getInteractionRecordedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(INTERACTIONRECORDED_EVENT, transactionReceipt);
        ArrayList<InteractionRecordedEventResponse> responses = new ArrayList<InteractionRecordedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            InteractionRecordedEventResponse typedResponse = new InteractionRecordedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.a = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.b = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.pairCount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static InteractionRecordedEventResponse getInteractionRecordedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(INTERACTIONRECORDED_EVENT, log);
        InteractionRecordedEventResponse typedResponse = new InteractionRecordedEventResponse();
        typedResponse.log = log;
        typedResponse.a = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.b = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.pairCount = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<InteractionRecordedEventResponse> interactionRecordedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getInteractionRecordedEventFromLog(log));
    }

    public Flowable<InteractionRecordedEventResponse> interactionRecordedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(INTERACTIONRECORDED_EVENT));
        return interactionRecordedEventFlowable(filter);
    }

    public static List<ThetaUpdatedEventResponse> getThetaUpdatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(THETAUPDATED_EVENT, transactionReceipt);
        ArrayList<ThetaUpdatedEventResponse> responses = new ArrayList<ThetaUpdatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ThetaUpdatedEventResponse typedResponse = new ThetaUpdatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.thetaA = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ThetaUpdatedEventResponse getThetaUpdatedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(THETAUPDATED_EVENT, log);
        ThetaUpdatedEventResponse typedResponse = new ThetaUpdatedEventResponse();
        typedResponse.log = log;
        typedResponse.thetaA = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<ThetaUpdatedEventResponse> thetaUpdatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getThetaUpdatedEventFromLog(log));
    }

    public Flowable<ThetaUpdatedEventResponse> thetaUpdatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(THETAUPDATED_EVENT));
        return thetaUpdatedEventFlowable(filter);
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

    public RemoteFunctionCall<BigInteger> affinity(String a, String b) {
        final Function function = new Function(FUNC_AFFINITY, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, a), 
                new org.web3j.abi.datatypes.Address(160, b)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<String> findView(String a) {
        final Function function = new Function(FUNC_FINDVIEW, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, a)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<BigInteger> groupCount() {
        final Function function = new Function(FUNC_GROUPCOUNT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<Boolean> groupHasShard(BigInteger param0) {
        final Function function = new Function(FUNC_GROUPHASSHARD, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<BigInteger> groupMemberCount(BigInteger param0) {
        final Function function = new Function(FUNC_GROUPMEMBERCOUNT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> groupOf(String a) {
        final Function function = new Function(FUNC_GROUPOF, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, a)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> groupPrimaryShard(BigInteger param0) {
        final Function function = new Function(FUNC_GROUPPRIMARYSHARD, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> groupVolume(BigInteger param0) {
        final Function function = new Function(FUNC_GROUPVOLUME, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<Boolean> inSameGroup(String a, String b) {
        final Function function = new Function(FUNC_INSAMEGROUP, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, a), 
                new org.web3j.abi.datatypes.Address(160, b)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<Boolean> isKnown(String param0) {
        final Function function = new Function(FUNC_ISKNOWN, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<BigInteger> lastRebalanceEpoch() {
        final Function function = new Function(FUNC_LASTREBALANCEEPOCH, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> pairInteractions(byte[] param0) {
        final Function function = new Function(FUNC_PAIRINTERACTIONS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<byte[]> pairKey(String a, String b) {
        final Function function = new Function(FUNC_PAIRKEY, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, a), 
                new org.web3j.abi.datatypes.Address(160, b)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<Tuple2<Boolean, BigInteger>> primaryShardOf(String a) {
        final Function function = new Function(FUNC_PRIMARYSHARDOF, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, a)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}, new TypeReference<Uint256>() {}));
        return new RemoteFunctionCall<Tuple2<Boolean, BigInteger>>(function,
                new Callable<Tuple2<Boolean, BigInteger>>() {
                    @Override
                    public Tuple2<Boolean, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple2<Boolean, BigInteger>(
                                (Boolean) results.get(0).getValue(), 
                                (BigInteger) results.get(1).getValue());
                    }
                });
    }

    public RemoteFunctionCall<TransactionReceipt> rebalanceGroups(BigInteger epoch, BigInteger shardCount, List<BigInteger> groupIds) {
        final Function function = new Function(
                FUNC_REBALANCEGROUPS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(epoch), 
                new org.web3j.abi.datatypes.generated.Uint256(shardCount), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint256>(
                        org.web3j.abi.datatypes.generated.Uint256.class,
                        org.web3j.abi.Utils.typeMap(groupIds, org.web3j.abi.datatypes.generated.Uint256.class))), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> recordGroupVolume(BigInteger groupId, BigInteger volume) {
        final Function function = new Function(
                FUNC_RECORDGROUPVOLUME, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(groupId), 
                new org.web3j.abi.datatypes.generated.Uint256(volume)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> recordInteraction(String a, String b) {
        final Function function = new Function(
                FUNC_RECORDINTERACTION, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, a), 
                new org.web3j.abi.datatypes.Address(160, b)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> recorder() {
        final Function function = new Function(FUNC_RECORDER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> setGroupPrimaryShard(BigInteger groupId, BigInteger shardId) {
        final Function function = new Function(
                FUNC_SETGROUPPRIMARYSHARD, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(groupId), 
                new org.web3j.abi.datatypes.generated.Uint256(shardId)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setRecorder(String _recorder) {
        final Function function = new Function(
                FUNC_SETRECORDER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _recorder)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setThetaA(BigInteger _thetaAWad) {
        final Function function = new Function(
                FUNC_SETTHETAA, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_thetaAWad)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> thetaA() {
        final Function function = new Function(FUNC_THETAA, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> totalInteractions(String param0) {
        final Function function = new Function(FUNC_TOTALINTERACTIONS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    @Deprecated
    public static AffinityManager load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new AffinityManager(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static AffinityManager load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new AffinityManager(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static AffinityManager load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new AffinityManager(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static AffinityManager load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new AffinityManager(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<AffinityManager> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, BigInteger _thetaAWad) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_thetaAWad)));
        return deployRemoteCall(AffinityManager.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<AffinityManager> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, BigInteger _thetaAWad) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_thetaAWad)));
        return deployRemoteCall(AffinityManager.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<AffinityManager> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, BigInteger _thetaAWad) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_thetaAWad)));
        return deployRemoteCall(AffinityManager.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<AffinityManager> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, BigInteger _thetaAWad) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_thetaAWad)));
        return deployRemoteCall(AffinityManager.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static class EdgeFormedEventResponse extends BaseEventResponse {
        public String a;

        public String b;

        public BigInteger affinityWad;
    }

    public static class GroupCreatedEventResponse extends BaseEventResponse {
        public BigInteger groupId;

        public String root;
    }

    public static class GroupShardAssignedEventResponse extends BaseEventResponse {
        public BigInteger groupId;

        public BigInteger shardId;
    }

    public static class GroupsMergedEventResponse extends BaseEventResponse {
        public BigInteger survivingGroup;

        public BigInteger absorbedGroup;
    }

    public static class InteractionRecordedEventResponse extends BaseEventResponse {
        public String a;

        public String b;

        public BigInteger pairCount;
    }

    public static class ThetaUpdatedEventResponse extends BaseEventResponse {
        public BigInteger thetaA;
    }
}
