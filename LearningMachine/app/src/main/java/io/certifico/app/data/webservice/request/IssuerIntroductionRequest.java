package io.certifico.app.data.webservice.request;

import com.google.gson.annotations.SerializedName;
import io.certifico.app.data.webservice.response.IssuerResponse;
import timber.log.Timber;

public class IssuerIntroductionRequest {
    @SerializedName("bitcoinAddress")
    private String mBitcoinAddress;
    @SerializedName("nonce")
    private String mNonce;

    private transient IssuerResponse mIssuerResponse;

    public IssuerIntroductionRequest(String bitcoinAddress, String nonce, IssuerResponse issuerResponse) {
        mBitcoinAddress = bitcoinAddress;
        mNonce = nonce;
        mIssuerResponse = issuerResponse;
    }

    public void setBitcoinAddress(String bitcoinAddress) {
        mBitcoinAddress = bitcoinAddress;
    }

    public String getBitcoinAddress() {
        return mBitcoinAddress;
    }

    public void setNonce(String nonce) {
        mNonce = nonce;
    }

    public String getNonce() {
        return mNonce;
    }

    public IssuerResponse getIssuerResponse() {
        return mIssuerResponse;
    }
}
