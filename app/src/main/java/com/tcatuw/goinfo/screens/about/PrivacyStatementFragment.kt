package com.tcatuw.goinfo.screens.about

import android.os.Bundle
import android.view.View
import com.tcatuw.goinfo.R
import com.tcatuw.goinfo.databinding.FragmentShowHtmlBinding
import com.tcatuw.goinfo.screens.HasTitle
import com.tcatuw.goinfo.screens.TwoPaneDetailFragment
import com.tcatuw.goinfo.screens.main.map.VectorTileProvider
import com.tcatuw.goinfo.util.viewBinding
import com.tcatuw.goinfo.view.setHtml
import org.koin.android.ext.android.inject

/** Shows the privacy statement */
class PrivacyStatementFragment : TwoPaneDetailFragment(R.layout.fragment_show_html), HasTitle {

    private val vectorTileProvider: VectorTileProvider by inject()

    private val binding by viewBinding(FragmentShowHtmlBinding::bind)

    override val title: String get() = getString(R.string.about_title_privacy_statement)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.textView.setHtml(
            getString(R.string.privacy_html) +
            getString(R.string.privacy_html_tileserver2, vectorTileProvider.title, vectorTileProvider.privacyStatementLink) +
            getString(R.string.privacy_html_statistics) +
            getString(R.string.privacy_html_third_party_quest_sources) +
            getString(R.string.privacy_html_image_upload2))
    }
}
