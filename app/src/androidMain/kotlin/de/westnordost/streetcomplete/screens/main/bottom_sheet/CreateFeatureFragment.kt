package de.westnordost.streetcomplete.screens.main.bottom_sheet

import android.content.res.Configuration
import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.BounceInterpolator
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import androidx.core.graphics.toPointF
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.load
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.osm.edits.ElementEditsController
import de.westnordost.streetcomplete.data.osm.edits.create.CreateNodeAction
import de.westnordost.streetcomplete.data.osm.edits.create_feature.FeaturePhotosController
import de.westnordost.streetcomplete.data.osm.geometry.ElementPointGeometry
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.databinding.CellFeaturePresetBinding
import de.westnordost.streetcomplete.databinding.FormCreateFeatureBinding
import de.westnordost.streetcomplete.databinding.FragmentCreateFeatureBinding
import de.westnordost.streetcomplete.quests.create_feature.AddFeaturePreset
import de.westnordost.streetcomplete.quests.create_feature.CustomIconCache
import de.westnordost.streetcomplete.quests.note_discussion.AttachPhotoFragment
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.CustomIcon
import de.westnordost.streetcomplete.quests.sidewalk_long_form.data.FeaturePreset
import de.westnordost.streetcomplete.util.ktx.getLocationInWindow
import de.westnordost.streetcomplete.util.ktx.viewLifecycleScope
import de.westnordost.streetcomplete.util.viewBinding
import de.westnordost.streetcomplete.view.presetIconIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.android.ext.android.inject

/** Bottom sheet fragment with which the user can add a new feature (a new OSM node) from the
 *  workspace-defined feature presets, in two steps: pick a preset from a grid, then optionally
 *  attach photos and confirm. The map stays interactive the whole time - like when creating a
 *  note, the node's position is wherever the centered pin points when the user confirms. */
class CreateFeatureFragment : AbstractBottomSheetFragment() {

    private val elementEditsController: ElementEditsController by inject()
    private val featurePhotosController: FeaturePhotosController by inject()
    private val customIconCache: CustomIconCache by inject()

    private var _binding: FragmentCreateFeatureBinding? = null
    private val binding: FragmentCreateFeatureBinding get() = _binding!!

    private val bottomSheetBinding get() = binding.questAnswerLayout

    override val bottomSheetContainer get() = bottomSheetBinding.bottomSheetContainer
    override val bottomSheet get() = bottomSheetBinding.bottomSheet
    override val scrollViewChild get() = bottomSheetBinding.scrollViewChild
    override val bottomSheetTitle get() = bottomSheetBinding.speechBubbleTitleContainer
    override val bottomSheetContent get() = bottomSheetBinding.speechbubbleContentContainer
    override val floatingBottomView: View? get() = null
    override val defaultExpanded = false

    private val contentBinding by viewBinding(FormCreateFeatureBinding::bind, R.id.content)

    private var presets: List<FeaturePreset> = emptyList()
    private var customIcons: List<CustomIcon> = emptyList()
    private var selectedPreset: FeaturePreset? = null
    private var isSubmitting = false

    private val attachPhotoFragment: AttachPhotoFragment?
        get() = childFragmentManager.findFragmentById(R.id.attachPhotoFragment) as? AttachPhotoFragment

    // the default Coil loader has no SVG support; custom icons may be SVGs
    private val iconImageLoader by lazy {
        ImageLoader.Builder(requireContext())
            .components { add(SvgDecoder.Factory()) }
            .build()
    }

