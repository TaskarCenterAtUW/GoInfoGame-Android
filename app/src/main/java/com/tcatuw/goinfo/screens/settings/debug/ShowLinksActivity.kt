package com.tcatuw.goinfo.screens.settings.debug

import android.os.Bundle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.data.user.achievements.Link
import com.tcatuw.goinfo.databinding.FragmentShowLinksBinding
import com.tcatuw.goinfo.screens.BaseActivity
import com.tcatuw.goinfo.screens.user.links.GroupedLinksAdapter
import com.tcatuw.goinfo.util.ktx.openUri
import com.tcatuw.goinfo.util.viewBinding
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

/** activity only used in debug, to show all achievement links */
class ShowLinksActivity : BaseActivity() {
    private val links: List<Link> by inject(named("Links"))
    private val binding by viewBinding(FragmentShowLinksBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_show_links)
        binding.toolbarLayout.toolbar.navigationIcon = getDrawable(R.drawable.ic_close_24dp)
        binding.toolbarLayout.toolbar.setNavigationOnClickListener { onBackPressed() }
        binding.toolbarLayout.toolbar.title = "Show Achievement Links"

        binding.linksList.apply {
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            layoutManager = LinearLayoutManager(context)
            adapter = GroupedLinksAdapter(links, ::openUri)
        }
    }
}
