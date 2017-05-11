package edu.vandy.tasktesterframeworklib.view.interfaces;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import edu.vandy.tasktesterframeworklib.utils.UiUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Interface for the Presenter layer to interact with the Count EditText.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public interface CountUpdateInterface {
    /**
     * This mechanism exists to store the reference to the EditText
     * that is initialized after the fact by the 'initializePresenterInterface'
     * method. This is done to use this Interface with Defaults as a
     * means to achieve multiple inheritance.
     */
    ArrayList<WeakReference<EditText>> editTextRef =
            new ArrayList<>(1);

    /**
     * Initialize the Counter instance from the UI to this Interface
     * so that this Interface's default methods can operate properly.
     */
    default void initializeCounter(EditText editText, ViewInterface viewInterface) {
        if (editTextRef.size() != 0)
            editTextRef.remove(0);

        editTextRef.add(0,
                new WeakReference<>(editText));

        editText.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {

                if (s.length() == 0) {
                    UiUtils.hideFab(viewInterface.getFABStartOrStop());
                } else {
                    UiUtils.showFab(viewInterface.getFABStartOrStop());
                }
            }

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {

            }
        });
    }

    /**
     * Helper method to localize access of the EditText from its
     * storage and to give runtime exception if EditText was not
     * properly initialized.
     */
    default EditText getCountEditText() {
        EditText count = editTextRef.get(0).get();
        if (count == null)
            throw new RuntimeException("Count EditText Uninitialized");

        return count;
    }

    /**
     * Set the text on the EditText for Count
     */
    default void SetEditText(String text) {
        getCountEditText().setText(text);
    }

    /**
     * Request focus onto the EditText.
     */
    default void CountEditTextRequestFocus() {
        getCountEditText().requestFocus();
    }

    /**
     * Get the Editable from the EditText.
     */
    default Editable CountEditTextGetText() {
        return getCountEditText().getEditableText();
    }

    /**
     * Set an {@link android.widget.TextView.OnEditorActionListener}
     * for the EditText. This notifies the listener whenever the text
     * of the EditText changes.
     */
    default void CountEditTextSetOnEditorActionListener(TextView.OnEditorActionListener listener) {
        getCountEditText().setOnEditorActionListener(listener);
    }
}
