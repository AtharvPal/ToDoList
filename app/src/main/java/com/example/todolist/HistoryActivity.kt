package com.example.todolist2

import android.graphics.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist2.databinding.ActivityHistoryBinding
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_history.*
import kotlinx.coroutines.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding:ActivityHistoryBinding
    var list = arrayListOf<TodoModel>()
    var adapter = TodoAdapter(list)

    private val todoDatabase by lazy {
        AppDatabase.getDatabase(this)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch(Dispatchers.IO) {
            list = todoDatabase.todoDao().getAllFinishedTasks() as ArrayList<TodoModel>
            adapter = TodoAdapter(list)
            binding.todoRv.apply {
                layoutManager = LinearLayoutManager(this@HistoryActivity)
                adapter = this@HistoryActivity.adapter
            }
        }




        todoDatabase.todoDao().getFinishedTasks().observe(this) {
            list.clear()
            if (!it.isNullOrEmpty())
                list.addAll(it)
            if (list.isEmpty()) {
                binding.emptyListHistory.visibility = View.VISIBLE
                binding.notFoundAnimHistory.visibility = View.VISIBLE
            } else {
                binding.emptyListHistory.visibility = View.GONE
                binding.notFoundAnimHistory.visibility = View.GONE
            }
        }
        initSwipe()

        binding.backHistory.setOnClickListener {
            finish()
        }
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

                if (direction == ItemTouchHelper.LEFT) {

                    lifecycleScope.launch(Dispatchers.IO) {
                        todoDatabase.todoDao().deleteTask(adapter.getItemId(position))
                        list.removeAt(position)
                        withContext(Dispatchers.Main) {
                            adapter.notifyItemRemoved(position)
                        }
                    }
                    val snack = Snackbar.make(historyToolbar,"To Do deleted", Snackbar.LENGTH_SHORT)
                    snack.show()
                }
                else if (direction == ItemTouchHelper.RIGHT) {

                    lifecycleScope.launch(Dispatchers.IO) {
                        todoDatabase.todoDao().unfinishTask(adapter.getItemId(position))
                        withContext (Dispatchers.Main) {
                            list.removeAt(position)
                            adapter.notifyItemRemoved(position)
                        }
                    }
                    val snack = Snackbar.make(historyToolbar,"To Do set as unfinished", Snackbar.LENGTH_SHORT)
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
                    val itemView = viewHolder.itemView as MaterialCardView

                    val paint = Paint()
                    var icon: Bitmap

                    if (dX > 0) {
                        itemView.radius = 20F
                        icon = BitmapFactory.decodeResource(resources, R.mipmap.ic_check_white_png)
                        icon = Bitmap.createScaledBitmap(icon, (4*icon.width)/3, (4*icon.height)/3,false)
                        paint.color = Color.parseColor("#388E3C")

                        canvas.drawRect(
                            itemView.left.toFloat(), itemView.top.toFloat(),
                            itemView.right.toFloat(), itemView.bottom.toFloat(), paint
                        )

                        canvas.drawBitmap(
                            icon,
                            itemView.left.toFloat() + icon.width / 3,
                            itemView.top.toFloat() + (itemView.bottom.toFloat() - itemView.top.toFloat() - icon.height.toFloat()) / 2,
                            paint
                        )


                    } else if(dX < 0){
                        itemView.radius = 20F
                        icon = BitmapFactory.decodeResource(resources, R.mipmap.ic_delete_white_png)
                        icon = Bitmap.createScaledBitmap(icon, (4*icon.width)/3, (4*icon.height)/3,false)
                        paint.color = Color.parseColor("#D32F2F")

                        canvas.drawRect(
                            itemView.left.toFloat(), itemView.top.toFloat(),
                            itemView.right.toFloat(), itemView.bottom.toFloat(), paint
                        )

                        canvas.drawBitmap(
                            icon,
                            itemView.right.toFloat() - icon.width - icon.width / 3,
                            itemView.top.toFloat() + (itemView.bottom.toFloat() - itemView.top.toFloat() - icon.height.toFloat()) / 2,
                            paint
                        )
                    }
                    else{
                        itemView.radius = 0F
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