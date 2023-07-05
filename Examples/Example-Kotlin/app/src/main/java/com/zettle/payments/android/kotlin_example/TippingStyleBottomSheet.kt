package com.zettle.payments.android.kotlin_example

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.zettle.sdk.feature.cardreader.payment.TippingStyle

class TippingStyleBottomSheet : BottomSheetDialogFragment() {

    private lateinit var styleNoneView: View
    private lateinit var styleDefaultView: View
    private lateinit var styleAmountView: View
    private lateinit var stylePercentView: View

    companion object {
        const val REQUEST_KEY = "TippingStyleBottomSheetRC"
        const val TIPPING_STYLE_KEY = "TippingStyleKey"

        fun newInstance() = TippingStyleBottomSheet()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = inflater.inflate(R.layout.fragment_tipping_style, container, false)
        styleNoneView = binding.findViewById(R.id.tipping_style_none)
        styleDefaultView = binding.findViewById(R.id.tipping_style_default)
        styleAmountView = binding.findViewById(R.id.tipping_style_amount)
        stylePercentView = binding.findViewById(R.id.tipping_style_percent)

        return binding.rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        styleNoneView.setOnClickListener { setResultAndExit(TippingStyle.None) }
        styleDefaultView.setOnClickListener { setResultAndExit(TippingStyle.Default) }
        styleAmountView.setOnClickListener { setResultAndExit(TippingStyle.Amount) }
        stylePercentView.setOnClickListener { setResultAndExit(TippingStyle.Percentage) }
    }

    private fun setResultAndExit(tippingStyle: TippingStyle) {
        setFragmentResult(REQUEST_KEY, Bundle().apply { putSerializable(TIPPING_STYLE_KEY, tippingStyle) })
        dismiss()
    }
}