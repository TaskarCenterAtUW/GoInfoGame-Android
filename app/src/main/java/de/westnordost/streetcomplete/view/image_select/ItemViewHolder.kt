package de.westnordost.streetcomplete.view.image_select

import android.app.Dialog
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.ui.res.colorResource
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isGone
import androidx.recyclerview.widget.RecyclerView
import com.github.chrisbanes.photoview.PhotoView
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.view.ImageUrl
import de.westnordost.streetcomplete.view.setImage
import de.westnordost.streetcomplete.view.setText

class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val imageView: ImageView? = itemView.findViewById(R.id.imageView)
    private val textView: TextView? = itemView.findViewById(R.id.textView)
    private val descriptionView: TextView? = itemView.findViewById(R.id.descriptionView)
    private val dropDownArrowImageView: ImageView? = itemView.findViewById(R.id.dropDownArrowImageView)

    var isSelected: Boolean
        get() = itemView.isSelected
        set(value) { itemView.isSelected = value }

    var isGroupExpanded: Boolean = false
        set(value) {
            field = value
            dropDownArrowImageView?.rotation = if (value) 90f else 0f
        }

    var onClickListener: ((index: Int) -> Unit)? = null
        set(value) {
            field = value
            if (value == null) {
                itemView.setOnClickListener(null)
            } else {
                itemView.setOnClickListener {
                    val index = adapterPosition
                    if (index != RecyclerView.NO_POSITION) value.invoke(index)
                }
            }
        }

    fun bind(item: DisplayItem<*>) {
        imageView?.setImage(item.image)
        textView?.setText(item.title)
        if (item.image == null || (item.image is ImageUrl && ((item.image as ImageUrl).url.isNullOrEmpty()))) {
            textView?.setTextColor(textView.context.resources.getColor(R.color.text))
            textView?.setShadowLayer(0f, 0f, 0f, textView.context.resources.getColor(android.R.color.transparent))
        }else{
            textView?.setTextColor(textView.context.resources.getColor(R.color.button_white))
            textView?.setShadowLayer(0f, 0f, 0f, textView.context.resources.getColor(android.R.color.black))
        }
        descriptionView?.setText(item.description)
        descriptionView?.isGone = item.description == null
        itemView.setOnLongClickListener {
            val dialog = Dialog(it.context)
            dialog.setContentView(R.layout.dialog_full_image)

            dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            dialog.window?.setDimAmount(0.7f) // controls dim background

            val fullImageView = dialog.findViewById<PhotoView>(R.id.fullImage)
            fullImageView.setImageDrawable(imageView?.drawable)

            fullImageView.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
            false
        }
    }
}
