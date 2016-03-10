package com.judopay.androidpay.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.BooleanResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.FullWalletRequest;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.MaskedWalletRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.PaymentMethodTokenizationType;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.google.android.gms.wallet.fragment.SupportWalletFragment;
import com.google.android.gms.wallet.fragment.WalletFragmentInitParams;
import com.google.android.gms.wallet.fragment.WalletFragmentMode;
import com.google.android.gms.wallet.fragment.WalletFragmentOptions;
import com.google.android.gms.wallet.fragment.WalletFragmentStyle;
import com.judopay.Judo;
import com.judopay.JudoApiService;
import com.judopay.model.AndroidPayRequest;
import com.judopay.model.Receipt;

import java.math.BigDecimal;

import rx.functions.Action1;

public class AndroidPayFragment extends Fragment implements GoogleApiClient.OnConnectionFailedListener {

    private static final int MASKED_WALLET_REQUEST = 501;
    private static final int FULL_WALLET_REQUEST = 601;

    private static final String AMOUNT = "0.05";
    private static final String CURRENCY = "USD";

    private GoogleApiClient googleApiClient;
    private SupportWalletFragment walletFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        googleApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(Wallet.API, new Wallet.WalletOptions.Builder()
                        .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                        .build())
                .enableAutoManage(getActivity(), this)
                .build();

        createWalletFragment();
        checkAndroidPayAvailable();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_android_pay, container, false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case MASKED_WALLET_REQUEST:
                performFullWalletRequest(data.<MaskedWallet>getParcelableExtra(WalletConstants.EXTRA_MASKED_WALLET));
                break;

            case FULL_WALLET_REQUEST:
                performJudoPayment(data.<FullWallet>getParcelableExtra(WalletConstants.EXTRA_FULL_WALLET));
                break;
        }
    }

    private void performJudoPayment(FullWallet fullWallet) {
        AndroidPayRequest androidPayRequest = new AndroidPayRequest.Builder()
                .setCurrency(CURRENCY)
                .setAmount(new BigDecimal(AMOUNT))
                .setPaymentMethodToken(fullWallet.getPaymentMethodToken().getToken())
                .build();

        JudoApiService apiService = Judo.getApiService(getActivity());

        apiService.androidPayPayment(androidPayRequest)
                .subscribe(new Action1<Receipt>() {
                    @Override
                    public void call(Receipt receipt) {
                        showPaymentStatusDialog(receipt);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        // show error message
                    }
                });
    }

    private void showPaymentStatusDialog(Receipt receipt) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        if (receipt.isSuccess()) {
            builder.setTitle(getString(R.string.payment_successful));
        } else {
            builder.setTitle(getString(R.string.payment_declined))
                    .setMessage(getString(R.string.please_check_card_details));
        }
        builder.show();
    }

    private void performFullWalletRequest(MaskedWallet maskedWallet) {
        FullWalletRequest fullWalletRequest = FullWalletRequest.newBuilder()
                .setGoogleTransactionId(maskedWallet.getGoogleTransactionId())
                .setCart(Cart.newBuilder()
                        .setCurrencyCode(CURRENCY)
                        .setTotalPrice(AMOUNT)
                        .build())
                .build();

        Wallet.Payments.loadFullWallet(googleApiClient, fullWalletRequest, FULL_WALLET_REQUEST);
    }

    private void checkAndroidPayAvailable() {
        Wallet.Payments.isReadyToPay(googleApiClient).setResultCallback(new ResultCallback<BooleanResult>() {
            @Override
            public void onResult(@NonNull BooleanResult result) {
                if (walletFragment != null) {
                    boolean enabled = result.getStatus().isSuccess() && result.getValue();
                    walletFragment.setEnabled(enabled);
                }
            }
        });
    }

    private void createWalletFragment() {
        WalletFragmentStyle walletStyle = new WalletFragmentStyle()
                .setBuyButtonText(WalletFragmentStyle.BuyButtonText.LOGO_ONLY)
                .setBuyButtonAppearance(WalletFragmentStyle.BuyButtonAppearance.ANDROID_PAY_LIGHT_WITH_BORDER)
                .setBuyButtonWidth(WalletFragmentStyle.Dimension.MATCH_PARENT);

        WalletFragmentOptions options = WalletFragmentOptions.newBuilder()
                .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                .setTheme(WalletConstants.THEME_DARK)
                .setFragmentStyle(walletStyle)
                .setMode(WalletFragmentMode.BUY_BUTTON)
                .build();

        PaymentMethodTokenizationParameters parameters = PaymentMethodTokenizationParameters.newBuilder()
                .setPaymentMethodTokenizationType(PaymentMethodTokenizationType.NETWORK_TOKEN)
                .addParameter("publicKey", getString(R.string.public_key))
                .build();

        walletFragment = SupportWalletFragment.newInstance(options);

        MaskedWalletRequest walletRequest = MaskedWalletRequest.newBuilder()
                .setMerchantName(getString(R.string.app_name))
                .setCurrencyCode(CURRENCY)
                .setEstimatedTotalPrice(AMOUNT)
                .setPaymentMethodTokenizationParameters(parameters)
                .setCart(Cart.newBuilder()
                        .setCurrencyCode(CURRENCY)
                        .setTotalPrice(AMOUNT)
                        .build())
                .build();

        WalletFragmentInitParams startParams = WalletFragmentInitParams.newBuilder()
                .setMaskedWalletRequest(walletRequest)
                .setMaskedWalletRequestCode(MASKED_WALLET_REQUEST)
                .build();

        walletFragment.initialize(startParams);

        getChildFragmentManager()
                .beginTransaction()
                .add(R.id.android_pay_layout, walletFragment)
                .commit();
    }

    public void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    public void onStop() {
        super.onStop();
        googleApiClient.disconnect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

}