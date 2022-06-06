package com.saigyouji.android.criminalintent

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.fragment.app.DialogFragment
import java.io.File

class ImageDetailDialog
    private constructor(): DialogFragment() {
    private lateinit var imagePath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imagePath = arguments?.getString("path")?:""
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val img = ImageView(this.context)

        val bitmap = getScaledBitmap(imagePath,requireActivity())
        img.setImageBitmap(bitmap)
        return img
    }


    companion object{

        fun getInstance(path: String): ImageDetailDialog{
            return ImageDetailDialog().apply {
                arguments = Bundle().apply {
                    putString("path", path)
                }
            }
        }
    }
}