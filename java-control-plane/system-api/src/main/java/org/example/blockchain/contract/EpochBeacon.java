package org.example.blockchain.contract;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
public class EpochBeacon extends Contract {
    public static final String BINARY = "60a060405234801561001057600080fd5b50604051610e45380380610e4583398101604081905261002f91610117565b600082116100975760405162461bcd60e51b815260206004820152602a60248201527f45706f6368426561636f6e3a2065706f6368206c656e677468206d75737420626044820152696520706f73697469766560b01b60648201526084015b60405180910390fd5b600081116100f75760405162461bcd60e51b815260206004820152602760248201527f45706f6368426561636f6e3a207468726573686f6c64206d75737420626520706044820152666f73697469766560c81b606482015260840161008e565b608091909152600355600080546001600160a01b0319163317905561013b565b6000806040838503121561012a57600080fd5b505080516020909101519092909150565b608051610ce861015d6000396000818161021b01526107360152610ce86000f3fe608060405234801561001057600080fd5b506004361061010b5760003560e01c806357d775f8116100a2578063b8e216ea11610071578063b8e216ea14610278578063ecfd89281461028b578063edcf55a214610294578063f2f03877146102b4578063f851a440146102c757600080fd5b806357d775f814610216578063766718081461023d578063a7dfd6c214610245578063b579184f1461026557600080fd5b80631d21c669116100de5780631d21c669146101a257806333727c4d146101b55780634036778f146101d8578063466907f9146101eb57600080fd5b806305261aea146101105780630885eff41461012557806316b5551d146101415780631d0d35f51461017f575b600080fd5b61012361011e366004610bc4565b6102f2565b005b61012e60035481565b6040519081526020015b60405180910390f35b61016f61014f366004610bf9565b600560209081526000928352604080842090915290825290205460ff1681565b6040519015158152602001610138565b61016f61018d366004610c25565b60016020526000908152604090205460ff1681565b61012e6101b0366004610bc4565b61044f565b61016f6101c3366004610bc4565b60096020526000908152604090205460ff1681565b6101236101e6366004610c47565b6104c0565b61012e6101f9366004610bf9565b600460209081526000928352604080842090915290825290205481565b61012e7f000000000000000000000000000000000000000000000000000000000000000081565b61012e61072f565b61012e610253366004610bc4565b60086020526000908152604090205481565b610123610273366004610c25565b610760565b610123610286366004610bc4565b6108f0565b61012e60025481565b61012e6102a2366004610bc4565b60066020526000908152604090205481565b6101236102c2366004610c47565b6109a6565b6000546102da906001600160a01b031681565b6040516001600160a01b039091168152602001610138565b60008181526009602052604090205460ff16156103565760405162461bcd60e51b815260206004820152601e60248201527f45706f6368426561636f6e3a20616c72656164792066696e616c697a6564000060448201526064015b60405180910390fd5b60035460008281526006602052604090205410156103b65760405162461bcd60e51b815260206004820152601f60248201527f45706f6368426561636f6e3a206e6f7420656e6f7567682072657665616c7300604482015260640161034d565b600081815260076020908152604080832054815192830152810183905260600160408051601f1981840301815282825280516020918201206000868152600883528381208290556009909252919020805460ff19166001179055915082907f3c8430a9271fd80e165f47c92d32ce3e116912d4ff217729f8fc3ddebc0503be906104439084815260200190565b60405180910390a25050565b60008181526009602052604081205460ff166104ad5760405162461bcd60e51b815260206004820181905260248201527f45706f6368426561636f6e3a2065706f6368206e6f742066696e616c697a6564604482015260640161034d565b5060009081526008602052604090205490565b3360009081526001602052604090205460ff1661051f5760405162461bcd60e51b815260206004820152601e60248201527f45706f6368426561636f6e3a206e6f74206120636f6e7472696275746f720000604482015260640161034d565b60008281526009602052604090205460ff161561057e5760405162461bcd60e51b815260206004820152601c60248201527f45706f6368426561636f6e3a2065706f63682066696e616c697a656400000000604482015260640161034d565b600082815260056020908152604080832033845290915290205460ff16156105e85760405162461bcd60e51b815260206004820152601d60248201527f45706f6368426561636f6e3a20616c72656164792072657665616c6564000000604482015260640161034d565b803360405160200161061692919091825260601b6bffffffffffffffffffffffff1916602082015260340190565b60408051601f198184030181529181528151602092830120600085815260048452828120338252909352912054146106905760405162461bcd60e51b815260206004820181905260248201527f45706f6368426561636f6e3a20636f6d6d69746d656e74206d69736d61746368604482015260640161034d565b60008281526005602090815260408083203384528252808320805460ff1916600190811790915585845260078352818420805486189055600690925282208054919290916106df908490610c69565b9091555050600082815260066020908152604091829020549151918252339184917ff87397f8ccf18c41bfee81144299ad64e11085c5a27b280fde98beeefd3e71bd910160405180910390a35050565b600061075b7f000000000000000000000000000000000000000000000000000000000000000043610c90565b905090565b6000546001600160a01b031633146107b35760405162461bcd60e51b815260206004820152601660248201527522b837b1b42132b0b1b7b71d103737ba1030b236b4b760511b604482015260640161034d565b6001600160a01b0381166108095760405162461bcd60e51b815260206004820152601960248201527f45706f6368426561636f6e3a207a65726f206164647265737300000000000000604482015260640161034d565b6001600160a01b03811660009081526001602052604090205460ff161561087d5760405162461bcd60e51b815260206004820152602260248201527f45706f6368426561636f6e3a20616c7265616479206120636f6e74726962757460448201526137b960f11b606482015260840161034d565b6001600160a01b03811660009081526001602081905260408220805460ff19168217905560028054919290916108b4908490610c69565b90915550506040516001600160a01b038216907f07681ffd9d424587412481b41d2ee582bb7bca757f626b0477051f302947027290600090a250565b6000546001600160a01b031633146109435760405162461bcd60e51b815260206004820152601660248201527522b837b1b42132b0b1b7b71d103737ba1030b236b4b760511b604482015260640161034d565b60008111801561095557506002548111155b6109a15760405162461bcd60e51b815260206004820152601e60248201527f45706f6368426561636f6e3a20696e76616c6964207468726573686f6c640000604482015260640161034d565b600355565b3360009081526001602052604090205460ff16610a055760405162461bcd60e51b815260206004820152601e60248201527f45706f6368426561636f6e3a206e6f74206120636f6e7472696275746f720000604482015260640161034d565b610a0d61072f565b821015610a675760405162461bcd60e51b815260206004820152602260248201527f45706f6368426561636f6e3a2065706f636820616c726561647920656c617073604482015261195960f21b606482015260840161034d565b60008281526009602052604090205460ff1615610ac65760405162461bcd60e51b815260206004820152601c60248201527f45706f6368426561636f6e3a2065706f63682066696e616c697a656400000000604482015260640161034d565b600082815260046020908152604080832033845290915290205415610b2d5760405162461bcd60e51b815260206004820152601e60248201527f45706f6368426561636f6e3a20616c726561647920636f6d6d69747465640000604482015260640161034d565b80610b7a5760405162461bcd60e51b815260206004820152601d60248201527f45706f6368426561636f6e3a20656d70747920636f6d6d69746d656e74000000604482015260640161034d565b600082815260046020908152604080832033808552925280832084905551909184917f452c97fab23345463f6fbb454d3bf4f408941c88abe2e79586032f5c8f9711fb9190a35050565b600060208284031215610bd657600080fd5b5035919050565b80356001600160a01b0381168114610bf457600080fd5b919050565b60008060408385031215610c0c57600080fd5b82359150610c1c60208401610bdd565b90509250929050565b600060208284031215610c3757600080fd5b610c4082610bdd565b9392505050565b60008060408385031215610c5a57600080fd5b50508035926020909101359150565b80820180821115610c8a57634e487b7160e01b600052601160045260246000fd5b92915050565b600082610cad57634e487b7160e01b600052601260045260246000fd5b50049056fea26469706673582212202419596026adf3146f9f88e774909f8cc8133c6ad18f8503afb85d605bc14fd664736f6c63430008130033";

