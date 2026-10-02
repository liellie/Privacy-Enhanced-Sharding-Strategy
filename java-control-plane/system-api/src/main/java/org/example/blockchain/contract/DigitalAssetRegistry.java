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
import org.web3j.tuples.generated.Tuple6;
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
public class DigitalAssetRegistry extends Contract {
    public static final String BINARY = "608060405234801561001057600080fd5b5060405161119d38038061119d83398101604081905261002f91610062565b60008054336001600160a01b031991821617909155600180549091166001600160a01b0392909216919091179055610092565b60006020828403121561007457600080fd5b81516001600160a01b038116811461008b57600080fd5b9392505050565b6110fc806100a16000396000f3fe608060405234801561001057600080fd5b50600436106101005760003560e01c8063c66ddb2011610097578063e6a53ada11610066578063e6a53ada14610328578063eafe7a741461033b578063f851a44014610344578063fec8d9371461035757600080fd5b8063c66ddb201461023a578063cf35bdd014610265578063d7ef0c04146102f0578063ddbe82161461031557600080fd5b8063867ee170116100d3578063867ee170146101d35780638f5d92ba146101e6578063aef0ac7c146101f9578063bcd5c6ad1461022757600080fd5b8063063b82b9146101055780633956cdfe1461011a578063441c0b941461012d5780638562e514146101b0575b600080fd5b610118610113366004610e8a565b61036a565b005b610118610128366004610eac565b6103de565b61019b61013b366004610ee1565b604080516bffffffffffffffffffffffff19606095861b81166020808401919091529490951b9094166034850152604880850192909252805180850390920182526068909301835280519082012060009081526005909152205460ff1690565b60405190151581526020015b60405180910390f35b61019b6101be366004610f0d565b60056020526000908152604090205460ff1681565b6101186101e1366004610f26565b610696565b6101186101f4366004610f52565b610851565b610219610207366004610f0d565b60009081526003602052604090205490565b6040519081526020016101a7565b61019b610235366004610f52565b6108e0565b60015461024d906001600160a01b031681565b6040516001600160a01b0390911681526020016101a7565b6102b7610273366004610f0d565b60026020819052600091825260409091208054600182015492820154600383015460048401546005909401546001600160a01b039093169493919290919060ff1686565b604080516001600160a01b03909716875260208701959095529385019290925260608401526080830152151560a082015260c0016101a7565b6103036102fe366004610fd5565b6109d1565b6040516101a79695949392919061100d565b610118610323366004610f0d565b610a53565b610219610336366004610f0d565b610ae3565b61021960045481565b60005461024d906001600160a01b031681565b610219610365366004610f26565b610ca5565b6000546001600160a01b031633146103bc5760405162461bcd60e51b815260206004820152601060248201526f20b9b9b2ba1d103737ba1030b236b4b760811b60448201526064015b60405180910390fd5b600180546001600160a01b0319166001600160a01b0392909216919091179055565b600083815260026020526040902060050154839060ff166104115760405162461bcd60e51b81526004016103b39061105f565b6000818152600260205260409020546001600160a01b0316331461046a5760405162461bcd60e51b815260206004820152601060248201526f20b9b9b2ba1d103737ba1037bbb732b960811b60448201526064016103b3565b6001600160a01b0383161580159061048b57506001600160a01b0383163314155b6104ce5760405162461bcd60e51b8152602060048201526014602482015273105cdcd95d0e88189859081c9958da5c1a595b9d60621b60448201526064016103b3565b600084815260026020818152604080842080546001600160a01b031981166001600160a01b038a8116918217845560018085018b905560038752858920865160c081018852828152929094168288018190529582019290925283870154606082015243608082015260a081018a90528254808301845592885294909620845160059092020180549296939590939192849260ff1990911691849081111561057757610577610ff7565b021790555060208201518154610100600160a81b0319166101006001600160a01b03928316021782556040830151600180840180546001600160a01b03191692841692909217909155606084015160028401556080840151600384015560a0909301516004909201919091559054161561064d5760015460405163445f767960e01b81526001600160a01b03838116600483015287811660248301529091169063445f767990604401600060405180830381600087803b15801561063a57600080fd5b505af192505050801561064b575060015b505b846001600160a01b0316816001600160a01b0316877fa993eb3a10693085bc7afc1de0202310fbd5992b9e51edd263b198f62f20cdae60405160405180910390a4505050505050565b600083815260026020526040902060050154839060ff166106c95760405162461bcd60e51b81526004016103b39061105f565b6000818152600260205260409020546001600160a01b031633146107225760405162461bcd60e51b815260206004820152601060248201526f20b9b9b2ba1d103737ba1037bbb732b960811b60448201526064016103b3565b600084815260026020818152604080842080840188905560038082018890558352818520825160c08101845285815233818601819052938101939093526060830189905243608084015260018083015460a0850152815480820183559187529390952082516005909602018054919592949093849260ff19169184908111156107ad576107ad610ff7565b02179055506020828101518254610100600160a81b0319166101006001600160a01b03928316021783556040808501516001850180546001600160a01b0319169190931617909155606084015160028401556080840151600384015560a090930151600490920191909155905185815286917f9c06c04824dfb4ebf41804d51120c24a2ab63d6230efe7b7bc5a199f837dd3ad910160405180910390a25050505050565b61085d848484846108e0565b6108a05760405162461bcd60e51b815260206004820152601460248201527320b9b9b2ba1d1034b73b30b634b210383937b7b360611b60448201526064016103b3565b837f756cf5cce7fdc999bbd78e085eaec6c735c3d50da26bf3ce36d1d6a84ff21843846040516108d291815260200190565b60405180910390a250505050565b60008481526002602052604081206005015460ff166109115760405162461bcd60e51b81526004016103b39061105f565b8360005b838110156109b257600085858381811061093157610931611089565b905060200201359050808311156109715760408051602081018390529081018490526060016040516020818303038152906040528051906020012061099c565b6040805160208101859052908101829052606001604051602081830303815290604052805190602001205b92505080806109aa9061109f565b915050610915565b5060008681526002602081905260409091200154149050949350505050565b6000806000806000806000600360008a81526020019081526020016000208881548110610a0057610a00611089565b60009182526020909120600590910201805460018201546002830154600384015460049094015460ff84169e6101009094046001600160a01b039081169e509092169b5099509197509095509350505050565b80610aa05760405162461bcd60e51b815260206004820152601760248201527f41737365743a20656d70747920636f6d6d69746d656e7400000000000000000060448201526064016103b3565b600081815260056020526040808220805460ff191660011790555182917f99db3d1fef7c1c437760e77a77595b016f8959861b7119f975a64e51bd0da4a891a250565b6000818152600360205260408120815b8154811015610c9e5782828281548110610b0f57610b0f611089565b600091825260209091206005909102015460ff166002811115610b3457610b34610ff7565b838381548110610b4657610b46611089565b906000526020600020906005020160000160019054906101000a90046001600160a01b0316848481548110610b7d57610b7d611089565b906000526020600020906005020160010160009054906101000a90046001600160a01b0316858581548110610bb457610bb4611089565b906000526020600020906005020160020154868681548110610bd857610bd8611089565b906000526020600020906005020160030154878781548110610bfc57610bfc611089565b906000526020600020906005020160040154604051602001610c73979695949392919096875260f89590951b6001600160f81b0319166020870152606093841b6bffffffffffffffffffffffff1990811660218801529290931b909116603585015260498401526069830152608982015260a90190565b6040516020818303038152906040528051906020012092508080610c969061109f565b915050610af3565b5050919050565b6000600460008154610cb69061109f565b91829055506040805160c0808201835233825260208083018781528385018a8152606085018a81524360808701908152600160a0880181815260008b815260028089528b82209a518b546001600160a01b0319166001600160a01b03909116178b559651928a019290925593519488019490945590516003808801919091559051600487015590516005909501805460ff191695151595909517909455929052828220835191820190935292935090919081908152600060208083018290523360408401526060830189905243608084015260a09092018690528354600181810186559482529190208251600590920201805492939092839160ff1990911690836002811115610dc857610dc8610ff7565b02179055506020828101518254610100600160a81b0319166101006001600160a01b03928316021783556040808501516001850180546001600160a01b0319169190931617909155606084015160028401556080840151600384015560a0909301516004909201919091559051838152339183917ff54837c9ba235cb37cd834add22a366a600686d55bc588af257f7928dacb198b910160405180910390a39392505050565b80356001600160a01b0381168114610e8557600080fd5b919050565b600060208284031215610e9c57600080fd5b610ea582610e6e565b9392505050565b600080600060608486031215610ec157600080fd5b83359250610ed160208501610e6e565b9150604084013590509250925092565b600080600060608486031215610ef657600080fd5b610eff84610e6e565b9250610ed160208501610e6e565b600060208284031215610f1f57600080fd5b5035919050565b600080600060608486031215610f3b57600080fd5b505081359360208301359350604090920135919050565b60008060008060608587031215610f6857600080fd5b8435935060208501359250604085013567ffffffffffffffff80821115610f8e57600080fd5b818701915087601f830112610fa257600080fd5b813581811115610fb157600080fd5b8860208260051b8501011115610fc657600080fd5b95989497505060200194505050565b60008060408385031215610fe857600080fd5b50508035926020909101359150565b634e487b7160e01b600052602160045260246000fd5b60c081016003881061102f57634e487b7160e01b600052602160045260246000fd5b9681526001600160a01b0395861660208201529390941660408401526060830191909152608082015260a0015290565b60208082526010908201526f105cdcd95d0e881b9bdd08199bdd5b9960821b604082015260600190565b634e487b7160e01b600052603260045260246000fd5b6000600182016110bf57634e487b7160e01b600052601160045260246000fd5b506001019056fea26469706673582212209ea52ef40c454e71885d57a2205e30f169227d75dab32a85c90137e078ed21b764736f6c63430008130033";

