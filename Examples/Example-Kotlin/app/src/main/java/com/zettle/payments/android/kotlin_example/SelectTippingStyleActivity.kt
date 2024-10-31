package com.zettle.payments.android.kotlin_example

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.zettle.sdk.ZettleSDK
import com.zettle.sdk.feature.cardreader.payment.PayPalReaderTippingStyle
import com.zettle.sdk.feature.cardreader.payment.TippingConfiguration
import com.zettle.sdk.feature.cardreader.payment.TippingPercentageOptions
import com.zettle.sdk.feature.cardreader.payment.ZettleReaderTippingStyle

class SelectTippingStyleActivity  : AppCompatActivity() {

    // Zettle Reader (ztr)
    private lateinit var ztrTippingStyleRadioGroup: RadioGroup

    // PayPal Reader (ppr)
    private lateinit var pprTippingStyleRadioGroup: RadioGroup
    private lateinit var pprPercentOptionsRadioGroup: RadioGroup
    private lateinit var pprSectionPercentOptions: LinearLayout

    private lateinit var pprPercentOption1: EditText
    private lateinit var pprPercentOption2: EditText
    private lateinit var pprPercentOption3: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        if (!ZettleSDK.isInitialized) {
            finish()
        }

        setContentView(R.layout.activity_tipping_settings_all)

        ztrTippingStyleRadioGroup = findViewById(R.id.ztr_tipping_style_group)
        pprTippingStyleRadioGroup = findViewById(R.id.ppr_tipping_style_group)
        pprSectionPercentOptions =  findViewById(R.id.section_ppr_percent_options)
        pprPercentOptionsRadioGroup = findViewById(R.id.ppr_percent_options_group)
        pprPercentOption1 = findViewById(R.id.ppr_percent_option_1)
        pprPercentOption2 = findViewById(R.id.ppr_percent_option_2)
        pprPercentOption3 = findViewById(R.id.ppr_percent_option_3)

        pprTippingStyleRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.ppr_tipping_style_percentage) {
                pprSectionPercentOptions.visibility = View.VISIBLE
            } else {
                pprSectionPercentOptions.visibility = View.GONE
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                saveAndClose()
            }
        })

        findViewById<Button>(R.id.save_btn).setOnClickListener {
            saveAndClose()
        }

        intent.parcelableExtra<TippingConfiguration>(
            TIPPING_CONFIGS_KEY
        )?.let {
            setZettleReaderTippingStyle(it.zettleReaderTippingStyle)
            setPayPalReaderTippingStyle(it.payPalReaderTippingStyle)
        }
    }

    private fun saveAndClose() {
        val data = Intent().apply {
            putExtra(
                TIPPING_CONFIGS_KEY, TippingConfiguration(
                zettleReaderTippingStyle = getZettleReaderTippingStyle(),
                payPalReaderTippingStyle = getPayPalReaderTippingStyle()
            ))
        }
        setResult(RESULT_OK, data)
        finish()
    }

    private fun setZettleReaderTippingStyle(style: ZettleReaderTippingStyle) {
        ztrTippingStyleRadioGroup.check(
            when (style) {
                ZettleReaderTippingStyle.Default -> R.id.ztr_tipping_style_default
                ZettleReaderTippingStyle.Amount -> R.id.ztr_tipping_style_amount
                ZettleReaderTippingStyle.Percentage -> R.id.ztr_tipping_style_percentage
                ZettleReaderTippingStyle.None -> R.id.ztr_tipping_style_none
            }
        )
    }

    private fun setPayPalReaderTippingStyle(style: PayPalReaderTippingStyle) {
        pprTippingStyleRadioGroup.check(
            when (style) {
                PayPalReaderTippingStyle.None -> R.id.ppr_tipping_style_none
                PayPalReaderTippingStyle.Default -> R.id.ppr_tipping_style_default
                PayPalReaderTippingStyle.CustomAmount -> R.id.ppr_tipping_style_amount
                is PayPalReaderTippingStyle.PredefinedPercentage -> R.id.ppr_tipping_style_percentage.also {
                    setPayPalReaderPercentOptions(style.options)
                }
                PayPalReaderTippingStyle.SDKConfigured -> R.id.ppr_tipping_style_sdk_configured
            }
        )
    }

    private fun setPayPalReaderPercentOptions(options: TippingPercentageOptions?) {
        pprPercentOptionsRadioGroup.check(
            when (options) {
                null -> R.id.ppr_percent_options_null
                else -> R.id.ppr_percent_options_custom.also {
                    showPayPalReaderPercentOptions(options)
                }
            }
        )
    }

    private fun showPayPalReaderPercentOptions(options: TippingPercentageOptions) {
        pprPercentOption1.setText(options.option1.toString())
        pprPercentOption2.setText(options.option2.toString())
        pprPercentOption3.setText(options.option3.toString())
    }

    private fun getZettleReaderTippingStyle(): ZettleReaderTippingStyle =
        when (ztrTippingStyleRadioGroup.checkedRadioButtonId) {
            R.id.ztr_tipping_style_none -> ZettleReaderTippingStyle.None
            R.id.ztr_tipping_style_default -> ZettleReaderTippingStyle.Default
            R.id.ztr_tipping_style_amount -> ZettleReaderTippingStyle.Amount
            R.id.ztr_tipping_style_percentage -> ZettleReaderTippingStyle.Percentage
            else -> ZettleReaderTippingStyle.None
        }

    private fun getPayPalReaderTippingStyle(): PayPalReaderTippingStyle =
        when (pprTippingStyleRadioGroup.checkedRadioButtonId) {
            R.id.ppr_tipping_style_none -> PayPalReaderTippingStyle.None
            R.id.ppr_tipping_style_default -> PayPalReaderTippingStyle.Default
            R.id.ppr_tipping_style_amount -> PayPalReaderTippingStyle.CustomAmount
            R.id.ppr_tipping_style_percentage -> PayPalReaderTippingStyle.PredefinedPercentage(getPayPalReaderPercentOptions())
            R.id.ppr_tipping_style_sdk_configured -> PayPalReaderTippingStyle.SDKConfigured
            else -> PayPalReaderTippingStyle.None
        }

    private fun getPayPalReaderPercentOptions(): TippingPercentageOptions? =
        when(pprPercentOptionsRadioGroup.checkedRadioButtonId) {
            R.id.ppr_percent_options_custom -> TippingPercentageOptions(
                option1 = pprPercentOption1.text.toString().toIntOrNull() ?: 1,
                option2 = pprPercentOption2.text.toString().toIntOrNull() ?: 1,
                option3 = pprPercentOption3.text.toString().toIntOrNull() ?: 1,
            )
            R.id.ppr_percent_options_null -> null
            else -> null
        }

    companion object {
        const val TIPPING_CONFIGS_KEY = "TIPPING_CONFIGS"
    }
}