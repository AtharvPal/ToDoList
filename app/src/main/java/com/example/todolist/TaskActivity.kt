package com.example.todolist

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
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_task.*
import kotlinx.android.synthetic.main.dialog_layout.*
import kotlinx.android.synthetic.main.item_todo.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.FieldPosition
import java.text.SimpleDateFormat
import java.util.*

const val DB_NAME = "todo.db"

class TaskActivity : AppCompatActivity(), View.OnClickListener {

    lateinit var myCalendar: Calendar

    lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    lateinit var timeSetListener: TimePickerDialog.OnTimeSetListener

    var finalDate = 0L
    var finalTime = 0L


    private val labels = arrayListOf("Personal", "Business", "Insurance", "Shopping", "Banking","Other")
    private val months = arrayListOf("Jan", "Feb", "Mar", "Apr", "May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")


    val db by lazy {
        AppDatabase.getDatabase(this)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
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

//        var imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager   // idk why tf this doesn't work
//        imm.showSoftInput(titleInpLay,InputMethodManager.SHOW_FORCED)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)   // why do I have to write this?
        if(title2!=null) {
            title_task.setText(title2.toString())
            addtask.text = "Edit Task"
            // below is to set cursor position of title at end of the textview
            title_task.setSelection(title_task.text.toString().length)
        }

        if(desc2!=null)
            desc_task.setText(desc2.toString())
        desc_task.setText(desc2)
        if(date2!=null)
            updateDate2(date2.toString())
        if(time2!=null)
            updateTime2(time2.toString())

        // how the fuck people do android dev? it is literally fucking trash

        var spinner_adapter = spinnerCategory.adapter as ArrayAdapter<String>
        var pos = spinner_adapter.getPosition(category2)
        spinnerCategory.setSelection(pos)


    }

    override fun onBackPressed() {
        var dialog = Dialog(titleInpLay.context)
        dialog.setContentView(R.layout.dialog_layout)
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
            finish()
        }
    }
    private fun setUpSpinner() {
        val adapter =
            ArrayAdapter<String>(this, R.layout.spinner_item_layout, labels)

        spinnerCategory.adapter = adapter
    }

    override fun onClick(v: View) {

//         hide the keyboard if open (as you don't need a keyboard for date or time editing)
        var imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(title_task.windowToken,0)
        imm.hideSoftInputFromWindow(desc_task.windowToken,0)
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
        val title = titleInpLay.editText?.text.toString()
        val description = taskInpLay.editText?.text.toString()
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

    private fun setTimeListener() {
        myCalendar = Calendar.getInstance()

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
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun updateDate() {
        //Mon, 5 Jan 2020
        val myformat = "EEE, d MMM yyyy"
        val sdf = SimpleDateFormat(myformat)
        finalDate = myCalendar.time.time
        dateEdt.setText(sdf.format(myCalendar.time))

        timeInptLay.visibility = View.VISIBLE

    }

    private fun updateDate2(currDate:String) {
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

        timeInptLay.visibility = View.VISIBLE

    }


    private fun updateTime2(currTime:String) {
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