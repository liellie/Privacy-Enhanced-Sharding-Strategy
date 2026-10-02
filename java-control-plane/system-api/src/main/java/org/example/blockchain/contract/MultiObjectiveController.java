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
import org.web3j.tuples.generated.Tuple3;
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
public class MultiObjectiveController extends Contract {
    public static final String BINARY = "608060405234801561001057600080fd5b50604051610f50380380610f5083398101604081905261002f916100f6565b6000811180156100475750670de0b6b3a76400008111155b6100975760405162461bcd60e51b815260206004820152601360248201527f436f6e74726f6c6c65723a206261642065746100000000000000000000000000604482015260640160405180910390fd5b600080546001600160a01b03199081163317909155600180546001600160a01b039586169083161790556002805493909416921691909117909155600355610132565b80516001600160a01b03811681146100f157600080fd5b919050565b60008060006060848603121561010b57600080fd5b610114846100da565b9250610122602085016100da565b9150604084015190509250925092565b610e0f806101416000396000f3fe608060405234801561001057600080fd5b506004361061009e5760003560e01c80637b103999116100665780637b103999146100ff578063840b4f231461012a57806384c6aea21461014d578063b36dbabf14610156578063f851a4401461016957600080fd5b806306a4c983146100a35780633a1cde75146100bf578063415deffa146100d45780636573570e146100dd5780636a146024146100f0575b600080fd5b6100ac60045481565b6040519081526020015b60405180910390f35b6100d26100cd366004610c4e565b61017c565b005b6100ac60055481565b6100d26100eb366004610c4e565b610555565b6100ac670de0b6b3a764000081565b600154610112906001600160a01b031681565b6040516001600160a01b0390911681526020016100b6565b610132610606565b604080519384526020840192909252908201526060016100b6565b6100ac60035481565b600254610112906001600160a01b031681565b600054610112906001600160a01b031681565b60045481118061018c5750600554155b6101dd5760405162461bcd60e51b815260206004820152601e60248201527f436f6e74726f6c6c65723a2065706f6368206e6f7420616476616e636564000060448201526064015b60405180910390fd5b60008060006101ea610606565b9194509250905060006003826102008587610c7d565b61020a9190610c7d565b6102149190610ca6565b905061021e610c30565b61023061022b8684610cba565b6107ce565b600160009054906101000a90046001600160a01b03166001600160a01b0316633d531f9d6040518163ffffffff1660e01b8152600401602060405180830381865afa158015610283573d6000803e3d6000fd5b505050506040513d601f19601f820116820180604052508101906102a79190610ce1565b6102b19190610cba565b81526102c061022b8584610cba565b600160009054906101000a90046001600160a01b03166001600160a01b0316630d4fd5766040518163ffffffff1660e01b8152600401602060405180830381865afa158015610313573d6000803e3d6000fd5b505050506040513d601f19601f820116820180604052508101906103379190610ce1565b6103419190610cba565b602082015261035361022b8484610cba565b600160009054906101000a90046001600160a01b03166001600160a01b031663b74d428c6040518163ffffffff1660e01b8152600401602060405180830381865afa1580156103a6573d6000803e3d6000fd5b505050506040513d601f19601f820116820180604052508101906103ca9190610ce1565b6103d49190610cba565b604082015260006103e4826107f7565b602081015181519192506000916103fb9190610c7d565b90506000811561042e576020830151829061041f90670de0b6b3a764000090610d10565b6104299190610ca6565b610441565b6104416002670de0b6b3a7640000610ca6565b6001548451602086015160408088015190516324bc9b5f60e11b8152600481019390935260248301919091526044820152606481018390529192506001600160a01b03169063497936be90608401600060405180830381600087803b1580156104a957600080fd5b505af11580156104bd573d6000803e3d6000fd5b50505050886004819055506001600560008282546104db9190610c7d565b9091555050825160208085015160408087015181518d81529384018c90529083018a90526060830193909352608082015260a081019190915260c0810182905289907f19a415ef04bbe93c18a09ce6ae5334783d7515f85bcb20ca746498e8f17c664d9060e00160405180910390a2505050505050505050565b6000546001600160a01b031633146105a75760405162461bcd60e51b815260206004820152601560248201527421b7b73a3937b63632b91d103737ba1030b236b4b760591b60448201526064016101d4565b6000811180156105bf5750670de0b6b3a76400008111155b6106015760405162461bcd60e51b8152602060048201526013602482015272436f6e74726f6c6c65723a206261642065746160681b60448201526064016101d4565b600355565b6000806000600260009054906101000a90046001600160a01b03166001600160a01b031663504f6cf36040518163ffffffff1660e01b8152600401602060405180830381865afa15801561065e573d6000803e3d6000fd5b505050506040513d601f19601f820116820180604052508101906106829190610ce1565b9250600160009054906101000a90046001600160a01b03166001600160a01b031663586991596040518163ffffffff1660e01b8152600401602060405180830381865afa1580156106d7573d6000803e3d6000fd5b505050506040513d601f19601f820116820180604052508101906106fb9190610ce1565b9150600160009054906101000a90046001600160a01b03166001600160a01b031663307a86576040518163ffffffff1660e01b8152600401602060405180830381865afa158015610750573d6000803e3d6000fd5b505050506040513d601f19601f820116820180604052508101906107749190610ce1565b9050670de0b6b3a764000083111561079257670de0b6b3a764000092505b670de0b6b3a76400008211156107ae57670de0b6b3a764000091505b670de0b6b3a76400008111156107c95750670de0b6b3a76400005b909192565b6000670de0b6b3a7640000600354836107e79190610d27565b6107f19190610d57565b92915050565b6107ff610c30565b600060405180606001604052808460006003811061081f5761081f610cfa565b602002015181526020018460016003811061083c5761083c610cfa565b602002015181526020018460026003811061085957610859610cfa565b60200201519052905080600160200201518160006020020151121561088657602081018051825190915281525b6040810151602082015112156108a85760408101805160208301805190925290525b6020810151815112156108c357602081018051825190915281525b670de0b6b3a76400006000808060015b6003811161098557856108e7600183610d85565b600381106108f7576108f7610cfa565b60200201516109069085610d98565b93506000816109158688610cba565b61091f9190610d57565b8761092b600185610d85565b6003811061093b5761093b610cfa565b602002015161094a9190610d98565b90506000811315610972578192508186866109659190610cba565b61096f9190610d57565b93505b508061097d81610dc0565b9150506108d3565b50600081116109d65760405162461bcd60e51b815260206004820152601d60248201527f436f6e74726f6c6c65723a2070726f6a656374696f6e206661696c656400000060448201526064016101d4565b6000805b6003811015610a68576000848a83600381106109f8576109f8610cfa565b6020020151610a079190610cba565b905060008113610a18576000610a1a565b805b898360038110610a2c57610a2c610cfa565b6020020152888260038110610a4357610a43610cfa565b6020020151610a529084610c7d565b9250508080610a6090610dc0565b9150506109da565b5080600003610adf57610a846003670de0b6b3a7640000610ca6565b8752610a996003670de0b6b3a7640000610ca6565b6020880152610ab16003670de0b6b3a7640000610ca6565b610abc906002610d10565b610ace90670de0b6b3a7640000610d85565b604088015250949695505050505050565b60005b6003811015610b495781670de0b6b3a7640000898360038110610b0757610b07610cfa565b6020020151610b169190610d10565b610b209190610ca6565b888260038110610b3257610b32610cfa565b602002015280610b4181610dc0565b915050610ae2565b5060408701516020880151885160009291610b6391610c7d565b610b6d9190610c7d565b9050670de0b6b3a7640000811015610ba857610b9181670de0b6b3a7640000610d85565b88518990610ba0908390610c7d565b905250610c24565b670de0b6b3a7640000811115610c24576000610bcc670de0b6b3a764000083610d85565b89519091508111610bf557808960005b60200201818151610bed9190610d85565b905250610c22565b60208901518111610c095780896001610bdc565b8089600260200201818151610c1e9190610d85565b9052505b505b50505050505050919050565b60405180606001604052806003906020820280368337509192915050565b600060208284031215610c6057600080fd5b5035919050565b634e487b7160e01b600052601160045260246000fd5b808201808211156107f1576107f1610c67565b634e487b7160e01b600052601260045260246000fd5b600082610cb557610cb5610c90565b500490565b8181036000831280158383131683831282161715610cda57610cda610c67565b5092915050565b600060208284031215610cf357600080fd5b5051919050565b634e487b7160e01b600052603260045260246000fd5b80820281158282048414176107f1576107f1610c67565b80820260008212600160ff1b84141615610d4357610d43610c67565b81810583148215176107f1576107f1610c67565b600082610d6657610d66610c90565b600160ff1b821460001984141615610d8057610d80610c67565b500590565b818103818111156107f1576107f1610c67565b8082018281126000831280158216821582161715610db857610db8610c67565b505092915050565b600060018201610dd257610dd2610c67565b506001019056fea26469706673582212202a7f1ea7e62e63a7c6d101d7ff2ad4f88d2e5de6e421fec91d9e3c8a2738dc4b64736f6c63430008130033";

