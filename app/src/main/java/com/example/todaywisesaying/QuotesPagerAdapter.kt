package com.example.todaywisesaying

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.todaywisesaying.databinding.ItemQuoteBinding

/**
 * TodayWiseSaying
 * Created by authorName
 * Date: 2022-04-22
 * Time: 오후 5:27
 * */
class QuotesPagerAdapter(
    private val quotes: List<Quote>,
    private val isNameRevealed: Boolean
) : RecyclerView.Adapter<QuotesPagerAdapter.QuotesViewHolder>() {

    inner class QuotesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val quoteTextView: TextView = itemView.findViewById(R.id.quote)
        private val nameTextView: TextView = itemView.findViewById(R.id.name)

        @SuppressLint("SetTextI18n")
        fun bind(quote: Quote, isNameRevealed: Boolean) {
            quoteTextView.text = "\"${quote.quote}\" "

            if(isNameRevealed) {
                nameTextView.text = "- ${quote.name} "
                nameTextView.visibility = View.VISIBLE
            } else {
                nameTextView.visibility = View.GONE
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = QuotesViewHolder(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quote, parent, false)
    )

    override fun onBindViewHolder(holder: QuotesViewHolder, position: Int) {
        // 무한 스와이프를 위해 변수 생성
        val actualPosition = position % quotes.size
        holder.bind(quotes[actualPosition], isNameRevealed)
    }

//    override fun getItemCount() = quotes.size
    // 무한 스와이프를 위해 Count 값을 변경함
    override fun getItemCount() = Int.MAX_VALUE

}