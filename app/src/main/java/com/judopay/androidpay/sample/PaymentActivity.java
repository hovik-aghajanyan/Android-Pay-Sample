package com.judopay.androidpay.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.judopay.Judo;
import com.judopay.JudoOptions;
import com.judopay.model.Currency;
import com.judopay.view.SingleClickOnClickListener;

public class PaymentActivity extends AppCompatActivity {

    private static final int PAYMENT_REQUEST = 101;
    private static final String JUDO_ID = "100407196";

    private CurrencyEditText currencyEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        Judo.setup("823Eja2fEM6E9NAE", "382df6f458294f49f02f073e8f356f8983e2460631ea1b4c8ed4c3ee502dcbe6", Judo.Environment.SANDBOX);

        currencyEditText = (CurrencyEditText) findViewById(R.id.currency_edit_text);

        findViewById(R.id.judo_pay_buton).setOnClickListener(new SingleClickOnClickListener() {
            @Override
            public void doClick() {
                Intent intent = new Intent(PaymentActivity.this, com.judopay.PaymentActivity.class);
                intent.putExtra(Judo.JUDO_OPTIONS, new JudoOptions.Builder()
                        .setCurrency(Currency.GBP)
                        .setJudoId(JUDO_ID)
                        .setConsumerRef("androidPaySampleApp")
                        .setAmount(currencyEditText.getAmount())
                        .build());
                startActivityForResult(intent, PAYMENT_REQUEST);
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
    }
}
