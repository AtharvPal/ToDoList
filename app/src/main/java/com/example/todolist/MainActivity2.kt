package com.example.todolist2

import android.app.Dialog
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main2.*
import kotlinx.android.synthetic.main.activity_task.*
import kotlinx.android.synthetic.main.delete_category_layout.*
import kotlinx.android.synthetic.main.item_todo.*
import kotlinx.android.synthetic.main.item_todo.view.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class MainActivity2 : AppCompatActivity() {

    var list = arrayListOf<TodoModel>()
    var adapter = TodoAdapter(list)

//    private val labels =
//        arrayListOf("All", "Personal", "Business", "Insurance", "Shopping", "Banking", "Other")
    private val per_labels = arrayListOf("All", "Personal", "Business", "Insurance", "Shopping", "Banking", "Other")
    var labels = arrayListOf<String>()

    val db by lazy {
        AppDatabase.getDatabase(this)
    }

    val db2 by lazy {
        CategoryDatabase.getDatabase(this)
    }

    override fun onStart() {
        super.onStart()
        setTheSpinner()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(R.anim.fadein,R.anim.fadeout)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        setSupportActionBar(toolbar)
        Log.e("wow 1","1")
        supportActionBar?.setDisplayShowTitleEnabled(false)
        todoRv.apply {
            layoutManager = LinearLayoutManager(this@MainActivity2)
            adapter = this@MainActivity2.adapter
        }
        todoRv.scheduleLayoutAnimation()
        initSwipe()

        Log.e("wow 2","2")
        db.todoDao().getTask().observe(this, Observer {
            Log.e("wow 3","3")
            list.clear()
            if (!it.isNullOrEmpty())
                list.addAll(it)
            adapter.notifyDataSetChanged()
            if (list.isEmpty()) {
                emptyList.visibility = View.VISIBLE
                not_found_anim.visibility = View.VISIBLE
            }
            else {
                emptyList.visibility = View.GONE
                not_found_anim.visibility = View.GONE
            }
            Log.e("wow 4","4")
        })
        Log.e("wow 5","5")
//        setTheSpinner()
        setTheSearchBar()
    }

    private fun setTheSearchBar() {
        searchView_toolbar.queryHint = "Enter a title to search..."

        // setOnSearchListener is when user clicks on the search icon, it should by default show all the todos
        searchView_toolbar.setOnSearchClickListener {
            displayTodo2(labels[0])
            Log.e("saarch","clck")
        }
        searchView_toolbar.setOnQueryTextFocusChangeListener(object:View.OnFocusChangeListener{
            override fun onFocusChange(v: View?, hasFocus: Boolean) {
                if(hasFocus){
                    Log.e("search","done 1")
                    spinner_toolbar.visibility = View.GONE
                    delete_toolbar.visibility = View.GONE
                    searchView_toolbar.maxWidth = 1000
                    toolbar.setBackgroundColor(getColor(R.color.black))

                }
                else {
                    Log.e("search", "done 2")
//                    spinner.visibility = View.VISIBLE
                }
            }
        })
        searchView_toolbar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Toast.makeText(this@MainActivity2, "thir", Toast.LENGTH_SHORT).show()
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                displayTodo(newText)
                return true
            }


        })
        searchView_toolbar.setOnCloseListener(object : SearchView.OnCloseListener {
            override fun onClose(): Boolean {
                displayTodo2(labels[spinner_toolbar.selectedItemPosition])
                spinner_toolbar.visibility = View.VISIBLE
                val per_labels2 = arrayListOf("All", "Personal", "Business", "Insurance", "Shopping", "Banking", "Other")
                if(spinner_toolbar.selectedItemPosition>=per_labels2.size){
                    delete_toolbar.visibility = View.VISIBLE
                }
                toolbar.setBackgroundColor(getColor(R.color.gray_toolbar))
                return false
            }

        })
    }

    fun printCat(l:MutableList<String>){
        for(x in l)
            Log.e(";abels",x)
        Log.e(";lala","   ")
    }

    fun deleteCategory(v:View){
        val cat = spinner_toolbar.selectedItem as String
        var dialog = Dialog(this)
        dialog.setContentView(R.layout.delete_category_layout)
        val w = resources.displayMetrics.widthPixels*0.9   // to occupy 90% of screen's width
//        val h = resources.displayMetrics.heightPixels*0.9   // to occupy 90% of screen's height
        dialog.window?.setLayout(w.toInt(),ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
        var delete_category = dialog.findViewById<TextView>(R.id.delete_category)
        var cancel_button_delete = dialog.findViewById<Button>(R.id.cancel_button_delete)
        var yes_button_delete = dialog.findViewById<Button>(R.id.yes_button_delete)
        delete_category.text = "Delete the category '" + cat + "' ? Any todos in this category will also be deleted."
        cancel_button_delete.setOnClickListener {
            dialog.dismiss()
        }
        yes_button_delete.setOnClickListener {
            val bad_todo = ArrayList<Long>()
            runBlocking {
                launch(Dispatchers.IO) {
                    for (x in db.todoDao().getAllTasks()){
                        if(x.category == cat)
                            bad_todo.add(x.id)
                    }

                }
            }
            runBlocking {
                launch(Dispatchers.IO) {
                    for(x in bad_todo)
                        db.todoDao().deleteTask(x)
                }
            }
            runBlocking {
                launch(Dispatchers.IO) {
                    db2.categoryDao().deleteCategory(cat)
                }
            }
            var adap = spinner_toolbar.adapter as ArrayAdapter<String>
            adap.remove(cat)
            adap.notifyDataSetChanged()
            displayTodo2(labels[adap.count-1])
            dialog.dismiss()
        }
    }

    private fun setTheSpinner() {

        var databaseLabels:MutableList<String> = per_labels

//        runBlocking {
//            GlobalScope.launch {
//                Log.e("thread 2",Thread.currentThread().name)
//                databaseLabels.addAll(db2.categoryDao().getCategories2())
//            }
//            Log.e("thread",Thread.currentThread().name)
//            for(x in databaseLabels)
//                Log.e(";abels",x)
//        }

        printCat(databaseLabels)
        runBlocking {
            launch(Dispatchers.IO) {
                val per_labels2 = arrayListOf("All", "Personal", "Business", "Insurance", "Shopping", "Banking", "Other")
                printCat(databaseLabels)
                databaseLabels.clear()
                printCat(databaseLabels)
                Log.e(";len",per_labels2.size.toString())
                Log.e(";thread",Thread.currentThread().name)
                databaseLabels.addAll(per_labels2)
                printCat(databaseLabels)
                databaseLabels.addAll(db2.categoryDao().getCategories2())
                printCat(databaseLabels)
            }
        }

        printCat(databaseLabels)

        labels = databaseLabels as ArrayList<String>
        val adap:ArrayAdapter<String> = object: ArrayAdapter<String>(this, R.layout.spinner_item_layout_main, labels){
            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val v = super.getDropDownView(position, convertView, parent)
                var selected = v as TextView
                if (position == spinner_toolbar.selectedItemPosition){
                    v.setBackgroundColor(getColor(R.color.white))
                    selected.setTextColor(Color.BLACK)
                }

                else
                {
                    v.setBackgroundColor(getColor(R.color.gray))
                    selected.setTextColor(Color.WHITE)
                }
                return v
            }
        }
        spinner_toolbar.adapter = adap

        spinner_toolbar.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {    // idk why tf this has to be done like this
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                displayTodo2(labels[position])
                Log.e("visisble",position.toString() + " " + per_labels.size.toString())
                val per_labels2 = arrayListOf("All", "Personal", "Business", "Insurance", "Shopping", "Banking", "Other")
                if(position>=per_labels2.size){
                    delete_toolbar.visibility = View.VISIBLE
                }
                else{
                    delete_toolbar.visibility = View.GONE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {    // is this ever called ????
                Toast.makeText(this@MainActivity2, "nothing", Toast.LENGTH_SHORT).show()
            }
        }

    }





    fun displayTodo(newText: String = "") {
        db.todoDao().getTask().observe(this, Observer {
            if (it.isNotEmpty()) {
                list.clear()
                if (TextUtils.isEmpty(newText))
                    list.addAll(it)
                else {
                    list.addAll(
                        it.filter { todo ->
                            todo.title.contains(newText, true)
                        }
                    )
                }
                adapter.notifyDataSetChanged()
            }
            if (list.isEmpty())
            {
                emptyList.visibility = View.VISIBLE
                not_found_anim.visibility = View.VISIBLE
            }
            else
            {
                emptyList.visibility = View.GONE
                not_found_anim.visibility = View.GONE
            }
        })
    }

    fun displayTodo2(newText: String) {
        db.todoDao().getTask().observe(this, Observer {
            if (it.isNotEmpty()) {
                list.clear()
                if (TextUtils.equals(newText, "All"))
                    list.addAll(it)
                else {
                    list.addAll(
                        it.filter { todo ->
                            todo.category.equals(newText, false)
                        }
                    )
                }
                adapter.notifyDataSetChanged()
            }
            if (list.isEmpty())
            {
                emptyList.visibility = View.VISIBLE
                not_found_anim.visibility = View.VISIBLE
            }
            else
            {
                emptyList.visibility = View.GONE
                not_found_anim.visibility = View.GONE
            }
        })
    }

    fun openNewTask(view: View) {
        startActivity(Intent(this,TaskActivity::class.java))


    }

    fun openHistory(view: View) {
        startActivity(Intent(this,HistoryActivity::class.java))

    }


















    fun initSwipe() {
        val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
//                Log.e("gg",position.toString())

                // I need the id (Primary Key) of the to-do because in case user decides to undo the delete operation,
                // the Primary Key is needed to restore the to-do in the same place as before

                var id = adapter.getItemId(position)
//                Log.e("gg",adapter.getItemId(position).toString())
//                Log.e("gg",v.toString())


                // saving the view to-be-deleted in a variable
                var restored_view = viewHolder.itemView
//                Log.e("adding",restored_view.txtShowTitle.text.toString())

                // making an object of the to-do item (ToDoModel) about to be deleted
                // Here I need to pass the primary key (ie id) so that item is restored in the same position in the recyclerview
                var restored_model = TodoModel(restored_view.txtShowTitle.text.toString(),
                    restored_view.txtShowTask.text.toString(),
                    restored_view.txtShowCategory.text.toString(),
                    updateDate3(restored_view.txtShowDate.text.toString()),
                    updateTime3(restored_view.txtShowTime.text.toString()),0,id)
                if (direction == ItemTouchHelper.LEFT) {


                    GlobalScope.launch(Dispatchers.IO) {
                        db.todoDao().deleteTask(adapter.getItemId(position))
                    }




                    var snack = Snackbar.make(toolbar,"To Do deleted",Snackbar.LENGTH_SHORT)

                    // if user decides to restore the to-do
                    snack.setAction("UNDO",View.OnClickListener {

                        // insert the deleted to-do as a new to-do, passing the primary key of the former to latter
                        GlobalScope.launch(Dispatchers.Main){
                            val id = withContext(Dispatchers.IO) {
                                return@withContext db.todoDao().insertTask(
                                    restored_model
                                )
                            }
                        }



//                        Log.e("gg2",position.toString())
//                        list.add(position,restored_model)
                        var snack2 = Snackbar.make(toolbar,"To Do restored",Snackbar.LENGTH_SHORT)
                        snack2.show()
                    })
                    snack.setActionTextColor(getColor(R.color.green))
                    snack.show()
                } else if (direction == ItemTouchHelper.RIGHT) {
                    GlobalScope.launch(Dispatchers.IO) {
                        db.todoDao().finishTask(adapter.getItemId(position))
                    }
                    Snackbar.make(toolbar,"To Do finished",Snackbar.LENGTH_SHORT).show()
                }
                displayTodo2(restored_view.txtShowCategory.text.toString())
            }

            override fun onChildDraw(
                canvas: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView

                    val paint = Paint()
                    val icon: Bitmap

                    if (dX > 0) {

                        icon = BitmapFactory.decodeResource(resources, R.mipmap.ic_check_white_png)
                        paint.color = Color.parseColor("#388E3C")

//                        canvas.drawRect(
//                            itemView.left.toFloat(), itemView.top.toFloat(),
//                            itemView.left.toFloat() + dX, itemView.bottom.toFloat(), paint
//                        )
                        canvas.drawRect(
                            itemView.left.toFloat(), itemView.top.toFloat(),
                            itemView.right.toFloat(), itemView.bottom.toFloat(), paint
                        )
                        canvas.drawBitmap(
                            icon,
                            itemView.left.toFloat(),
                            itemView.top.toFloat() + (itemView.bottom.toFloat() - itemView.top.toFloat() - icon.height.toFloat()) / 2,
                            paint
                        )


                    } else  if (dX<0){
                        icon = BitmapFactory.decodeResource(resources, R.mipmap.ic_delete_white_png)

                        paint.color = Color.parseColor("#D32F2F")

//                        canvas.drawRect(
//                            itemView.right.toFloat() + dX, itemView.top.toFloat(),
//                            itemView.right.toFloat(), itemView.bottom.toFloat(), paint
//                        )
                        canvas.drawRect(
                            itemView.left.toFloat(), itemView.top.toFloat(),
                            itemView.right.toFloat(), itemView.bottom.toFloat(), paint
                        )
                        canvas.drawBitmap(
                            icon,
                            itemView.right.toFloat() - icon.width,
                            itemView.top.toFloat() + (itemView.bottom.toFloat() - itemView.top.toFloat() - icon.height.toFloat()) / 2,
                            paint
                        )
                    }
                    else{   // this else is when item is not swiped, which is to set color to black
                        paint.color = getColor(R.color.black)

                        canvas.drawRect(
                            itemView.left.toFloat(), itemView.top.toFloat(),
                            itemView.right.toFloat(), itemView.bottom.toFloat(), paint
                        )
                    }
                    viewHolder.itemView.translationX = dX


                } else {
                    super.onChildDraw(
                        canvas,
                        recyclerView,
                        viewHolder,
                        dX,
                        dY,
                        actionState,
                        isCurrentlyActive
                    )
                }
            }


        }

        val itemTouchHelper = ItemTouchHelper(simpleItemTouchCallback)
        itemTouchHelper.attachToRecyclerView(todoRv)
    }




















    private fun updateDate3(currDate:String):Long {
        // ADD THIS FUNCTION TO A CLASS
        var myCalendar:Calendar
        var finalDate:Long
        val months = arrayListOf("Jan", "Feb", "Mar", "Apr", "May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
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
        return finalDate
    }


    private fun updateTime3(currTime:String):Long {
        // ADD THIS FUNCTION TO A CLASS

        //Mon, 5 Jan 2020

        var myCalendar:Calendar
        var finalTime:Long
        myCalendar = Calendar.getInstance()
        var n = currTime.length
//        Log.e("len",n.toString())
        if(n==8){
            var am_pm = currTime.substring(6,8);
            if(am_pm=="am")
                myCalendar.set(Calendar.AM_PM, Calendar.AM)
            else
                myCalendar.set(Calendar.AM_PM, Calendar.PM)
            myCalendar.set(Calendar.MINUTE,currTime.substring(3,5).toInt())
            myCalendar.set(Calendar.HOUR,currTime.substring(0,2).toInt())
        }
        else{
            var am_pm = currTime.substring(5,7);
            if(am_pm=="am")
                myCalendar.set(Calendar.AM_PM, Calendar.AM)
            else
                myCalendar.set(Calendar.AM_PM, Calendar.PM)
            myCalendar.set(Calendar.MINUTE,currTime.substring(2,4).toInt())
            myCalendar.set(Calendar.HOUR,currTime.substring(0,1).toInt())
        }
        val myformat = "h:mm a"
        val sdf = SimpleDateFormat(myformat)
        finalTime = myCalendar.time.time
        Log.e("time 1",finalTime.toString())
        Log.e("time 2",System.currentTimeMillis().toString())
        return finalTime

    }

}