    public static final String FUNC_WAD = "WAD";

    public static final String FUNC_ADMIN = "admin";

    public static final String FUNC_CROSSSHARD = "crossShard";

    public static final String FUNC_ETAWAD = "etaWad";

    public static final String FUNC_LASTEPOCH = "lastEpoch";

    public static final String FUNC_OBJECTIVES = "objectives";

    public static final String FUNC_REGISTRY = "registry";

    public static final String FUNC_SETETA = "setEta";

    public static final String FUNC_STEP = "step";

    public static final String FUNC_STEPCOUNT = "stepCount";

    public static final Event STEPPED_EVENT = new Event("Stepped", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected MultiObjectiveController(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected MultiObjectiveController(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected MultiObjectiveController(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected MultiObjectiveController(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<SteppedEventResponse> getSteppedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(STEPPED_EVENT, transactionReceipt);
        ArrayList<SteppedEventResponse> responses = new ArrayList<SteppedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            SteppedEventResponse typedResponse = new SteppedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.epoch = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.f1 = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.f2 = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.f3 = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            typedResponse.l1 = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
            typedResponse.l2 = (BigInteger) eventValues.getNonIndexedValues().get(4).getValue();
            typedResponse.l3 = (BigInteger) eventValues.getNonIndexedValues().get(5).getValue();
            typedResponse.beta = (BigInteger) eventValues.getNonIndexedValues().get(6).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static SteppedEventResponse getSteppedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(STEPPED_EVENT, log);
        SteppedEventResponse typedResponse = new SteppedEventResponse();
        typedResponse.log = log;
        typedResponse.epoch = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.f1 = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.f2 = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        typedResponse.f3 = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
        typedResponse.l1 = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
        typedResponse.l2 = (BigInteger) eventValues.getNonIndexedValues().get(4).getValue();
        typedResponse.l3 = (BigInteger) eventValues.getNonIndexedValues().get(5).getValue();
        typedResponse.beta = (BigInteger) eventValues.getNonIndexedValues().get(6).getValue();
        return typedResponse;
    }

    public Flowable<SteppedEventResponse> steppedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getSteppedEventFromLog(log));
    }

    public Flowable<SteppedEventResponse> steppedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(STEPPED_EVENT));
        return steppedEventFlowable(filter);
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

    public RemoteFunctionCall<String> crossShard() {
        final Function function = new Function(FUNC_CROSSSHARD, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<BigInteger> etaWad() {
        final Function function = new Function(FUNC_ETAWAD, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> lastEpoch() {
        final Function function = new Function(FUNC_LASTEPOCH, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<Tuple3<BigInteger, BigInteger, BigInteger>> objectives() {
        final Function function = new Function(FUNC_OBJECTIVES, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
        return new RemoteFunctionCall<Tuple3<BigInteger, BigInteger, BigInteger>>(function,
                new Callable<Tuple3<BigInteger, BigInteger, BigInteger>>() {
                    @Override
                    public Tuple3<BigInteger, BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple3<BigInteger, BigInteger, BigInteger>(
                                (BigInteger) results.get(0).getValue(), 
                                (BigInteger) results.get(1).getValue(), 
                                (BigInteger) results.get(2).getValue());
                    }
                });
    }

    public RemoteFunctionCall<String> registry() {
        final Function function = new Function(FUNC_REGISTRY, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> setEta(BigInteger _etaWad) {
        final Function function = new Function(
                FUNC_SETETA, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_etaWad)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> step(BigInteger epoch) {
        final Function function = new Function(
                FUNC_STEP, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(epoch)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> stepCount() {
        final Function function = new Function(FUNC_STEPCOUNT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    @Deprecated
    public static MultiObjectiveController load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new MultiObjectiveController(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static MultiObjectiveController load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new MultiObjectiveController(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static MultiObjectiveController load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new MultiObjectiveController(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static MultiObjectiveController load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new MultiObjectiveController(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<MultiObjectiveController> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, String _registry, String _crossShard, BigInteger _etaWad) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _registry), 
                new org.web3j.abi.datatypes.Address(160, _crossShard), 
                new org.web3j.abi.datatypes.generated.Uint256(_etaWad)));
        return deployRemoteCall(MultiObjectiveController.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<MultiObjectiveController> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, String _registry, String _crossShard, BigInteger _etaWad) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _registry), 
                new org.web3j.abi.datatypes.Address(160, _crossShard), 
                new org.web3j.abi.datatypes.generated.Uint256(_etaWad)));
        return deployRemoteCall(MultiObjectiveController.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<MultiObjectiveController> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String _registry, String _crossShard, BigInteger _etaWad) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _registry), 
                new org.web3j.abi.datatypes.Address(160, _crossShard), 
                new org.web3j.abi.datatypes.generated.Uint256(_etaWad)));
        return deployRemoteCall(MultiObjectiveController.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<MultiObjectiveController> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String _registry, String _crossShard, BigInteger _etaWad) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _registry), 
                new org.web3j.abi.datatypes.Address(160, _crossShard), 
                new org.web3j.abi.datatypes.generated.Uint256(_etaWad)));
        return deployRemoteCall(MultiObjectiveController.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static class SteppedEventResponse extends BaseEventResponse {
        public BigInteger epoch;

        public BigInteger f1;

        public BigInteger f2;

        public BigInteger f3;

        public BigInteger l1;

        public BigInteger l2;

        public BigInteger l3;

        public BigInteger beta;
    }
}
