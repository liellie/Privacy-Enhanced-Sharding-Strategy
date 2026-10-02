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
public class ValidatorRegistry extends Contract {
    public static final String BINARY = "608060405234801561001057600080fd5b506040516116ec3803806116ec83398101604081905261002f916100c3565b600083116100835760405162461bcd60e51b815260206004820152601a60248201527f56616c696461746f723a2062616420736861726420636f756e74000000000000604482015260640160405180910390fd5b60008054336001600160a01b031991821617909155600180549091166001600160a01b03959095169490941790935560029190915560035560045561010e565b600080600080608085870312156100d957600080fd5b84516001600160a01b03811681146100f057600080fd5b60208601516040870151606090970151919890975090945092505050565b6115cf8061011d6000396000f3fe6080604052600436106101355760003560e01c80637b720df6116100ab578063b048e0561161006f578063b048e05614610355578063bdbab58014610375578063c96be4cb14610395578063d5f5d0c3146103b5578063f851a440146103d5578063fa52c7d8146103f557600080fd5b80637b720df6146102b25780637f8e3b4e146102d257806383072bbd146102e75780639f23887414610307578063a83061ff1461033f57600080fd5b8063375b3c0a116100fd578063375b3c0a146101cf5780633ccfd60b146101e55780634419d44d146101fa578063503aba731461022a578063650344511461025a578063794df89e1461029257600080fd5b806304e9c77a1461013a5780630f43a67714610163578063174af824146101785780631aa3a0081461019a5780632da7bcdb146101a2575b600080fd5b34801561014657600080fd5b5061015060025481565b6040519081526020015b60405180910390f35b34801561016f57600080fd5b50600654610150565b34801561018457600080fd5b50610198610193366004611380565b61046f565b005b61019861070e565b3480156101ae57600080fd5b506101c26101bd366004611399565b61092e565b60405161015a91906113bb565b3480156101db57600080fd5b5061015060035481565b3480156101f157600080fd5b506101986109a4565b34801561020657600080fd5b5061021a610215366004611424565b610c64565b604051901515815260200161015a565b34801561023657600080fd5b5061021a610245366004611380565b60086020526000908152604090205460ff1681565b34801561026657600080fd5b5060015461027a906001600160a01b031681565b6040516001600160a01b03909116815260200161015a565b34801561029e57600080fd5b5061021a6102ad36600461144e565b610cee565b3480156102be57600080fd5b506101506102cd366004611399565b610d73565b3480156102de57600080fd5b50610198610e64565b3480156102f357600080fd5b50610150610302366004611380565b610fec565b34801561031357600080fd5b50610150610322366004611399565b600091825260076020908152604080842092845291905290205490565b34801561034b57600080fd5b5061015060045481565b34801561036157600080fd5b5061027a610370366004611380565b611056565b34801561038157600080fd5b50610198610390366004611380565b611080565b3480156103a157600080fd5b506101986103b0366004611483565b611126565b3480156103c157600080fd5b506101506103d0366004611424565b611245565b3480156103e157600080fd5b5060005461027a906001600160a01b031681565b34801561040157600080fd5b50610443610410366004611483565b60056020526000908152604090208054600182015460028301546003909301549192909160ff8082169161010090041685565b60408051958652602086019490945292840191909152151560608301521515608082015260a00161015a565b60008181526008602052604090205460ff16156104d35760405162461bcd60e51b815260206004820152601960248201527f56616c696461746f723a20616c726561647920666f726d65640000000000000060448201526064015b60405180910390fd5b6001546040516333727c4d60e01b8152600481018390526001600160a01b03909116906333727c4d90602401602060405180830381865afa15801561051c573d6000803e3d6000fd5b505050506040513d601f19601f82011682018060405250810190610540919061149e565b61058c5760405162461bcd60e51b815260206004820152601f60248201527f56616c696461746f723a20626561636f6e206e6f742066696e616c697a65640060448201526064016104ca565b6000805b600654811015610652576000600682815481106105af576105af6114c0565b6000918252602090912001546001600160a01b031690506105d08185610c64565b6105da5750610640565b6000848152600760205260408120906105f38387611245565b815260208082019290925260400160009081208054600180820183559183529290912090910180546001600160a01b0319166001600160a01b03841617905561063c90846114ec565b9250505b8061064a816114ff565b915050610590565b506002548110156106b55760405162461bcd60e51b815260206004820152602760248201527f56616c696461746f723a206e6f7420656e6f756768206163746976652076616c604482015266696461746f727360c81b60648201526084016104ca565b60008281526008602052604090819020805460ff191660011790555182907f447158a908671c5924cfc03a811cc396d0e874d59a3f727cea779345ddb64211906107029084815260200190565b60405180910390a25050565b3360009081526005602052604090206003015460ff16156107715760405162461bcd60e51b815260206004820152601d60248201527f56616c696461746f723a20616c7265616479207265676973746572656400000060448201526064016104ca565b6003543410156107c35760405162461bcd60e51b815260206004820152601e60248201527f56616c696461746f723a207374616b652062656c6f77206d696e696d756d000060448201526064016104ca565b60015460408051630ecce30160e31b815290516000926001600160a01b03169163766718089160048083019260209291908290030181865afa15801561080d573d6000803e3d6000fd5b505050506040513d601f19601f820116820180604052508101906108319190611518565b61083c9060016114ec565b6040805160a08101825234808252602080830185815260008486018181526001606087018181526080880184815233808652600588528a862099518a55955189840155925160028901555160039097018054925115156101000261ff00199815159890981661ffff199093169290921796909617905560068054958601815590527ff652222313e28459528d920b65115c16c04f3efc82aaedc97be59f3f377c0d3f90930180546001600160a01b0319168417905583519182528101849052929350917f4fedf9289a156b214915bd5c2db58d3ee16acc185e80df66ee404e4573c821e191015b60405180910390a250565b600082815260076020908152604080832084845282529182902080548351818402810184019094528084526060939283018282801561099657602002820191906000526020600020905b81546001600160a01b03168152600190910190602001808311610978575b505050505090505b92915050565b336000908152600560205260409020600381015460ff16610a075760405162461bcd60e51b815260206004820152601960248201527f56616c696461746f723a206e6f7420726567697374657265640000000000000060448201526064016104ca565b8060020154600003610a5b5760405162461bcd60e51b815260206004820152601d60248201527f56616c696461746f723a2065786974206e6f742072657175657374656400000060448201526064016104ca565b6003810154610100900460ff1615610aaa5760405162461bcd60e51b815260206004820152601260248201527115985b1a59185d1bdc8e881cdb185cda195960721b60448201526064016104ca565b6004548160020154610abc91906114ec565b600160009054906101000a90046001600160a01b03166001600160a01b031663766718086040518163ffffffff1660e01b8152600401602060405180830381865afa158015610b0f573d6000803e3d6000fd5b505050506040513d601f19601f82011682018060405250810190610b339190611518565b1015610b815760405162461bcd60e51b815260206004820152601a60248201527f56616c696461746f723a207374696c6c20756e626f6e64696e6700000000000060448201526064016104ca565b8054600080835560038301805460ff19169055604051339083908381818185875af1925050503d8060008114610bd3576040519150601f19603f3d011682016040523d82523d6000602084013e610bd8565b606091505b5050905080610c295760405162461bcd60e51b815260206004820152601a60248201527f56616c696461746f723a207472616e73666572206661696c656400000000000060448201526064016104ca565b60405182815233907fa011545e03027296d3a8442724d6dc906b26fc29dfb764502d44c9db2762c0de906020015b60405180910390a2505050565b6001600160a01b0382166000908152600560205260408120600381015460ff161580610c9957506003810154610100900460ff165b15610ca857600091505061099e565b8060010154831015610cbe57600091505061099e565b600281015415801590610cd5575080600201548310155b15610ce457600091505061099e565b5060019392505050565b60008381526007602090815260408083208584529091528120815b8154811015610d6557836001600160a01b0316828281548110610d2e57610d2e6114c0565b6000918252602090912001546001600160a01b031603610d5357600192505050610d6c565b80610d5d816114ff565b915050610d09565b5060009150505b9392505050565b60008083118015610d845750603283105b610dd05760405162461bcd60e51b815260206004820152601b60248201527f56616c696461746f723a20706374206f7574206f662072616e6765000000000060448201526064016104ca565b6000610ddb8461130d565b905060008111610e2d5760405162461bcd60e51b815260206004820152601f60248201527f56616c696461746f723a20646976657267656e636520746f6f20736d616c6c0060448201526064016104ca565b80600181610e3e86620a939b611531565b610e4891906114ec565b610e529190611548565b610e5c9190611571565b949350505050565b336000908152600560205260409020600381015460ff168015610e9157506003810154610100900460ff16155b610ed55760405162461bcd60e51b815260206004820152601560248201527456616c696461746f723a206e6f742061637469766560581b60448201526064016104ca565b600281015415610f315760405162461bcd60e51b815260206004820152602160248201527f56616c696461746f723a206578697420616c72656164792072657175657374656044820152601960fa1b60648201526084016104ca565b600160009054906101000a90046001600160a01b03166001600160a01b031663766718086040518163ffffffff1660e01b8152600401602060405180830381865afa158015610f84573d6000803e3d6000fd5b505050506040513d601f19601f82011682018060405250810190610fa89190611518565b610fb39060016114ec565b6002820181905560405190815233907f239de5bbc96d8d3958406ea8afcce5ea046fbc26e65e14b9b03e33288362ec9390602001610923565b6000805b6006548110156110505761102b60068281548110611010576110106114c0565b6000918252602090912001546001600160a01b031684610c64565b1561103e5761103b6001836114ec565b91505b80611048816114ff565b915050610ff0565b50919050565b6006818154811061106657600080fd5b6000918252602090912001546001600160a01b0316905081565b6000546001600160a01b031633146110d15760405162461bcd60e51b81526020600482015260146024820152732b30b634b230ba37b91d103737ba1030b236b4b760611b60448201526064016104ca565b600081116111215760405162461bcd60e51b815260206004820152601a60248201527f56616c696461746f723a2062616420736861726420636f756e7400000000000060448201526064016104ca565b600255565b6000546001600160a01b031633146111775760405162461bcd60e51b81526020600482015260146024820152732b30b634b230ba37b91d103737ba1030b236b4b760611b60448201526064016104ca565b6001600160a01b0381166000908152600560205260409020600381015460ff1680156111ad57506003810154610100900460ff16155b6111f15760405162461bcd60e51b815260206004820152601560248201527456616c696461746f723a206e6f742061637469766560581b60448201526064016104ca565b80546000825560038201805461ff0019166101001790556040516001600160a01b038416907f17bddadfd7ec8898c3b9eadd0cf5ae77ba8d5df3a50e96ab86ec2dd711aa8fbb90610c579084815260200190565b600154604051631d21c66960e01b81526004810183905260009182916001600160a01b0390911690631d21c66990602401602060405180830381865afa158015611293573d6000803e3d6000fd5b505050506040513d601f19601f820116820180604052508101906112b79190611518565b905060025481856040516020016112ea92919091825260601b6bffffffffffffffffffffffff1916602082015260340190565b6040516020818303038152906040528051906020012060001c610e5c9190611585565b6000600a821161132157506203e5b4919050565b600f8211611333575062025e0a919050565b60148211611345575062016419919050565b60198211611356575061bf91919050565b601e82116113675750614ea7919050565b602182116113785750611fad919050565b506001919050565b60006020828403121561139257600080fd5b5035919050565b600080604083850312156113ac57600080fd5b50508035926020909101359150565b6020808252825182820181905260009190848201906040850190845b818110156113fc5783516001600160a01b0316835292840192918401916001016113d7565b50909695505050505050565b80356001600160a01b038116811461141f57600080fd5b919050565b6000806040838503121561143757600080fd5b61144083611408565b946020939093013593505050565b60008060006060848603121561146357600080fd5b833592506020840135915061147a60408501611408565b90509250925092565b60006020828403121561149557600080fd5b610d6c82611408565b6000602082840312156114b057600080fd5b81518015158114610d6c57600080fd5b634e487b7160e01b600052603260045260246000fd5b634e487b7160e01b600052601160045260246000fd5b8082018082111561099e5761099e6114d6565b600060018201611511576115116114d6565b5060010190565b60006020828403121561152a57600080fd5b5051919050565b808202811582820484141761099e5761099e6114d6565b8181038181111561099e5761099e6114d6565b634e487b7160e01b600052601260045260246000fd5b6000826115805761158061155b565b500490565b6000826115945761159461155b565b50069056fea2646970667358221220e69974bb738477c29c56ccefc13d53f4fbd9162ae085644b4d9261c2eab05f0964736f6c63430008130033";

