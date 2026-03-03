package com.inc.codemy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.BaseAdapter

class LeagueAdapter(
    private val users: List<LeagueUser>
) : BaseAdapter() {

    override fun getCount() = users.size
    override fun getItem(position: Int) = users[position]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(parent?.context).inflate(R.layout.item_league_user, parent, false)

        val rank = view.findViewById<TextView>(R.id.userRank)
        val name = view.findViewById<TextView>(R.id.userName)
        val xpWeekly = view.findViewById<TextView>(R.id.userXPWeekly)
        val avatar = view.findViewById<ImageView>(R.id.userAvatar)
        val medal = view.findViewById<TextView>(R.id.userMedal)

        val user = users[position]

        rank.text = user.rank.toString()
        name.text = user.name
        xpWeekly.text = "${user.weeklyXP} XP"

        // Медаль для топ-3
        when (user.rank) {
            1 -> {
                medal.visibility = View.VISIBLE
                medal.text = "🥇"
            }
            2 -> {
                medal.visibility = View.VISIBLE
                medal.text = "🥈"
            }
            3 -> {
                medal.visibility = View.VISIBLE
                medal.text = "🥉"
            }
            else -> {
                medal.visibility = View.GONE
            }
        }

        // Выделяем текущего пользователя
        if (user.isCurrentUser) {
            view.setBackgroundResource(R.drawable.bg_league_item_current)
        } else {
            view.setBackgroundResource(R.drawable.bg_league_item)
        }

        return view
    }
}
