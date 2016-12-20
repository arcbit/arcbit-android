package com.arcbit.arcbit.ui.items;

public class ActionItem implements Item {

    public final String title;

    public ActionItem(String title) {
        this.title = title;
    }

    @Override
    public boolean isSection() {
        return false;
    }
}
