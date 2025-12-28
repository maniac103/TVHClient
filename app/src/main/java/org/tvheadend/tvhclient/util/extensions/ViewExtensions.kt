package org.tvheadend.tvhclient.util.extensions

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import org.tvheadend.data.entity.ChannelBaseInterface
import org.tvheadend.data.entity.RecordingInterface
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.util.getIconUrl

fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }
    })
}

fun TextView.applyTextAndAdjustVisibility(value: CharSequence?) {
    text = value
    isVisible = text.isNotEmpty()
}

fun TextView.applyTextAndAdjustVisibility(generator: Context.() -> CharSequence?) {
    text = generator.invoke(context)
    isVisible = text.isNotEmpty()
}

fun TextView.applyText(generator: Context.() -> CharSequence?) {
    text = generator.invoke(context)
}

fun ImageView.applyChannelIcon(channel: ChannelBaseInterface, fallbackText: TextView? = null) {
    applyIcon(channel.icon, channel.name, fallbackText)
}

fun ImageView.applyIcon(icon: String?, name: String? = null, fallbackText: TextView? = null) {
    fallbackText?.text = if (name.isNullOrEmpty()) context.getString(R.string.all_channels) else name
    if (icon.isNullOrEmpty()) {
        isVisible = false
        fallbackText?.isVisible = true
    } else {
        val url = getIconUrl(context, icon)

        Picasso.get().cancelRequest(this)
        Picasso.get()
            .load(url)
            .into(this, object : Callback {
                override fun onSuccess() {
                    isVisible = true
                    fallbackText?.isVisible = false
                }

                override fun onError(e: Exception) {
                    isVisible = false
                    fallbackText?.isVisible = true
                }
            })
    }
}

fun ImageView.applyRecordingStateIcon(recording: RecordingInterface?) {
    val drawableResId = recording?.let {
        when {
            recording.isFailed -> R.drawable.ic_error_small
            recording.isCompleted -> R.drawable.ic_success_small
            recording.isMissed -> R.drawable.ic_error_small
            recording.isRecording -> R.drawable.ic_rec_small
            recording.isScheduled -> R.drawable.ic_schedule_small
            else -> null
        }
    }

    isVisible = drawableResId != null
    drawableResId?.let { setImageResource(it) }
}
