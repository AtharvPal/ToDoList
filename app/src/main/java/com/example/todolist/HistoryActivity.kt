package com.example.todolist2

import android.graphics.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_history.*
import kotlinx.android.synthetic.main.activity_history.todoRv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    var list = arrayListOf<TodoModel>()
    var adapter = TodoAdapter(list)

    val db by lazy {
        AppDatabase.getDatabase(this)
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        todoRv.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = this@HistoryActivity.adapter
        }

        db.todoDao().getFinishedTasks().observe(this, Observer {
            list.clear()
            if (!it.isNullOrEmpty())
                list.addAll(it)
            adapter.notifyDataSetChanged()
            if (list.isEmpty()) {
                emptyListHistory.visibility = View.VISIBLE
                not_found_anim_history.visibility = View.VISIBLE
            }
            else {
                emptyListHistory.visibility = View.GONE
                not_found_anim_history.visibility = View.GONE
            }
        })

        initSwipe()
    }


    fun finishHistory(view:View){
        finish()
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
//                Log.e("gg",position.toString())




                if (direction == ItemTouchHelper.LEFT) {

                    GlobalScope.launch(Dispatchers.IO) {
                        db.todoDao().deleteTask(adapter.getItemId(position))
                    }
                    val snack = Snackbar.make(historyToolbar,"To Do deleted", Snackbar.LENGTH_SHORT)
                    adapter.notifyDataSetChanged()
                    snack.show()
                }
                else if (direction == ItemTouchHelper.RIGHT) {

                        GlobalScope.launch(Dispatchers.IO) {
                            db.todoDao().unfinishTask(adapter.getItemId(position))
                        }
                        val snack = Snackbar.make(historyToolbar,"To Do set as unfinished", Snackbar.LENGTH_SHORT)
                        adapter.notifyDataSetChanged()
                        snack.show()
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


                    } else if(dX<0){
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
                    else{
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
}