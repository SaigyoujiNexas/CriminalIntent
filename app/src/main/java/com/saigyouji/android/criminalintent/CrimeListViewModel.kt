package com.saigyouji.android.criminalintent

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import java.io.File

class CrimeListViewModel: ViewModel() {
    private val crimeRepository = CrimeRepository.get()
    val crimeListLiveData: LiveData<List<Crime>> = crimeRepository.getCrimes()

    fun addCrime(crime: Crime){
        crimeRepository.addCrime(crime)
    }

}