    public static final String FUNC_ACTIVEVALIDATORCOUNT = "activeValidatorCount";

    public static final String FUNC_ADMIN = "admin";

    public static final String FUNC_BEACONCONTRACT = "beaconContract";

    public static final String FUNC_COMMITTEEFORMED = "committeeFormed";

    public static final String FUNC_COMMITTEEOF = "committeeOf";

    public static final String FUNC_COMMITTEESIZE = "committeeSize";

    public static final String FUNC_FORMCOMMITTEE = "formCommittee";

    public static final String FUNC_ISACTIVEAT = "isActiveAt";

    public static final String FUNC_ISCOMMITTEEMEMBER = "isCommitteeMember";

    public static final String FUNC_MINSTAKE = "minStake";

    public static final String FUNC_REGISTER = "register";

    public static final String FUNC_REQUESTEXIT = "requestExit";

    public static final String FUNC_REQUIREDSHARDSIZE = "requiredShardSize";

    public static final String FUNC_SETSHARDCOUNT = "setShardCount";

    public static final String FUNC_SHARDCOUNT = "shardCount";

    public static final String FUNC_SHARDOF = "shardOf";

    public static final String FUNC_SLASH = "slash";

    public static final String FUNC_UNBONDINGEPOCHS = "unbondingEpochs";