    interface Listener {
        fun getMapPositionAt(screenPos: PointF): LatLon?
        fun onCreatedFeature(position: LatLon)
        fun closeCreateFeature()
    }
    private val listener: Listener? get() = parentFragment as? Listener ?: activity as? Listener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presets = Json.decodeFromString(requireArguments().getString(ARG_PRESETS)!!)
        customIcons = Json.decodeFromString(requireArguments().getString(ARG_CUSTOM_ICONS)!!)
        selectedPreset = savedInstanceState?.getString(STATE_SELECTED_PRESET)?.let { Json.decodeFromString(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateFeatureBinding.inflate(inflater, container, false)
        inflater.inflate(R.layout.form_create_feature, bottomSheetBinding.content)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomSheetBinding.buttonPanel.isGone = true
        bottomSheetBinding.okButtonContainer.isGone = true

        if (savedInstanceState == null) {
            binding.markerCreateLayout.markerLayoutContainer.startAnimation(createFallDownAnimation())
        }

        bottomSheetBinding.hideButton.visibility = View.GONE
        bottomSheetBinding.closeButton.setOnClickListener {
            onClickCloseAll()
        }

        contentBinding.presetsList.layoutManager = GridLayoutManager(requireContext(), GRID_COLUMNS)
        contentBinding.presetsList.adapter = FeaturePresetsAdapter(presets, ::bindPresetIcon) {
            selectPreset(it)
        }

        contentBinding.pickerCancelButton.setOnClickListener { onClickCloseAll() }
        contentBinding.detailsCancelButton.setOnClickListener { onClickCloseAll() }
        contentBinding.addFeatureButton.setOnClickListener { onClickAddFeature() }

        updateStep()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedPreset?.let { outState.putString(STATE_SELECTED_PRESET, Json.encodeToString(it)) }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.markerCreateLayout.centeredMarkerLayout.setPadding(
            resources.getDimensionPixelSize(R.dimen.quest_form_leftOffset),
            resources.getDimensionPixelSize(R.dimen.quest_form_topOffset),
            resources.getDimensionPixelSize(R.dimen.quest_form_rightOffset),
            resources.getDimensionPixelSize(R.dimen.quest_form_bottomOffset)
        )
    }

    private fun selectPreset(preset: FeaturePreset) {
        selectedPreset = preset
        updateStep()
    }

    private fun updateStep() {
        val preset = selectedPreset
        if (preset == null) {
            contentBinding.root.displayedChild = 0
            bottomSheetBinding.titleLabel.text = getString(R.string.map_btn_create_node)
        } else {
            contentBinding.root.displayedChild = 1
            val title = getString(R.string.create_feature_new_title, preset.name)
            bottomSheetBinding.titleLabel.text = title
            contentBinding.selectedPresetName.text = title
            bindPresetIcon(contentBinding.selectedPresetIcon, preset.icon)
        }
    }

    /** Resolves a preset's icon: a built-in preset icon (either by its iD-style name or its
     *  drawable resource name, which is what the schema's enum uses), else a workspace-defined
     *  custom icon downloaded once into [CustomIconCache] and served from disk from then on,
     *  else a generic marker. */
    private fun bindPresetIcon(imageView: ImageView, iconName: String?) {
        val resId = iconName?.let { name ->
            presetIconIndex[name]
                ?: resources.getIdentifier(name, "drawable", requireContext().packageName)
                    .takeIf { it != 0 }
        }
        if (resId != null) {
            imageView.setImageResource(resId)
            return
        }
        val url = iconName?.let { name ->
            customIcons.firstOrNull { it.name == name && it.type == "feature-preset" }?.url
        }
        if (url == null) {
            imageView.setImageResource(R.drawable.preset_maki_marker_stroked)
            return
        }

        val cached = customIconCache.getCached(url)
        if (cached != null) {
            imageView.load(cached, iconImageLoader) {
                error(R.drawable.preset_maki_marker_stroked)
            }
            return
        }
        // not downloaded yet: show the fallback while fetching; the tag guards against the view
        // having been recycled and re-bound to another preset by the time the download finishes
        imageView.setImageResource(R.drawable.preset_maki_marker_stroked)
        imageView.tag = url
        viewLifecycleScope.launch {
            val file = customIconCache.getOrDownload(url)
            if (file != null && imageView.tag == url) {
                imageView.load(file, iconImageLoader) {
                    error(R.drawable.preset_maki_marker_stroked)
                }
            }
        }
    }

    /* ------------------------------------- close / back --------------------------------------- */

    /** Back (or the sheet's X / a Cancel button while on the picker) while on the details step
     *  returns to the picker instead of closing; photos already taken are kept. */
    override fun onClickClose(onConfirmed: () -> Unit) {
        if (selectedPreset != null) {
            selectedPreset = null
            updateStep()
        } else {
            super.onClickClose(onConfirmed)
        }
    }

    /** Closes the whole sheet regardless of step (with the discard-confirm dialog if photos
     *  would be lost). */
    private fun onClickCloseAll() {
        super.onClickClose { listener?.closeCreateFeature() }
    }

    override fun isRejectingClose() =
        attachPhotoFragment?.imagePaths?.isNotEmpty() == true ||
            contentBinding.notesInput.text.isNotBlank()

    override fun onDiscard() {
        super.onDiscard()
        attachPhotoFragment?.deleteImages()
        binding.markerCreateLayout.markerLayoutContainer.visibility = View.INVISIBLE
    }

    /* ----------------------------------------- submit ----------------------------------------- */

    private fun onClickAddFeature() {
        val preset = selectedPreset ?: return
        if (isSubmitting) return

        val pinView = binding.markerCreateLayout.pin
        val screenPos = pinView.getLocationInWindow()
        screenPos.offset(pinView.width / 2, pinView.height / 2)

        val statusBarResId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight =
            if (statusBarResId != 0) resources.getDimensionPixelSize(statusBarResId) else 0
        screenPos.offset(0, -statusBarHeight)

        val position = listener?.getMapPositionAt(screenPos.toPointF()) ?: return
        val imagePaths = attachPhotoFragment?.imagePaths.orEmpty()
        val notes = contentBinding.notesInput.text.toString().trim()
        val tags = if (notes.isNotEmpty()) preset.tags + ("ext:notes" to notes) else preset.tags

        isSubmitting = true
        binding.markerCreateLayout.markerLayoutContainer.visibility = View.INVISIBLE

        viewLifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val editId = elementEditsController.add(
                    AddFeaturePreset,
                    ElementPointGeometry(position),
                    "survey",
                    CreateNodeAction(position, tags),
                    isNearUserLocation = true
                )
                // photos are NOT deleted here - they live on disk until the edit syncs, at which
                // point they are uploaded and cleaned up (or deleted with the edit if it is undone)
                if (imagePaths.isNotEmpty()) {
                    featurePhotosController.add(editId, imagePaths)
                }
            }
            listener?.onCreatedFeature(position)
        }
    }

    private fun createFallDownAnimation(): Animation {
        val a = AnimationSet(false)
        a.startOffset = 200

        val ta = TranslateAnimation(0, 0f, 0, 0f, 1, -0.2f, 0, 0f)
        ta.interpolator = BounceInterpolator()
        ta.duration = 400
        a.addAnimation(ta)

        val aa = AlphaAnimation(0f, 1f)
        aa.interpolator = AccelerateInterpolator()
        aa.duration = 200
        a.addAnimation(aa)

        return a
    }

    companion object {
        private const val ARG_PRESETS = "presets"
        private const val ARG_CUSTOM_ICONS = "customIcons"
        private const val STATE_SELECTED_PRESET = "selectedPreset"
        private const val GRID_COLUMNS = 3

        fun create(presets: List<FeaturePreset>, customIcons: List<CustomIcon>) =
            CreateFeatureFragment().also {
                it.arguments = bundleOf(
                    ARG_PRESETS to Json.encodeToString(presets),
                    ARG_CUSTOM_ICONS to Json.encodeToString(customIcons)
                )
            }
    }
}

private class FeaturePresetsAdapter(
    private val presets: List<FeaturePreset>,
    private val bindIcon: (ImageView, String?) -> Unit,
    private val onClick: (FeaturePreset) -> Unit,
) : RecyclerView.Adapter<FeaturePresetsAdapter.ViewHolder>() {

    class ViewHolder(val binding: CellFeaturePresetBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        CellFeaturePresetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = presets.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val preset = presets[position]
        holder.binding.presetName.text = preset.name
        bindIcon(holder.binding.presetIcon, preset.icon)
        holder.binding.root.setOnClickListener { onClick(preset) }
    }
}
