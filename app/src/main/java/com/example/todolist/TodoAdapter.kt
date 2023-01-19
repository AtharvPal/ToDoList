package com.example.todolist2

import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_task.*
import kotlinx.android.synthetic.main.todo_item.view.*
import java.text.SimpleDateFormat
import java.util.*

class TodoAdapter(val list: List<TodoModel>) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        return TodoViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.todo_item, parent, false))

    }
    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.bind(list[position],position)
        holder.itemView.share.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_SEND

            var todoData = ""
            todoData+="Title: "+holder.itemView.txtShowTitle.text.toString()
            if (holder.itemView.txtShowTask.text.isNotBlank())
                todoData+="\nDescription: "+holder.itemView.txtShowTask.text.toString()
            todoData+="\nDate: "+holder.itemView.txtShowDate.text.toString()
            todoData+="\nTime: "+holder.itemView.txtShowTime.text.toString()
            todoData+="\nCategory: "+holder.itemView.txtShowCategory.text.toString()
            
            intent.putExtra(Intent.EXTRA_TEXT,todoData)
            intent.type = "text/plain"
            holder.itemView.context.startActivity(Intent.createChooser(intent,"Share"))
        }
        holder.itemView.setOnClickListener(object :View.OnClickListener{
            override fun onClick(v: View?) {
                val title = v?.findViewById<TextView>(R.id.txtShowTitle)
                val desc = v?.findViewById<TextView>(R.id.txtShowTask)
                val category = v?.findViewById<TextView>(R.id.txtShowCategory)
                val time = v?.findViewById<TextView>(R.id.txtShowTime)
                val date = v?.findViewById<TextView>(R.id.txtShowDate)
                val intent = Intent(holder.itemView.context,TaskActivity::class.java)

                intent.putExtra("title",title?.text.toString())
                intent.putExtra("desc",desc?.text.toString())
                intent.putExtra("category",category?.text.toString())
                intent.putExtra("date",date?.text.toString())
                intent.putExtra("time",time?.text.toString())
                intent.putExtra("position",getItemId(position).toInt())

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

                val myFormat = "EEE, d MMM yyyy h:mm a"
                val sdf = SimpleDateFormat(myFormat)
                val myFormat1 = "EEE, d MMM yyyy"
                val sdf1 = SimpleDateFormat(myFormat1)
                val myFormat2 = "h:mm a"
                val sdf2 = SimpleDateFormat(myFormat2)
                val currentDateTime = sdf.format(Date(System.currentTimeMillis()))
                val combinedDateTime = sdf1.format(todoModel.date)+" "+ sdf2.format(todoModel.time)
                if (sdf.parse(combinedDateTime).before(sdf.parse(currentDateTime))) {
                    txtShowDate.setTextColor(ContextCompat.getColor(itemView.context,R.color.red_delete))  // complex way :(
                    txtShowTime.setTextColor(ContextCompat.getColor(itemView.context,R.color.red_delete))
                    Log.e("compareDates", " $combinedDateTime is before $currentDateTime")
                }
                else{
                    txtShowDate.setTextColor(Color.WHITE)
                    txtShowTime.setTextColor(Color.WHITE)
                    Log.e("compareDates"," $combinedDateTime is equal to or after $currentDateTime")
                }
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