    public static final String FUNC_VALIDATORCOUNT = "validatorCount";

    public static final String FUNC_VALIDATORLIST = "validatorList";

    public static final String FUNC_VALIDATORS = "validators";

    public static final String FUNC_WITHDRAW = "withdraw";

    public static final Event COMMITTEEFORMED_EVENT = new Event("CommitteeFormed", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event VALIDATOREXITREQUESTED_EVENT = new Event("ValidatorExitRequested", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event VALIDATORREGISTERED_EVENT = new Event("ValidatorRegistered", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event VALIDATORSLASHED_EVENT = new Event("ValidatorSlashed", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event VALIDATORWITHDRAWN_EVENT = new Event("ValidatorWithdrawn", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected ValidatorRegistry(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected ValidatorRegistry(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected ValidatorRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected ValidatorRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<CommitteeFormedEventResponse> getCommitteeFormedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(COMMITTEEFORMED_EVENT, transactionReceipt);
        ArrayList<CommitteeFormedEventResponse> responses = new ArrayList<CommitteeFormedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            CommitteeFormedEventResponse typedResponse = new CommitteeFormedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.epoch = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.totalActive = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static CommitteeFormedEventResponse getCommitteeFormedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(COMMITTEEFORMED_EVENT, log);
        CommitteeFormedEventResponse typedResponse = new CommitteeFormedEventResponse();
        typedResponse.log = log;
        typedResponse.epoch = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.totalActive = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<CommitteeFormedEventResponse> committeeFormedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getCommitteeFormedEventFromLog(log));
    }

    public Flowable<CommitteeFormedEventResponse> committeeFormedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(COMMITTEEFORMED_EVENT));
        return committeeFormedEventFlowable(filter);
    }

    public static List<ValidatorExitRequestedEventResponse> getValidatorExitRequestedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(VALIDATOREXITREQUESTED_EVENT, transactionReceipt);
        ArrayList<ValidatorExitRequestedEventResponse> responses = new ArrayList<ValidatorExitRequestedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ValidatorExitRequestedEventResponse typedResponse = new ValidatorExitRequestedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.who = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.exitEpoch = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ValidatorExitRequestedEventResponse getValidatorExitRequestedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(VALIDATOREXITREQUESTED_EVENT, log);
        ValidatorExitRequestedEventResponse typedResponse = new ValidatorExitRequestedEventResponse();
        typedResponse.log = log;
        typedResponse.who = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.exitEpoch = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<ValidatorExitRequestedEventResponse> validatorExitRequestedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getValidatorExitRequestedEventFromLog(log));
    }

    public Flowable<ValidatorExitRequestedEventResponse> validatorExitRequestedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(VALIDATOREXITREQUESTED_EVENT));
        return validatorExitRequestedEventFlowable(filter);
    }

    public static List<ValidatorRegisteredEventResponse> getValidatorRegisteredEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(VALIDATORREGISTERED_EVENT, transactionReceipt);
        ArrayList<ValidatorRegisteredEventResponse> responses = new ArrayList<ValidatorRegisteredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ValidatorRegisteredEventResponse typedResponse = new ValidatorRegisteredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.who = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.stake = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.joinEpoch = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ValidatorRegisteredEventResponse getValidatorRegisteredEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(VALIDATORREGISTERED_EVENT, log);
        ValidatorRegisteredEventResponse typedResponse = new ValidatorRegisteredEventResponse();
        typedResponse.log = log;
        typedResponse.who = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.stake = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.joinEpoch = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<ValidatorRegisteredEventResponse> validatorRegisteredEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getValidatorRegisteredEventFromLog(log));
    }

    public Flowable<ValidatorRegisteredEventResponse> validatorRegisteredEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(VALIDATORREGISTERED_EVENT));
        return validatorRegisteredEventFlowable(filter);
    }

    public static List<ValidatorSlashedEventResponse> getValidatorSlashedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(VALIDATORSLASHED_EVENT, transactionReceipt);
        ArrayList<ValidatorSlashedEventResponse> responses = new ArrayList<ValidatorSlashedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ValidatorSlashedEventResponse typedResponse = new ValidatorSlashedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.who = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.stake = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ValidatorSlashedEventResponse getValidatorSlashedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(VALIDATORSLASHED_EVENT, log);
        ValidatorSlashedEventResponse typedResponse = new ValidatorSlashedEventResponse();
        typedResponse.log = log;
        typedResponse.who = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.stake = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<ValidatorSlashedEventResponse> validatorSlashedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getValidatorSlashedEventFromLog(log));
    }

    public Flowable<ValidatorSlashedEventResponse> validatorSlashedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(VALIDATORSLASHED_EVENT));
        return validatorSlashedEventFlowable(filter);
    }

    public static List<ValidatorWithdrawnEventResponse> getValidatorWithdrawnEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(VALIDATORWITHDRAWN_EVENT, transactionReceipt);
        ArrayList<ValidatorWithdrawnEventResponse> responses = new ArrayList<ValidatorWithdrawnEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ValidatorWithdrawnEventResponse typedResponse = new ValidatorWithdrawnEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.who = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.stake = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ValidatorWithdrawnEventResponse getValidatorWithdrawnEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(VALIDATORWITHDRAWN_EVENT, log);
        ValidatorWithdrawnEventResponse typedResponse = new ValidatorWithdrawnEventResponse();
        typedResponse.log = log;
        typedResponse.who = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.stake = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<ValidatorWithdrawnEventResponse> validatorWithdrawnEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getValidatorWithdrawnEventFromLog(log));
    }

    public Flowable<ValidatorWithdrawnEventResponse> validatorWithdrawnEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(VALIDATORWITHDRAWN_EVENT));
        return validatorWithdrawnEventFlowable(filter);
    }

    public RemoteFunctionCall<BigInteger> activeValidatorCount(BigInteger epoch) {
        final Function function = new Function(FUNC_ACTIVEVALIDATORCOUNT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(epoch)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<String> admin() {
        final Function function = new Function(FUNC_ADMIN, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> beaconContract() {
        final Function function = new Function(FUNC_BEACONCONTRACT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<Boolean> committeeFormed(BigInteger param0) {
        final Function function = new Function(FUNC_COMMITTEEFORMED, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<List> committeeOf(BigInteger epoch, BigInteger shardId) {
        final Function function = new Function(FUNC_COMMITTEEOF, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(epoch), 
                new org.web3j.abi.datatypes.generated.Uint256(shardId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Address>>() {}));
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

    public RemoteFunctionCall<BigInteger> committeeSize(BigInteger epoch, BigInteger shardId) {
        final Function function = new Function(FUNC_COMMITTEESIZE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(epoch), 
                new org.web3j.abi.datatypes.generated.Uint256(shardId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> formCommittee(BigInteger epoch) {
        final Function function = new Function(
                FUNC_FORMCOMMITTEE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(epoch)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> isActiveAt(String who, BigInteger epoch) {
        final Function function = new Function(FUNC_ISACTIVEAT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, who), 
                new org.web3j.abi.datatypes.generated.Uint256(epoch)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<Boolean> isCommitteeMember(BigInteger epoch, BigInteger shardId, String who) {
        final Function function = new Function(FUNC_ISCOMMITTEEMEMBER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(epoch), 
                new org.web3j.abi.datatypes.generated.Uint256(shardId), 
                new org.web3j.abi.datatypes.Address(160, who)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<BigInteger> minStake() {
        final Function function = new Function(FUNC_MINSTAKE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> register(BigInteger weiValue) {
        final Function function = new Function(
                FUNC_REGISTER, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function, weiValue);
    }

    public RemoteFunctionCall<TransactionReceipt> requestExit() {
        final Function function = new Function(
                FUNC_REQUESTEXIT, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> requiredShardSize(BigInteger maliciousPct, BigInteger lambda) {
        final Function function = new Function(FUNC_REQUIREDSHARDSIZE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(maliciousPct), 
                new org.web3j.abi.datatypes.generated.Uint256(lambda)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> setShardCount(BigInteger k) {
        final Function function = new Function(
                FUNC_SETSHARDCOUNT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(k)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> shardCount() {
        final Function function = new Function(FUNC_SHARDCOUNT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> shardOf(String who, BigInteger epoch) {
        final Function function = new Function(FUNC_SHARDOF, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, who), 
                new org.web3j.abi.datatypes.generated.Uint256(epoch)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> slash(String who) {
        final Function function = new Function(
                FUNC_SLASH, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, who)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> unbondingEpochs() {
        final Function function = new Function(FUNC_UNBONDINGEPOCHS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> validatorCount() {
        final Function function = new Function(FUNC_VALIDATORCOUNT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<String> validatorList(BigInteger param0) {
        final Function function = new Function(FUNC_VALIDATORLIST, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<Tuple5<BigInteger, BigInteger, BigInteger, Boolean, Boolean>> validators(String param0) {
        final Function function = new Function(FUNC_VALIDATORS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Bool>() {}, new TypeReference<Bool>() {}));
        return new RemoteFunctionCall<Tuple5<BigInteger, BigInteger, BigInteger, Boolean, Boolean>>(function,
                new Callable<Tuple5<BigInteger, BigInteger, BigInteger, Boolean, Boolean>>() {
                    @Override
                    public Tuple5<BigInteger, BigInteger, BigInteger, Boolean, Boolean> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple5<BigInteger, BigInteger, BigInteger, Boolean, Boolean>(
                                (BigInteger) results.get(0).getValue(), 
                                (BigInteger) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue(), 
                                (Boolean) results.get(3).getValue(), 
                                (Boolean) results.get(4).getValue());
                    }
                });
    }

    public RemoteFunctionCall<TransactionReceipt> withdraw() {
        final Function function = new Function(
                FUNC_WITHDRAW, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static ValidatorRegistry load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new ValidatorRegistry(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static ValidatorRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new ValidatorRegistry(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static ValidatorRegistry load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new ValidatorRegistry(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static ValidatorRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new ValidatorRegistry(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<ValidatorRegistry> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, String _beacon, BigInteger _shardCount, BigInteger _minStake, BigInteger _unbondingEpochs) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _beacon), 
                new org.web3j.abi.datatypes.generated.Uint256(_shardCount), 
                new org.web3j.abi.datatypes.generated.Uint256(_minStake), 
                new org.web3j.abi.datatypes.generated.Uint256(_unbondingEpochs)));
        return deployRemoteCall(ValidatorRegistry.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<ValidatorRegistry> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, String _beacon, BigInteger _shardCount, BigInteger _minStake, BigInteger _unbondingEpochs) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _beacon), 
                new org.web3j.abi.datatypes.generated.Uint256(_shardCount), 
                new org.web3j.abi.datatypes.generated.Uint256(_minStake), 
                new org.web3j.abi.datatypes.generated.Uint256(_unbondingEpochs)));
        return deployRemoteCall(ValidatorRegistry.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<ValidatorRegistry> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String _beacon, BigInteger _shardCount, BigInteger _minStake, BigInteger _unbondingEpochs) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _beacon), 
                new org.web3j.abi.datatypes.generated.Uint256(_shardCount), 
                new org.web3j.abi.datatypes.generated.Uint256(_minStake), 
                new org.web3j.abi.datatypes.generated.Uint256(_unbondingEpochs)));
        return deployRemoteCall(ValidatorRegistry.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<ValidatorRegistry> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String _beacon, BigInteger _shardCount, BigInteger _minStake, BigInteger _unbondingEpochs) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _beacon), 
                new org.web3j.abi.datatypes.generated.Uint256(_shardCount), 
                new org.web3j.abi.datatypes.generated.Uint256(_minStake), 
                new org.web3j.abi.datatypes.generated.Uint256(_unbondingEpochs)));
        return deployRemoteCall(ValidatorRegistry.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static class CommitteeFormedEventResponse extends BaseEventResponse {
        public BigInteger epoch;

        public BigInteger totalActive;
    }

    public static class ValidatorExitRequestedEventResponse extends BaseEventResponse {
        public String who;

        public BigInteger exitEpoch;
    }

    public static class ValidatorRegisteredEventResponse extends BaseEventResponse {
        public String who;

        public BigInteger stake;

        public BigInteger joinEpoch;
    }

    public static class ValidatorSlashedEventResponse extends BaseEventResponse {
        public String who;

        public BigInteger stake;
    }

    public static class ValidatorWithdrawnEventResponse extends BaseEventResponse {
        public String who;

        public BigInteger stake;
    }
}
