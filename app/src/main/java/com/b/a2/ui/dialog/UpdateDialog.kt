package com.b.a2.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.b.a2.R

class UpdateDialog(
    private val onNegative: () -> Unit,
    private val onPositive: () -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.apply {
            // 关键参数：去除默认 padding
            setBackgroundDrawableResource(android.R.color.transparent)
            // 设置无边界（可选）
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            // 设置宽度为 90%，高度自适应，左右边距各 5%
            setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_update, container, false)

        view.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dismiss()
            onNegative()
        }
        view.findViewById<Button>(R.id.btn_confirm).setOnClickListener {
            dismiss()
            onPositive()
        }
        return view
    }

}