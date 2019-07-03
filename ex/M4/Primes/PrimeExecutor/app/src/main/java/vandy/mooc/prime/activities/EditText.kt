package vandy.mooc.prime.activities

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.widget.EditText

/**
 * Add EditText extension that enables the widget to clear
 * input with right button
 *
 * @param onIsNotEmpty callback invoked when input is completed and and is not empty
 * @param onCanceled callback which invoked when cancel button is clicked.
 * @param clearDrawable right drawable which is used as cancel/clear button
 */
fun EditText.makeClearableEditText(
        onIsNotEmpty: (() -> Unit)?,
        onCanceled: (() -> Unit)?,
        clearDrawable: Drawable
) {
    val updateRightDrawable = {
        setCompoundDrawables(
                null,
                null,
                if (text.isNotEmpty()) clearDrawable else null,
                null)
    }

    updateRightDrawable()

    afterTextChanged {
        if (it.isNotEmpty()) {
            onIsNotEmpty?.invoke()
        }

        updateRightDrawable()
    }

    onRightDrawableClicked {
        text.clear()
        setCompoundDrawables(null, null, null, null)
        onCanceled?.invoke()
        requestFocus()
    }
}

/**
 *
 * Calculate right compound drawable and in case it exists calls
 * @see EditText.makeClearableEditText
 *
 * Arguments:
 *  @param onIsNotEmpty - callback which is invoked when input is completed and is not empty. Is good for clearing error
 *  @param onCanceled - callbacks which is invoked when cancel button is clicked and input is cleared
 */
fun EditText.makeClearableEditText(onIsNotEmpty: (() -> Unit)?, onCanceled: (() -> Unit)?) {
    compoundDrawables[COMPOUND_DRAWABLE_RIGHT_INDEX]?.let { clearDrawable ->
        makeClearableEditText(onIsNotEmpty, onCanceled, clearDrawable)
    }
}

private const val COMPOUND_DRAWABLE_RIGHT_INDEX = 2

/**
 * Based on View.OnTouchListener. Be careful EditText replaces old View.OnTouchListener when setting new one
 */
@SuppressLint("ClickableViewAccessibility")
private fun EditText.onRightDrawableClicked(onClicked: (view: EditText) -> Unit) {
    setOnTouchListener { v, event ->
        var hasConsumed = false
        if (v is EditText) {
            if (event.x >= v.width - v.totalPaddingRight) {
                if (event.action == MotionEvent.ACTION_UP) {
                    onClicked(this)
                }
                hasConsumed = true
            }
        }
        hasConsumed
    }
}

private fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }
    })
}