    public static final String FUNC_ADMIN = "admin";

    public static final String FUNC_AFFINITY = "affinity";

    public static final String FUNC_ASSETCOUNT = "assetCount";

    public static final String FUNC_ASSETS = "assets";

    public static final String FUNC_COMMITRELATIONSHIP = "commitRelationship";

    public static final String FUNC_CREATEASSET = "createAsset";

    public static final String FUNC_DISCLOSEATTRIBUTE = "discloseAttribute";

    public static final String FUNC_PROVENANCEAT = "provenanceAt";

    public static final String FUNC_PROVENANCEDIGEST = "provenanceDigest";

    public static final String FUNC_PROVENANCELENGTH = "provenanceLength";

    public static final String FUNC_RELATIONSHIPCOMMITMENTS = "relationshipCommitments";

    public static final String FUNC_SETAFFINITY = "setAffinity";

    public static final String FUNC_TRANSFERASSET = "transferAsset";

    public static final String FUNC_UPDATEMETADATA = "updateMetadata";

    public static final String FUNC_VERIFYATTRIBUTE = "verifyAttribute";

    public static final String FUNC_VERIFYRELATIONSHIP = "verifyRelationship";

    public static final Event ASSETCREATED_EVENT = new Event("AssetCreated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event ASSETTRANSFERRED_EVENT = new Event("AssetTransferred", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event ATTRIBUTEDISCLOSED_EVENT = new Event("AttributeDisclosed", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Bytes32>() {}));
    ;

    public static final Event METADATAUPDATED_EVENT = new Event("MetadataUpdated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Bytes32>() {}));
    ;

    public static final Event RELATIONSHIPCOMMITTED_EVENT = new Event("RelationshipCommitted", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}));
    ;

