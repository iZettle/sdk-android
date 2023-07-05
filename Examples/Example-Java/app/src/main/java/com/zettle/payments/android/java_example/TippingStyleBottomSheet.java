package com.zettle.payments.android.java_example;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.zettle.sdk.feature.cardreader.payment.TippingStyle;

public class TippingStyleBottomSheet extends BottomSheetDialogFragment {

    private View styleNoneView;
    private View styleDefaultView;
    private View styleAmountView;
    private View stylePercentView;

    public static final String REQUEST_KEY = "TippingStyleBottomSheetRC";
    public static final String TIPPING_STYLE_KEY = "TippingStyleKey";

    public static TippingStyleBottomSheet newInstance() {
        return new TippingStyleBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tipping_style, container, false);
        styleNoneView = view.findViewById(R.id.tipping_style_none);
        styleDefaultView = view.findViewById(R.id.tipping_style_default);
        styleAmountView = view.findViewById(R.id.tipping_style_amount);
        stylePercentView = view.findViewById(R.id.tipping_style_percent);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        styleNoneView.setOnClickListener(v -> setResultAndExit(TippingStyle.None));
        styleDefaultView.setOnClickListener(v -> setResultAndExit(TippingStyle.Default));
        styleAmountView.setOnClickListener(v -> setResultAndExit(TippingStyle.Amount));
        stylePercentView.setOnClickListener(v -> setResultAndExit(TippingStyle.Percentage));
    }

    private void setResultAndExit(TippingStyle tippingStyle) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(TIPPING_STYLE_KEY, tippingStyle);

        getParentFragmentManager().setFragmentResult(REQUEST_KEY, bundle);
        dismiss();
    }
}
