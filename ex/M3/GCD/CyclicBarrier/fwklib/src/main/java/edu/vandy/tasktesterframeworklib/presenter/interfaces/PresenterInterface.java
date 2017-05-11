package edu.vandy.tasktesterframeworklib.presenter.interfaces;

/**
 * Single interface for referencing all Presenter interfaces.
 */
public interface PresenterInterface 
       extends NotifyOfGUIActionsInterface {
    /**
     * Tell the UI to reset the Control Interface Views/FABs.
     */
    public void resetControlUI();

    /**
     * Notify the presenter layer of a state change.
     */
    public void notifyOfStateChange();
}
