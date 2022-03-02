package com.example.todolist2

import android.app.Dialog
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist2.databinding.ActivityMainBinding
import com.example.todolist2.databinding.DeleteCategoryLayoutBinding
import com.example.todolist2.databinding.TodoItemBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.todo_item.view.*
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bindingItemTodo: TodoItemBinding
    private lateinit var bindingDeleteCategory: DeleteCategoryLayoutBinding
    var list = arrayListOf<TodoModel>()
    var adapter = TodoAdapter(list)

    private val defaultCategories = listOf("All", "Personal", "Business", "Insurance", "Shopping", "Banking", "Other")
    private val months = listOf("Jan", "Feb", "Mar", "Apr", "May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    private var categories = arrayListOf<String>()

    val db by lazy {
        AppDatabase.getDatabase(this)
    }

    private val db2 by lazy {
        CategoryDatabase.getDatabase(this)
    }

    override fun onStart() {
        super.onStart()
        setTheSpinner()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(R.anim.fadein,R.anim.fadeout)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        bindingItemTodo = TodoItemBinding.inflate(layoutInflater)
        bindingDeleteCategory = DeleteCategoryLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.todoRv.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
        binding.todoRv.scheduleLayoutAnimation()
        initSwipe()
        db.todoDao().getTask().observe(this, Observer {
            list.clear()
            if (!it.isNullOrEmpty())
                list.addAll(it)
            adapter.notifyDataSetChanged()
            if (list.isEmpty()) {
                not_found_textview.visibility = View.VISIBLE
                not_found_anim.visibility = View.VISIBLE
            }
            else {
                not_found_textview.visibility = View.GONE
                not_found_anim.visibility = View.GONE
            }
        })
        setTheSearchBar()
        setTheSpinner()
        binding.toolbarDelete.setOnClickListener {
            deleteCategory()
        }
        binding.viewHistory.setOnClickListener {
            openHistory()
        }
        binding.addTask.setOnClickListener {
            openNewTask()
        }
    }

    private fun setTheSearchBar() {

        // setOnSearchListener is when user clicks on the search icon, it should by default show all the todos
        toolbar_search.setOnSearchClickListener {
            displayTodoByCategory(categories[0])
        }
        toolbar_search.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                toolbar_spinner.visibility = View.GONE
                toolbar_delete.visibility = View.GONE
                toolbar_search.maxWidth = 1000
                toolbar.setBackgroundColor(getColor(R.color.black))

            }
        }
        toolbar_search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                displayTodoByTitle(newText)
                return true
            }


        })
        toolbar_search.setOnCloseListener {
            displayTodoByCategory(categories[toolbar_spinner.selectedItemPosition])
            toolbar_spinner.visibility = View.VISIBLE
            if (toolbar_spinner.selectedItemPosition >= defaultCategories.size) {
                toolbar_delete.visibility = View.VISIBLE
            }
            toolbar.setBackgroundColor(getColor(R.color.gray_toolbar))
            false
        }
    }

    private fun deleteCategory(){
        val cat = toolbar_spinner.selectedItem as String
        val dialog = Dialog(this)
        dialog.setContentView(bindingDeleteCategory.root)
        val w = resources.displayMetrics.widthPixels*0.9   // to occupy 90% of screen's width
        dialog.window?.setLayout(w.toInt(),ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
        bindingDeleteCategory.deleteCategory.text = String.format(getString(R.string.delete_category_text),cat)
        bindingDeleteCategory.cancelButtonDelete.setOnClickListener {
            dialog.dismiss()
        }
        bindingDeleteCategory.yesButtonDelete.setOnClickListener {
            val todosToBeDeleted = ArrayList<Long>()
            runBlocking {
                launch(Dispatchers.IO) {
                    for (x in db.todoDao().getAllTasks()){
                        if(x.category == cat)
                            todosToBeDeleted.add(x.id)
                    }

                }
            }
            runBlocking {
                launch(Dispatchers.IO) {
                    for(x in todosToBeDeleted)
                        db.todoDao().deleteTask(x)
                }
            }
            runBlocking {
                launch(Dispatchers.IO) {
                    db2.categoryDao().deleteCategory(cat)
                }
            }
            val spinnerAdapter = toolbar_spinner.adapter as ArrayAdapter<String>
            spinnerAdapter.remove(cat)
            spinnerAdapter.notifyDataSetChanged()
            displayTodoByCategory(categories[spinnerAdapter.count-1])
            dialog.dismiss()
        }
    }

    private fun setTheSpinner() {

        val databaseLabels = mutableListOf<String>()
        databaseLabels.addAll(defaultCategories)
        runBlocking {
            launch(Dispatchers.IO) {
                databaseLabels.addAll(db2.categoryDao().getCategories2())
            }
        }
        categories = databaseLabels as ArrayList<String>
        val spinnerAdapter:ArrayAdapter<String> = object: ArrayAdapter<String>(this, R.layout.spinner_item_layout_main, categories){
            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val v = super.getDropDownView(position, convertView, parent)
                val selectedCategory = v as TextView
                if (position == toolbar_spinner.selectedItemPosition){
                    v.setBackgroundColor(getColor(R.color.white))
                    selectedCategory.setTextColor(Color.BLACK)
                }

                else {
                    v.setBackgroundColor(getColor(R.color.gray))
                    selectedCategory.setTextColor(Color.WHITE)
                }
                return v
            }
        }
        toolbar_spinner.adapter = spinnerAdapter

        toolbar_spinner.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {    // idk why tf this has to be done like this
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                displayTodoByCategory(categories[position])
                if(position >= defaultCategories.size){
                    toolbar_delete.visibility = View.VISIBLE
                }
                else{
                    toolbar_delete.visibility = View.GONE
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {    // is this ever called ????

            }
        }

    }

    fun displayTodoByTitle(newText: String = "") {
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
            if (list.isEmpty()) {
                not_found_textview.visibility = View.VISIBLE
                not_found_anim.visibility = View.VISIBLE
            }
            else {
                not_found_textview.visibility = View.GONE
                not_found_anim.visibility = View.GONE
            }
        })
    }

    fun displayTodoByCategory(newText: String) {
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
            if (list.isEmpty()) {
                not_found_textview.visibility = View.VISIBLE
                not_found_anim.visibility = View.VISIBLE
            }
            else {
                not_found_textview.visibility = View.GONE
                not_found_anim.visibility = View.GONE
            }
        })
    }

    private fun openNewTask() {
        startActivity(Intent(this,TaskActivity::class.java))

    }

    private fun openHistory() {
        startActivity(Intent(this,HistoryActivity::class.java))
    }


















    private fun initSwipe() {
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

                // I need the id (Primary Key) of the to-do because in case user decides to undo the delete operation,
                // the Primary Key is needed to restore the to-do in the same place as before

                val id = adapter.getItemId(position)

                // saving the view to-be-deleted in a variable
                val restoredView = viewHolder.itemView

                // making an object of the to-do item (ToDoModel) about to be deleted
                // Here I need to pass the primary key (ie id) so that item is restored in the same position in the recyclerview
                val restoredModel = TodoModel(restoredView.txtShowTitle.text.toString(),
                    restoredView.txtShowTask.text.toString(),
                    restoredView.txtShowCategory.text.toString(),
                    updateDate3(restoredView.txtShowDate.text.toString()),
                    updateTime3(restoredView.txtShowTime.text.toString()),0,id)
                if (direction == ItemTouchHelper.LEFT) {

                    GlobalScope.launch(Dispatchers.IO) {
                        db.todoDao().deleteTask(adapter.getItemId(position))
                    }




                    val snack = Snackbar.make(toolbar,"To Do deleted",Snackbar.LENGTH_SHORT)

                    // if user decides to restore the to-do
                    snack.setAction("UNDO") {

                        // insert the deleted to-do as a new to-do, passing the primary key of the former to latter
                        GlobalScope.launch(Dispatchers.Main) {
                            val id2 = withContext(Dispatchers.IO) {
                                return@withContext db.todoDao().insertTask(
                                    restoredModel
                                )
                            }
                        }

                        val snack2 = Snackbar.make(toolbar, "To Do restored", Snackbar.LENGTH_SHORT)
                        snack2.show()
                    }
                    snack.setActionTextColor(getColor(R.color.green))
                    snack.show()
                } else if (direction == ItemTouchHelper.RIGHT) {
                    GlobalScope.launch(Dispatchers.IO) {
                        db.todoDao().finishTask(adapter.getItemId(position))
                    }
                    Snackbar.make(toolbar,"To Do finished",Snackbar.LENGTH_SHORT).show()
                }
                displayTodoByCategory(restoredView.txtShowCategory.text.toString())
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
        val finalDate:Long
        //Mon, 5 Jan 2020
        val myCalendar = Calendar.getInstance()
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
        finalDate = myCalendar.time.time
        return finalDate
    }


    private fun updateTime3(currTime:String):Long {
        // ADD THIS FUNCTION TO A CLASS

        //Mon, 5 Jan 2020

        val finalTime:Long
        val myCalendar = Calendar.getInstance()
        val n = currTime.length
        if(n==8){
            val format24Hr = currTime.substring(6,8)
            if(format24Hr == "am")
                myCalendar.set(Calendar.AM_PM, Calendar.AM)
            else
                myCalendar.set(Calendar.AM_PM, Calendar.PM)
            myCalendar.set(Calendar.MINUTE,currTime.substring(3,5).toInt())
            myCalendar.set(Calendar.HOUR,currTime.substring(0,2).toInt())
        }
        else {
            val format24Hr = currTime.substring(5,7)
            if(format24Hr == "am")
                myCalendar.set(Calendar.AM_PM, Calendar.AM)
            else
                myCalendar.set(Calendar.AM_PM, Calendar.PM)
            myCalendar.set(Calendar.MINUTE,currTime.substring(2,4).toInt())
            myCalendar.set(Calendar.HOUR,currTime.substring(0,1).toInt())
        }
        finalTime = myCalendar.time.time
        return finalTime

    }

}