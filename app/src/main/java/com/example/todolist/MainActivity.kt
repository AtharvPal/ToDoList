package com.example.todolist2

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_task.*
import kotlinx.android.synthetic.main.item_todo.*
import kotlinx.android.synthetic.main.item_todo.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    var list = arrayListOf<TodoModel>()
    var adapter = TodoAdapter(list)

    private val labels =
        arrayListOf("All", "Personal", "Business", "Insurance", "Shopping", "Banking", "Other")

    val db by lazy {
        AppDatabase.getDatabase(this)
    }

    override fun onPause() {
        overridePendingTransition(R.anim.fadein,R.anim.fadeout)
        super.onPause()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(R.anim.fadein,R.anim.fadeout)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        todoRv.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        initSwipe()



        db.todoDao().getTask().observe(this, Observer {
            list.clear()
            if (!it.isNullOrEmpty())
                list.addAll(it)
            adapter.notifyDataSetChanged()
            if (list.isEmpty())
                emptyList.visibility = View.VISIBLE
            else
                emptyList.visibility = View.GONE
        })


    }




    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val item = menu.findItem(R.id.search)
        val searchView = item.actionView as SearchView
        val item2 = menu.findItem(R.id.categories)
        val spinner = item2.actionView as Spinner
        val adapter = ArrayAdapter<String>(this, R.layout.spinner_item_layout_main, labels)
        spinner.adapter = adapter
        searchView.maxWidth = 10000
        searchView.queryHint = "Enter a title to search..."


//        item.setOnActionExpandListener(object :MenuItem.OnActionExpandListener{
//            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
//                displayTodo()
//                Toast.makeText(this@MainActivity,"first",Toast.LENGTH_SHORT).show()
//                return true
//            }
//
//            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
//                displayTodo()
//                Toast.makeText(this@MainActivity,"second",Toast.LENGTH_SHORT).show()
//                return true
//            }
//
//
//        })

        searchView.setOnQueryTextFocusChangeListener(object:View.OnFocusChangeListener{
            override fun onFocusChange(v: View?, hasFocus: Boolean) {
                if(hasFocus==true){
                    Log.e("search","done 1")
                    spinner.visibility = View.GONE

                }
                else {
                    Log.e("search", "done 2")
                    spinner.visibility = View.VISIBLE
                }
            }
        })
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Toast.makeText(this@MainActivity, "thir", Toast.LENGTH_SHORT).show()
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                displayTodo(newText)
                return true
            }


        })
        searchView.setOnCloseListener(object : SearchView.OnCloseListener {
            override fun onClose(): Boolean {
                displayTodo()
                return false
            }

        })





        spinner.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {    // idk why tf this has to be done like this
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                displayTodo2(labels[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>) {    // is this ever called ????
                Toast.makeText(this@MainActivity, "nothing", Toast.LENGTH_SHORT).show()
            }
        }
        return super.onCreateOptionsMenu(menu)
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
                emptyList.visibility = View.VISIBLE
            else
                emptyList.visibility = View.GONE
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
                            todo.category.contains(newText, true)
                        }
                    )
                }
                adapter.notifyDataSetChanged()
            }
            if (list.isEmpty())
                emptyList.visibility = View.VISIBLE
            else
                emptyList.visibility = View.GONE
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
//                        adapter.notifyDataSetChanged()

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

                        canvas.drawRect(
                            itemView.left.toFloat(), itemView.top.toFloat(),
                            itemView.left.toFloat() + dX, itemView.bottom.toFloat(), paint
                        )

                        canvas.drawBitmap(
                            icon,
                            itemView.left.toFloat(),
                            itemView.top.toFloat() + (itemView.bottom.toFloat() - itemView.top.toFloat() - icon.height.toFloat()) / 2,
                            paint
                        )


                    } else {
                        icon = BitmapFactory.decodeResource(resources, R.mipmap.ic_delete_white_png)

                        paint.color = Color.parseColor("#D32F2F")

                        canvas.drawRect(
                            itemView.right.toFloat() + dX, itemView.top.toFloat(),
                            itemView.right.toFloat(), itemView.bottom.toFloat(), paint
                        )

                        canvas.drawBitmap(
                            icon,
                            itemView.right.toFloat() - icon.width,
                            itemView.top.toFloat() + (itemView.bottom.toFloat() - itemView.top.toFloat() - icon.height.toFloat()) / 2,
                            paint
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
        return finalTime

    }


}