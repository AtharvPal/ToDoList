package com.example.todolist2

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import androidx.room.Room
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main2.*
import kotlinx.android.synthetic.main.activity_task.*
import kotlinx.android.synthetic.main.add_category_dialog.*
import kotlinx.android.synthetic.main.dialog_layout.*
import kotlinx.android.synthetic.main.item_todo.*
import kotlinx.coroutines.*
import java.text.FieldPosition
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

const val DB_NAME = "todo.db"

class TaskActivity : AppCompatActivity(), View.OnClickListener {

    lateinit var myCalendar: Calendar

    lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    lateinit var timeSetListener: TimePickerDialog.OnTimeSetListener

    var finalDate = 0L
    var finalTime = 0L


//    private val labels = arrayListOf("Personal", "Business", "Insurance", "Shopping", "Banking","Other")
    private val per_labels = arrayListOf("Personal", "Business", "Insurance", "Shopping", "Banking","Other")
    private val months = arrayListOf("Jan", "Feb", "Mar", "Apr", "May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")

    lateinit var date3:String
    lateinit var time3:String

    val db by lazy {
        AppDatabase.getDatabase(this)
    }

    val db2 by lazy {
        CategoryDatabase.getDatabase(this)
    }

    override fun onPause() {
        overridePendingTransition(R.anim.fadein,R.anim.fadeout)
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(R.anim.fadein,R.anim.fadeout)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)

        setUpSpinner()

        titleInpLay.requestFocus()   // to set the initial focus to title edit text view
        dateEdt.setOnClickListener(this)
        timeEdt.setOnClickListener(this)
        saveBtn.setOnClickListener(this)

        var title2 = getIntent().getStringExtra("title")
        var desc2 = getIntent().getStringExtra("desc")
        var category2 = getIntent().getStringExtra("category").toString()
        var date2 = getIntent().getStringExtra("date")
        var time2 = getIntent().getStringExtra("time")
        var position2 = getIntent().getIntExtra("position",-1)
        Log.e("ok2",position2.toString())



        if(title2!=null) {
            titleInpLay.setText(title2.toString())
            addtask.text = "Edit Task"
            // below is to set cursor position of title at end of the textview
            titleInpLay.setSelection(titleInpLay.text.toString().length)
        }
        var defaultDate:String
        var defaultTime:String
        val myformat = "h:mm a"
        val sdf = SimpleDateFormat(myformat)
        defaultTime=sdf.format(System.currentTimeMillis())

        val myformat1 = "EEE, d MMM yyyy"
        val sdf1 = SimpleDateFormat(myformat1)
        defaultDate=sdf1.format(System.currentTimeMillis())
        Log.e("defult",defaultDate + " " + defaultTime)

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

        date3 = if (date2==null) defaultDate else date2
        time3 = if (time2==null) defaultTime else time2

        // how the heck people do android dev? it is literally trash

