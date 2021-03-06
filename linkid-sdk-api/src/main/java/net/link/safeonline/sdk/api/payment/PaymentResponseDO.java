package net.link.safeonline.sdk.api.payment;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;


public class PaymentResponseDO implements Serializable {

    public static final String TXN_ID_KEY = "PaymentResponse.txnId";
    public static final String STATE_KEY  = "PaymentResponse.state";

    private final String       transactionId;
    private final PaymentState paymentState;

    /**
     * @param transactionId the payment order reference
     * @param paymentState  the payment order state
     */
    public PaymentResponseDO(final String transactionId, final PaymentState paymentState) {

        this.transactionId = transactionId;
        this.paymentState = paymentState;
    }

    // Helper methods

    public Map<String, String> toMap() {

        Map<String, String> map = new HashMap<String, String>();

        map.put( TXN_ID_KEY, transactionId );
        map.put( STATE_KEY, paymentState.name() );

        return map;
    }

    @Nullable
    public static PaymentResponseDO fromMap(final Map<String, String> paymentResponseMap) {

        // check map valid
        if (!paymentResponseMap.containsKey( TXN_ID_KEY ))
            throw new RuntimeException( "Payment response's transaction ID field is not present!" );
        if (!paymentResponseMap.containsKey( STATE_KEY ))
            throw new RuntimeException( "Payment response's state field is not present!" );

        // convert
        return new PaymentResponseDO( paymentResponseMap.get( TXN_ID_KEY ), PaymentState.parse( paymentResponseMap.get( STATE_KEY ) ) );
    }

    // Accessors

    public String getTransactionId() {

        return transactionId;
    }

    public PaymentState getPaymentState() {

        return paymentState;
    }
}
