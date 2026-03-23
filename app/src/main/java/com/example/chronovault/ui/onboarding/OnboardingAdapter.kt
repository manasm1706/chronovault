package com.example.chronovault.ui.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chronovault.R

class OnboardingAdapter(
    private val pages: List<OnboardingPage>
) : RecyclerView.Adapter<OnboardingAdapter.PageViewHolder>() {

    inner class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_onboard_title)
        val desc: TextView = view.findViewById(R.id.tv_onboard_desc)
        val icon: ImageView = view.findViewById(R.id.iv_onboard_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val page = pages[position]
        holder.title.setText(page.titleRes)
        holder.desc.setText(page.descRes)
        holder.icon.setBackgroundResource(page.backgroundRes)
    }

    override fun getItemCount(): Int = pages.size
}

