package com.saigyouji.android.criminalintent

import android.app.Dialog
import android.app.TimePickerDialog
import android.icu.util.Calendar
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import java.util.*
import kotlin.time.Duration.Companion.hours

class TimePickerFragment
    private constructor(): DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {


        val date = arguments?.getSerializable("time") as Date
        val calendar = Calendar.getInstance()
        calendar.time = date
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

//        public TimePickerDialog(Context context, OnTimeSetListener listener, int hourOfDay, int minute,
//        boolean is24HourView)
        val listener = TimePickerDialog.OnTimeSetListener{
            _, hour, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            parentFragmentManager.setFragmentResult("1",
                Bundle().apply {putSerializable("time", calendar.time)})
        }
        return TimePickerDialog(requireContext(),
            listener,
            hour,
            minute,
            true
        )
    }
    companion object{
        fun newInstance(date: Date) = TimePickerFragment().apply {
            val args = Bundle().apply { putSerializable("time", date) }
            arguments = args
        }
    }
}