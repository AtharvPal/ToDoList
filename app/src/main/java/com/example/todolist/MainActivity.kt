package com.example.todolist

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
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    var list = arrayListOf<TodoModel>()
    var adapter = TodoAdapter(list)

    private val labels =
        arrayListOf("All", "Personal", "Business", "Insurance", "Shopping", "Banking", "Other")

    val db by lazy {
        AppDatabase.getDatabase(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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
        val adapter = ArrayAdapter<String>(this, R.layout.spinner_item_layout, labels)
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
                Log.e("ok4",adapter.getItemId(position).toString())
                if (direction == ItemTouchHelper.LEFT) {
                    GlobalScope.launch(Dispatchers.IO) {
                        db.todoDao().deleteTask(adapter.getItemId(position))
                    }
                } else if (direction == ItemTouchHelper.RIGHT) {
                    GlobalScope.launch(Dispatchers.IO) {
                        db.todoDao().finishTask(adapter.getItemId(position))
                    }
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
}