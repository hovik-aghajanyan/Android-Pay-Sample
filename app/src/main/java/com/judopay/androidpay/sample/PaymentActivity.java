package com.judopay.androidpay.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.judopay.Judo;
import com.judopay.model.Currency;

import static com.judopay.Judo.SANDBOX;

public class PaymentActivity extends AppCompatActivity {
    private static final int PAYMENT_REQUEST_CODE = 101;

    private static final String JUDO_ID = "100411420";
    private static final String API_TOKEN = "4YsSsUsAoc94gie2";
    private static final String API_SECRET = "b3479ca967e23884932f7747c8c85d9865d8450ea45d869774d9f358fa1a629b";

    private CurrencyEditText currencyEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_payment);

        currencyEditText = findViewById(R.id.currency_edit_text);

        findViewById(R.id.judo_pay_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PaymentActivity.this, com.judopay.PaymentActivity.class);
                intent.putExtra(Judo.JUDO_OPTIONS,
                        new Judo.Builder()
                                .setJudoId(JUDO_ID)
                                .setApiSecret(API_SECRET)
                                .setApiToken(API_TOKEN)
                                .setCurrency(Currency.USD)
                                .setEnvironment(SANDBOX)
                                .setConsumerReference("androidPaySampleApp")
                                .setAmount(currencyEditText.getAmount())
                                .build());
                startActivityForResult(intent, PAYMENT_REQUEST_CODE);
            }
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.android_pay_button, new AndroidPayFragment())
                    .commit();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }
}