    public static final String FUNC_ADDCONTRIBUTOR = "addContributor";

    public static final String FUNC_ADMIN = "admin";

    public static final String FUNC_BEACON = "beacon";

    public static final String FUNC_BEACONOF = "beaconOf";

    public static final String FUNC_COMMIT = "commit";

    public static final String FUNC_COMMITMENTOF = "commitmentOf";

    public static final String FUNC_CONTRIBUTORCOUNT = "contributorCount";

    public static final String FUNC_CURRENTEPOCH = "currentEpoch";

    public static final String FUNC_EPOCHLENGTH = "epochLength";

    public static final String FUNC_FINALIZE = "finalize";

    public static final String FUNC_HASREVEALED = "hasRevealed";

    public static final String FUNC_ISCONTRIBUTOR = "isContributor";

    public static final String FUNC_ISFINALIZED = "isFinalized";

    public static final String FUNC_REVEAL = "reveal";

    public static final String FUNC_REVEALCOUNT = "revealCount";

    public static final String FUNC_REVEALTHRESHOLD = "revealThreshold";

    public static final String FUNC_SETREVEALTHRESHOLD = "setRevealThreshold";

    public static final Event BEACONFINALIZED_EVENT = new Event("BeaconFinalized", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Bytes32>() {}));
    ;

    public static final Event COMMITTED_EVENT = new Event("Committed", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event CONTRIBUTORADDED_EVENT = new Event("ContributorAdded", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}));
    ;

    public static final Event REVEALED_EVENT = new Event("Revealed", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected EpochBeacon(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected EpochBeacon(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected EpochBeacon(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected EpochBeacon(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<BeaconFinalizedEventResponse> getBeaconFinalizedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(BEACONFINALIZED_EVENT, transactionReceipt);
        ArrayList<BeaconFinalizedEventResponse> responses = new ArrayList<BeaconFinalizedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            BeaconFinalizedEventResponse typedResponse = new BeaconFinalizedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.epoch = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.beacon = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static BeaconFinalizedEventResponse getBeaconFinalizedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(BEACONFINALIZED_EVENT, log);
        BeaconFinalizedEventResponse typedResponse = new BeaconFinalizedEventResponse();
        typedResponse.log = log;
        typedResponse.epoch = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.beacon = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<BeaconFinalizedEventResponse> beaconFinalizedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getBeaconFinalizedEventFromLog(log));
    }

    public Flowable<BeaconFinalizedEventResponse> beaconFinalizedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(BEACONFINALIZED_EVENT));
        return beaconFinalizedEventFlowable(filter);
    }

    public static List<CommittedEventResponse> getCommittedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(COMMITTED_EVENT, transactionReceipt);
        ArrayList<CommittedEventResponse> responses = new ArrayList<CommittedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            CommittedEventResponse typedResponse = new CommittedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.epoch = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.who = (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static CommittedEventResponse getCommittedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(COMMITTED_EVENT, log);
        CommittedEventResponse typedResponse = new CommittedEventResponse();
        typedResponse.log = log;
        typedResponse.epoch = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.who = (String) eventValues.getIndexedValues().get(1).getValue();
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

    public static List<ContributorAddedEventResponse> getContributorAddedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(CONTRIBUTORADDED_EVENT, transactionReceipt);
        ArrayList<ContributorAddedEventResponse> responses = new ArrayList<ContributorAddedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ContributorAddedEventResponse typedResponse = new ContributorAddedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.who = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ContributorAddedEventResponse getContributorAddedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(CONTRIBUTORADDED_EVENT, log);
        ContributorAddedEventResponse typedResponse = new ContributorAddedEventResponse();
        typedResponse.log = log;
        typedResponse.who = (String) eventValues.getIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<ContributorAddedEventResponse> contributorAddedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getContributorAddedEventFromLog(log));
    }

    public Flowable<ContributorAddedEventResponse> contributorAddedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(CONTRIBUTORADDED_EVENT));
        return contributorAddedEventFlowable(filter);
    }

    public static List<RevealedEventResponse> getRevealedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(REVEALED_EVENT, transactionReceipt);
        ArrayList<RevealedEventResponse> responses = new ArrayList<RevealedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RevealedEventResponse typedResponse = new RevealedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.epoch = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.who = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.count = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RevealedEventResponse getRevealedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(REVEALED_EVENT, log);
        RevealedEventResponse typedResponse = new RevealedEventResponse();
        typedResponse.log = log;
        typedResponse.epoch = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.who = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.count = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<RevealedEventResponse> revealedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRevealedEventFromLog(log));
    }

    public Flowable<RevealedEventResponse> revealedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(REVEALED_EVENT));
        return revealedEventFlowable(filter);
    }

    public RemoteFunctionCall<TransactionReceipt> addContributor(String who) {
        final Function function = new Function(
                FUNC_ADDCONTRIBUTOR, 
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

    public RemoteFunctionCall<byte[]> beacon(BigInteger epoch) {
        final Function function = new Function(FUNC_BEACON, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(epoch)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<byte[]> beaconOf(BigInteger param0) {
        final Function function = new Function(FUNC_BEACONOF, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<TransactionReceipt> commit(BigInteger epoch, byte[] commitment) {
        final Function function = new Function(
                FUNC_COMMIT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(epoch), 
                new org.web3j.abi.datatypes.generated.Bytes32(commitment)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<byte[]> commitmentOf(BigInteger param0, String param1) {
        final Function function = new Function(FUNC_COMMITMENTOF, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0), 
                new org.web3j.abi.datatypes.Address(160, param1)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<BigInteger> contributorCount() {
        final Function function = new Function(FUNC_CONTRIBUTORCOUNT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
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

    public RemoteFunctionCall<TransactionReceipt> finalize(BigInteger epoch) {
        final Function function = new Function(
                FUNC_FINALIZE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(epoch)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> hasRevealed(BigInteger param0, String param1) {
        final Function function = new Function(FUNC_HASREVEALED, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0), 
                new org.web3j.abi.datatypes.Address(160, param1)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<Boolean> isContributor(String param0) {
        final Function function = new Function(FUNC_ISCONTRIBUTOR, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<Boolean> isFinalized(BigInteger param0) {
        final Function function = new Function(FUNC_ISFINALIZED, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<TransactionReceipt> reveal(BigInteger epoch, byte[] secret) {
        final Function function = new Function(
                FUNC_REVEAL, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(epoch), 
                new org.web3j.abi.datatypes.generated.Bytes32(secret)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> revealCount(BigInteger param0) {
        final Function function = new Function(FUNC_REVEALCOUNT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> revealThreshold() {
        final Function function = new Function(FUNC_REVEALTHRESHOLD, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> setRevealThreshold(BigInteger t) {
        final Function function = new Function(
                FUNC_SETREVEALTHRESHOLD, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(t)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static EpochBeacon load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new EpochBeacon(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static EpochBeacon load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new EpochBeacon(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static EpochBeacon load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new EpochBeacon(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static EpochBeacon load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new EpochBeacon(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<EpochBeacon> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, BigInteger _epochLength, BigInteger _revealThreshold) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_epochLength), 
                new org.web3j.abi.datatypes.generated.Uint256(_revealThreshold)));
        return deployRemoteCall(EpochBeacon.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<EpochBeacon> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, BigInteger _epochLength, BigInteger _revealThreshold) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_epochLength), 
                new org.web3j.abi.datatypes.generated.Uint256(_revealThreshold)));
        return deployRemoteCall(EpochBeacon.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<EpochBeacon> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, BigInteger _epochLength, BigInteger _revealThreshold) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_epochLength), 
                new org.web3j.abi.datatypes.generated.Uint256(_revealThreshold)));
        return deployRemoteCall(EpochBeacon.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<EpochBeacon> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, BigInteger _epochLength, BigInteger _revealThreshold) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_epochLength), 
                new org.web3j.abi.datatypes.generated.Uint256(_revealThreshold)));
        return deployRemoteCall(EpochBeacon.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static class BeaconFinalizedEventResponse extends BaseEventResponse {
        public BigInteger epoch;

        public byte[] beacon;
    }

    public static class CommittedEventResponse extends BaseEventResponse {
        public BigInteger epoch;

        public String who;
    }

    public static class ContributorAddedEventResponse extends BaseEventResponse {
        public String who;
    }

    public static class RevealedEventResponse extends BaseEventResponse {
        public BigInteger epoch;

        public String who;

        public BigInteger count;
    }
}