    @Deprecated
    protected DigitalAssetRegistry(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected DigitalAssetRegistry(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected DigitalAssetRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected DigitalAssetRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<AssetCreatedEventResponse> getAssetCreatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ASSETCREATED_EVENT, transactionReceipt);
        ArrayList<AssetCreatedEventResponse> responses = new ArrayList<AssetCreatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AssetCreatedEventResponse typedResponse = new AssetCreatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.assetId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.owner = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.shardId = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static AssetCreatedEventResponse getAssetCreatedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ASSETCREATED_EVENT, log);
        AssetCreatedEventResponse typedResponse = new AssetCreatedEventResponse();
        typedResponse.log = log;
        typedResponse.assetId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.owner = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.shardId = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<AssetCreatedEventResponse> assetCreatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getAssetCreatedEventFromLog(log));
    }

    public Flowable<AssetCreatedEventResponse> assetCreatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ASSETCREATED_EVENT));
        return assetCreatedEventFlowable(filter);
    }

    public static List<AssetTransferredEventResponse> getAssetTransferredEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ASSETTRANSFERRED_EVENT, transactionReceipt);
        ArrayList<AssetTransferredEventResponse> responses = new ArrayList<AssetTransferredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AssetTransferredEventResponse typedResponse = new AssetTransferredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.assetId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.from = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.to = (String) eventValues.getIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static AssetTransferredEventResponse getAssetTransferredEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ASSETTRANSFERRED_EVENT, log);
        AssetTransferredEventResponse typedResponse = new AssetTransferredEventResponse();
        typedResponse.log = log;
        typedResponse.assetId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.from = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.to = (String) eventValues.getIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<AssetTransferredEventResponse> assetTransferredEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getAssetTransferredEventFromLog(log));
    }

    public Flowable<AssetTransferredEventResponse> assetTransferredEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ASSETTRANSFERRED_EVENT));
        return assetTransferredEventFlowable(filter);
    }

    public static List<AttributeDisclosedEventResponse> getAttributeDisclosedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ATTRIBUTEDISCLOSED_EVENT, transactionReceipt);
        ArrayList<AttributeDisclosedEventResponse> responses = new ArrayList<AttributeDisclosedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AttributeDisclosedEventResponse typedResponse = new AttributeDisclosedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.assetId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.leaf = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static AttributeDisclosedEventResponse getAttributeDisclosedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ATTRIBUTEDISCLOSED_EVENT, log);
        AttributeDisclosedEventResponse typedResponse = new AttributeDisclosedEventResponse();
        typedResponse.log = log;
        typedResponse.assetId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.leaf = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<AttributeDisclosedEventResponse> attributeDisclosedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getAttributeDisclosedEventFromLog(log));
    }

    public Flowable<AttributeDisclosedEventResponse> attributeDisclosedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ATTRIBUTEDISCLOSED_EVENT));
        return attributeDisclosedEventFlowable(filter);
    }

    public static List<MetadataUpdatedEventResponse> getMetadataUpdatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(METADATAUPDATED_EVENT, transactionReceipt);
        ArrayList<MetadataUpdatedEventResponse> responses = new ArrayList<MetadataUpdatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            MetadataUpdatedEventResponse typedResponse = new MetadataUpdatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.assetId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.metadataRoot = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static MetadataUpdatedEventResponse getMetadataUpdatedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(METADATAUPDATED_EVENT, log);
        MetadataUpdatedEventResponse typedResponse = new MetadataUpdatedEventResponse();
        typedResponse.log = log;
        typedResponse.assetId = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.metadataRoot = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<MetadataUpdatedEventResponse> metadataUpdatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getMetadataUpdatedEventFromLog(log));
    }

    public Flowable<MetadataUpdatedEventResponse> metadataUpdatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(METADATAUPDATED_EVENT));
        return metadataUpdatedEventFlowable(filter);
    }

    public static List<RelationshipCommittedEventResponse> getRelationshipCommittedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(RELATIONSHIPCOMMITTED_EVENT, transactionReceipt);
        ArrayList<RelationshipCommittedEventResponse> responses = new ArrayList<RelationshipCommittedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RelationshipCommittedEventResponse typedResponse = new RelationshipCommittedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.commitment = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RelationshipCommittedEventResponse getRelationshipCommittedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(RELATIONSHIPCOMMITTED_EVENT, log);
        RelationshipCommittedEventResponse typedResponse = new RelationshipCommittedEventResponse();
        typedResponse.log = log;
        typedResponse.commitment = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<RelationshipCommittedEventResponse> relationshipCommittedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRelationshipCommittedEventFromLog(log));
    }

    public Flowable<RelationshipCommittedEventResponse> relationshipCommittedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(RELATIONSHIPCOMMITTED_EVENT));
        return relationshipCommittedEventFlowable(filter);
    }

    public RemoteFunctionCall<String> admin() {
        final Function function = new Function(FUNC_ADMIN, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> affinity() {
        final Function function = new Function(FUNC_AFFINITY, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<BigInteger> assetCount() {
        final Function function = new Function(FUNC_ASSETCOUNT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<Tuple6<String, BigInteger, byte[], byte[], BigInteger, Boolean>> assets(BigInteger param0) {
        final Function function = new Function(FUNC_ASSETS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Uint256>() {}, new TypeReference<Bytes32>() {}, new TypeReference<Bytes32>() {}, new TypeReference<Uint256>() {}, new TypeReference<Bool>() {}));
        return new RemoteFunctionCall<Tuple6<String, BigInteger, byte[], byte[], BigInteger, Boolean>>(function,
                new Callable<Tuple6<String, BigInteger, byte[], byte[], BigInteger, Boolean>>() {
                    @Override
                    public Tuple6<String, BigInteger, byte[], byte[], BigInteger, Boolean> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple6<String, BigInteger, byte[], byte[], BigInteger, Boolean>(
                                (String) results.get(0).getValue(), 
                                (BigInteger) results.get(1).getValue(), 
                                (byte[]) results.get(2).getValue(), 
                                (byte[]) results.get(3).getValue(), 
                                (BigInteger) results.get(4).getValue(), 
                                (Boolean) results.get(5).getValue());
                    }
                });
    }

    public RemoteFunctionCall<TransactionReceipt> commitRelationship(byte[] commitment) {
        final Function function = new Function(
                FUNC_COMMITRELATIONSHIP, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(commitment)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> createAsset(byte[] metadataRoot, byte[] encryptedRef, BigInteger shardId) {
        final Function function = new Function(
                FUNC_CREATEASSET, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(metadataRoot), 
                new org.web3j.abi.datatypes.generated.Bytes32(encryptedRef), 
                new org.web3j.abi.datatypes.generated.Uint256(shardId)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> discloseAttribute(BigInteger assetId, byte[] leaf, List<byte[]> proof) {
        final Function function = new Function(
                FUNC_DISCLOSEATTRIBUTE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(assetId), 
                new org.web3j.abi.datatypes.generated.Bytes32(leaf), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Bytes32>(
                        org.web3j.abi.datatypes.generated.Bytes32.class,
                        org.web3j.abi.Utils.typeMap(proof, org.web3j.abi.datatypes.generated.Bytes32.class))), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Tuple6<BigInteger, String, String, byte[], BigInteger, BigInteger>> provenanceAt(BigInteger assetId, BigInteger index) {
        final Function function = new Function(FUNC_PROVENANCEAT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(assetId), 
                new org.web3j.abi.datatypes.generated.Uint256(index)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}, new TypeReference<Address>() {}, new TypeReference<Address>() {}, new TypeReference<Bytes32>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
        return new RemoteFunctionCall<Tuple6<BigInteger, String, String, byte[], BigInteger, BigInteger>>(function,
                new Callable<Tuple6<BigInteger, String, String, byte[], BigInteger, BigInteger>>() {
                    @Override
                    public Tuple6<BigInteger, String, String, byte[], BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple6<BigInteger, String, String, byte[], BigInteger, BigInteger>(
                                (BigInteger) results.get(0).getValue(), 
                                (String) results.get(1).getValue(), 
                                (String) results.get(2).getValue(), 
                                (byte[]) results.get(3).getValue(), 
                                (BigInteger) results.get(4).getValue(), 
                                (BigInteger) results.get(5).getValue());
                    }
                });
    }

    public RemoteFunctionCall<byte[]> provenanceDigest(BigInteger assetId) {
        final Function function = new Function(FUNC_PROVENANCEDIGEST, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(assetId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<BigInteger> provenanceLength(BigInteger assetId) {
        final Function function = new Function(FUNC_PROVENANCELENGTH, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(assetId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<Boolean> relationshipCommitments(byte[] param0) {
        final Function function = new Function(FUNC_RELATIONSHIPCOMMITMENTS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<TransactionReceipt> setAffinity(String a) {
        final Function function = new Function(
                FUNC_SETAFFINITY, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, a)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> transferAsset(BigInteger assetId, String to, BigInteger newShardId) {
        final Function function = new Function(
                FUNC_TRANSFERASSET, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(assetId), 
                new org.web3j.abi.datatypes.Address(160, to), 
                new org.web3j.abi.datatypes.generated.Uint256(newShardId)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> updateMetadata(BigInteger assetId, byte[] newRoot, byte[] newEncryptedRef) {
        final Function function = new Function(
                FUNC_UPDATEMETADATA, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(assetId), 
                new org.web3j.abi.datatypes.generated.Bytes32(newRoot), 
                new org.web3j.abi.datatypes.generated.Bytes32(newEncryptedRef)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> verifyAttribute(BigInteger assetId, byte[] leaf, List<byte[]> proof) {
        final Function function = new Function(FUNC_VERIFYATTRIBUTE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(assetId), 
                new org.web3j.abi.datatypes.generated.Bytes32(leaf), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Bytes32>(
                        org.web3j.abi.datatypes.generated.Bytes32.class,
                        org.web3j.abi.Utils.typeMap(proof, org.web3j.abi.datatypes.generated.Bytes32.class))), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<Boolean> verifyRelationship(String oi, String oj, byte[] salt) {
        final Function function = new Function(FUNC_VERIFYRELATIONSHIP, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, oi), 
                new org.web3j.abi.datatypes.Address(160, oj), 
                new org.web3j.abi.datatypes.generated.Bytes32(salt)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    @Deprecated
    public static DigitalAssetRegistry load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new DigitalAssetRegistry(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static DigitalAssetRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new DigitalAssetRegistry(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static DigitalAssetRegistry load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new DigitalAssetRegistry(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static DigitalAssetRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new DigitalAssetRegistry(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<DigitalAssetRegistry> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, String _affinity) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _affinity)));
        return deployRemoteCall(DigitalAssetRegistry.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<DigitalAssetRegistry> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, String _affinity) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _affinity)));
        return deployRemoteCall(DigitalAssetRegistry.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<DigitalAssetRegistry> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String _affinity) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _affinity)));
        return deployRemoteCall(DigitalAssetRegistry.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<DigitalAssetRegistry> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String _affinity) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _affinity)));
        return deployRemoteCall(DigitalAssetRegistry.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static class AssetCreatedEventResponse extends BaseEventResponse {
        public BigInteger assetId;

        public String owner;

        public BigInteger shardId;
    }

    public static class AssetTransferredEventResponse extends BaseEventResponse {
        public BigInteger assetId;

        public String from;

        public String to;
    }

    public static class AttributeDisclosedEventResponse extends BaseEventResponse {
        public BigInteger assetId;

        public byte[] leaf;
    }

    public static class MetadataUpdatedEventResponse extends BaseEventResponse {
        public BigInteger assetId;

        public byte[] metadataRoot;
    }

    public static class RelationshipCommittedEventResponse extends BaseEventResponse {
        public byte[] commitment;
    }
}
