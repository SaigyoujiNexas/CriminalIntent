package com.saigyouji.android.criminalintent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import java.io.File
import java.util.*

private const val TAG = "CrimeFragment"
private const val ARG_CRIME_ID = "crime_id"
private const val DIALOG_DATE = "DialogDate"
private const val DIALOG_TIME = "DialogTime"
private const val DIALOG_IMAGE = "DialogImage"
private const val REQUEST_DATE = 0
private const val REQUEST_TIME = 1
private const val REQUEST_CONTACT = 2
private const val REQUEST_PHOTO = 3
private const val DETAILS_QUERY_ID: Int = 0
private const val DATE_FORMAT = "EEE MMM, dd"
class CrimeFragment: Fragment(), DatePickerFragment.Callbacks, LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * Required interface for hosting activities.
     *
     */
    private lateinit var crime: Crime
    private lateinit var photoFile: File

    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var timeButton: Button
    private lateinit var reportButton: Button
    private lateinit var suspectButton: Button
    private lateinit var callButton: Button
    private lateinit var photoButton: ImageButton
    private lateinit var photoView: ImageView
    private lateinit var photoUri: Uri
    private val crimeDetailViewModel: CrimeDetailViewModel by lazy{
        ViewModelProvider(this)[CrimeDetailViewModel::class.java]
    }
    private lateinit var contactPickLauncher : ActivityResultLauncher<Void>
    private lateinit var contactPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var photoLauncher: ActivityResultLauncher<Uri>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
        val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
       crimeDetailViewModel.loadCrime(crimeId)
        contactPickLauncher = registerForActivityResult(ActivityResultContracts.PickContact()
        ) {
            uri ->
            //Specify which fields you want your query to return values for
            val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
            val cursor = requireActivity().contentResolver.query(uri, queryFields, null, null, null)
            cursor?.use {
                //Verify cursor contains at least one result field
                if(it.count == 0)
                    return@registerForActivityResult
                it.moveToFirst()
                val suspect = it.getString(0)
                crime.suspect = suspect
                crimeDetailViewModel.saveCrime(crime)
                suspectButton.text = suspect
            }
        }
        contactPermissionLauncher =registerForActivityResult(ActivityResultContracts.RequestPermission()){
            isPermitted->
            if(!isPermitted)
                Toast.makeText(context, "No Permission", Toast.LENGTH_SHORT).show()
            else
                contactPickLauncher.launch(null)
        }
        photoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()){
            requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            Log.d(TAG, "onCreate: get image! the answer is $it")
            if(it == true)
                updatePhotoView()
        }
        
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val projection: Array<out String> = arrayOf(
            ContactsContract.Contacts._ID
        )
        val SELECTION = "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} LIKE ?"
        val selectionArgs: Array<String> = arrayOf(crime.suspect)
        val loader = when(id) {

            DETAILS_QUERY_ID-> {
                Log.d(TAG, "onCreateLoader: suspect is ${crime.suspect}")
                activity?.let {
                    CursorLoader(
                        it,
                        ContactsContract.Contacts.CONTENT_URI,
                        projection,
                        SELECTION,
                        selectionArgs,
                        null
                    )
                }
            }
            1->{
                activity?.let {
                    CursorLoader(
                        it,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(args!!.getString("id")),
                        null
                    )
                }
            }
            else -> {null}
        }
        return loader!!
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        when(loader.id){
            0 -> {
                LoaderManager.getInstance(this).initLoader(1, data?.let{ cursor->
                    cursor.moveToFirst()
                    Bundle().apply{ putString("id", cursor.getString(0))}
                }, this)
            }
            1 ->{
                callButton.text = data?.let{cursor ->
                    cursor.moveToFirst()
                    cursor.getString(0).toString()}
            }
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {

    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_crime, container, false)

        titleField = v.findViewById(R.id.crime_title) as EditText
        dateButton = v.findViewById(R.id.crime_date) as Button
        solvedCheckBox = v.findViewById(R.id.crime_solved) as CheckBox
        timeButton = v.findViewById(R.id.crime_time)
        reportButton = v.findViewById(R.id.crime_report)
        suspectButton = v.findViewById(R.id.crime_suspect)
        callButton = v.findViewById(R.id.crime_call)
        photoButton = v.findViewById(R.id.crime_camera)
        photoView = v.findViewById(R.id.crime_photo)

        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(viewLifecycleOwner){
            crime ->
            crime?.let {
                this.crime = crime
                photoFile = crimeDetailViewModel.getPhotoFile(crime)
                photoUri = FileProvider.getUriForFile(requireActivity(),
                "com.saigyouji.android.criminalintent.fileprovider",
                photoFile)

                if(crime.suspect.isNotEmpty())
                    LoaderManager.getInstance(this).initLoader(DETAILS_QUERY_ID, null,  this)
                updateUI()
            }
        }
    }
    override fun onStart() {
        super.onStart()

        val titleWatcher = object: TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                crime.title = s.toString()
            }

            override fun afterTextChanged(s: Editable?) {
            }
        }
        titleField.addTextChangedListener(titleWatcher)

        solvedCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked -> crime.isSolved = isChecked }
        }
        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(crime.date).apply{
                with(this@CrimeFragment) {
                    val fragmentManager = parentFragmentManager
                    fragmentManager.setFragmentResultListener(REQUEST_DATE.toString(), this
                    ){ _, result ->
                        val date = result.getSerializable("date") as Date
                        crime.date = date
                        updateUI()
                    }
                    this@apply.show(fragmentManager, DIALOG_DATE)
                }
            }
        }
        timeButton.setOnClickListener {
            TimePickerFragment.newInstance(crime.date).apply {
                val fragmentManager = this@CrimeFragment.parentFragmentManager
                fragmentManager.setFragmentResultListener(REQUEST_TIME.toString(), this@CrimeFragment)
                {
                    _, result ->
                    val date = result.getSerializable("time") as Date
                    crime.date = date
                    updateUI()
                }
                show(fragmentManager, DIALOG_TIME)
            }
        }
        reportButton.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
            }.also { intent ->
                val chooser = Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooser)
            }
        }
        suspectButton.apply {

            setOnClickListener {

                contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
              //  contactPickLauncher.launch(null)
            }

           val packageManager: PackageManager = requireActivity().packageManager
            val resolvedActivity: ResolveInfo? = packageManager.resolveActivity(Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI), PackageManager.MATCH_DEFAULT_ONLY)
            if(resolvedActivity == null)
               isEnabled = false
        }
        callButton.apply {
            setOnClickListener{
                val intent = Intent(Intent.ACTION_DIAL)
                val uri = Uri.parse("tel:${(it as Button).text}")
                intent.data = uri
                startActivity(intent)
            }
        }
        photoButton.apply {
            val packageManager: PackageManager = requireActivity().packageManager
            val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(captureImage, PackageManager.MATCH_DEFAULT_ONLY)
            if(resolvedActivity == null)
                isEnabled = false
            setOnClickListener {
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                val cameraActivitys: List<ResolveInfo> = packageManager.queryIntentActivities(captureImage,
                PackageManager.MATCH_DEFAULT_ONLY)

                for(cameraActivity in cameraActivitys){
                    requireActivity().grantUriPermission(
                        cameraActivity.activityInfo.packageName,
                        photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
                //startActivityForResult(captureImage, REQUEST_PHOTO)
                photoLauncher.launch(photoUri)
            }
        }
        photoView.apply {
            viewTreeObserver.addOnGlobalLayoutListener {
                if(this@CrimeFragment::photoUri.isInitialized)
                    updatePhotoView()
            }
            setOnClickListener {
                ImageDetailDialog.getInstance(photoFile.path)
                    .show(parentFragmentManager, DIALOG_IMAGE)
            }
        }

    }
    private fun updateUI(){
        titleField.setText(crime.title)
        dateButton.text = DateFormat.format("yyyy-MMM-ddd", crime.date)
        timeButton.text = DateFormat.format("hh:mm",crime.date.time)
        solvedCheckBox.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }
        if(crime.suspect.isNotEmpty()) {
            callButton.isEnabled = true
            suspectButton.text = crime.suspect
        }
        else{
            callButton.isEnabled = false
        }
    //    updatePhotoView()
    }

    private fun updatePhotoView(){
        if(photoFile.exists())
        {
            Log.d(TAG, "updatePhotoView: getImageUri: $photoUri")
            val bitmap = getScaledBitmap(photoFile.path, photoView.width, photoView.height)
            photoView.setImageBitmap(bitmap)
        }
        else
         photoView.setImageDrawable(null)
    }

    private fun getCrimeReport(): String{
        val solvedString = if(crime.isSolved)
            getString(R.string.crime_report_solved)
        else getString(R.string.crime_report_unsolved)

        val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()
        var suspect = if(crime.suspect.isBlank())
            getString(R.string.crime_report_no_suspect)
        else getString(R.string.crime_report_suspect, crime.suspect)

        return getString(R.string.crime_report, crime.title, dateString, solvedString, suspect)
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().revokeUriPermission(photoUri
        , Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }
    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    override fun onDateSelected(date: Date) {
        crime.date = date
        updateUI()
    }

    companion object{
        fun newInstance(crimeId: UUID): CrimeFragment{
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragment().apply {
                arguments = args
            }
        }
    }
}