        var spinner_adapter = spinnerCategory.adapter as ArrayAdapter<String>
        var pos = spinner_adapter.getPosition(category2)
        spinnerCategory.setSelection(Math.max(0,pos))
        Log.e("spinner selected item 4",spinnerCategory.selectedItemPosition.toString())


    }


    fun addNewCategory(v:View){
        var dialog = Dialog(titleInpLay.context)
        dialog.setContentView(R.layout.add_category_dialog)
        dialog.show()
        var new_category_ok = dialog.findViewById<Button>(R.id.new_category_ok)
        var new_category_cancel = dialog.findViewById<Button>(R.id.new_category_cancel)
        var new_category_name = dialog.findViewById<EditText>(R.id.new_category_name)
        new_category_ok.isEnabled = false
        new_category_ok.setTextColor(getColor(R.color.gray))
        new_category_name.addTextChangedListener {
            var input = new_category_name.text.toString()
            // input.trim() doesn't work for input with multiple spaces eg "    " (4 spaces)
            input = input.trim()
            Log.e("len",input.length.toString())
            new_category_ok.isEnabled = !input.isBlank()
            if(new_category_ok.isEnabled)
                new_category_ok.setTextColor(getColor(R.color.green))
            else
                new_category_ok.setTextColor(getColor(R.color.gray))
        }
        new_category_cancel.setOnClickListener {
            dialog.dismiss()
        }
        new_category_ok.setOnClickListener {
            var new_cat = new_category_name.text.toString()
            new_cat.trim()
            Log.e("new ",new_cat)
            var already = false
//            runBlocking {
//                GlobalScope.launch {
//                    if ( new_cat in db2.categoryDao().getCategories2())
//                        already = true
//                }
//            }

//            GlobalScope.launch(Dispatchers.Main){
//                val id = withContext(Dispatchers.IO) {
//                    if(new_cat in db2.categoryDao().getCategories2())
//                        already = true
//                }
//            }
            runBlocking {
                launch(Dispatchers.IO) {
                    if(new_cat in db2.categoryDao().getCategories2())
                        already = true
                }
            }
            if (already){
                var imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                Log.e("already",new_cat + " exists")
                var snack = Snackbar.make(titleInpLay,"Category already exists!",Snackbar.LENGTH_SHORT)
                snack.show()
                dialog.dismiss()
            }
            else{
                GlobalScope.launch(Dispatchers.Main){
                    val id = withContext(Dispatchers.IO) {
                        return@withContext db2.categoryDao().insertCategory(
                            CategoryModel(
                                new_cat
                            )
                        )
                    }
                }
                var adap = spinnerCategory.adapter as ArrayAdapter<String>
                adap.add(new_cat)
                adap.notifyDataSetChanged()
                dialog.dismiss()
            }

        }
    }


    override fun onBackPressed() {
        var dialog = Dialog(titleInpLay.context)
        dialog.setContentView(R.layout.dialog_layout)
        val w = resources.displayMetrics.widthPixels*0.9   // to occupy 90% of screen's width
//        val h = resources.displayMetrics.heightPixels*0.9   // to occupy 90% of screen's height
        dialog.window?.setLayout(w.toInt(),ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
        var dialog_text = "Discard To Do?"

        // this is to determine whether task activity is opened for new/update
        // task; depending on that, the text of dialog changes
        var title2 = getIntent().getStringExtra("title")
        if(title2!=null)
            dialog_text = "Discard changes?"
        var discard = dialog.findViewById<TextView>(R.id.discard)
        var cancel_button = dialog.findViewById<Button>(R.id.cancel_button)
        var yes_button = dialog.findViewById<Button>(R.id.yes_button)
        discard.text = dialog_text
//        yes_button.setBackgroundColor(Color.RED)  // why this doesn't happen???
        cancel_button.setOnClickListener {
            dialog.dismiss()
        }
        yes_button.setOnClickListener {
            dialog.dismiss()
            finish()
        }
    }
    private fun setUpSpinner() {
        var databaseLabels:MutableList<String> = per_labels

        runBlocking {
            GlobalScope.launch {
                Log.e("thread 2",Thread.currentThread().name)
                databaseLabels.addAll(db2.categoryDao().getCategories2())
            }
            Log.e("thread",Thread.currentThread().name)

        }

        val adap:ArrayAdapter<String> = object: ArrayAdapter<String>(this, R.layout.spinner_item_layout, databaseLabels){
            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val v = super.getDropDownView(position, convertView, parent)
                var selected = v as TextView
                if (position == spinnerCategory.selectedItemPosition){
                    v.setBackgroundColor(getColor(R.color.white))
                    selected.setTextColor(getColor(R.color.gray_toolbar))
                }

                else
                {
                    v.setBackgroundColor(getColor(R.color.gray))
                    selected.setTextColor(Color.WHITE)
                }
                return v
            }
        }
        spinnerCategory.adapter = adap

        spinnerCategory.setPopupBackgroundResource(R.color.black)



    }

    override fun onClick(v: View) {

//         hide the keyboard if open (as you don't need a keyboard for date or time editing)
        var imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        imm.hideSoftInputFromWindow(titleInpLay.windowToken,0)
//        imm.hideSoftInputFromWindow(taskInpLay.windowToken,0)
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        when (v.id) {
            R.id.dateEdt -> {
                setListener()
            }
            R.id.timeEdt -> {
                setTimeListener()
            }
            R.id.saveBtn -> {
                var position2 = getIntent().getIntExtra("position",-1)
                Log.e("ok3",position2.toString())
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
        else if(finalDate==0L)
            Toast.makeText(titleInpLay.context,"Please select a date",Toast.LENGTH_SHORT).show()
        else if(finalTime==0L)
            Toast.makeText(titleInpLay.context,"Please select a time",Toast.LENGTH_SHORT).show()

        else {
            var is_updated = false
            if (position!=-1){
                is_updated = true
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
//                Log.e("compare 4"," ${combinedDateTime} is befiore ${currentDateTime}")
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
            if(is_updated)
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
            TimePickerDialog.OnTimeSetListener() { _: TimePicker, hourOfDay: Int, min: Int ->
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
        val myformat = "h:mm a"
        val sdf = SimpleDateFormat(myformat)
        finalTime = myCalendar.time.time
        timeEdt.setText(sdf.format(myCalendar.time))

    }

    private fun setListener() {
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
//        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun updateDate() {
        //Mon, 5 Jan 2020
        val myformat = "EEE, d MMM yyyy"
        val sdf = SimpleDateFormat(myformat)
        finalDate = myCalendar.time.time
        dateEdt.setText(sdf.format(myCalendar.time))

    }

    private fun updateDate2(currDate:String) {
        // ADD THIS FUNCTION TO A CLASS
        //Mon, 5 Jan 2020
        myCalendar = Calendar.getInstance()
        var n = currDate.length
//        Log.e("len",n.toString())
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

        val myformat = "EEE, d MMM yyyy"
        val sdf = SimpleDateFormat(myformat)
        finalDate = myCalendar.time.time
        dateEdt.setText(sdf.format(myCalendar.time))

        timeEdt.visibility = View.VISIBLE

    }


    private fun updateTime2(currTime:String) {

        // ADD THIS FUNCTION TO A CLASS
        //Mon, 5 Jan 2020

        myCalendar = Calendar.getInstance()
        var n = currTime.length
//        Log.e("len",n.toString())
        if(n==8){
            var am_pm = currTime.substring(6,8);
            if(am_pm=="am")
                myCalendar.set(Calendar.AM_PM,Calendar.AM)
            else
                myCalendar.set(Calendar.AM_PM,Calendar.PM)
            myCalendar.set(Calendar.MINUTE,currTime.substring(3,5).toInt())
            myCalendar.set(Calendar.HOUR,currTime.substring(0,2).toInt())
        }
        else{
            var am_pm = currTime.substring(5,7);
            if(am_pm=="am")
                myCalendar.set(Calendar.AM_PM,Calendar.AM)
            else
                myCalendar.set(Calendar.AM_PM,Calendar.PM)
            myCalendar.set(Calendar.MINUTE,currTime.substring(2,4).toInt())
            myCalendar.set(Calendar.HOUR,currTime.substring(0,1).toInt())
        }
        val myformat = "h:mm a"
        val sdf = SimpleDateFormat(myformat)
        finalTime = myCalendar.time.time
        timeEdt.setText(sdf.format(myCalendar.time))

    }


}
