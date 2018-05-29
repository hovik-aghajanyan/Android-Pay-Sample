package com.judopay.androidpay.sample;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;

import java.text.DecimalFormat;

public class CurrencyEditText extends android.support.v7.widget.AppCompatEditText {
    private String currencySymbol = "$";
    private DecimalFormat decimalFormat = new DecimalFormat("0.00");
    private InputFilter[] filters;

    public CurrencyEditText(Context context) {
        super(context);
        initialize();
    }

    public CurrencyEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public CurrencyEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        setText(getContext().getString(R.string.zero_money, currencySymbol));

        int numericInput = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED;
        setInputType(numericInput);
        filters = new InputFilter[]{new InputFilter.LengthFilter(6), new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (end > start) {
                    char[] acceptedChars = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '$', '.'};

                    for (int index = start; index < end; index++) {
                        if (!new String(acceptedChars).contains(String.valueOf(source.charAt(index)))) {
                            return "";
                        }
                    }
                }
                return null;
            }
        }};

        setFilters(filters);

        setPrivateImeOptions("nm");

        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable string) {
                String digits = string.toString().replace(currencySymbol, "").replace(".", "").replaceAll("^0*", "");

                setFilters(new InputFilter[0]);
                removeTextChangedListener(this);
                string.delete(1, string.length());

                if (digits.length() > 0) {
                    String formattedString = decimalFormat.format(Float.valueOf(digits) / 100);
                    string.insert(1, formattedString);
                } else {
                    string.insert(1, decimalFormat.format(0.00));
                }

                addTextChangedListener(this);
                setFilters(filters);
            }
        });
    }

    @Override
    public void onSelectionChanged(int start, int end) {
        CharSequence text = getText();

        if (text != null) {
            if (start != text.length() || end != text.length()) {
                setSelection(text.length(), text.length());
                return;
            }
        }

        super.onSelectionChanged(start, end);
    }

    public String getAmount() {
        Editable text = getText();
        if (!TextUtils.isEmpty(text)) {
            return text.subSequence(1, text.length()).toString();
        }
        return "";
    }
}
