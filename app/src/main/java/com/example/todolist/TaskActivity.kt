package com.example.todolist2

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.todolist2.databinding.ActivityTaskBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_task.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

const val DB_NAME = "todo.db"

class TaskActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var myCalendar: Calendar

    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    private lateinit var timeSetListener: TimePickerDialog.OnTimeSetListener

    private lateinit var binding:ActivityTaskBinding

    private var finalDate = 0L
    private var finalTime = 0L


    private val defaultCategories = listOf("Personal", "Business", "Insurance", "Shopping", "Banking","Other")
    private val months = listOf("Jan", "Feb", "Mar", "Apr", "May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")

    private lateinit var date3:String
    private lateinit var time3:String

    private val db by lazy {
        AppDatabase.getDatabase(this)
    }

    private val db2 by lazy {
        CategoryDatabase.getDatabase(this)
    }

    override fun onPause() {
        overridePendingTransition(R.anim.fadein,R.anim.fadeout)
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(R.anim.fadein,R.anim.fadeout)
        super.onCreate(savedInstanceState)
        binding = ActivityTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpSpinner()

        titleInpLay.requestFocus()   // to set the initial focus to title edit text view
        dateEdt.setOnClickListener(this)
        timeEdt.setOnClickListener(this)
        saveBtn.setOnClickListener(this)

        val title2 = intent.getStringExtra("title")
        val desc2 = intent.getStringExtra("desc")
        val category2 = intent.getStringExtra("category").toString()
        val date2 = intent.getStringExtra("date")
        val time2 = intent.getStringExtra("time")
        val position2 = intent.getIntExtra("position",-1)
        Log.e("ok2",position2.toString())



        if(title2!=null) {
            titleInpLay.setText(title2.toString())
            addtask.text = "Edit Task"
            // below is to set cursor position of title at end of the textview
            titleInpLay.setSelection(titleInpLay.text.toString().length)
        }
        val defaultDate:String
        val defaultTime:String
        val myFormat = "h:mm a"
        val sdf = SimpleDateFormat(myFormat)
        defaultTime=sdf.format(System.currentTimeMillis())

        val myFormat1 = "EEE, d MMM yyyy"
        val sdf1 = SimpleDateFormat(myFormat1)
        defaultDate=sdf1.format(System.currentTimeMillis())

        if(desc2!=null)
            taskInpLay.setText(desc2.toString())
        taskInpLay.setText(desc2)
        if(date2!=null)
            updateDate2(date2.toString())
        else
            updateDate2(defaultDate)
        if(time2!=null)
            updateTime2(time2.toString())
        else
            updateTime2(defaultTime)

        date3 = date2 ?: defaultDate
        time3 = time2 ?: defaultTime

        // how the heck people do android dev? it is literally trash

        val spinnerAdapter = spinnerCategory.adapter as ArrayAdapter<String>
        val pos = spinnerAdapter.getPosition(category2)
        spinnerCategory.setSelection(Math.max(0,pos))

        binding.addNewCategory.setOnClickListener {
            addNewCategory()
        }

    }


    private fun addNewCategory(){
        val dialog = Dialog(titleInpLay.context)
        dialog.setContentView(R.layout.add_category_dialog)
        dialog.show()
        val newCategoryOk = dialog.findViewById<Button>(R.id.new_category_ok)
        val newCategoryCancel = dialog.findViewById<Button>(R.id.new_category_cancel)
        val newCategoryName = dialog.findViewById<EditText>(R.id.new_category_name)
        newCategoryOk.isEnabled = false
        newCategoryOk.setTextColor(getColor(R.color.gray))
        newCategoryName.addTextChangedListener {
            var input = newCategoryName.text.toString()
            // input.trim() doesn't work for input with multiple spaces eg "    " (4 spaces)
            input = input.trim()
            newCategoryOk.isEnabled = input.isNotBlank()
            if(newCategoryOk.isEnabled)
                newCategoryOk.setTextColor(getColor(R.color.green))
            else
                newCategoryOk.setTextColor(getColor(R.color.gray))
        }
        newCategoryCancel.setOnClickListener {
            dialog.dismiss()
        }
        newCategoryOk.setOnClickListener {
            val newCategory = newCategoryName.text.toString()
            newCategory.trim()
            var already = false
            runBlocking {
                launch(Dispatchers.IO) {
                    if(newCategory in db2.categoryDao().getCategories2())
                        already = true
                }
            }
            if (already){
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
                val snack = Snackbar.make(titleInpLay,"Category already exists!",Snackbar.LENGTH_SHORT)
                snack.show()
                dialog.dismiss()
            }
            else{
                GlobalScope.launch(Dispatchers.Main){
                    val id = withContext(Dispatchers.IO) {
                        return@withContext db2.categoryDao().insertCategory(
                            CategoryModel(
                                newCategory
                            )
                        )
                    }
                }
                val spinnerAdapter = spinnerCategory.adapter as ArrayAdapter<String>
                spinnerAdapter.add(newCategory)
                spinnerAdapter.notifyDataSetChanged()
                dialog.dismiss()
            }

        }
    }


    override fun onBackPressed() {
        val dialog = Dialog(titleInpLay.context)
        dialog.setContentView(R.layout.dialog_layout)
        val w = resources.displayMetrics.widthPixels*0.95   // to occupy 95% of screen's width
        dialog.window?.setLayout(w.toInt(),ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
        var dialogText = "Discard To Do?"

        // this is to determine whether task activity is opened for new/update
        // task; depending on that, the text of dialog changes
        val title2 = intent.getStringExtra("title")
        if(title2!=null)
            dialogText = "Discard changes?"
        val discard = dialog.findViewById<TextView>(R.id.discard)
        val cancelBtn = dialog.findViewById<Button>(R.id.cancel_button)
        val yesBtn = dialog.findViewById<Button>(R.id.yes_button)
        discard.text = dialogText
//        yes_button.setBackgroundColor(Color.RED)  // why this doesn't happen???
        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }
        yesBtn.setOnClickListener {
            dialog.dismiss()
            finish()
        }
    }
    private fun setUpSpinner() {
        val databaseLabels = mutableListOf<String>()
        databaseLabels.addAll(defaultCategories)

        runBlocking {
            GlobalScope.launch {
                databaseLabels.addAll(db2.categoryDao().getCategories2())
            }
        }

        val spinnerAdapter:ArrayAdapter<String> = object: ArrayAdapter<String>(this, R.layout.spinner_item_layout, databaseLabels){
            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val v = super.getDropDownView(position, convertView, parent)
                val selected = v as TextView
                if (position == spinnerCategory.selectedItemPosition) {
                    v.setBackgroundColor(getColor(R.color.white))
                    selected.setTextColor(getColor(R.color.gray_toolbar))
                }

                else {
                    v.setBackgroundColor(getColor(R.color.gray))
                    selected.setTextColor(Color.WHITE)
                }
                return v
            }
        }
        spinnerCategory.adapter = spinnerAdapter
        spinnerCategory.setPopupBackgroundResource(R.color.black)
    }

    override fun onClick(v: View) {

//         hide the keyboard if open (as you don't need a keyboard for date or time editing)
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        when (v.id) {
            R.id.dateEdt -> {
                setDateListener()
            }
            R.id.timeEdt -> {
                setTimeListener()
            }
            R.id.saveBtn -> {
                val position2 = intent.getIntExtra("position",-1)
                saveTodo(position2)
            }
        }
    }

    private fun saveTodo(position: Int) {
        val category = spinnerCategory.selectedItem.toString()
        val title = titleInpLay.text.toString()
        val description = taskInpLay.text.toString()
        if (TextUtils.isEmpty(title))
            Toast.makeText(titleInpLay.context,"Please enter a title",Toast.LENGTH_SHORT).show()
        else if(finalDate == 0L)
            Toast.makeText(titleInpLay.context,"Please select a date",Toast.LENGTH_SHORT).show()
        else if(finalTime == 0L)
            Toast.makeText(titleInpLay.context,"Please select a time",Toast.LENGTH_SHORT).show()

        else {
            var isUpdated = false
            if (position!=-1){
                isUpdated = true
                GlobalScope.launch(Dispatchers.IO) {
                    db.todoDao().deleteTask(position.toLong())
                }
            }
//            val myFormat = "EEE, d MMM yyyy h:mm a"
//            val sdf = SimpleDateFormat(myFormat)
//            val myFormat1 = "EEE, d MMM yyyy"
//            val sdf1 = SimpleDateFormat(myFormat1)
//            val myFormat2 = "h:mm a"
//            val sdf2 = SimpleDateFormat(myFormat2)
//            val currentDateTime = sdf.format(Date(System.currentTimeMillis()))
//            Log.e("compare 1","${sdf.format(Date(myCalendar.timeInMillis))}: ${currentDateTime} and ${myCalendar.timeInMillis}: ${System.currentTimeMillis()}")
//            Log.e("compare 2","${sdf.format(Date(finalDate))} and ${sdf.format(Date(finalTime))}")
//            Log.e("compare 3","${sdf1.format(Date(finalDate))} and ${sdf2.format(Date(finalTime))}")
//            val combinedDateTime = sdf1.format(finalDate)+" "+ sdf2.format(finalTime)
//            if (sdf.parse(combinedDateTime).before(sdf.parse(currentDateTime)))
//                Log.e("compare 4"," ${combinedDateTime} is before ${currentDateTime}")
//            else if(sdf.parse(combinedDateTime).after(sdf.parse(currentDateTime)))
//                Log.e("compare 4"," ${combinedDateTime} is after ${currentDateTime}")
//            else
//                Log.e("compare 4"," ${combinedDateTime} is equal to ${currentDateTime}")
            GlobalScope.launch(Dispatchers.Main){
                val id = withContext(Dispatchers.IO) {
                    return@withContext db.todoDao().insertTask(
                        TodoModel(
                            title,
                            description,
                            category,
                            finalDate,
                            finalTime
                        )
                    )
                }
            }
            var msg = "To Do saved"
            if(isUpdated)
                msg = "To Do updated"
            Snackbar.make(findViewById(android.R.id.content),msg,Snackbar.LENGTH_SHORT).show()
            Toast.makeText(this@TaskActivity,msg,Toast.LENGTH_SHORT).show()
            finish()
        }
    }


    fun finishTask(view:View){
        onBackPressed()
    }




    private fun setTimeListener() {
        myCalendar = Calendar.getInstance()

        updateTime2(time3)

        timeSetListener =
            TimePickerDialog.OnTimeSetListener { _: TimePicker, hourOfDay: Int, min: Int ->
                myCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                myCalendar.set(Calendar.MINUTE, min)
                updateTime()
            }

        val timePickerDialog = TimePickerDialog(
            this, timeSetListener, myCalendar.get(Calendar.HOUR_OF_DAY),
            myCalendar.get(Calendar.MINUTE), false
        )

        timePickerDialog.show()
    }

    private fun updateTime() {
        //Mon, 5 Jan 2020
        val myFormat = "h:mm a"
        val sdf = SimpleDateFormat(myFormat)
        finalTime = myCalendar.time.time
        timeEdt.setText(sdf.format(myCalendar.time))

    }

    private fun setDateListener() {
        myCalendar = Calendar.getInstance()

        updateDate2(date3)

        dateSetListener =
            DatePickerDialog.OnDateSetListener { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                myCalendar.set(Calendar.YEAR, year)
                myCalendar.set(Calendar.MONTH, month)
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDate()

            }

        val datePickerDialog = DatePickerDialog(
            this, dateSetListener, myCalendar.get(Calendar.YEAR),
            myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun updateDate() {
        //Mon, 5 Jan 2020
        val myFormat = "EEE, d MMM yyyy"
        val sdf = SimpleDateFormat(myFormat)
        finalDate = myCalendar.time.time
        dateEdt.setText(sdf.format(myCalendar.time))

    }

    private fun updateDate2(currDate:String) {
        // ADD THIS FUNCTION TO A CLASS
        //Mon, 5 Jan 2020
        myCalendar = Calendar.getInstance()
        val n = currDate.length
        if(n==16) {
            myCalendar.set(Calendar.YEAR, currDate.substring(12, 16).toInt())
            for(i in 0..11){
                if(TextUtils.equals(months[i],currDate.substring(8,11))){
                    myCalendar.set(Calendar.MONTH,i)
                    break
                }
            }
            myCalendar.set(Calendar.DAY_OF_MONTH, currDate.substring(5, 7).toInt())
        }
        else {
            myCalendar.set(Calendar.YEAR, currDate.substring(11, 15).toInt())
            myCalendar.set(Calendar.DATE, currDate.substring(5, 6).toInt())
            for(i in 0..11){
                if(TextUtils.equals(months[i],currDate.substring(7,10))){
                    myCalendar.set(Calendar.MONTH,i)
                    break
                }
            }
            myCalendar.set(Calendar.DAY_OF_MONTH, currDate.substring(5, 6).toInt())
        }

        val myFormat = "EEE, d MMM yyyy"
        val sdf = SimpleDateFormat(myFormat)
        finalDate = myCalendar.time.time
        dateEdt.setText(sdf.format(myCalendar.time))

        timeEdt.visibility = View.VISIBLE

    }


    private fun updateTime2(currTime:String) {

        // ADD THIS FUNCTION TO A CLASS
        //Mon, 5 Jan 2020

        myCalendar = Calendar.getInstance()
        val n = currTime.length
        if(n==8){
            val format24Hr = currTime.substring(6,8)
            if(format24Hr == "am")
                myCalendar.set(Calendar.AM_PM,Calendar.AM)
            else
                myCalendar.set(Calendar.AM_PM,Calendar.PM)
            myCalendar.set(Calendar.MINUTE,currTime.substring(3,5).toInt())
            myCalendar.set(Calendar.HOUR,currTime.substring(0,2).toInt())
        }
        else{
            val format24Hr = currTime.substring(5,7)
            if(format24Hr == "am")
                myCalendar.set(Calendar.AM_PM,Calendar.AM)
            else
                myCalendar.set(Calendar.AM_PM,Calendar.PM)
            myCalendar.set(Calendar.MINUTE,currTime.substring(2,4).toInt())
            myCalendar.set(Calendar.HOUR,currTime.substring(0,1).toInt())
        }
        val myFormat = "h:mm a"
        val sdf = SimpleDateFormat(myFormat)
        finalTime = myCalendar.time.time
        timeEdt.setText(sdf.format(myCalendar.time))

    }


}
