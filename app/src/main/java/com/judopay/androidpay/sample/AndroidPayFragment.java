package com.judopay.androidpay.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.FullWalletRequest;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.MaskedWalletRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.PaymentsClient;
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
import com.judopay.model.Currency;
import com.judopay.model.Receipt;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import static com.judopay.Judo.SANDBOX;

public class AndroidPayFragment extends Fragment implements GoogleApiClient.OnConnectionFailedListener {
    private static final int MASKED_WALLET_REQUEST_CODE = 501;
    private static final int FULL_WALLET_REQUEST_CODE = 601;

    private static final String AMOUNT = "0.01";
    private static final String CURRENCY = "USD";

    private static final String JUDO_ID = "100411420";
    private static final String API_TOKEN = "4YsSsUsAoc94gie2";
    private static final String API_SECRET = "b3479ca967e23884932f7747c8c85d9865d8450ea45d869774d9f358fa1a629b";

    private static final int ENVIRONMENT = WalletConstants.ENVIRONMENT_TEST;

    private GoogleApiClient googleApiClient;
    private SupportWalletFragment walletFragment;
    private PaymentsClient paymentsClient;
    private Subscription subscription;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getActivity() == null) {
            return;
        }

        googleApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(Wallet.API, new Wallet.WalletOptions.Builder().setEnvironment(ENVIRONMENT).build())
                .enableAutoManage(getActivity(), this)
                .build();

        paymentsClient = Wallet.getPaymentsClient(getActivity(), new Wallet.WalletOptions.Builder()
                .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                .build());

        createWalletFragment();
        checkAndroidPayAvailable();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_android_pay, container, false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case MASKED_WALLET_REQUEST_CODE:
                performFullWalletRequest(data.<MaskedWallet>getParcelableExtra(WalletConstants.EXTRA_MASKED_WALLET));
                break;

            case FULL_WALLET_REQUEST_CODE:
                performJudoPayment(data.<FullWallet>getParcelableExtra(WalletConstants.EXTRA_FULL_WALLET));
                break;
        }
    }

    private void performFullWalletRequest(MaskedWallet maskedWallet) {
        FullWalletRequest fullWalletRequest = FullWalletRequest.newBuilder()
                .setGoogleTransactionId(maskedWallet.getGoogleTransactionId())
                .setCart(Cart.newBuilder()
                        .setCurrencyCode(CURRENCY)
                        .setTotalPrice(AMOUNT)
                        .build())
                .build();

        Wallet.Payments.loadFullWallet(googleApiClient, fullWalletRequest, FULL_WALLET_REQUEST_CODE);
    }

    private void performJudoPayment(FullWallet wallet) {
        if (getActivity() == null) {
            return;
        }

        AndroidPayRequest androidPayRequest = new AndroidPayRequest.Builder()
                .setJudoId(JUDO_ID)
                .setCurrency(CURRENCY)
                .setAmount(AMOUNT)
                .setConsumerReference("androidPaySampleApp")
                .setWallet(new com.judopay.model.Wallet.Builder()
                        .setPublicKey(getString(R.string.public_key))
                        .setEnvironment(ENVIRONMENT)
                        .setPaymentMethodToken(wallet.getPaymentMethodToken().getToken())
                        .setGoogleTransactionId(wallet.getGoogleTransactionId())
                        .setInstrumentDetails(wallet.getInstrumentInfos()[0].getInstrumentDetails())
                        .setInstrumentType(wallet.getInstrumentInfos()[0].getInstrumentType())
                        .build())
                .build();

        Judo judo = new Judo.Builder()
                .setJudoId(JUDO_ID)
                .setApiSecret(API_SECRET)
                .setApiToken(API_TOKEN)
                .setEnvironment(SANDBOX)
                .setCurrency(Currency.USD)
                .setConsumerReference("androidPaySampleApp")
                .setAmount(AMOUNT)
                .build();

        JudoApiService apiService = judo.getApiService(getActivity());

        subscription = apiService.androidPayPayment(androidPayRequest)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Receipt>() {
                    @Override
                    public void call(Receipt receipt) {
                        showPaymentStatusDialog(receipt);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Toast.makeText(getActivity(), throwable.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        Log.e("AndroidPayFragment", throwable.getLocalizedMessage(), throwable);
                    }
                });
    }

    private void showPaymentStatusDialog(Receipt receipt) {
        if (getContext() == null) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        if (receipt.isSuccess()) {
            builder.setTitle(getString(R.string.payment_successful));
        } else {
            builder.setTitle(getString(R.string.payment_declined))
                    .setMessage(getString(R.string.please_check_card_details));
        }
        builder.show();
    }

    private void checkAndroidPayAvailable() {
        if (getContext() == null) {
            return;
        }

        IsReadyToPayRequest request = IsReadyToPayRequest.newBuilder()
                .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
                .build();
        Task<Boolean> task = paymentsClient.isReadyToPay(request);

        task.addOnCompleteListener(new OnCompleteListener<Boolean>() {
            public void onComplete(@NonNull Task<Boolean> task) {
                try {
                    boolean result = task.getResult(ApiException.class);
                    walletFragment.setEnabled(result);
                } catch (ApiException ignored) {
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
                .setEnvironment(ENVIRONMENT)
                .setTheme(WalletConstants.THEME_DARK)
                .setFragmentStyle(walletStyle)
                .setMode(WalletFragmentMode.BUY_BUTTON)
                .build();

        PaymentMethodTokenizationParameters parameters = PaymentMethodTokenizationParameters.newBuilder()
                .setPaymentMethodTokenizationType(WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_NETWORK_TOKEN)
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
                .setMaskedWalletRequestCode(MASKED_WALLET_REQUEST_CODE)
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
        subscription.unsubscribe();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(getActivity(), "Connection failed! " + connectionResult.getErrorMessage() + ", " + connectionResult.getErrorCode(), Toast.LENGTH_SHORT).show();
    }
}