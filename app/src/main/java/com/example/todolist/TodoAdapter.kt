package com.example.todolist

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import kotlinx.android.synthetic.main.activity_task.*
import kotlinx.android.synthetic.main.item_todo.view.*
import java.text.SimpleDateFormat
import java.util.*

class TodoAdapter(val list: List<TodoModel>) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        return TodoViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_todo, parent, false))

    }
    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.bind(list[position],position)
        holder.itemView.share.setOnClickListener {
            Log.e("share","done")
            var info = holder.itemView.context.applicationInfo
            var intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.putExtra(Intent.EXTRA_TEXT,holder.itemView.txtShowTitle.text.toString())
            intent.type = "text/plain"
            holder.itemView.context.startActivity(Intent.createChooser(intent,"Share"))
        }
        holder.itemView.setOnClickListener(object :View.OnClickListener{
            override fun onClick(v: View?) {
                var title = v?.findViewById<TextView>(R.id.txtShowTitle)
                var desc = v?.findViewById<TextView>(R.id.txtShowTask)
                var category = v?.findViewById<TextView>(R.id.txtShowCategory)
                var time = v?.findViewById<TextView>(R.id.txtShowTime)
                var date = v?.findViewById<TextView>(R.id.txtShowDate)

                var intent = Intent(holder.itemView.context,TaskActivity::class.java)

                intent.putExtra("title",title?.text.toString())
                intent.putExtra("desc",desc?.text.toString())
                intent.putExtra("category",category?.text.toString())
                intent.putExtra("date",date?.text.toString())
                intent.putExtra("time",time?.text.toString())

                intent.putExtra("position",getItemId(position).toInt())

                Log.e("ok",getItemId(position).toString())

                holder.itemView.context.startActivity(intent)
            }
        })

    }

    override fun getItemId(position: Int): Long {
        return list[position].id
    }

    class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {


        fun bind(todoModel: TodoModel,pos:Int) {
            with(itemView) {
//                val colors = resources.getIntArray(R.array.random_color)
//                val randomColor = colors[pos%colors.size]
//                viewColorTag.setBackgroundColor(randomColor)
                txtShowTitle.text = todoModel.title
                txtShowTask.text = todoModel.description
                txtShowCategory.text = todoModel.category
                updateTime(todoModel.time)
                updateDate(todoModel.date)

            }

        }

        private fun updateTime(time: Long) {
            //Mon, 5 Jan 2020
            val myformat = "h:mm a"
            val sdf = SimpleDateFormat(myformat)
            itemView.txtShowTime.text = sdf.format(Date(time))

        }

        private fun updateDate(time: Long) {
            //Mon, 5 Jan 2020
            val myformat = "EEE, d MMM yyyy"
            val sdf = SimpleDateFormat(myformat)
            itemView.txtShowDate.text = sdf.format(Date(time))

        }
    }

}