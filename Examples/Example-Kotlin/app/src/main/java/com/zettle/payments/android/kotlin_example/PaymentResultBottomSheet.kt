package com.zettle.payments.android.kotlin_example

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PaymentResultBottomSheet : BottomSheetDialogFragment() {

    private lateinit var doneButton: Button
    private lateinit var titleTextView: TextView
    private lateinit var amountTextView: TextView
    private lateinit var referenceTextView: TextView
    private lateinit var paymentResultList: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = inflater.inflate(R.layout.fragment_payment_result, container, false)
        doneButton = binding.findViewById(R.id.doneBtn)
        titleTextView = binding.findViewById(R.id.titleTxt)
        amountTextView = binding.findViewById(R.id.amountTxt)
        referenceTextView = binding.findViewById(R.id.referenceTxt)
        paymentResultList = binding.findViewById(R.id.paymentResultList)

        return binding.rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        doneButton.setOnClickListener {
            dismiss()
        }

        initView(arguments?.getParcelable(PAYMENT_RESULT_KEY))
    }

    private fun initView(paymentResultData: PaymentResultData?) {
        paymentResultData?.let {
            titleTextView.text = paymentResultData.title
            amountTextView.text = paymentResultData.amount

            if (paymentResultData.resultType == ResultType.REFUND) {
                amountTextView.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        android.R.color.holo_red_light
                    )
                )
            }
            referenceTextView.text = paymentResultData.reference
            paymentResultList.apply {
                addItemDecoration(
                    DividerItemDecoration(
                        requireContext(),
                        DividerItemDecoration.VERTICAL
                    ).apply {
                        setDrawable(
                            ColorDrawable(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.divider
                                )
                            )
                        )
                    })
                adapter = PaymentListAdapter(it.resultList)
            }
        }
    }

    companion object {
        const val TAG = "PaymentResultBottomSheet"
        private const val PAYMENT_RESULT_KEY = "paymentResultDataKey"

        fun newInstance(paymentResultData: PaymentResultData): PaymentResultBottomSheet {
            val fragment = PaymentResultBottomSheet()
            fragment.arguments = Bundle().apply {
                putParcelable(PAYMENT_RESULT_KEY, paymentResultData)
            }
            return fragment
        }
    }
}

private class PaymentListAdapter(private val resultItems: List<ResultItem>) :
    RecyclerView.Adapter<PaymentListAdapter.ResultViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment_result_view, parent, false)

        return ResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        val item = resultItems[position]
        holder.resultTitle.text = item.name
        holder.resultValue.text = item.value ?: ""
    }

    override fun getItemCount(): Int = resultItems.size

    class ResultViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val resultTitle: TextView = view.findViewById(R.id.resultTitleTxt)
        val resultValue: TextView = view.findViewById(R.id.resultValueTxt)
    }
}
