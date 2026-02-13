package com.inc.codemy

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.BaseAdapter

class LeagueAdapter(private val context: Context, private val users: List<LeagueUser>) : BaseAdapter() {

    override fun getCount() = users.size
    override fun getItem(position: Int) = users[position]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_league_user, parent, false)

        val rank = view.findViewById<TextView>(R.id.userRank)
        val name = view.findViewById<TextView>(R.id.userName)
        val xpWeekly = view.findViewById<TextView>(R.id.userXPWeekly)
        val avatar = view.findViewById<ImageView>(R.id.userAvatar)

        val user = users[position]

        rank.text = (position + 1).toString()
        name.text = user.name
        xpWeekly.text = "${user.weeklyXP} XP за 7 дней"
        avatar.setImageResource(R.drawable.ic_avatar_placeholder) // пока используем одну иконку

        return view
    }
}
